#!/usr/bin/env python3
"""
Генератор иконок приложения ChineseGames (фиолетовый «汉»).

Создаёт:
  * mipmap-*/ic_launcher.png          — обычная иконка для старых лаунчеров
  * mipmap-*/ic_launcher_round.png    — круглая иконка
  * mipmap-*/ic_launcher_foreground.png — передний слой адаптивной иконки

Запуск:  python3 tools/generate_icons.py
Нужны Pillow и любой CJK-шрифт (по умолчанию берётся Noto Sans CJK).
"""

import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.normpath(os.path.join(HERE, "..", "app", "src", "main", "res"))

FONT_CANDIDATES = [
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
    "/System/Library/Fonts/PingFang.ttc",
    "C:/Windows/Fonts/msyhbd.ttc",
]

GLYPH = "汉"
VIOLET_TOP = (167, 139, 250)      # #A78BFA
VIOLET_MID = (124, 58, 237)       # #7C3AED
VIOLET_BOTTOM = (67, 18, 138)     # #43128A
FUCHSIA = (217, 70, 239)

# плотность -> размер обычной иконки
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
# плотность -> размер переднего слоя адаптивной иконки (108dp)
FOREGROUND = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def find_font() -> str:
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            return path
    raise SystemExit("Не найден CJK-шрифт. Укажите путь в FONT_CANDIDATES.")


def gradient(size: int) -> Image.Image:
    """Вертикально-радиальный фиолетовый градиент."""
    img = Image.new("RGB", (size, size), VIOLET_MID)
    px = img.load()
    for y in range(size):
        for x in range(size):
            ny = y / size
            nx = x / size
            # базовый вертикальный градиент
            if ny < 0.5:
                k = ny / 0.5
                base = tuple(
                    int(VIOLET_TOP[i] + (VIOLET_MID[i] - VIOLET_TOP[i]) * k) for i in range(3)
                )
            else:
                k = (ny - 0.5) / 0.5
                base = tuple(
                    int(VIOLET_MID[i] + (VIOLET_BOTTOM[i] - VIOLET_MID[i]) * k) for i in range(3)
                )
            # световое пятно сверху слева и фуксия справа снизу
            d1 = ((nx - 0.22) ** 2 + (ny - 0.18) ** 2) ** 0.5
            glow1 = max(0.0, 1.0 - d1 * 2.3) ** 2
            d2 = ((nx - 0.88) ** 2 + (ny - 0.82) ** 2) ** 0.5
            glow2 = max(0.0, 1.0 - d2 * 2.8) ** 2
            col = [
                min(255, int(base[i] + glow1 * 55 * (1 - i * 0.30) + glow2 * 45 * (0.3 + i * 0.35)))
                for i in range(3)
            ]
            px[x, y] = tuple(col)
    return img


def rounded_mask(size: int, radius_ratio: float = 0.24) -> Image.Image:
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size * 4 - 1, size * 4 - 1),
        radius=int(size * 4 * radius_ratio),
        fill=255,
    )
    return mask.resize((size, size), Image.LANCZOS)


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def draw_glyph(base: Image.Image, font_path: str, ratio: float, with_glow: bool = True) -> None:
    """Рисует 汉 по центру с мягким свечением."""
    size = base.width
    font = ImageFont.truetype(font_path, int(size * ratio))
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    box = draw.textbbox((0, 0), GLYPH, font=font)
    x = (size - (box[2] - box[0])) / 2 - box[0]
    y = (size - (box[3] - box[1])) / 2 - box[1]

    if with_glow:
        glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow)
        gdraw.text((x, y), GLYPH, font=font, fill=(255, 255, 255, 170))
        glow = glow.filter(ImageFilter.GaussianBlur(radius=max(2, size * 0.045)))
        layer = Image.alpha_composite(layer, glow)

    draw = ImageDraw.Draw(layer)
    draw.text((x, y), GLYPH, font=font, fill=(255, 255, 255, 255))
    base.alpha_composite(layer)


def save(img: Image.Image, folder: str, name: str) -> None:
    path = os.path.join(RES_DIR, folder)
    os.makedirs(path, exist_ok=True)
    img.save(os.path.join(path, name), "PNG", optimize=True)


def main() -> None:
    font_path = find_font()
    master = 512
    bg = gradient(master).convert("RGBA")

    # --- обычная (квадратная со скруглением) и круглая иконки ---
    for mask_kind, file_name in (("round_rect", "ic_launcher.png"), ("circle", "ic_launcher_round.png")):
        canvas = Image.new("RGBA", (master, master), (0, 0, 0, 0))
        face = bg.copy()
        draw_glyph(face, font_path, 0.64)
        mask = rounded_mask(master) if mask_kind == "round_rect" else circle_mask(master)
        canvas.paste(face, (0, 0), mask)
        for density, px in DENSITIES.items():
            save(canvas.resize((px, px), Image.LANCZOS), f"mipmap-{density}", file_name)

    # --- передний слой адаптивной иконки (прозрачный фон, глиф в безопасной зоне) ---
    for density, px in FOREGROUND.items():
        layer = Image.new("RGBA", (master, master), (0, 0, 0, 0))
        draw_glyph(layer, font_path, 0.40)
        save(layer.resize((px, px), Image.LANCZOS), f"mipmap-{density}", "ic_launcher_foreground.png")

    print("Иконки обновлены в", RES_DIR)


if __name__ == "__main__":
    main()
