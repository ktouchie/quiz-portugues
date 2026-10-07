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
 * every vocabulary category must have a CEFR tag, or [unlockedCheckpoints] can't gate it — so
 * adding new content without tagging it here should fail CI, not silently fall through.
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
    fun `every ser estar ficar category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "ser_estar_ficar.json").readText()
        val categories = parseSerEstarFicarEntries(rawJson).map { it.category }.toSet()
        assertTrue("ser_estar_ficar.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Ser/estar/ficar categories missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `every contractions category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "contractions.json").readText()
        val categories = parseContractionEntries(rawJson).map { it.category }.toSet()
        assertTrue("contractions.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - CONTRACTIONS_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Contractions categories missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `every subjunctive category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "subjunctive_quiz.json").readText()
        val categories = parseSubjunctiveEntries(rawJson).map { it.category }.toSet()
        assertTrue("subjunctive_quiz.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - SUBJUNCTIVE_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Subjunctive categories missing a CEFR level: $untagged", untagged.isEmpty())
    }

    @Test
    fun `an A1 verb in an advanced tense is gated by the tense, not the verb`() {
        // "falar" is A1, but conjuntivo is B1 — the combined item must not be treated as A1, or a
        // first-ever session could surface it before futuro/condicional/conjuntivo unlock.
        val item = verbItem(verb = "falar", tense = "conjuntivo")
        assertEquals(CefrLevel.B1, cefrLevelOf(item))
    }

    @Test
    fun `every verb is A1 — all 26 are learned in presente before any other tense`() {
        // Product owner decision after manual testing: verb difficulty no longer gates content
        // breadth at all — even "vir", the most irregular/infrequent verb in the set, is A1. The
        // within-presente ordering (regular model verbs, then common irregulars, then the rest) is
        // handled entirely by checkpointOf's wave logic below, not by a per-verb CEFR level.
        val item = verbItem(verb = "vir", tense = "presente", regular = false, difficulty = Difficulty.ADVANCED)
        assertEquals(CefrLevel.A1, cefrLevelOf(item))
    }

    @Test
    fun `two tenses sharing an effective CEFR level get distinct checkpoints`() {
        // pretérito and imperfeito are both A2 tenses (every verb is A1, so this is also their
        // effective level) — checkpointOf must keep them as separate checkpoints (different rank)
        // so one tense is mastered before the other opens, not a single merged A2 bucket.
        val preterito = verbItem(verb = "falar", tense = "pretérito")
        val imperfeito = verbItem(verb = "falar", tense = "imperfeito")
        val preteritoCheckpoint = checkpointOf(preterito)
        val imperfeitoCheckpoint = checkpointOf(imperfeito)

        assertEquals(CefrLevel.A2, preteritoCheckpoint.level)
        assertEquals(CefrLevel.A2, imperfeitoCheckpoint.level)
        assertTrue("Same-level checkpoints for different tenses must not collide", preteritoCheckpoint != imperfeitoCheckpoint)
    }

    @Test
    fun `the same tense and effective level always produces the same checkpoint`() {
        val falarPreterito = verbItem(verb = "falar", tense = "pretérito")
        val tiPreterito = verbItem(verb = "ter", tense = "pretérito")
        assertEquals(checkpointOf(falarPreterito), checkpointOf(tiPreterito))
    }

    @Test
    fun `presente splits into three waves — regular models, common irregulars, then the rest`() {
        val regularModel = verbItem(verb = "comer", tense = "presente", regular = true, difficulty = Difficulty.BEGINNER)
        val commonIrregular = verbItem(verb = "ser", tense = "presente", regular = false, difficulty = Difficulty.BEGINNER)
        val remaining = verbItem(verb = "pôr", tense = "presente", regular = false, difficulty = Difficulty.ADVANCED)

        val checkpoints = listOf(regularModel, commonIrregular, remaining).map { checkpointOf(it) }
        assertEquals("All three waves stay within presente's A1 level", listOf(CefrLevel.A1, CefrLevel.A1, CefrLevel.A1), checkpoints.map { it.level })
        assertEquals(listOf(0, 1, 2), checkpoints.map { it.rank })
        assertEquals(3, checkpoints.toSet().size) // all distinct checkpoints
    }

    @Test
    fun `within a wave, different verbs share the same presente checkpoint`() {
        // Two regular-model verbs — must be the exact same checkpoint so they're practiced
        // together, not gated behind each other.
        val comer = verbItem(verb = "comer", tense = "presente", regular = true, difficulty = Difficulty.BEGINNER)
        val partir = verbItem(verb = "partir", tense = "presente", regular = true, difficulty = Difficulty.BEGINNER)
        assertEquals(checkpointOf(comer), checkpointOf(partir))
    }

    private fun verbItem(
        verb: String,
        tense: String,
        regular: Boolean = true,
        difficulty: Difficulty = Difficulty.BEGINNER,
    ) = VerbQuizItem(
        id = verbItemId(verb, tense, 0),
        verb = verb,
        tense = tense,
        personIndex = 0,
        person = "eu",
        answer = "x",
        regular = regular,
        difficulty = difficulty,
        english = "x",
        exampleSentence = null,
    )
}
