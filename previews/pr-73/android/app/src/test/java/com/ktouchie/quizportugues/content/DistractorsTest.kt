package com.ktouchie.quizportugues.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractorsTest {

    @Test
    fun picksThreeDistinctWrongOptions() {
        val picked = pickDistractors("dog", listOf(listOf("cat", "bird", "fish", "cow", "dog")))
        assertEquals(3, picked.size)
        assertEquals(3, picked.toSet().size)
        assertTrue("dog" !in picked)
    }

    @Test
    fun neverOffersTheSameTranslationTwice() {
        // sete e meia and dezanove e trinta are both "seven thirty" in vocabulary.json.
        val picked = pickDistractors("eight", listOf(listOf("seven thirty", "seven thirty", "Seven thirty", "nine")))
        assertEquals(listOf("nine", "seven thirty"), picked.map { it.lowercase() }.sorted())
    }

    @Test
    fun skipsCandidatesThatReadTheSameAsTheAnswer() {
        val picked = pickDistractors("seven thirty", listOf(listOf("seven thirty", "eight", "nine", "ten")))
        assertEquals(setOf("eight", "nine", "ten"), picked.toSet())
    }

    @Test
    fun takesTheFirstPoolBeforeToppingUpFromTheNext() {
        val picked = pickDistractors("red", listOf(listOf("blue", "green"), listOf("one", "two", "three")))
        assertTrue(picked.containsAll(listOf("blue", "green")))
        assertEquals(3, picked.size)
    }

    @Test
    fun returnsFewerWhenThereAreNotEnoughCandidates() {
        // "b" and "B" read the same, so only one of them is kept; which one depends on the shuffle.
        assertEquals(listOf("b"), pickDistractors("a", listOf(listOf("a", "b", "B"))).map { it.lowercase() })
    }
}
