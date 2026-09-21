package com.chinesegames.app.ui.game

/**
 * Настройки по умолчанию и варианты выбора для каждой игры.
 * Экран настройки партии рисует свои контролы по этому описанию,
 * а маршрут навигации передаёт выбранные значения строкой вида `q=15;opt=4;...`.
 */
data class QuizPreset(
    val kind: GameKind,
    val prompt: PromptKind,
    val answer: AnswerKind,
    val defaultQuestions: Int,
    val questionChoices: List<Int>,
    val allowOptions: Boolean = false,
    val defaultOptions: Int = 4,
    val optionChoices: List<Int> = listOf(3, 4, 5),
    val allowSecondsPerQuestion: Boolean = false,
    val defaultSecondsPerQuestion: Int = 0,
    val secondChoices: List<Int> = listOf(0, 5, 7, 10),
    val allowTotalSeconds: Boolean = false,
    val defaultTotalSeconds: Int = 0,
    val totalChoices: List<Int> = listOf(30, 45, 60, 90),
    val allowMemorize: Boolean = false,
    val defaultMemorize: Int = 3,
    val memorizeChoices: List<Int> = listOf(3, 4, 5),
    val allowMemorizeSeconds: Boolean = false,
    val defaultMemorizeSeconds: Int = 5,
    val memorizeSecondChoices: List<Int> = listOf(3, 5, 8),
    /** Подпись у счётчика раундов. */
    val roundsLabel: String = "Сколько слов",
    /** Настройки для карточной игры «Найди пару» (партий с сеткой). */
    val allowPairs: Boolean = false,
    val defaultPairs: Int = 10,
    val pairChoices: List<Int> = listOf(6, 8, 10, 12, 16, 20)
) {
    val kindLabel: String get() = kind.title
}

/** Значения настроек, которые едут в маршрут навигации строкой. */
object SettingsCodec {

    const val KEY_QUESTIONS = "q"
    const val KEY_OPTIONS = "opt"
    const val KEY_SECONDS = "sec"
    const val KEY_TOTAL = "tot"
    const val KEY_MEMORIZE = "mem"
    const val KEY_MEMORIZE_SECONDS = "memS"
    const val KEY_SRS = "srs"

    fun encode(values: Map<String, Any>): String =
        values.entries.joinToString(";") { (key, value) -> "$key=$value" }

    fun decode(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";")
            .mapNotNull { part ->
                val pieces = part.split("=")
                if (pieces.size != 2) null else pieces[0] to pieces[1]
            }
            .toMap()
    }

    /**
     * Настройки едут в маршрут навигации строкой. Чтобы маршрут оставался
     * простым путём без экранирования, меняем `;` и `=` на безопасные
     * символы и возвращаем их обратно при разборе.
     */
    fun toRoute(token: String): String =
        token.replace(";", "_").replace("=", "-")

    fun fromRoute(raw: String?): String =
        raw.orEmpty().replace("_", ";").replace("-", "=")

    fun int(values: Map<String, String>, key: String, fallback: Int): Int =
        values[key]?.toIntOrNull() ?: fallback

    fun bool(values: Map<String, String>, key: String, fallback: Boolean): Boolean =
        values[key]?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: fallback
}

/** Готовые настройки всех игр. */
object QuizPresets {

    fun of(kind: GameKind): QuizPreset = when (kind) {
        GameKind.MATCH -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 10,
            questionChoices = listOf(6, 8, 10, 12, 16, 20),
            allowPairs = true,
            defaultPairs = 10
        )

