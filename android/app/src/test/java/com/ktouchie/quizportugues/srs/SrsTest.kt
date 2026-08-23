package com.ktouchie.quizportugues.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors tests/srs.test.js's cases, so the two SM-2 implementations stay provably in sync. */
class SrsTest {

    private fun freshItem() = SrsRecord()

    @Test
    fun `sets interval=1 on first correct answer (quality 4)`() {
        val item = sm2(freshItem(), 4)
        assertEquals(1, item.interval)
        assertEquals(1, item.repetitions)
    }

    @Test
    fun `sets interval=3 on second correct answer`() {
        var item = sm2(freshItem(), 4)
        item = sm2(item, 4)
        assertEquals(3, item.interval)
        assertEquals(2, item.repetitions)
    }

    @Test
    fun `grows interval geometrically after second repetition`() {
        var item = sm2(freshItem(), 5)
        item = sm2(item, 5)
        val prevInterval = item.interval
        item = sm2(item, 5)
        assertTrue(item.interval > prevInterval)
    }

    @Test
    fun `resets on quality less than 3`() {
        var item = sm2(freshItem(), 5)
        item = sm2(item, 5)
        item = sm2(item, 2) // fail
        assertEquals(0, item.repetitions)
        assertEquals(1, item.interval)
    }

    @Test
    fun `schedules nextReview interval days from the given now`() {
        val now = 1_700_000_000_000L
        val item = sm2(freshItem(), 4, now = now)
        assertEquals(now + 86_400_000L, item.nextReview)
    }

    @Test
    fun `keeps easeFactor greater than or equal to 1_3`() {
        var item = freshItem()
        repeat(10) { item = sm2(item, 0) }
        assertTrue(item.easeFactor >= 1.3)
    }

    @Test
    fun `isDue is true only when nextReview is in the past and set`() {
        val now = 1_700_000_000_000L
        val due = SrsRecord(nextReview = now - 86_400_000L)
        val future = SrsRecord(nextReview = now + 86_400_000L)
        val neverReviewed = SrsRecord(nextReview = 0L)

        assertTrue(isDue(due, now))
        assertTrue(!isDue(future, now))
        assertTrue(!isDue(neverReviewed, now))
    }

    @Test
    fun `dueItemIds returns only the ids that are due`() {
        val now = 1_700_000_000_000L
        val records = mapOf(
            "due" to SrsRecord(nextReview = now - 86_400_000L),
            "future" to SrsRecord(nextReview = now + 86_400_000L),
            "new" to SrsRecord(nextReview = 0L),
        )
        assertEquals(listOf("due"), dueItemIds(records, now))
    }
}
