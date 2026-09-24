package com.chinesegames.app.ui.game

import java.text.Normalizer

/** Слог пиньиня с тоном: [text] — как записан в словаре («hǎo»), [tone] — 1…4, 0 — лёгкий тон. */
data class ToneSyllable(val text: String, val tone: Int)

/**
 * Разбор пиньиня на слоги и тоны для «Тренажёра тонов».
 *
 * В словаре пиньинь записан по-разному: «nǐ hǎo», «nǐhǎo», «Xī'ān», «bǎoān»,
 * «yīdiǎnr», «dǎ / dá». Мы приводим строку к «скелету» без тонов, разбиваем
 * на допустимые слоги путунхуа (таблица из 400+ слогов) и выбираем
 * разбиение, у которого число слогов совпадает с числом иероглифов.
 * Тоновые знаки помогают: слог не может содержать два знака тона, поэтому
 * «xīān» гарантированно распадётся на «xī» + «ān».
 */
object PinyinTones {

    private const val MAX_SYLLABLE = 6

    /** Знак тона → (гласная без тона, номер тона). */
    private val toneMarks: Map<Char, Pair<Char, Int>> = buildMap {
        fun put(base: Char, marks: String) {
            marks.forEachIndexed { index, mark -> put(mark, base to index + 1) }
        }
        put('a', "āáǎà")
        put('e', "ēéěè")
        put('i', "īíǐì")
        put('o', "ōóǒò")
        put('u', "ūúǔù")
        put('v', "ǖǘǚǜ")
    }

    private val plainMarks: Map<Char, Char> = mapOf(
        'ü' to 'v', 'ê' to 'e', 'ń' to 'n', 'ň' to 'n', 'ǹ' to 'n', 'ḿ' to 'm'
    )

    /** Все слоги путунхуа без тонов (ü записано как v). */
    private val syllables: Set<String> = (
        "a ai an ang ao ba bai ban bang bao bei ben beng bi bian biao bie bin bing bo bu " +
            "ca cai can cang cao ce cen ceng cha chai chan chang chao che chen cheng chi chong chou chu " +
            "chua chuai chuan chuang chui chun chuo ci cong cou cu cuan cui cun cuo " +
            "da dai dan dang dao de dei den deng di dia dian diao die ding diu dong dou du duan dui dun duo " +
            "e ei en eng er fa fan fang fei fen feng fo fou fu " +
            "ga gai gan gang gao ge gei gen geng gong gou gu gua guai guan guang gui gun guo " +
            "ha hai han hang hao he hei hen heng hong hou hu hua huai huan huang hui hun huo " +
            "ji jia jian jiang jiao jie jin jing jiong jiu ju juan jue jun " +
            "ka kai kan kang kao ke kei ken keng kong kou ku kua kuai kuan kuang kui kun kuo " +
            "la lai lan lang lao le lei leng li lia lian liang liao lie lin ling liu lo long lou lu luan lun luo lv lve " +
            "ma mai man mang mao me mei men meng mi mian miao mie min ming miu mo mou mu " +
            "na nai nan nang nao ne nei nen neng ni nian niang niao nie nin ning niu nong nou nu nuan nuo nv nve " +
            "o ou pa pai pan pang pao pei pen peng pi pian piao pie pin ping po pou pu " +
            "qi qia qian qiang qiao qie qin qing qiong qiu qu quan que qun " +
            "ran rang rao re ren reng ri rong rou ru rua ruan rui run ruo " +
            "sa sai san sang sao se sen seng sha shai shan shang shao she shei shen sheng shi shou shu shua " +
            "shuai shuan shuang shui shun shuo si song sou su suan sui sun suo " +
            "ta tai tan tang tao te teng ti tian tiao tie ting tong tou tu tuan tui tun tuo " +
            "wa wai wan wang wei wen weng wo wu xi xia xian xiang xiao xie xin xing xiong xiu xu xuan xue xun " +
            "ya yan yang yao ye yi yin ying yo yong you yu yuan yue yun " +
            "za zai zan zang zao ze zei zen zeng zha zhai zhan zhang zhao zhe zhei zhen zheng zhi zhong zhou zhu " +
            "zhua zhuai zhuan zhuang zhui zhun zhuo zi zong zou zu zuan zui zun zuo"
        ).split(' ').filter { it.isNotBlank() }.toHashSet()

    /** Подписи тонов для кнопок и подсказок. */
    fun toneLabel(tone: Int): String = when (tone) {
        1 -> "1-й"
        2 -> "2-й"
        3 -> "3-й"
        4 -> "4-й"
        else -> "лёгкий"
    }

    fun toneMark(tone: Int): String = when (tone) {
        1 -> "ˉ"
        2 -> "ˊ"
        3 -> "ˇ"
        4 -> "ˋ"
        else -> "·"
    }

