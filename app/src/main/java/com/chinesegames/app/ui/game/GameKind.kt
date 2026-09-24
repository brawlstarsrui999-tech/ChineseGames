package com.chinesegames.app.ui.game

/** Группа игр — по ней хаб игр раскладывает карточки по разделам. */
enum class GameGroup(val title: String, val emoji: String) {
    MEMORY("Память", "🧠"),
    SPEED("Скорость", "⚡"),
    LISTENING("Аудио", "🎧"),
    WRITING("Письмо", "✍️"),
    REVIEW("Повторение", "🔁")
}

/**
 * Все игры приложения. [id] используется в маршрутах навигации,
 * поэтому менять его у существующих игр нельзя.
 */
enum class GameKind(
    val id: String,
    val title: String,
    val emoji: String,
    val tagline: String,
    val group: GameGroup,
    val rules: String
) {
    MATCH(
        id = "match",
        title = "Найди пару",
        emoji = "🃏",
        tagline = "汉字 ↔ перевод, три режима",
        group = GameGroup.MEMORY,
        rules = "Открывайте по две карточки: иероглиф и его перевод. Три режима: классика, «русский → 汉字» и «аудио → 汉字»."
    ),
    MEMORY_GRID(
        id = "memory",
        title = "Мемори-сетка",
        emoji = "🧩",
        tagline = "Карточки открываются парами по памяти",
        group = GameGroup.MEMORY,
        rules = "Сначала все пары показываются на пару секунд, потом закрываются. Дальше только память: нашли пару — она остаётся открытой."
    ),
    MEMORY_CHAIN(
        id = "chain",
        title = "Memory-цепочка",
        emoji = "🧠",
        tagline = "Показали 3 слова — вспомни их",
        group = GameGroup.MEMORY,
        rules = "Приложение показывает несколько слов, потом прячет. Задача — вспомнить, какое значение было у каждого иероглифа."
    ),
    BUBBLE(
        id = "bubble",
        title = "Bubble pop",
        emoji = "🫧",
        tagline = "Лопни пузырь с правильным переводом",
        group = GameGroup.SPEED,
        rules = "Пузыри с переводами поднимаются вверх. Лопните тот, где верный перевод иероглифа, пока пузыри не улетели."
    ),
    FALLING(
        id = "falling",
        title = "Падающие слова",
        emoji = "🌠",
        tagline = "Успей выбрать перевод, пока слово летит",
        group = GameGroup.SPEED,
        rules = "Иероглиф медленно падает вниз. Выберите правильный перевод до того, как он достигнет земли."
    ),
    SPRINT(
        id = "sprint",
        title = "Спринт на время",
        emoji = "⚡",
        tagline = "Сколько слов угадаешь за 60 секунд",
        group = GameGroup.SPEED,
        rules = "60 секунд, слова летят одно за другим. Ошиблись — серия сбрасывается, но время продолжает идти."
    ),
    AUDIO_QUIZ(
        id = "audio",
        title = "Аудио-квиз",
        emoji = "🎧",
        tagline = "Слушай слово → выбирай иероглиф",
        group = GameGroup.LISTENING,
        rules = "Слово произносится вслух (нужен китайский голос TTS). Нужно выбрать иероглиф, который прозвучал."
    ),
    PINYIN(
        id = "pinyin",
        title = "Ввод пиньиня",
        emoji = "⌨️",
        tagline = "По иероглифу набери пиньинь",
        group = GameGroup.WRITING,
        rules = "Видите иероглиф — печатаете его пиньинь. Тоны можно не писать: «nihao» и «nǐ hǎo» принимаются одинаково."
    ),
    TONES(
        id = "tones",
        title = "Тренажёр тонов",
        emoji = "🎼",
        tagline = "Слушай слово → отметь тон каждого слога",
        group = GameGroup.LISTENING,
        rules = "Показан иероглиф, слово звучит вслух, пиньинь скрыт. Нажимайте тоны по порядку слогов: " +
            "nǐ hǎo → 3, 3; zǎo ān → 3, 1. Для лёгкого тона (ma в māma) есть отдельная кнопка «·»."
    ),
    HANDS_FREE(
        id = "handsfree",
        title = "Без рук",
        emoji = "🎧",
        tagline = "Карточки листаются и озвучиваются сами",
        group = GameGroup.REVIEW,
        rules = "Режим для дороги и домашних дел: каждые N секунд показывается новое слово — сначала иероглиф " +
            "с китайской озвучкой, затем карточка переворачивается и звучит перевод. Идёт по кругу, пока не выключите."
    );

    /** Игры, которые играются через «Найди пару» (сетка карточек). */
    val isCardGame: Boolean get() = this == MATCH || this == MEMORY_GRID

    /** Режим без очков и таймера — результат в статистику не пишется. */
    val isPassive: Boolean get() = this == HANDS_FREE

    companion object {
        fun fromId(id: String?): GameKind =
            entries.firstOrNull { it.id == id } ?: MATCH

        /** Порядок карточек в хабе игр. */
        val hubOrder: List<GameKind> = listOf(
            MATCH, MEMORY_GRID, MEMORY_CHAIN, BUBBLE, FALLING, SPRINT, AUDIO_QUIZ, TONES, PINYIN, HANDS_FREE
        )
    }
}
