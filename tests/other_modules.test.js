import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { cwd } from 'node:process';
import {
    CONTRACTIONS_CATEGORY_CEFR_LEVEL, GENDER_CATEGORY_CEFR_LEVEL, INDIRECT_SPEECH_CEFR_LEVEL,
    SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL, SUBJUNCTIVE_CATEGORY_CEFR_LEVEL,
} from '../cefr.js';
import { categoryItemKeys, genderItemKeys } from '../home.js';
import { GenderQuiz } from '../gender_quiz.js';
import { SerEstarFicarQuiz, serEstarFicarVerbDistractors } from '../ser_estar_ficar_quiz.js';
import { ContractionsQuiz } from '../contractions_quiz.js';
import { SubjunctiveQuiz, indicativeForm, subjunctiveInfinitive } from '../subjunctive_quiz.js';
import { IndirectSpeechQuiz } from '../indirect_speech_quiz.js';
import { QUICK_PRACTICE_CAP } from '../practice.js';

const read = (file) => readFileSync(join(cwd(), file), 'utf8');
const readJson = (file) => JSON.parse(read(file));

function quizDom() {
    document.body.innerHTML = `
        <div id="setup"></div><div id="quiz" class="hidden"></div><div id="result" class="hidden"></div>
        <div id="score-display"></div><div id="timer-display"></div><p id="question"></p>
        <div id="answer-container"><input id="answer"><button id="submit-answer"></button></div>
        <button id="next-question"></button><p id="feedback"></p>
        <div id="progress-bar"></div><p id="progress-percentage"></p>`;
}

function make(QuizClass, file) {
    const quiz = new QuizClass();
    quiz.data = readJson(file);
    return quiz;
}