        GameKind.MEMORY_GRID -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 8,
            questionChoices = listOf(6, 8, 10, 12, 15),
            allowPairs = true,
            defaultPairs = 8,
            pairChoices = listOf(6, 8, 10, 12, 15),
            allowMemorizeSeconds = true,
            defaultMemorizeSeconds = 3,
            memorizeSecondChoices = listOf(2, 3, 5),
            roundsLabel = "Сколько пар"
        )

        GameKind.MEMORY_CHAIN -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 12,
            questionChoices = listOf(6, 9, 12, 15),
            allowOptions = true,
            defaultOptions = 4,
            allowMemorize = true,
            defaultMemorize = 3,
            allowMemorizeSeconds = true,
            defaultMemorizeSeconds = 5,
            roundsLabel = "Сколько слов всего"
        )

        GameKind.BUBBLE -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 15,
            questionChoices = listOf(10, 15, 20, 30),
            allowOptions = true,
            defaultOptions = 4,
            optionChoices = listOf(3, 4, 5),
            allowSecondsPerQuestion = true,
            defaultSecondsPerQuestion = 7,
            secondChoices = listOf(5, 7, 10, 0)
        )

        GameKind.FALLING -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 15,
            questionChoices = listOf(10, 15, 20, 30),
            allowOptions = true,
            defaultOptions = 4,
            optionChoices = listOf(3, 4, 5),
            allowSecondsPerQuestion = true,
            defaultSecondsPerQuestion = 6,
            secondChoices = listOf(4, 6, 8, 0)
        )

        GameKind.SPRINT -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.TRANSLATION,
            defaultQuestions = 60,
            questionChoices = listOf(60),
            allowOptions = true,
            defaultOptions = 4,
            optionChoices = listOf(3, 4, 5, 6),
            allowTotalSeconds = true,
            defaultTotalSeconds = 60,
            totalChoices = listOf(30, 45, 60, 90),
            roundsLabel = "Слов в очереди"
        )

        GameKind.AUDIO_QUIZ -> QuizPreset(
            kind = kind,
            prompt = PromptKind.AUDIO,
            answer = AnswerKind.HANZI,
            defaultQuestions = 12,
            questionChoices = listOf(8, 12, 16, 24),
            allowOptions = true,
            defaultOptions = 4,
            optionChoices = listOf(3, 4, 5, 6),
            allowSecondsPerQuestion = true,
            defaultSecondsPerQuestion = 12,
            secondChoices = listOf(0, 8, 12, 20)
        )

        GameKind.PINYIN -> QuizPreset(
            kind = kind,
            prompt = PromptKind.HANZI,
            answer = AnswerKind.HANZI,
            defaultQuestions = 12,
            questionChoices = listOf(8, 12, 16, 24),
            allowSecondsPerQuestion = true,
            defaultSecondsPerQuestion = 0,
            secondChoices = listOf(0, 10, 15, 25)
        )
    }

    /** Собираем настройки партии из строки маршрута. */
    fun buildConfig(kind: GameKind, deckIds: List<Long>, raw: String?): QuizConfig {
        val preset = of(kind)
        val values = SettingsCodec.decode(raw)
        return QuizConfig(
            kind = kind,
            deckIds = deckIds,
            questions = SettingsCodec.int(values, SettingsCodec.KEY_QUESTIONS, preset.defaultQuestions),
            options = SettingsCodec.int(values, SettingsCodec.KEY_OPTIONS, preset.defaultOptions),
            secondsPerQuestion = SettingsCodec.int(
                values, SettingsCodec.KEY_SECONDS, preset.defaultSecondsPerQuestion
            ),
            totalSeconds = SettingsCodec.int(values, SettingsCodec.KEY_TOTAL, preset.defaultTotalSeconds),
            prompt = preset.prompt,
            answer = preset.answer,
            memorizeCount = SettingsCodec.int(values, SettingsCodec.KEY_MEMORIZE, preset.defaultMemorize),
            memorizeSeconds = SettingsCodec.int(
                values, SettingsCodec.KEY_MEMORIZE_SECONDS, preset.defaultMemorizeSeconds
            ),
            srsFirst = SettingsCodec.bool(values, SettingsCodec.KEY_SRS, true)
        )
    }
}