    /** Пример гласной с тоном: ā á ǎ à a. */
    fun toneSample(tone: Int): String = when (tone) {
        1 -> "ā"
        2 -> "á"
        3 -> "ǎ"
        4 -> "à"
        else -> "a"
    }

    /**
     * Разбираем слово. Возвращает `null`, если пиньинь не удалось уверенно
     * разбить на слоги (такое слово тренажёр пропускает).
     */
    fun analyze(hanzi: String, pinyin: String): List<ToneSyllable>? {
        val raw = pinyin.substringBefore('/').trim()
        if (raw.isEmpty()) return null
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC).lowercase()

        // скелет без тонов + тон каждой буквы (0 — без знака); разделители → пробел
        val skeleton = StringBuilder(normalized.length)
        val tones = IntArray(normalized.length)
        val original = StringBuilder(normalized.length)
        normalized.forEachIndexed { index, char ->
            val mark = toneMarks[char]
            when {
                mark != null -> {
                    skeleton.append(mark.first)
                    tones[index] = mark.second
                }
                plainMarks.containsKey(char) -> skeleton.append(plainMarks.getValue(char))
                char in 'a'..'z' -> skeleton.append(char)
                else -> skeleton.append(' ')
            }
            original.append(if (skeleton.last() == ' ') ' ' else char)
        }

        // куски между разделителями разбираем независимо
        val chunks = ArrayList<IntRange>()
        var start = -1
        for (index in skeleton.indices) {
            val isLetter = skeleton[index] != ' '
            if (isLetter && start < 0) start = index
            if (!isLetter && start >= 0) {
                chunks.add(start until index)
                start = -1
            }
        }
        if (start >= 0) chunks.add(start until skeleton.length)
        if (chunks.isEmpty()) return null

        var combos: List<List<ToneSyllable>> = listOf(emptyList())
        for (chunk in chunks) {
            val options = segment(skeleton, tones, original, chunk)
            if (options.isEmpty()) return null
            val merged = ArrayList<List<ToneSyllable>>()
            for (prefix in combos) {
                for (option in options) {
                    merged.add(prefix + option)
                    if (merged.size > MAX_COMBOS) break
                }
                if (merged.size > MAX_COMBOS) break
            }
            combos = merged
        }

        val expected = hanzi.count { it.isCjk() }
        if (expected == 0) return combos.firstOrNull()
        val erhua = hanzi.endsWith("儿")

        // лучший вариант: число слогов = число иероглифов (儿 может сливаться
        // с предыдущим слогом: 一点儿 → yī diǎnr), меньше «немаркированных»
        // слогов, короче; при равенстве — жадное чтение слева направо
        return combos.minByOrNull { option ->
            val last = option.last().text
            val erhuaForm = erhua && last.length > 2 && last.endsWith("r") && last != "er"
            val effective = option.size + if (erhuaForm) 1 else 0
            val mismatch = kotlin.math.abs(effective - expected)
            val unmarked = option.count { it.tone == 0 }
            mismatch * 1_000 + unmarked * 10 + option.size
        }?.takeIf { option ->
            val last = option.last().text
            val erhuaForm = erhua && last.length > 2 && last.endsWith("r") && last != "er"
            option.size + (if (erhuaForm) 1 else 0) == expected
        }
    }

    /** Все разбиения куска на слоги: сначала жадные (длинные слоги в начале). */
    private fun segment(
        skeleton: CharSequence,
        tones: IntArray,
        original: CharSequence,
        range: IntRange
    ): List<List<ToneSyllable>> {
        val result = ArrayList<List<ToneSyllable>>()
        val current = ArrayList<ToneSyllable>()
        fun recurse(position: Int) {
            if (result.size >= MAX_COMBOS) return
            if (position > range.last) {
                result.add(ArrayList(current))
                return
            }
            var end = minOf(range.last + 1, position + MAX_SYLLABLE)
            while (end > position) {
                val piece = skeleton.substring(position, end)
                var marks = 0
                var tone = 0
                for (index in position until end) {
                    if (tones[index] != 0) {
                        marks++
                        tone = tones[index]
                    }
                }
                val valid = marks <= 1 && (
                    syllables.contains(piece) ||
                        (piece.length > 2 && piece.endsWith("r") && piece != "er" &&
                            syllables.contains(piece.dropLast(1)))
                    )
                if (valid) {
                    current.add(ToneSyllable(original.substring(position, end), tone))
                    recurse(end)
                    current.removeAt(current.lastIndex)
                }
                end--
            }
        }
        recurse(range.first)
        return result
    }

    private fun Char.isCjk(): Boolean =
        this in '\u4E00'..'\u9FFF' || this in '\u3400'..'\u4DBF' || this == '〇'

    private const val MAX_COMBOS = 64
}
