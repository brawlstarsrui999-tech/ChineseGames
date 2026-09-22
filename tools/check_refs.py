#!/usr/bin/env python3
"""Статические проверки Kotlin-кода без компилятора.

Сборка APK требует JDK + Android SDK, которых может не быть под рукой,
поэтому самые частые ошибки «не компилируется» ловим здесь:

1. Голое имя иконки Material.  `Icons.Filled.Star` — это РАСШИРЕНИЕ
   (`val Icons.Filled.Star: ImageVector`), поэтому `import ...filled.Star`
   не даёт пользоваться именем `Star` напрямую — нужен приёмник.
   Ошибка компилятора: «Unresolved reference … receiver type mismatch».
2. Обращение к `Icons.<...>.<Name>` без соответствующего импорта.
3. Именованный аргумент, которого нет в сигнатуре функции проекта,
   или непереда́нный обязательный параметр.
4. Внутренний импорт `com.chinesegames.app.*`, которому ничего не соответствует.
5. Вызов функции проекта, которой не существует.

Запуск:  python3 tools/check_refs.py     (код возврата 1, если найдены ошибки)
"""

from __future__ import annotations

import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCES = os.path.join(ROOT, "app", "src", "main", "java")

ICON_STYLES = {
    "Filled": "filled",
    "Outlined": "outlined",
    "Rounded": "rounded",
    "Sharp": "sharp",
    "TwoTone": "twotone",
}
ICON_USE = re.compile(
    r"Icons\.((?:AutoMirrored\.)?(?:Filled|Outlined|Rounded|Sharp|TwoTone))\.([A-Z]\w+)"
)
ICON_IMPORT = re.compile(r"^import\s+androidx\.compose\.material\.icons\.[\w.]*?(\w+)$", re.M)
INTERNAL_IMPORT = re.compile(r"^import\s+(com\.chinesegames\.app\.[\w.]+)$", re.M)


def strip_noise(src: str) -> str:
    """Убираем комментарии и строковые литералы, чтобы не искать в них код."""
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    src = re.sub(r'"""(?:[^"]|"(?!""))*"""', '""', src, flags=re.S)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    return src


def is_depth(ch: str, prev: str) -> int:
    """Изменение глубины вложенности. `<`/`>` считаем скобками только у дженериков."""
    if ch in "([{":
        return 1
    if ch in ")]}":
        return -1
    if ch == "<" and (prev.isalnum() or prev in "_>?"):
        return 1
    if ch == ">" and (prev.isalnum() or prev in "_)? "):
        return -1
    return 0


def split_top_level(text: str) -> list[str]:
    parts, depth, cur, prev = [], 0, "", " "
    for ch in text:
        if ch == "," and depth == 0:
            parts.append(cur)
            cur, prev = "", " "
            continue
        depth = max(0, depth + is_depth(ch, prev))
        cur += ch
        prev = ch
    if cur.strip():
        parts.append(cur)
    return parts


def balanced(src: str, open_index: int) -> tuple[str | None, int]:
    """Текст внутри скобок, начинающихся в open_index, и индекс закрывающей."""
    depth, i = 0, open_index
    while i < len(src):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[open_index + 1 : i], i
        i += 1
    return None, open_index


def parse_params(params: str) -> tuple[list[str], set[str]]:
    """Параметры в порядке объявления + множество обязательных."""
    order, required = [], set()
    for part in split_top_level(params):
        match = re.match(r"^\s*(\w+)\s*:\s*(.+)$", part.strip(), re.S)
        if not match:
            continue
        name = match.group(1)
        rest, depth, has_default, prev = match.group(2), 0, False, " "
        for ch in rest:
            if ch == "=" and depth == 0:
                has_default = True
                break
            depth = max(0, depth + is_depth(ch, prev))
            prev = ch
        order.append(name)
        if not has_default:
            required.add(name)
    return order, required


def kotlin_files() -> list[str]:
    return sorted(glob.glob(os.path.join(SOURCES, "**", "*.kt"), recursive=True))


