import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { cwd } from 'node:process';
import {
    CATEGORY_NAMES_EN, STRINGS, appendTemplate, applyTranslations, categoryName, categorySpan, getLanguage,
    initLanguage, localized, setLanguage, t, triggerText,
} from '../i18n.js';
import { getGenderHint, getVerbHint } from '../grammar_hints.js';
import { QuizBase } from '../quiz_base.js';

const read = (file) => readFileSync(join(cwd(), file), 'utf8');
const readJson = (file) => JSON.parse(read(file));

beforeEach(() => localStorage.clear());

describe('interface language', () => {
    it('is English by default', () => {
        expect(getLanguage()).toBe('en');
        expect(t('home.nothingDue')).toBe('Nothing to review');
    });

    it('switches to Portuguese and remembers it', () => {
        setLanguage('pt');
        expect(getLanguage()).toBe('pt');
        expect(localStorage.getItem('language')).toBe('pt');
        expect(t('home.nothingDue')).toBe('Nada por rever');
        expect(document.documentElement.lang).toBe('pt-PT');
    });

    it('tells listeners when it changes', () => {
        const listener = vi.fn();
        document.addEventListener('languagechange', listener);
        setLanguage('pt');
        document.removeEventListener('languagechange', listener);
        expect(listener).toHaveBeenCalledOnce();
    });

    it('has every string in both languages, with the same slots', () => {
        for (const [key, entry] of Object.entries(STRINGS)) {
            expect(typeof entry.en, key).toBe(typeof entry.pt);
            if (typeof entry.en === 'string') {
                const slots = (text) => [...text.matchAll(/\{(\w+)\}/g)].map(m => m[1]).sort();
                expect(slots(entry.en), key).toEqual(slots(entry.pt));
            }
        }
    });

    it('fills slots with values or with elements', () => {
        expect(t('quiz.dueToday', { n: 1 })).toBe('1 item due for review today');
        expect(t('quiz.dueToday', { n: 2 })).toBe('2 items due for review today');
        const el = document.createElement('p');
        const word = document.createElement('strong');
        word.textContent = 'gato';
        appendTemplate(el, 'vocab.toEnglish', { word });
        expect(el.textContent).toBe('Translate gato into English');
        expect(el.querySelector('strong')).toBe(word);
    });

    it('throws on a missing string rather than showing a key', () => {
        expect(() => t('no.such.key')).toThrow(/no\.such\.key/);
    });
});

describe('page markup', () => {
    const pages = readdirSync(cwd()).filter(f => f.endsWith('.html'));

    it('only refers to strings that exist', () => {
        for (const page of pages) {
            const html = read(page);
            const keys = [
                ...[...html.matchAll(/data-i18n(?:-html)?="([^"]+)"/g)].map(m => m[1]),
                ...[...html.matchAll(/data-i18n-attr="([^"]+)"/g)].flatMap(m => m[1].split(';').map(p => p.split(':')[1])),
            ];
            for (const key of keys) expect(STRINGS, `${page}: ${key}`).toHaveProperty([key]);
        }
    });

    it('has every page load the language switch', () => {
        for (const page of pages) {
            const html = read(page);
            const usesQuizBase = /<script type="module" src="[^"]+\.js"><\/script>/.test(html);
            expect(usesQuizBase || html.includes('initLanguage()'), page).toBe(true);
        }
    });

    it('translates text, markup and attributes, and adds the switch', () => {
        document.body.innerHTML = `
            <button id="theme-toggle"></button>
            <h1 data-i18n="verbs.title">Quiz dos Verbos Portugueses</h1>
            <p data-i18n-html="sef.intro"></p>
            <input data-i18n-attr="placeholder:sef.placeholder">`;
        initLanguage();
        expect(document.querySelector('h1').textContent).toBe('Portuguese Verb Quiz');
        expect(document.querySelectorAll('p em')).toHaveLength(3);
        expect(document.querySelector('input').placeholder).toBe('type the verb form');

        const toggle = document.getElementById('lang-toggle');
        expect(toggle.textContent).toBe('EN');
        toggle.click();
        expect(toggle.textContent).toBe('PT');
        expect(document.querySelector('h1').textContent).toBe('Quiz dos Verbos Portugueses');
        applyTranslations();
        expect(document.querySelector('input').placeholder).toBe('escreva a forma verbal');
    });
});

