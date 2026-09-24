package com.chinesegames.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.chinesegames.app.data.ColorStyle
import com.chinesegames.app.data.StylePack
import kotlin.math.abs

/* --------------------------------------------------------------------------
 *  Палитра ChineseGames.
 *
 *  Все цвета — «двойные»: ночная тема (глубокий фиолетовый неон) и дневная
 *  (светлая пастель). Какой вариант активен, решает [CgPaletteState],
 *  а весь интерфейс переключается целиком через key(theme, style) в
 *  MainActivity — поэтому цвета можно читать как обычные значения.
 *
 *  Поверх ночь/день накладывается цветовой стиль ([ColorStyle]): весь
 *  «фиолетовый» слой палитры поворачивается по оттенку (розовый, синий,
 *  красный…), а смысловые акценты — золото, мята, роза, небо — остаются
 *  на месте, чтобы «верно/ошибка/звук» читались одинаково в любом стиле.
 *
 *  ВАЖНО: цвета меняются только вместе с темой/стилем, а не каждый кадр,
 *  так что читать их в композиции безопасно.
 * -------------------------------------------------------------------------- */

object CgPaletteState {
    /** true — ночная (фиолетовый неон), false — дневная (пастель). */
    var night: Boolean = true

    /** Активный цветовой стиль (фиолетовый — бесплатный и по умолчанию). */
    var style: ColorStyle = ColorStyle.PURPLE

    /** Активный стилевой набор (украшения фона, звуки, музыка). */
    var pack: StylePack = StylePack.CLASSIC

    /** Поворот оттенка в градусах для «фиолетового» слоя палитры. */
    val hueShift: Float get() = style.hueShift

    /** Ключ для кэшей и key() в композиции. */
    val key: String get() = "${if (night) "n" else "d"}|${style.name}|${pack.name}"
}

/**
 * Выбор «ночного» или «дневного» варианта одного и того же значения.
 * Функция обобщённая: так она работает и с отдельными цветами (`Color`),
 * и с градиентами (`List<Color>`).
 */
private fun <T> dual(nightValue: T, dayValue: T): T =
    if (CgPaletteState.night) nightValue else dayValue

/** Цвет «фиолетового» слоя: поворачивается вместе с цветовым стилем. */
private fun tinted(nightValue: Color, dayValue: Color): Color =
    dual(nightValue, dayValue).shiftHue(CgPaletteState.hueShift)

private fun tintedList(nightValue: List<Color>, dayValue: List<Color>): List<Color> =
    dual(nightValue, dayValue).map { it.shiftHue(CgPaletteState.hueShift) }

/**
 * Поворот оттенка (HSV) на [degrees]. Серые и белые цвета не меняются,
 * прозрачность сохраняется. Чистая арифметика — дёшево даже при чтении
 * в композиции.
 */
fun Color.shiftHue(degrees: Float): Color {
    if (degrees == 0f) return this
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta < 0.0005f || max <= 0f) return this
    var h = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    h = ((h + degrees) % 360f + 360f) % 360f
    val s = delta / max
    val v = max
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/* ------------------------------- Ночная тема ------------------------------- */

private val NightVoid = Color(0xFF0B0618)
private val NightDeep = Color(0xFF140A2B)
private val NightMidnight = Color(0xFF1C0F3D)
private val NightIndigo = Color(0xFF4C1D95)
private val NightTextPrimary = Color(0xFFF6F3FF)
private val NightTextSecondary = Color(0xFFBBAEE4)
private val NightTextMuted = Color(0xFF8271AC)

/* ------------------------------- Дневная тема ------------------------------ */

private val DaySky = Color(0xFFE7F0FF)
private val DayLilac = Color(0xFFF5ECFF)
private val DayRose = Color(0xFFFFF0F7)
private val DayCard = Color(0xFFFFFFFF)
private val DayCardSoft = Color(0xFFF4ECFF)
private val DayTextPrimary = Color(0xFF241443)
private val DayTextSecondary = Color(0xFF5A4A80)
private val DayTextMuted = Color(0xFF8C7DAF)

/* ------------------------------- Общие акценты ----------------------------- */

