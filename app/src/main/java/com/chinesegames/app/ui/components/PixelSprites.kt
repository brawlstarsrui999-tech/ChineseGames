package com.chinesegames.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.FuchsiaGlow
import com.chinesegames.app.ui.theme.RoyalPurple
import com.chinesegames.app.ui.theme.VividPurple

/**
 * Пиксель-арт приложения. Спрайты — это обычные строки: один символ = один
 * «пиксель», точка = прозрачный. Рисуются квадратиками на Canvas, поэтому
 * выглядят честно пиксельно на любом экране и не требуют картинок.
 */
data class PixelSprite(
    val rows: List<String>,
    val tint: Color? = null
) {
    val cols: Int get() = rows.maxOfOrNull { it.length } ?: 0

    val height: Int get() = rows.size

    /** Пропорции спрайта (ширина / высота) — чтобы не искажать рисунок. */
    val aspect: Float get() = if (height == 0) 1f else cols.toFloat() / height.toFloat()
}

/** Палитра пиксель-арта: одна буква — один цвет. */
private fun pixelColor(char: Char): Color = when (char) {
    'k' -> CG.pixelInk
    'w' -> CG.pixelWhite
    'p' -> CG.pixelPetal
    'P' -> CG.pixelPetalDark
    's' -> CG.pixelBlush
    'l' -> CG.pixelLeaf
    'y' -> CG.pixelGold
    'b' -> CG.pixelSky
    'r' -> CG.pixelLantern
    'v' -> VividPurple
    'd' -> RoyalPurple
    'f' -> FuchsiaGlow
    // Персонажи стилевых наборов и талисман
    'S' -> CG.pixelSkin
    'T' -> CG.pixelSkinShade
    'B' -> CG.pixelHairBlue
    'D' -> CG.pixelHairBlueDark
    'R' -> CG.pixelHairRose
    'Q' -> CG.pixelHairRoseDark
    'H' -> CG.pixelHairDark
    'J' -> CG.pixelHairDarkLight
    'C' -> CG.pixelCrimson
    'U' -> CG.pixelUniform
    // Дракон и красное золото
    'j' -> CG.pixelJade
    'g' -> CG.pixelJadeDark
    'e' -> CG.pixelScarlet
    'E' -> CG.pixelScarletDark
    else -> CG.pixelInk
}

fun spriteOf(rows: List<String>, tint: Color? = null) = PixelSprite(rows, tint)

/* ------------------------------- Спрайты ------------------------------- */

/** Лепесток сакуры. */
val PetalSprite = PixelSprite(
    listOf(
        "..ppp..",
        ".ppppp.",
        "ppppppp",
        "pppPppp",
        ".ppppp.",
        "..ppp..",
        "...p..."
    )
)

/** Цветок сакуры — для уголков и заголовков. */
val SakuraSprite = PixelSprite(
    listOf(
        "...p.p...",
        "..ppppp..",
        ".ppppppp.",
        "ppppppppp",
        ".ppppppp.",
        "..ppppp..",
        "...p.p..."
    )
)

/** Искрящаяся звёздочка. */
val StarSprite = PixelSprite(
    listOf(
        "...y...",
        "..yyy..",
        ".yyyyy.",
        "yyyyyyy",
        ".yyyyy.",
        ".y.y.y."
    )
)

/** Сердечко — например, для любимых слов. */
val HeartSprite = PixelSprite(
    listOf(
        ".pp.pp.",
        "ppppppp",
        "ppppppp",
        ".ppppp.",
        "..ppp..",
        "...p..."
    )
)

/** Милый котик-талисман. */
val CatSprite = PixelSprite(
    listOf(
        "..k......k..",
        ".kkk....kkk.",
        ".kkkkkkkkkk.",
        "kkkkkkkkkkkk",
        "kwwkkkkkkwwk",
        "kwkkkkkkkkwk",
        "kkkkkkkkkkkk",
        "kskkkkkkkksk",
        "kkkkkppkkkkk",
        "kkkkppppkkkk",
        ".kkkkkkkkkk."
    )
)

/** Китайский фонарик. */
val LanternSprite = PixelSprite(
    listOf(
        "..kkkkk..",
        ".kkrrrkk.",
        "krryyyrrk",
        "kryyyyyrk",
        "kryyyyyrk",
        "krryyyrrk",
        ".kkrrrkk.",
        "..krrrk..",
        "...kkk...",
        "....k...."
    )
)

/** Ворота-тории — для дневной темы. */
val ToriiSprite = PixelSprite(
    listOf(
        "kkkkkkkkkkk",
        "krrrrrrrrrk",
        "krrrrrrrrrk",
        "...krrrk...",
        "..krrrrrk..",
        "krrrrrrrrrk",
        "krrrrrrrrrk",
        "...krrrk...",
        "...krrrk..."
    )
)