describe('grammar explanations', () => {
    const files = ['ser_estar_ficar.json', 'contractions.json', 'subjunctive_quiz.json', 'indirect_speech.json'];
    const items = (file) => {
        const data = readJson(file);
        return Array.isArray(data) ? data : Object.values(data).flat();
    };

    it('come in English as well as Portuguese in every content file', () => {
        for (const file of files) {
            for (const item of items(file)) {
                for (const field of ['hint', 'rule']) {
                    if (!item[field]) continue;
                    expect(item[`${field}_en`], `${file}: ${item[field]}`).toEqual(expect.any(String));
                    expect(item[`${field}_en`].length, `${file}: ${item[field]}`).toBeGreaterThan(0);
                }
            }
        }
    });

    it('are actually in English where they contain words', () => {
        const portuguese = /\b(presente|pretérito|imperfeito|conjuntivo|expressão|Localização|Estado|Mudança|Profissão)\b/;
        for (const file of files) {
            for (const item of items(file)) {
                if (item.hint_en) expect(item.hint_en, `${file}: ${item.hint}`).not.toMatch(portuguese);
            }
        }
    });

    it('follow the interface language', () => {
        const item = { hint: 'Estado temporário → estar', hint_en: 'Temporary state → estar' };
        expect(localized(item, 'hint')).toBe('Temporary state → estar');
        setLanguage('pt');
        expect(localized(item, 'hint')).toBe('Estado temporário → estar');
        expect(localized({ hint: 'só em português' }, 'hint')).toBe('só em português');
    });

    it('cover verbs and gender in both languages', () => {
        expect(getVerbHint('futuro', 'falar', '0')).toMatch(/^The future/);
        expect(getGenderHint('Palavras em -ão')).toMatch(/^Words in -ão/);
        setLanguage('pt');
        expect(getVerbHint('futuro', 'falar', '0')).toMatch(/^O futuro/);
        expect(getGenderHint('Palavras em -ão')).toMatch(/^Palavras em -ão/);
    });

    it('show the "nós" and "tu" hints for the person in the item key', () => {
        // Item keys carry the person as text; these hints never showed before.
        expect(getVerbHint('presente', 'falar', '3')).toMatch(/"nós" form has no accent.*falámos/);
        expect(getVerbHint('imperativo', 'falar', '1')).toMatch(/"tu" imperative/);
        expect(getVerbHint('participios_passados', 'falar', 'ser')).toMatch(/^With "ser"/);
    });
});

describe('switching language mid-quiz', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    class OneQuestion extends QuizBase {
        constructor() { super('bestScore_lang'); }
        async fetchData() { return {}; }
        getSelectedItems() { return [{ key: 'a' }, { key: 'b' }]; }
        renderQuestion() { document.getElementById('question').textContent = t('quiz.next'); }
        getCorrectAnswer() { return 'x'; }
        formatMistake() { return document.createElement('li'); }
    }

    it('re-renders the question and the score in the new language', () => {
        document.body.innerHTML = `
            <div id="setup"></div><div id="quiz" class="hidden"></div><div id="result" class="hidden"></div>
            <div id="score-display"></div><div id="timer-display"></div><p id="question"></p>
            <div id="answer-container"><input id="answer"><button id="submit-answer"></button></div>
            <button id="next-question"></button><p id="feedback"></p>
            <div id="progress-bar"></div><p id="progress-percentage"></p>`;
        const quiz = new OneQuestion();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        const onChange = () => quiz._onLanguageChange();
        document.addEventListener('languagechange', onChange);
        setLanguage('pt');
        document.removeEventListener('languagechange', onChange);
        expect(document.getElementById('question').textContent).toBe('Próxima');
        expect(document.getElementById('score-display').textContent).toBe('Corretas: 0 | Erros: 0');
        expect(document.getElementById('progress-percentage').textContent).toMatch(/^Progresso/);
    });
});

