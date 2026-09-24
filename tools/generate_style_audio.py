#!/usr/bin/env python3
"""
Генератор аудио для стилевых наборов ChineseGames (магазин украшений).

  * bgm_china.ogg  — «Китайский дракон»: тихая традиционная музыка,
                     гучжэн (щипки и глиссандо), бамбуковая флейта, деревянный
                     барабанчик и низкий бурдон. Пентатоника, 58 BPM.
  * bgm_anime.ogg  — «Аниме»: энергичная чиптюн-тема (квадратный лид, арпеджио,
                     треугольный бас, лёгкая ударка), 128 BPM.
  * anime_pop.ogg / anime_kira.ogg / anime_don.ogg / anime_yay.ogg /
    anime_combo.ogg — аниме-звуки для игр (щелчок, блеск, удар, ура, комбо).

Треки бесшовно зациклены и, как и встроенная музыка, сведены тихо —
поверх них в приложении есть регулятор громкости.

Запуск:  python3 tools/generate_style_audio.py
Нужны только numpy и soundfile (см. tools/generate_music.py).
"""

import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from generate_music import (  # noqa: E402
    OUT_DIR,
    SR,
    TARGET_PEAK,
    add,
    note_freq,
    pad,
    pluck,
    save_ogg,
    soft_clip,
    t_of,
)

PENTA = [0, 2, 4, 7, 9]


# ------------------------------- Инструменты ---------------------------------


def flute(freq: float, dur: float, amp: float = 0.18) -> np.ndarray:
    """Бамбуковая флейта (дицзы): мягкий тон с вибрато и лёгким «дыханием»."""
    t = t_of(dur)
    vib = 1.0 + 0.006 * np.sin(2 * np.pi * 5.3 * t) * np.clip((t - 0.15) / 0.3, 0, 1)
    sig = np.sin(2 * np.pi * freq * t * vib)
    sig += 0.35 * np.sin(2 * np.pi * freq * 2 * t * vib)
    sig += 0.12 * np.sin(2 * np.pi * freq * 3 * t * vib)
    rnd = np.random.default_rng(int(freq * 10))
    breath = rnd.normal(0.0, 1.0, len(t))
    breath = np.convolve(breath, np.ones(24) / 24.0, mode="same")
    sig += 0.06 * breath
    attack = np.clip(t / 0.09, 0, 1) ** 1.5
    release = np.clip((dur - t) / 0.18, 0, 1)
    return sig * attack * release * amp


def wood_block(amp: float = 0.25) -> np.ndarray:
    """Деревянный барабанчик (木鱼): короткий глухой щелчок."""
    dur = 0.12
    t = t_of(dur)
    sig = np.sin(2 * np.pi * 820 * t) * np.exp(-t * 60) + 0.5 * np.sin(2 * np.pi * 1230 * t) * np.exp(-t * 90)
    return sig * amp


