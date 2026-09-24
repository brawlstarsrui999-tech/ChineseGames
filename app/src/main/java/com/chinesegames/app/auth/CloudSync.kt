package com.chinesegames.app.auth

import com.chinesegames.app.data.HskDao
import com.chinesegames.app.data.HskExam
import com.chinesegames.app.data.HskGroupProgress
import com.chinesegames.app.data.HskSentenceProgress
import com.chinesegames.app.data.PurchaseStore
import com.chinesegames.app.data.SettingsStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Результат облачной синхронизации. */
sealed class CloudSyncResult {
    data object Success : CloudSyncResult()
    data object NotSignedIn : CloudSyncResult()
    data class Error(val message: String) : CloudSyncResult()
}

/**
 * Firestore-синхронизация `users/{uid}`.
 *
 * Что переносится:
 *  * покупки: объединяются множеством — купленное на одном устройстве не теряется;
 *  * HSK-группы: passedMask объединяется OR, attempts/score берутся максимумом,
 *    learned — OR; то есть прогресс тоже не может откатиться;
 *  * HSK-экзамены и предложения — по тем же правилам;
 *  * удобные настройки: в облако пишутся при каждой ручной/авто-синхронизации.
 *
 * Слова пользовательского словаря намеренно пока не копируются: у них нет
 * устойчивого общего ID. Прогресс и покупки сохраняются полностью, а CSV
 * остаётся безопасным переносом личных слов.
 */
