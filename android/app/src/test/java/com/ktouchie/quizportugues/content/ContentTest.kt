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

private const val SAMPLE_SER_ESTAR_FICAR_JSON = """
{
  "Profissões e identidade (ser)": [
    { "sentence": "A Joana ___ arquiteta.", "answer": "é", "hint": "Profissão permanente → ser", "english": "Joana is an architect." }
  ],
  "Resultado e mudança de estado (ficar)": [
    { "sentence": "Ela ___ muito triste.", "answer": "ficou", "hint": "Mudança de estado → ficar", "english": "She became very sad." }
  ]
}
"""

private const val SAMPLE_CONTRACTIONS_JSON = """
{
  "de + artigo definido": [
    { "parts": ["de", "o"], "answer": "do", "example": "O livro é do professor.", "english": "The book belongs to the teacher.", "hint": "de + o = do" }
  ],
  "a + demonstrativo": [
    { "parts": ["a", "aquele"], "answer": "àquele", "example": "Refiro-me àquele senhor.", "english": "I am referring to that gentleman.", "hint": "a + aquele = àquele" }
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

    @Test
    fun `parseSerEstarFicarEntries flattens categories and keeps per-item hint and english`() {
        val entries = parseSerEstarFicarEntries(SAMPLE_SER_ESTAR_FICAR_JSON)
        assertEquals(2, entries.size)
        val joana = entries.first { it.answer == "é" }
        assertEquals("A Joana ___ arquiteta.", joana.sentence)
        assertEquals("Profissão permanente → ser", joana.hint)
        assertEquals("Joana is an architect.", joana.english)
    }

    @Test
    fun `serEstarFicarQuizItems produces one item per sentence`() {
        val items = serEstarFicarQuizItems(parseSerEstarFicarEntries(SAMPLE_SER_ESTAR_FICAR_JSON))
        assertEquals(2, items.size)
        assertTrue(items.any { it.answer == "ficou" && it.category == "Resultado e mudança de estado (ficar)" })
    }

    @Test
    fun `ser estar ficar item ids are stable and derived from category and sentence`() {
        assertEquals(
            "Profissões e identidade (ser)|||A Joana ___ arquiteta.",
            serEstarFicarItemId("Profissões e identidade (ser)", "A Joana ___ arquiteta."),
        )
    }

    @Test
    fun `parseContractionEntries splits parts into prep and article`() {
        val entries = parseContractionEntries(SAMPLE_CONTRACTIONS_JSON)
        assertEquals(2, entries.size)
        val do_ = entries.first { it.answer == "do" }
        assertEquals("de", do_.prep)
        assertEquals("o", do_.article)
        assertEquals("O livro é do professor.", do_.example)
        assertEquals("de + o = do", do_.hint)
    }

    @Test
    fun `contractionQuizItems produces one item per entry`() {
        val items = contractionQuizItems(parseContractionEntries(SAMPLE_CONTRACTIONS_JSON))
        assertEquals(2, items.size)
        assertTrue(items.any { it.answer == "àquele" && it.category == "a + demonstrativo" })
    }

    @Test
    fun `contraction item ids are stable and derived from category, prep, article`() {
        assertEquals("de + artigo definido|||de|||o", contractionItemId("de + artigo definido", "de", "o"))
    }
}
