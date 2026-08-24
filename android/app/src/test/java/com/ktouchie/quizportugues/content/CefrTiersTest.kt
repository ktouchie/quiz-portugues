package com.ktouchie.quizportugues.content

import java.io.File
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
    fun `every vocabulary category has a CEFR level assigned`() {
        val rawJson = File(repoRoot, "vocabulary.json").readText()
        val categories = parseVocabularyEntries(rawJson).map { it.category }.toSet()
        assertTrue("vocabulary.json not found or empty at $repoRoot", categories.isNotEmpty())

        val untagged = categories - VOCABULARY_CATEGORY_CEFR_LEVEL.keys
        assertTrue("Vocabulary categories missing a CEFR level: $untagged", untagged.isEmpty())
    }
}
