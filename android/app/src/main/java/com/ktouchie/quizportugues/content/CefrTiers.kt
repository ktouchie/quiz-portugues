package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.content.CefrLevel.A1
import com.ktouchie.quizportugues.content.CefrLevel.A2
import com.ktouchie.quizportugues.content.CefrLevel.B1
import com.ktouchie.quizportugues.content.CefrLevel.B2
import com.ktouchie.quizportugues.content.CefrLevel.C1
import com.ktouchie.quizportugues.content.CefrLevel.C2

/**
 * Assigns a CEFR level to each verb and vocabulary category, independently of the JSON content
 * files (which stay untouched — see docs/MOBILE_APP_SPEC.md §9: this is an Android-only content
 * enrichment layer, not a change to the shared verbs.json/vocabulary.json schema, so the web app's
 * existing `difficulty` field and adaptive-difficulty filter are unaffected).
 *
 * Every verb is A1 (product owner decision, after manual testing): all 26 verbs' presente forms
 * are learned before any other tense, not staged by verb frequency/difficulty across CEFR levels
 * the way [TENSE_CEFR_LEVEL] stages tenses. Verb difficulty still matters, but as the *within-A1*
 * ordering of presente itself — see [checkpointOf]'s presente-specific wave logic below, which
 * reuses each verb's own `regular`/`difficulty` content fields instead of a second hand-maintained
 * map. Every verb and category MUST still appear here, so a new entry that's missing a tag fails
 * loudly via [verbCefrLevel]/[vocabularyCategoryCefrLevel] instead of silently defaulting into a
 * tier.
 */
val VERB_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "ser" to A1, "estar" to A1, "ter" to A1, "ir" to A1, "falar" to A1,
    "comer" to A1, "fazer" to A1, "ver" to A1, "saber" to A1, "poder" to A1,
    "querer" to A1, "partir" to A1, "dar" to A1, "dizer" to A1, "dormir" to A1,
    "ler" to A1, "ouvir" to A1, "sair" to A1, "pedir" to A1, "conseguir" to A1,
    "descer" to A1, "perder" to A1, "preferir" to A1, "pôr" to A1, "trazer" to A1,
    "vir" to A1,
)

/**
 * Grammatical complexity of each conjugation tense, independent of which verb it's applied to —
 * "eu falo" (A1 verb, presente) and "eu fale" (A1 verb, conjuntivo) are not the same difficulty.
 * An item's effective level is the *harder* of its verb and its tense (see [cefrLevelOf]), so a
 * basic verb's advanced-tense forms stay gated behind the tense's own tier rather than opening up
 * immediately just because the verb itself is elementary — this is what stops a first-ever Quick
 * Practice session from surfacing conjuntivo or mais-que-perfeito forms of "falar"/"ser".
 * Approximate, grounded in the same general EP CEFR ordering as [VERB_CEFR_LEVEL]: presente is
 * introduced first (A1); pretérito/imperfeito/imperativo next (A2); futuro, condicional, and a
 * first exposure to the conjuntivo follow (B1); the compound/less common past tenses are more
 * advanced (B2); infinitivo pessoal — a construction fairly particular to Portuguese — comes last
 * (C1). Every tense in [VERB_TENSES] MUST appear here (enforced in [CefrTiersTest]).
 */
val TENSE_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "presente" to A1,
    "pretérito" to A2,
    "imperfeito" to A2,
    "imperativo" to A2,
    "futuro" to B1,
    "condicional" to B1,
    "conjuntivo" to B1,
    "pretérito mais-que-perfeito" to B2,
    "perfeito_composto" to B2,
    "infinitivo pessoal" to C1,
)

