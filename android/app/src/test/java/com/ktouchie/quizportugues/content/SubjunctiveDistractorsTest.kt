package com.ktouchie.quizportugues.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val SAMPLE_VERBS_FOR_INDICATIVE = """
{
  "vir": {
    "regular": false,
    "difficulty": "advanced",
    "english": "to come",
    "presente": ["venho", "vens", "vem", "vimos", "vêm"]
  }
}
"""

class SubjunctiveDistractorsTest {

    @Test
    fun `inferSubjectPerson recognizes explicit subject pronouns`() {
        assertEquals(1, inferSubjectPerson("Avisa quando tu chegares."))
        assertEquals(2, inferSubjectPerson("Duvido que ele diga a verdade."))
        assertEquals(0, inferSubjectPerson("Espero que eu consiga."))
    }

    @Test
    fun `inferSubjectPerson returns null when no pronoun keyword matches`() {
        assertNull(inferSubjectPerson("Avisa-me quando chegar a casa."))
    }

    @Test
    fun `indicativeDistractor looks up the presente form for the inferred person`() {
        val verbEntries = parseVerbEntries(SAMPLE_VERBS_FOR_INDICATIVE)
        assertEquals("vens", indicativeDistractor("vir", "Duvido que tu venhas amanhã.", verbEntries))
        // No pronoun keyword in this prompt — falls back to 3rd person ("vem").
        assertEquals("vem", indicativeDistractor("vir", "Quero que ele venha mais cedo.", verbEntries))
    }

    @Test
    fun `indicativeDistractor returns null for a verb with no full conjugation table`() {
        val verbEntries = parseVerbEntries(SAMPLE_VERBS_FOR_INDICATIVE)
        assertNull(indicativeDistractor("acontecer", "Espero que aconteça.", verbEntries))
    }
}
