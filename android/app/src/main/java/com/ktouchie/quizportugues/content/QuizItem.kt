package com.ktouchie.quizportugues.content

/** Common interface for a single quizzable item, addressed by a stable [id] (see below). */
sealed interface QuizItem {
    val id: String
    val module: String
}

data class VerbQuizItem(
    override val id: String,
    val verb: String,
    val tense: String,
    val personIndex: Int,
    val person: String,
    val answer: String,
    val regular: Boolean,
    val difficulty: Difficulty,
    val english: String,
    val exampleSentence: String?,
) : QuizItem {
    override val module: String get() = "verbs"
}

data class VocabularyQuizItem(
    override val id: String,
    val category: String,
    val portuguese: String,
    val english: String,
) : QuizItem {
    override val module: String get() = "vocabulary"
}

/**
 * Stable content-item IDs — shared convention (docs/MOBILE_APP_SPEC.md §6.1).
 *
 * Deterministically derived from each item's natural key, matching the composite keys the web
 * app already assembles at runtime (`` `${verb}|||${tense}|||${personIdx}` `` in script.js,
 * `` `${category}|||${ptWord}|||${enWord}` `` in vocabulary_quiz.js). This isn't shared code —
 * Kotlin can't consume the web app's JS — it's the same derivation rule reimplemented
 * independently in each app, so an item's identity stays consistent regardless of which app
 * computed it.
 *
 * This is a stability contract, not an implementation detail: changing the separator or field
 * order here orphans every existing Room `srs_records` row (see the Room schema task).
 */
private const val ID_SEPARATOR = "|||"

fun verbItemId(verb: String, tense: String, personIndex: Int): String =
    "$verb$ID_SEPARATOR$tense$ID_SEPARATOR$personIndex"

fun vocabularyItemId(category: String, portuguese: String, english: String): String =
    "$category$ID_SEPARATOR$portuguese$ID_SEPARATOR$english"

/**
 * Flattens parsed [VerbEntry] data into individual quizzable conjugation items — one per
 * (verb, tense, person) combination that has a non-blank form. Mirrors `getSelectedItems()` in
 * script.js, including its skip of blank forms (e.g. imperativo has no "eu" form).
 *
 * `participios_passados` items are intentionally not included — out of scope for the v1 Verb
 * Conjugation module (docs/MOBILE_APP_SPEC.md §14).
 */
fun verbQuizItems(entries: Map<String, VerbEntry>): List<VerbQuizItem> {
    val items = mutableListOf<VerbQuizItem>()
    for (entry in entries.values) {
        for ((tense, forms) in entry.tenses) {
            forms.forEachIndexed { personIndex, answer ->
                if (answer.isBlank()) return@forEachIndexed
                items += VerbQuizItem(
                    id = verbItemId(entry.verb, tense, personIndex),
                    verb = entry.verb,
                    tense = tense,
                    personIndex = personIndex,
                    person = PERSONS[personIndex],
                    answer = answer,
                    regular = entry.regular,
                    difficulty = entry.difficulty,
                    english = entry.english,
                    exampleSentence = entry.examples[tense]?.getOrNull(personIndex),
                )
            }
        }
    }
    return items
}

/** Flattens parsed [VocabularyEntry] data into quizzable items, one per word. */
fun vocabularyQuizItems(entries: List<VocabularyEntry>): List<VocabularyQuizItem> =
    entries.map { entry ->
        VocabularyQuizItem(
            id = vocabularyItemId(entry.category, entry.portuguese, entry.english),
            category = entry.category,
            portuguese = entry.portuguese,
            english = entry.english,
        )
    }