val VOCABULARY_CATEGORY_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    // A1 — closed, high-frequency sets and the phrases every beginner course opens with.
    "Números" to A1,
    "Cores" to A1,
    "Dias da Semana" to A1,
    "Meses" to A1,
    "Estações" to A1,
    "Hora" to A1,
    "Horas do Dia" to A1,
    "Frases Comuns" to A1,
    "Parentesco" to A1,
    // A2 — everyday concrete nouns.
    "Animais" to A2,
    "Comida e Bebida" to A2,
    "O Corpo Humano" to A2,
    "O Rosto" to A2,
    "Anatomia" to A2,
    "Roupa" to A2,
    "Objetos Comuns" to A2,
    "Ferramentas" to A2,
    "Direções" to A2,
    "Transporte" to A2,
    "Lugares" to A2,
    // B1 — broader/less concrete everyday topics.
    "Ocupações" to B1,
    "Escola" to B1,
    "Tecnologia" to B1,
    "Tempo" to B1,
    "Diversos" to B1,
    "Perguntas" to B1,
    // B2 — descriptive/abstract vocabulary.
    "Adjetivos" to B2,
    "Advérbios" to B2,
    "Emoções" to B2,
    "Natureza" to B2,
    "Verbos" to B2,
    // No category is tagged C1/C2 yet — reserved for future vocabulary expansion (idiomatic and
    // register-specific sets). ContentProgression.unlockedCheckpoints() auto-unlocks an empty
    // checkpoint so this doesn't block anything.
)

/**
 * The four gender_quiz.json categories are grammatical noun/adjective patterns (irregular gender
 * and plural formation rules), not a frequency-graded vocabulary set — so unlike
 * [VOCABULARY_CATEGORY_CEFR_LEVEL], these levels reflect how early Portuguese courses typically
 * introduce each pattern rather than word frequency: -or/-ão gender-plural patterns are commonly
 * covered by A2, the -l adjective plural rule and the fully irregular pairs a bit later.
 */
val GENDER_CATEGORY_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "Nomes em -or" to A2,
    "Palavras em -ão" to A2,
    "Adjetivos em -l" to B1,
    "Outros adjetivos e nomes" to B1,
)

/**
 * ser_estar_ficar.json's 4 categories, ordered by how early Portuguese courses tackle each
 * ser/estar distinction: identity/profession (ser) and temporary state (estar) are core A2
 * material; picking correctly between all three verbs for location, and ficar's "become"/result
 * sense, are the genuinely harder discrimination the module is really testing, so B1.
 */
val SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "Profissões e identidade (ser)" to A2,
    "Estado temporário (estar)" to A2,
    "Localização de pessoas/coisas (estar vs ser vs ficar)" to B1,
    "Resultado e mudança de estado (ficar)" to B1,
)

/**
 * contractions.json's 8 categories, ordered by how early Portuguese courses introduce each
 * preposition+article/demonstrative combination: definite-article contractions with the most
 * common prepositions (de/em/a) are core A1 material; indefinite-article and the less frequent
 * "por" combination follow at A2; demonstrative contractions require knowing demonstrative
 * pronouns first, so B1/B2.
 */
val CONTRACTIONS_CATEGORY_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "de + artigo definido" to A1,
    "em + artigo definido" to A1,
    "a + artigo definido" to A1,
    "por + artigo definido" to A2,
    "em + artigo indefinido" to A2,
    "de + demonstrativo" to B1,
    "em + demonstrativo" to B1,
    "a + demonstrativo" to B2,
)

/**
 * subjunctive_quiz.json's 6 categories, per GitHub #54: subjunctive content as a whole skews
 * higher than the other modules (the mood itself isn't introduced until at least B1 in most EP
 * courses), so nothing here is tagged A1/A2. Simple will/desire and emotion triggers (the most
 * commonly taught conjuntivo presente uses) are B1; doubt/impersonal-necessity triggers and the
 * temporal/conditional conjunctions (conjuntivo pessoal, an EP-specific construction) are B2; the
 * conjuntivo imperfeito category — a distinct, more advanced verb form entirely — is C1.
 */
val SUBJUNCTIVE_CATEGORY_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    "Expressões de vontade e desejo" to B1,
    "Expressões de emoção e sentimento" to B1,
    "Expressões de dúvida e incerteza" to B2,
    "Expressões impessoais de necessidade e obrigação" to B2,
    "Conjunções temporais e condicionais" to B2,
    "Conjuntivo imperfeito (após expressões de passado)" to C1,
)

fun verbCefrLevel(verb: String): CefrLevel =
    VERB_CEFR_LEVEL[verb] ?: error("No CEFR level assigned for verb \"$verb\" — add it to VERB_CEFR_LEVEL")

