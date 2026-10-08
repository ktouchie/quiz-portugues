package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/** The content files' English explanations (`hint_en`, `rule_en`) reach the app. */
class EnglishHintsTest {

    private fun read(name: String) = File("../../$name").readText()

    @Test
    fun `every ser-estar-ficar, contraction and subjunctive hint has its English version`() {
        val hinted = parseSerEstarFicarEntries(read("ser_estar_ficar.json")).map { it.hint to it.hintEn } +
            parseContractionEntries(read("contractions.json")).map { it.hint to it.hintEn } +
            parseSubjunctiveEntries(read("subjunctive_quiz.json")).map { it.hint to it.hintEn }
        for ((pt, en) in hinted.filter { it.first != null }) {
            assertNotNull("No English for \"$pt\"", en)
        }
    }

    @Test
    fun `every indirect speech rule has its English version`() {
        for (entry in parseIndirectSpeechEntries(read("indirect_speech.json"))) {
            assertNotNull("No English for \"${entry.rule}\"", entry.ruleEn)
        }
    }

    @Test
    fun `gender hints come in both languages`() {
        val hint = getGenderHint("Palavras em -ão")!!
        assert(hint.get(AppLanguage.EN).startsWith("Words in -ão"))
        assert(hint.get(AppLanguage.PT).startsWith("Palavras em -ão"))
    }

    @Test
    fun `ser-estar-ficar hints are read in both languages`() {
        val joana = parseSerEstarFicarEntries(read("ser_estar_ficar.json")).first { it.sentence.startsWith("A Joana") }
        assertEquals("Profissão permanente → ser", joana.hint)
        assertEquals("Permanent profession → ser", joana.hintEn)
    }
}
