package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.i18n.LocalizedText

/**
 * Ported from grammar_hints.js's VERB_HINTS — shown on a wrong answer alongside the correct
 * conjugation, in both interface languages (the screen picks one). The web app's `_verb`
 * parameter is unused there too (every hint is generic to the tense/person, not the specific
 * verb), so it's dropped here.
 */
fun getVerbHint(tense: String, personIndex: Int): LocalizedText? = when (tense) {
    "presente" -> if (personIndex == 3) {
        LocalizedText(
            en = "Watch out: the present \"nós\" form has no accent (falamos, comemos, pedimos); " +
                "the preterite of -ar verbs does (falámos).",
            pt = "Atenção: no presente, a forma \"nós\" não tem acento (falamos, comemos, pedimos); " +
                "no pretérito dos verbos em -ar tem (falámos).",
        )
    } else {
        LocalizedText(
            en = "In the present indicative, regular -ar verbs take -o/-as/-a/-amos/-am, -er verbs " +
                "-o/-es/-e/-emos/-em and -ir verbs -o/-es/-e/-imos/-em.",
            pt = "No presente do indicativo, verbos regulares em -ar usam -o/-as/-a/-amos/-am, -er usam " +
                "-o/-es/-e/-emos/-em, -ir usam -o/-es/-e/-imos/-em.",
        )
    }

    "pretérito" -> LocalizedText(
        en = "In the preterite, regular -ar verbs take -ei/-aste/-ou/-ámos/-aram; -er verbs " +
            "-i/-este/-eu/-emos/-eram; -ir verbs -i/-iste/-iu/-imos/-iram.",
        pt = "No pretérito perfeito, verbos regulares em -ar: -ei/-aste/-ou/-ámos/-aram; em -er: " +
            "-i/-este/-eu/-emos/-eram; em -ir: -i/-iste/-iu/-imos/-iram.",
    )

    "imperfeito" -> LocalizedText(
        en = "In the imperfect, -ar verbs take -ava/-avas/-ava/-ávamos/-avam; -er/-ir verbs " +
            "-ia/-ias/-ia/-íamos/-iam.",
        pt = "No imperfeito, verbos em -ar: -ava/-avas/-ava/-ávamos/-avam; em -er/-ir: " +
            "-ia/-ias/-ia/-íamos/-iam.",
    )

    "condicional" -> LocalizedText(
        en = "The conditional is the infinitive + -ia/-ias/-ia/-íamos/-iam (the same for every regular verb).",
        pt = "O condicional forma-se com o infinitivo + -ia/-ias/-ia/-íamos/-iam (igual para todos " +
            "os verbos regulares).",
    )

    "futuro" -> LocalizedText(
        en = "The future is the infinitive + -ei/-ás/-á/-emos/-ão.",
        pt = "O futuro forma-se com o infinitivo + -ei/-ás/-á/-emos/-ão.",
    )

    "conjuntivo" -> LocalizedText(
        en = "The present subjunctive is built from the \"eu\" form of the present: -ar → -e/-es/-e/-emos/-em; " +
            "-er/-ir → -a/-as/-a/-amos/-am.",
        pt = "O conjuntivo presente forma-se a partir da 1ª pessoa do presente: -ar → -e/-es/-e/-emos/-em; " +
            "-er/-ir → -a/-as/-a/-amos/-am.",
    )

    "imperativo" -> if (personIndex == 1) {
        LocalizedText(
            en = "The \"tu\" imperative of regular -ar verbs is the same as the 3rd person singular present " +
                "(e.g. fala!). -er/-ir verbs drop the -s (e.g. come!, parte!).",
            pt = "O imperativo \"tu\" dos verbos regulares em -ar é igual ao presente 3ª pessoa singular " +
                "(ex: fala!). Verbos -er/-ir perdem o -s (ex: come!, parte!).",
        )
    } else {
        LocalizedText(
            en = "The formal imperative uses the present subjunctive forms.",
            pt = "O imperativo formal usa as formas do conjuntivo presente.",
        )
    }

    "infinitivo pessoal" -> LocalizedText(
        en = "The personal infinitive is the infinitive + endings: -∅/-es/-∅/-mos/-em.",
        pt = "O infinitivo pessoal é o infinitivo + desinências: -∅/-es/-∅/-mos/-em.",
    )

    "pretérito mais-que-perfeito" -> LocalizedText(
        en = "The compound pluperfect uses \"tinha/tinhas/tinha/tínhamos/tinham\" + past participle.",
        pt = "O mais-que-perfeito composto usa \"tinha/tinhas/tinha/tínhamos/tinham\" + participio passado.",
    )

    "perfeito_composto" -> LocalizedText(
        en = "The compound perfect uses \"tenho/tens/tem/temos/têm\" + past participle for actions repeated up to now.",
        pt = "O perfeito composto usa \"tenho/tens/tem/temos/têm\" + participio passado para ações " +
            "repetidas até ao presente.",
    )

    else -> null
}

/** Mirrors config.js' TENSE_LABELS — display label for a tense key ("perfeito_composto" -> "perfeito composto"). */
fun tenseLabel(tense: String): String = if (tense == "perfeito_composto") "perfeito composto" else tense
