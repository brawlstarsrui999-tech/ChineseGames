#!/usr/bin/env python3
"""
Генератор фоновой музыки ChineseGames.

Два бесшовно зацикленных трека в китайской пентатонике — очень тихие,
спокойные, без резких звуков (специально сделаны так, чтобы не раздражать):

  * bgm_night.ogg — ночная тема: низкая подушка, редкие щипки гуциня
  * bgm_day.ogg   — дневная тема: музыкальная шкатулка, светлее и подвижнее

Запуск:  python3 tools/generate_music.py
Нужны только numpy и soundfile.
"""

import os
import shutil
import subprocess
import tempfile

import numpy as np
import soundfile as sf

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "app", "src", "main", "res", "raw"))

# Пик итогового файла. Музыка намеренно тише звуковых эффектов и озвучки,
# а в приложении поверх неё есть ещё и отдельный регулятор громкости.
TARGET_PEAK = 0.30


def t_of(dur: float) -> np.ndarray:
    return np.arange(int(dur * SR)) / SR


def note_freq(semitones_from_a4: float) -> float:
    return 440.0 * (2.0 ** (semitones_from_a4 / 12.0))


# Китайская пентатоника (по полутонам от A4) — без полутонов, звучит мягко.
PENTATONIC = [0, 2, 4, 7, 9]


