#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор уровня «HSK 7» курса «Поэтапное изучение».

Идея уровня: все часто употребимые слова, которых нет в HSK 1–6 нашего курса.
Берём их из нового официального стандарта HSK 3.0 (2021, «国际中文教育中文水平
等级标准»), уровни 1–6 — это лексика современных учебников («HSK Standard
Course», «Boya Chinese», «Developing Chinese»), и вычитаем из неё все слова,
которые уже есть в level1.json … level6.json. Получается ~2400 слов:
устойчивые сочетания (打电话, 吃饭, 车站), новая бытовая лексика (微信, 微博,
上班族, 微波炉), «недостающие» простые глаголы и прилагательные (帮, 唱, 查, 变).

Слова HSK 7–9 нового стандарта в уровень не включаются — это уже книжная и
газетная лексика (委员会, 帝国主义, 曰), а не «частая речь».

Источники:
  * список HSK 3.0 с пиньинем и частями речи —
    https://github.com/ivankra/hsk30  (файл hsk30-expanded.csv);
  * русские переводы — словарь БКРС (bkrs.info): либо sqlite-база с таблицей
    dict(hanzi, pinyin, translation), либо текстовый дамп dabkrs (*.txt).

Скрипт работает в два шага, чтобы 340-мегабайтный словарь не был нужен каждому:

  1. extract — вытаскивает из HSK 3.0 + БКРС таблицу слов и сохраняет её в
     tools/hsk7_words.tsv (иероглиф, пиньинь, часть речи, уровень HSK 3.0,
     перевод). Этот файл лежит в репозитории, его можно править руками.

        python3 tools/generate_hsk7.py extract \
            --hsk30 /path/hsk30-expanded.csv --bkrs /path/bkrs.sqlite

  2. build — из tools/hsk7_words.tsv собирает app/src/main/assets/hsk/level7.json
     (разделы по темам, группы по 5 слов, как в остальных уровнях) и добавляет
     предложения уровня 7 из tools/hsk_sentences.py в sentences.json.

        python3 tools/generate_hsk7.py build

Правки переводов: tools/hsk7_overrides.py (применяются на обоих шагах).
Нужны: pypinyin, jieba (только для build).
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import sqlite3
import sys
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import generate_hsk_course as base  # noqa: E402
from hsk7_overrides import HSK7_OVERRIDES, HSK7_SKIP  # noqa: E402
from hsk_sentences import SENTENCE_TOPICS  # noqa: E402

LEVEL = 7
TSV_PATH = os.path.join(HERE, "hsk7_words.tsv")
OUT_DIR = base.OUT_DIR

CJK = re.compile(r"[\u4e00-\u9fff〇]")
TONE_LETTERS = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"


# --------------------------------------------------------------------------- #
#                               Список HSK 3.0                                #
# --------------------------------------------------------------------------- #

def course_words() -> set[str]:
    """Все иероглифы уровней 1–6 нашего курса (с ними HSK 7 не должен пересекаться)."""
    known: set[str] = set()
    for level in range(1, LEVEL):
        path = os.path.join(OUT_DIR, f"level{level}.json")
        with open(path, encoding="utf-8") as fh:
            payload = json.load(fh)
        for topic in payload["topics"]:
            for group in topic["groups"]:
                for hanzi, _pinyin, _translation in group:
                    known.add(hanzi)
    return known


def normalize_pinyin(pinyin: str) -> str:
    """«dì-èr», «bǎo'ān», «bù dà» → «dìèr», «bǎoān», «bùdà» — как в остальных уровнях."""
    return re.sub(r"[\s\-'’]", "", pinyin.strip())


