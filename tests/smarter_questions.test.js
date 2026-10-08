import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { cwd } from 'node:process';
import {
    CEFR_LEVELS, TENSE_CEFR_LEVEL, TIER_UNLOCK_THRESHOLD, VERB_CEFR_LEVEL, VOCABULARY_CATEGORY_CEFR_LEVEL,
    unlockedTiers, verbItemLevel, vocabularyCategoryLevel,
} from '../cefr.js';
import { TENSE_LABELS } from '../config.js';
import { answerMatches, buildGatedQuickPracticePool, stringSimilarity, QUICK_PRACTICE_CAP } from '../practice.js';
import { isReadyForTyping, sm2 } from '../srs.js';
import { QuizBase } from '../quiz_base.js';
import { VerbQuiz } from '../script.js';
import { VocabQuiz } from '../vocabulary_quiz.js';

const readJson = (file) => JSON.parse(readFileSync(join(cwd(), file), 'utf8'));
const seen = (keys) => Object.fromEntries(keys.map(k => [k, { repetitions: 1, interval: 1, easeFactor: 2.5, nextReview: Date.now() + 86_400_000 }]));

describe('CEFR levels', () => {
    it('covers every verb, tense and vocabulary category in the content files', () => {
        // Participle-only verbs (e.g. aceitar) aren't conjugated, so they never reach Quick Practice.
        const conjugated = Object.entries(readJson('verbs.json'))
            .filter(([, entry]) => Object.keys(TENSE_LABELS).some(t => entry[t]))
            .map(([verb]) => verb);
        expect(conjugated).toHaveLength(26);
        expect(conjugated.filter(v => !VERB_CEFR_LEVEL[v])).toEqual([]);
        expect(Object.keys(TENSE_LABELS).filter(t => !TENSE_CEFR_LEVEL[t])).toEqual([]);
        expect(Object.keys(readJson('vocabulary.json')).filter(c => !VOCABULARY_CATEGORY_CEFR_LEVEL[c])).toEqual([]);
    });

    it('gives a verb item the harder of its verb level and its tense level', () => {
        expect(verbItemLevel('falar', 'presente')).toBe('A1');
        expect(verbItemLevel('falar', 'conjuntivo')).toBe('B1');
        expect(verbItemLevel('vir', 'presente')).toBe('C2');
    });

    it('throws for anything without a level instead of guessing one', () => {
        expect(() => verbItemLevel('nadar', 'presente')).toThrow(/nadar/);
        expect(() => vocabularyCategoryLevel('Desportos')).toThrow(/Desportos/);
    });
});

describe('unlockedTiers', () => {
    const byLevel = {
        A1: Array.from({ length: 10 }, (_, i) => `a1-${i}`),
        A2: Array.from({ length: 10 }, (_, i) => `a2-${i}`),
        B1: ['b1-0'],
    };

    it('opens only A1 for a new learner', () => {
        expect(unlockedTiers(byLevel, {})).toEqual(new Set(['A1']));
    });

    it('opens the next tier once 80% of the previous one has been answered correctly', () => {
        expect(unlockedTiers(byLevel, seen(byLevel.A1.slice(0, 7)))).toEqual(new Set(['A1']));
        expect(unlockedTiers(byLevel, seen(byLevel.A1.slice(0, 8)))).toEqual(new Set(['A1', 'A2']));
    });

    it('does not count items that were only ever answered wrong', () => {
        const wrongOnly = Object.fromEntries(byLevel.A1.map(k => [k, { repetitions: 0, interval: 1, easeFactor: 2.3, nextReview: 1 }]));
        expect(unlockedTiers(byLevel, wrongOnly)).toEqual(new Set(['A1']));
    });

    it('skips over tiers with no content', () => {
        const all = [...byLevel.A1, ...byLevel.A2, ...byLevel.B1];
        expect(unlockedTiers(byLevel, seen(all))).toEqual(new Set(CEFR_LEVELS));
    });
});

