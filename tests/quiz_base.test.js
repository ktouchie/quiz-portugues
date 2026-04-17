import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { QuizBase } from '../quiz_base.js';

function setupDOM() {
    document.body.innerHTML = `
        <div id="setup"></div>
        <div id="quiz" class="hidden"></div>
        <div id="result" class="hidden"></div>
        <div id="score-display"></div>
        <div id="timer-display"></div>
        <p id="question"></p>
        <div id="answer-container"></div>
        <input id="answer" type="text" />
        <button id="submit-answer"></button>
        <button id="next-question" class="hidden"></button>
        <button id="start-quiz"></button>
        <button id="restart"></button>
        <p id="feedback" class="hidden"></p>
        <div id="progress-bar" style="width:0%"></div>
        <p id="progress-percentage"></p>
        <p id="total-score"></p>
        <p id="best-score"></p>
        <ol id="top-mistakes"></ol>
    `;
}

class TestQuiz extends QuizBase {
    constructor() {
        super('test_key');
        this._items = [
            { key: 'a|||0', answer: 'alpha' },
            { key: 'b|||1', answer: 'beta' },
            { key: 'c|||2', answer: 'gamma' },
        ];
    }
    async fetchData() { return {}; }
    getSelectedItems() { return this._items; }
    renderQuestion(key) {
        document.getElementById('question').textContent = key;
    }
    getCorrectAnswer(key) { return this.itemData[key].answer; }
    formatMistake(key, count, index) {
        const li = document.createElement('li');
        li.textContent = `${index}: ${key} (${count})`;
        return li;
    }
}

describe('QuizBase.startQuiz', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    it('populates itemsToPractice from getSelectedItems', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        expect(quiz.itemsToPractice.length).toBe(3);
    });

    it('resets counters on start', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.correctCount = 5;
        quiz.errorCount = 3;
        quiz.startQuiz();
        expect(quiz.correctCount).toBe(0);
        expect(quiz.errorCount).toBe(0);
    });

    it('sets totalNeeded to item count', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        expect(quiz.totalNeeded).toBe(3);
    });
});

describe('QuizBase.submitAnswer', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    function makeQuizInProgress() {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        return quiz;
    }

    it('increments correctCount on correct answer', () => {
        const quiz = makeQuizInProgress();
        quiz.currentKey = 'a|||0';
        document.getElementById('answer').value = 'alpha';
        quiz.submitAnswer();
        expect(quiz.correctCount).toBe(1);
        expect(quiz.errorCount).toBe(0);
    });

    it('increments errorCount on wrong answer', () => {
        const quiz = makeQuizInProgress();
        quiz.currentKey = 'a|||0';
        document.getElementById('answer').value = 'wrong';
        quiz.submitAnswer();
        expect(quiz.correctCount).toBe(0);
        expect(quiz.errorCount).toBe(1);
    });

    it('retires item from queue on correct answer', () => {
        const quiz = makeQuizInProgress();
        const key = quiz.itemsToPractice[0];
        quiz.currentKey = key;
        document.getElementById('answer').value = quiz.getCorrectAnswer(key);
        quiz.submitAnswer();
        expect(quiz.itemsToPractice).not.toContain(key);
    });

    it('does not retire item on wrong answer', () => {
        const quiz = makeQuizInProgress();
        const key = quiz.itemsToPractice[0];
        quiz.currentKey = key;
        document.getElementById('answer').value = 'wrong';
        quiz.submitAnswer();
        expect(quiz.itemsToPractice).toContain(key);
    });

    it('increments completedCount only on correct answer', () => {
        const quiz = makeQuizInProgress();
        const key = quiz.itemsToPractice[0];
        quiz.currentKey = key;
        document.getElementById('answer').value = quiz.getCorrectAnswer(key);
        quiz.submitAnswer();
        expect(quiz.completedCount).toBe(1);
    });

    it('tracks mistakes per key', () => {
        const quiz = makeQuizInProgress();
        quiz.currentKey = 'a|||0';
        document.getElementById('answer').value = 'wrong';
        quiz.submitAnswer();
        quiz.isFeedbackDisplayed = false;
        document.getElementById('answer').value = 'still wrong';
        quiz.submitAnswer();
        expect(quiz.mistakeCounters['a|||0']).toBe(2);
    });

    it('accepts answer case-insensitively', () => {
        const quiz = makeQuizInProgress();
        quiz.currentKey = 'a|||0';
        document.getElementById('answer').value = 'ALPHA';
        quiz.submitAnswer();
        expect(quiz.correctCount).toBe(1);
    });
});

describe('QuizBase.endQuiz', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    it('shows result section', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        quiz.endQuiz();
        expect(document.getElementById('result').classList.contains('hidden')).toBe(false);
    });

    it('renders top mistakes', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        quiz.mistakeCounters['a|||0'] = 3;
        quiz.mistakeCounters['b|||1'] = 1;
        quiz.endQuiz();
        const items = document.getElementById('top-mistakes').querySelectorAll('li');
        expect(items.length).toBe(2);
    });

    it('shows no-mistake message when clean', () => {
        const quiz = new TestQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        quiz.endQuiz();
        const text = document.getElementById('top-mistakes').textContent;
        expect(text).toContain('Parabéns');
    });
});
