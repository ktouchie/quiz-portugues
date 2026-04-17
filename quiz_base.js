import { initTheme, loadVersion, startTimer, stopTimer, resumeTimer, updateTimerDisplay, updateBestScore } from './common.js';

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

        /** @type {TimerState} */
        this.timerState = { timerInterval: null, elapsedTime: 0, timerDisplay: null };
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

        this.timerState.timerDisplay = document.getElementById('timer-display');
        updateTimerDisplay(this.timerState.timerDisplay, 0);
        this._updateScoreDisplay();

        this.setupUI();

        document.getElementById('start-quiz').addEventListener('click', () => this.startQuiz());
        document.getElementById('submit-answer').addEventListener('click', () => this.submitAnswer());
        document.getElementById('next-question').addEventListener('click', () => this.nextQuestion());
        document.getElementById('restart').addEventListener('click', () => location.reload());

        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter') return;
            if (!this.isFeedbackDisplayed) {
                document.getElementById('submit-answer').click();
            } else {
                document.getElementById('next-question').click();
            }
        });
    }

    startQuiz() {
        const items = this.getSelectedItems();
        if (!items) return;

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

        const randomIndex = Math.floor(Math.random() * this.itemsToPractice.length);
        this.currentKey = this.itemsToPractice[randomIndex];

        this.renderQuestion(this.currentKey);

        document.getElementById('answer').value = '';
        const feedbackEl = document.getElementById('feedback');
        feedbackEl.textContent = '';
        feedbackEl.className = '';
        document.getElementById('answer-container').classList.remove('hidden');
        feedbackEl.classList.add('hidden');
        document.getElementById('next-question').classList.add('hidden');
        document.getElementById('answer').focus();

        resumeTimer(this.timerState);
    }

    submitAnswer() {
        const userAnswer = document.getElementById('answer').value.trim().toLowerCase().normalize('NFC');
        const correctAnswer = this.getCorrectAnswer(this.currentKey);
        const isCorrect = userAnswer === correctAnswer.toLowerCase().normalize('NFC');

        const feedbackEl = document.getElementById('feedback');
        feedbackEl.textContent = '';
        feedbackEl.className = '';

        if (isCorrect) {
            feedbackEl.textContent = 'Correto!';
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
        } else {
            feedbackEl.textContent = `Errado. A resposta correta é "${correctAnswer}".`;
            feedbackEl.className = 'incorrect';
            this.errorCount++;
            this.mistakeCounters[this.currentKey]++;
        }

        this._updateScoreDisplay();
        this._updateProgressBar();

        document.getElementById('answer-container').classList.add('hidden');
        feedbackEl.classList.remove('hidden');
        document.getElementById('next-question').classList.remove('hidden');
        this.isFeedbackDisplayed = true;

        stopTimer(this.timerState);
    }

    endQuiz() {
        stopTimer(this.timerState);
        document.getElementById('quiz').classList.add('hidden');
        document.getElementById('result').classList.remove('hidden');

        const scoreEl = document.getElementById('total-score');
        scoreEl.textContent = `Corretas: ${this.correctCount} | Erros: ${this.errorCount}`;

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
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    _updateScoreDisplay() {
        const el = document.getElementById('score-display');
        if (el) el.textContent = `Corretas: ${this.correctCount} | Erros: ${this.errorCount}`;
    }

    _updateProgressBar() {
        const pct = this.totalNeeded > 0
            ? Math.max(0, Math.min(100, (this.completedCount / this.totalNeeded) * 100))
            : 0;
        document.getElementById('progress-bar').style.width = pct + '%';
        document.getElementById('progress-percentage').textContent = `Progresso: ${pct.toFixed(2)}%`;
    }
}