describe('review fixes', () => {
    it('still switches when the browser blocks storage', () => {
        const setItem = vi.spyOn(window.Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
        setLanguage('pt');
        expect(getLanguage()).toBe('pt');
        expect(t('home.nothingDue')).toBe('Nada por rever');
        setItem.mockRestore();
        setLanguage('en'); // storable again: back to the normal path
        expect(getLanguage()).toBe('en');
    });

    function quizDom() {
        document.body.innerHTML = `
            <div id="setup"></div><div id="quiz" class="hidden"></div><div id="result" class="hidden"></div>
            <div id="score-display"></div><div id="timer-display"></div><p id="question"></p>
            <div id="answer-container"><input id="answer"><button id="submit-answer"></button></div>
            <button id="next-question"></button><p id="feedback"></p>
            <div id="progress-bar"></div><p id="progress-percentage"></p>
            <p id="total-score"></p><p id="quiz-time"></p><p id="accuracy"></p><p id="best-score"></p>
            <ol id="top-mistakes"></ol><button id="retry-mistakes" class="hidden"></button>`;
    }

    class Hinted extends QuizBase {
        constructor() { super('bestScore_hinted'); }
        async fetchData() { return {}; }
        getSelectedItems() { return [{ key: 'a' }]; }
        renderQuestion() {}
        getCorrectAnswer() { return 'x'; }
        getHint() { return getVerbHint('futuro', 'falar', '0'); }
        formatMistake() { return document.createElement('li'); }
    }

    it('redraws the feedback for the last answer in the new language', () => {
        vi.useFakeTimers();
        quizDom();
        const quiz = new Hinted();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        quiz.submitAnswer('wrong');
        expect(document.getElementById('feedback').textContent).toMatch(/^Wrong\..*The future/);
        setLanguage('pt');
        quiz._onLanguageChange();
        expect(document.getElementById('feedback').textContent).toMatch(/^Errado\..*O futuro/);
        vi.useRealTimers();
    });

    it('redraws the results in the new language', () => {
        vi.useFakeTimers();
        quizDom();
        const quiz = new Hinted();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        quiz.submitAnswer('wrong');
        quiz.endQuiz();
        expect(document.getElementById('total-score').textContent).toBe('Correct: 0 | Wrong: 1');
        expect(document.getElementById('retry-mistakes').textContent).toBe('Practise mistakes (1)');
        setLanguage('pt');
        quiz._onLanguageChange();
        expect(document.getElementById('total-score').textContent).toBe('Corretas: 0 | Erros: 1');
        expect(document.getElementById('retry-mistakes').textContent).toBe('Praticar erros (1)');
        expect(document.getElementById('best-score').textContent).toMatch(/^Novo recorde/);
        expect(document.getElementById('accuracy').dataset.label).toBe('PRECISÃO');
        vi.useRealTimers();
    });
});

describe('category names', () => {
    const files = ['vocabulary.json', 'gender_quiz.json', 'ser_estar_ficar.json', 'contractions.json', 'subjunctive_quiz.json'];

    it('have an English name for every category in every content file', () => {
        for (const file of files) {
            for (const category of Object.keys(readJson(file))) {
                expect(CATEGORY_NAMES_EN, `${file}: ${category}`).toHaveProperty([category]);
            }
        }
    });

    it('show in the interface language and follow a switch', () => {
        const span = categorySpan('Cores');
        document.body.replaceChildren(span);
        expect(span.textContent).toBe('Colours');
        setLanguage('pt');
        expect(span.textContent).toBe('Cores');
        expect(categoryName('Tempo')).toBe('Tempo');
    });

    it('translate only the notes inside subjunctive triggers', () => {
        expect(triggerText('quando (futuro)')).toBe('quando (future)');
        expect(triggerText('pedir (passado) que')).toBe('pedir (past) que');
        expect(triggerText('é preciso que')).toBe('é preciso que');
        setLanguage('pt');
        expect(triggerText('quando (futuro)')).toBe('quando (futuro)');
    });

    it('match the Android app for vocabulary', () => {
        const kotlin = read('android/app/src/main/java/com/ktouchie/quizportugues/ui/i18n/CategoryNames.kt');
        const android = Object.fromEntries([...kotlin.matchAll(/"([^"]+)" to "([^"]+)"/g)].map(m => [m[1], m[2]]));
        const vocabulary = Object.keys(readJson('vocabulary.json'));
        expect(android).toEqual(Object.fromEntries(vocabulary.map(c => [c, CATEGORY_NAMES_EN[c]])));
    });
});
