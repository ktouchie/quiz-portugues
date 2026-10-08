import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { cwd } from 'node:process';
import { GenderQuiz } from '../gender_quiz.js';

// Vitest runs from the repo root, where the content JSON lives.
const load = (file) => JSON.parse(readFileSync(join(cwd(), file), 'utf8'));

const nonEmptyString = (v) => typeof v === 'string' && v.trim().length > 0;

const CONJUGATION_TENSES = [
    'presente', 'pretérito', 'imperfeito', 'condicional', 'pretérito mais-que-perfeito',
    'perfeito_composto', 'futuro', 'imperativo', 'conjuntivo', 'infinitivo pessoal',
];

describe('verbs.json', () => {
    const verbs = load('verbs.json');
    const conjugated = Object.entries(verbs).filter(([, v]) => v.presente);

    it('every fully conjugated verb has a difficulty, a regular flag and 5 forms in every tense', () => {
        expect(conjugated.length).toBeGreaterThan(0);
        for (const [verb, entry] of conjugated) {
            expect(['beginner', 'intermediate', 'advanced'], verb).toContain(entry.difficulty);
            expect(typeof entry.regular, verb).toBe('boolean');
            for (const tense of CONJUGATION_TENSES) {
                // querer has no imperative in practice; every other verb must have every tense.
                if (tense === 'imperativo' && verb === 'querer') continue;
                const forms = entry[tense];
                expect(forms, `${verb} ${tense}`).toHaveLength(5);
                forms.forEach((form, person) => {
                    // The imperative has no "eu" form, so its first slot is intentionally empty.
                    if (tense === 'imperativo' && person === 0) {
                        expect(form, `${verb} imperativo eu`).toBe('');
                    } else {
                        expect(nonEmptyString(form), `${verb} ${tense} [${person}]`).toBe(true);
                    }
                });
            }
        }
    });

    it('every participle-only verb has ter/ser/estar participles', () => {
        for (const [verb, entry] of Object.entries(verbs).filter(([, v]) => !v.presente)) {
            for (const aux of ['ter', 'ser', 'estar']) {
                expect(nonEmptyString(entry.participios_passados?.[aux]), `${verb} ${aux}`).toBe(true);
            }
        }
    });

    it('uses estar’s own participle in its pluperfect, not ser’s', () => {
        expect(verbs.estar['pretérito mais-que-perfeito']).toEqual(
            ['tinha estado', 'tinhas estado', 'tinha estado', 'tínhamos estado', 'tinham estado'],
        );
    });

    it('has the corrected participles for cultivar and limpar', () => {
        expect(verbs.cultivar.participios_passados).toEqual({ ter: 'cultivado', ser: 'cultivado', estar: 'cultivado' });
        expect(verbs.limpar.participios_passados.ter).toBe('limpado');
    });
});

describe('vocabulary.json', () => {
    const vocab = load('vocabulary.json');

    it('maps every Portuguese word to a non-empty English translation', () => {
        for (const [category, words] of Object.entries(vocab)) {
            expect(Object.keys(words).length, category).toBeGreaterThan(0);
            for (const [pt, en] of Object.entries(words)) {
                expect(nonEmptyString(pt) && nonEmptyString(en), `${category}: ${pt}`).toBe(true);
            }
        }
    });

    it('uses European Portuguese spellings and correct translations', () => {
        const all = Object.assign({}, ...Object.values(vocab));
        expect(all).toHaveProperty('dezanove e trinta');
        expect(all).not.toHaveProperty('dezenove e trinta');
        expect(all).toHaveProperty('catorze horas');
        expect(all).not.toHaveProperty('quatorze horas');
        expect(all).toHaveProperty('bom apetite');
        expect(all).not.toHaveProperty('bom aproveito');
        expect(all.madrugada).not.toMatch(/dusk/);
    });

    it('never gives two different words the same English, except agreed interchangeable pairs', () => {
        // Asked English → Portuguese, either word in an agreed pair is accepted. Any other clash
        // needs a bracketed note to tell the words apart, like "short (height)" for baixo.
        const interchangeable = [['dezanove e trinta', 'sete e meia']];
        const byEnglish = {};
        for (const words of Object.values(vocab)) {
            for (const [pt, en] of Object.entries(words)) {
                (byEnglish[en.trim().toLowerCase()] ??= new Set()).add(pt);
            }
        }
        const clashes = Object.values(byEnglish).filter(words => words.size > 1).map(words => [...words].sort());
        expect(clashes).toEqual(interchangeable);
    });

    it('tells baixo from curto and atrás from costas', () => {
        const all = Object.assign({}, ...Object.values(vocab));
        expect(all.baixo).toBe('short (height)');
        expect(all.curto).toBe('short (length)');
        expect(all['atrás']).toBe('behind');
        expect(all.costas).toBe('back (body)');
    });
});

