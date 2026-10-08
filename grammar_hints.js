import { getLanguage } from './i18n.js';

/**
 * Provides pedagogical feedback hints for quiz mistakes, in the interface language (English or
 * Portuguese; same texts as the Android app's GrammarHints.kt).
 * Each function returns a hint string or null if no hint is applicable.
 */

/** @typedef {{ en: string, pt: string }} Hint */

/** @type {Object.<string, (verb: string, third: number|string) => Hint|null>} */
const VERB_HINTS = {
    'presente': (_verb, personIdx) => {
        if (personIdx === 3) {
            return {
                en: 'Watch out: the "nós" form of the present can have an accent (e.g. falamos, comemos, pedimos).',
                pt: 'Atenção: a forma "nós" no presente pode ter acento (ex: falamos, comemos, pedimos).',
            };
        }
        return {
            en: 'In the present indicative, regular -ar verbs take -o/-as/-a/-amos/-am, -er verbs -o/-es/-e/-emos/-em and -ir verbs -o/-es/-e/-imos/-em.',
            pt: 'No presente do indicativo, verbos regulares em -ar usam -o/-as/-a/-amos/-am, -er usam -o/-es/-e/-emos/-em, -ir usam -o/-es/-e/-imos/-em.',
        };
    },
    'pretérito': () => ({
        en: 'In the preterite, regular -ar verbs take -ei/-aste/-ou/-ámos/-aram; -er verbs -i/-este/-eu/-emos/-eram; -ir verbs -i/-iste/-iu/-imos/-iram.',
        pt: 'No pretérito perfeito, verbos regulares em -ar: -ei/-aste/-ou/-ámos/-aram; em -er: -i/-este/-eu/-emos/-eram; em -ir: -i/-iste/-iu/-imos/-iram.',
    }),
    'imperfeito': () => ({
        en: 'In the imperfect, -ar verbs take -ava/-avas/-ava/-ávamos/-avam; -er/-ir verbs -ia/-ias/-ia/-íamos/-iam.',
        pt: 'No imperfeito, verbos em -ar: -ava/-avas/-ava/-ávamos/-avam; em -er/-ir: -ia/-ias/-ia/-íamos/-iam.',
    }),
    'condicional': () => ({
        en: 'The conditional is the infinitive + -ia/-ias/-ia/-íamos/-iam (the same for every regular verb).',
        pt: 'O condicional forma-se com o infinitivo + -ia/-ias/-ia/-íamos/-iam (igual para todos os verbos regulares).',
    }),
    'futuro': () => ({
        en: 'The future is the infinitive + -ei/-ás/-á/-emos/-ão.',
        pt: 'O futuro forma-se com o infinitivo + -ei/-ás/-á/-emos/-ão.',
    }),
    'conjuntivo': () => ({
        en: 'The present subjunctive is built from the "eu" form of the present: -ar → -e/-es/-e/-emos/-em; -er/-ir → -a/-as/-a/-amos/-am.',
        pt: 'O conjuntivo presente forma-se a partir da 1ª pessoa do presente: -ar → -e/-es/-e/-emos/-em; -er/-ir → -a/-as/-a/-amos/-am.',
    }),
    'imperativo': (_verb, personIdx) => {
        if (personIdx === 1) {
            return {
                en: 'The "tu" imperative of regular -ar verbs is the same as the 3rd person singular present (e.g. fala!). -er/-ir verbs drop the -s (e.g. come!, parte!).',
                pt: 'O imperativo "tu" dos verbos regulares em -ar é igual ao presente 3ª pessoa singular (ex: fala!). Verbos -er/-ir perdem o -s (ex: come!, parte!).',
            };
        }
        return {
            en: 'The formal imperative uses the present subjunctive forms.',
            pt: 'O imperativo formal usa as formas do conjuntivo presente.',
        };
    },
    'infinitivo pessoal': () => ({
        en: 'The personal infinitive is the infinitive + endings: -∅/-es/-∅/-mos/-em.',
        pt: 'O infinitivo pessoal é o infinitivo + desinências: -∅/-es/-∅/-mos/-em.',
    }),
    'pretérito mais-que-perfeito': () => ({
        en: 'The compound pluperfect uses "tinha/tinhas/tinha/tínhamos/tinham" + past participle.',
        pt: 'O mais-que-perfeito composto usa "tinha/tinhas/tinha/tínhamos/tinham" + participio passado.',
    }),
    'perfeito_composto': () => ({
        en: 'The compound perfect uses "tenho/tens/tem/temos/têm" + past participle for actions repeated up to now.',
        pt: 'O perfeito composto usa "tenho/tens/tem/temos/têm" + participio passado para ações repetidas até ao presente.',
    }),
    'participios_passados': (_verb, aux) => ({
        ter: {
            en: 'With "ter", use the invariable participle (the short regular participle): -ado/-ido.',
            pt: 'Com "ter", usa-se o particípio invariável (participio curto regular): -ado/-ido.',
        },
        ser: {
            en: 'With "ser" (passive voice), the participle agrees in gender and number.',
            pt: 'Com "ser" (voz passiva), usa-se o particípio variável em género e número.',
        },
        estar: {
            en: 'With "estar" (passive of state), the participle agrees too.',
            pt: 'Com "estar" (voz passiva de estado), usa-se o particípio variável.',
        },
    })[aux] ?? null,
};

/** @type {Object.<string, Hint>} */
const GENDER_HINTS = {
    'Nomes em -or': {
        en: 'Nouns in -or make the feminine with -ora (e.g. trabalhador → trabalhadora), or -triz (e.g. ator → atriz) for Latin-derived professions.',
        pt: 'Nomes em -or formam o feminino em -ora (ex: trabalhador → trabalhadora) ou -triz (ex: ator → atriz) para profissões latinas.',
    },
    'Palavras em -ão': {
        en: 'Words in -ão have three plural patterns: -ões (campeão → campeões), -ães (cão → cães), -ãos (irmão → irmãos).',
        pt: 'Palavras em -ão têm três padrões de plural: -ões (campeão → campeões), -ães (cão → cães), -ãos (irmão → irmãos).',
    },
    'Adjetivos em -l': {
        en: 'Adjectives in -l: the plural replaces -l with -is (e.g. azul → azuis, difícil → difíceis, espanhol → espanhóis).',
        pt: 'Adjetivos em -l: o plural substitui -l por -is (ex: azul → azuis, difícil → difíceis, espanhol → espanhóis).',
    },
    'Outros adjetivos e nomes': {
        en: 'For these irregular adjectives, learn the pairs: mau/má, bom/boa, feliz/feliz (the same in both genders).',
        pt: 'Para estes adjetivos irregulares, memorize os pares: mau/má, bom/boa, feliz/feliz (invariável em género).',
    },
};

/**
 * @param {string} tense
 * @param {string} verb
 * @param {string|number} third - person index (as in the item key, so possibly a string) or auxiliary
 * @returns {string|null}
 */
export function getVerbHint(tense, verb, third) {
    const hintFn = VERB_HINTS[tense];
    if (!hintFn) return null;
    // Item keys carry the person as text ("3"); auxiliaries stay as they are.
    const param = tense === 'participios_passados' ? third : Number(third);
    return hintFn(verb, param)?.[getLanguage()] ?? null;
}

/**
 * @param {string} category
 * @returns {string|null}
 */
export function getGenderHint(category) {
    return GENDER_HINTS[category]?.[getLanguage()] ?? null;
}
