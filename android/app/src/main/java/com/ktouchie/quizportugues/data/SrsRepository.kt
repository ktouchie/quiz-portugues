package com.ktouchie.quizportugues.data

import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.sm2

/**
 * Bridges the pure SM-2 algorithm (srs/Srs.kt) to Room persistence. This is where the "record an
 * answer" flow lives: apply [sm2] to compute the new schedule, then persist it — the quiz session
 * UI (a later task) calls [recordAnswer], not [sm2] directly.
 */
class SrsRepository(private val dao: SrsRecordDao) {

    suspend fun getRecord(itemId: String): SrsRecord? = dao.get(itemId)?.toDomain()

    /** Applies [sm2] to the item's current record (or a fresh one if it's never been seen) and
     *  persists the result. Returns the updated record. */
    suspend fun recordAnswer(
        itemId: String,
        module: String,
        quality: Int,
        now: Long = System.currentTimeMillis(),
    ): SrsRecord {
        val current = getRecord(itemId) ?: SrsRecord()
        val updated = sm2(current, quality, now)
        dao.upsert(updated.toEntity(itemId = itemId, module = module, lastQuality = quality, updatedAt = now))
        return updated
    }

    suspend fun getDueItemIds(module: String, now: Long = System.currentTimeMillis()): List<String> =
        dao.getDueForModule(module, now).map { it.itemId }

    suspend fun countMastered(): Int = dao.countMastered()
}

private fun SrsRecordEntity.toDomain() = SrsRecord(
    interval = intervalDays,
    repetitions = repetitions,
    easeFactor = easeFactor,
    nextReview = nextReviewAt,
)

private fun SrsRecord.toEntity(itemId: String, module: String, lastQuality: Int, updatedAt: Long) =
    SrsRecordEntity(
        itemId = itemId,
        module = module,
        repetitions = repetitions,
        easeFactor = easeFactor,
        intervalDays = interval,
        nextReviewAt = nextReview,
        lastQuality = lastQuality,
        updatedAt = updatedAt,
    )
