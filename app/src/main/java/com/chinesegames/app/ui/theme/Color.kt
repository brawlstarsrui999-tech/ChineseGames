package com.chinesegames.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* --------------------------------------------------------------------------
 *  Палитра ChineseGames.
 *
 *  Все цвета — «двойные»: ночная тема (глубокий фиолетовый неон) и дневная
 *  (светлая аниме-пастель). Какой вариант активен, решает [CgPaletteState],
 *  а весь интерфейс переключается целиком через key(theme) в MainActivity —
 *  поэтому цвета можно читать как обычные значения.
 *
 *  ВАЖНО: цвета меняются только вместе с темой, а не каждый кадр,
 *  так что читать их в композиции безопасно.
 * -------------------------------------------------------------------------- */

object CgPaletteState {
    /** true — ночная (фиолетовый неон), false — дневная (пастель). */
    var night: Boolean = true
}

/**
 * Выбор «ночного» или «дневного» варианта одного и того же значения.
 * Функция обобщённая: так она работает и с отдельными цветами (`Color`),
 * и с градиентами (`List<Color>`).
 */
private fun <T> dual(nightValue: T, dayValue: T): T =
    if (CgPaletteState.night) nightValue else dayValue

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

val VoidPurple: Color get() = dual(NightVoid, DaySky)
val DeepPurple: Color get() = dual(NightDeep, DayCardSoft)
val MidnightPurple: Color get() = dual(NightMidnight, DayCard)
val RoyalPurple: Color get() = Color(0xFF6D28D9)
val VividPurple: Color get() = dual(Color(0xFF8B5CF6), Color(0xFF7C3AED))
val LightViolet: Color get() = dual(Color(0xFFA78BFA), Color(0xFF7C5CE0))
val LavenderGlow: Color get() = dual(Color(0xFFC4B5FD), Color(0xFF6D28D9))
val Lilac: Color get() = dual(Color(0xFFE9D5FF), Color(0xFF3B2465))
val FuchsiaGlow: Color get() = dual(Color(0xFFD946EF), Color(0xFFC026D3))
val IndigoDeep: Color get() = dual(NightIndigo, Color(0xFFDDD0FF))

val GoldAccent: Color get() = dual(Color(0xFFFBBF24), Color(0xFFD97706))
val MintAccent: Color get() = dual(Color(0xFF34D399), Color(0xFF059669))
val RoseAccent: Color get() = dual(Color(0xFFFB7185), Color(0xFFE11D48))
val SkyAccent: Color get() = dual(Color(0xFF60A5FA), Color(0xFF2563EB))

val TextPrimary: Color get() = dual(NightTextPrimary, DayTextPrimary)
val TextSecondary: Color get() = dual(NightTextSecondary, DayTextSecondary)
val TextMuted: Color get() = dual(NightTextMuted, DayTextMuted)

val GlassFill: Color get() = dual(Color(0x1AFFFFFF), Color(0x4DFFFFFF))
val GlassFillStrong: Color get() = dual(Color(0x26FFFFFF), Color(0x66FFFFFF))
val GlassStroke: Color get() = dual(Color(0x4DB794F6), Color(0x558B5CF6))

/** Цвет текста на золотой/светлой плашке (одинаково читается в обеих темах). */
val OnAccentInk: Color get() = Color(0xFF4A2500)

/**
 * Готовые градиенты и «клейкие» цвета для карточек и кнопок,
 * чтобы оформление было одинаковым на всех экранах.
 */
object CG {
    private val nightBackground by lazy {
        Brush.verticalGradient(listOf(Color(0xFF1A0B36), Color(0xFF0E0620), Color(0xFF160A2E)))
    }
    private val dayBackground by lazy {
        Brush.verticalGradient(listOf(DaySky, DayLilac, DayRose))
    }

    val background: Brush get() = if (CgPaletteState.night) nightBackground else dayBackground

    private val nightHeaderGlow by lazy {
        Brush.radialGradient(listOf(VividPurple.copy(alpha = 0.45f), Color.Transparent))
    }
    private val dayHeaderGlow by lazy {
        Brush.radialGradient(listOf(Color(0x668B5CF6), Color.Transparent))
    }