/** Панда — маленький попутчик. */
val PandaSprite = PixelSprite(
    listOf(
        ".kk.....kk.",
        "kkk.....kkk",
        ".kkkkkkkkk.",
        "kkkkkkkkkkk",
        "kkwwkkkwwkk",
        "kkwbkkkwbkk",
        "kkkkkkkkkkk",
        "kkkkpppkkkk",
        ".kkkkkkkkk."
    )
)

/** Пузырьковый чай (бабл-ти). */
val BobaSprite = PixelSprite(
    listOf(
        "..kkkkk..",
        "..kwwwk..",
        ".kwyyywk.",
        ".kwyyywk.",
        ".kwyyywk.",
        ".kwyyywk.",
        ".kwyyywk.",
        ".kwyyywk.",
        ".kwkkkwk.",
        ".kwkkkwk.",
        "..kkkkk.."
    )
)

/** Облачко. */
val CloudSprite = PixelSprite(
    listOf(
        "...www...",
        "..wwwww..",
        ".wwwwwww.",
        "wwwwwwwww",
        ".wwwwwww."
    )
)

/** Четырёхлучевая искра. */
val SparkSprite = PixelSprite(
    listOf(
        "..w..",
        ".www.",
        "wwwww",
        ".www.",
        "..w.."
    )
)


/* ------------------------ Спрайты стилевых наборов ------------------------ */

/** Чиби-талисман: девочка с голубыми волосами и небесным платьем. */
val MascotSprite = PixelSprite(
    listOf(
        "......BBBBBB......",
        "....BBBBBBBBBB....",
        "...BBBBBBBBBBBB...",
        "..BBBBBBBBBBBBBB..",
        "..BBBDBBBBBBDBBB..",
        ".BBBDSSSSSSSSDBBB.",
        ".BBBSSSSSSSSSSBBB.",
        ".BBBSSkSSSSkSSBBB.",
        ".BBBSSbwSSbwSSBBB.",
        ".BBBSSSSSSSSSSBBB.",
        ".BBBSsSSSSSSsSBBB.",
        ".BBBSSSSssSSSSBBB.",
        ".BBB.SSSSSSSS.BBB.",
        ".BB..wwwwwwww..BB.",
        ".BB.wwbwwwwbww.BB.",
        ".B..bbbbbbbbbb..B.",
        "....bbbbbbbbbb....",
        "....bbbbbbbbbb....",
        ".....SS....SS.....",
        ".....kk....kk....."
    )
)

/** Талисман с зажмуренными от радости глазами — пока говорит. */
val MascotHappySprite = PixelSprite(
    listOf(
        "......BBBBBB......",
        "....BBBBBBBBBB....",
        "...BBBBBBBBBBBB...",
        "..BBBBBBBBBBBBBB..",
        "..BBBDBBBBBBDBBB..",
        ".BBBDSSSSSSSSDBBB.",
        ".BBBSSSSSSSSSSBBB.",
        ".BBBSkSkSSkSkSBBB.",
        ".BBBSSSSSSSSSSBBB.",
        ".BBBSSSSSSSSSSBBB.",
        ".BBBSsSSSSSSsSBBB.",
        ".BBBSSSSssSSSSBBB.",
        ".BBB.SSSSSSSS.BBB.",
        ".BB..wwwwwwww..BB.",
        ".BB.wwbwwwwbww.BB.",
        ".B..bbbbbbbbbb..B.",
        "....bbbbbbbbbb....",
        "....bbbbbbbbbb....",
        ".....SS....SS.....",
        ".....kk....kk....."
    )
)

/** Вагури: мягкие розово-русые волосы, голубые глаза, белая блузка и тёмная юбка. */
val WaguriSprite = PixelSprite(
    listOf(
        ".....RRRRRRRR.....",
        "....RRRRRRRRRR....",
        "...RRRRRRRRRRRR...",
        "..RRRRRRRRRRRRRR..",
        "..RRQRRRRRRRRQRR..",
        ".RRRQSSSSSSSSQRRR.",
        ".RRRSSSSSSSSSSRRR.",
        ".RRRSSkSSSSkSSRRR.",
        ".RRRSSbwSSbwSSRRR.",
        ".RRRSSSSSSSSSSRRR.",
        ".RRRSsSSSSSSsSRRR.",
        ".RRRSSSSssSSSSRRR.",
        ".RRR.SSSSSSSS.RRR.",
        ".RRR.wwwUUwww.RRR.",
        ".RRR.wwwwwwww.RRR.",
        ".RRR.wwwwwwww.RRR.",
        ".RR..UUUUUUUU..RR.",
        ".....UUUUUUUU.....",
        ".....SS....SS.....",
        ".....kk....kk....."
    )
)

