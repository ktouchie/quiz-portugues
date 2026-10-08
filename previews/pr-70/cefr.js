/**
 * CEFR levels for verbs, tenses and vocabulary categories, and the tier-unlock rule that gates
 * Quick Practice. Ported from the Android app (content/CefrTiers.kt, content/ContentProgression.kt)
 * so both apps open up content in the same order. Hand-assigned and approximate: A1 is presente
 * plus concrete, high-frequency topics; later tiers add less frequent verbs, harder tenses and more
 * abstract topics. Every verb, tense and category must be listed: a missing one throws instead of
 * silently landing in some tier (tests/smarter_questions.test.js checks the content files against these
 * maps, and that they match the Android app's).
 *
 * @typedef {'A1'|'A2'|'B1'|'B2'|'C1'|'C2'} CefrLevel
 */

/** @type {CefrLevel[]} unlock order */
export const CEFR_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

/** Fraction of a tier's items that must have been answered correctly at least once before the
 *  next tier unlocks. */
export const TIER_UNLOCK_THRESHOLD = 0.8;

/** @type {Object.<string, CefrLevel>} */
export const VERB_CEFR_LEVEL = {
    // A1: the handful of verbs every absolute-beginner course teaches first.
    'ser': 'A1', 'estar': 'A1', 'ter': 'A1', 'ir': 'A1', 'falar': 'A1',
    // A2: still core and high-frequency, next wave.
    'comer': 'A2', 'fazer': 'A2', 'ver': 'A2', 'saber': 'A2', 'poder': 'A2', 'querer': 'A2', 'partir': 'A2',
    // B1
    'dar': 'B1', 'dizer': 'B1', 'dormir': 'B1', 'ler': 'B1', 'ouvir': 'B1', 'sair': 'B1', 'pedir': 'B1',
    // B2: less frequent or semantically narrower.
    'conseguir': 'B2', 'descer': 'B2', 'perder': 'B2', 'preferir': 'B2',
    // C1/C2: the most irregular, least frequent verbs in the current set.
    'pôr': 'C1', 'trazer': 'C1', 'vir': 'C2',
};

/**
 * How hard each tense is, whatever the verb: "eu falo" and "eu fale" are not the same difficulty.
 * An item's level is the harder of its verb's and its tense's (see verbItemLevel).
 * @type {Object.<string, CefrLevel>}
 */
export const TENSE_CEFR_LEVEL = {
    'presente': 'A1',
    'pretérito': 'A2',
    'imperfeito': 'A2',
    'imperativo': 'A2',
    'futuro': 'B1',
    'condicional': 'B1',
    'conjuntivo': 'B1',
    'pretérito mais-que-perfeito': 'B2',
    'perfeito_composto': 'B2',
    'infinitivo pessoal': 'C1',
};

/** @type {Object.<string, CefrLevel>} */
export const VOCABULARY_CATEGORY_CEFR_LEVEL = {
    // A1: closed, high-frequency sets and the phrases every beginner course opens with.
    'Números': 'A1', 'Cores': 'A1', 'Dias da Semana': 'A1', 'Meses': 'A1', 'Estações': 'A1',
    'Hora': 'A1', 'Horas do Dia': 'A1', 'Frases Comuns': 'A1', 'Parentesco': 'A1',
    // A2: everyday concrete nouns.
    'Animais': 'A2', 'Comida e Bebida': 'A2', 'O Corpo Humano': 'A2', 'O Rosto': 'A2', 'Anatomia': 'A2',
    'Roupa': 'A2', 'Objetos Comuns': 'A2', 'Ferramentas': 'A2', 'Direções': 'A2', 'Transporte': 'A2',
    'Lugares': 'A2',
    // B1: broader, less concrete everyday topics.
    'Ocupações': 'B1', 'Escola': 'B1', 'Tecnologia': 'B1', 'Tempo': 'B1', 'Diversos': 'B1', 'Perguntas': 'B1',
    // B2: descriptive and abstract vocabulary. Nothing is C1/C2 yet; empty tiers unlock by themselves.
    'Adjetivos': 'B2', 'Advérbios': 'B2', 'Emoções': 'B2', 'Natureza': 'B2', 'Verbos': 'B2',
};

function lookup(map, name, what) {
    const level = map[name];
    if (!level) throw new Error(`No CEFR level assigned for ${what} "${name}"`);
    return level;
}

/**
 * The harder of the verb's own level and the tense's level.
 * @param {string} verb
 * @param {string} tense
 * @returns {CefrLevel}
 */
export function verbItemLevel(verb, tense) {
    const verbLevel = lookup(VERB_CEFR_LEVEL, verb, 'verb');
    const tenseLevel = lookup(TENSE_CEFR_LEVEL, tense, 'tense');
    return CEFR_LEVELS.indexOf(verbLevel) >= CEFR_LEVELS.indexOf(tenseLevel) ? verbLevel : tenseLevel;
}

/**
 * @param {string} category
 * @returns {CefrLevel}
 */
export function vocabularyCategoryLevel(category) {
    return lookup(VOCABULARY_CATEGORY_CEFR_LEVEL, category, 'vocabulary category');
}

/**
 * Which tiers are open. A1 always is; each later tier opens once TIER_UNLOCK_THRESHOLD of the
 * previous tier's items have been answered correctly at least once (repetitions > 0, the same bar
 * as the home page's "mastered" count). An empty tier opens by itself so it never blocks the rest.
 * @param {Object.<string, string[]>} keysByLevel - every item key in the module, grouped by level
 * @param {import('./srs.js').SRSState} srsState
 * @returns {Set<CefrLevel>}
 */
export function unlockedTiers(keysByLevel, srsState) {
    const unlocked = new Set();
    for (const level of CEFR_LEVELS) {
        unlocked.add(level);
        const keys = keysByLevel[level] ?? [];
        if (keys.length === 0) continue;
        const seen = keys.filter(k => (srsState[k]?.repetitions ?? 0) > 0).length;
        if (seen / keys.length < TIER_UNLOCK_THRESHOLD) break;
    }
    return unlocked;
}
