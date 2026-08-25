package com.ktouchie.quizportugues.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reads the real repo-root verbs.json/vocabulary.json directly (Gradle unit tests run with the
 * module directory, android/app, as the working directory — two levels up is the repo root) —
 * not the AssetManager-based [loadVerbEntries]/[loadVocabularyEntries], which need a real Android
 * runtime. This is a coverage guard, not a content test: every verb the quiz actually uses and
 * every vocabulary category must have a CEFR tag, or [unlockedTiers] can't gate it — so adding
 * new content without tagging it here should fail CI, not silently fall through.
 */
class CefrTiersTest {

    private val repoRoot = File("../..").canonicalFile

    @Test
    fun `every quizzable verb has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "verbs.json").readText()
        val verbs = parseVerbEntries(rawJson).keys
        assertTrue("verbs.json not found or empty at $repoRoot", verbs.isNotEmpty())

        val untagged = verbs - VERB_CEFR_LEVEL.keys
        assertTrue("Verbs missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `every conjugation tense has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "verbs.json").readText()
        val entries = parseVerbEntries(rawJson).values
        val tenses = entries.flatMap { it.tenses.keys }.toSet()
        assertTrue("No tenses found in verbs.json at $repoRoot", tenses.isNotEmpty())

        val untagged = tenses - TENSE_CEFR_LEVEL.keys
        assertTrue("Tenses missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `every vocabulary category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "vocabulary.json").readText()
        val categories = parseVocabularyEntries(rawJson).map { it.category }.toSet()
        assertTrue("vocabulary.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - VOCABULARY_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Vocabulary categories missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `every gender category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "gender_quiz.json").readText()
        val categories = parseGenderEntries(rawJson).map { it.category }.toSet()
        assertTrue("gender_quiz.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - GENDER_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Gender categories missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `an A1 verb in an advanced tense is gated by the tense, not the verb`() {
        // "falar" is A1, but conjuntivo is B1 — the combined item must not be treated as A1, or a
        // first-ever session could surface it before futuro/condicional/conjuntivo unlock.
        val item = verbItem(verb = "falar", tense = "conjuntivo")
        assertEquals(CefrLevel.B1, cefrLevelOf(item))
    }

    @Test
    fun `an advanced verb in the simplest tense is gated by the verb, not the tense`() {
        // "vir" is C2 even in presente, its simplest tense — a beginner still doesn't know "vir".
        val item = verbItem(verb = "vir", tense = "presente")
        assertEquals(CefrLevel.C2, cefrLevelOf(item))
    }

    private fun verbItem(verb: String, tense: String) = VerbQuizItem(
        id = verbItemId(verb, tense, 0),
        verb = verb,
        tense = tense,
        personIndex = 0,
        person = "eu",
        answer = "x",
        regular = true,
        difficulty = Difficulty.BEGINNER,
        english = "x",
        exampleSentence = null,
    )
}
