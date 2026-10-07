import { CEFR_LEVELS, unlockedTiers } from './cefr.js';

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
 * Quick Practice gated by CEFR tier, as on Android: only items in unlocked tiers, due items first,
 * then items from the newest unlocked ("frontier") tier, so that tier gets practised enough to
 * open the next one instead of being crowded out by tiers the learner already knows.
 * @template {{ key: string }} T
 * @param {T[]} items - every item in the module
 * @param {Set<string>} dueKeys - keys currently due for SRS review
 * @param {import('./srs.js').SRSState} srsState
 * @param {(item: T) => import('./cefr.js').CefrLevel} levelOf
 * @param {number} [cap]
 * @param {() => number} [random]
 * @returns {T[]}
 */
export function buildGatedQuickPracticePool(items, dueKeys, srsState, levelOf, cap = QUICK_PRACTICE_CAP, random = Math.random) {
    /** @type {Object.<string, string[]>} */
    const keysByLevel = {};
    for (const item of items) (keysByLevel[levelOf(item)] ??= []).push(item.key);
    const unlocked = unlockedTiers(keysByLevel, srsState);
    const frontier = CEFR_LEVELS.filter(level => unlocked.has(level)).at(-1);

    const eligible = items.filter(i => unlocked.has(levelOf(i)));
    const due = shuffle(eligible.filter(i => dueKeys.has(i.key)), random).slice(0, cap);
    const notDue = eligible.filter(i => !dueKeys.has(i.key));
    const filler = [
        ...shuffle(notDue.filter(i => levelOf(i) === frontier), random),
        ...shuffle(notDue.filter(i => levelOf(i) !== frontier), random),
    ].slice(0, cap - due.length);
    return [...due, ...filler];
}

/** Lower-cased, NFC-normalised form used to compare answers. */
const normalise = (text) => text.trim().toLowerCase().normalize('NFC');

/**
 * Whether a typed answer matches the expected one, ignoring case, surrounding spaces and Unicode
 * form. A bracketed note at the end of the expected answer is optional: it tells two words apart
 * when they're the prompt ("short (height)" → baixo, "short (length)" → curto), but "short" alone
 * is a right answer when translating either into English. Same rule as Android's answersMatch.
 * @param {string} given
 * @param {string} expected
 * @returns {boolean}
 */
export function answerMatches(given, expected) {
    const answer = normalise(given);
    return answer === normalise(expected) || answer === normalise(expected.replace(/\s*\([^)]*\)\s*$/, ''));
}

/**
 * Builds the options for a multiple-choice question: the correct answer plus up to
 * DISTRACTOR_COUNT wrong answers, taken from `pools` in order (each pool shuffled), so the most
 * confusable candidates come first. Candidates that read the same as the correct answer or as
 * each other are skipped.
 * @param {string} correct
 * @param {string[][]} pools - candidate wrong answers, best pool first
 * @param {() => number} [random]
 * @returns {string[]} shuffled options, including `correct` exactly once
 */
export function buildOptions(correct, pools, random = Math.random) {
    const seen = new Set([normalise(correct)]);
    const distractors = [];
    for (const pool of pools) {
        for (const candidate of shuffle(pool, random)) {
            if (distractors.length === DISTRACTOR_COUNT) break;
            if (seen.has(normalise(candidate))) continue;
            seen.add(normalise(candidate));
            distractors.push(candidate);
        }
    }
    return shuffle([...distractors, correct], random);
}

/**
 * Edit-distance similarity, used to rank distractors by how confusable they are with the answer
 * (same as the Android app's content/StringSimilarity.kt). Case-insensitive but accent-sensitive:
 * an accent is part of what tells two Portuguese words apart.
 * @param {string} a
 * @param {string} b
 * @returns {number} 1 for identical strings, towards 0 as they diverge
 */
export function stringSimilarity(a, b) {
    const x = a.toLowerCase();
    const y = b.toLowerCase();
    const maxLen = Math.max(x.length, y.length);
    if (maxLen === 0) return 1;
    return 1 - levenshtein(x, y) / maxLen;
}

/** Wagner–Fischer edit distance with unit costs, one row at a time. */
function levenshtein(a, b) {
    let prev = Array.from({ length: b.length + 1 }, (_, j) => j);
    for (let i = 1; i <= a.length; i++) {
        const row = [i];
        for (let j = 1; j <= b.length; j++) {
            row[j] = a[i - 1] === b[j - 1]
                ? prev[j - 1]
                : 1 + Math.min(prev[j], row[j - 1], prev[j - 1]);
        }
        prev = row;
    }
    return prev[b.length];
}
