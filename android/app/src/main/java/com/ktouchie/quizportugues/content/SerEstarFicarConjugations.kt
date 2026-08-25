package com.ktouchie.quizportugues.content

/**
 * Ser/estar/ficar conjugated forms actually used across ser_estar_ficar.json's 38 items, keyed by
 * person index (matching [PERSONS] in VerbsContent.kt) — a small, hand-built lookup rather than a
 * full conjugation engine, since only presente (ser/estar) and a handful of pretérito (ficar) forms
 * appear in the current content. Backs "genuinely confusable" multiple-choice distractors
 * (docs/MOBILE_APP_SPEC.md §9, GitHub #52): the actual skill this module tests is picking the
 * *right verb*, not conjugating it, so a distractor should be the *other* verbs' form for the
 * *same grammatical person* — not a random, differently-conjugated word.
 */
private val SER_PRESENTE = mapOf(0 to "sou", 1 to "és", 2 to "é", 3 to "somos", 4 to "são")
private val ESTAR_PRESENTE = mapOf(0 to "estou", 1 to "estás", 2 to "está", 3 to "estamos", 4 to "estão")
private val FICAR_PRESENTE = mapOf(2 to "fica")
private val FICAR_PRETERITO = mapOf(0 to "fiquei", 2 to "ficou", 3 to "ficámos", 4 to "ficaram")

private enum class SerEstarFicarVerb { SER, ESTAR, FICAR }

/** (verb, person index) for every distinct answer form appearing in ser_estar_ficar.json today.
 *  New content whose answer isn't in this table simply gets no verb-aware distractor for that item
 *  (see [serEstarFicarVerbDistractors]) rather than crashing — this is a content-coverage nicety,
 *  not a correctness requirement enforced elsewhere. */
private val ANSWER_VERB_PERSON: Map<String, Pair<SerEstarFicarVerb, Int>> = buildMap {
    SER_PRESENTE.forEach { (person, form) -> put(form, SerEstarFicarVerb.SER to person) }
    ESTAR_PRESENTE.forEach { (person, form) -> put(form, SerEstarFicarVerb.ESTAR to person) }
    FICAR_PRESENTE.forEach { (person, form) -> put(form, SerEstarFicarVerb.FICAR to person) }
    FICAR_PRETERITO.forEach { (person, form) -> put(form, SerEstarFicarVerb.FICAR to person) }
}

private fun formFor(verb: SerEstarFicarVerb, person: Int): String? = when (verb) {
    SerEstarFicarVerb.SER -> SER_PRESENTE[person]
    SerEstarFicarVerb.ESTAR -> ESTAR_PRESENTE[person]
    SerEstarFicarVerb.FICAR -> FICAR_PRETERITO[person] ?: FICAR_PRESENTE[person]
}

/**
 * The other two verbs' conjugated form for the same grammatical person as [answer] — e.g. for
 * "ficou" (ficar, 3rd person), returns `["é", "está"]`. Returns an empty list for an answer not
 * covered by [ANSWER_VERB_PERSON]; the caller falls back to a plain different-answer pick in that
 * case (should not happen for the current real content).
 */
fun serEstarFicarVerbDistractors(answer: String): List<String> {
    val (verb, person) = ANSWER_VERB_PERSON[answer] ?: return emptyList()
    return SerEstarFicarVerb.entries
        .filter { it != verb }
        .mapNotNull { formFor(it, person) }
}
