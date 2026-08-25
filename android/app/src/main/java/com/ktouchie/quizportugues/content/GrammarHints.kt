package com.ktouchie.quizportugues.content

/**
 * Ported from grammar_hints.js's VERB_HINTS — shown on a wrong answer alongside the correct
 * conjugation. The web app's `_verb` parameter is unused there too (every hint is generic to the
 * tense/person, not the specific verb), so it's dropped here.
 */
fun getVerbHint(tense: String, personIndex: Int): String? = when (tense) {
    "presente" -> if (personIndex == 3) {
        "Atenção: a forma \"nós\" no presente pode ter acento (ex: falamos, comemos, pedimos)."
    } else {
        "No presente do indicativo, verbos regulares em -ar usam -o/-as/-a/-amos/-am, -er usam " +
            "-o/-es/-e/-emos/-em, -ir usam -o/-es/-e/-imos/-em."
    }

    "pretérito" ->
        "No pretérito perfeito, verbos regulares em -ar: -ei/-aste/-ou/-ámos/-aram; em -er: " +
            "-i/-este/-eu/-emos/-eram; em -ir: -i/-iste/-iu/-imos/-iram."

    "imperfeito" ->
        "No imperfeito, verbos em -ar: -ava/-avas/-ava/-ávamos/-avam; em -er/-ir: " +
            "-ia/-ias/-ia/-íamos/-iam."

    "condicional" ->
        "O condicional forma-se com o infinitivo + -ia/-ias/-ia/-íamos/-iam (igual para todos " +
            "os verbos regulares)."

    "futuro" ->
        "O futuro forma-se com o infinitivo + -ei/-ás/-á/-emos/-ão."

    "conjuntivo" ->
        "O conjuntivo presente forma-se a partir da 1ª pessoa do presente: -ar → -e/-es/-e/-emos/-em; " +
            "-er/-ir → -a/-as/-a/-amos/-am."

    "imperativo" -> if (personIndex == 1) {
        "O imperativo \"tu\" dos verbos regulares em -ar é igual ao presente 3ª pessoa singular " +
            "(ex: fala!). Verbos -er/-ir perdem o -s (ex: come!, parte!)."
    } else {
        "O imperativo formal usa as formas do conjuntivo presente."
    }

    "infinitivo pessoal" ->
        "O infinitivo pessoal é o infinitivo + desinências: -∅/-es/-∅/-mos/-em."

    "pretérito mais-que-perfeito" ->
        "O mais-que-perfeito composto usa \"tinha/tinhas/tinha/tínhamos/tinham\" + participio passado."

    "perfeito_composto" ->
        "O perfeito composto usa \"tenho/tens/tem/temos/têm\" + participio passado para ações " +
            "repetidas até ao presente."

    else -> null
}

/** Mirrors config.js' TENSE_LABELS — display label for a tense key ("perfeito_composto" -> "perfeito composto"). */
fun tenseLabel(tense: String): String = if (tense == "perfeito_composto") "perfeito composto" else tense

/** Ported from grammar_hints.js's GENDER_HINTS — one hint per gender_quiz.json category. */
private val GENDER_HINTS: Map<String, String> = mapOf(
    "Nomes em -or" to "Nomes em -or formam o feminino em -ora (ex: trabalhador → trabalhadora) ou " +
        "-triz (ex: ator → atriz) para profissões latinas.",
    "Palavras em -ão" to "Palavras em -ão têm três padrões de plural: -ões (campeão → campeões), " +
        "-ães (cão → cães), -ãos (irmão → irmãos).",
    "Adjetivos em -l" to "Adjetivos em -l: o plural substitui -l por -is (ex: azul → azuis, " +
        "difícil → difíceis, espanhol → espanhóis).",
    "Outros adjetivos e nomes" to "Para estes adjetivos irregulares, memorize os pares: mau/má, " +
        "bom/boa, feliz/feliz (invariável em género).",
)

fun getGenderHint(category: String): String? = GENDER_HINTS[category]