val VoidPurple: Color get() = tinted(NightVoid, DaySky)
val DeepPurple: Color get() = tinted(NightDeep, DayCardSoft)
val MidnightPurple: Color get() = tinted(NightMidnight, DayCard)
val RoyalPurple: Color get() = Color(0xFF6D28D9).shiftHue(CgPaletteState.hueShift)
val VividPurple: Color get() = tinted(Color(0xFF8B5CF6), Color(0xFF7C3AED))
val LightViolet: Color get() = tinted(Color(0xFFA78BFA), Color(0xFF7C5CE0))
val LavenderGlow: Color get() = tinted(Color(0xFFC4B5FD), Color(0xFF6D28D9))
val Lilac: Color get() = tinted(Color(0xFFE9D5FF), Color(0xFF3B2465))
val FuchsiaGlow: Color get() = tinted(Color(0xFFD946EF), Color(0xFFC026D3))
val IndigoDeep: Color get() = tinted(NightIndigo, Color(0xFFDDD0FF))

val GoldAccent: Color get() = dual(Color(0xFFFBBF24), Color(0xFFD97706))
val MintAccent: Color get() = dual(Color(0xFF34D399), Color(0xFF059669))
val RoseAccent: Color get() = dual(Color(0xFFFB7185), Color(0xFFE11D48))
val SkyAccent: Color get() = dual(Color(0xFF60A5FA), Color(0xFF2563EB))

val TextPrimary: Color get() = tinted(NightTextPrimary, DayTextPrimary)
val TextSecondary: Color get() = tinted(NightTextSecondary, DayTextSecondary)
val TextMuted: Color get() = tinted(NightTextMuted, DayTextMuted)

val GlassFill: Color get() = dual(Color(0x1AFFFFFF), Color(0x4DFFFFFF))
val GlassFillStrong: Color get() = dual(Color(0x26FFFFFF), Color(0x66FFFFFF))
val GlassStroke: Color get() = tinted(Color(0x4DB794F6), Color(0x558B5CF6))

/** Цвет текста на золотой/светлой плашке (одинаково читается в обеих темах). */
val OnAccentInk: Color get() = Color(0xFF4A2500)

/**
 * Готовые градиенты и «клейкие» цвета для карточек и кнопок,
 * чтобы оформление было одинаковым на всех экранах.
 *
 * Кисти и списки кэшируются по ключу «тема|стиль», поэтому при смене
 * цветового стиля они пересчитываются, а в остальное время не создаются заново.
 */
object CG {
    private var cacheKey: String = ""
    private val cache = HashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> cached(name: String, build: () -> T): T {
        val key = CgPaletteState.key
        if (key != cacheKey) {
            cache.clear()
            cacheKey = key
        }
        return cache.getOrPut(name, build) as T
    }

    // Color — inline/value type Compose, поэтому vararg<Color> запрещён компилятором.
    // Список сохраняет ту же читаемость без упаковки vararg-массива.
    private fun shifted(colors: List<Color>): List<Color> =
        colors.map { it.shiftHue(CgPaletteState.hueShift) }

    val background: Brush
        get() = cached("background") {
            Brush.verticalGradient(
                tintedList(
                    listOf(Color(0xFF1A0B36), Color(0xFF0E0620), Color(0xFF160A2E)),
                    listOf(DaySky, DayLilac, DayRose)
                )
            )
        }

    val headerGlow: Brush
        get() = cached("headerGlow") {
            Brush.radialGradient(
                if (CgPaletteState.night) {
                    listOf(VividPurple.copy(alpha = 0.45f), Color.Transparent)
                } else {
                    listOf(Color(0x668B5CF6).shiftHue(CgPaletteState.hueShift), Color.Transparent)
                }
            )
        }

    val cardGradient: List<Color>
        get() = cached("cardGradient") {
            tintedList(
                listOf(Color(0x338B5CF6), Color(0x1A6D28D9), Color(0x14FFFFFF)),
                listOf(Color(0xF7FFFFFF), Color(0xF2F6EEFF), Color(0xF7FFF3FB))
            )
        }

    val cardBorder: List<Color>
        get() = cached("cardBorder") {
            if (CgPaletteState.night) {
                listOf(LavenderGlow.copy(alpha = 0.45f), Color.Transparent, FuchsiaGlow.copy(alpha = 0.35f))
            } else {
                shifted(listOf(Color(0x668B5CF6), Color(0x22FFFFFF), Color(0x55D946EF)))
            }
        }