describe('buildGatedQuickPracticePool', () => {
    const items = [
        ...Array.from({ length: 10 }, (_, i) => ({ key: `a1-${i}`, level: 'A1' })),
        ...Array.from({ length: 20 }, (_, i) => ({ key: `a2-${i}`, level: 'A2' })),
        ...Array.from({ length: 20 }, (_, i) => ({ key: `b1-${i}`, level: 'B1' })),
    ];
    const levelOf = (item) => item.level;

    it('only draws from unlocked tiers', () => {
        const pool = buildGatedQuickPracticePool(items, new Set(), {}, levelOf);
        expect(pool).toHaveLength(10);
        expect(pool.every(i => i.level === 'A1')).toBe(true);
    });

    it('tops up from the newest unlocked tier first', () => {
        const state = seen(items.filter(i => i.level === 'A1').map(i => i.key));
        const pool = buildGatedQuickPracticePool(items, new Set(), state, levelOf);
        expect(pool).toHaveLength(QUICK_PRACTICE_CAP);
        expect(pool.every(i => i.level === 'A2')).toBe(true);
    });

    it('still puts due items first, but never ones from a locked tier', () => {
        const due = new Set(['a1-3', 'b1-0']);
        const pool = buildGatedQuickPracticePool(items, due, {}, levelOf);
        expect(pool[0].key).toBe('a1-3');
        expect(pool.some(i => i.key === 'b1-0')).toBe(false);
    });
});

describe('isReadyForTyping', () => {
    it('needs three correct reviews in a row and a six-day interval', () => {
        expect(isReadyForTyping(undefined)).toBe(false);
        expect(isReadyForTyping({ repetitions: 2, interval: 8 })).toBe(false);
        expect(isReadyForTyping({ repetitions: 3, interval: 5 })).toBe(false);
        expect(isReadyForTyping({ repetitions: 3, interval: 6 })).toBe(true);
    });

    it('is reached after three first-try answers and lost again after one wrong answer', () => {
        const item = { interval: 0, repetitions: 0, easeFactor: 2.5, nextReview: 0 };
        sm2(item, 4); sm2(item, 4);
        expect(isReadyForTyping(item)).toBe(false);
        sm2(item, 4);
        expect(isReadyForTyping(item)).toBe(true);
        sm2(item, 0);
        expect(isReadyForTyping(item)).toBe(false);
    });
});

describe('stringSimilarity', () => {
    it('is 1 for the same word, ignoring case', () => {
        expect(stringSimilarity('Irmão', 'irmão')).toBe(1);
        expect(stringSimilarity('', '')).toBe(1);
    });

    it('ranks look-alikes above unrelated words', () => {
        expect(stringSimilarity('irmão', 'irmã')).toBeGreaterThan(stringSimilarity('irmão', 'cavalo'));
        expect(stringSimilarity('constipação', 'constipation')).toBeGreaterThan(0.5);
    });

    it('treats accents as real differences', () => {
        expect(stringSimilarity('e', 'é')).toBe(0);
    });
});

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

class OneItemQuiz extends QuizBase {
    constructor() { super('bestScore_one'); }
    async fetchData() { return {}; }
    getSelectedItems() { return [{ key: 'gato', answer: 'cat' }]; }
    getOptions() { return ['dog', 'cat', 'bird', 'fish']; }
    renderQuestion() {}
    getCorrectAnswer() { return 'cat'; }
    formatMistake() { return document.createElement('li'); }
}

describe('multiple choice until an item is ready for typing', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    function start(record) {
        const quiz = new OneItemQuiz();
        if (record) quiz.srsState.gato = record;
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuiz();
        return quiz;
    }

    const buttons = () => [...document.querySelectorAll('#options button.option')];

    it('shows lettered options for an item that is new or not yet solid', () => {
        start({ repetitions: 2, interval: 3, easeFactor: 2.5, nextReview: 0 });
        expect(buttons().map(b => b.querySelector('.option-badge').textContent)).toEqual(['A', 'B', 'C', 'D']);
        expect(document.getElementById('answer').classList.contains('hidden')).toBe(true);
    });

    it('asks for a typed answer once the item is ready', () => {
        const quiz = start({ repetitions: 3, interval: 8, easeFactor: 2.5, nextReview: 0 });
        expect(quiz.currentOptions).toBeNull();
        expect(document.getElementById('answer').classList.contains('hidden')).toBe(false);
    });

    it('after an answer, keeps the options on screen, locked, with the right and wrong ones marked', () => {
        start();
        buttons().find(b => b.textContent.endsWith('dog')).click();
        const marked = (cls) => buttons().filter(b => b.classList.contains(cls)).map(b => b.querySelector('.option-text').textContent);
        expect(marked('option--correct')).toEqual(['cat']);
        expect(marked('option--wrong')).toEqual(['dog']);
        expect(buttons().every(b => b.disabled)).toBe(true);
        expect(document.getElementById('answer-container').classList.contains('hidden')).toBe(false);
    });
});

