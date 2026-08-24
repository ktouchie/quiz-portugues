package com.ktouchie.quizportugues.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringSimilarityTest {

    @Test
    fun `identical strings score 1`() {
        assertEquals(1.0, stringSimilarity("falar", "FALAR"), 0.0001)
    }

    @Test
    fun `completely different strings score low`() {
        assertTrue(stringSimilarity("gato", "computador") < 0.3)
    }

    @Test
    fun `near-miss Portuguese word pairs score highly`() {
        // The confusable-distractor example from product feedback: "irmã" (sister) is a
        // near-miss for "irmão" (brother) - a genuinely hard multiple-choice distractor.
        assertTrue(stringSimilarity("irmão", "irmã") > 0.7)
    }

    @Test
    fun `a false-friend English word scores highly against the Portuguese original`() {
        // "constipação" (a cold, in EP) vs "constipation" (its English false friend) - a
        // deliberately deceptive but genuinely confusable distractor.
        assertTrue(stringSimilarity("constipação", "constipation") > 0.6)
    }
}
