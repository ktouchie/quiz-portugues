import { addSelectAll, getCheckedValues } from './common.js';
import { QuizBase } from './quiz_base.js';
import { PERSONS, TENSE_LABELS, STORAGE_KEYS } from './config.js';
import { getVerbHint } from './grammar_hints.js';
import { buildGatedQuickPracticePool, buildOptions } from './practice.js';
import { getDueItems } from './srs.js';
import { verbItemLevel } from './cefr.js';
import { appendTemplate, t } from './i18n.js';

const TENSE_KEYS = Object.keys(TENSE_LABELS);

export class VerbQuiz extends QuizBase {
    constructor() {
        super(STORAGE_KEYS.verbs);
        this.selectedTenses = [];
        this.interleaved = false;
        this.difficultyFilter = 'all';
    }

    async fetchData() {
        const res = await fetch('verbs.json');
        if (!res.ok) throw new Error('Failed to load verbs data.');
        return res.json();
    }

    setupUI() {
        const tensesDiv = document.getElementById('tenses');
        TENSE_KEYS.forEach(tense => {
            const label = document.createElement('label');
            const cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.value = tense;
            label.appendChild(cb);
            label.appendChild(document.createTextNode(' ' + TENSE_LABELS[tense]));
            tensesDiv.appendChild(label);
        });
        addSelectAll('tenses');

        const ppCb = document.createElement('input');
        ppCb.type = 'checkbox';
        ppCb.value = 'participios_passados';
        const ppLabel = document.createElement('label');
        ppLabel.appendChild(ppCb);
        ppLabel.append(' ', _i18nSpan('verbs.participles'));
        tensesDiv.appendChild(ppLabel);

        // Difficulty selector
        // Difficulty and interleaving go with the other Advanced options, above the start button.
        const startButton = document.getElementById('start-quiz');
        const advanced = startButton.parentNode;

        const diffH = document.createElement('h2');
        diffH.dataset.i18n = 'verbs.difficulty';
        diffH.textContent = t('verbs.difficulty');
        advanced.insertBefore(diffH, startButton);

        const diffDiv = document.createElement('div');
        diffDiv.id = 'difficulty-selector';
        ['all', 'beginner', 'intermediate', 'advanced'].forEach((val, i) => {
            const radio = document.createElement('input');
            radio.type = 'radio';
            radio.name = 'difficulty';
            radio.value = val;
            radio.id = `diff-${val}`;
            if (i === 0) radio.checked = true;
            const lbl = document.createElement('label');
            lbl.htmlFor = `diff-${val}`;
            lbl.appendChild(radio);
            lbl.append(' ', _i18nSpan(`verbs.difficulty.${val}`));
            diffDiv.appendChild(lbl);
        });
        advanced.insertBefore(diffDiv, startButton);

        // Interleaved mode toggle
        const intLabel = document.createElement('label');
        intLabel.className = 'interleaved-label';
        const intCb = document.createElement('input');
        intCb.type = 'checkbox';
        intCb.id = 'interleaved-mode';
        intLabel.appendChild(intCb);
        intLabel.append(' ', _i18nSpan('verbs.interleaved'));
        advanced.insertBefore(intLabel, startButton);
    }

    getSelectedItems() {
        this.selectedTenses = getCheckedValues('tenses');

        if (this.selectedTenses.length === 0) {
            alert(t('verbs.selectTense'));
            return null;
        }

        this.difficultyFilter = document.querySelector("input[name='difficulty']:checked")?.value ?? 'all';
        this.interleaved = document.getElementById('interleaved-mode')?.checked ?? false;
        return this._itemsFor(this.selectedTenses, this.difficultyFilter);
    }

    /** Quick Practice draws from every conjugated form of every verb, as on Android. */
    getAllItems() {
        return this._itemsFor(TENSE_KEYS, 'all');
    }

    /** Quick Practice only draws from CEFR tiers the learner has unlocked, as on Android. */
    startQuickPractice() {
        this.quickPractice = true;
        const dueKeys = new Set(getDueItems(this.srsState));
        this.startQuiz(buildGatedQuickPracticePool(
            this.getAllItems(), dueKeys, this.srsState, item => verbItemLevel(item.verb, item.tense),
        ));
    }

    /**
     * Wrong options, most confusable first (same order as the Android app): the same verb and
     * person in other tenses (e.g. "fiz", "fazia" for "eu faço"), then other persons of the same
     * verb and tense, then other verbs in the same tense. Participles: the same verb with another
     * auxiliary, then other verbs' participles.
     */
    getOptions(key) {
        const [verb, tense, third] = key.split('|||');
        const correct = this.getCorrectAnswer(key);
        if (tense === 'participios_passados') {
            const participles = Object.entries(this.data).filter(([, v]) => v.participios_passados);
            return buildOptions(correct, [
                Object.values(this.data[verb].participios_passados),
                participles.filter(([name]) => name !== verb).map(([, v]) => v.participios_passados[third]),
            ]);
        }
        const personIdx = parseInt(third, 10);
        this._forms ??= this.getAllItems().map(i => ({ ...i, form: this.getCorrectAnswer(i.key) }));
        const forms = this._forms;
        return buildOptions(correct, [
            forms.filter(i => i.verb === verb && i.personIdx === personIdx).map(i => i.form),
            forms.filter(i => i.verb === verb && i.tense === tense).map(i => i.form),
            forms.filter(i => i.tense === tense).map(i => i.form),
        ]);
    }

