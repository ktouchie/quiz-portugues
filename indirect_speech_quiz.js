import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';

class IndirectSpeechQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.indirectSpeech);
    }

    async fetchData() {
        const res = await fetch('indirect_speech.json');
        if (!res.ok) throw new Error('Failed to load indirect speech data.');
        return res.json();
    }

    getSelectedItems() {
        return this.data.map((entry, index) => ({
            key: String(index),
            direct: entry.direct,
            context: entry.context,
            verb_direct: entry.verb_direct,
            answer: entry.answer,
            rule: entry.rule,
            indirect_full: entry.indirect_full,
            english: entry.english,
            hint: entry.hint || null,
        }));
    }

    renderQuestion(key) {
        const item = this.itemData[key];
        const el = document.getElementById('question');
        el.textContent = '';

        const contextEl = document.createElement('p');
        contextEl.className = 'person-color-1';
        contextEl.style.fontWeight = '600';
        contextEl.textContent = item.context;
        el.appendChild(contextEl);

        const directEl = document.createElement('p');
        directEl.className = 'example-sentence';
        directEl.textContent = item.direct;
        el.appendChild(directEl);

        const promptEl = document.createElement('p');
        promptEl.append('Em discurso indireto, como fica ');
        const s = document.createElement('strong');
        s.className = 'irregular-verb';
        s.textContent = `"${item.verb_direct}"`;
        promptEl.appendChild(s);
        promptEl.append('?');
        el.appendChild(promptEl);

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
        const item = this.itemData[key];
        const parts = [item.rule];
        if (item.indirect_full) parts.push(`Ex: ${item.indirect_full}`);
        return parts.join(' — ');
    }

    getLabel(key) {
        const item = this.itemData[key];
        if (!item) return key;
        return `${item.verb_direct} → ${item.answer} (${item.rule})`;
    }

    formatMistake(key, count, index) {
        const item = this.itemData[key];
        const li = document.createElement('li');
        li.append(`${index + 1}. `);
        const s1 = document.createElement('strong');
        s1.textContent = item.verb_direct;
        li.appendChild(s1);
        li.append(' → ');
        const s2 = document.createElement('strong');
        s2.textContent = item.answer;
        li.appendChild(s2);
        li.append(` (${item.rule})`);
        li.append(` — ${count} erro${count > 1 ? 's' : ''}`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new IndirectSpeechQuiz().init());
