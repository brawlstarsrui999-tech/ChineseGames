package com.chinesegames.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Папка со словами («колода»). Пользователь может создавать их без ограничений.
 *
 * Помимо пользовательских папок есть системные:
 *  - «Выученное» — туда курс складывает выученные слова;
 *  - папки курса «HSK 1» … «HSK 7» с подпапками-разделами («Еда и напитки»,
 *    «Время и погода»…). Они заполняются автоматически из материала курса
 *    (см. [HskDeckSeeder]) и содержат ровно те же слова, что и уровни курса, —
 *    чтобы играть в любую игру со словами уровня или отдельного раздела.
 */
@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String = "📚",
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Системная папка — её нельзя удалить или переименовать.
     *
     * Значение по умолчанию совпадает с миграцией 2 → 3
     * (`ALTER TABLE decks ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0`).
     */
    @ColumnInfo(defaultValue = "0")
    val isSystem: Boolean = false,
    /**
     * Родительская папка: у разделов курса это папка уровня («HSK 3»),
     * у обычных папок — `null`. Добавлено миграцией 3 → 4.
     */
    val parentId: Long? = null,
    /**
     * Ключ материала курса, из которого папка собрана:
     * `hsk:3` — уровень целиком, `hsk:3/l3_food` — раздел уровня.
     * `null` — обычная папка пользователя. Добавлено миграцией 3 → 4.
     */
    val courseKey: String? = null,
    /** Порядок среди соседей (разделы уровня идут как в курсе). */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0
) {
    /** Папка собрана из курса: слова в ней менять нельзя. */
    val isCourse: Boolean get() = courseKey != null

    /** Папка уровня курса («HSK 3») — содержит подпапки-разделы. */
    val isCourseLevel: Boolean get() = courseKey != null && parentId == null

    /** Номер уровня HSK для папок курса, иначе `null`. */
    val courseLevel: Int? get() = courseKey?.let { parseCourseLevel(it) }

    companion object {
        /** Имя системной папки для выученных слов. */
        const val LEARNED_DECK = "Выученное"
        const val LEARNED_EMOJI = "✅"

        private const val COURSE_PREFIX = "hsk:"

        /** Ключ папки уровня: `hsk:3`. */
        fun levelKey(level: Int): String = "$COURSE_PREFIX$level"

        /** Ключ папки раздела: `hsk:3/l3_food`. */
        fun topicKey(level: Int, topicId: String): String = "$COURSE_PREFIX$level/$topicId"

        /** Номер уровня из ключа курса (`hsk:3/l3_food` → 3). */
        fun parseCourseLevel(key: String): Int? {
            if (!key.startsWith(COURSE_PREFIX)) return null
            return key.removePrefix(COURSE_PREFIX).substringBefore('/').toIntOrNull()
        }

        /** Эмодзи папок уровней в словаре. */
        fun levelEmoji(level: Int): String = when (level) {
            1 -> "🌱"
            2 -> "🌿"
            3 -> "🎋"
            4 -> "🏮"
            5 -> "🐉"
            6 -> "🏯"
            else -> "🚀"
        }
    }
}

/** Сколько слов в папке (для списка папок). */
data class DeckWordCount(val deckId: Long, val count: Int)

/** Сколько слов из папки уже «выучено» (уверенно отвечали в играх). */
data class DeckLearnedCount(val deckId: Long, val count: Int)