describe('category-based content files', () => {
    const shapes = {
        'gender_quiz.json': ['masculine', 'plural', 'english'],
        'ser_estar_ficar.json': ['sentence', 'answer', 'hint', 'english'],
        'contractions.json': ['answer', 'example', 'english', 'hint'],
        'subjunctive_quiz.json': ['prompt', 'answer', 'trigger', 'hint', 'english'],
    };

    for (const [file, fields] of Object.entries(shapes)) {
        it(`${file} has non-empty categories with the expected fields`, () => {
            const data = load(file);
            for (const [category, items] of Object.entries(data)) {
                expect(items.length, category).toBeGreaterThan(0);
                for (const item of items) {
                    for (const field of fields) {
                        expect(nonEmptyString(item[field]), `${category}: ${field}`).toBe(true);
                    }
                }
            }
        });
    }

    it('ser_estar_ficar.json sentences contain the ___ blank the quiz splits on', () => {
        for (const items of Object.values(load('ser_estar_ficar.json'))) {
            for (const item of items) {
                expect(item.sentence).toContain('___');
            }
        }
    });

    it('subjunctive_quiz.json prompts have a "___ (infinitive)" blank', () => {
        for (const items of Object.values(load('subjunctive_quiz.json'))) {
            for (const item of items) {
                expect(item.prompt).toMatch(/___ \([^)]+\)/);
            }
        }
    });

    it('contractions.json items have a [preposition, article] pair', () => {
        for (const items of Object.values(load('contractions.json'))) {
            for (const item of items) {
                expect(item.parts).toHaveLength(2);
            }
        }
    });

    it('gender_quiz.json feminine is a word or explicitly null', () => {
        for (const items of Object.values(load('gender_quiz.json'))) {
            for (const item of items) {
                expect(item.feminine === null || nonEmptyString(item.feminine)).toBe(true);
            }
        }
    });
});

describe('indirect_speech.json', () => {
    it('is a non-empty list of items with the expected fields', () => {
        const items = load('indirect_speech.json');
        expect(items.length).toBeGreaterThan(0);
        for (const item of items) {
            for (const field of ['direct', 'context', 'verb_direct', 'answer', 'rule', 'indirect_full', 'english']) {
                expect(nonEmptyString(item[field]), `${item.direct}: ${field}`).toBe(true);
            }
        }
    });
});

describe('GenderQuiz', () => {
    it('labels plural questions "plural", not "plural masculino" (some nouns are feminine)', () => {
        const quiz = new GenderQuiz();
        quiz.data = { Teste: [{ masculine: 'mão', feminine: null, plural: 'mãos', english: 'hand' }] };
        const items = quiz.getSelectedItems();
        expect(items).toHaveLength(1);
        expect(items[0].label).toBe('plural');
    });
});

describe('version.txt', () => {
    it('is MAJOR.MINOR.PATCH with parts below 1000 (the Android build derives its version from it)', () => {
        const version = readFileSync(join(cwd(), 'version.txt'), 'utf8').trim();
        expect(version).toMatch(/^\d{1,3}\.\d{1,3}\.\d{1,3}$/);
    });
});