    val headerGlow: Brush get() = if (CgPaletteState.night) nightHeaderGlow else dayHeaderGlow

    private val nightCardGradient by lazy {
        listOf(Color(0x338B5CF6), Color(0x1A6D28D9), Color(0x14FFFFFF))
    }
    private val dayCardGradient by lazy {
        listOf(Color(0xF7FFFFFF), Color(0xF2F6EEFF), Color(0xF7FFF3FB))
    }

    val cardGradient: List<Color>
        get() = if (CgPaletteState.night) nightCardGradient else dayCardGradient

    private val nightCardBorder by lazy {
        listOf(LavenderGlow.copy(alpha = 0.45f), Color.Transparent, FuchsiaGlow.copy(alpha = 0.35f))
    }
    private val dayCardBorder by lazy {
        listOf(Color(0x668B5CF6), Color(0x22FFFFFF), Color(0x55D946EF))
    }

    val cardBorder: List<Color>
        get() = if (CgPaletteState.night) nightCardBorder else dayCardBorder

    val primaryGradient: List<Color>
        get() = dual(
            listOf(Color(0xFF9F6BFF), Color(0xFFD946EF)),
            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        )

    val primaryGradientDeep: List<Color>
        get() = dual(
            listOf(Color(0xFF6D28D9), Color(0xFFA855F7)),
            listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
        )

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
        get() = dual(
            listOf(Color(0xFF60A5FA), Color(0xFF7C3AED)),
            listOf(Color(0xFF60A5FA), Color(0xFF7C3AED))
        )

    /* ------------------ Поверхности, панели и «стекло» ------------------ */

    private val nightSurface by lazy { listOf(Color(0xFF2B1657), Color(0xFF170B31)) }
    private val daySurface by lazy { listOf(Color(0xFFFFFFFF), Color(0xFFF3E9FF)) }

    /** Внутренность крупных панелей и диалогов. */
    val surface: List<Color> get() = if (CgPaletteState.night) nightSurface else daySurface

    private val nightPanel by lazy { listOf(Color(0xFF2C1758), Color(0xFF140A2B)) }
    private val dayPanel by lazy { listOf(Color(0xFFFFFDFF), Color(0xFFF1E7FF)) }

    /** Панель экрана победы. */
    val panel: List<Color> get() = if (CgPaletteState.night) nightPanel else dayPanel

    private val nightCardBack by lazy {
        listOf(Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFF2E1065))
    }
    private val dayCardBack by lazy {
        listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6), Color(0xFF6D28D9))
    }

    /** Рубашка закрытой игровой карточки. */
    val cardBack: List<Color> get() = if (CgPaletteState.night) nightCardBack else dayCardBack

    private val nightCardFace by lazy { listOf(Color(0xFF2B1657), Color(0xFF1A0E38)) }
    private val dayCardFace by lazy { listOf(Color(0xFFFFFFFF), Color(0xFFEFE4FF)) }

    /** Лицо открытой игровой карточки. */
    val cardFace: List<Color> get() = if (CgPaletteState.night) nightCardFace else dayCardFace

    /** Затемнение фона под оверлеями (пауза, победа, «мало слов»). */
    val scrim: Color get() = dual(Color(0xE60B0618), Color(0xE6F3EDFF))

    /** Плотное затемнение — под финальными экранами. */
    val scrimStrong: Color get() = dual(Color(0xEE0B0618), Color(0xF2F3EDFF))

    /** Мягкое «стекло» поверх фона: в ночной теме белое, в дневной — фиолетовое. */
    fun glass(alpha: Float): Color =
        if (CgPaletteState.night) Color.White.copy(alpha = alpha)
        else Color(0xFF6D28D9).copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f))

    /** Подложка прогресс-бара. */
    val track: Color get() = glass(0.09f)

    /* --------------------- Пиксельные декорации --------------------- */

    /** «Чернила» пиксель-арта. */
    val pixelInk: Color get() = dual(Color(0xFF24103F), Color(0xFF3B2465))

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