fun tenseCefrLevel(tense: String): CefrLevel =
    TENSE_CEFR_LEVEL[tense] ?: error("No CEFR level assigned for tense \"$tense\" — add it to TENSE_CEFR_LEVEL")

fun vocabularyCategoryCefrLevel(category: String): CefrLevel =
    VOCABULARY_CATEGORY_CEFR_LEVEL[category]
        ?: error("No CEFR level assigned for vocabulary category \"$category\" — add it to VOCABULARY_CATEGORY_CEFR_LEVEL")

fun genderCategoryCefrLevel(category: String): CefrLevel =
    GENDER_CATEGORY_CEFR_LEVEL[category]
        ?: error("No CEFR level assigned for gender category \"$category\" — add it to GENDER_CATEGORY_CEFR_LEVEL")

fun serEstarFicarCategoryCefrLevel(category: String): CefrLevel =
    SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL[category]
        ?: error("No CEFR level assigned for ser/estar/ficar category \"$category\" — add it to SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL")

fun subjunctiveCategoryCefrLevel(category: String): CefrLevel =
    SUBJUNCTIVE_CATEGORY_CEFR_LEVEL[category]
        ?: error("No CEFR level assigned for subjunctive category \"$category\" — add it to SUBJUNCTIVE_CATEGORY_CEFR_LEVEL")

fun contractionsCategoryCefrLevel(category: String): CefrLevel =
    CONTRACTIONS_CATEGORY_CEFR_LEVEL[category]
        ?: error("No CEFR level assigned for contractions category \"$category\" — add it to CONTRACTIONS_CATEGORY_CEFR_LEVEL")

/** The harder of the verb's own level and the tense's level — see [TENSE_CEFR_LEVEL]. Every verb
 *  is A1 today, so in practice this always resolves to the tense's own level; the `max` is kept
 *  so a future re-introduction of per-verb levels doesn't need this formula rewritten. */
fun cefrLevelOf(item: VerbQuizItem): CefrLevel {
    val verbLevel = verbCefrLevel(item.verb)
    val tenseLevel = tenseCefrLevel(item.tense)
    return if (verbLevel.ordinal >= tenseLevel.ordinal) verbLevel else tenseLevel
}

fun cefrLevelOf(item: VocabularyQuizItem): CefrLevel = vocabularyCategoryCefrLevel(item.category)

fun cefrLevelOf(item: GenderQuizItem): CefrLevel = genderCategoryCefrLevel(item.category)

fun cefrLevelOf(item: SerEstarFicarQuizItem): CefrLevel = serEstarFicarCategoryCefrLevel(item.category)

fun cefrLevelOf(item: SubjunctiveQuizItem): CefrLevel = subjunctiveCategoryCefrLevel(item.category)

/**
 * indirect_speech.json has no categories to tag (a flat 20-item list) — per GitHub #55's own
 * guidance not to over-engineer tiering for a module this small, every item gets the same fixed
 * level rather than a per-category map like the other modules. B1: tense-backshift rules are
 * intermediate grammar, on par with the simpler subjunctive triggers.
 */
fun cefrLevelOf(item: IndirectSpeechQuizItem): CefrLevel = B1

fun cefrLevelOf(item: ContractionQuizItem): CefrLevel = contractionsCategoryCefrLevel(item.category)

// ── Checkpoints (docs/MOBILE_APP_SPEC.md §9) ────────────────────────────────────────────────
//
// A checkpoint's rank breaks ties between checkpoints that land on the same effective CEFR level
// — e.g. the Vocabulary module's several A2-level categories are each a separate checkpoint, so
// one unlocks before the next rather than all of A2 opening at once. Rank is just each tense/
// category's position in its CEFR map above (already ordered by how early Portuguese courses
// introduce it), computed once so `checkpointOf` doesn't re-scan the map per item.

private val TENSE_RANK: Map<String, Int> = TENSE_CEFR_LEVEL.keys.withIndex().associate { (i, t) -> t to i }
private val VOCABULARY_CATEGORY_RANK: Map<String, Int> =
    VOCABULARY_CATEGORY_CEFR_LEVEL.keys.withIndex().associate { (i, c) -> c to i }
