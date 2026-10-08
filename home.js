import { loadStreak, getTotalMastered } from './gamification.js';
import { loadSRSState, saveSRSState, getDueItems, pruneRecords } from './srs.js';
import { PERSONS, STORAGE_KEYS, TENSE_LABELS } from './config.js';

/**
 * Every item key Quick Practice can ask in the verb module: each conjugated form of each verb
 * (no participles), same as VerbQuiz.getAllItems().
 * @param {Object.<string, any>} verbs - verbs.json
 * @returns {string[]}
 */
export function verbItemKeys(verbs) {
    const keys = [];
    for (const [verb, entry] of Object.entries(verbs)) {
        for (const tense of Object.keys(TENSE_LABELS)) {
            if (!entry[tense]) continue;
            for (let personIdx = 0; personIdx < PERSONS.length; personIdx++) {
                if (entry[tense][personIdx]) keys.push(`${verb}|||${tense}|||${personIdx}`);
            }
        }
    }
    return keys;
}

/**
 * Every item key in the vocabulary module, same as VocabQuiz.getAllItems().
 * @param {Object.<string, Object.<string, string>>} vocabulary - vocabulary.json
 * @returns {string[]}
 */
export function vocabularyItemKeys(vocabulary) {
    return Object.entries(vocabulary).flatMap(([category, words]) =>
        Object.entries(words).map(([pt, en]) => `${category}|||${pt}|||${en}`));
}

/**
 * How much of a module has been answered correctly at least once, 0–100, rounded down (the same
 * figure as the app's module cards).
 * @param {string[]} itemKeys
 * @param {import('./srs.js').SRSState} srsState
 * @returns {number}
 */
export function seenPercent(itemKeys, srsState) {
    if (itemKeys.length === 0) return 0;
    const seen = itemKeys.filter(k => (srsState[k]?.repetitions ?? 0) > 0).length;
    return Math.floor((seen * 100) / itemKeys.length);
}

/** @param {string} bestScoreKey */
const srsKeyFor = (bestScoreKey) => bestScoreKey.replace('bestScore_', 'srs_');

/**
 * The home page's module cards. `progress` loads the module's content and returns its item keys;
 * modules without it show their due count only, until they get Quick Practice too. `pruneStale`
 * deletes records for items that no longer exist.
 */
export const MODULES = [
    {
        title: 'Conjugação de Verbos', icon: '🗣️', href: 'verb_quiz.html', storageKey: STORAGE_KEYS.verbs,
        progress: async () => verbItemKeys(await fetchJson('verbs.json')),
    },
    {
        title: 'Vocabulário', icon: '📚', href: 'vocabulary_quiz.html', storageKey: STORAGE_KEYS.vocab,
        progress: async () => vocabularyItemKeys(await fetchJson('vocabulary.json')),
        // Every vocabulary record should match a current word (see pruneRecords). Not verbs: the
        // Avançado setup also records participles, which progress() leaves out.
        pruneStale: true,
    },
    { title: 'Género e Plural', icon: '🔤', href: 'gender_quiz.html', storageKey: STORAGE_KEYS.gender },
    { title: 'Ser / Estar / Ficar', icon: '⚖️', href: 'ser_estar_ficar_quiz.html', storageKey: STORAGE_KEYS.serEstarFicar },
    { title: 'Contrações', icon: '🔗', href: 'contractions_quiz.html', storageKey: STORAGE_KEYS.contractions },
    { title: 'Conjuntivo', icon: '💭', href: 'subjunctive_quiz.html', storageKey: STORAGE_KEYS.subjunctive },
    { title: 'Discurso Indireto', icon: '💬', href: 'indirect_speech_quiz.html', storageKey: STORAGE_KEYS.indirectSpeech },
];

async function fetchJson(file) {
    const res = await fetch(file);
    if (!res.ok) throw new Error(`Failed to load ${file}`);
    return res.json();
}

/**
 * One module card: icon, title, due count, an optional progress bar, and a "Praticar" label.
 * @param {typeof MODULES[number]} module
 * @param {import('./srs.js').SRSState} srsState
 * @returns {HTMLAnchorElement}
 */
export function renderModuleCard(module, srsState) {
    const card = document.createElement('a');
    card.className = 'module-card';
    card.href = module.href;

    const icon = document.createElement('span');
    icon.className = 'module-icon';
    icon.textContent = module.icon;

    const body = document.createElement('span');
    body.className = 'module-body';
    const title = document.createElement('span');
    title.className = 'module-title';
    title.textContent = module.title;
    const due = document.createElement('span');
    due.className = 'module-due';
    body.append(title, due);

    const cta = document.createElement('span');
    cta.className = 'module-cta';
    cta.textContent = 'Praticar';

    card.append(icon, body, cta);
    setDueCount(card, getDueItems(srsState).length);
    return card;
}

/**
 * Shows how many items are due on a module card.
 * @param {HTMLElement} card
 * @param {number} count
 */
export function setDueCount(card, count) {
    card.querySelector('.module-due').textContent = count > 0 ? `${count} por rever` : 'Nada por rever';
}

/**
 * Due items among the module's current items only: a word whose spelling or translation changed
 * leaves its old record behind, which nothing can ever review.
 * @param {string[]} itemKeys
 * @param {import('./srs.js').SRSState} srsState
 * @returns {number}
 */
export function dueAmong(itemKeys, srsState) {
    const known = new Set(itemKeys);
    return getDueItems(srsState).filter(key => known.has(key)).length;
}

/**
 * Adds a progress bar to a module card.
 * @param {HTMLElement} card
 * @param {number} percent
 */
export function addProgressBar(card, percent) {
    const track = document.createElement('span');
    track.className = 'progress-track';
    track.setAttribute('role', 'progressbar');
    track.setAttribute('aria-valuenow', String(percent));
    track.setAttribute('aria-valuemin', '0');
    track.setAttribute('aria-valuemax', '100');
    track.setAttribute('aria-label', `${percent}% visto`);
    const fill = document.createElement('span');
    fill.className = 'progress-fill';
    fill.style.display = 'block';
    fill.style.width = `${percent}%`;
    track.appendChild(fill);
    card.querySelector('.module-body').appendChild(track);
}

/** Fills in the home page: streak, stats and module cards. */
export async function renderHome() {
    const streak = loadStreak().currentStreak;
    document.getElementById('streak-pill').textContent = `🔥 ${streak}`;
    document.getElementById('streak-count').textContent = String(streak);
    document.getElementById('mastered-count').textContent = String(getTotalMastered());

    const list = document.getElementById('module-list');
    list.textContent = '';
    const pending = MODULES.map(module => {
        const srsState = loadSRSState(srsKeyFor(module.storageKey));
        const card = renderModuleCard(module, srsState);
        list.appendChild(card);
        if (!module.progress) return null;
        return module.progress()
            .then(keys => {
                const srsKey = srsKeyFor(module.storageKey);
                if (module.pruneStale && keys.length > 0 && pruneRecords(srsState, new Set(keys)) > 0) {
                    saveSRSState(srsKey, srsState);
                    document.getElementById('mastered-count').textContent = String(getTotalMastered());
                }
                setDueCount(card, dueAmong(keys, srsState));
                addProgressBar(card, seenPercent(keys, srsState));
            })
            .catch(console.error);
    });
    await Promise.all(pending);
}
