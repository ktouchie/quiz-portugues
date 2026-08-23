package com.ktouchie.quizportugues.data

import com.ktouchie.quizportugues.gamification.StreakData
import com.ktouchie.quizportugues.gamification.checkMilestone
import com.ktouchie.quizportugues.gamification.updateStreak
import java.time.LocalDate

/**
 * Bridges the pure streak/milestone logic (gamification/Gamification.kt) to Room persistence,
 * mirroring gamification.js's public functions but backed by real tables instead of localStorage
 * blobs. Depends on [SrsRepository] because "mastered count" — the basis for milestones — is
 * fundamentally SRS state, same as in the web app's `checkMilestone()` calling
 * `getTotalMastered()` internally.
 */
class GamificationRepository(
    private val bestScoreDao: BestScoreDao,
    private val streakDao: StreakDao,
    private val seenMilestoneDao: SeenMilestoneDao,
    private val srsRepository: SrsRepository,
) {

    // ── Best score ───────────────────────────────────────────────────────────

    suspend fun getBestScore(module: String): Int =
        bestScoreDao.get(module)?.bestCorrectCount ?: 0

    /** Persists [correctCount] as the new best only if it beats the current one. */
    suspend fun recordScoreIfBest(module: String, correctCount: Int) {
        if (correctCount > getBestScore(module)) {
            bestScoreDao.upsert(BestScoreEntity(module = module, bestCorrectCount = correctCount))
        }
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    suspend fun getStreak(): StreakData = streakDao.get()?.toDomain() ?: StreakData()

    /** Mirrors `updateStreak()`: call once per completed quiz session. */
    suspend fun recordPracticeToday(today: LocalDate = LocalDate.now()): StreakData {
        val updated = updateStreak(getStreak(), today)
        streakDao.upsert(updated.toEntity())
        return updated
    }

    // ── Mastered count / milestones ─────────────────────────────────────────

    suspend fun getTotalMastered(): Int = srsRepository.countMastered()

    /** Mirrors `checkMilestone()`: returns the newly reached milestone (and marks it seen), or
     *  null if none was newly reached. Call once per completed quiz session. */
    suspend fun checkAndMarkNewMilestone(): Int? {
        val total = getTotalMastered()
        val seen = seenMilestoneDao.getAll().toSet()
        val reached = checkMilestone(total, seen) ?: return null
        seenMilestoneDao.markSeen(SeenMilestoneEntity(milestone = reached))
        return reached
    }
}

private fun StreakDataEntity.toDomain() = StreakData(
    lastPracticeDate = lastCompletedDate?.let(LocalDate::parse),
    currentStreak = currentStreak,
    longestStreak = longestStreak,
)

private fun StreakData.toEntity() = StreakDataEntity(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastCompletedDate = lastPracticeDate?.toString(), // LocalDate.toString() is ISO-8601 (YYYY-MM-DD)
)
