package com.ktouchie.quizportugues.gamification

import com.ktouchie.quizportugues.srs.SrsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GamificationTest {

    private val today = LocalDate.of(2026, 4, 17)

    @Test
    fun `updateStreak is a no-op when already practiced today`() {
        val data = StreakData(lastPracticeDate = today, currentStreak = 3, longestStreak = 5)
        val updated = updateStreak(data, today)
        assertEquals(data, updated)
    }

    @Test
    fun `updateStreak increments when last practice was yesterday`() {
        val data = StreakData(lastPracticeDate = today.minusDays(1), currentStreak = 3, longestStreak = 5)
        val updated = updateStreak(data, today)
        assertEquals(4, updated.currentStreak)
        assertEquals(5, updated.longestStreak)
        assertEquals(today, updated.lastPracticeDate)
    }

    @Test
    fun `updateStreak resets to 1 when a day was missed`() {
        val data = StreakData(lastPracticeDate = today.minusDays(3), currentStreak = 10, longestStreak = 10)
        val updated = updateStreak(data, today)
        assertEquals(1, updated.currentStreak)
        assertEquals(10, updated.longestStreak) // longest streak is preserved, not reset
    }

    @Test
    fun `updateStreak on a fresh streak with no prior practice starts at 1`() {
        val updated = updateStreak(StreakData(), today)
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.longestStreak)
    }

    @Test
    fun `updateStreak raises longestStreak when currentStreak exceeds it`() {
        val data = StreakData(lastPracticeDate = today.minusDays(1), currentStreak = 5, longestStreak = 5)
        val updated = updateStreak(data, today)
        assertEquals(6, updated.currentStreak)
        assertEquals(6, updated.longestStreak)
    }

    @Test
    fun `countMastered counts only records with repetitions greater than 0`() {
        val records = listOf(
            SrsRecord(repetitions = 0),
            SrsRecord(repetitions = 1),
            SrsRecord(repetitions = 4),
        )
        assertEquals(2, countMastered(records))
    }

    @Test
    fun `checkMilestone returns the lowest unseen reached threshold`() {
        assertEquals(10, checkMilestone(totalMastered = 12, seenMilestones = emptySet()))
        assertEquals(25, checkMilestone(totalMastered = 30, seenMilestones = setOf(10)))
    }

    @Test
    fun `checkMilestone returns null when nothing new is reached`() {
        assertNull(checkMilestone(totalMastered = 12, seenMilestones = setOf(10)))
        assertNull(checkMilestone(totalMastered = 5, seenMilestones = emptySet()))
    }

    @Test
    fun `checkMilestone does not re-surface an already-seen milestone even if still the lowest reached`() {
        assertEquals(50, checkMilestone(totalMastered = 60, seenMilestones = setOf(10, 25)))
    }
}
