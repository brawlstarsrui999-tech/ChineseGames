package com.chinesegames.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Прогресс курса «Поэтапное изучение».
 *
 * Хранится отдельно от обычной статистики игр: здесь важно не «сколько очков»,
 * а «какие игры группы пройдены без ошибок» — только так группа засчитывается
 * выученной, а её слова уезжают в папку «Выученное».
 */
@Entity(tableName = "hsk_group_progress")
data class HskGroupProgress(
    /** «уровень|раздел|номер группы», например `1|l1_food|0`. */
    @PrimaryKey val groupKey: String,
    val level: Int,
    val topicId: String,
    val groupIndex: Int,
    /** Битовая маска: по биту на каждую из шести игр группы. */
    val passedMask: Int = 0,
    val attempts: Int = 0,
    val bestScore: Int = 0,
    /** Группа закрыта на 100% — слова добавлены в папку «Выученное». */
    val learned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Сколько игр из шести пройдено без ошибок. */
    val passedGames: Int get() = Integer.bitCount(passedMask)

    val progress: Float get() = passedGames.toFloat() / HskCourse.GAMES_PER_GROUP
}

/** Результат экзамена по уровню HSK — открывает следующий уровень. */
@Entity(tableName = "hsk_exam")
data class HskExam(
    @PrimaryKey val level: Int,
    val passed: Boolean = false,
    val bestAccuracy: Float = 0f,
    val bestScore: Int = 0,
    val bestCorrect: Int = 0,
    val asked: Int = 0,
    val takenAt: Long = System.currentTimeMillis()
)

/** Прогресс раздела «Изучение предложений» (4 игры на раздел). */
@Entity(tableName = "hsk_sentence_progress")
data class HskSentenceProgress(
    /** «уровень|раздел», например `1|l1_hello`. */
    @PrimaryKey val topicKey: String,
    val level: Int,
    val topicId: String,
    val passedMask: Int = 0,
    val attempts: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val passedGames: Int get() = Integer.bitCount(passedMask)

    val progress: Float get() = passedGames.toFloat() / HskCourse.SENTENCE_GAMES
}

@Dao
interface HskDao {

    @Query("SELECT * FROM hsk_group_progress")
    fun observeGroups(): Flow<List<HskGroupProgress>>

    @Query("SELECT * FROM hsk_group_progress WHERE groupKey = :key")
    suspend fun getGroup(key: String): HskGroupProgress?

    /** Снимок для облачной синхронизации (не меняет схему Room). */
    @Query("SELECT * FROM hsk_group_progress")
    suspend fun allGroups(): List<HskGroupProgress>

    @Upsert
    suspend fun upsertGroup(progress: HskGroupProgress)

    @Query("SELECT * FROM hsk_exam")
    fun observeExams(): Flow<List<HskExam>>

    @Query("SELECT * FROM hsk_exam WHERE level = :level")
    suspend fun getExam(level: Int): HskExam?

    @Query("SELECT * FROM hsk_exam")
    suspend fun allExams(): List<HskExam>

    @Upsert
    suspend fun upsertExam(exam: HskExam)

    @Query("SELECT * FROM hsk_sentence_progress")
    fun observeSentenceTopics(): Flow<List<HskSentenceProgress>>

    @Query("SELECT * FROM hsk_sentence_progress WHERE topicKey = :key")
    suspend fun getSentenceTopic(key: String): HskSentenceProgress?

    @Query("SELECT * FROM hsk_sentence_progress")
    suspend fun allSentenceTopics(): List<HskSentenceProgress>

    @Upsert
    suspend fun upsertSentenceTopic(progress: HskSentenceProgress)

    @Query("SELECT COUNT(*) FROM hsk_group_progress WHERE learned = 1")
    fun observeLearnedGroups(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM hsk_group_progress WHERE learned = 1")
    suspend fun learnedGroupsCount(): Int
}