describe('VerbQuiz options', () => {
    function makeVerbs() {
        const quiz = new VerbQuiz();
        quiz.data = {
            fazer: {
                presente: ['faço', 'fazes', 'faz', 'fazemos', 'fazem'],
                pretérito: ['fiz', 'fizeste', 'fez', 'fizemos', 'fizeram'],
                imperfeito: ['fazia', 'fazias', 'fazia', 'fazíamos', 'faziam'],
                futuro: ['farei', 'farás', 'fará', 'faremos', 'farão'],
                participios_passados: { ter: 'feito', ser: 'feito', estar: 'feito' },
            },
            falar: {
                presente: ['falo', 'falas', 'fala', 'falamos', 'falam'],
                participios_passados: { ter: 'falado', ser: 'falado', estar: 'falado' },
            },
            ser: { presente: ['sou', 'és', 'é', 'somos', 'são'] },
        };
        return quiz;
    }

    it('offers the same verb and person in other tenses first', () => {
        const options = makeVerbs().getOptions('fazer|||presente|||0');
        expect(new Set(options)).toEqual(new Set(['faço', 'fiz', 'fazia', 'farei']));
    });

    it('then other persons of the same verb and tense', () => {
        const quiz = makeVerbs();
        quiz.data.fazer = { presente: quiz.data.fazer.presente };
        const options = quiz.getOptions('fazer|||presente|||0');
        expect(options).toContain('faço');
        expect(options.filter(o => o !== 'faço').every(o => quiz.data.fazer.presente.includes(o))).toBe(true);
    });

    it('falls back to other verbs for participles that look the same with every auxiliary', () => {
        const options = makeVerbs().getOptions('fazer|||participios_passados|||ter');
        expect(options).toContain('feito');
        expect(options).toContain('falado');
        expect(new Set(options).size).toBe(options.length);
    });

    it('starts Quick Practice on beginner content only', () => {
        const quiz = new VerbQuiz();
        quiz.data = readJson('verbs.json');
        setupDOM();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        vi.useFakeTimers();
        quiz.startQuickPractice();
        vi.useRealTimers();
        const keys = Object.keys(quiz.itemData);
        expect(keys).toHaveLength(QUICK_PRACTICE_CAP);
        expect(keys.every(k => {
            const [verb, tense] = k.split('|||');
            return verbItemLevel(verb, tense) === 'A1';
        })).toBe(true);
    });
});

describe('VocabQuiz options', () => {
    it('draws the wrong options from the most confusable words', () => {
        const quiz = new VocabQuiz();
        // Exactly CONFUSABLE_SHORTLIST_SIZE (8) look-alikes, so the shortlist holds nothing else.
        const lookAlikes = {
            irmã: 'sister', irmãos: 'siblings', irmãs: 'sisters', irmandade: 'brotherhood',
            irmanado: 'twinned', irmanar: 'to twin', irmo: 'brotherly', armão: 'big arm',
        };
        const unrelated = Object.fromEntries(Array.from({ length: 30 }, (_, i) => [`zzzzzzzzzz${i}`, `qqqqqqqqqq${i}`]));
        quiz.data = { Parentesco: { irmão: 'brother', ...lookAlikes }, Diversos: unrelated };
        quiz.ENtoPT = false;
        for (let run = 0; run < 20; run++) {
            const options = quiz.getOptions('Parentesco|||irmão|||brother');
            expect(options).toHaveLength(4);
            expect(options).toContain('brother');
            expect(options.filter(o => o !== 'brother').every(o => Object.values(lookAlikes).includes(o))).toBe(true);
        }
    });

    it('starts Quick Practice on A1 categories only', () => {
        const quiz = new VocabQuiz();
        quiz.data = readJson('vocabulary.json');
        setupDOM();
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        vi.useFakeTimers();
        quiz.startQuickPractice();
        vi.useRealTimers();
        const categories = Object.values(quiz.itemData).map(i => i.category);
        expect(categories).toHaveLength(QUICK_PRACTICE_CAP);
        expect(categories.every(c => vocabularyCategoryLevel(c) === 'A1')).toBe(true);
    });
});

