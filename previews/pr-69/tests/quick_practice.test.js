import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { shuffle, buildQuickPracticePool, buildOptions, QUICK_PRACTICE_CAP } from '../practice.js';
import { QuizBase } from '../quiz_base.js';
import { VocabQuiz } from '../vocabulary_quiz.js';
import { VerbQuiz } from '../script.js';

function setupDOM() {
    document.body.innerHTML = `
        <div id="setup"></div>
        <div id="quiz" class="hidden"></div>
        <div id="result" class="hidden"></div>
        <div id="score-display"></div>
        <div id="timer-display"></div>
        <p id="question"></p>
        <div id="answer-container">
            <input id="answer" type="text" />
            <button id="submit-answer"></button>
        </div>
        <button id="next-question" class="hidden"></button>
        <p id="feedback" class="hidden"></p>
        <div id="progress-bar" style="width:0%"></div>
        <p id="progress-percentage"></p>
        <p id="mastery-counter"></p>
    `;
}

const items = (n) => Array.from({ length: n }, (_, i) => ({ key: `k${i}` }));

describe('shuffle', () => {
    it('returns a new array with the same elements', () => {
        const input = [1, 2, 3, 4, 5];
        const out = shuffle(input);
        expect(out).not.toBe(input);
        expect([...out].sort()).toEqual(input);
    });
});

describe('buildQuickPracticePool', () => {
    it('caps the session at 12 items', () => {
        expect(QUICK_PRACTICE_CAP).toBe(12);
        expect(buildQuickPracticePool(items(50), new Set())).toHaveLength(12);
    });

    it('puts every due item first, then tops up with items that are not due', () => {
        const due = new Set(['k3', 'k7', 'k9']);
        const pool = buildQuickPracticePool(items(30), due);
        expect(pool).toHaveLength(12);
        expect(new Set(pool.slice(0, 3).map(i => i.key))).toEqual(due);
        expect(pool.slice(3).every(i => !due.has(i.key))).toBe(true);
    });

    it('takes only 12 due items when more than 12 are due', () => {
        const all = items(40);
        const due = new Set(all.slice(0, 20).map(i => i.key));
        const pool = buildQuickPracticePool(all, due);
        expect(pool).toHaveLength(12);
        expect(pool.every(i => due.has(i.key))).toBe(true);
    });

    it('uses every item when the module has fewer than 12, without duplicates', () => {
        const pool = buildQuickPracticePool(items(5), new Set(['k1']));
        expect(pool).toHaveLength(5);
        expect(new Set(pool.map(i => i.key)).size).toBe(5);
    });
});

describe('buildOptions', () => {
    it('returns the correct answer once plus three distinct wrong answers', () => {
        const options = buildOptions('dog', ['cat', 'bird', 'fish', 'cow', 'dog']);
        expect(options).toHaveLength(4);
        expect(options.filter(o => o === 'dog')).toHaveLength(1);
        expect(new Set(options).size).toBe(4);
    });

    it('prefers the preferred candidates and only falls back when there are too few', () => {
        const options = buildOptions('red', ['blue', 'green'], ['one', 'two', 'three']);
        expect(options).toEqual(expect.arrayContaining(['red', 'blue', 'green']));
        expect(options.filter(o => ['one', 'two', 'three'].includes(o))).toHaveLength(1);
    });

    it('ignores duplicate candidates', () => {
        const options = buildOptions('a', ['b', 'b', 'b'], ['c', 'c', 'd']);
        expect([...options].sort()).toEqual(['a', 'b', 'c', 'd']);
    });
});

class McQuiz extends QuizBase {
    constructor() {
        super('bestScore_mc');
        this.all = items(20).map(i => ({ ...i, answer: `answer-${i.key}` }));
    }
    async fetchData() { return {}; }
    getSelectedItems() { return this.all.slice(0, 2); }
    getAllItems() { return this.all; }
    getOptions(key) {
        return this.quickPractice ? [this.itemData[key].answer, 'wrong 1', 'wrong 2', 'wrong 3'] : null;
    }
    renderQuestion(key) { document.getElementById('question').textContent = key; }
    getCorrectAnswer(key) { return this.itemData[key].answer; }
    formatMistake() { return document.createElement('li'); }
}

describe('QuizBase Quick Practice and multiple choice', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    function startQuick() {
        const quiz = new McQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuickPractice();
        return quiz;
    }

    it('starts a capped session from getAllItems, not the setup selection', () => {
        const quiz = startQuick();
        expect(quiz.quickPractice).toBe(true);
        expect(quiz.totalNeeded).toBe(12);
    });

    it('shows tappable options instead of the text box', () => {
        startQuick();
        const buttons = document.querySelectorAll('#options button.option');
        expect(buttons).toHaveLength(4);
        expect(document.getElementById('answer').classList.contains('hidden')).toBe(true);
        expect(document.getElementById('submit-answer').classList.contains('hidden')).toBe(true);
    });

    it('scores a tapped option like a typed answer', () => {
        const quiz = startQuick();
        const right = quiz.itemData[quiz.currentKey].answer;
        [...document.querySelectorAll('#options button')].find(b => b.textContent === right).click();
        expect(quiz.correctCount).toBe(1);

        quiz.nextQuestion();
        [...document.querySelectorAll('#options button')].find(b => b.textContent === 'wrong 1').click();
        expect(quiz.errorCount).toBe(1);
    });

    it('goes back to the text box for a normal session', () => {
        const quiz = startQuick();
        quiz.quickPractice = false;
        quiz.startQuiz();
        expect(document.getElementById('options').classList.contains('hidden')).toBe(true);
        expect(document.getElementById('answer').classList.contains('hidden')).toBe(false);
        expect(quiz.currentOptions).toBeNull();
    });
});

