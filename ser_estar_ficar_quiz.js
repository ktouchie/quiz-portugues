import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { categoryName, categorySpan, localized, t } from './i18n.js';

class SerEstarFicarQuiz extends QuizBase {
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

        const items = [];
        for (const category of selected) {
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
