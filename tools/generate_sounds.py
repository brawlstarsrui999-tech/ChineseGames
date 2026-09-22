#!/usr/bin/env python3
"""
Генератор звуковых эффектов ChineseGames — «мягкая» редакция.

Звуки синтезируются прямо здесь (без внешних библиотек-сэмплов и без интернета)
и сохраняются в app/src/main/res/raw/*.ogg.

Запуск:  python3 tools/generate_sounds.py
Нужны только numpy и soundfile.

Что здесь сделано, чтобы эффекты НЕ резали уши (в отличие от первой версии):
  * длинные и плавные атаки (10–30 мс) вместо резкого «щелчка»;
  * только низкие, тёплые тембры: синусы с парой тихих гармоник, никаких
    звонких частичных на 2,6–3,5 кГц;
  * общий фильтр НЧ (~2,2 кГц) + мягкая «тёплая» полочка по верхам — режется
    именно то, что чаще всего и «режет ухо»;
  * шум только сильно сглаженный, без «циканья»;
  * уровень каждого эффекта понижен и приведён к общему мягкому пику (0,22–0,42);
  * у каждого сэмпла — короткий фейд-аут, поэтому нет щелчков на границах.
"""

import os

import numpy as np
import soundfile as sf

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "app", "src", "main", "res", "raw"))

# Общий «тёплый» фильтр: всё, что выше этой частоты, приглушается.
WARM_CUTOFF = 2200.0


def t_of(dur: float) -> np.ndarray:
    return np.arange(int(dur * SR)) / SR