def main() -> int:
    files = kotlin_files()
    clean = {f: strip_noise(open(f, encoding="utf-8").read()) for f in files}
    errors: list[str] = []

    # --- сигнатуры всех функций проекта (с учётом одноимённых) ---------
    # Ключ — (файл, имя): у одноимённых функций в разных экранах разные
    # параметры, и искать соответствующую надо сначала в своём файле.
    signatures: dict[tuple[str, str], list[tuple[list[str], set[str]]]] = {}
    all_signatures: dict[str, list[tuple[list[str], set[str]]]] = {}
    for path in files:
        src = clean[path]
        for match in re.finditer(r"fun\s+(\w+)\s*\(", src):
            params, _ = balanced(src, match.end() - 1)
            if params is None:
                continue
            parsed = parse_params(params)
            signatures.setdefault((path, match.group(1)), []).append(parsed)
            all_signatures.setdefault(match.group(1), []).append(parsed)

    # --- верхнеуровневые объявления (для проверки внутренних импортов) --
    declared: set[str] = set()
    for path in files:
        src = clean[path]
        package = re.search(r"^package\s+([\w.]+)", src, re.M)
        prefix = package.group(1) if package else ""
        for match in re.finditer(
            r"^(?:@\w+\s+)*(?:private\s+|internal\s+|public\s+)?"
            r"(?:data\s+|enum\s+|sealed\s+|value\s+|abstract\s+|open\s+|annotation\s+)*"
            r"(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?([\w.`]+)",
            src,
            re.M,
        ):
            name = match.group(1).strip("`").split(".")[-1]
            declared.add(f"{prefix}.{name}")
    declared.add("com.chinesegames.app.R")  # генерируется Android Gradle Plugin

    for path in files:
        src = clean[path]
        rel = os.path.relpath(path, ROOT)
        line_no = lambda pos: src[:pos].count("\n") + 1  # noqa: E731
        imports = set(re.findall(r"^import\s+([\w.]+)$", src, re.M))

        # 1. голые имена иконок
        icon_names = {m.group(1) for m in ICON_IMPORT.finditer(src)} - {"Icons"}
        for name in icon_names:
            for match in re.finditer(r"(?<![\w.$])" + re.escape(name) + r"\b", src):
                line = src.splitlines()[line_no(match.start()) - 1].strip()
                if line.startswith("import "):
                    continue
                errors.append(
                    f"{rel}:{line_no(match.start())} голое имя иконки «{name}» — "
                    f"нужно Icons.AutoMirrored.Filled.{name} / Icons.Filled.{name}"
                )

        # 2. иконки без импорта
        for match in ICON_USE.finditer(src):
            style, name = match.group(1), match.group(2)
            folder = ICON_STYLES[style.replace("AutoMirrored.", "")]
            prefix = "automirrored." if style.startswith("AutoMirrored") else ""
            expected = f"androidx.compose.material.icons.{prefix}{folder}.{name}"
            if expected not in imports:
                errors.append(
                    f"{rel}:{line_no(match.start())} {style}.{name} — нет импорта {expected}"
                )

        # 4. внутренние импорты
        for match in INTERNAL_IMPORT.finditer(src):
            if match.group(1) not in declared:
                errors.append(f"{rel}:{line_no(match.start())} импорт ни на что не указывает: {match.group(1)}")

        # 3/5. вызовы функций проекта
        for match in re.finditer(r"(?<![\w.])(\w+)\s*\(", src):
            name = match.group(1)
            # одноимённые функции в своём файле важнее остальных
            candidates = signatures.get((path, name)) or all_signatures.get(name)
            if not candidates:
                continue
            line = src.splitlines()[line_no(match.start()) - 1]
            if re.search(r"\bfun\s*$", line.split(name)[0]):
                continue  # это объявление, а не вызов
            body, end = balanced(src, match.end() - 1)
            if body is None:
                continue
            args = [a for a in split_top_level(body) if a.strip()]
            named = [re.match(r"^\s*(\w+)\s*=(?!=)", a).group(1) for a in args if re.match(r"^\s*(\w+)\s*=(?!=)", a)]
            if not named:
                continue
            positional = len(args) - len(named)
            trailing = 0
            j = end + 1
            while j < len(src) and src[j] in " \n\t":
                j += 1
            if j < len(src) and src[j] == "{":
                trailing = 1

            def fits(order: list[str], required: set[str]) -> bool:
                if any(n not in order for n in named):
                    return False
                if positional + trailing > len(order):
                    return False
                # именованный аргумент не может дублировать позиционный
                if any(n in order[:positional] for n in named):
                    return False
                covered = set(order[:positional]) | set(named)
                if trailing:
                    # лямбда вне скобок достаётся последнему непокрытому параметру
                    for param in reversed(order):
                        if param not in covered:
                            covered.add(param)
                            break
                return required <= covered

            if any(fits(order, required) for order, required in candidates):
                continue
            known = sorted({p for order, _ in candidates for p in order})
            unknown = [n for n in named if n not in known]
            if unknown:
                errors.append(
                    f"{rel}:{line_no(match.start())} {name}: неизвестные аргументы {unknown} "
                    f"(в сигнатуре: {known})"
                )
            else:
                missing = sorted({p for _, req in candidates for p in req} - set(named))
                errors.append(
                    f"{rel}:{line_no(match.start())} {name}: не переданы обязательные {missing} "
                    f"(позиционных аргументов вместе с лямбдой: {positional + trailing})"
                )

    if errors:
        print(f"Найдено проблем: {len(errors)}")
        for error in errors:
            print("  " + error)
        return 1
    print(f"Проверено файлов: {len(files)} — статических ошибок не найдено ✔")
    return 0


if __name__ == "__main__":
    sys.exit(main())
