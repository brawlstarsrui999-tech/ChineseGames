#!/usr/bin/env python3
"""Нежные синтезированные звериные звуки Кловерушки.

Это не человеческая речь: короткие мурр-переливы, трели и радостные
«мяф»-чирпы. Текстовые реплики остаются в пузыре рядом с талисманом,
а звук делает её именно милым зверьком.

Запуск: python3 tools/generate_cloverushka_audio.py
Требуются numpy, soundfile и (для наиболее надёжной записи OGG) imageio-ffmpeg.
"""

import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from generate_music import OUT_DIR, SR, save_ogg, t_of  # noqa: E402


def normalize(signal: np.ndarray, peak: float = 0.58) -> np.ndarray:
    return signal.astype(np.float32) * (peak / (np.abs(signal).max() + 1e-9))


def chirp(start: float, end: float, duration: float, amp: float = 0.35, wobble: float = 0.0) -> np.ndarray:
    """Мягкий высокий «мяф»: волна с несколькими тёплыми обертонами."""
    t = t_of(duration)
    freq = start + (end - start) * (t / duration) + wobble * np.sin(2 * np.pi * 7.4 * t)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    sound = np.sin(phase) + 0.35 * np.sin(2 * phase) + 0.10 * np.sin(3 * phase)
    attack = np.clip(t / 0.025, 0, 1) ** 1.6
    release = np.clip((duration - t) / 0.08, 0, 1) ** 1.8
    return sound * attack * release * amp


def purr(duration: float, amp: float = 0.18) -> np.ndarray:
    """Тёплое мурчание: низкий тон, негромкое дрожание и мягкий шум."""
    t = t_of(duration)
    lfo = 1.0 + 0.08 * np.sin(2 * np.pi * 24.0 * t)
    low = np.sin(2 * np.pi * 54 * t) + 0.45 * np.sin(2 * np.pi * 108 * t)
    high = 0.20 * np.sin(2 * np.pi * 216 * t)
    rng = np.random.default_rng(332)
    noise = rng.normal(0.0, 1.0, len(t))
    noise = np.convolve(noise, np.ones(65) / 65.0, mode="same")
    env = np.clip(t / 0.08, 0, 1) * np.clip((duration - t) / 0.12, 0, 1)
    return (low + high + 0.22 * noise) * lfo * env * amp


def place(buf: np.ndarray, signal: np.ndarray, at: float) -> None:
    start = int(at * SR)
    end = min(len(buf), start + len(signal))
    if end > start:
        buf[start:end] += signal[: end - start]


def happy() -> np.ndarray:
    out = np.zeros(int(0.78 * SR))
    place(out, chirp(550, 920, 0.17, 0.30, 18), 0.03)
    place(out, chirp(760, 1260, 0.22, 0.34, 23), 0.23)
    place(out, purr(0.34, 0.12), 0.36)
    return normalize(out)


def proud() -> np.ndarray:
    out = np.zeros(int(0.95 * SR))
    place(out, purr(0.55, 0.18), 0.00)
    place(out, chirp(630, 1120, 0.24, 0.28, 15), 0.50)
    return normalize(out, 0.52)


def record() -> np.ndarray:
    out = np.zeros(int(1.0 * SR))
    for at, f in ((0.02, 700), (0.17, 900), (0.32, 1170), (0.50, 1420)):
        place(out, chirp(f, f * 1.12, 0.23, 0.24, 12), at)
    return normalize(out, 0.6)


def comfort() -> np.ndarray:
    out = np.zeros(int(0.92 * SR))
    place(out, purr(0.78, 0.22), 0.00)
    place(out, chirp(440, 680, 0.24, 0.16, 8), 0.46)
    return normalize(out, 0.46)


def hello() -> np.ndarray:
    out = np.zeros(int(0.62 * SR))
    place(out, chirp(840, 1320, 0.21, 0.31, 27), 0.03)
    place(out, chirp(1020, 850, 0.24, 0.22, 19), 0.28)
    return normalize(out, 0.56)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, make in (
        ("cloverushka_happy.ogg", happy),
        ("cloverushka_proud.ogg", proud),
        ("cloverushka_record.ogg", record),
        ("cloverushka_comfort.ogg", comfort),
        ("cloverushka_hello.ogg", hello),
    ):
        signal = make()
        path = os.path.join(OUT_DIR, name)
        save_ogg(path, signal)
        print(f"{name}: {len(signal) / SR:.2f} s")


if __name__ == "__main__":
    main()
