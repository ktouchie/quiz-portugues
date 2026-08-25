package com.ktouchie.quizportugues.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SAMPLE_VERBS_JSON = """
{
  "aceitar": {
    "participios_passados": { "ter": "aceitado", "ser": "aceite", "estar": "aceite" }
  },
  "comer": {
    "regular": true,
    "difficulty": "beginner",
    "english": "to eat",
    "presente": ["como", "comes", "come", "comemos", "comem"],
    "imperativo": ["", "come", "coma", "comamos", "comam"],
    "exemplos": {
      "presente": ["Eu como pão.", "Tu comes devagar.", "Ele come muito.", "Nós comemos juntos.", "Eles comem tarde."]
    }
  }
}
"""

private const val SAMPLE_VOCAB_JSON = """
{
  "Animais": { "cão": "dog", "gato": "cat" },
  "Cores": { "vermelho": "red" }
}
"""

private const val SAMPLE_GENDER_JSON = """
{
  "Palavras em -ão": [
    { "masculine": "campeão", "feminine": "campeã", "plural": "campeões", "english": "champion" },
    { "masculine": "mão", "feminine": null, "plural": "mãos", "english": "hand" }
  ]
}
"""

class ContentTest {

    @Test
    fun `parseVerbEntries skips entries with no difficulty field`() {
        val entries = parseVerbEntries(SAMPLE_VERBS_JSON)
        assertEquals(setOf("comer"), entries.keys)
    }

    @Test
    fun `parseVerbEntries parses tenses, examples, and metadata`() {
        val comer = parseVerbEntries(SAMPLE_VERBS_JSON).getValue("comer")
        assertEquals(Difficulty.BEGINNER, comer.difficulty)
        assertTrue(comer.regular)
        assertEquals("to eat", comer.english)
        assertEquals(listOf("como", "comes", "come", "comemos", "comem"), comer.tenses["presente"])
        assertEquals("Eu como pão.", comer.examples["presente"]?.get(0))
    }

    @Test
    fun `verbQuizItems skips blank forms like imperativo eu`() {
        val entries = parseVerbEntries(SAMPLE_VERBS_JSON)
        val items = verbQuizItems(entries)
        val imperativoItems = items.filter { it.tense == "imperativo" }
        assertEquals(4, imperativoItems.size) // 5 persons minus the blank "eu" slot
        assertTrue(imperativoItems.none { it.personIndex == 0 })
    }

    @Test
    fun `verbQuizItems attaches per-person example sentences`() {
        val entries = parseVerbEntries(SAMPLE_VERBS_JSON)
        val items = verbQuizItems(entries)
        val eu = items.first { it.tense == "presente" && it.personIndex == 0 }
        assertEquals("Eu como pão.", eu.exampleSentence)

        val comerNoExample = parseVerbEntries(
            SAMPLE_VERBS_JSON.replace("\"exemplos\"", "\"noExemplos\""),
        ).getValue("comer")
        assertTrue(comerNoExample.examples.isEmpty())
    }

    @Test
    fun `verb item ids are stable and match the natural key`() {
        assertEquals("comer|||presente|||0", verbItemId("comer", "presente", 0))
    }

    @Test
    fun `parseVocabularyEntries flattens categories`() {
        val entries = parseVocabularyEntries(SAMPLE_VOCAB_JSON)
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.category == "Animais" && it.portuguese == "cão" && it.english == "dog" })
    }

    @Test
    fun `vocabulary quiz items get stable ids derived from category, word, translation`() {
        val items = vocabularyQuizItems(parseVocabularyEntries(SAMPLE_VOCAB_JSON))
        val cao = items.first { it.portuguese == "cão" }
        assertEquals("Animais|||cão|||dog", cao.id)
    }

    @Test
    fun `imperativo eu slot has no exampleSentence when absent`() {
        val items = verbQuizItems(parseVerbEntries(SAMPLE_VERBS_JSON))
        val imperativoComa = items.first { it.tense == "imperativo" && it.personIndex == 2 }
        assertNull(imperativoComa.exampleSentence)
    }

    @Test
    fun `parseGenderEntries treats a null feminine field as no feminine form`() {
        val entries = parseGenderEntries(SAMPLE_GENDER_JSON)
        val mao = entries.first { it.masculine == "mão" }
        assertNull(mao.feminine)
        val campeao = entries.first { it.masculine == "campeão" }
        assertEquals("campeã", campeao.feminine)
    }

    @Test
    fun `genderQuizItems only emits a feminino item when feminine is non-null`() {
        val items = genderQuizItems(parseGenderEntries(SAMPLE_GENDER_JSON))
        assertEquals(3, items.size) // campeão: feminino + plural; mão: plural only
        assertTrue(items.none { it.masculine == "mão" && it.label == "feminino" })
        assertTrue(items.any { it.masculine == "mão" && it.label == "plural" && it.answer == "mãos" })
    }

    @Test
    fun `gender item ids are stable and derived from category, masculine, label`() {
        assertEquals("Palavras em -ão|||campeão|||feminino", genderItemId("Palavras em -ão", "campeão", "feminino"))
    }
}