def load_hsk30(path: str, known: set[str]) -> list[dict]:
    """Слова HSK 3.0 (уровни 1–6), которых нет в курсе. Полифония схлопывается."""
    merged: "OrderedDict[str, dict]" = OrderedDict()
    with open(path, encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            level = row["Level"].strip()
            if not level.isdigit():
                continue  # 7-9
            hanzi = re.sub(r"\d+$", "", row["Simplified"].strip())  # 点1 → 点
            if not hanzi or hanzi in known or hanzi in HSK7_SKIP:
                continue
            pos = [p for p in row["POS"].strip().split("/") if p]
            pinyin = normalize_pinyin(row["Pinyin"])
            entry = merged.get(hanzi)
            if entry is None:
                merged[hanzi] = {"h": hanzi, "p": pinyin, "pos": pos, "lv": int(level)}
            else:
                for item in pos:
                    if item not in entry["pos"]:
                        entry["pos"].append(item)
                if pinyin and pinyin.lower() != entry["p"].lower() \
                        and pinyin.lower() not in entry["p"].lower():
                    entry["p"] = f"{entry['p']} / {pinyin}"
    return list(merged.values())


# --------------------------------------------------------------------------- #
#                                   БКРС                                      #
# --------------------------------------------------------------------------- #

class Bkrs:
    """Словарь БКРС: sqlite (таблица dict) или текстовый дамп dabkrs."""

    def __init__(self, path: str):
        self.con: sqlite3.Connection | None = None
        self.text: dict[str, str] = {}
        if path.endswith((".sqlite", ".db", ".sqlite3")):
            self.con = sqlite3.connect(path)
        else:
            self._load_text(path)

    def _load_text(self, path: str) -> None:
        # Формат дампа: пустая строка, иероглифы, пиньинь, перевод (одной строкой,
        # переносы закодированы как \n).
        with open(path, encoding="utf-8", errors="ignore") as fh:
            block: list[str] = []
            for line in fh:
                line = line.rstrip("\n")
                if not line.strip():
                    if len(block) >= 3:
                        self.text.setdefault(block[0].strip(), block[2])
                    block = []
                else:
                    block.append(line)
            if len(block) >= 3:
                self.text.setdefault(block[0].strip(), block[2])

    def raw(self, hanzi: str) -> str | None:
        if self.con is not None:
            row = self.con.execute(
                "select translation from dict where hanzi = ?", (hanzi,)
            ).fetchone()
            return row[0] if row else None
        return self.text.get(hanzi)


# Части речи HSK 3.0 → как БКРС подписывает разделы статьи (•I гл. / •II сущ.).
POS_MARKERS = {
    "V": ("гл",),
    "N": ("сущ",),
    "Adj": ("прил",),
    "Adv": ("нареч",),
    "Prep": ("предлог", "гл.-предлог"),
    "Conj": ("союз",),
    "M": ("счётное", "счетное", "сч. сл"),
    "Pron": ("местоим",),
    "Num": ("числ",),
    "Part": ("частица", "служебное"),
    "Interj": ("междом",),
    "Aux": ("мод. гл", "модальный"),
    "Prefix": ("словообр", "префикс", "формообр"),
    "Suffix": ("словообр", "суффикс", "формообр"),
}

SECTION_LINE = re.compile(r"^•\s*([IVX]+)\b(.*)$")
POS_LINE = re.compile(
    r"^(гл\.|сущ\.|прил\.|нареч(ие|\.)|союз|предлог|частица|междом\.|местоим\.|"
    r"числ\.|собств\.|мод\. гл\.|служебное слово|словообр\.|формообр\.|суффикс|"
    r"префикс|глагол|существительное|прилагательное|гл\.-предлог|союзное наречие|"
    r"счётное слово|счетное слово|сч\. сл\.|наречие[^\n]*|местоимение|звукоподр\.|"
    r"вводн\. слово|модальный глагол)(?=[\s/,;]|$)",
    re.I,
)
BRACKETS = re.compile(r"[（(\[][^()（）\[\]]*[)）\]]")
# Пометы вроде «бот.», «воен.», «разг.», «кит. мед.» в начале значения.
LABEL = re.compile(r"^(?:(?:[а-яё]{1,7}\.\s*|[а-яё]+\.-[а-яё]+\.\s*)+)", re.I)
# Значения с такими пометами берём только если других нет.
RARE = re.compile(
    r"^(\*|уст\.|устар\.|стар\.|книжн\.|поэт\.|диал\.|вм\.|сокр\.|вежл\.|груб\.|"
    r"бран\.|жарг\.|обр\.|см\.|ср\.|также|тж\.|мат\.|физ\.|хим\.|биол\.|бот\.|"
    r"зоол\.|мед\.|юр\.|эк\.|фин\.|тех\.|техн\.|воен\.|геол\.|геогр\.|лингв\.|"
    r"грам\.|филос\.|рел\.|будд\.|даос\.|ист\.|астр\.|муз\.|театр\.|спорт\.|"
    r"комп\.|инт\.|эл\.|радио|авиа|мор\.|ж\.-д\.|с\.-х\.|пищ\.|полигр\.|кул\.)",
    re.I,
)
SKIP_SENSE = re.compile(r"^(собств\.|фамилия$|[А-ЯЁ][а-яё\-]+ \(фамилия\))", re.I)
SENSE_NUMBER = re.compile(r"^(\d+|[а-яa-z])\)\s*")
OTHER_READING = re.compile(rf"^\[[a-z{TONE_LETTERS} ,;]+\]$", re.I)


def strip_tones(pinyin: str) -> str:
    table = str.maketrans(TONE_LETTERS, "aaaaeeeeiiiioooouuuuüüüü")
    return re.sub(r"[\s'’\-]", "", pinyin.lower()).translate(table)


def select_reading(text: str, pinyin: str) -> str:
    """Оставляем в статье только строки нужного чтения ([qī] … [jī] …, блоки «-----»)."""
    wanted = [normalize_pinyin(p).lower() for p in pinyin.split(" / ") if p.strip()]
    wanted_base = [strip_tones(p) for p in wanted]

    def block_reading(block: str) -> str:
        head = block.strip().split("\n", 1)[0]
        match = re.match(rf"^\s*(?:•\s*[IVX]+\s*)?\[?([a-z{TONE_LETTERS}][a-z{TONE_LETTERS}\s,;]*)\]?\s*$", head, re.I)
        return match.group(1) if match else ""

    # 1) блоки разных чтений, разделённые «-----»
    blocks = [b for b in re.split(r"\n-{3,}\n", text) if b.strip()]
    if len(blocks) > 1:
        scored = []
        for block in blocks:
            reading = block_reading(block)
            variants = [normalize_pinyin(v).lower() for v in re.split(r"[,;]", reading) if v.strip()]
            score = 2 if any(v in wanted for v in variants) else \
                1 if any(strip_tones(v) in wanted_base for v in variants) else 0
            scored.append((score, block))
        best = max(score for score, _ in scored)
        text = "\n".join(block for score, block in scored if score == best or best == 0)

    # 2) маркеры чтений внутри блока: «[qī]» … «[jī]»
    lines = text.split("\n")
    if not any(OTHER_READING.match(line.strip()) for line in lines):
        return text
    groups: list[tuple[str, list[str]]] = []
    current: tuple[str, list[str]] = ("", [])
    for line in lines:
        stripped = line.strip()
        if OTHER_READING.match(stripped):
            if current[0] or current[1]:
                groups.append(current)
            current = (stripped.strip("[]"), [])
        else:
            current[1].append(line)
    groups.append(current)

    def group_score(reading: str) -> int:
        if not reading:
            return 1  # общая часть до первого маркера
        variants = [normalize_pinyin(v).lower() for v in re.split(r"[,;]", reading) if v.strip()]
        if any(v in wanted for v in variants):
            return 2
        if any(strip_tones(v) in wanted_base for v in variants):
            return 1
        return 0

    best = max(group_score(reading) for reading, _ in groups)
    kept = [lines_ for reading, lines_ in groups if group_score(reading) == best]
    return "\n".join(line for lines_ in kept for line in lines_)


def split_sections(raw: str, pinyin: str) -> list[tuple[str, list[str]]]:
    """Статья БКРС → список (подпись части речи, строки значений) для нужного чтения."""
    text = select_reading(raw.replace("\\n", "\n"), pinyin)

    sections: list[tuple[str, list[str]]] = []
    label = ""
    lines: list[str] = []
    for line in text.split("\n"):
        stripped = line.strip()
        if not stripped:
            continue
        match = SECTION_LINE.match(stripped)
        if match:
            if lines:
                sections.append((label, lines))
            label, lines = "", []
            rest = match.group(2).strip()
            # «•I yú предлог» — после номера может стоять чтение и/или часть речи
            rest = re.sub(rf"^[a-z{TONE_LETTERS}]+\b", "", rest, flags=re.I).strip()
            if rest:
                label = rest
            continue
        if POS_LINE.match(stripped) and not SENSE_NUMBER.match(stripped) \
                and len(stripped) <= 40 and not CJK.search(stripped):
            if lines:
                sections.append((label, lines))
                label, lines = "", []
            label = f"{label} {stripped}".strip()
            continue
        if CJK.match(stripped):
            continue  # пример употребления
        lines.append(stripped)
    if lines:
        sections.append((label, lines))
    return sections


def section_matches(label: str, pos: str, primary: bool) -> bool:
    """primary=True: часть речи стоит в подписи первой («прил. /наречие» → прил.)."""
    low = label.lower().strip()
    markers = POS_MARKERS.get(pos, ())
    if primary:
        return any(low.startswith(marker) for marker in markers)
    return any(marker in low for marker in markers)


def clean_sense(sense: str) -> str:
    text = SENSE_NUMBER.sub("", sense)
    for _ in range(4):  # вложенные скобки: (период (см. 节气))
        cleaned = BRACKETS.sub(" ", text)
        if cleaned == text:
            break
        text = cleaned
    text = re.sub(r"[（(\[][^()（）\[\]]*$", "", text)  # незакрытая скобка
    text = re.sub(r"\s+", " ", text).strip(" ,;:—-")
    text = LABEL.sub("", text)
    text = text.replace("[", "").replace("]", "")
    text = CJK.sub("", text)
    text = re.sub(r"\s+([,;:])", r"\1", text)
    text = re.sub(r"\s+", " ", text).strip(" ,;:—-…")
    return text


def is_rare(sense: str) -> bool:
    body = SENSE_NUMBER.sub("", sense).strip()
    return bool(RARE.match(body)) or body.startswith("*")


def join_glosses(pieces: list[str], limit: int = 40) -> str:
    joined = ""
    for piece in pieces:
        candidate = piece if not joined else f"{joined}; {piece}"
        if len(candidate) > limit and joined:
            break
        joined = candidate
        if len(joined) >= limit:
            break
    return joined


VERB_GLOSS = re.compile(r"^(не\s+)?[а-яё]+(ть|ться|чь|чься|сти|стись|зти|зтись)(\b|,|;)", re.I)
ADJ_GLOSS = re.compile(r"^[а-яё]+(ый|ий|ой|ая|яя|ое|ее|ые|ие|ший|щий|вший|нный|тый)(\b|,|;)", re.I)
ADV_GLOSS = re.compile(r"^(не\s+)?[а-яё]+(о|е|ом|ю|ки|ью|ему|ому)(\b|,|;)|^(в|на|по|с|из|без|до|за|при|через)\s", re.I)


def sense_fits_pos(sense: str, pos: str) -> bool:
    """Похоже ли русское значение на нужную часть речи (грубая эвристика по окончанию)."""
    if pos == "V":
        return bool(VERB_GLOSS.match(sense))
    if pos == "Adj":
        return bool(ADJ_GLOSS.match(sense))
    if pos == "Adv":
        return bool(ADV_GLOSS.match(sense)) and not VERB_GLOSS.match(sense)
    return True


def order_by_pos(cleaned: list[str], pos: list[str]) -> list[str]:
    """Если первое значение не той части речи, что в HSK, поднимаем подходящее выше."""
    for item in pos:
        if item not in ("V", "Adj", "Adv"):
            continue
        if cleaned and sense_fits_pos(cleaned[0], item):
            return cleaned
        for index, sense in enumerate(cleaned[:6]):
            if sense_fits_pos(sense, item):
                return [sense] + cleaned[:index] + cleaned[index + 1:]
    return cleaned


def glosses_from_lines(lines: list[str], allow_rare: bool, pos: list[str] = ()) -> list[str]:
    cleaned: list[str] = []
    for line in lines:
        if not allow_rare and is_rare(line):
            continue
        if SKIP_SENSE.match(SENSE_NUMBER.sub("", line)):
            continue
        sense = clean_sense(line)
        if not sense or sense.lower().startswith(("см.", "ср.", "вм.")):
            continue
        # «модальный глагол возможности: можно, возможно» → «можно, возможно»
        colon = re.match(r"^([^:]{12,}):\s*(.+)$", sense)
        if colon and len(colon.group(2)) >= 3:
            sense = colon.group(2)
        cleaned.append(sense)
    cleaned = order_by_pos(cleaned, list(pos))
    pieces: list[str] = []
    for sense in cleaned:
        for piece in re.split(r";\s*", sense):
            piece = piece.strip(" ,.")
            if len(piece) > 45:  # слишком длинное описание режем по запятым
                short = ""
                for part in piece.split(","):
                    candidate = part.strip() if not short else f"{short}, {part.strip()}"
                    if len(candidate) > 45 and short:
                        break
                    short = candidate
                piece = short
            if len(piece) < 2:
                continue
            if piece.lower() not in [p.lower() for p in pieces]:
                pieces.append(piece)
        if len(pieces) >= 4:
            break
    return pieces


def pick_gloss(dictionary: Bkrs, hanzi: str, pinyin: str, pos: list[str], depth: int = 0) -> str:
    """Короткая русская подпись слова из статьи БКРС (с учётом части речи HSK)."""
    raw = dictionary.raw(hanzi)
    if not raw:
        return ""
    redirect = re.match(
        r"^\s*(?:см\.|вм\.|сокр\.)\s*([\u4e00-\u9fff]+)\s*$", raw.replace("\\n", " ")
    )
    if redirect and depth < 2:
        target = pick_gloss(dictionary, redirect.group(1), pinyin, pos, depth + 1)
        if target:
            return target

    sections = split_sections(raw, pinyin)
    if not sections:
        return ""

    ordered: list[list[str]] = []
    # 1) разделы, совпадающие с частью речи HSK (в порядке частей речи HSK):
    #    сначала где она главная («наречие времени»), потом где упомянута («прил. /наречие»)
    for primary in (True, False):
        for item in pos:
            for label, lines in sections:
                if label and section_matches(label, item, primary) and lines not in ordered:
                    ordered.append(lines)
    # 2) остальные разделы, кроме имён собственных
    for label, lines in sections:
        if "собств" in label.lower():
            continue
        if lines not in ordered:
            ordered.append(lines)
    for label, lines in sections:
        if lines not in ordered:
            ordered.append(lines)

    for allow_rare in (False, True):
        for lines in ordered:
            pieces = glosses_from_lines(lines, allow_rare, pos)
            if pieces:
                return join_glosses(pieces)
    return ""


# --------------------------------------------------------------------------- #
#                                  extract                                    #
# --------------------------------------------------------------------------- #

def cmd_extract(args: argparse.Namespace) -> int:
    known = course_words()
    words = load_hsk30(args.hsk30, known)
    dictionary = Bkrs(args.bkrs)

    missing: list[str] = []
    rows: list[list[str]] = []
    for word in words:
        translation = HSK7_OVERRIDES.get(word["h"]) or pick_gloss(
            dictionary, word["h"], word["p"], word["pos"]
        )
        if not translation:
            missing.append(word["h"])
        rows.append([word["h"], word["p"], "/".join(word["pos"]), str(word["lv"]), translation])

    with open(TSV_PATH, "w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh, delimiter="\t", lineterminator="\n")
        writer.writerow(["hanzi", "pinyin", "pos", "hsk30", "translation"])
        writer.writerows(rows)

    print(f"HSK 7: {len(rows)} слов -> {TSV_PATH}")
    if missing:
        print(f"Без перевода ({len(missing)}): {' '.join(missing)}")
        print("Добавьте их в tools/hsk7_overrides.py и повторите extract (или build).")
    return 0


# --------------------------------------------------------------------------- #
#                                   build                                     #
# --------------------------------------------------------------------------- #

def load_tsv() -> list[dict]:
    words: list[dict] = []
    with open(TSV_PATH, encoding="utf-8") as fh:
        for row in csv.DictReader(fh, delimiter="\t"):
            hanzi = row["hanzi"].strip()
            if not hanzi or hanzi in HSK7_SKIP:
                continue
            translation = HSK7_OVERRIDES.get(hanzi) or row["translation"].strip()
            words.append(
                {
                    "h": hanzi,
                    "p": row["pinyin"].strip(),
                    "pos": [p for p in row["pos"].split("/") if p],
                    "lv": int(row["hsk30"] or 0),
                    "t": translation,
                }
            )
    return words


# Часть речи HSK → раздел курса, если правила по смыслу не сработали.
POS_TOPIC = {
    "V": "actions", "Adj": "qualities", "Adv": "qualities", "Pron": "pronouns",
    "Num": "numbers", "M": "numbers", "Conj": "grammar", "Prep": "grammar",
    "Part": "grammar", "Aux": "grammar", "Interj": "communication",
    "Prefix": "grammar", "Suffix": "grammar",
}


# Подсказки классификатору для лексики HSK 7: правила базового скрипта писались
# под HSK 1–6 и многие «новые» слова (爸, 今年, 可乐) иначе уезжают в «Разное».
EXTRA_TOPICS: dict[str, str] = {}
for _topic, _glyphs in {
    "people": "爸 妈 哥 姐 弟 妹 朋友们 男孩儿 男朋友 女孩儿 女朋友 爱人 父母 姐妹 老公 老婆 孙女 "
              "子女 儿女 少年 帅哥 华人 穷人 富人 父女 父子 母女 母子 孤儿 恩人 王 王后 国王 上帝 神 鬼 "
              "骗子 强盗 小偷儿 车主 消费者 残疾人 毕业生 诗人 设计师 宇航员 飞行员 模特儿 歌迷 偶像 "
              "听众 高手 大使 少儿 童年 一生 小时候 岁数 辈 各位 有人 咱 你们 他们 她们 它们 蓝领 白领 "
              "上班族 大人 老人 老太太 老头儿 大哥 大姐 大妈 大爷 男 女 男女 男子 女子 男士 男性 女性 "
              "新人 亲人 亲属 家人 家长 大家 人们 人群 男生 女生 学员 队员 球员 球星 影星 歌星 歌手 "
              "画家 作家 学者 商人 军人 战士 兵 士兵 战友 老乡 好友 网友 熟人 盲人 胖子 酒鬼 名人 艺人 "
              "祖母 大人 成人 青年 中年 老年 自我 自身 独自 各个 师父 师生 院长 校长 主任 局长 部长 "
              "市长 队长 组长 班长 团长 处长 司长 所长 会长 厂长 船长 首相 首脑 总监 总经理 秘书长 "
              "负责人 发言人 公务员 外交官 法官 律师 清洁工 厨师 保安 民警 交警 摄影师 主持人 主角",
    "time": "今年 半年 后年 年初 年底 年前 星期日 星期天 上次 下次 上周 下周 上个月 下个月 今后 此后 "
            "此前 之后 之前 当天 当年 一时 早就 早已 整天 日夜 明日 今日 近日 过后 往后 到期 半天 "
            "不一会儿 多云 零下 白天 半夜 早晨 夜里 夜间 此刻 此时 这时 这时候 那时 那时候 那会儿 "
            "前年 明年 前天 后天 春天 夏天 秋天 冬天 春季 夏季 秋季 冬季 新年 春节 中秋节 清明节 "
            "圣诞节 假期 假日 节假日 长假 暑假 休假 工作日 月份 月底 周 学年 学时 长期 短期 近期 "
            "初期 早期 中期 同期 本期 时节 时时 有时 有时候 多久 多年 多次 当时 晴天 阴天 暴雨 暴风雨 "
            "雨 雨水 雪 下雪 打雷 乌云 蓝天 气温 高温 低温 降温 寒冷 凉 暖 温度 天气 气候 早晚 从小 "
            "长久 长远 永远 一生 那时 这时 此时 时 期 季 年 天 日 月 钟头 一下子 一会儿 一时 一齐 一同",
    "city": "汽车 电动车 电车 直升机 站台 大街 前方 地下室 大厅 礼堂 教堂 办事处 平台 车号 车牌 车站 "
            "地铁站 停车场 火车 高铁 快车 慢车 特快 客车 公交车 大巴 巴士 马车 摩托 机动车 车辆 飞船 "
            "军舰 卧铺 车票 机票 停车 倒车 开车 骑车 修车 打车 上车 下车 乘车 坐车 车上 路口 路上 "
            "路边 马路 公路 高速公路 铁路 道路 大道 路线 线路 交通 堵车 晚点 乘客 旅客 车主 加油站 "
            "航班 港口 机场 码头 桥 隧道 街 街头 城 城区 城镇 城乡 城里 市区 市民 郊区 商场 商城 "
            "写字楼 大楼 楼房 楼道 楼梯 楼上 楼下 上楼 下楼 电影院 剧场 体育场 体育馆 球场 赛场 "
            "场馆 场地 游泳池 动物园 网吧 书店 药店 餐馆 饭店 酒店 旅馆 旅店 食堂 快餐 门口 门票 "
            "入口 出口 广场 公园 园 校园 大学 小学 中学 高中 初中 学院 教学楼 阅览室 实验室 研究所 "
            "教育部 法庭 银行 邮局 取款机 信箱 邮箱 邮件 快递 通道 窗口 站 电梯 出租 房租 房价 住房 房屋",
    "home": "窗台 闹钟 耳机 电视机 板 卡 键 开关 热水器 煤气 暖气 冷气 脸盆 剪子 钢笔 墨水 胶带 吸管 "
            "口袋 柱子 库 盒 箱 箱子 环 台灯 电灯 灯光 电饭锅 微波炉 电器 家电 衣架 书柜 书桌 书房 "
            "写字台 柜子 楼梯 地板 床 屋 房子 房间 院子 院 大门 门 窗子 窗户 地下室 车库 厨房 卫生间 "
            "客厅 卧室 阳台 花瓶 毛笔 圆珠笔 牙刷 刷子 洗衣粉 塑料 袋 钱包 皮包 背包 书包 相机 摄像机 "
            "录音机 打印机 计算机 游戏机 机器人 屏幕 电话 手机 家里 在家 回家 家园 老家 本地 家 屋子",
    "food": "可乐 汽水 酸奶 奶茶 奶粉 火腿 香肠 寿司 三明治 番茄 茄子 白菜 果酱 大米 薯条 薯片 瓜 "
            "水产品 乳制品 豆制品 农产品 蛋 鸡 牛 羊 肉 鱼 饭 早饭 午饭 晚饭 早餐 午餐 晚餐 西餐 中餐 "
            "快餐 套餐 盒饭 外卖 美食 面条儿 方便面 饼 月饼 烤肉 酒 白酒 红酒 葡萄酒 红茶 绿茶 茶叶 "
            "美元 凉水 冷水 热水 纯净水 饼 糖 盐 油 酱 餐 吃饭 做饭 点 餐饮 食欲 饮料 酒水 咖啡 果汁",
    "clothes": "短裤 球鞋 领带 背心 凉鞋 雨衣 棉 珠宝 玉 宝 宝石 毛衣 大衣 上衣 内衣 外衣 外套 衬衣 "
               "西装 拖鞋 高跟鞋 帽子 手套 围巾 袜子 皮 布 丝 首饰 戒指 眼镜 名牌儿 品牌 牌子 型号",
    "nature": "草地 海边 草原 山谷 山峰 山坡 山区 海湾 海浪 海水 海底 海 岸上 松树 桃树 果树 树林 "
              "树叶 梅花 桃花 鲜花 熊 大熊猫 公鸡 母鸡 奶牛 猴 虎 兔 鼠 鸭子 虫子 羊 鸡 泥 沟 地形 "
              "地带 两岸 南北 光线 气体 水分 水库 矿 煤 江 湖 河流 泉 岛 森林 山 石头 沙子 土 田 "
              "月球 太阳能 天上 空中 星星 北 北边 南边 东边 西边 大自然 野生 野 风 雨 雪 冰 冰雪 灾 "
              "水灾 火灾 灾区 受灾 地面 地下 地上 天下 全球 全世界 世界 环保 绿化 垃圾 污水 燃料 能源",
    "hobby": "电视剧 画儿 图画 舞台 歌声 歌曲 专辑 诗歌 喜剧 悲剧 相声 话剧 戏 戏曲 动画 视频 博客 "
             "微博 微信 网址 网页 网上 网吧 大奖赛 半决赛 比分 抽奖 皮球 台上 全场 掌声 排行榜 铃声 "
             "音乐会 演唱会 晚会 乐队 乐曲 民歌 唱片 影片 影视 短片 录像 音像 电影 演唱 歌唱 唱 跳 "
             "舞 跳水 跳高 跳远 长跑 登山 健身 体操 足球 篮球 球拍 球队 运动会 亚运会 世界杯 联赛 "
             "大赛 单打 双打 参赛 犯规 得分 金牌 银牌 铜牌 冠军 打牌 打球 玩儿 游戏 游玩 度假 旅行 "
             "旅游 游客 游人 照相 拍照 拍摄 摄像 摄影 相片 图片 画 画家 明星 影迷 歌迷 球迷 迷 娱乐 "
             "文娱 爱好 兴趣 感兴趣 有意思 好玩儿 京剧 钢琴 琴 鼓 演出 上演 出场 开幕 闭幕 开幕式 闭幕式",
    "study": "笔记 例子 音节 括号 辞典 奖学金 实验室 小组 点名 稿子 说明书 类型 型 长度 宽度 尺寸 "
             "课本 课文 课堂 生词 听写 口语 听力 读音 语音 文字 汉字 英文 英语 华语 法语 日语 外语 "
             "外文 西班牙语 试题 考题 考场 考生 分数 学分 学年 学时 学费 学科 学会 学习 自学 补课 "
             "补习 补考 笔试 口试 面试 报考 招生 入学 升学 开学 放学 上课 下课 上学 讲课 听讲 教学 "
             "教师 学员 学生 大学生 中学生 小学生 留学生 毕业生 校园 班级 班长 组长 年级 中级 初等 "
             "高等 高中 初中 中小学 大学 学院 培训班 必修 选修 期末 期中 大纲 教育 知识 研究 研制 "
             "研发 科研 学者 论文 作业 讲话 讲 教 学 练 读书 抄写 识字 复习 预习 练习 题 答 答案",
    "body": "体重 泪 泪水 眼泪 胡子 流感 儿科 外科 医学 医药 医疗 药店 药品 药物 药片 药水 中药 中医 "
            "西医 使劲 脚步 脚印 笑脸 笑容 笑声 个儿 身高 身上 身边 身体 心 心脏病 头 头疼 疼痛 痛 "
            "肿 伤 伤口 伤害 伤亡 伤员 病 病人 病房 病情 看病 治病 住院 出院 体检 急救 康复 疗养 "
            "保健 卫生 健康 健身 睡眠 睡 睡着 午睡 起 起床 出汗 咳 吸烟 吸毒 中毒 手 手里 双手 两手 "
            "肩 肝 肠 胆 胸部 大脑 脑子 头脑 牙 眼 眼前 眼里 嘴巴 皮 血液 血管 骨头 牙刷 刷牙 洗澡 "
            "视力 听力 食欲 减肥 长大 生长 成长 出生 死 生命 命 疼 累 饿 渴 冷 热 暖 凉 湿 干净 洗",
    "society": "高层 国会 部队 海军 空军 陆军 炮 剑 炸弹 炸药 战场 救灾 灾区 峰会 传媒 大使 基督教 佛 佛教 "
               "道教 上帝 圣诞节 口号 名义 人民 人权 民意 民工 民警 交警 公安 警察 政党 政府 国民 "
               "国家 国旗 国歌 国庆 国产 国内 国外 国有 全国 首相 首脑 首席 总部 总监 委员会 议会 "
               "议题 法 法规 法制 法庭 法官 律师 犯罪 犯 罪 罪恶 违法 违规 处罚 罚 起诉 官司 判 捕 "
               "偷 骗子 强盗 小偷儿 盗版 正版 监测 安检 报警 救援 救助 援助 捐款 捐赠 捐助 志愿 "
               "志愿者 公众 群体 大众 社 社会 社区 居民 市民 村 村庄 乡 乡村 城乡 内地 本土 中华 "
               "中华民族 中外 联合国 亚运会 世界杯 军舰 军人 兵 战士 战友 作战 战胜 敌人 和平 会谈 "
               "会见 出访 代表团 外交官 选民 投诉 举手 集会 游行 示威 阶级 制度 机制 体制 权 权利",
    "work": "商务 债 账 消费者 办公 办事 办事处 写字楼 会员 员工 职工 职责 主任 经理 老板 上班 下班 "
            "上班族 白领 蓝领 工作日 加班 求职 招聘 应聘 简历 面试 合约 签约 签名 签 收费 手续费 "
            "小费 房租 租金 价 价钱 高价 特价 票价 物价 定价 房价 涨价 降价 存款 取款 汇款 支付 "
            "付 付出 交费 赔 赚钱 省钱 理财 投资 股 上市 外汇 外币 美元 美金 金额 金钱 现金 银行卡 "
            "信用 贷款 经济 商人 商城 商场 生意 买卖 出售 促销 收购 购买 产量 产品 成品 精品 厂 "
            "厂长 厂商 工厂 工程 工艺 制成 打造 创办 创建 开业 加盟 品牌 总数 总量 总经理 总监",
    "science": "二维码 网页 网址 网吧 多媒体 机器人 电子版 电子邮件 博客 视频 音像 平台 键 屏幕 数据 "
               "软件 程序 系统 高科技 科技 科研 研发 研制 太阳能 电力 电动 发电 能源 燃料 气体 化 "
               "转化 物理 化学 生物 医学 卫星 火箭 飞船 宇航员 直升机 飞行 飞行员 机制 技术 工程 "
               "装置 设备 配置 型号 启动 开机 关机 充电 杀毒 升级 输出 传输 下载 上传 发送 接收 网络",
    "communication": "话 没事儿 比如说 哈哈 能不能 加油 见过 看起来 看上去 听说 请问 请坐 请进 招呼 "
                     "晚安 早安 干什么 干吗 怎么办 怎样 什么样 谈话 讲话 通话 打电话 回信 来信 留言 "
                     "口号 传言 说法 一句话 就是说 这就是说 说实话 很难说 没错 没什么 不怎么样 挺好 "
                     "算是 也好 罢了 得了 完了 是不是 有没有 好不好 行不行 表面上 实际上 事实上 "
                     "一般来说 另一方面 一方面 意想不到 没想到 想不到 用不着 用得着 离不开 赶不上 "
                     "不见 不许 不停 不再 不曾 不至于 只顾 只管 只见 只得 只能 只不过 决不 从不 从没",
    "actions": "冒 毁 灭 找到 出门 出事 通信 沿着 向前 低于 认同 走 跑 跳 飞 游 爬 拿走 带来 送到 送给 "
               "交给 递给 放到 放下 拿出 拿到 打开 关上 穿上 坐下 站住 停下 留下 剩下 抓住 记住 看到 "
               "听到 听见 见到 遇见 碰到 得到 想到 想起 收到 接到 做到 得出 找出 看出 查出 认出 显出 "
               "指出 传来 传出 发出 走过 走进 走开 进来 进去 出来 出去 回来 回去 上来 上去 下来 下去 "
               "过来 回到 来到 赶到 赶上 到来 前来 前往 归 返回 退出 撤离 逃跑 逃走 躲 避 藏 追 捉",
    "feelings": "悲剧 悲伤 担忧 愁 慌 烦 怨 怕 惊喜 惊人 感人 动人 亲密 亲眼 高兴 乐 爽 酷 妙 难忘 "
                "关爱 喜爱 喜欢 爱国 忠心 诚信 真诚 修养 用心 心愿 心里 心中 内心 情感 感到 感兴趣",
    "grammar": "此次 此事 等到 该 刚 而是 却是 由此 不仅仅 不能不 之外 之下 之内 之中 之间 之后 之前 之一 之类 以上 以外 "
               "以下 以内 者 其 此 因 曾 稍 另 尽可能 使得 为何 不论 便是 或 或是 若 如 如同 如此 "
               "如下 如一 虽 既 且 于 自 及 极了 第 们 子 性 员 化 族 界 力 头 儿 一下儿 一下子 一番 "
               "大大 大都 大多 大力 多半 大约 差点儿 差一点儿 好容易 好不容易 稍 越来越 越 更是 "
               "更 仅 仅仅 只是 只有 就是 就要 快要 将 将要 必 必将 已 早已 曾 仍 却 倒是 再说 再也 "
               "总 全 全都 处处 四处 到处 一齐 一同 一道 一路上 一时 加以 加上 为止 为主 为此 据",
}.items():
    for _glyph in _glyphs.split():
        EXTRA_TOPICS.setdefault(_glyph, _topic)


def classify(word: dict) -> str:
    hint = EXTRA_TOPICS.get(word["h"])
    if hint:
        return hint
    topic = base.classify(word["h"], word["t"], "")
    if topic == base.MISC_TOPIC[0]:
        for pos in word["pos"]:
            if pos in POS_TOPIC:
                return POS_TOPIC[pos]
    return topic


def build_level(words: list[dict]) -> dict:
    titles = base.build_topic_index()
    titles[base.MISC_TOPIC[0]] = (base.MISC_TOPIC[1], base.MISC_TOPIC[2])

    by_topic: "OrderedDict[str, list[dict]]" = OrderedDict()
    for topic_id in base.TOPIC_ORDER:
        by_topic[topic_id] = []
    by_topic[base.MISC_TOPIC[0]] = []
    for word in words:
        by_topic.setdefault(classify(word), []).append(word)

    payload = {"level": LEVEL, "words": len(words), "topics": []}
    misc: list[dict] = []
    for topic_id, topic_words in by_topic.items():
        if topic_id == base.MISC_TOPIC[0]:
            continue
        if len(topic_words) < base.MIN_TOPIC_WORDS:
            misc.extend(topic_words)
            continue
        payload["topics"].append(topic_payload(topic_id, titles[topic_id], topic_words))
    misc.extend(by_topic[base.MISC_TOPIC[0]])
    if misc:
        payload["topics"].append(
            topic_payload(base.MISC_TOPIC[0], titles[base.MISC_TOPIC[0]], misc)
        )
    return payload


def topic_payload(topic_id: str, title_emoji: tuple[str, str], words: list[dict]) -> dict:
    title, emoji = title_emoji
    groups = base.chunk_words(words)
    return {
        "id": f"l{LEVEL}_{topic_id}",
        "title": title,
        "emoji": emoji,
        "groups": [[[w["h"], w["p"], w["t"]] for w in g] for g in groups],
    }


def build_sentences(vocab: set[str], higher: set[str]) -> tuple[list[dict], list[str]]:
    """Предложения уровня 7 → формат sentences.json; плюс список замечаний."""
    from pypinyin import Style, lazy_pinyin  # noqa: PLC0415
    import jieba  # noqa: PLC0415

    notes: list[str] = []
    topics: list[dict] = []
    for topic in SENTENCE_TOPICS:
        if topic["level"] != LEVEL:
            continue
        entries = []
        for hanzi, russian in topic["sentences"]:
            tokens = [t for t in jieba.lcut(hanzi) if t.strip()]
            han_tokens = [t for t in tokens if CJK.search(t)]
            for token in han_tokens:
                if not base.token_ok(token, vocab):
                    notes.append(f"«{token}» нет в словаре HSK 1–7 (в «{hanzi}»)")
            pinyin_tokens = []
            for token in tokens:
                if CJK.search(token):
                    pinyin_tokens.append("".join(lazy_pinyin(token, style=Style.TONE)))
                else:
                    pinyin_tokens.append(token)
            pinyin = " ".join(pinyin_tokens)
            pinyin = re.sub(r"\s+([，。！？；：、])", r"\1", pinyin).strip()
            ru_words = [w.strip(" ,.!?;:«»\"'") for w in russian.split()]
            entries.append([hanzi, pinyin, russian, han_tokens, [w for w in ru_words if w]])
        topics.append(
            {"id": topic["id"], "title": topic["title"], "emoji": topic["emoji"], "sentences": entries}
        )
    return topics, notes


def cmd_build(_args: argparse.Namespace) -> int:
    known = course_words()
    words = load_tsv()

    duplicates = [w["h"] for w in words if w["h"] in known]
    seen: set[str] = set()
    repeated = [w["h"] for w in words if w["h"] in seen or seen.add(w["h"])]  # type: ignore[func-returns-value]
    empty = [w["h"] for w in words if not w["t"]]
    if duplicates or repeated or empty:
        if duplicates:
            print(f"ОШИБКА: слова уже есть в HSK 1–6: {' '.join(duplicates)}")
        if repeated:
            print(f"ОШИБКА: повторы в TSV: {' '.join(repeated)}")
        if empty:
            print(f"ОШИБКА: нет перевода: {' '.join(empty)}")
        return 1

    payload = build_level(words)
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"level{LEVEL}.json")
    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, separators=(",", ":"))

    print(f"HSK {LEVEL}: {len(words)} слов, разделов {len(payload['topics'])}")
    for topic in payload["topics"]:
        count = sum(len(g) for g in topic["groups"])
        print(f"  {topic['title']:26s} {count:4d} слов · {len(topic['groups']):3d} групп")
    print(f"-> {out_path} ({os.path.getsize(out_path)} байт)")

    # ------------------------------ Предложения ------------------------------ #
    vocab = set(known) | {w["h"] for w in words}
    topics, notes = build_sentences(vocab, set())
    sent_path = os.path.join(OUT_DIR, "sentences.json")
    with open(sent_path, encoding="utf-8") as fh:
        sentences = json.load(fh)
    levels = [lvl for lvl in sentences.get("levels", []) if lvl.get("level") != LEVEL]
    if topics:
        levels.append({"level": LEVEL, "topics": topics})
    levels.sort(key=lambda lvl: lvl.get("level", 0))
    sentences["levels"] = levels
    with open(sent_path, "w", encoding="utf-8") as fh:
        json.dump(sentences, fh, ensure_ascii=False, separators=(",", ":"))
    total = sum(len(t["sentences"]) for t in topics)
    print(f"Предложений HSK {LEVEL}: {total} в {len(topics)} разделах -> {sent_path}")
    for note in dict.fromkeys(notes):
        print("  примечание:", note)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    extract = sub.add_parser("extract", help="HSK 3.0 + БКРС -> tools/hsk7_words.tsv")
    extract.add_argument("--hsk30", required=True, help="hsk30-expanded.csv из github.com/ivankra/hsk30")
    extract.add_argument("--bkrs", required=True, help="БКРС: sqlite (таблица dict) или текстовый дамп")
    extract.set_defaults(func=cmd_extract)
    build = sub.add_parser("build", help="tools/hsk7_words.tsv -> level7.json + sentences.json")
    build.set_defaults(func=cmd_build)
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