def env(dur: float, attack: float = 0.012, tau: float = 0.2) -> np.ndarray:
    """Плавная огибающая: медленный вход, мягкое затухание, фейд в конце."""
    t = t_of(dur)
    e = (1.0 - np.exp(-t / max(attack, 1e-6))) * np.exp(-t / max(tau, 1e-6))
    fade = min(int(0.012 * SR), max(2, len(e) // 4))
    e[-fade:] *= np.linspace(1.0, 0.0, fade)
    return e


def sine(freq: float, dur: float, phase: float = 0.0) -> np.ndarray:
    return np.sin(2 * np.pi * freq * t_of(dur) + phase)


def soft_tone(freq: float, dur: float, warmth: float = 0.18) -> np.ndarray:
    """Синус с парой тихих гармоник — тёплая «округлая» волна без резкости."""
    t = t_of(dur)
    sig = np.sin(2 * np.pi * freq * t)
    sig += warmth * np.sin(2 * np.pi * 2 * freq * t)
    sig += warmth * 0.35 * np.sin(2 * np.pi * 3 * freq * t)
    return sig / (1.0 + warmth * 1.35)


def sweep(f0: float, f1: float, dur: float) -> np.ndarray:
    """Плавный свип по частоте (экспоненциальный)."""
    t = t_of(dur)
    k = (f1 / f0) ** (t / max(dur, 1e-6))
    phase = 2 * np.pi * np.cumsum(f0 * k) / SR
    return np.sin(phase)


def pad(freq: float, dur: float, decay: float = 1.0, amp: float = 1.0) -> np.ndarray:
    """
    Мягкая «маримба»: основная нота + две тихие гармоники, каждая со своим
    медленным затуханием. Никаких негармонических звонов — именно они резали слух.
    """
    t = t_of(dur)
    partials = [
        (1.00, 1.00, 1.0),
        (2.00, 0.20, 1.6),
        (3.01, 0.06, 2.2),
    ]
    sig = np.zeros(len(t))
    for ratio, p_amp, p_dec in partials:
        sig += p_amp * np.exp(-t * p_dec * decay) * np.sin(2 * np.pi * freq * ratio * t)
    sig *= 1.0 - np.exp(-t / 0.012)  # мягкая атака
    fade = min(int(0.015 * SR), max(2, len(sig) // 4))
    sig[-fade:] *= np.linspace(1.0, 0.0, fade)
    return sig * amp


def noise(dur: float, seed: int = 0) -> np.ndarray:
    rng = np.random.default_rng(seed)
    return rng.uniform(-1.0, 1.0, int(dur * SR))


def lowpass(sig: np.ndarray, cutoff: float) -> np.ndarray:
    """Однополюсный ФНЧ (векторизованный)."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    prev = np.zeros(1, dtype=np.float64)
    out = np.empty_like(sig, dtype=np.float64)
    b = 1.0 - a
    acc = 0.0
    for i in range(len(sig)):
        acc = b * sig[i] + a * acc
        out[i] = acc
    del prev
    return out


def warm(sig: np.ndarray) -> np.ndarray:
    """Финальное «утепление»: срез верхов и лёгкий фейд в конце."""
    sig = lowpass(sig, WARM_CUTOFF)
    fade = min(int(0.010 * SR), max(2, len(sig) // 5))
    sig[-fade:] *= np.linspace(1.0, 0.0, fade)
    return sig


def place(base: np.ndarray, sig: np.ndarray, at: float) -> None:
    start = int(at * SR)
    end = min(len(base), start + len(sig))
    if end > start:
        base[start:end] += sig[: end - start]


def mix(*sigs: np.ndarray) -> np.ndarray:
    if not sigs:
        return np.zeros(1)
    size = max(len(s) for s in sigs)
    out = np.zeros(size)
    for s in sigs:
        out[: len(s)] += s
    return out


def normalize(sig: np.ndarray, peak: float) -> np.ndarray:
    m = float(np.max(np.abs(sig))) or 1.0
    out = sig / m * peak
    # мягкое ограничение без «металла»
    return np.tanh(out * 1.1) / np.tanh(1.1)


# --------------------------------------------------------------------------- #
#                              Сами звуки                                      #
# --------------------------------------------------------------------------- #

def sfx_click() -> np.ndarray:
    """Короткий мягкий «ток» интерфейса: тёплый, без щелчка."""
    dur = 0.10
    s = mix(
        soft_tone(520.0, dur, 0.12) * env(dur, 0.010, 0.030),
        0.25 * soft_tone(780.0, dur, 0.08) * env(dur, 0.012, 0.022),
    )
    return normalize(warm(s), 0.26)


def sfx_flip() -> np.ndarray:
    """Переворот карточки — тихий «шшух» с плавным свипом."""
    dur = 0.22
    body = sweep(300.0, 620.0, dur) * env(dur, 0.018, 0.075)
    breath = lowpass(noise(0.22, seed=3), 900.0) * env(0.22, 0.030, 0.060)
    s = mix(body, 0.30 * breath)
    return normalize(warm(s), 0.28)


def sfx_match() -> np.ndarray:
    """Пара найдена — тёплый двойной аккорд C5 → E5 (мягкая маримба)."""
    dur = 0.85
    s = np.zeros(int(dur * SR))
    place(s, pad(523.25, 0.70, decay=1.5) * 0.85, 0.0)          # C5
    place(s, pad(659.25, 0.72, decay=1.6) * 0.70, 0.09)         # E5
    place(s, pad(1046.50, 0.55, decay=1.8) * 0.22, 0.12)        # тихий блеск октавой выше
    return normalize(warm(s), 0.34)


def sfx_combo() -> np.ndarray:
    """Комбо — плавное восходящее арпеджио, всё на мягких синусах."""
    dur = 1.0
    s = np.zeros(int(dur * SR))
    for i, f in enumerate([523.25, 659.25, 783.99, 1046.50]):
        place(s, pad(f, 0.55, decay=1.7) * (0.75 - i * 0.10), 0.075 * i)
    return normalize(warm(s), 0.32)


def sfx_error() -> np.ndarray:
    """Ошибка — мягкий низкий «буп», без резкого края и без «бззз»."""
    dur = 0.42
    body = sweep(220.0, 150.0, dur) * env(dur, 0.020, 0.15)
    sub = soft_tone(110.0, dur, 0.10) * env(dur, 0.025, 0.16) * 0.55
    s = mix(body, sub)
    return normalize(warm(s), 0.26)


def sfx_star() -> np.ndarray:
    """Звёздочка на экране победы — тихий «динь» (не свистящий)."""
    dur = 0.55
    s = mix(
        pad(1318.5, 0.45, decay=2.0) * 0.9,
        pad(1975.5, 0.35, decay=2.6) * 0.18,
    )
    return normalize(warm(s), 0.24)


def sfx_win() -> np.ndarray:
    """Победная фанфара — спокойная пентатоника C5-D5-E5-G5-C6, мягкие синусы."""
    dur = 2.3
    s = np.zeros(int(dur * SR))
    notes = [523.25, 587.33, 659.25, 783.99, 1046.50]
    for i, f in enumerate(notes):
        place(s, pad(f, 1.25, decay=1.15) * (0.62 + 0.06 * i), i * 0.15)
    place(s, pad(1318.50, 1.5, decay=1.0) * 0.45, 0.72)
    for i in range(5):  # тихие «искры» — только как фон
        place(s, pad(1567.98 * (1 + i * 0.06), 0.5, decay=3.0) * 0.07, 0.85 + i * 0.09)
    return normalize(warm(s), 0.38)


def sfx_whoosh() -> np.ndarray:
    """Переход между экранами — едва слышный вздох воздуха."""
    dur = 0.42
    n = noise(dur, seed=11)
    body = lowpass(n, 700.0) * env(dur, 0.060, 0.19)
    tone = sweep(160.0, 460.0, dur) * env(dur, 0.055, 0.16) * 0.30
    return normalize(warm(mix(body, tone)), 0.16)


def sfx_pop() -> np.ndarray:
    """Лопнувший пузырь — мягкий «пльох» с булькающим хвостом (Bubble pop)."""
    dur = 0.30
    blip = sweep(600.0, 260.0, 0.14) * env(0.14, 0.014, 0.045)
    bubble = sweep(260.0, 520.0, 0.20) * env(0.20, 0.020, 0.075) * 0.45
    s = np.zeros(int(dur * SR))
    place(s, blip, 0.0)
    place(s, bubble, 0.05)
    return normalize(warm(s), 0.26)


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
