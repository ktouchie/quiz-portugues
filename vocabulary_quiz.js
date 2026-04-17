import { addSelectAll } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';

class VocabQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.vocab);
        this.ENtoPT = true;
    }

    async fetchData() {
        const res = await fetch('vocabulary.json');
        if (!res.ok) throw new Error('Failed to load vocabulary data.');
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
        const selectedCategories = Array.from(
            document.querySelectorAll('#categories input:checked')
        ).map(i => i.value);

        if (selectedCategories.length === 0) {
            alert('Por favor, selecione pelo menos uma categoria.');
            return null;
        }

        this.ENtoPT = document.querySelector("input[name='translation-direction']:checked").value === 'true';

        const items = [];
        for (const category of selectedCategories) {
            for (const ptWord of Object.keys(this.data[category])) {
                const enWord = this.data[category][ptWord];
                items.push({ key: `${category}|||${ptWord}|||${enWord}`, category, ptWord, enWord });
            }
        }
        return items;
    }

    renderQuestion(key) {
        const [, ptWord, enWord] = key.split('|||');
        const el = document.getElementById('question');
        el.textContent = '';
        el.append('Traduza ');
        const s = document.createElement('strong');
        s.className = 'person-color-3';
        s.textContent = this.ENtoPT ? enWord : ptWord;
        el.append(s, this.ENtoPT ? ' em Português' : ' em Inglês');
    }

    getCorrectAnswer(key) {
        const [, ptWord, enWord] = key.split('|||');
        return this.ENtoPT ? ptWord : enWord;
    }

    formatMistake(key, count, index) {
        const [category, ptWord, enWord] = key.split('|||');
        const li = document.createElement('li');
        const badge = document.createElement('span');
        badge.className = 'category-badge';
        badge.textContent = category;
        li.append(`${index + 1}. `);
        li.appendChild(badge);
        li.append(' ');
        _strong(li, ptWord);
        li.append(' ← ');
        _strong(li, enWord);
        li.append(` (${count} erro${count > 1 ? 's' : ''})`);
        return li;
    }
}

function _strong(parent, text) {
    const s = document.createElement('strong');
    s.textContent = text;
    parent.appendChild(s);
}

document.addEventListener('DOMContentLoaded', () => new VocabQuiz().init());
