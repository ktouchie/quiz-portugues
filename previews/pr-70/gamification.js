/**
 * Streak tracking, milestone detection, and personal goals.
 *
 * @typedef {{ lastPracticeDate: string|null, currentStreak: number, longestStreak: number }} StreakData
 * @typedef {{ target: number, days: number, startDate: string }} GoalData
 */

const STREAK_KEY = 'streak_data';
const SEEN_MILESTONES_KEY = 'seen_milestones';
const GOAL_KEY = 'goal_data';

const SRS_KEYS = ['srs_verbs', 'srs_vocab', 'srs_gender', 'srs_ser_estar_ficar', 'srs_contractions', 'srs_subjunctive', 'srs_indirect_speech'];
const MILESTONES = [10, 25, 50, 100, 250, 500];

// ── Streak ──────────────────────────────────────────────────────────────────

/** @returns {StreakData} */
export function loadStreak() {
    try {
        return JSON.parse(localStorage.getItem(STREAK_KEY) || 'null') ||
            { lastPracticeDate: null, currentStreak: 0, longestStreak: 0 };
    } catch {
        return { lastPracticeDate: null, currentStreak: 0, longestStreak: 0 };
    }
}

/** @param {StreakData} data */
function saveStreak(data) {
    localStorage.setItem(STREAK_KEY, JSON.stringify(data));
}

/**
 * Record that the user practiced today and update streak.
 * @returns {StreakData}
 */
export function updateStreak() {
    const today = new Date().toISOString().slice(0, 10);
    const data = loadStreak();

    if (data.lastPracticeDate === today) return data; // already counted today

    const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
    if (data.lastPracticeDate === yesterday) {
        data.currentStreak++;
    } else {
        data.currentStreak = 1; // streak broken
    }

    data.longestStreak = Math.max(data.longestStreak, data.currentStreak);
    data.lastPracticeDate = today;
    saveStreak(data);
    return data;
}

// ── Mastered items count (across all quizzes) ───────────────────────────────

/** @returns {number} total items that have been reviewed at least once */
export function getTotalMastered() {
    let total = 0;
    for (const key of SRS_KEYS) {
        try {
            const state = JSON.parse(localStorage.getItem(key) || '{}');
            total += Object.values(state).filter(item => item.repetitions > 0).length;
        } catch {
            // ignore
        }
    }
    return total;
}

// ── Milestones ───────────────────────────────────────────────────────────────

/** @returns {number[]} */
function loadSeenMilestones() {
    try {
        return JSON.parse(localStorage.getItem(SEEN_MILESTONES_KEY) || '[]');
    } catch { return []; }
}

/**
 * Check if any unseen milestone has been reached and return it, or null.
 * @returns {number|null}
 */
export function checkMilestone() {
    const total = getTotalMastered();
    const seen = loadSeenMilestones();
    const reached = MILESTONES.find(m => total >= m && !seen.includes(m));
    if (reached !== undefined) {
        seen.push(reached);
        localStorage.setItem(SEEN_MILESTONES_KEY, JSON.stringify(seen));
        return reached;
    }
    return null;
}

/**
 * Show a milestone overlay banner that auto-dismisses after 4 seconds.
 * @param {number} count
 */
export function showMilestoneBanner(count) {
    const existing = document.getElementById('milestone-banner');
    if (existing) existing.remove();

    const banner = document.createElement('div');
    banner.id = 'milestone-banner';
    banner.setAttribute('role', 'status');
    banner.setAttribute('aria-live', 'polite');

    const msg = document.createElement('span');
    msg.textContent = `Parabéns! ${count} itens dominados! 🎉`;
    banner.appendChild(msg);

    document.body.appendChild(banner);

    setTimeout(() => banner.classList.add('milestone-banner--visible'), 50);
    setTimeout(() => {
        banner.classList.remove('milestone-banner--visible');
        setTimeout(() => banner.remove(), 400);
    }, 4000);
}

// ── Personal goals ───────────────────────────────────────────────────────────

/** @returns {GoalData|null} */
export function loadGoal() {
    try {
        return JSON.parse(localStorage.getItem(GOAL_KEY) || 'null');
    } catch { return null; }
}

/**
 * @param {number} target - number of items to master
 * @param {number} days - number of days to achieve it
 */
export function saveGoal(target, days) {
    const goal = {
        target,
        days,
        startDate: new Date().toISOString().slice(0, 10),
    };
    localStorage.setItem(GOAL_KEY, JSON.stringify(goal));
    return goal;
}

/** @param {GoalData} goal @returns {{ mastered: number, daysLeft: number, pct: number }} */
export function getGoalProgress(goal) {
    const mastered = getTotalMastered();
    const elapsed = Math.floor((Date.now() - new Date(goal.startDate).getTime()) / 86_400_000);
    const daysLeft = Math.max(0, goal.days - elapsed);
    const pct = Math.min(100, Math.round((mastered / goal.target) * 100));
    return { mastered, daysLeft, pct };
}
