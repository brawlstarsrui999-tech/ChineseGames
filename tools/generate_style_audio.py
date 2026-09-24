#!/usr/bin/env python3
"""Генератор музыки премиальных стилевых наборов ChineseGames.

* ``bgm_china.ogg`` — тихая традиционная тема: гучжэн, бамбуковая флейта,
  деревянный барабанчик и мягкий бурдон.
* ``bgm_clover.ogg`` — воздушная розовая тема Клевер-стиля: арфа, колокольчики
  и лёгкий шорох листьев клевера.

Оба трека бесшовно зациклены и сведены тише озвучки.
Запуск: ``python3 tools/generate_style_audio.py``.
"""

import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from generate_music import OUT_DIR, SR, TARGET_PEAK, add, note_freq, pad, pluck, save_ogg, soft_clip, t_of  # noqa: E402

PENTATONIC = [0, 2, 4, 7, 9]


def flute(freq: float, duration: float, amp: float = 0.16) -> np.ndarray:
    """Мягкая бамбуковая флейта с очень лёгким вибрато."""
    t = t_of(duration)
    vib = 1.0 + 0.005 * np.sin(2 * np.pi * 5.1 * t)
    signal = np.sin(2 * np.pi * freq * t * vib)
    signal += 0.32 * np.sin(2 * np.pi * freq * 2 * t * vib)
    signal += 0.10 * np.sin(2 * np.pi * freq * 3 * t * vib)
    attack = np.clip(t / 0.08, 0, 1) ** 1.6
    release = np.clip((duration - t) / 0.18, 0, 1)
    return signal * attack * release * amp


def wood_block(amp: float = 0.12) -> np.ndarray:
    duration = 0.12
    t = t_of(duration)
    return (
        np.sin(2 * np.pi * 820 * t) * np.exp(-t * 60) +
        0.5 * np.sin(2 * np.pi * 1230 * t) * np.exp(-t * 90)
    ) * amp


def bell(freq: float, duration: float, amp: float) -> np.ndarray:
    """Нежный стеклянный колокольчик для листьев клевера."""
    t = t_of(duration)
    signal = np.sin(2 * np.pi * freq * t)
    signal += 0.48 * np.sin(2 * np.pi * freq * 2.76 * t)
    signal += 0.16 * np.sin(2 * np.pi * freq * 4.08 * t)
    env = np.exp(-t * 3.8) * np.clip(t / 0.012, 0, 1)
    return signal * env * amp


def harp(freq: float, duration: float, amp: float) -> np.ndarray:
    """Тёплый арфовый щипок: менее резкий, чем стандартный гучжэн."""
    return pluck(freq, duration, amp=amp, bright=0.55)


def finish(buffer: np.ndarray, peak: float, crossfade: float = 0.5) -> np.ndarray:
    buffer -= buffer.mean()
    buffer = soft_clip(buffer / (np.abs(buffer).max() + 1e-9) * 1.45)
    buffer *= peak / (np.abs(buffer).max() + 1e-9)
    xf = int(crossfade * SR)
    ramp = np.linspace(0.0, 1.0, xf)
    buffer[:xf] = buffer[:xf] * ramp + buffer[-xf:] * (1.0 - ramp)
    return buffer[:-xf]


def make_china(bars: int = 24, bpm: float = 58.0, seed: int = 8888) -> np.ndarray:
    rng = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar = beat * 4.0
    buffer = np.zeros(int(bar * bars * SR))
    root = -7

    for index in range(0, bars, 4):
        start = int(index * bar * SR)
        add(buffer, pad(note_freq(root - 24), bar * 4.05, amp=0.13), start)
        add(buffer, pad(note_freq(root - 17), bar * 4.05, amp=0.08), start)

    phrase = [0, 2, 4, 2, 0, -3, 0, 2, 4, 7, 4, 2, 0, -3, -5, 0]
    step = 0
    for index in range(bars):
        for beat_part in (0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5):
            degree = phrase[step % len(phrase)]
            step += 1
            if rng.random() < 0.27:
                continue
            start = int((index * bar + beat_part * beat) * SR)
            add(buffer, harp(note_freq(root + degree), 1.8, 0.18 * (0.7 + 0.3 * rng.random())), start)

    for index, bar_index in enumerate(range(2, bars, 3)):
        degree = [7, 9, 7, 4, 2, 4, 0, -3][index % 8]
        add(buffer, flute(note_freq(root + degree + 12), beat * 3.2, 0.09), int((bar_index * bar + beat * 0.5) * SR))

    for index in range(bars):
        for beat_part in (1.0, 3.0):
            if rng.random() < 0.5:
                add(buffer, wood_block(0.085), int((index * bar + beat_part * beat) * SR))
    return finish(buffer, TARGET_PEAK, 0.6)


def make_clover(bars: int = 20, bpm: float = 72.0, seed: int = 404) -> np.ndarray:
    """Светлая воздушная тема: розовый сад и медленно падающие листья."""
    rng = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar = beat * 4.0
    buffer = np.zeros(int(bar * bars * SR))
    root = -5  # E4, ласковая мажорная окраска
    chords = ([0, 4, 7], [5, 9, 12], [2, 7, 9], [4, 7, 11])

    for index in range(0, bars, 2):
        chord = chords[(index // 2) % len(chords)]
        start = int(index * bar * SR)
        for degree in chord:
            add(buffer, pad(note_freq(root + degree - 24), bar * 2.05, amp=0.085, detune=0.25), start)

    melody = [7, 9, 12, 9, 7, 4, 7, 9, 14, 12, 9, 7, 4, 2, 4, 7]
    note_index = 0
    for index in range(bars):
        for beat_part in (0.0, 0.75, 1.5, 2.25, 3.0):
            if rng.random() < 0.12:
                continue
            degree = melody[note_index % len(melody)]
            note_index += 1
            start = int((index * bar + beat_part * beat) * SR)
            add(buffer, harp(note_freq(root + degree), 1.5, 0.14), start)
            if rng.random() < 0.35:
                add(buffer, bell(note_freq(root + degree + 12), 1.1, 0.045), start + int(0.04 * SR))

    # Случайные редкие «капельки света» — как падающие листья клевера.
    for index in range(bars * 2):
        start = int(rng.uniform(0, bars * bar) * SR)
        degree = int(rng.choice(PENTATONIC)) + 12
        add(buffer, bell(note_freq(root + degree), 1.8, 0.035), start)
    return finish(buffer, 0.27, 0.55)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, maker in (("bgm_china.ogg", make_china), ("bgm_clover.ogg", make_clover)):
        signal = maker().astype(np.float32)
        path = os.path.join(OUT_DIR, name)
        save_ogg(path, signal)
        print(f"{name}: {len(signal) / SR:.1f} s")


if __name__ == "__main__":
    main()