    _itemsFor(tenses, difficultyFilter) {
        const items = [];
        const verbs = this.data;
        for (const verb in verbs) {
            if (difficultyFilter !== 'all' && verbs[verb].difficulty !== difficultyFilter) continue;
            for (const tense of tenses) {
                if (tense === 'participios_passados' && verbs[verb].participios_passados) {
                    for (const aux of ['ter', 'ser', 'estar']) {
                        items.push({ key: `${verb}|||participios_passados|||${aux}`, verb, tense: 'participios_passados', aux });
                    }
                } else if (verbs[verb][tense]) {
                    for (let personIdx = 0; personIdx < PERSONS.length; personIdx++) {
                        const form = verbs[verb][tense][personIdx];
                        if (form && form.length > 0) {
                            items.push({ key: `${verb}|||${tense}|||${personIdx}`, verb, tense, personIdx });
                        }
                    }
                }
            }
        }
        return items;
    }

    startQuiz(explicitItems = null) {
        super.startQuiz(explicitItems);
        if (this.interleaved && this.itemsToPractice.length > 0) {
            for (let i = this.itemsToPractice.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [this.itemsToPractice[i], this.itemsToPractice[j]] = [this.itemsToPractice[j], this.itemsToPractice[i]];
            }
        }
    }

    renderQuestion(key) {
        const [verb, tense, third] = key.split('|||');
        const verbs = this.data;
        const el = document.getElementById('question');
        el.textContent = '';

        if (tense === 'participios_passados') {
            const s1 = document.createElement('strong');
            s1.className = 'irregular-verb';
            _withTooltip(s1, verb, verbs[verb].english);
            const s2 = document.createElement('strong');
            s2.className = third;
            s2.textContent = third;
            appendTemplate(el, 'verbs.participleQuestion', { verb: s1, aux: s2 });
        } else {
            const personIdx = parseInt(third, 10);
            const s1 = document.createElement('strong');
            s1.className = verbs[verb].regular ? 'regular-verb' : 'irregular-verb';
            _withTooltip(s1, verb, verbs[verb].english);
            const s2 = document.createElement('strong');
            s2.className = `tense-color-${TENSE_KEYS.indexOf(tense)}`;
            s2.textContent = TENSE_LABELS[tense] || tense;
            const s3 = document.createElement('strong');
            s3.className = `person-color-${personIdx}`;
            s3.textContent = PERSONS[personIdx];
            appendTemplate(el, 'verbs.question', { verb: s1, tense: s2, person: s3 });
        }
    }

    getCorrectAnswer(key) {
        const [verb, tense, third] = key.split('|||');
        if (tense === 'participios_passados') {
            return this.data[verb].participios_passados[third];
        }
        return this.data[verb][tense][parseInt(third, 10)];
    }

    getHint(key) {
        const [verb, tense, third] = key.split('|||');
        return getVerbHint(tense, verb, third);
    }

    getExample(key) {
        const [verb, tense, third] = key.split('|||');
        if (tense === 'participios_passados') return null;
        const personIdx = parseInt(third, 10);
        return this.data[verb]?.exemplos?.[tense]?.[personIdx] ?? null;
    }

    getLabel(key) {
        const [verb, tense, third] = key.split('|||');
        if (tense === 'participios_passados') return `${verb} — particípio — ${third}`;
        return `${verb} — ${TENSE_LABELS[tense] || tense} — ${PERSONS[parseInt(third, 10)]}`;
    }

    formatMistake(key, count, index) {
        const [verb, tense, third] = key.split('|||');
        const li = document.createElement('li');
        const field = (labelKey, value) => {
            li.append(`${t(labelKey)}: `);
            _strong(li, value);
        };

        li.append(`${index + 1}. `);
        field('verbs.mistake.verb', verb);
        li.append(', ');
        if (tense === 'participios_passados') {
            field('verbs.mistake.tense', t('verbs.participles'));
            li.append(', ');
            field('verbs.mistake.auxiliary', third);
            li.append(', ');
            field('verbs.mistake.answer', this.data[verb].participios_passados[third]);
        } else {
            const personIdx = parseInt(third, 10);
            field('verbs.mistake.tense', TENSE_LABELS[tense] || tense);
            li.append(', ');
            field('verbs.mistake.person', PERSONS[personIdx]);
            li.append(', ');
            field('verbs.mistake.answer', this.data[verb][tense][personIdx]);
        }
        li.append(`, ${t('verbs.mistake.count')}: ${count}`);
        return li;
    }
}

/** A span whose text follows the interface language (see applyTranslations in i18n.js). */
function _i18nSpan(key) {
    const span = document.createElement('span');
    span.dataset.i18n = key;
    span.textContent = t(key);
    return span;
}

function _strong(parent, text) {
    const s = document.createElement('strong');
    s.textContent = text;
    parent.appendChild(s);
}

function _withTooltip(el, text, translation) {
    if (translation) {
        const span = document.createElement('span');
        span.className = 'pt-tooltip';
        span.dataset.tooltip = translation;
        span.textContent = text;
        el.appendChild(span);
    } else {
        el.textContent = text;
    }
}

document.addEventListener('DOMContentLoaded', () => new VerbQuiz().init());
