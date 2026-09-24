package com.chinesegames.app.data

/**
 * Всё, что относится к «украшательству» приложения: цветовые стили,
 * стилевые наборы (китайский дракон, аниме) и уголок для талисмана.
 *
 * Обучение полностью бесплатно; эти вещи — единственное, что продаётся
 * в магазине (см. [Product]). Фиолетовый стиль — бесплатный и включён
 * по умолчанию.
 */
enum class ColorStyle(
    val title: String,
    val emoji: String,
    /** Поворот оттенка относительно базового фиолетового (≈258°). */
    val hueShift: Float,
    /** Цвет-образец для плитки выбора. */
    val swatch: Long
) {
    PURPLE("Фиолетовый", "💜", 0f, 0xFF8B5CF6),
    PINK("Розовый", "🌸", 72f, 0xFFF472B6),
    BLUE("Синий", "💙", -36f, 0xFF3B82F6),
    RED("Красный", "❤️", 100f, 0xFFEF4444),
    ORANGE("Оранжевый", "🧡", 127f, 0xFFF97316),
    YELLOW("Жёлтый", "💛", 150f, 0xFFEAB308),
    GREEN("Зелёный", "💚", 247f, 0xFF22C55E),
    CYAN("Голубой", "🩵", -63f, 0xFF06B6D4);

    /** Бесплатный стиль — только базовый фиолетовый. */
    val isFree: Boolean get() = this == PURPLE

    companion object {
        fun fromName(raw: String?): ColorStyle =
            entries.firstOrNull { it.name == raw } ?: PURPLE
    }
}

/**
 * Стилевой набор: помимо цветов меняет украшения фона, звуки и музыку.
 */
enum class StylePack(
    val title: String,
    val emoji: String,
    val description: String,
    /** Цветовой стиль, который включается вместе с набором. */
    val defaultColor: ColorStyle
) {
    CLASSIC(
        title = "Классика",
        emoji = "🌙",
        description = "Фиолетовый неон, сакура и облака",
        defaultColor = ColorStyle.PURPLE
    ),
    CHINA(
        title = "Китайский дракон",
        emoji = "🐉",
        description = "Красное золото, дракон, фонарики и монеты, тихая традиционная музыка",
        defaultColor = ColorStyle.RED
    ),
    ANIME(
        title = "Аниме",
        emoji = "✨",
        description = "Пиксельные Вагури и Сукуна-в-Мэгуми по краям экрана, блёстки, аниме-звуки и музыка",
        defaultColor = ColorStyle.PINK
    );

    val isFree: Boolean get() = this == CLASSIC

    companion object {
        fun fromName(raw: String?): StylePack =
            entries.firstOrNull { it.name == raw } ?: CLASSIC
    }
}

/** Угол экрана, в котором живёт чиби-талисман. */
enum class MascotCorner(val title: String) {
    BOTTOM_END("Справа снизу"),
    BOTTOM_START("Слева снизу"),
    TOP_END("Справа сверху"),
    TOP_START("Слева сверху");

    val isTop: Boolean get() = this == TOP_END || this == TOP_START
    val isEnd: Boolean get() = this == BOTTOM_END || this == TOP_END

    companion object {
        fun fromName(raw: String?): MascotCorner =
            entries.firstOrNull { it.name == raw } ?: BOTTOM_END

        fun of(top: Boolean, end: Boolean): MascotCorner = when {
            top && end -> TOP_END
            top -> TOP_START
            end -> BOTTOM_END
            else -> BOTTOM_START
        }
    }
}
