import { initTheme, loadVersion, startTimer, stopTimer, resumeTimer, updateTimerDisplay, updateBestScore } from './common.js';
import { loadSRSState, saveSRSState, getItemSRS, sm2, getDueItems, isReadyForTyping } from './srs.js';
import { updateStreak, checkMilestone, showMilestoneBanner } from './gamification.js';
import { buildQuickPracticePool } from './practice.js';

/**
 * @typedef {{ timerInterval: number|null, elapsedTime: number, timerDisplay: HTMLElement|null }} TimerState
 * @typedef {{ key: string, [k: string]: unknown }} QuizItem
 */

export class QuizBase {
    /**
     * @param {string} storageKey - localStorage key for best score
     */
    constructor(storageKey) {
        this.storageKey = storageKey;

        /** @type {unknown} raw JSON data loaded from server */
        this.data = null;

        /** @type {Object.<string, QuizItem>} key → full item object */
        this.itemData = {};

        /** @type {string[]} keys remaining to practice */
        this.itemsToPractice = [];

        /** @type {Object.<string, number>} correct-answer count per key */
        this.itemCounters = {};

        /** @type {Object.<string, number>} mistake count per key */
        this.mistakeCounters = {};

        /** @type {string|null} key of the current question */
        this.currentKey = null;

        this.correctCount = 0;
        this.errorCount = 0;
        this.completedCount = 0;
        this.totalNeeded = 0;
        this.isFeedbackDisplayed = false;

        /** @type {string[]|null} keys to use for retry-mistakes session */
        this.retryMistakeKeys = null;

        /** True while running a Quick Practice session (started from the setup screen's button). */
        this.quickPractice = false;

        /** @type {string[]|null} options shown for the current question, or null when typed */
        this.currentOptions = null;

        /** @type {TimerState} */
        this.timerState = { timerInterval: null, elapsedTime: 0, timerDisplay: null };

        // SRS storage key is derived from best-score key, e.g. bestScore_verbs → srs_verbs
        this.srsStorageKey = storageKey.replace('bestScore_', 'srs_');
        /** @type {import('./srs.js').SRSState} */
        this.srsState = {};
    }

    // ── Abstract methods ─────────────────────────────────────────────────────

    /** Fetch and return raw JSON data for this quiz. */
    async fetchData() { throw new Error('fetchData() not implemented'); }

    /**
     * Read user selections from the DOM and return an array of QuizItems.
     * Return null if validation fails (show alert inside this method).
     * @returns {QuizItem[]|null}
     */
    getSelectedItems() { throw new Error('getSelectedItems() not implemented'); }

    /**
     * Update the #question element to show the question for the given key.
     * @param {string} _key
     */
    renderQuestion(_key) { throw new Error('renderQuestion() not implemented'); }

    /**
     * Return the canonical correct answer string for the given key.
     * @param {string} _key
     * @returns {string}
     */
    getCorrectAnswer(_key) { throw new Error('getCorrectAnswer() not implemented'); }

    /**
     * Return a <li> element displaying one mistake in the results list.
     * @param {string} _key
     * @param {number} _count
     * @param {number} _index
     * @returns {HTMLLIElement}
     */
    formatMistake(_key, _count, _index) { throw new Error('formatMistake() not implemented'); }

    /**
     * Optional: return a grammar hint string for the given key, or null.
     * @param {string} _key
     * @returns {string|null}
     */
    getHint(_key) { return null; }

    /**
     * Optional: return an example sentence for the given key, or null.
     * @param {string} _key
     * @returns {string|null}
     */
    getExample(_key) { return null; }

    /**
     * Return a human-readable label for the given key (used by the SRS manager).
     * Override in subclasses to provide better labels.
     * @param {string} _key
     * @returns {string}
     */
    getLabel(_key) { return _key; }

    /**
     * Optional: every item in the module, for Quick Practice. Return null to disable it.
     * @returns {QuizItem[]|null}
     */
    getAllItems() { return null; }