describe('CEFR levels for the other five modules', () => {
    const maps = [
        ['gender_quiz.json', GENDER_CATEGORY_CEFR_LEVEL, 'GENDER_CATEGORY_CEFR_LEVEL'],
        ['ser_estar_ficar.json', SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL, 'SER_ESTAR_FICAR_CATEGORY_CEFR_LEVEL'],
        ['contractions.json', CONTRACTIONS_CATEGORY_CEFR_LEVEL, 'CONTRACTIONS_CATEGORY_CEFR_LEVEL'],
        ['subjunctive_quiz.json', SUBJUNCTIVE_CATEGORY_CEFR_LEVEL, 'SUBJUNCTIVE_CATEGORY_CEFR_LEVEL'],
    ];
    const kotlin = read('android/app/src/main/java/com/ktouchie/quizportugues/content/CefrTiers.kt');
    function kotlinMap(name) {
        const start = kotlin.indexOf(`val ${name}`);
        const body = kotlin.slice(start, kotlin.indexOf('\n)', start));
        return Object.fromEntries([...body.matchAll(/"([^"]+)" to (A1|A2|B1|B2|C1|C2)/g)].map(m => [m[1], m[2]]));
    }

    it('cover every category in the content files', () => {
        for (const [file, map] of maps) {
            expect(Object.keys(readJson(file)).filter(c => !map[c]), file).toEqual([]);
        }
    });

    it('match the Android app', () => {
        for (const [, map, name] of maps) expect(kotlinMap(name), name).toEqual(map);
        expect(kotlin).toMatch(new RegExp(`cefrLevelOf\\(item: IndirectSpeechQuizItem\\): CefrLevel = ${INDIRECT_SPEECH_CEFR_LEVEL}\\b`));
    });
});

describe('home page progress for the other five modules', () => {
    it('counts the same items as each quiz', () => {
        expect(genderItemKeys(readJson('gender_quiz.json')))
            .toEqual(make(GenderQuiz, 'gender_quiz.json').getAllItems().map(i => i.key));
        for (const [Quiz, file] of [[SerEstarFicarQuiz, 'ser_estar_ficar.json'], [ContractionsQuiz, 'contractions.json'], [SubjunctiveQuiz, 'subjunctive_quiz.json']]) {
            expect(categoryItemKeys(readJson(file)), file).toEqual(make(Quiz, file).getAllItems().map(i => i.key));
        }
        expect(make(IndirectSpeechQuiz, 'indirect_speech.json').getAllItems().map(i => i.key))
            .toEqual(readJson('indirect_speech.json').map((_, i) => String(i)));
    });
});

describe('multiple-choice options', () => {
    const fourDistinct = (options, answer) => {
        expect(options).toHaveLength(4);
        expect(options).toContain(answer);
        expect(new Set(options.map(o => o.toLowerCase())).size).toBe(4);
    };

    it('gender: offers the answers spelt most like the right one', () => {
        const quiz = make(GenderQuiz, 'gender_quiz.json');
        const item = quiz.getAllItems().find(i => i.answer === 'trabalhadora');
        quiz.itemData = { [item.key]: item };
        const options = quiz.getOptions(item.key);
        fourDistinct(options, 'trabalhadora');
    });

    it('ser/estar/ficar: offers the same person of the other two verbs first', () => {
        expect(serEstarFicarVerbDistractors('sou')).toEqual(['estou', 'fiquei']);
        expect(serEstarFicarVerbDistractors('ficou')).toEqual(['é', 'está']);
        expect(serEstarFicarVerbDistractors('xyz')).toEqual([]);
        const quiz = make(SerEstarFicarQuiz, 'ser_estar_ficar.json');
        const item = quiz.getAllItems().find(i => i.answer === 'é');
        quiz.itemData = { [item.key]: item };
        const options = quiz.getOptions(item.key);
        fourDistinct(options, 'é');
        expect(options).toEqual(expect.arrayContaining(['está', 'ficou']));
    });

    it('contractions: offers the same preposition first', () => {
        const quiz = make(ContractionsQuiz, 'contractions.json');
        const item = quiz.getAllItems().find(i => i.answer === 'do');
        quiz.itemData = { [item.key]: item };
        const options = quiz.getOptions(item.key);
        fourDistinct(options, 'do');
        expect(options.filter(o => o !== 'do').every(o => /^d/.test(o))).toBe(true);
    });

    it('subjunctive: offers the indicative of the same verb and person first', () => {
        expect(subjunctiveInfinitive('Quero que ele ___ (vir) mais cedo.')).toBe('vir');
        const verbs = readJson('verbs.json');
        expect(indicativeForm(verbs.vir, 'Quero que ele ___ (vir) mais cedo.')).toBe('vem');
        expect(indicativeForm(verbs.vir, 'Espero que nós ___ (vir) cedo.')).toBe('vimos');
        expect(indicativeForm(verbs.vir, 'Embora ___ (vir) cedo.')).toBe('vem');

        const quiz = make(SubjunctiveQuiz, 'subjunctive_quiz.json');
        quiz.verbs = verbs;
        const item = quiz.getAllItems().find(i => i.prompt.includes('(vir)') && i.answer === 'venha');
        quiz.itemData = { [item.key]: item };
        const options = quiz.getOptions(item.key);
        fourDistinct(options, 'venha');
        expect(options).toContain('vem');
    });

    it('indirect speech: offers the unchanged verb of the original sentence', () => {
        const quiz = make(IndirectSpeechQuiz, 'indirect_speech.json');
        const item = quiz.getAllItems()[0];
        quiz.itemData = { [item.key]: item };
        const options = quiz.getOptions(item.key);
        fourDistinct(options, item.answer);
        expect(options).toContain(item.verb_direct);
    });
});

describe('Quick Practice for the other five modules', () => {
    beforeEach(() => {
        quizDom();
        localStorage.clear();
        vi.useFakeTimers();
    });
    afterEach(() => vi.useRealTimers());

    function start(QuizClass, file) {
        const quiz = make(QuizClass, file);
        quiz.timerState.timerDisplay = document.getElementById('timer-display');
        quiz.startQuickPractice();
        return quiz;
    }

    it('starts a new learner on the lowest level of each module', () => {
        const contractions = start(ContractionsQuiz, 'contractions.json');
        expect(Object.values(contractions.itemData).every(i => CONTRACTIONS_CATEGORY_CEFR_LEVEL[i.category] === 'A1')).toBe(true);

        // Nothing in the subjunctive is below B1, so the empty A1/A2 tiers open by themselves.
        const subjunctive = start(SubjunctiveQuiz, 'subjunctive_quiz.json');
        expect(Object.keys(subjunctive.itemData)).toHaveLength(QUICK_PRACTICE_CAP);
        expect(Object.values(subjunctive.itemData).every(i => SUBJUNCTIVE_CATEGORY_CEFR_LEVEL[i.category] === 'B1')).toBe(true);
    });

    it('asks every module with multiple choice first', () => {
        for (const [Quiz, file] of [[GenderQuiz, 'gender_quiz.json'], [SerEstarFicarQuiz, 'ser_estar_ficar.json'], [ContractionsQuiz, 'contractions.json'], [IndirectSpeechQuiz, 'indirect_speech.json']]) {
            quizDom();
            const quiz = start(Quiz, file);
            expect(quiz.currentOptions, file).toHaveLength(4);
            expect(document.getElementById('answer').classList.contains('hidden'), file).toBe(true);
        }
    });
});

describe('contraction examples', () => {
    it('are shown with the answer blanked, so they do not give it away', async () => {
        const { maskAnswer } = await import('../practice.js');
        expect(maskAnswer('O livro é do professor.', 'do')).toBe('O livro é ___ professor.');
        expect(maskAnswer('Do outro lado, dou-lhe isto.', 'do')).toBe('___ outro lado, dou-lhe isto.');
        for (const item of make(ContractionsQuiz, 'contractions.json').getAllItems()) {
            if (!item.example) continue;
            const masked = maskAnswer(item.example, item.answer);
            expect(masked.toLowerCase().split(/[^\p{L}]+/u), item.example).not.toContain(item.answer.toLowerCase());
        }
    });

    it('show in full after a wrong answer', () => {
        const quiz = make(ContractionsQuiz, 'contractions.json');
        const item = quiz.getAllItems().find(i => i.answer === 'do');
        quiz.itemData = { [item.key]: item };
        expect(quiz.getExample(item.key)).toBe(item.example);
    });
});
