package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GrammarHintsTest {

    @Test
    fun `presente hint differs for the nos person`() {
        val nosHint = getVerbHint("presente", personIndex = 3)
        val otherHint = getVerbHint("presente", personIndex = 0)
        assertNotNull(nosHint)
        assertNotNull(otherHint)
        assert(nosHint != otherHint)
    }

    @Test
    fun `imperativo hint differs for tu person`() {
        val tuHint = getVerbHint("imperativo", personIndex = 1)
        val otherHint = getVerbHint("imperativo", personIndex = 2)
        assert(tuHint != otherHint)
    }

    @Test
    fun `unknown tense returns null`() {
        assertNull(getVerbHint("participios_passados", personIndex = 0))
    }

    @Test
    fun `tenseLabel converts perfeito_composto only`() {
        assertEquals("perfeito composto", tenseLabel("perfeito_composto"))
        assertEquals("presente", tenseLabel("presente"))
        assertEquals("pretérito mais-que-perfeito", tenseLabel("pretérito mais-que-perfeito"))
    }

    @Test
    fun `every hint comes in English and Portuguese`() {
        val tenses = listOf(
            "presente", "pretérito", "imperfeito", "condicional", "futuro", "conjuntivo", "imperativo",
            "infinitivo pessoal", "pretérito mais-que-perfeito", "perfeito_composto",
        )
        for (tense in tenses) {
            for (person in 0..4) {
                val hint = getVerbHint(tense, person)
                assertNotNull("$tense/$person", hint)
                assert(hint!!.en.isNotBlank() && hint.pt.isNotBlank())
                assert(hint.en != hint.pt) { "$tense/$person has no English version" }
            }
        }
    }

    @Test
    fun `hints follow the chosen language`() {
        val hint = getVerbHint("futuro", personIndex = 0)!!
        assert(hint.get(AppLanguage.EN).startsWith("The future"))
        assert(hint.get(AppLanguage.PT).startsWith("O futuro"))
    }
}