/** Сукуна в теле Мэгуми: тёмные торчащие волосы, красные метки на лице, чёрная форма. */
val SukunaSprite = PixelSprite(
    listOf(
        "..H....HH....H....",
        "..HH.HHHHHH.HH..H.",
        "..HHHHHHHHHHHHHHH.",
        "...HHHHHHHHHHHHH..",
        "..HHHJHHHHHHJHHH..",
        "..HHHSSSSSSSSHHH..",
        "..HHSSSSSSSSSSHH..",
        "..HHSCSkSSSSkCSHH.",
        "...SSCSCSSSSCSCS..",
        "...SSSCwSSSSwCSS..",
        "...SSSSSSSSSSSSS..",
        "...SSCSSSCCSSSCS..",
        "....SSSSSSSSSSS...",
        "....UUUUUUUUUUU...",
        "...UUUUUUUUUUUUU..",
        "...UUUUUUUUUUUUU..",
        "...UUUUUUUUUUUUU..",
        "....UUUUUUUUUUU...",
        ".....UU.....UU....",
        ".....kk.....kk...."
    )
)

/** Летящий китайский дракон: нефритовая чешуя, золотые гребни, алые усы и хвост. */
val DragonSprite = PixelSprite(
    listOf(
        ".........................y..........................",
        ".........................y...y...y..................",
        ".....................y..jjjjjjjj.y.........yy..yy...",
        ".................y...jjjjjjjjjjjjj........yy....yy..",
        ".................y.jjjjjjjjjjjjjjjjj.y....jjjjjjjj..",
        ".................jjjjjjjjjjjjjjjjjjjjj...jjjjjjjjjj.",
        "e...............jjjjjjjgggggggggjjjjjjjjjjjjkwjjjjjj",
        "ee.......y...yjjjjjjjgg........jgggjjjjjjjjjjjjjjjjj",
        "jjjj.y...y.jjjjjjjjgg..........j...ggjjjjjjjjjjeeeee",
        "eejjjjjjjjjjjjjjjgg...........y.y....ggjjjjjjjjjjjj.",
        "e.gggjjjjjjjjjggg......................g.gggjjjjjj..",
        ".....ggggggggg..............................jjjjj...",
        "............j...............................gggg....",
        "............j.......................................",
        "...........y.y......................................"
    )
)

/** Старинная китайская монета с квадратным отверстием. */
val CoinSprite = PixelSprite(
    listOf(
        "..yyyyy..",
        ".yyyyyyy.",
        "yyyyyyyyy",
        "yyykkkyyy",
        "yyyk.kyyy",
        "yyykkkyyy",
        "yyyyyyyyy",
        ".yyyyyyy.",
        "..yyyyy.."
    )
)

/** Китайский узел (中国结) с золотой серединой. */
val KnotSprite = PixelSprite(
    listOf(
        "...r...",
        "..rrr..",
        ".rryrr.",
        "rryyyrr",
        ".rryrr.",
        "..rrr..",
        ".rryrr.",
        "rryyyrr",
        ".rryrr.",
        "..rrr..",
        "...r...",
        "..r.r..",
        "..r.r.."
    )
)

/** Хлопья, которые сыплются на фон: и цветы, и искры. */
val DecorSprites: List<PixelSprite> = listOf(PetalSprite, SakuraSprite, StarSprite, SparkSprite)

/* ------------------------------ Отрисовка ------------------------------ */

/** Рисуем спрайт квадратиками: [heightPx] задаёт высоту, ширина — по пропорциям. */
fun DrawScope.drawSprite(
    sprite: PixelSprite,
    topLeft: Offset,
    heightPx: Float,
    alpha: Float = 1f,
    tint: Color? = null
) {
    val cols = sprite.cols
    val rows = sprite.height
    if (cols == 0 || rows == 0) return
    val cell = heightPx / rows
    val override = tint ?: sprite.tint
    sprite.rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, char ->
            if (char == '.') return@forEachIndexed
            val base = override ?: pixelColor(char)
            val color = base.copy(alpha = (base.alpha * alpha).coerceIn(0f, 1f))
            drawRect(
                color = color,
                topLeft = Offset(topLeft.x + x * cell, topLeft.y + y * cell),
                size = Size(cell + 0.6f, cell + 0.6f)
            )
        }
    }
}

/**
 * Пиксельный спрайт как элемент интерфейса.
 * [size] — высота картинки, ширина считается по пропорциям.
 */
@Composable
fun PixelSpriteView(
    sprite: PixelSprite,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color? = null
) {
    val ratio = sprite.aspect
    Canvas(
        modifier = modifier.size(width = size * ratio, height = size)
    ) {
        drawSprite(sprite, Offset.Zero, this.size.height, alpha, tint)
    }
}

/** Спрайт, вписанный в заданную рамку (по пропорциям, по центру). */
@Composable
fun PixelSpriteFit(
    sprite: PixelSprite,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color? = null
) {
    Canvas(modifier = modifier.size(width = width, height = height)) {
        val byHeight = this.size.height
        val byWidth = this.size.width / sprite.aspect
        val drawHeight = minOf(byHeight, byWidth)
        val drawWidth = drawHeight * sprite.aspect
        drawSprite(
            sprite = sprite,
            topLeft = Offset(
                x = (this.size.width - drawWidth) / 2f,
                y = (this.size.height - drawHeight) / 2f
            ),
            heightPx = drawHeight,
            alpha = alpha,
            tint = tint
        )
    }
}
