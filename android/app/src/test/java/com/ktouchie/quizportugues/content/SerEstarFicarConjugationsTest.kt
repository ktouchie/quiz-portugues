package com.ktouchie.quizportugues.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerEstarFicarConjugationsTest {

    private val repoRoot = File("../..").canonicalFile

    @Test
    fun `every real answer in ser_estar_ficar json has a verb-person mapping`() {
        val rawJson = File(repoRoot, "ser_estar_ficar.json").readText()
        val answers = parseSerEstarFicarEntries(rawJson).map { it.answer }.toSet()
        assertTrue("ser_estar_ficar.json not found or empty at $repoRoot", answers.isNotEmpty())

        val uncovered = answers.filter { serEstarFicarVerbDistractors(it).isEmpty() }
        assertTrue("Answers with no verb-aware distractor mapping: $uncovered", uncovered.isEmpty())
    }

    @Test
    fun `ficou (ficar, 3rd person) is distracted by the same-person ser and estar forms`() {
        assertEquals(setOf("é", "está"), serEstarFicarVerbDistractors("ficou").toSet())
    }

    @Test
    fun `sou (ser, 1st person) is distracted by the same-person estar and ficar forms`() {
        assertEquals(setOf("estou", "fiquei"), serEstarFicarVerbDistractors("sou").toSet())
    }

    @Test
    fun `an unknown answer returns no verb-aware distractors`() {
        assertTrue(serEstarFicarVerbDistractors("xyz").isEmpty())
    }
}
