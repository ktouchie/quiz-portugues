/**
 * SM-2 spaced repetition algorithm.
 *
 * @typedef {{ interval: number, repetitions: number, easeFactor: number, nextReview: number }} SRSItem
 * @typedef {Object.<string, SRSItem>} SRSState
 */

const DEFAULT_EASE = 2.5;

/**
 * Compute next SRS interval for an item.
 * @param {SRSItem} item
 * @param {number} quality - 0 (blackout) to 5 (perfect)
 * @returns {SRSItem} updated item (mutated in place and returned)
 */
export function sm2(item, quality) {
    if (quality < 3) {
        item.repetitions = 0;
        item.interval = 1;
    } else {
        if (item.repetitions === 0) {
            item.interval = 1;
        } else if (item.repetitions === 1) {
            item.interval = 3;
        } else {
            item.interval = Math.round(item.interval * item.easeFactor);
        }
        item.repetitions++;
        item.easeFactor = Math.max(
            1.3,
            item.easeFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
        );
    }
    item.nextReview = Date.now() + item.interval * 86_400_000;
    return item;
}

/**
 * @param {string} storageKey
 * @returns {SRSState}
 */
export function loadSRSState(storageKey) {
    try {
        return JSON.parse(localStorage.getItem(storageKey) || '{}');
    } catch {
        return {};
    }
}

/**
 * @param {string} storageKey
 * @param {SRSState} state
 */
export function saveSRSState(storageKey, state) {
    localStorage.setItem(storageKey, JSON.stringify(state));
}

/**
 * Get or initialise the SRS record for a single item key.
 * @param {SRSState} state
 * @param {string} key
 * @returns {SRSItem}
 */
export function getItemSRS(state, key) {
    if (!state[key]) {
        state[key] = { interval: 0, repetitions: 0, easeFactor: DEFAULT_EASE, nextReview: 0 };
    }
    return state[key];
}

/**
 * Apply a SM-2 quality score to an item and persist the state.
 * @param {SRSState} state
 * @param {string} key
 * @param {number} quality - 0–5
 * @param {string} storageKey - localStorage key for persistence
 * @returns {SRSItem}
 */
export function updateItemSRS(state, key, quality, storageKey) {
    const item = getItemSRS(state, key);
    sm2(item, quality);
    saveSRSState(storageKey, state);
    return item;
}

/**
 * Return all keys whose nextReview timestamp is in the past (due now).
 * @param {SRSState} state
 * @returns {string[]}
 */
export function getDueItems(state) {
    const now = Date.now();
    return Object.keys(state).filter(k => state[k].nextReview > 0 && state[k].nextReview <= now);
}

/**
 * @param {SRSState} state
 * @param {string} key
 * @returns {boolean}
 */
export function isItemDue(state, key) {
    const item = state[key];
    return item ? (item.nextReview > 0 && item.nextReview <= Date.now()) : false;
}
