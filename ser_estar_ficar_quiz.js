import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { categoryName, categorySpan, localized, t } from './i18n.js';
import { buildOptions } from './practice.js';
import { serEstarFicarCategoryLevel } from './cefr.js';

const SER_PRESENTE = { 0: 'sou', 1: 'és', 2: 'é', 3: 'somos', 4: 'são' };
const ESTAR_PRESENTE = { 0: 'estou', 1: 'estás', 2: 'está', 3: 'estamos', 4: 'estão' };
const FICAR_PRESENTE = { 2: 'fica' };
const FICAR_PRETERITO = { 0: 'fiquei', 2: 'ficou', 3: 'ficámos', 4: 'ficaram' };

/**
 * The same person of the other two verbs, for a ser/estar/ficar answer ("sou" → "estou", "ficou"…),
 * same as the Android app's SerEstarFicarConjugations.
 * @param {string} answer
 * @returns {string[]}
 */
export function serEstarFicarVerbDistractors(answer) {
    const forms = { ser: SER_PRESENTE, estar: ESTAR_PRESENTE, ficar: { ...FICAR_PRESENTE, ...FICAR_PRETERITO } };
    for (const [verb, table] of Object.entries(forms)) {
        const person = Object.keys(table).find(p => table[p] === answer);
        if (person === undefined) continue;
        return Object.keys(forms).filter(other => other !== verb)
            .map(other => (other === 'ficar' ? (FICAR_PRETERITO[person] ?? FICAR_PRESENTE[person]) : forms[other][person]))
            .filter(Boolean);
    }
    return [];
}

export class SerEstarFicarQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.serEstarFicar);
    }

    async fetchData() {
        const res = await fetch('ser_estar_ficar.json');
        if (!res.ok) throw new Error('Failed to load ser/estar/ficar data.');
        return res.json();
    }

    setupUI() {
        const categoryDiv = document.getElementById('categories');
        Object.keys(this.data).forEach(category => {
            const label = document.createElement('label');
            const cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.value = category;
            label.appendChild(cb);
            label.append(' ', categorySpan(category));
            categoryDiv.appendChild(label);
        });
        addSelectAll('categories');
    }

    getSelectedItems() {
        const selected = getCheckedValues('categories');

        if (selected.length === 0) {
            alert(t('quiz.selectCategory'));
            return null;
        }

        return this._itemsFor(selected);
    }

    getAllItems() {
        return this._itemsFor(Object.keys(this.data));
    }

    getItemLevel(item) {
        return serEstarFicarCategoryLevel(item.category);
    }

    /**
     * Wrong options: the same person of the other two verbs first ("estou" for "sou"), then other
     * answers from the module — same as the Android app's SerEstarFicarConjugations.
     */
    getOptions(key) {
        const answer = this.getCorrectAnswer(key);
        const others = this.getAllItems().map(item => item.answer).filter(other => other !== answer);
        return buildOptions(answer, [serEstarFicarVerbDistractors(answer), others]);
    }

    _itemsFor(categories) {
        const items = [];
        for (const category of categories) {
            this.data[category].forEach((entry, index) => {
                items.push({
                    key: `${category}|||${index}`,
                    category,
                    sentence: entry.sentence,
                    answer: entry.answer,
                    hint: entry.hint || null,
                    hint_en: entry.hint_en || null,
                    english: entry.english || null,
                });
            });
        }
        return items;
    }

    renderQuestion(key) {
        const item = this.itemData[key];
        const el = document.getElementById('question');
        el.textContent = '';

        const parts = item.sentence.split('___');
        el.append(parts[0]);
        const blank = document.createElement('span');
        blank.className = 'fill-blank';
        blank.textContent = '___';
        el.appendChild(blank);
        if (parts[1]) el.append(parts[1]);

        if (item.english) {
            const hint = document.createElement('p');
            hint.className = 'question-translation';
            hint.textContent = item.english;
            el.appendChild(hint);
        }
    }

    getCorrectAnswer(key) {
        return this.itemData[key].answer;
    }

    getHint(key) {
        return localized(this.itemData[key], 'hint') || null;
    }

    getLabel(key) {
        const item = this.itemData[key];
        if (!item) return key;
        return item.sentence.replace('___', `[${item.answer}]`);
    }

    formatMistake(key, count, index) {
        const item = this.itemData[key];
        const li = document.createElement('li');
        const badge = document.createElement('span');
        badge.className = 'category-badge';
        badge.textContent = categoryName(item.category);
        li.append(`${index + 1}. `);
        li.appendChild(badge);
        li.append(' ');
        const s = document.createElement('strong');
        s.textContent = item.sentence.replace('___', `[${item.answer}]`);
        li.appendChild(s);
        li.append(` (${t('result.mistakeCount', { n: count })})`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new SerEstarFicarQuiz().init());
