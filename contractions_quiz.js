import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { appendTemplate, categoryName, categorySpan, localized, t } from './i18n.js';
import { buildOptions, maskAnswer } from './practice.js';
import { contractionsCategoryLevel } from './cefr.js';

export class ContractionsQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.contractions);
    }

    async fetchData() {
        const res = await fetch('contractions.json');
        if (!res.ok) throw new Error('Failed to load contractions data.');
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

    /** The full example sentence, shown after a wrong answer. */
    getExample(key) {
        return this.itemData[key]?.example ?? null;
    }

    getItemLevel(item) {
        return contractionsCategoryLevel(item.category);
    }

    /**
     * Wrong options: contractions of the same preposition, then of the same article or
     * demonstrative, then any other (same as the Android app).
     */
    getOptions(key) {
        const item = this.itemData[key] ?? this.getAllItems().find(i => i.key === key);
        const all = this.getAllItems();
        return buildOptions(item.answer, [
            all.filter(i => i.parts[0] === item.parts[0]).map(i => i.answer),
            all.filter(i => i.parts[1] === item.parts[1]).map(i => i.answer),
            all.map(i => i.answer),
        ]);
    }

    _itemsFor(categories) {
        const items = [];
        for (const category of categories) {
            this.data[category].forEach((entry, index) => {
                items.push({
                    key: `${category}|||${index}`,
                    category,
                    parts: entry.parts,
                    answer: entry.answer,
                    example: entry.example || null,
                    english: entry.english || null,
                    hint: entry.hint || null,
                    hint_en: entry.hint_en || null,
                });
            });
        }
        return items;
    }

    renderQuestion(key) {
        const item = this.itemData[key];
        const el = document.getElementById('question');
        el.textContent = '';

        const s1 = document.createElement('strong');
        s1.className = 'irregular-verb';
        s1.textContent = item.parts[0];
        const s2 = document.createElement('strong');
        s2.className = 'person-color-3';
        s2.textContent = item.parts[1];
        appendTemplate(el, 'contractions.question', { first: s1, second: s2 });

        if (item.example) {
            const ex = document.createElement('p');
            ex.className = 'question-translation';
            // Blanked: the example would otherwise give the answer away.
            ex.textContent = maskAnswer(item.example, item.answer);
            if (item.english) ex.textContent += ' — ' + item.english;
            el.appendChild(ex);
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
        return `${item.parts[0]} + ${item.parts[1]} = ${item.answer}`;
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
        s.textContent = `${item.parts[0]} + ${item.parts[1]} = ${item.answer}`;
        li.appendChild(s);
        li.append(` (${t('result.mistakeCount', { n: count })})`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new ContractionsQuiz().init());
