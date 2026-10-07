package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.srs.SrsRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentProgressionTest {

    private val a1 = Checkpoint(CefrLevel.A1, rank = 0, key = "a1")
    private val a2Presente = Checkpoint(CefrLevel.A2, rank = 0, key = "a2-presente")
    private val a2Preterito = Checkpoint(CefrLevel.A2, rank = 1, key = "a2-preterito")
    private val b1 = Checkpoint(CefrLevel.B1, rank = 0, key = "b1")
    private val c1Empty = Checkpoint(CefrLevel.C1, rank = 0, key = "c1-empty")
    private val c2Final = Checkpoint(CefrLevel.C2, rank = 0, key = "c2-final")

    private val itemsByCheckpoint = mapOf(
        a1 to listOf("a1-1", "a1-2", "a1-3", "a1-4", "a1-5"),
        a2Presente to listOf("a2p-1", "a2p-2"),
        a2Preterito to listOf("a2t-1", "a2t-2"),
        b1 to listOf("b1-1"),
        c1Empty to emptyList(), // no content assigned yet — must never block c2Final below.
        c2Final to listOf("c2-1"),
    )

    @Test
    fun `the first checkpoint is unlocked with no review history at all`() {
        assertEquals(setOf(a1), unlockedCheckpoints(itemsByCheckpoint, emptyMap()))
    }

    @Test
    fun `next checkpoint stays locked below the mastery threshold`() {
        // 3 of 5 A1 items mastered (60%) — below the 80% threshold.
        val records = masteredRecords("a1-1", "a1-2", "a1-3")
        assertEquals(setOf(a1), unlockedCheckpoints(itemsByCheckpoint, records))
    }

    @Test
    fun `merely seeing an item is not enough — it must cross the typing-readiness bar`() {
        // All 5 A1 items reviewed, but none meet isReadyForTyping's repetitions/interval bar.
        val records = ids("a1-1", "a1-2", "a1-3", "a1-4", "a1-5")
            .associateWith { SrsRecord(repetitions = 1, interval = 1) }
        assertEquals(setOf(a1), unlockedCheckpoints(itemsByCheckpoint, records))
    }

    @Test
    fun `next checkpoint unlocks once the mastery threshold is crossed`() {
        // 4 of 5 A1 items mastered (80%) — meets the threshold.
        val records = masteredRecords("a1-1", "a1-2", "a1-3", "a1-4")
        assertEquals(setOf(a1, a2Presente), unlockedCheckpoints(itemsByCheckpoint, records))
    }

    @Test
    fun `mastering a1 opens the frontier checkpoint at a2, not every checkpoint sharing a2's level`() {
        // A1 fully mastered, a2Presente NOT yet mastered — a2Preterito (rank 1, same A2 level as
        // a2Presente) must stay locked: sharing a CEFR level is not enough to unlock together,
        // only mastering the earlier-ranked checkpoint is. This is the actual fix for "I don't get
        // to master all verbs of one tense before moving onto others".
        val records = masteredRecords("a1-1", "a1-2", "a1-3", "a1-4", "a1-5")
        assertEquals(setOf(a1, a2Presente), unlockedCheckpoints(itemsByCheckpoint, records))
    }

    @Test
    fun `mastering a2Presente in turn opens a2Preterito as the new frontier`() {
        // Once a2Presente (rank 0) is itself mastered too, progression moves on to a2Preterito
        // (rank 1) — same cascade as moving to a brand new CEFR level, just within A2.
        val records = masteredRecords("a1-1", "a1-2", "a1-3", "a1-4", "a1-5", "a2p-1", "a2p-2")
        assertEquals(setOf(a1, a2Presente, a2Preterito), unlockedCheckpoints(itemsByCheckpoint, records))
    }

    @Test
    fun `an empty checkpoint auto-unlocks and never blocks what comes after it`() {
        // Everything through b1 mastered -> c1Empty (no items) and c2Final (unmastered) should
        // both open up; breadth access past an empty checkpoint doesn't wait on the next
        // checkpoint's own content being mastered, only on the one before it.
        val records = masteredRecords(
            "a1-1", "a1-2", "a1-3", "a1-4", "a1-5",
            "a2p-1", "a2p-2", "a2t-1", "a2t-2",
            "b1-1",
        )
        assertEquals(itemsByCheckpoint.keys, unlockedCheckpoints(itemsByCheckpoint, records))
    }

    private fun ids(vararg ids: String): List<String> = ids.toList()

    private fun masteredRecords(vararg ids: String): Map<String, SrsRecord> =
        ids.associateWith { SrsRecord(repetitions = 3, interval = 6) }
}
