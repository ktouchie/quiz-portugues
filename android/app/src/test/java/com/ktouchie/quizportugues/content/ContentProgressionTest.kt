package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.srs.SrsRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentProgressionTest {

    private val itemsByLevel = mapOf(
        CefrLevel.A1 to listOf("a1-1", "a1-2", "a1-3", "a1-4", "a1-5"),
        CefrLevel.A2 to listOf("a2-1", "a2-2"),
        CefrLevel.B1 to listOf("b1-1"),
        // B2/C1/C2 deliberately have no items — should never block anything.
    )

    @Test
    fun `A1 is unlocked with no review history at all`() {
        assertEquals(setOf(CefrLevel.A1), unlockedTiers(itemsByLevel, emptyMap()))
    }

    @Test
    fun `next tier stays locked below the unlock threshold`() {
        // 3 of 5 A1 items seen (60%) — below the 80% threshold.
        val records = seenRecords("a1-1", "a1-2", "a1-3")
        assertEquals(setOf(CefrLevel.A1), unlockedTiers(itemsByLevel, records))
    }

    @Test
    fun `next tier unlocks once the threshold is crossed`() {
        // 4 of 5 A1 items seen (80%) — meets the threshold.
        val records = seenRecords("a1-1", "a1-2", "a1-3", "a1-4")
        assertEquals(setOf(CefrLevel.A1, CefrLevel.A2), unlockedTiers(itemsByLevel, records))
    }

    @Test
    fun `unlocking cascades through empty tiers`() {
        // All of A1 and A2 seen, B1 has a single item also seen -> every tier including the
        // empty B2/C1/C2 ones should be unlocked.
        val records = seenRecords("a1-1", "a1-2", "a1-3", "a1-4", "a1-5", "a2-1", "a2-2", "b1-1")
        assertEquals(CefrLevel.entries.toSet(), unlockedTiers(itemsByLevel, records))
    }

    @Test
    fun `a record with zero repetitions does not count as seen`() {
        val records = mapOf(
            "a1-1" to SrsRecord(repetitions = 0),
            "a1-2" to SrsRecord(repetitions = 0),
            "a1-3" to SrsRecord(repetitions = 0),
            "a1-4" to SrsRecord(repetitions = 0),
        )
        assertEquals(setOf(CefrLevel.A1), unlockedTiers(itemsByLevel, records))
    }

    private fun seenRecords(vararg ids: String): Map<String, SrsRecord> =
        ids.associateWith { SrsRecord(repetitions = 1) }
}