const VOCAB = {
    Cores: { azul: 'blue', verde: 'green', vermelho: 'red', preto: 'black', branco: 'white' },
    Animais: { cão: 'dog', gato: 'cat' },
};

describe('VocabQuiz Quick Practice', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    function makeVocab() {
        const quiz = new VocabQuiz();
        quiz.data = VOCAB;
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        return quiz;
    }

    it('offers every word in every category', () => {
        expect(makeVocab().getAllItems().map(i => i.ptWord).sort())
            .toEqual(['azul', 'branco', 'cão', 'gato', 'preto', 'verde', 'vermelho']);
    });

    it('asks Portuguese → English with four options, wrong ones from the same category', () => {
        const quiz = makeVocab();
        quiz.startQuickPractice();
        expect(quiz.ENtoPT).toBe(false);
        const options = quiz.getOptions('Cores|||azul|||blue');
        expect(options).toHaveLength(4);
        expect(options).toContain('blue');
        expect(options.every(o => Object.values(VOCAB.Cores).includes(o))).toBe(true);
        expect(quiz.getCorrectAnswer('Cores|||azul|||blue')).toBe('blue');
    });

    it('falls back to other categories when a category is too small', () => {
        const quiz = makeVocab();
        quiz.startQuickPractice();
        const options = quiz.getOptions('Animais|||cão|||dog');
        expect(options).toHaveLength(4);
        expect(options).toContain('dog');
        expect(options).toContain('cat');
    });

    it('keeps typed answers for a normal session', () => {
        const quiz = makeVocab();
        expect(quiz.getOptions('Cores|||azul|||blue')).toBeNull();
    });
});

describe('VerbQuiz Quick Practice', () => {
    it('draws from every conjugated form of every verb, skipping participles and empty forms', () => {
        const quiz = new VerbQuiz();
        quiz.data = {
            falar: {
                difficulty: 'beginner',
                presente: ['falo', 'falas', 'fala', 'falamos', 'falam'],
                imperativo: ['', 'fala', 'fale', 'falemos', 'falem'],
            },
            vir: { difficulty: 'advanced', presente: ['venho', 'vens', 'vem', 'vimos', 'vêm'] },
            cultivar: { participios_passados: { ter: 'cultivado', ser: 'cultivado', estar: 'cultivado' } },
        };
        const keys = quiz.getAllItems().map(i => i.key);
        expect(keys).toHaveLength(14);
        expect(keys).toContain('vir|||presente|||0');
        expect(keys).not.toContain('falar|||imperativo|||0');
        expect(keys.some(k => k.startsWith('cultivar'))).toBe(false);
    });
});

describe('fixes from the PR #69 review', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    it('never asks a missed question again straight away while other items remain', () => {
        const quiz = new McQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuickPractice();
        for (let i = 0; i < 30; i++) {
            const missed = quiz.currentKey;
            quiz.submitAnswer('wrong 1');
            quiz.nextQuestion();
            expect(quiz.currentKey).not.toBe(missed);
        }
    });

    it('asks the last item again when it is the only one left', () => {
        const quiz = new McQuiz();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz([quiz.all[0]]);
        quiz.submitAnswer('wrong 1');
        quiz.nextQuestion();
        expect(quiz.currentKey).toBe('k0');
    });

    it('ignores Enter on a multiple-choice question', async () => {
        document.body.insertAdjacentHTML('beforeend', '<button id="start-quiz"></button><button id="restart"></button>');
        const quiz = new McQuiz();
        await quiz.init();
        quiz.startQuickPractice();
        document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Enter' }));
        expect(quiz.correctCount + quiz.errorCount).toBe(0);
    });

    it('keeps Portuguese → English multiple choice when retrying Quick Practice mistakes', () => {
        document.body.insertAdjacentHTML('beforeend', `
            <p id="total-score"></p><p id="quiz-time"></p><p id="accuracy"></p><p id="best-score"></p>
            <ol id="top-mistakes"></ol><button id="retry-mistakes" class="hidden"></button>`);
        const quiz = new VocabQuiz();
        quiz.data = VOCAB;
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuickPractice();
        quiz.submitAnswer('definitely wrong');
        while (quiz.itemsToPractice.length > 0) {
            quiz.nextQuestion();
            quiz.submitAnswer(quiz.getCorrectAnswer(quiz.currentKey));
        }
        quiz.nextQuestion(); // ends the session
        document.getElementById('retry-mistakes').click();
        expect(quiz.totalNeeded).toBe(1);
        expect(quiz.ENtoPT).toBe(false);
        expect(quiz.currentOptions).toHaveLength(4);
    });
});

describe('"Selecionar tudo" box', () => {
    it('is not read as a category when starting a quiz', () => {
        document.body.innerHTML = '<div id="categories"></div>';
        const quiz = new VocabQuiz();
        quiz.data = VOCAB;
        quiz.setupUI();
        document.body.insertAdjacentHTML('beforeend', `
            <input type="radio" name="translation-direction" value="true" checked>`);
        const selectAll = document.getElementById('categories-select-all');
        selectAll.checked = true;
        selectAll.dispatchEvent(new window.Event('change'));
        const items = quiz.getSelectedItems();
        expect(items).toHaveLength(7);
        expect(items.every(i => i.category in VOCAB)).toBe(true);
    });
});
