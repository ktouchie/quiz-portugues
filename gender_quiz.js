import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { getGenderHint } from './grammar_hints.js';
import { appendTemplate, t } from './i18n.js';

export class GenderQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.gender);
    }

    async fetchData() {
        const res = await fetch('gender_quiz.json');
        if (!res.ok) throw new Error('Failed to load gender quiz data.');
        return res.json();
    }

    getSelectedItems() {
        const items = [];
        for (const category of Object.keys(this.data)) {
            this.data[category].forEach((word, index) => {
                if (word.feminine !== null) {
                    items.push({
                        key: `${category}|||${index}|||f`,
                        masculine: word.masculine,
                        english: word.english,
                        label: 'feminino',
                        answer: word.feminine,
                    });
                }
                items.push({
                    key: `${category}|||${index}|||p`,
                    masculine: word.masculine,
                    english: word.english,
                    label: 'plural',
                    answer: word.plural,
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
        s1.className = 'person-color-1';
        s1.textContent = t(`gender.form.${item.label}`);
        const s2 = document.createElement('strong');
        s2.className = 'person-color-3';
        if (item.english) {
            const span = document.createElement('span');
            span.className = 'pt-tooltip';
            span.dataset.tooltip = item.english;
            span.textContent = item.masculine;
            s2.appendChild(span);
        } else {
            s2.textContent = item.masculine;
        }
        appendTemplate(el, 'gender.question', { form: s1, word: s2 });
    }

    getCorrectAnswer(key) {
        return this.itemData[key].answer;
    }

    getHint(key) {
        const [category] = key.split('|||');
        return getGenderHint(category);
    }

    getLabel(key) {
        const item = this.itemData[key];
        if (!item) return key;
        return `${item.masculine} (${item.label})`;
    }

    formatMistake(key, count, _index) {
        const item = this.itemData[key];
        const li = document.createElement('li');
        li.append(`${item.masculine} → ${t(`gender.form.${item.label}`)}: `);
        const s = document.createElement('strong');
        s.textContent = item.answer;
        li.append(s, ' ');
        const span = document.createElement('span');
        span.style.color = 'var(--text-muted)';
        span.textContent = `(${t('result.mistakeCount', { n: count })})`;
        li.append(span);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new GenderQuiz().init());