    /**
     * Optional: multiple-choice options for the given key (correct answer included), or null to
     * always ask for a typed answer. Only asked while the item isn't ready for typing yet (see
     * isReadyForTyping in srs.js), the same per-item rule as the Android app.
     * @param {string} _key
     * @returns {string[]|null}
     */
    getOptions(_key) { return null; }

    // ── Template hook ────────────────────────────────────────────────────────

    /** Override to build setup-screen UI (checkboxes, etc.) after data loads. */
    setupUI() {}

    // ── Lifecycle ────────────────────────────────────────────────────────────

    async init() {
        initTheme();
        loadVersion();

        try {
            this.data = await this.fetchData();
        } catch (e) {
            console.error(e);
            alert('Erro ao carregar os dados.');
            return;
        }

        this.srsState = loadSRSState(this.srsStorageKey);

        this.timerState.timerDisplay = document.getElementById('timer-display');
        updateTimerDisplay(this.timerState.timerDisplay, 0);
        this._updateScoreDisplay();

        this.setupUI();
        this._updateDueCount();

        document.getElementById('start-quiz').addEventListener('click', () => {
            this.quickPractice = false;
            this.startQuiz();
        });
        document.getElementById('quick-practice')?.addEventListener('click', () => this.startQuickPractice());
        document.getElementById('submit-answer').addEventListener('click', () => this.submitAnswer());
        document.getElementById('next-question').addEventListener('click', () => this.nextQuestion());
        document.getElementById('restart').addEventListener('click', () => location.reload());

        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter') return;
            if (!this.isFeedbackDisplayed) {
                // Multiple-choice questions are answered by tapping an option, not Enter.
                if (this.currentOptions) return;
                document.getElementById('submit-answer').click();
            } else {
                document.getElementById('next-question').click();
            }
        });
    }

    /** Starts a short session over the whole module: due items first, topped up to the cap. */
    startQuickPractice() {
        const all = this.getAllItems();
        if (!all || all.length === 0) return;
        this.quickPractice = true;
        this.startQuiz(buildQuickPracticePool(all, new Set(getDueItems(this.srsState))));
    }

    /** @param {QuizItem[]|null} [explicitItems] - session items; defaults to the setup selection */
    startQuiz(explicitItems = null) {
        let items;
        if (explicitItems) {
            items = explicitItems;
        } else if (this.retryMistakeKeys) {
            const oldItemData = this.itemData;
            items = this.retryMistakeKeys.map(k => oldItemData[k]).filter(Boolean);
            this.retryMistakeKeys = null;
        } else {
            items = this.getSelectedItems();
        }
        if (!items || items.length === 0) return;

        this.itemData = {};
        this.itemCounters = {};
        this.mistakeCounters = {};
        this.correctCount = 0;
        this.errorCount = 0;
        this.completedCount = 0;

        this.itemsToPractice = items.map(item => {
            this.itemData[item.key] = item;
            this.itemCounters[item.key] = 0;
            this.mistakeCounters[item.key] = 0;
            return item.key;
        });

        // Sort: due-for-review items appear before new items
        const dueSet = new Set(getDueItems(this.srsState));
        this.itemsToPractice.sort((a, b) => {
            const aDue = dueSet.has(a) ? 0 : 1;
            const bDue = dueSet.has(b) ? 0 : 1;
            return aDue - bDue;
        });

        this.totalNeeded = this.itemsToPractice.length;

        document.getElementById('setup').classList.add('hidden');
        document.getElementById('quiz').classList.remove('hidden');
        this._updateProgressBar();
        this.nextQuestion();
        startTimer(this.timerState);
    }

    nextQuestion() {
        if (this.itemsToPractice.length === 0) {
            this.endQuiz();
            return;
        }
        this.isFeedbackDisplayed = false;

        // A missed item stays in the pool, but never comes straight back while others remain:
        // with multiple choice its answer was just shown.
        const candidates = this.itemsToPractice.length > 1
            ? this.itemsToPractice.filter(k => k !== this.currentKey)
            : this.itemsToPractice;
        this.currentKey = candidates[Math.floor(Math.random() * candidates.length)];

        this.renderQuestion(this.currentKey);
        this._renderOptions(isReadyForTyping(this.srsState[this.currentKey]) ? null : this.getOptions(this.currentKey));

        document.getElementById('answer').value = '';
        const feedbackEl = document.getElementById('feedback');
        feedbackEl.textContent = '';
        feedbackEl.className = '';
        document.getElementById('answer-container').classList.remove('hidden');
        feedbackEl.classList.add('hidden');
        document.getElementById('next-question').classList.add('hidden');
        if (!this.currentOptions) document.getElementById('answer').focus();

        resumeTimer(this.timerState);
    }

    /** @param {string} [chosen] - the tapped option; defaults to the typed answer */
    submitAnswer(chosen) {
        const raw = typeof chosen === 'string' ? chosen : document.getElementById('answer').value;
        const userAnswer = raw.trim().toLowerCase().normalize('NFC');
        const correctAnswer = this.getCorrectAnswer(this.currentKey);
        const isCorrect = userAnswer === correctAnswer.toLowerCase().normalize('NFC');

        const feedbackEl = document.getElementById('feedback');
        feedbackEl.textContent = '';
        feedbackEl.className = '';

        if (isCorrect) {
            feedbackEl.textContent = 'Correto! ';
            feedbackEl.className = 'correct';
            this.correctCount++;
            this.itemCounters[this.currentKey]++;

            if (this.itemCounters[this.currentKey] >= 1) {
                const idx = this.itemsToPractice.indexOf(this.currentKey);
                if (idx > -1) {
                    this.itemsToPractice.splice(idx, 1);
                    this.completedCount++;
                }
            }
            // Implicit SRS quality: first-try correct = 4, correct after mistakes = 2 (resets schedule)
            const quality = this.mistakeCounters[this.currentKey] > 0 ? 2 : 4;
            this._recordSRS(this.currentKey, quality);
            document.getElementById('next-question').classList.remove('hidden');
        } else {
            feedbackEl.textContent = '';
            feedbackEl.className = 'incorrect';
            const wrongMsg = document.createElement('span');
            wrongMsg.textContent = `Errado. A resposta correta é "${correctAnswer}". `;
            feedbackEl.appendChild(wrongMsg);
            const hint = this.getHint(this.currentKey);
            if (hint) {
                const hintEl = document.createElement('p');
                hintEl.className = 'grammar-hint';
                hintEl.textContent = hint;
                feedbackEl.appendChild(hintEl);
            }
            const example = this.getExample(this.currentKey);
            if (example) {
                const exEl = document.createElement('p');
                exEl.className = 'example-sentence';
                exEl.textContent = example;
                feedbackEl.appendChild(exEl);
            }
            this.errorCount++;
            this.mistakeCounters[this.currentKey]++;
            this._recordSRS(this.currentKey, 0);
            document.getElementById('next-question').classList.remove('hidden');
        }

        this._updateScoreDisplay();
        this._updateProgressBar();

        if (this.currentOptions) {
            this._revealOptions(correctAnswer, raw);
        } else {
            document.getElementById('answer-container').classList.add('hidden');
        }
        feedbackEl.classList.remove('hidden');
        this.isFeedbackDisplayed = true;

        stopTimer(this.timerState);
    }

    endQuiz() {
        stopTimer(this.timerState);
        document.getElementById('quiz').classList.add('hidden');
        document.getElementById('result').classList.remove('hidden');

        const total = this.correctCount + this.errorCount;
        const accuracyPct = total > 0 ? Math.round((this.correctCount / total) * 100) : 100;
        const mins = Math.floor(this.timerState.elapsedTime / 60);
        const secs = this.timerState.elapsedTime % 60;

        document.getElementById('total-score').textContent =
            `Corretas: ${this.correctCount} | Erros: ${this.errorCount}`;
        document.getElementById('quiz-time').textContent =
            `Tempo: ${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
        document.getElementById('accuracy').textContent = `Precisão: ${accuracyPct}%`;

        updateBestScore(this.storageKey, this.correctCount);

        const sorted = Object.entries(this.mistakeCounters)
            .filter(([, count]) => count > 0)
            .sort(([, a], [, b]) => b - a)
            .slice(0, 10);

        const list = document.getElementById('top-mistakes');
        list.textContent = '';

        if (sorted.length > 0) {
            sorted.forEach(([key, count], index) => {
                list.appendChild(this.formatMistake(key, count, index));
            });
        } else {
            const li = document.createElement('li');
            li.textContent = 'Parabéns! Não cometeu nenhum erro.';
            list.appendChild(li);
        }

        updateStreak();
        const milestone = checkMilestone();
        if (milestone) showMilestoneBanner(milestone);

        const retryBtn = document.getElementById('retry-mistakes');
        if (retryBtn) {
            const mistakeKeys = Object.keys(this.mistakeCounters).filter(k => this.mistakeCounters[k] > 0);
            if (mistakeKeys.length > 0) {
                retryBtn.textContent = `Praticar erros (${mistakeKeys.length})`;
                retryBtn.classList.remove('hidden');
                retryBtn.onclick = () => {
                    this.retryMistakeKeys = mistakeKeys;
                    document.getElementById('result').classList.add('hidden');
                    this.startQuiz();
                };
            } else {
                retryBtn.classList.add('hidden');
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Shows tappable answer options instead of the text box when `options` is given.
     * @param {string[]|null} options
     */
    _renderOptions(options) {
        this.currentOptions = options && options.length > 0 ? options : null;
        const container = document.getElementById('answer-container');
        let list = document.getElementById('options');
        if (!list) {
            list = document.createElement('div');
            list.id = 'options';
            container.prepend(list);
        }
        list.textContent = '';
        list.classList.toggle('hidden', !this.currentOptions);
        document.getElementById('answer').classList.toggle('hidden', !!this.currentOptions);
        document.getElementById('submit-answer').classList.toggle('hidden', !!this.currentOptions);
        (this.currentOptions ?? []).forEach((option, index) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'option';
            const badge = document.createElement('span');
            badge.className = 'option-badge';
            badge.textContent = String.fromCharCode(65 + index);
            const text = document.createElement('span');
            text.className = 'option-text';
            text.textContent = option;
            btn.append(badge, text);
            btn.addEventListener('click', () => this.submitAnswer(option));
            list.appendChild(btn);
        });
    }

    /** Locks the options and marks the right one, and the tapped one if it was wrong. */
    _revealOptions(correctAnswer, chosen) {
        for (const btn of document.querySelectorAll('#options button.option')) {
            const text = btn.querySelector('.option-text')?.textContent ?? btn.textContent;
            btn.disabled = true;
            btn.classList.toggle('option--correct', text === correctAnswer);
            btn.classList.toggle('option--wrong', text === chosen && text !== correctAnswer);
        }
    }

    _updateScoreDisplay() {
        const el = document.getElementById('score-display');
        if (el) el.textContent = `Corretas: ${this.correctCount} | Erros: ${this.errorCount}`;
    }

    _recordSRS(key, quality) {
        const item = getItemSRS(this.srsState, key);
        if (!item.label) item.label = this.getLabel(key);
        sm2(item, quality);
        saveSRSState(this.srsStorageKey, this.srsState);
    }

    _updateDueCount() {
        const el = document.getElementById('srs-due-count');
        if (!el) return;
        const due = getDueItems(this.srsState).length;
        el.textContent = due > 0 ? `${due} ${due === 1 ? 'item' : 'itens'} para rever hoje` : '';
        el.classList.toggle('hidden', due === 0);
    }

    _updateProgressBar() {
        const pct = this.totalNeeded > 0
            ? Math.max(0, Math.min(100, (this.completedCount / this.totalNeeded) * 100))
            : 0;
        document.getElementById('progress-bar').style.width = pct + '%';
        document.getElementById('progress-percentage').textContent = `Progresso: ${pct.toFixed(2)}%`;
        const masteryEl = document.getElementById('mastery-counter');
        if (masteryEl) {
            masteryEl.textContent = `Dominadas: ${this.completedCount}/${this.totalNeeded}`;
        }
    }
}
