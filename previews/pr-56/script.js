import { addSelectAll } from './common.js';
import { QuizBase } from './quiz_base.js';
import { PERSONS, TENSE_LABELS, STORAGE_KEYS } from './config.js';
import { getVerbHint } from './grammar_hints.js';

const TENSE_KEYS = Object.keys(TENSE_LABELS);

class VerbQuiz extends QuizBase {
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
        ppLabel.appendChild(document.createTextNode(' particípios passados'));
        tensesDiv.appendChild(ppLabel);

        // Difficulty selector
        const setup = document.getElementById('setup');
        const dueSibling = document.getElementById('srs-due-count');

        const diffH = document.createElement('h2');
        diffH.textContent = 'Nível de dificuldade';
        setup.insertBefore(diffH, dueSibling);

        const diffDiv = document.createElement('div');
        diffDiv.id = 'difficulty-selector';
        [['all', 'Todos'], ['beginner', 'Iniciante'], ['intermediate', 'Intermédio'], ['advanced', 'Avançado']].forEach(([val, label], i) => {
            const radio = document.createElement('input');
            radio.type = 'radio';
            radio.name = 'difficulty';
            radio.value = val;
            radio.id = `diff-${val}`;
            if (i === 0) radio.checked = true;
            const lbl = document.createElement('label');
            lbl.htmlFor = `diff-${val}`;
            lbl.appendChild(radio);
            lbl.appendChild(document.createTextNode(' ' + label));
            diffDiv.appendChild(lbl);
        });
        setup.insertBefore(diffDiv, dueSibling);

        // Interleaved mode toggle
        const intLabel = document.createElement('label');
        intLabel.className = 'interleaved-label';
        const intCb = document.createElement('input');
        intCb.type = 'checkbox';
        intCb.id = 'interleaved-mode';
        intLabel.appendChild(intCb);
        intLabel.appendChild(document.createTextNode(' Modo intercalado (melhor para retenção)'));
        setup.insertBefore(intLabel, dueSibling);
    }

    getSelectedItems() {
        this.selectedTenses = Array.from(
            document.querySelectorAll('#tenses input:checked')
        ).map(i => i.value);

        if (this.selectedTenses.length === 0) {
            alert('Por favor, selecione pelo menos um tempo verbal.');
            return null;
        }

        this.difficultyFilter = document.querySelector("input[name='difficulty']:checked")?.value ?? 'all';
        this.interleaved = document.getElementById('interleaved-mode')?.checked ?? false;

        const items = [];
        const verbs = this.data;
        for (const verb in verbs) {
            if (this.difficultyFilter !== 'all' && verbs[verb].difficulty !== this.difficultyFilter) continue;
            for (const tense of this.selectedTenses) {
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

    startQuiz() {
        super.startQuiz();
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
            el.append('Qual é o particípio correto para o verbo ');
            const s1 = document.createElement('strong');
            s1.className = 'irregular-verb';
            _withTooltip(s1, verb, verbs[verb].english);
            el.append(s1, ' usado com o verbo auxiliar ');
            const s2 = document.createElement('strong');
            s2.className = third;
            s2.textContent = third;
            el.append(s2, '?');
        } else {
            const personIdx = parseInt(third, 10);
            const verbClass = verbs[verb].regular ? 'regular-verb' : 'irregular-verb';
            const tenseClass = `tense-color-${TENSE_KEYS.indexOf(tense)}`;
            const personClass = `person-color-${personIdx}`;

            el.append('Conjugue o verbo ');
            const s1 = document.createElement('strong');
            s1.className = verbClass;
            _withTooltip(s1, verb, verbs[verb].english);
            el.append(s1, ' no tempo ');
            const s2 = document.createElement('strong');
            s2.className = tenseClass;
            s2.textContent = TENSE_LABELS[tense] || tense;
            el.append(s2, ' para ');
            const s3 = document.createElement('strong');
            s3.className = personClass;
            s3.textContent = PERSONS[personIdx];
            el.append(s3, ':');
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

        if (tense === 'participios_passados') {
            const answer = this.data[verb].participios_passados[third];
            li.append(`${index + 1}. Verbo: `);
            _strong(li, verb);
            li.append(', Tempo: ');
            _strong(li, 'particípios passados');
            li.append(', Auxiliar: ');
            _strong(li, third);
            li.append(', Resposta: ');
            _strong(li, answer);
            li.append(`, Erros: ${count}`);
        } else {
            const personIdx = parseInt(third, 10);
            const answer = this.data[verb][tense][personIdx];
            li.append(`${index + 1}. Verbo: `);
            _strong(li, verb);
            li.append(', Tempo: ');
            _strong(li, TENSE_LABELS[tense] || tense);
            li.append(', Pessoa: ');
            _strong(li, PERSONS[personIdx]);
            li.append(', Resposta: ');
            _strong(li, answer);
            li.append(`, Erros: ${count}`);
        }
        return li;
    }
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