class CloudSync(
    /** Не создаём Firestore до успешного Firebase-входа: placeholders безопасны. */
    private val firestoreProvider: () -> FirebaseFirestore,
    private val hskDao: HskDao,
    private val purchases: PurchaseStore,
    private val settings: SettingsStore
) {
    suspend fun sync(uid: String): CloudSyncResult = try {
        val ref = firestoreProvider().collection(USERS).document(uid)
        val remote = ref.get().await().data.orEmpty()

        // 1. Сначала вбираем облачные данные в локальные, не теряя прогресс.
        purchases.importCodes(stringList(remote[KEY_PURCHASES]))
        (remote[KEY_SETTINGS] as? Map<*, *>)?.let { settings.importFromSync(stringAnyMap(it)) }

        mergeGroups(mapList(remote[KEY_GROUPS]))
        mergeExams(mapList(remote[KEY_EXAMS]))
        mergeSentenceTopics(mapList(remote[KEY_SENTENCES]))

        // 2. Считываем объединённую локальную картину и записываем обратно.
        // SetOptions.merge() оставляет место для будущих данных профиля.
        val payload = mapOf(
            "schema" to 1,
            "updatedAt" to FieldValue.serverTimestamp(),
            KEY_PURCHASES to purchases.exportCodes(),
            KEY_SETTINGS to settings.exportForSync().filterValues { it != null },
            KEY_GROUPS to hskDao.allGroups().map(::groupMap),
            KEY_EXAMS to hskDao.allExams().map(::examMap),
            KEY_SENTENCES to hskDao.allSentenceTopics().map(::sentenceMap)
        )
        ref.set(payload, SetOptions.merge()).await()
        CloudSyncResult.Success
    } catch (t: Throwable) {
        CloudSyncResult.Error(t.message ?: "Не удалось синхронизировать данные")
    }

    private suspend fun mergeGroups(remote: List<Map<String, Any?>>) {
        remote.forEach { values ->
            val key = values.string("groupKey") ?: return@forEach
            val cloud = HskGroupProgress(
                groupKey = key,
                level = values.int("level"),
                topicId = values.string("topicId").orEmpty(),
                groupIndex = values.int("groupIndex"),
                passedMask = values.int("passedMask"),
                attempts = values.int("attempts"),
                bestScore = values.int("bestScore"),
                learned = values.boolean("learned"),
                updatedAt = values.long("updatedAt")
            )
            val local = hskDao.getGroup(key)
            hskDao.upsertGroup(if (local == null) cloud else merge(local, cloud))
        }
    }

    private suspend fun mergeExams(remote: List<Map<String, Any?>>) {
        remote.forEach { values ->
            val level = values.int("level")
            if (level <= 0) return@forEach
            val cloud = HskExam(
                level = level,
                passed = values.boolean("passed"),
                bestAccuracy = values.float("bestAccuracy"),
                bestScore = values.int("bestScore"),
                bestCorrect = values.int("bestCorrect"),
                asked = values.int("asked"),
                takenAt = values.long("takenAt")
            )
            val local = hskDao.getExam(level)
            hskDao.upsertExam(if (local == null) cloud else merge(local, cloud))
        }
    }

    private suspend fun mergeSentenceTopics(remote: List<Map<String, Any?>>) {
        remote.forEach { values ->
            val key = values.string("topicKey") ?: return@forEach
            val cloud = HskSentenceProgress(
                topicKey = key,
                level = values.int("level"),
                topicId = values.string("topicId").orEmpty(),
                passedMask = values.int("passedMask"),
                attempts = values.int("attempts"),
                updatedAt = values.long("updatedAt")
            )
            val local = hskDao.getSentenceTopic(key)
            hskDao.upsertSentenceTopic(if (local == null) cloud else merge(local, cloud))
        }
    }

    private fun merge(a: HskGroupProgress, b: HskGroupProgress) = a.copy(
        // Имена разделов из локального курса надёжнее (и не зависят от версии перевода).
        passedMask = a.passedMask or b.passedMask,
        attempts = maxOf(a.attempts, b.attempts),
        bestScore = maxOf(a.bestScore, b.bestScore),
        learned = a.learned || b.learned,
        updatedAt = maxOf(a.updatedAt, b.updatedAt)
    )

    private fun merge(a: HskExam, b: HskExam) = a.copy(
        passed = a.passed || b.passed,
        bestAccuracy = maxOf(a.bestAccuracy, b.bestAccuracy),
        bestScore = maxOf(a.bestScore, b.bestScore),
        bestCorrect = maxOf(a.bestCorrect, b.bestCorrect),
        asked = maxOf(a.asked, b.asked),
        takenAt = maxOf(a.takenAt, b.takenAt)
    )

    private fun merge(a: HskSentenceProgress, b: HskSentenceProgress) = a.copy(
        passedMask = a.passedMask or b.passedMask,
        attempts = maxOf(a.attempts, b.attempts),
        updatedAt = maxOf(a.updatedAt, b.updatedAt)
    )

    private fun groupMap(value: HskGroupProgress): Map<String, Any> = mapOf(
        "groupKey" to value.groupKey,
        "level" to value.level,
        "topicId" to value.topicId,
        "groupIndex" to value.groupIndex,
        "passedMask" to value.passedMask,
        "attempts" to value.attempts,
        "bestScore" to value.bestScore,
        "learned" to value.learned,
        "updatedAt" to value.updatedAt
    )

    private fun examMap(value: HskExam): Map<String, Any> = mapOf(
        "level" to value.level,
        "passed" to value.passed,
        "bestAccuracy" to value.bestAccuracy.toDouble(),
        "bestScore" to value.bestScore,
        "bestCorrect" to value.bestCorrect,
        "asked" to value.asked,
        "takenAt" to value.takenAt
    )

    private fun sentenceMap(value: HskSentenceProgress): Map<String, Any> = mapOf(
        "topicKey" to value.topicKey,
        "level" to value.level,
        "topicId" to value.topicId,
        "passedMask" to value.passedMask,
        "attempts" to value.attempts,
        "updatedAt" to value.updatedAt
    )

    private fun mapList(value: Any?): List<Map<String, Any?>> =
        (value as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let(::stringAnyMap)
        }.orEmpty()

    private fun stringList(value: Any?): List<String> =
        (value as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private fun stringAnyMap(values: Map<*, *>): Map<String, Any?> =
        values.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }.toMap()

    private fun Map<String, Any?>.string(key: String): String? = this[key] as? String
    private fun Map<String, Any?>.int(key: String): Int = (this[key] as? Number)?.toInt() ?: 0
    private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: System.currentTimeMillis()
    private fun Map<String, Any?>.float(key: String): Float = (this[key] as? Number)?.toFloat() ?: 0f
    private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false

    private companion object {
        const val USERS = "users"
        const val KEY_PURCHASES = "purchases"
        const val KEY_SETTINGS = "settings"
        const val KEY_GROUPS = "hskGroups"
        const val KEY_EXAMS = "hskExams"
        const val KEY_SENTENCES = "hskSentenceTopics"
    }
}
