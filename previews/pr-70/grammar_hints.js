/**
 * Provides pedagogical feedback hints for quiz mistakes.
 * Each function returns a hint string or null if no hint is applicable.
 */

const VERB_HINTS = {
    'presente': (verb, personIdx) => {
        if (personIdx === 3) return 'Atenção: a forma "nós" no presente pode ter acento (ex: falamos, comemos, pedimos).';
        return 'No presente do indicativo, verbos regulares em -ar usam -o/-as/-a/-amos/-am, -er usam -o/-es/-e/-emos/-em, -ir usam -o/-es/-e/-imos/-em.';
    },
    'pretérito': () =>
        'No pretérito perfeito, verbos regulares em -ar: -ei/-aste/-ou/-ámos/-aram; em -er: -i/-este/-eu/-emos/-eram; em -ir: -i/-iste/-iu/-imos/-iram.',
    'imperfeito': () =>
        'No imperfeito, verbos em -ar: -ava/-avas/-ava/-ávamos/-avam; em -er/-ir: -ia/-ias/-ia/-íamos/-iam.',
    'condicional': () =>
        'O condicional forma-se com o infinitivo + -ia/-ias/-ia/-íamos/-iam (igual para todos os verbos regulares).',
    'futuro': () =>
        'O futuro forma-se com o infinitivo + -ei/-ás/-á/-emos/-ão.',
    'conjuntivo': () =>
        'O conjuntivo presente forma-se a partir da 1ª pessoa do presente: -ar → -e/-es/-e/-emos/-em; -er/-ir → -a/-as/-a/-amos/-am.',
    'imperativo': (verb, personIdx) => {
        if (personIdx === 1) return 'O imperativo "tu" dos verbos regulares em -ar é igual ao presente 3ª pessoa singular (ex: fala!). Verbos -er/-ir perdem o -s (ex: come!, parte!).';
        return 'O imperativo formal usa as formas do conjuntivo presente.';
    },
    'infinitivo pessoal': () =>
        'O infinitivo pessoal é o infinitivo + desinências: -∅/-es/-∅/-mos/-em.',
    'pretérito mais-que-perfeito': () =>
        'O mais-que-perfeito composto usa "tinha/tinhas/tinha/tínhamos/tinham" + participio passado.',
    'perfeito_composto': () =>
        'O perfeito composto usa "tenho/tens/tem/temos/têm" + participio passado para ações repetidas até ao presente.',
    'participios_passados': (_verb, aux) => {
        const auxHints = {
            ter: 'Com "ter", usa-se o particípio invariável (participio curto regular): -ado/-ido.',
            ser: 'Com "ser" (voz passiva), usa-se o particípio variável em género e número.',
            estar: 'Com "estar" (voz passiva de estado), usa-se o particípio variável.',
        };
        return auxHints[aux] || null;
    },
};

const GENDER_HINTS = {
    'Nomes em -or': 'Nomes em -or formam o feminino em -ora (ex: trabalhador → trabalhadora) ou -triz (ex: ator → atriz) para profissões latinas.',
    'Palavras em -ão': 'Palavras em -ão têm três padrões de plural: -ões (campeão → campeões), -ães (cão → cães), -ãos (irmão → irmãos).',
    'Adjetivos em -l': 'Adjetivos em -l: o plural substitui -l por -is (ex: azul → azuis, difícil → difíceis, espanhol → espanhóis).',
    'Outros adjetivos e nomes': 'Para estes adjetivos irregulares, memorize os pares: mau/má, bom/boa, feliz/feliz (invariável em género).',
};

/**
 * @param {string} tense
 * @param {string} _verb
 * @param {string|number} thirdParam - personIdx or aux
 * @returns {string|null}
 */
export function getVerbHint(tense, _verb, thirdParam) {
    const hintFn = VERB_HINTS[tense];
    if (!hintFn) return null;
    return hintFn(_verb, thirdParam) || null;
}

/**
 * @param {string} category
 * @returns {string|null}
 */
export function getGenderHint(category) {
    return GENDER_HINTS[category] || null;
}