private val GENDER_CATEGORY_RANK: Map<String, Int> =
    GENDER_CATEGORY_CEFR_LEVEL.keys.withIndex().associate { (i, c) -> c to i }
private val SER_ESTAR_FICAR_CATEGORY_RANK: Map<String, Int> =
    SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL.keys.withIndex().associate { (i, c) -> c to i }
private val CONTRACTIONS_CATEGORY_RANK: Map<String, Int> =
    CONTRACTIONS_CATEGORY_CEFR_LEVEL.keys.withIndex().associate { (i, c) -> c to i }
private val SUBJUNCTIVE_CATEGORY_RANK: Map<String, Int> =
    SUBJUNCTIVE_CATEGORY_CEFR_LEVEL.keys.withIndex().associate { (i, c) -> c to i }

/**
 * presente is further divided into three waves (product owner decision, after manual testing:
 * all 26 verbs should be known in presente before any other tense is introduced at all) —
 * derived from each item's own `regular`/`difficulty` content fields rather than a second
 * hand-maintained list, since `verbs.json` already carries exactly this grouping:
 *  0. **Regular model verbs** (`regular == true`: comer/falar/partir) — the -er/-ar/-ir patterns
 *     every other regular verb follows.
 *  1. **Common irregulars** (`difficulty == BEGINNER`, irregular: ser/estar/ir/ter) — the small
 *     set of highest-frequency irregular verbs every EP course front-loads.
 *  2. **Remaining verbs** (`difficulty` INTERMEDIATE/ADVANCED) — introduced gradually, still
 *     within presente, after waves 0 and 1 are mastered.
 * Every other tense stays one checkpoint per tense, as before — this only subdivides presente
 * because it's the one tense every verb's effective level now collapses into (see
 * [VERB_CEFR_LEVEL]'s note): without subdividing it, all 26 verbs' presente forms would be a
 * single checkpoint, which is too coarse to actually sequence "regular, then common irregulars,
 * then the rest."
 */
private fun presenteWaveRank(item: VerbQuizItem): Int = when {
    item.regular -> 0
    item.difficulty == Difficulty.BEGINNER -> 1
    else -> 2
}

private fun presenteWaveKey(item: VerbQuizItem): String = when (presenteWaveRank(item)) {
    0 -> "presente:regular-model"
    1 -> "presente:common-irregular"
    else -> "presente:remaining"
}

fun checkpointOf(item: VerbQuizItem): Checkpoint =
    if (item.tense == "presente") {
        Checkpoint(cefrLevelOf(item), presenteWaveRank(item), presenteWaveKey(item))
    } else {
        Checkpoint(cefrLevelOf(item), TENSE_RANK.getValue(item.tense), item.tense)
    }

fun checkpointOf(item: VocabularyQuizItem): Checkpoint =
    Checkpoint(cefrLevelOf(item), VOCABULARY_CATEGORY_RANK.getValue(item.category), item.category)

fun checkpointOf(item: GenderQuizItem): Checkpoint =
    Checkpoint(cefrLevelOf(item), GENDER_CATEGORY_RANK.getValue(item.category), item.category)

fun checkpointOf(item: SerEstarFicarQuizItem): Checkpoint =
    Checkpoint(cefrLevelOf(item), SER_ESTAR_FICAR_CATEGORY_RANK.getValue(item.category), item.category)

fun checkpointOf(item: ContractionQuizItem): Checkpoint =
    Checkpoint(cefrLevelOf(item), CONTRACTIONS_CATEGORY_RANK.getValue(item.category), item.category)

fun checkpointOf(item: SubjunctiveQuizItem): Checkpoint =
    Checkpoint(cefrLevelOf(item), SUBJUNCTIVE_CATEGORY_RANK.getValue(item.category), item.category)

/** No categories to sub-divide (see [cefrLevelOf]'s own note) — the whole module is one
 *  checkpoint, same as it was one tier. */
fun checkpointOf(item: IndirectSpeechQuizItem): Checkpoint = Checkpoint(cefrLevelOf(item), 0, "indirect_speech")
