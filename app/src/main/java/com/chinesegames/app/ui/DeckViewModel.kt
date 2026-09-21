package com.chinesegames.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.CsvImportSummary
import com.chinesegames.app.data.DailyStat
import com.chinesegames.app.data.Deck
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.DeckStat
import com.chinesegames.app.data.Word
import com.chinesegames.app.data.WordStatRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Вью-модель словаря: папки, слова, избранное и общая статистика.
 */
class DeckViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    val sounds = (app as ChineseGamesApplication).sounds

    val decks: StateFlow<List<Deck>> = repo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wordCounts: StateFlow<Map<Long, Int>> = repo.wordCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val learnedCounts: StateFlow<Map<Long, Int>> = repo.learnedCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val deckStats: StateFlow<List<DeckStat>> = repo.deckStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repo.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val favoritesCount: StateFlow<Int> = repo.favoritesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hardWordCount: StateFlow<Int> = repo.hardWordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalWords: StateFlow<Int> = repo.totalWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val learnedWords: StateFlow<Int> = repo.learnedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val gamesPlayed: StateFlow<Int> = repo.gamesPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pairsFound: StateFlow<Int> = repo.pairsFound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val averageAccuracy: StateFlow<Float> = repo.averageAccuracy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val bestScore: StateFlow<Int> = repo.bestScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalSeconds: StateFlow<Int> = repo.totalSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /* ------------------------------- Папки ------------------------------- */

    fun deckFlow(deckId: Long): Flow<Deck?> = repo.deckFlow(deckId)

    fun wordsOf(deckId: Long): Flow<List<Word>> = repo.words(deckId)

    fun favoriteWords(): Flow<List<Word>> = repo.favoriteWordsFlow

    fun hardWords(limit: Int = 20): Flow<List<WordStatRow>> = repo.hardWords(limit)

    fun dailyStats(days: Int): Flow<List<DailyStat>> = repo.dailyStats(days)

    fun gamesFor(game: String): Flow<Int> = repo.gamesFor(game)

    fun bestScoreFor(game: String): Flow<Int> = repo.bestScoreFor(game)

    fun createDeck(name: String, emoji: String) {
        viewModelScope.launch { repo.createDeck(name, emoji) }
    }

    fun updateDeck(deck: Deck, name: String, emoji: String) {
        viewModelScope.launch { repo.updateDeck(deck.copy(name = name.trim(), emoji = emoji)) }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch { repo.deleteDeck(deck) }
    }

    /** Слова выбранных папок (учитывает «Избранное» и «Сложные слова»). */
    fun countForSelection(selection: List<Long>, onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(repo.countFor(selection)) }
    }

    /* ------------------------------- Слова ------------------------------- */

    fun addWord(deckId: Long, hanzi: String, pinyin: String, translation: String) {
        viewModelScope.launch { repo.addWord(deckId, hanzi, pinyin, translation) }
    }

    fun updateWord(word: Word, hanzi: String, pinyin: String, translation: String) {
        viewModelScope.launch {
            repo.updateWord(
                word.copy(
                    hanzi = hanzi.trim(),
                    pinyin = pinyin.trim(),
                    translation = translation.trim()
                )
            )
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch { repo.deleteWord(word) }
    }

    fun toggleFavorite(wordId: Long) {
        viewModelScope.launch { repo.toggleFavorite(wordId) }
    }

    /* ---------------------------- CSV-обмен ---------------------------- */

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(repo.exportCsv()) }
    }

    fun importCsv(text: String, onResult: (CsvImportSummary) -> Unit) {
        viewModelScope.launch { onResult(repo.importCsv(text)) }
    }
}