def gliss(base_deg: int, dur: float, up: bool = True, amp: float = 0.12) -> np.ndarray:
    """Глиссандо гучжэна: быстрый пробег по пентатонике вверх или вниз."""
    steps = [base_deg + PENTA[i % 5] + 12 * (i // 5) for i in range(10)]
    if not up:
        steps = steps[::-1]
    out = np.zeros(int(dur * SR) + SR)
    gap = dur / len(steps)
    for i, deg in enumerate(steps):
        note = pluck(note_freq(deg), 0.9, amp=amp * (0.6 + 0.4 * i / len(steps)), bright=1.1)
        start = int(i * gap * SR)
        out[start:start + len(note)] += note
    return out[: int((dur + 0.9) * SR)]


def square(freq: float, dur: float, amp: float = 0.2, duty: float = 0.5) -> np.ndarray:
    """Квадратный чиптюн-лид с мягкими краями."""
    t = t_of(dur)
    phase = (freq * t) % 1.0
    sig = np.where(phase < duty, 1.0, -1.0)
    sig = np.convolve(sig, np.ones(6) / 6.0, mode="same")
    attack = np.clip(t / 0.008, 0, 1)
    release = np.clip((dur - t) / 0.04, 0, 1)
    decay = np.exp(-t * 1.2)
    return sig * attack * release * (0.55 + 0.45 * decay) * amp


def triangle(freq: float, dur: float, amp: float = 0.25) -> np.ndarray:
    t = t_of(dur)
    sig = 2.0 * np.abs(2.0 * ((freq * t) % 1.0) - 1.0) - 1.0
    attack = np.clip(t / 0.01, 0, 1)
    release = np.clip((dur - t) / 0.05, 0, 1)
    return sig * attack * release * amp


def saw_arp(freq: float, dur: float, amp: float = 0.12) -> np.ndarray:
    t = t_of(dur)
    sig = 2.0 * ((freq * t) % 1.0) - 1.0
    sig = np.convolve(sig, np.ones(10) / 10.0, mode="same")
    env = np.exp(-t * 9.0)
    return sig * env * amp


def kick(amp: float = 0.5) -> np.ndarray:
    dur = 0.22
    t = t_of(dur)
    f = 130 * np.exp(-t * 18) + 45
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 16)
    return sig * amp


def hat(amp: float = 0.12, seed: int = 0, dur: float = 0.05) -> np.ndarray:
    rnd = np.random.default_rng(seed)
    t = t_of(dur)
    sig = rnd.normal(0.0, 1.0, len(t))
    sig = sig - np.convolve(sig, np.ones(8) / 8.0, mode="same")  # убираем низ
    return sig * np.exp(-t * 90) * amp


def finish(buf: np.ndarray, peak: float, crossfade: float = 0.5) -> np.ndarray:
    """Убираем DC, мягко ограничиваем, подгоняем пик и делаем бесшовную петлю."""
    buf = buf - buf.mean()
    buf = soft_clip(buf / (np.abs(buf).max() + 1e-9) * 1.5)
    buf *= peak / (np.abs(buf).max() + 1e-9)
    xf = int(crossfade * SR)
    ramp = np.linspace(0.0, 1.0, xf)
    head = buf[:xf].copy()
    tail = buf[-xf:].copy()
    buf[:xf] = head * ramp + tail * (1.0 - ramp)
    return buf[:-xf]


# --------------------------------- Треки -------------------------------------


def make_china(bars: int = 24, bpm: float = 58.0, seed: int = 8888) -> np.ndarray:
    rnd = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar_len = beat * 4.0
    buf = np.zeros(int(bar_len * bars * SR))

    root = -7  # D4 относительно A4 — светлый «гун»-лад

    # Низкий бурдон: тоника и квинта, очень тихо.
    for bar in range(0, bars, 4):
        start = int(bar * bar_len * SR)
        add(buf, pad(note_freq(root - 24), bar_len * 4.05, amp=0.13), start)
        add(buf, pad(note_freq(root - 17), bar_len * 4.05, amp=0.08), start)

    # Гучжэн: мелодия из пентатоники с частыми повторами (как народная тема).
    phrase = [0, 2, 4, 2, 0, -3, 0, 2, 4, 7, 4, 2, 0, -3, -5, 0]
    grid = [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5]
    idx = 0
    for bar in range(bars):
        for s in grid:
            if rnd.random() < 0.28:
                idx += 1
                continue
            deg = phrase[idx % len(phrase)]
            idx += 1
            f = note_freq(root + deg)
            amp = 0.20 * (0.7 + 0.3 * rnd.random())
            start = int((bar * bar_len + s * beat) * SR)
            add(buf, pluck(f, 1.8, amp=amp, bright=0.95), start)
            # Иногда — октавное удвоение, как на гучжэне.
            if rnd.random() < 0.18:
                add(buf, pluck(f * 2, 1.2, amp=amp * 0.35, bright=0.8), start)

    # Глиссандо в конце каждой второй фразы (каждые 8 тактов).
    for bar in range(7, bars, 8):
        start = int((bar * bar_len + 2.5 * beat) * SR)
        add(buf, gliss(root - 12, 1.1, up=True, amp=0.10), start)

    # Флейта: длинные ноты поверх, с паузами.
    flute_line = [7, 9, 7, 4, 2, 4, 0, -3]
    for i, bar in enumerate(range(2, bars, 3)):
        deg = flute_line[i % len(flute_line)]
        start = int((bar * bar_len + 0.5 * beat) * SR)
        add(buf, flute(note_freq(root + deg + 12), beat * 3.2, amp=0.09), start)

    # Деревянный барабанчик на 2 и 4 долю — редкий, ненавязчивый.
    for bar in range(bars):
        for s in (1.0, 3.0):
            if rnd.random() < 0.55:
                start = int((bar * bar_len + s * beat) * SR)
                add(buf, wood_block(amp=0.09), start)

    # Храмовый колокол раз в 8 тактов.
    for bar in range(0, bars, 8):
        add(buf, pluck(note_freq(root - 12), 4.0, amp=0.12, bright=1.6), int(bar * bar_len * SR))

    return finish(buf, TARGET_PEAK, crossfade=0.6)


def make_anime(bars: int = 16, bpm: float = 128.0, seed: int = 4242) -> np.ndarray:
    rnd = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar_len = beat * 4.0
    buf = np.zeros(int(bar_len * bars * SR))

    root = -9  # C4
    # I – V – vi – IV (мажор): классический «опенинг».
    chords = [
        [0, 4, 7],
        [7, 11, 14],
        [9, 12, 16],
        [5, 9, 12],
    ]
    major = [0, 2, 4, 5, 7, 9, 11, 12, 14, 16]

    # Лид: две повторяющиеся двухтактовые фразы с вариациями.
    motif_a = [(0.0, 7, 0.5), (0.5, 9, 0.5), (1.0, 12, 1.0), (2.0, 11, 0.5), (2.5, 9, 0.5), (3.0, 7, 1.0)]
    motif_b = [(0.0, 4, 0.5), (0.5, 5, 0.5), (1.0, 7, 0.5), (1.5, 9, 0.5), (2.0, 12, 1.5), (3.5, 14, 0.5)]
    for bar in range(bars):
        motif = motif_a if (bar // 2) % 2 == 0 else motif_b
        transpose = 0 if bar % 8 < 4 else (2 if bar % 2 == 0 else 0)
        for s, deg, dur in motif:
            if bar % 2 == 1 and rnd.random() < 0.25:
                deg = major[min(len(major) - 1, major.index(deg) + 1)] if deg in major else deg
            f = note_freq(root + deg + transpose + 12)
            start = int((bar * bar_len + s * beat) * SR)
            add(buf, square(f, dur * beat * 0.95, amp=0.16, duty=0.35), start)

    # Арпеджио на аккордах — шестнадцатыми.
    for bar in range(bars):
        chord = chords[bar % len(chords)]
        for k in range(16):
            deg = chord[k % 3] + (12 if k % 6 >= 3 else 0)
            f = note_freq(root + deg)
            start = int((bar * bar_len + k * beat / 4) * SR)
            add(buf, saw_arp(f, beat / 4 * 0.9, amp=0.07), start)

    # Бас — восьмыми, октавой ниже, с ходом на квинту.
    for bar in range(bars):
        chord_root = chords[bar % len(chords)][0]
        for k in range(8):
            deg = chord_root - 24 + (7 if k in (3, 7) else 0)
            f = note_freq(root + deg)
            start = int((bar * bar_len + k * beat / 2) * SR)
            add(buf, triangle(f, beat / 2 * 0.85, amp=0.22), start)

    # Ударные: бочка на каждую долю, хэт восьмыми, «хлопок» на 2 и 4.
    for bar in range(bars):
        for k in range(4):
            add(buf, kick(amp=0.35), int((bar * bar_len + k * beat) * SR))
        for k in range(8):
            add(buf, hat(amp=0.07 if k % 2 == 0 else 0.045, seed=bar * 8 + k), int((bar * bar_len + k * beat / 2) * SR))
        for k in (1, 3):
            add(buf, hat(amp=0.16, seed=100 + bar * 4 + k, dur=0.12), int((bar * bar_len + k * beat) * SR))

    return finish(buf, 0.32, crossfade=0.25)


# ------------------------------ Аниме-звуки ---------------------------------


def _env(dur: float, attack: float = 0.006, tau: float = 0.12) -> np.ndarray:
    t = t_of(dur)
    return np.clip(t / attack, 0, 1) * np.exp(-t / tau) * np.clip((dur - t) / 0.01, 0, 1)


def _norm(sig: np.ndarray, peak: float) -> np.ndarray:
    return sig * (peak / (np.abs(sig).max() + 1e-9))


def sfx_pop() -> np.ndarray:
    """«Пойо»: коротенький мультяшный чирп для щелчков."""
    dur = 0.11
    t = t_of(dur)
    f = 620 + 420 * np.clip(t / 0.05, 0, 1)
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * np.sin(2 * np.pi * np.cumsum(f * 2) / SR)
    return _norm(sig * _env(dur, tau=0.05), 0.6)


def sfx_kira() -> np.ndarray:
    """«Кира-кира»: блеск — быстрый восходящий перезвон высоких нот."""
    dur = 0.55
    out = np.zeros(int(dur * SR))
    for i, f in enumerate((1568, 2093, 2637, 3136, 3951)):
        n = int(0.32 * SR)
        t = t_of(0.32)
        note = (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 1.5 * t)) * _env(0.32, tau=0.09)
        start = int(i * 0.045 * SR)
        out[start:start + n] += note * (0.8 + 0.2 * i / 5)
    return _norm(out, 0.55)


def sfx_don() -> np.ndarray:
    """«Дон!»: драматичный аниме-удар для ошибки."""
    dur = 0.4
    t = t_of(dur)
    f = 150 * np.exp(-t * 9) + 48
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7)
    rnd = np.random.default_rng(3)
    noise = rnd.normal(0.0, 1.0, len(t))
    noise = np.convolve(noise, np.ones(40) / 40.0, mode="same") * np.exp(-t * 22)
    return _norm(body + 0.5 * noise, 0.7)


