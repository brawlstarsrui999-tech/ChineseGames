#!/usr/bin/env python3
"""
Генератор звуковых эффектов ChineseGames.

Все звуки синтезируются прямо здесь (без внешних библиотек и без интернета)
и сохраняются в app/src/main/res/raw/*.ogg.

Запуск:  python3 tools/generate_sounds.py
Нужны только numpy и soundfile.
"""

import os

import numpy as np
import soundfile as sf

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "app", "src", "main", "res", "raw"))


def t_of(dur: float) -> np.ndarray:
    return np.arange(int(dur * SR)) / SR


def env(dur: float, attack: float = 0.004, tau: float = 0.15) -> np.ndarray:
    """Экспоненциальная огибающая с плавным входом и без щелчка в конце."""
    t = t_of(dur)
    e = np.clip(t / max(attack, 1e-6), 0, 1) * np.exp(-t / max(tau, 1e-6))
    fade = min(int(0.005 * SR), len(e) // 3)
    if fade > 2:
        e[-fade:] *= np.linspace(1.0, 0.0, fade)
    return e


def sine(freq: float, dur: float, phase: float = 0.0) -> np.ndarray:
    return np.sin(2 * np.pi * freq * t_of(dur) + phase)


def sweep(f0: float, f1: float, dur: float) -> np.ndarray:
    """Экспоненциальный свип по частоте."""
    t = t_of(dur)
    k = (f1 / f0) ** (t / dur)
    phase = 2 * np.pi * np.cumsum(f0 * k) / SR
    return np.sin(phase)


def bell(freq: float, dur: float, decay: float = 1.0, amp: float = 1.0) -> np.ndarray:
    """Колокольчик/маримба: неармонические партиалы с разным затуханием."""
    t = t_of(dur)
    partials = [
        (1.00, 1.00, 1.0),
        (2.01, 0.45, 1.7),
        (3.02, 0.22, 2.4),
        (4.18, 0.11, 3.2),
        (5.44, 0.06, 4.0),
        (7.10, 0.03, 5.2),
    ]
    sig = np.zeros(len(t))
    for ratio, p_amp, p_dec in partials:
        sig += p_amp * np.exp(-t * p_dec * decay) * np.sin(2 * np.pi * freq * ratio * t)
    sig *= np.clip(t / 0.003, 0, 1)  # мягкая атака
    fade = min(int(0.006 * SR), len(sig) // 3)
    if fade > 2:
        sig[-fade:] *= np.linspace(1.0, 0.0, fade)
    return sig * amp


def noise(dur: float, seed: int = 0) -> np.ndarray:
    rng = np.random.default_rng(seed)
    return rng.uniform(-1.0, 1.0, int(dur * SR))


def lowpass(sig: np.ndarray, cutoff: float) -> np.ndarray:
    """Простой однополюсный ФНЧ."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    out = np.zeros_like(sig)
    prev = 0.0
    for i, x in enumerate(sig):
        prev = (1 - a) * x + a * prev
        out[i] = prev
    return out


def place(base: np.ndarray, sig: np.ndarray, at: float) -> None:
    i = int(at * SR)
    n = min(len(sig), len(base) - i)
    if n > 0:
        base[i:i + n] += sig[:n]


def mix(*sigs: np.ndarray) -> np.ndarray:
    """Складывает сигналы разной длины, выравнивая их по началу."""
    n = max(len(s) for s in sigs)
    out = np.zeros(n)
    for s in sigs:
        out[:len(s)] += s
    return out


def normalize(sig: np.ndarray, peak: float) -> np.ndarray:
    m = float(np.max(np.abs(sig))) or 1.0
    out = sig / m * peak
    return np.tanh(out * 1.15) / np.tanh(1.15)  # мягкое ограничение


# --------------------------------------------------------------------------- #
#                              Сами звуки                                      #
# --------------------------------------------------------------------------- #

def sfx_click() -> np.ndarray:
    """Короткий мягкий щелчок интерфейса."""
    dur = 0.06
    s = mix(
        sine(1180, dur) * env(dur, 0.001, 0.012),
        0.35 * sine(2360, dur) * env(dur, 0.001, 0.008),
    )
    return normalize(s, 0.35)


def sfx_flip() -> np.ndarray:
    """Переворот карточки — короткий «клац» со свипом."""
    dur = 0.14
    body = sweep(420, 980, dur) * env(dur, 0.002, 0.045)
    click = noise(0.03, seed=3) * env(0.03, 0.001, 0.006)
    click = lowpass(click, 4200)
    s = mix(body, 0.5 * click)
    return normalize(s, 0.5)


def sfx_match() -> np.ndarray:
    """Пара найдена — тёплый двойной колокольчик C6 → E6."""
    dur = 0.75
    s = np.zeros(int(dur * SR))
    place(s, bell(1046.5, 0.55, decay=1.5), 0.0)          # C6
    place(s, bell(1318.5, 0.6, decay=1.6) * 0.85, 0.07)   # E6
    place(s, 0.12 * sine(2093.0, 0.4) * env(0.4, 0.002, 0.09), 0.05)  # блеск
    return normalize(s, 0.6)


def sfx_combo() -> np.ndarray:
    """Комбо — восходящее арпеджио."""
    dur = 0.95
    s = np.zeros(int(dur * SR))
    for i, f in enumerate([1046.5, 1318.5, 1568.0, 2093.0]):
        place(s, bell(f, 0.5, decay=1.8) * (1.0 - i * 0.12), 0.055 * i)
    return normalize(s, 0.6)


def sfx_error() -> np.ndarray:
    """Ошибка — мягкий низкий «блуп», без резкости."""
    dur = 0.34
    body = sweep(240, 140, dur) * env(dur, 0.003, 0.1)
    sub = sine(90, dur) * env(dur, 0.005, 0.11) * 0.6
    s = mix(body, sub)
    return normalize(s, 0.5)


def sfx_star() -> np.ndarray:
    """Звёздочка на экране победы."""
    dur = 0.45
    s = mix(bell(2637.0, 0.4, decay=2.4) * 0.9, bell(3520.0, 0.3, decay=3.0) * 0.35)
    return normalize(s, 0.5)


def sfx_win() -> np.ndarray:
    """Победная фанфара C5-E5-G5-C6 + искристый хвост."""
    dur = 2.1
    s = np.zeros(int(dur * SR))
    notes = [523.25, 659.25, 783.99, 1046.50]
    for i, f in enumerate(notes):
        place(s, bell(f, 1.2, decay=1.2) * (0.75 + 0.1 * i), i * 0.125)
    place(s, bell(1318.5, 1.4, decay=1.1) * 0.8, 0.55)
    for i in range(7):  # искры
        place(s, bell(2093.0 * (1 + i * 0.09), 0.4, decay=3.2) * 0.16, 0.62 + i * 0.07)
    return normalize(s, 0.8)


def sfx_whoosh() -> np.ndarray:
    """Переход между экранами — воздушный свист."""
    dur = 0.32
    n = noise(dur, seed=11)
    body = lowpass(n, 1800) * env(dur, 0.03, 0.13)
    tone = sweep(180, 900, dur) * env(dur, 0.03, 0.1) * 0.5
    return normalize(mix(body, tone), 0.38)


def sfx_pop() -> np.ndarray:
    """Лопнувший пузырь — короткий «поп» с булькающим хвостом (Bubble pop)."""
    dur = 0.22
    blip = sweep(880, 300, 0.1) * env(0.1, 0.001, 0.03)
    bubble = sweep(320, 720, 0.16) * env(0.16, 0.004, 0.06) * 0.55
    s = np.zeros(int(dur * SR))
    place(s, blip, 0.0)
    place(s, bubble, 0.05)
    return normalize(s, 0.55)


SOUNDS = {
    "click": sfx_click,
    "flip": sfx_flip,
    "match": sfx_match,
    "combo": sfx_combo,
    "error": sfx_error,
    "star": sfx_star,
    "win": sfx_win,
    "whoosh": sfx_whoosh,
    "pop": sfx_pop,
}


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, factory in SOUNDS.items():
        samples = np.clip(factory(), -1.0, 1.0).astype(np.float32)
        path = os.path.join(OUT_DIR, f"{name}.ogg")
        sf.write(path, samples, SR, format="OGG", subtype="VORBIS")
        print(f"{name:8s} -> {path}  ({len(samples) / SR:.2f} c, {os.path.getsize(path)} байт)")


if __name__ == "__main__":
    main()