describe('answers that share a meaning', () => {
    const VOCAB = {
        Adjetivos: { baixo: 'short (height)', curto: 'short (length)', alto: 'tall' },
        'Horas do Dia': { 'sete e meia': 'seven thirty', 'dezanove e trinta': 'seven thirty', 'oito horas': 'eight o\'clock' },
        Cores: { azul: 'blue', verde: 'green', preto: 'black' },
    };

    function makeVocab(ENtoPT) {
        const quiz = new VocabQuiz();
        quiz.data = VOCAB;
        quiz.ENtoPT = ENtoPT;
        return quiz;
    }

    it('makes a bracketed note optional in a typed answer', () => {
        expect(answerMatches('short', 'short (height)')).toBe(true);
        expect(answerMatches(' Short (Height) ', 'short (height)')).toBe(true);
        expect(answerMatches('class', 'class (subject)')).toBe(true);
        expect(answerMatches('short (length)', 'short (height)')).toBe(false);
        expect(answerMatches('height', 'short (height)')).toBe(false);
    });

    it('accepts "short" for baixo and for curto when asked in English', () => {
        const quiz = makeVocab(false);
        expect(quiz.getAcceptedAnswers('Adjetivos|||baixo|||short (height)').some(a => answerMatches('short', a))).toBe(true);
        expect(quiz.getAcceptedAnswers('Adjetivos|||curto|||short (length)').some(a => answerMatches('short', a))).toBe(true);
    });

    it('keeps baixo and curto apart when asked in Portuguese', () => {
        const quiz = makeVocab(true);
        expect(quiz.getAcceptedAnswers('Adjetivos|||baixo|||short (height)')).toEqual(['baixo']);
    });

    it('accepts either sete e meia or dezanove e trinta for "seven thirty"', () => {
        const quiz = makeVocab(true);
        expect(new Set(quiz.getAcceptedAnswers('Horas do Dia|||sete e meia|||seven thirty')))
            .toEqual(new Set(['sete e meia', 'dezanove e trinta']));
    });

    it('never offers the other "seven thirty" as a wrong option', () => {
        const quiz = makeVocab(true);
        for (let run = 0; run < 20; run++) {
            const options = quiz.getOptions('Horas do Dia|||sete e meia|||seven thirty');
            expect(options).toContain('sete e meia');
            expect(options).not.toContain('dezanove e trinta');
        }
    });

    it('marks the other "seven thirty" right when it is typed', () => {
        setupDOM();
        vi.useFakeTimers();
        const quiz = makeVocab(true);
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.srsState['Horas do Dia|||sete e meia|||seven thirty'] = { repetitions: 3, interval: 8, easeFactor: 2.5, nextReview: 0 };
        quiz.startQuiz([{ key: 'Horas do Dia|||sete e meia|||seven thirty' }]);
        quiz.submitAnswer('dezanove e trinta');
        vi.useRealTimers();
        expect(quiz.correctCount).toBe(1);
    });
});

describe('CEFR levels match the Android app', () => {
    const kotlin = readFileSync(join(cwd(), 'android/app/src/main/java/com/ktouchie/quizportugues/content/CefrTiers.kt'), 'utf8');

    /** Reads one `val NAME: Map<String, CefrLevel> = mapOf("x" to A1, ...)` from CefrTiers.kt. */
    function kotlinMap(name) {
        const start = kotlin.indexOf(`val ${name}`);
        expect(start, name).toBeGreaterThan(-1);
        const body = kotlin.slice(start, kotlin.indexOf('\n)', start));
        return Object.fromEntries([...body.matchAll(/"([^"]+)" to (A1|A2|B1|B2|C1|C2)/g)].map(m => [m[1], m[2]]));
    }

    it('has the same verb, tense and vocabulary category levels', () => {
        expect(kotlinMap('VERB_CEFR_LEVEL')).toEqual(VERB_CEFR_LEVEL);
        expect(kotlinMap('TENSE_CEFR_LEVEL')).toEqual(TENSE_CEFR_LEVEL);
        expect(kotlinMap('VOCABULARY_CATEGORY_CEFR_LEVEL')).toEqual(VOCABULARY_CATEGORY_CEFR_LEVEL);
    });

    it('unlocks at the same threshold', () => {
        const progression = readFileSync(join(cwd(), 'android/app/src/main/java/com/ktouchie/quizportugues/content/ContentProgression.kt'), 'utf8');
        expect(progression).toMatch(/const val TIER_UNLOCK_THRESHOLD = 0\.8\b/);
        expect(TIER_UNLOCK_THRESHOLD).toBe(0.8);
    });
});