    val primaryGradient: List<Color>
        get() = cached("primaryGradient") {
            tintedList(
                listOf(Color(0xFF9F6BFF), Color(0xFFD946EF)),
                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
            )
        }

    val primaryGradientDeep: List<Color>
        get() = cached("primaryGradientDeep") {
            tintedList(
                listOf(Color(0xFF6D28D9), Color(0xFFA855F7)),
                listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
            )
        }

    val goldGradient: List<Color>
        get() = dual(
            listOf(Color(0xFFFCD34D), Color(0xFFF59E0B)),
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
        )

    val successGradient: List<Color>
        get() = dual(
            listOf(Color(0xFF34D399), Color(0xFF059669)),
            listOf(Color(0xFF34D399), Color(0xFF059669))
        )

    val dangerGradient: List<Color>
        get() = dual(
            listOf(Color(0xFFFB7185), Color(0xFFBE123C)),
            listOf(Color(0xFFFB7185), Color(0xFFBE123C))
        )

    val audioGradient: List<Color>
        get() = cached("audioGradient") {
            listOf(Color(0xFF60A5FA), Color(0xFF7C3AED).shiftHue(CgPaletteState.hueShift))
        }

    /* ------------------ Поверхности, панели и «стекло» ------------------ */

    /** Внутренность крупных панелей и диалогов. */
    val surface: List<Color>
        get() = cached("surface") {
            tintedList(
                listOf(Color(0xFF2B1657), Color(0xFF170B31)),
                listOf(Color(0xFFFFFFFF), Color(0xFFF3E9FF))
            )
        }

    /** Панель экрана победы. */
    val panel: List<Color>
        get() = cached("panel") {
            tintedList(
                listOf(Color(0xFF2C1758), Color(0xFF140A2B)),
                listOf(Color(0xFFFFFDFF), Color(0xFFF1E7FF))
            )
        }

    /** Рубашка закрытой игровой карточки. */
    val cardBack: List<Color>
        get() = cached("cardBack") {
            tintedList(
                listOf(Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFF2E1065)),
                listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6), Color(0xFF6D28D9))
            )
        }

    /** Лицо открытой игровой карточки. */
    val cardFace: List<Color>
        get() = cached("cardFace") {
            tintedList(
                listOf(Color(0xFF2B1657), Color(0xFF1A0E38)),
                listOf(Color(0xFFFFFFFF), Color(0xFFEFE4FF))
            )
        }

    /** Затемнение фона под оверлеями (пауза, победа, «мало слов»). */
    val scrim: Color get() = tinted(Color(0xE60B0618), Color(0xE6F3EDFF))

    /** Плотное затемнение — под финальными экранами. */
    val scrimStrong: Color get() = tinted(Color(0xEE0B0618), Color(0xF2F3EDFF))

    /** Мягкое «стекло» поверх фона: в ночной теме белое, в дневной — фиолетовое. */
    fun glass(alpha: Float): Color =
        if (CgPaletteState.night) Color.White.copy(alpha = alpha)
        else Color(0xFF6D28D9).shiftHue(CgPaletteState.hueShift).copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f))

    /** Подложка прогресс-бара. */
    val track: Color get() = glass(0.09f)

    /* --------------------- Пиксельные декорации --------------------- */

    /** «Чернила» пиксель-арта. */
    val pixelInk: Color get() = tinted(Color(0xFF24103F), Color(0xFF3B2465))

    /** Лепестки сакуры, звёзды и прочая пиксельная милота. */
    val pixelPetal: Color get() = dual(Color(0xFFFFA8D5), Color(0xFFFF8AC4))
    val pixelPetalDark: Color get() = dual(Color(0xFFF472B6), Color(0xFFEC4899))
    val pixelLeaf: Color get() = dual(Color(0xFF6EE7B7), Color(0xFF34D399))
    val pixelGold: Color get() = dual(Color(0xFFFCD34D), Color(0xFFF59E0B))
    val pixelSky: Color get() = dual(Color(0xFF93C5FD), Color(0xFF60A5FA))
    val pixelWhite: Color get() = dual(Color(0xFFF8F5FF), Color(0xFFFFFFFF))
    val pixelBlush: Color get() = dual(Color(0xFFFF9EB5), Color(0xFFFF7A9B))
    val pixelLantern: Color get() = dual(Color(0xFFF87171), Color(0xFFEF4444))

    const val LEARNED_THRESHOLD = 0.75f
}
