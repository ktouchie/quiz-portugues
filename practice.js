/** Maximum number of items in a Quick Practice session (same as the Android app). */
export const QUICK_PRACTICE_CAP = 12;

/** Number of wrong options shown next to the right one in a multiple-choice question. */
export const DISTRACTOR_COUNT = 3;

/**
 * Fisher–Yates shuffle into a new array.
 * @template T
 * @param {T[]} items
 * @param {() => number} [random]
 * @returns {T[]}
 */
export function shuffle(items, random = Math.random) {
    const out = items.slice();
    for (let i = out.length - 1; i > 0; i--) {
        const j = Math.floor(random() * (i + 1));
        [out[i], out[j]] = [out[j], out[i]];
    }
    return out;
}

/**
 * Picks a Quick Practice session: items due for review first, topped up with items that aren't
 * due, up to `cap` in total.
 * @template {{ key: string }} T
 * @param {T[]} items - every item in the module
 * @param {Set<string>} dueKeys - keys currently due for SRS review
 * @param {number} [cap]
 * @param {() => number} [random]
 * @returns {T[]}
 */
export function buildQuickPracticePool(items, dueKeys, cap = QUICK_PRACTICE_CAP, random = Math.random) {
    const due = shuffle(items.filter(i => dueKeys.has(i.key)), random).slice(0, cap);
    const filler = shuffle(items.filter(i => !dueKeys.has(i.key)), random).slice(0, cap - due.length);
    return [...due, ...filler];
}

/**
 * Builds the options for a multiple-choice question: the correct answer plus up to
 * DISTRACTOR_COUNT distinct wrong answers, preferring `preferred` candidates (e.g. the same
 * category) and falling back to `fallback` when there aren't enough.
 * @param {string} correct
 * @param {string[]} preferred
 * @param {string[]} [fallback]
 * @param {() => number} [random]
 * @returns {string[]} shuffled options, including `correct` exactly once
 */
export function buildOptions(correct, preferred, fallback = [], random = Math.random) {
    const distractors = [];
    for (const pool of [shuffle(preferred, random), shuffle(fallback, random)]) {
        for (const candidate of pool) {
            if (distractors.length === DISTRACTOR_COUNT) break;
            if (candidate !== correct && !distractors.includes(candidate)) distractors.push(candidate);
        }
    }
    return shuffle([...distractors, correct], random);
}
