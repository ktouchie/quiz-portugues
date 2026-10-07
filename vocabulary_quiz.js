import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { buildGatedQuickPracticePool, buildOptions, stringSimilarity } from './practice.js';
import { getDueItems } from './srs.js';
import { vocabularyCategoryLevel } from './cefr.js';

/** How many of the most confusable words the three wrong options are drawn from, so the same
 *  word doesn't always get the same options (same as the Android app). */
const CONFUSABLE_SHORTLIST_SIZE = 8;

export class VocabQuiz extends QuizBase {
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
        const selectedCategories = getCheckedValues('categories');

        if (selectedCategories.length === 0) {
            alert('Por favor, selecione pelo menos uma categoria.');
            return null;
        }

        this.ENtoPT = document.querySelector("input[name='translation-direction']:checked").value === 'true';
        return this._itemsFor(selectedCategories);
    }

    getAllItems() {
        return this._itemsFor(Object.keys(this.data));
    }

    /** Quick Practice asks Portuguese → English, from unlocked CEFR tiers only, as on Android. */
    startQuickPractice() {
        this.ENtoPT = false;
        this.quickPractice = true;
        const dueKeys = new Set(getDueItems(this.srsState));
        this.startQuiz(buildGatedQuickPracticePool(
            this.getAllItems(), dueKeys, this.srsState, item => vocabularyCategoryLevel(item.category),
        ));
    }

    /**
     * Wrong options are the words most easily confused with this one (same as the Android app):
     * every other word is scored by the closer of two spellings, its answer-language word against
     * the prompt (false friends, e.g. "constipation" for "constipação") and its Portuguese word
     * against this Portuguese word (look-alikes, e.g. "irmã" for "irmão"). The options come from
     * the top few at random.
     */
    getOptions(key) {
        const [, ptWord, enWord] = key.split('|||');
        const prompt = this.ENtoPT ? enWord : ptWord;
        const answerOf = (item) => (this.ENtoPT ? item.ptWord : item.enWord);
        // Words with the same English are right answers too (see getAcceptedAnswers), so they're
        // never offered as wrong options.
        const ranked = this.getAllItems()
            .filter(item => item.key !== key && !sameMeaning(item.enWord, enWord))
            .map(item => ({
                answer: answerOf(item),
                score: Math.max(stringSimilarity(ptWord, item.ptWord), stringSimilarity(prompt, answerOf(item))),
            }))
            .sort((a, b) => b.score - a.score)
            .map(c => c.answer);
        return buildOptions(this.getCorrectAnswer(key), [
            ranked.slice(0, CONFUSABLE_SHORTLIST_SIZE),
            ranked.slice(CONFUSABLE_SHORTLIST_SIZE),
        ]);
    }

    _itemsFor(categories) {
        const items = [];
        for (const category of categories) {
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

    /**
     * Asked English → Portuguese, any word with the same English is right: "seven thirty" is both
     * sete e meia and dezanove e trinta. Words that only differ by a bracketed note, like
     * "short (height)" and "short (length)", are different words.
     */
    getAcceptedAnswers(key) {
        if (!this.ENtoPT) return [this.getCorrectAnswer(key)];
        const [, ptWord, enWord] = key.split('|||');
        const synonyms = this.getAllItems().filter(item => sameMeaning(item.enWord, enWord)).map(item => item.ptWord);
        return [ptWord, ...synonyms.filter(word => word !== ptWord)];
    }

    getLabel(key) {
        const [, ptWord, enWord] = key.split('|||');
        return `${ptWord} ↔ ${enWord}`;
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

function sameMeaning(a, b) {
    return a.trim().toLowerCase() === b.trim().toLowerCase();
}

function _strong(parent, text) {
    const s = document.createElement('strong');
    s.textContent = text;
    parent.appendChild(s);
}

document.addEventListener('DOMContentLoaded', () => new VocabQuiz().init());
