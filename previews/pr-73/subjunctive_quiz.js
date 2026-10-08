import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { STORAGE_KEYS } from './config.js';
import { categoryName, categorySpan, localized, t, triggerText } from './i18n.js';
import { buildOptions } from './practice.js';
import { subjunctiveCategoryLevel } from './cefr.js';

/** The infinitive in a prompt's "___ (vir)" brackets, or null. */
export function subjunctiveInfinitive(prompt) {
    return prompt.match(/\(([^)]+)\)/)?.[1] ?? null;
}

const SUBJECT_PERSONS = [
    [/\beu\b/, 0], [/\btu\b/, 1], [/\b(ele|ela|você)(?!\p{L})/u, 2], [/\bnós(?!\p{L})/u, 3], [/\b(eles|elas|vocês)(?!\p{L})/u, 4],
];

/**
 * The present indicative of a verb for the subject named in the prompt (third person singular
 * when none is named), or null — the Android app's indicativeDistractor.
 * @param {{ presente?: string[] }|undefined} verb - the verb's verbs.json entry
 * @param {string} prompt
 * @returns {string|null}
 */
export function indicativeForm(verb, prompt) {
    const lower = prompt.toLowerCase();
    const person = SUBJECT_PERSONS.find(([pattern]) => pattern.test(lower))?.[1] ?? 2;
    return verb?.presente?.[person] || null;
}

export class SubjunctiveQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.subjunctive);
    }

    async fetchData() {
        const res = await fetch('subjunctive_quiz.json');
        if (!res.ok) throw new Error('Failed to load subjunctive quiz data.');
        // verbs.json gives the indicative forms offered as wrong options; the quiz works without it.
        this.verbs = await fetch('verbs.json').then(r => (r.ok ? r.json() : {})).catch(() => ({}));
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
        return subjunctiveCategoryLevel(item.category);
    }

    /**
     * Wrong options (same as the Android app): the indicative present of the same verb and person
     * (the classic mistake: "vem" for "venha"), then the same verb's other subjunctive answers,
     * then any other answer.
     */
    getOptions(key) {
        const item = this.itemData[key] ?? this.getAllItems().find(i => i.key === key);
        const all = this.getAllItems();
        const infinitive = subjunctiveInfinitive(item.prompt);
        const indicative = infinitive ? indicativeForm(this.verbs?.[infinitive], item.prompt) : null;
        return buildOptions(item.answer, [
            indicative ? [indicative] : [],
            all.filter(i => infinitive && subjunctiveInfinitive(i.prompt) === infinitive).map(i => i.answer),
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
                    prompt: entry.prompt,
                    answer: entry.answer,
                    trigger: entry.trigger || null,
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
            triggerEl.textContent = t('subjunctive.trigger', { trigger: triggerText(item.trigger) });
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
        return localized(this.itemData[key], 'hint') || null;
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
        badge.textContent = categoryName(item.category);
        li.append(`${index + 1}. `);
        li.appendChild(badge);
        li.append(' ');
        const s = document.createElement('strong');
        s.textContent = item.prompt.replace(/___.*$/, `[${item.answer}]`);
        li.appendChild(s);
        li.append(` (${t('result.mistakeCount', { n: count })})`);
        return li;
    }
}

document.addEventListener('DOMContentLoaded', () => new SubjunctiveQuiz().init());
