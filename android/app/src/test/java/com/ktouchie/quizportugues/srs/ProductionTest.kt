package com.ktouchie.quizportugues.srs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionTest {

    @Test
    fun `a never-seen item is not ready for typing`() {
        assertFalse(isReadyForTyping(null))
    }

    @Test
    fun `below either threshold is not ready`() {
        assertFalse(isReadyForTyping(SrsRecord(repetitions = 2, interval = 30)))
        assertFalse(isReadyForTyping(SrsRecord(repetitions = 5, interval = 2)))
    }

    @Test
    fun `meeting both thresholds is ready`() {
        assertTrue(
            isReadyForTyping(
                SrsRecord(repetitions = PRODUCTION_MIN_REPETITIONS, interval = PRODUCTION_MIN_INTERVAL_DAYS),
            ),
        )
    }

    @Test
    fun `a failed review resets repetitions and interval below the threshold`() {
        val readyRecord = SrsRecord(repetitions = 4, interval = 12, easeFactor = 2.5)
        assertTrue(isReadyForTyping(readyRecord))

        val afterWrongAnswer = sm2(readyRecord, quality = 0)
        assertFalse(isReadyForTyping(afterWrongAnswer))
    }
}