def pluck(freq: float, dur: float, amp: float = 0.5, bright: float = 1.0) -> np.ndarray:
    """Щипок гуциня/музыкальной шкатулки: несколько партиалов с разным затуханием."""
    t = t_of(dur)
    partials = [
        (1.00, 1.00, 1.0),
        (2.00, 0.38 * bright, 1.9),
        (3.01, 0.17 * bright, 2.6),
        (4.03, 0.08 * bright, 3.4),
        (5.42, 0.04 * bright, 4.4),
    ]
    sig = np.zeros(len(t))
    for ratio, p_amp, p_dec in partials:
        sig += p_amp * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t * p_dec / max(dur, 0.05)) * 3.2

    attack = np.clip(t / 0.006, 0, 1)
    decay = np.exp(-t * (2.2 / max(dur, 0.05)))
    body = attack * decay
    fade = min(int(0.02 * SR), len(body) // 2)
    if fade > 2:
        body[-fade:] *= np.linspace(1.0, 0.0, fade)
    return sig * body * amp


def pad(freq: float, dur: float, amp: float = 0.2, detune: float = 0.6) -> np.ndarray:
    """Мягкая «подушка»: почти синус с медленным входом и выходом (loop-safe)."""
    t = t_of(dur)
    vib = 1.0 + 0.0016 * np.sin(2 * np.pi * 0.18 * t)
    sig = np.zeros(len(t))
    for k, w in ((0.0, 1.0), (detune, 0.6), (-detune, 0.6)):
        sig += w * np.sin(2 * np.pi * (freq + k) * t * vib)
    sig += 0.10 * np.sin(2 * np.pi * freq * 2 * t)
    fade_len = int(2.6 * SR)
    env = np.ones(len(t))
    fade_len = min(fade_len, len(t) // 2)
    env[:fade_len] = np.linspace(0.0, 1.0, fade_len) ** 1.4
    env[-fade_len:] *= np.linspace(1.0, 0.0, fade_len) ** 1.4
    return sig * env * amp


def add(buf: np.ndarray, sig: np.ndarray, start: int, loop: bool = True) -> None:
    """Складывает сигнал в буфер, при loop=True «заворачивает» хвост в начало."""
    n = len(buf)
    if not loop:
        end = min(n, start + len(sig))
        if end > start:
            buf[start:end] += sig[: end - start]
        return
    idx = (np.arange(len(sig)) + start) % n
    np.add.at(buf, idx, sig)


def soft_clip(x: np.ndarray) -> np.ndarray:
    return np.tanh(x * 1.15) / np.tanh(1.15)


def make_track(seed: int, bars: int, bpm: float, day: bool) -> np.ndarray:
    rnd = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar_len = beat * 4.0
    total = bar_len * bars
    buf = np.zeros(int(total * SR))

    # --- Гармоническая сетка: медленные аккорды по 2 такта ---------------------
    # Ступени пентатоники, взятые широко и низко — «ветер в бамбуковой роще».
    progressions = [
        [0, 4, 7],
        [2, 5, 9],
        [0, 3, 7],
        [4, 7, 11],
    ]
    pad_octave = -24 if not day else -21
    for bar in range(0, bars, 2):
        chord = progressions[(bar // 2) % len(progressions)]
        start = int(bar * bar_len * SR)
        for i, step in enumerate(chord):
            f = note_freq(pad_octave + step)
            add(buf, pad(f, bar_len * 2.0 * 1.02, amp=0.16 if day else 0.20), start)

    # --- Мелодия: редкие ноты по сетке ---------------------------------------
    melody_octave = -9 if day else -14
    step_grid = [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5] if day else [0.0, 1.5, 2.0, 3.5]
    density = 0.72 if day else 0.42
    for bar in range(bars):
        chord_root = progressions[(bar // 2) % len(progressions)][0]
        for s in step_grid:
            if rnd.random() > density:
                continue
            deg = int(chord_root + PENTATONIC[rnd.integers(0, len(PENTATONIC))] + 12 * rnd.integers(0, 2))
            octave = melody_octave + (0 if day else -2)
            f = note_freq(octave + deg)
            dur = 1.1 if day else 2.4
            amp = (0.20 if day else 0.16) * (0.7 + 0.3 * rnd.random())
            start = int((bar * bar_len + s * beat) * SR)
            add(buf, pluck(f, dur, amp=amp, bright=1.15 if day else 0.75), start)

    # --- Мягкий «воздух»: шипящий шум с очень медленной фильтрацией -----------
    noise = rnd.normal(0.0, 1.0, len(buf))
    # Однополюсный ФНЧ (двумя проходами) для глухого шума.
    for _ in range(2):
        noise = np.convolve(noise, np.ones(340) / 340.0, mode="same")
    lfo = 0.5 + 0.5 * np.sin(2 * np.pi * (0.035 if not day else 0.06) * np.arange(len(buf)) / SR)
    buf += noise * lfo * (0.05 if day else 0.07)

    # --- Колокольчики на «сильную долю» каждые 4 такта ------------------------
    for bar in range(0, bars, 4):
        deg = PENTATONIC[(bar // 4) % len(PENTATONIC)]
        f = note_freq((-2 if day else -7) + deg)
        add(buf, pluck(f, 3.0, amp=0.16, bright=1.5), int(bar * bar_len * SR))

    # --- Финальная обработка -------------------------------------------------
    # Убираем постоянную составляющую, мягко ограничиваем и подгоняем пик.
    buf -= buf.mean()
    buf = soft_clip(buf / (np.abs(buf).max() + 1e-9) * 1.6)
    buf *= TARGET_PEAK / (np.abs(buf).max() + 1e-9)

    # Бесшовный стык: хвост «переливается» в начало, буфер обрезается —
    # на стыке петли получается непрерывная волна (без щелчка).
    xf = int(0.6 * SR)
    ramp = np.linspace(0.0, 1.0, xf)
    head = buf[:xf].copy()
    tail = buf[-xf:].copy()
    buf[:xf] = head * ramp + tail * (1.0 - ramp)
    buf = buf[:-xf]
    return buf


def find_ffmpeg() -> str | None:
    """ffmpeg из PATH или бинарник из пакета imageio-ffmpeg (если он установлен)."""
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg  # type: ignore

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None


def save_ogg(path: str, sig: np.ndarray, quality: int = 2) -> None:
    """
    Пишем OGG Vorbis. Для длинных треков libsndfile на некоторых сборках падает,
    поэтому сначала сохраняем WAV и кодируем через ffmpeg; если ffmpeg нет —
    пробуем записать напрямую через soundfile.
    """
    ffmpeg = find_ffmpeg()
    if ffmpeg:
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            wav_path = tmp.name
        try:
            sf.write(wav_path, sig.astype(np.float32), SR, subtype="PCM_16")
            subprocess.run(
                [ffmpeg, "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", str(quality), path],
                check=True,
            )
        finally:
            if os.path.exists(wav_path):
                os.remove(wav_path)
        return
    sf.write(path, sig.astype(np.float32), SR, format="OGG", subtype="VORBIS")


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    tracks = [
        ("bgm_night.ogg", dict(seed=20240501, bars=24, bpm=52.0, day=False)),
        ("bgm_day.ogg", dict(seed=777, bars=24, bpm=66.0, day=True)),
    ]
    for name, kwargs in tracks:
        sig = make_track(**kwargs)
        path = os.path.join(OUT_DIR, name)
        save_ogg(path, sig)
        size_kb = os.path.getsize(path) / 1024
        print(f"{name}: {len(sig) / SR:5.1f} с, {size_kb:6.1f} КБ, пик {np.abs(sig).max():.3f}")


if __name__ == "__main__":
    main()
