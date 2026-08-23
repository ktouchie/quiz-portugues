package com.ktouchie.quizportugues.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GamificationRepositoryTest {

    private fun repo(
        srsDao: FakeSrsRecordDao = FakeSrsRecordDao(),
        bestScoreDao: FakeBestScoreDao = FakeBestScoreDao(),
        streakDao: FakeStreakDao = FakeStreakDao(),
        seenMilestoneDao: FakeSeenMilestoneDao = FakeSeenMilestoneDao(),
    ) = GamificationRepository(bestScoreDao, streakDao, seenMilestoneDao, SrsRepository(srsDao))

    // ── Best score ───────────────────────────────────────────────────────────

    @Test
    fun `getBestScore is 0 when nothing recorded`() = runBlocking {
        assertEquals(0, repo().getBestScore("verbs"))
    }

    @Test
    fun `recordScoreIfBest only updates when the new score is higher`() = runBlocking {
        val dao = FakeBestScoreDao()
        val r = repo(bestScoreDao = dao)

        r.recordScoreIfBest("verbs", 10)
        assertEquals(10, r.getBestScore("verbs"))

        r.recordScoreIfBest("verbs", 5) // lower — ignored
        assertEquals(10, r.getBestScore("verbs"))

        r.recordScoreIfBest("verbs", 15) // higher — updates
        assertEquals(15, r.getBestScore("verbs"))
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    @Test
    fun `getStreak defaults to zero when nothing recorded`() = runBlocking {
        val streak = repo().getStreak()
        assertEquals(0, streak.currentStreak)
        assertNull(streak.lastPracticeDate)
    }

    @Test
    fun `recordPracticeToday persists an incremented streak and round-trips through Room types`() = runBlocking {
        val streakDao = FakeStreakDao()
        val r = repo(streakDao = streakDao)
        val day1 = LocalDate.of(2026, 4, 17)
        val day2 = day1.plusDays(1)

        r.recordPracticeToday(day1)
        val second = r.recordPracticeToday(day2)

        assertEquals(2, second.currentStreak)
        assertEquals("2026-04-18", streakDao.stored?.lastCompletedDate)
        assertEquals(day2, r.getStreak().lastPracticeDate)
    }

    // ── Milestones ───────────────────────────────────────────────────────────

    @Test
    fun `checkAndMarkNewMilestone returns null when total mastered is below every threshold`() = runBlocking {
        assertNull(repo().checkAndMarkNewMilestone())
    }

    @Test
    fun `checkAndMarkNewMilestone marks and returns a newly reached threshold, only once`() = runBlocking {
        val srsDao = FakeSrsRecordDao()
        repeat(12) { i -> srsDao.records["item$i"] = SrsRecordEntity("item$i", "vocabulary", 1, 2.5, 1, 0, 4, 0) }
        val seenDao = FakeSeenMilestoneDao()
        val r = repo(srsDao = srsDao, seenMilestoneDao = seenDao)

        assertEquals(10, r.checkAndMarkNewMilestone())
        assertEquals(setOf(10), seenDao.seen)
        assertNull(r.checkAndMarkNewMilestone()) // already seen, total still 12 — nothing new
    }
}
