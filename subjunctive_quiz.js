import { addSelectAll } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';

class SubjunctiveQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.subjunctive);
    }

    async fetchData() {
        const res = await fetch('subjunctive_quiz.json');
        if (!res.ok) throw new Error('Failed to load subjunctive quiz data.');
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
            label.appendChild(document.createTextNode(' ' + category));
            categoryDiv.appendChild(label);
        });
        addSelectAll('categories');
    }

    getSelectedItems() {
        const selected = Array.from(
            document.querySelectorAll('#categories input:checked')
        ).map(i => i.value);

        if (selected.length === 0) {
            alert('Por favor, selecione pelo menos uma categoria.');
            return null;
        }

        const items = [];
        for (const category of selected) {
            this.data[category].forEach((entry, index) => {
                items.push({
                    key: `${category}|||${index}`,
                    category,
                    prompt: entry.prompt,
                    answer: entry.answer,
                    trigger: entry.trigger || null,
                    hint: entry.hint || null,
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

        // Split on ___ (verb) pattern — show the infinitive as a hint
        const match = item.prompt.match(/^(.*?)___ \(([^)]+)\)(.*)$/);
        if (match) {
            el.append(match[1]);
            const blank = document.createElement('span');
            blank.className = 'fill-blank';
            blank.textContent = '___';
            el.appendChild(blank);
            const inf = document.createElement('span');
            inf.className = 'person-color-1';
            inf.textContent = ` (${match[2]})`;
            el.appendChild(inf);
            if (match[3]) el.append(match[3]);
        } else {
            el.textContent = item.prompt;
        }

        if (item.trigger) {
            const triggerEl = document.createElement('p');
            triggerEl.className = 'question-translation';
            triggerEl.textContent = `Gatilho: ${item.trigger}`;
            el.appendChild(triggerEl);
        }

        if (item.english) {
            const engEl = document.createElement('p');
            engEl.className = 'question-translation';
            engEl.style.fontStyle = 'italic';
            engEl.textContent = item.english;
            el.appendChild(engEl);
        }
    }

    getCorrectAnswer(key) {
        return this.itemData[key].answer;
    }

    getHint(key) {
        return this.itemData[key].hint || null;
    }

    getLabel(key) {
        const item = this.itemData[key];
        if (!item) return key;
        return item.prompt.replace(/___.*$/, `[${item.answer}]`);
    }

    formatMistake(key, count, index) {
        const item = this.itemData[key];
        const li = document.createElement('li');
        const badge = document.createElement('span');
        badge.className = 'category-badge';
        badge.textContent = item.category;
        li.append(`${index + 1}. `);
        li.appendChild(badge);
        li.append(' ');
        const s = document.createElement('strong');
        s.textContent = item.prompt.replace(/___.*$/, `[${item.answer}]`);
        li.appendChild(s);
        li.append(` (${count} erro${count > 1 ? 's' : ''})`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new SubjunctiveQuiz().init());
