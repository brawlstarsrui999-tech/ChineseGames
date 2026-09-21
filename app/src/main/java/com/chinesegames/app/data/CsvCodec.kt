package com.chinesegames.app.data

/** Одна строка словаря для CSV. */
data class CsvRow(
    val deck: String,
    val emoji: String,
    val hanzi: String,
    val pinyin: String,
    val translation: String
)

/** Что получилось после импорта. */
data class CsvImportSummary(
    val decksCreated: Int = 0,
    val wordsAdded: Int = 0,
    val wordsSkipped: Int = 0
) {
    val isEmpty: Boolean get() = wordsAdded == 0 && decksCreated == 0
}

/**
 * Чтение и запись словаря в CSV.
 *
 * * Экспорт: `папка;эмодзи;иероглиф;пиньинь;перевод` + BOM, чтобы Excel
 *   открывал файл в UTF-8 и не превращал иероглифы в кракозябры.
 * * Импорт: разделитель (`;`, `,`, таб) определяется автоматически,
 *   кавычки и переводы строк внутри кавычек поддерживаются.
 */
object CsvCodec {

    const val DEFAULT_DECK = "Импорт"

    private const val BOM = '\uFEFF'

    fun encode(rows: List<CsvRow>): String {
        val sb = StringBuilder()
        sb.append(BOM)
        sb.append("папка;эмодзи;иероглиф;пиньинь;перевод\n")
        rows.forEach { row ->
            sb.append(field(row.deck)).append(';')
            sb.append(field(row.emoji)).append(';')
            sb.append(field(row.hanzi)).append(';')
            sb.append(field(row.pinyin)).append(';')
            sb.append(field(row.translation)).append('\n')
        }
        return sb.toString()
    }

    private fun field(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(';') || clean.contains('"')) "\"" + clean.replace("\"", "\"\"") + "\"" else clean
    }

    fun decode(text: String): List<CsvRow> {
        val clean = text.removePrefix(BOM.toString())
        val delimiter = detectDelimiter(clean)
        val records = splitRecords(clean, delimiter)
        val rows = ArrayList<CsvRow>(records.size)
        var headerSkipped = false
        records.forEach { cells ->
            if (cells.isEmpty()) return@forEach
            val values = cells.map { it.trim() }
            if (values.all { it.isEmpty() }) return@forEach
            if (!headerSkipped && looksLikeHeader(values)) {
                headerSkipped = true
                return@forEach
            }
            headerSkipped = true
            rows.add(toRow(values))
        }
        return rows
    }

    private fun toRow(values: List<String>): CsvRow {
        val v = values.map { it }
        return when {
            v.size >= 5 -> CsvRow(
                deck = v[0].ifBlank { DEFAULT_DECK },
                emoji = v[1],
                hanzi = v[2],
                pinyin = v[3],
                translation = v[4]
            )

            v.size == 4 -> CsvRow(
                deck = v[0].ifBlank { DEFAULT_DECK },
                emoji = "",
                hanzi = v[1],
                pinyin = v[2],
                translation = v[3]
            )

            v.size == 3 -> CsvRow(
                deck = DEFAULT_DECK,
                emoji = "",
                hanzi = v[0],
                pinyin = v[1],
                translation = v[2]
            )

            else -> CsvRow(
                deck = DEFAULT_DECK,
                emoji = "",
                hanzi = v.getOrElse(0) { "" },
                pinyin = "",
                translation = v.getOrElse(1) { "" }
            )
        }
    }

    private val headerWords = setOf(
        "hanzi", "иероглиф", "deck", "папка", "папки", "pinyin", "пиньинь",
        "translation", "перевод", "emoji", "эмодзи", "слово"
    )

    private fun looksLikeHeader(values: List<String>): Boolean =
        values.any { it.lowercase() in headerWords }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ';'
        val semicolons = firstLine.count { it == ';' }
        val commas = firstLine.count { it == ',' }
        val tabs = firstLine.count { it == '\t' }
        return when {
            tabs > semicolons && tabs > commas -> '\t'
            commas > semicolons -> ','
            else -> ';'
        }
    }

    /** Разбор CSV с поддержкой кавычек и переводов строк внутри поля. */
    private fun splitRecords(text: String, delimiter: Char): List<List<String>> {
        val records = ArrayList<List<String>>()
        var cells = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        cell.append('"')
                        i++
                    }

                    c == '"' -> inQuotes = false
                    else -> cell.append(c)
                }

                c == '"' -> inQuotes = true
                c == delimiter -> {
                    cells.add(cell.toString())
                    cell.setLength(0)
                }

                c == '\n' -> {
                    cells.add(cell.toString())
                    cell.setLength(0)
                    records.add(cells)
                    cells = ArrayList()
                }

                c == '\r' -> Unit
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || cells.isNotEmpty()) {
            cells.add(cell.toString())
            records.add(cells)
        }
        return records
    }
}
