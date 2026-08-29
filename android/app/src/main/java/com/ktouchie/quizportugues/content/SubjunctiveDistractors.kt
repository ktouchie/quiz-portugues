package com.ktouchie.quizportugues.content

/**
 * Multiple-choice distractor support for the Subjunctive module (docs/MOBILE_APP_SPEC.md §9,
 * GitHub #54). The two distractor sources the issue calls for:
 *
 * 1. **The same verb's indicative form** — the actual error a learner makes when they haven't
 *    internalized that the sentence needs the subjunctive. [indicativeDistractor] looks up
 *    [VerbEntry.tenses]'s presente indicative form for the subject person it can infer from the
 *    prompt text (falls back to 3rd person, the most common subject in this content, when no
 *    pronoun keyword matches); returns null when the verb isn't in the shared [VerbEntry] table at
 *    all (verbs.json only has full conjugations for a subset of verbs) or lacks a presente entry.
 * 2. **Other subjunctive tenses of the same verb** — handled directly in
 *    `SubjunctiveSessionViewModel` by matching other items with the same [SubjunctiveEntry.infinitive],
 *    since that's just "another item in the same content file," not something this file needs to
 *    know how to conjugate.
 */
private val SUBJECT_PERSON_KEYWORDS: List<Pair<Regex, Int>> = listOf(
    Regex("""\beu\b""") to 0,
    Regex("""\btu\b""") to 1,
    Regex("""\b(ele|ela|você)\b""") to 2,
    Regex("""\bnós\b""") to 3,
    Regex("""\b(eles|elas|vocês)\b""") to 4,
)

/** Best-effort guess at which of [PERSONS] the prompt's subject is, from its Portuguese subject
 *  pronoun (when the sentence uses one explicitly) — e.g. "tu" in "Avisa-me quando ___ (chegar) a
 *  casa" isn't literally present, so this returns null and the caller falls back to 3rd person. */
fun inferSubjectPerson(prompt: String): Int? {
    val lower = prompt.lowercase()
    for ((pattern, person) in SUBJECT_PERSON_KEYWORDS) {
        if (pattern.containsMatchIn(lower)) return person
    }
    return null
}

/**
 * The indicative presente form of [infinitive] for the person [inferSubjectPerson] guessed (or
 * 3rd person as a default) — see the class doc above. Returns null when [verbEntries] (loaded
 * from verbs.json) has no full conjugation for this verb, or no presente tense recorded.
 */
fun indicativeDistractor(infinitive: String, prompt: String, verbEntries: Map<String, VerbEntry>): String? {
    val forms = verbEntries[infinitive]?.tenses?.get("presente") ?: return null
    val person = inferSubjectPerson(prompt) ?: 2
    return forms.getOrNull(person)?.takeIf { it.isNotBlank() }
}
