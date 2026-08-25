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
 * These are hand-assigned, not derived from an official frequency list — grounded in the general
 * shape of published EP CEFR guidance (Instituto Camões course levels; A1 = presente do
 * indicativo + concrete/high-frequency topics, A2 adds pretérito perfeito/imperfeito and everyday
 * concrete vocabulary, B1/B2 add less-frequent verbs and more abstract topics) but approximate.
 * Expected to be revisited as content grows — every verb and category MUST appear here, so a new
 * entry that's missing a tag fails loudly via [verbCefrLevel]/[vocabularyCategoryCefrLevel]
 * instead of silently defaulting into a tier.
 */
val VERB_CEFR_LEVEL: Map<String, CefrLevel> = mapOf(
    // A1 — the handful of verbs every absolute-beginner course teaches first.
    "ser" to A1,
    "estar" to A1,
    "ter" to A1,
    "ir" to A1,
    "falar" to A1,
    // A2 — still core/high-frequency, next wave.
    "comer" to A2,
    "fazer" to A2,
    "ver" to A2,
    "saber" to A2,
    "poder" to A2,
    "querer" to A2,
    "partir" to A2,
    // B1
    "dar" to B1,
    "dizer" to B1,
    "dormir" to B1,
    "ler" to B1,
    "ouvir" to B1,
    "sair" to B1,
    "pedir" to B1,
    // B2 — less frequent / semantically narrower.
    "conseguir" to B2,
    "descer" to B2,
    "perder" to B2,
    "preferir" to B2,
    // C1/C2 — thin on purpose today; the most irregular, least-frequent verbs in the current set.
    "pôr" to C1,
    "trazer" to C1,
    "vir" to C2,
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
    // register-specific sets). ContentProgression.unlockedTiers() auto-unlocks an empty tier so
    // this doesn't block anything.
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

/** The harder of the verb's own level and the tense's level — see [TENSE_CEFR_LEVEL]. */
fun cefrLevelOf(item: VerbQuizItem): CefrLevel {
    val verbLevel = verbCefrLevel(item.verb)
    val tenseLevel = tenseCefrLevel(item.tense)
    return if (verbLevel.ordinal >= tenseLevel.ordinal) verbLevel else tenseLevel
}

fun cefrLevelOf(item: VocabularyQuizItem): CefrLevel = vocabularyCategoryCefrLevel(item.category)

fun cefrLevelOf(item: GenderQuizItem): CefrLevel = genderCategoryCefrLevel(item.category)

fun cefrLevelOf(item: SerEstarFicarQuizItem): CefrLevel = serEstarFicarCategoryCefrLevel(item.category)
