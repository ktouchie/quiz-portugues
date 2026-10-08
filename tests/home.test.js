import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { cwd } from 'node:process';
import { verbItemKeys, vocabularyItemKeys, seenPercent, renderModuleCard, addProgressBar, setDueCount, dueAmong, renderHome, MODULES } from '../home.js';
import { pruneRecords } from '../srs.js';
import { VerbQuiz } from '../script.js';
import { VocabQuiz } from '../vocabulary_quiz.js';

const readJson = (file) => JSON.parse(readFileSync(join(cwd(), file), 'utf8'));

describe('home page progress', () => {
    it('counts the same verb items as Quick Practice', () => {
        const quiz = new VerbQuiz();
        quiz.data = readJson('verbs.json');
        expect(verbItemKeys(quiz.data)).toEqual(quiz.getAllItems().map(i => i.key));
    });

    it('counts the same vocabulary items as Quick Practice', () => {
        const quiz = new VocabQuiz();
        quiz.data = readJson('vocabulary.json');
        expect(vocabularyItemKeys(quiz.data)).toEqual(quiz.getAllItems().map(i => i.key));
    });

    it('shows the share of items answered correctly at least once, rounded down', () => {
        const keys = ['a', 'b', 'c'];
        expect(seenPercent(keys, {})).toBe(0);
        expect(seenPercent(keys, { a: { repetitions: 1 }, b: { repetitions: 0 }, z: { repetitions: 5 } })).toBe(33);
        expect(seenPercent([], {})).toBe(0);
    });
});

describe('module cards', () => {
    beforeEach(() => { document.body.innerHTML = ''; });

    it('lists all seven modules, linking to their pages', () => {
        expect(MODULES.map(m => m.href)).toEqual([
            'verb_quiz.html', 'vocabulary_quiz.html', 'gender_quiz.html', 'ser_estar_ficar_quiz.html',
            'contractions_quiz.html', 'subjunctive_quiz.html', 'indirect_speech_quiz.html',
        ]);
    });

    it('shows how many items are due', () => {
        const due = { a: { nextReview: Date.now() - 1000 }, b: { nextReview: Date.now() - 1000 }, c: { nextReview: Date.now() + 1e9 } };
        expect(renderModuleCard(MODULES[0], due).querySelector('.module-due').textContent).toBe('2 por rever');
        expect(renderModuleCard(MODULES[0], {}).querySelector('.module-due').textContent).toBe('Nada por rever');
    });

    it('is a single link with a progress bar once progress is known', () => {
        const card = renderModuleCard(MODULES[1], {});
        addProgressBar(card, 40);
        expect(card.tagName).toBe('A');
        expect(card.getAttribute('href')).toBe('vocabulary_quiz.html');
        expect(card.querySelector('.progress-fill').style.width).toBe('40%');
        expect(card.querySelector('[role="progressbar"]').getAttribute('aria-valuenow')).toBe('40');
    });
});

describe('records left behind by renamed words', () => {
    const past = Date.now() - 1000;

    it('are not counted as due on the home card', () => {
        const state = { 'Adjetivos|||baixo|||short': { nextReview: past }, 'Cores|||azul|||blue': { nextReview: past } };
        expect(dueAmong(['Cores|||azul|||blue', 'Adjetivos|||baixo|||short (height)'], state)).toBe(1);
        const card = renderModuleCard(MODULES[1], state);
        setDueCount(card, dueAmong(['Cores|||azul|||blue'], state));
        expect(card.querySelector('.module-due').textContent).toBe('1 por rever');
    });

    it('are not counted as due on the setup screen', () => {
        document.body.innerHTML = '<p id="srs-due-count" class="hidden"></p>';
        const quiz = new VocabQuiz();
        quiz.data = { Cores: { azul: 'blue' }, Adjetivos: { baixo: 'short (height)' } };
        quiz.srsState = { 'Adjetivos|||baixo|||short': { nextReview: past }, 'Cores|||azul|||blue': { nextReview: past } };
        quiz._updateDueCount();
        expect(document.getElementById('srs-due-count').textContent).toBe('1 item para rever hoje');
    });
});

describe('deleting records left behind by renamed words', () => {
    const OLD = 'Adjetivos|||baixo|||short';
    const NEW = 'Adjetivos|||baixo|||short (height)';
    const record = { interval: 3, repetitions: 2, easeFactor: 2.5, nextReview: Date.now() - 1000 };
    const vocab = { Adjetivos: { baixo: 'short (height)' } };

    beforeEach(() => {
        localStorage.clear();
        localStorage.setItem('srs_vocab', JSON.stringify({ [OLD]: record, [NEW]: record }));
        localStorage.setItem('srs_verbs', JSON.stringify({ 'ser|||participios_passados|||ter': record }));
    });
    afterEach(() => vi.unstubAllGlobals());

    it('pruneRecords removes only keys that are not current', () => {
        const state = { [OLD]: record, [NEW]: record };
        expect(pruneRecords(state, new Set([NEW]))).toBe(1);
        expect(Object.keys(state)).toEqual([NEW]);
        expect(pruneRecords(state, new Set([NEW]))).toBe(0);
    });

    it('happens when the vocabulary quiz opens', () => {
        document.body.innerHTML = '<div id="categories"></div>';
        const quiz = new VocabQuiz();
        quiz.data = vocab;
        quiz.srsState = JSON.parse(localStorage.getItem('srs_vocab'));
        quiz.setupUI();
        expect(Object.keys(JSON.parse(localStorage.getItem('srs_vocab')))).toEqual([NEW]);
    });

    it('happens on the home page, which then shows the corrected mastered count, and leaves verbs alone', async () => {
        document.body.innerHTML = `
            <span id="streak-pill"></span><span id="streak-count"></span><span id="mastered-count"></span>
            <nav id="module-list"></nav>`;
        vi.stubGlobal('fetch', async (file) => ({
            ok: true,
            json: async () => (file === 'vocabulary.json' ? vocab : { ser: { presente: ['sou', 'és', 'é', 'somos', 'são'] } }),
        }));
        await renderHome();
        expect(Object.keys(JSON.parse(localStorage.getItem('srs_vocab')))).toEqual([NEW]);
        expect(Object.keys(JSON.parse(localStorage.getItem('srs_verbs')))).toEqual(['ser|||participios_passados|||ter']);
        expect(document.getElementById('mastered-count').textContent).toBe('2');
    });
});
