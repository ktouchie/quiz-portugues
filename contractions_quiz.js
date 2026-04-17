import { addSelectAll } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';

class ContractionsQuiz extends QuizBase {
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
                    parts: entry.parts,
                    answer: entry.answer,
                    example: entry.example || null,
                    english: entry.english || null,
                    hint: entry.hint || null,
                });
            });
        }
        return items;
    }

    renderQuestion(key) {
        const item = this.itemData[key];
        const el = document.getElementById('question');
        el.textContent = '';

        el.append('Qual é a contração de ');
        const s1 = document.createElement('strong');
        s1.className = 'irregular-verb';
        s1.textContent = item.parts[0];
        el.appendChild(s1);
        el.append(' + ');
        const s2 = document.createElement('strong');
        s2.className = 'person-color-3';
        s2.textContent = item.parts[1];
        el.appendChild(s2);
        el.append('?');

        if (item.example) {
            const ex = document.createElement('p');
            ex.className = 'question-translation';
            ex.textContent = item.example;
            if (item.english) ex.textContent += ' — ' + item.english;
            el.appendChild(ex);
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
        return `${item.parts[0]} + ${item.parts[1]} = ${item.answer}`;
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
        s.textContent = `${item.parts[0]} + ${item.parts[1]} = ${item.answer}`;
        li.appendChild(s);
        li.append(` (${count} erro${count > 1 ? 's' : ''})`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new ContractionsQuiz().init());