def sfx_yay() -> np.ndarray:
    """«Ятта!»: победный перезвон вверх по мажорному трезвучию."""
    dur = 1.3
    out = np.zeros(int(dur * SR))
    seq = [(0.0, 523), (0.11, 659), (0.22, 784), (0.33, 1047), (0.55, 784), (0.66, 1047), (0.77, 1319)]
    for start_s, f in seq:
        n = int(0.5 * SR)
        t = t_of(0.5)
        vib = 1.0 + 0.008 * np.sin(2 * np.pi * 6 * t)
        note = (np.sin(2 * np.pi * f * t * vib) + 0.35 * np.sin(2 * np.pi * 2 * f * t)) * _env(0.5, tau=0.16)
        start = int(start_s * SR)
        out[start:start + n] += note
    return _norm(out, 0.65)


def sfx_combo() -> np.ndarray:
    """Комбо: короткая «кира» с пружинистым подскоком."""
    dur = 0.45
    t = t_of(dur)
    f = 880 + 660 * np.clip(t / 0.12, 0, 1)
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR) * _env(dur, tau=0.14)
    sig += 0.5 * sfx_kira()[: len(sig)] if len(sfx_kira()) >= len(sig) else 0
    return _norm(sig, 0.6)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    items = [
        ("bgm_china.ogg", make_china),
        ("bgm_anime.ogg", make_anime),
        ("anime_pop.ogg", sfx_pop),
        ("anime_kira.ogg", sfx_kira),
        ("anime_don.ogg", sfx_don),
        ("anime_yay.ogg", sfx_yay),
        ("anime_combo.ogg", sfx_combo),
    ]
    for name, fn in items:
        sig = fn().astype(np.float32)
        path = os.path.join(OUT_DIR, name)
        save_ogg(path, sig)
        print(f"{name}: {len(sig) / SR:5.1f} с, {os.path.getsize(path) / 1024:6.1f} КБ, пик {np.abs(sig).max():.3f}")


if __name__ == "__main__":
    main()
