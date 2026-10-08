/**
 * Interface language: English (default) or Portuguese, for instructions, labels and grammar
 * explanations — never for the Portuguese being learnt (words, verb forms, example sentences).
 * Same strings as the Android app's ui/i18n/Strings.kt. Static page text is marked up with
 * data-i18n / data-i18n-attr / data-i18n-html and translated by applyTranslations(); text built in
 * JavaScript calls t(). Switching language fires a "languagechange" event on document.
 */

export const LANGUAGES = ['en', 'pt'];
export const DEFAULT_LANGUAGE = 'en';
const STORAGE_KEY = 'language';

/** The choice made on this page when the browser won't store it (e.g. blocked storage). */
let unsavedLanguage = null;

const plural = (n, one, many) => (n === 1 ? one : many);

/** @type {Object.<string, { en: string|Function, pt: string|Function }>} */
export const STRINGS = {
    // ── Shared chrome ──
    'lang.switch': { en: 'Mudar para português', pt: 'Switch to English' },
    'theme.toLight': { en: 'Switch to light mode', pt: 'Mudar para modo claro' },
    'theme.toDark': { en: 'Switch to dark mode', pt: 'Mudar para modo escuro' },
    'nav.home': { en: '← Home', pt: '← Início' },
    'footer.support': { en: 'Buy me a pastel de nata', pt: 'Compre-me um pastel de nata' },
    'footer.version': { en: 'Version:', pt: 'Versão:' },
    'error.load': { en: 'Error loading the data.', pt: 'Erro ao carregar os dados.' },
    'selectAll': { en: 'Select all', pt: 'Selecionar tudo' },
    'categories': { en: 'Categories', pt: 'Categorias' },

    // ── Quiz screens ──
    'quiz.score': { en: ({ correct, wrong }) => `Correct: ${correct} | Wrong: ${wrong}`, pt: ({ correct, wrong }) => `Corretas: ${correct} | Erros: ${wrong}` },
    'quiz.time': { en: ({ time }) => `Time: ${time}`, pt: ({ time }) => `Tempo: ${time}` },
    'quiz.quickPractice': { en: 'Quick Practice', pt: 'Prática Rápida' },
    'quiz.quickPracticeHint': {
        en: '12 items at your level, those due for review first. The next level opens once you know most of this one.',
        pt: '12 itens ao seu nível, primeiro os que estão para rever. O nível seguinte abre quando dominar a maior parte deste.',
    },
    'quiz.advanced': { en: 'Advanced', pt: 'Avançado' },
    'quiz.start': { en: 'Start quiz', pt: 'Iniciar Quiz' },
    'quiz.submit': { en: 'Submit', pt: 'Enviar' },
    'quiz.enterHint': { en: 'Press Enter to submit', pt: 'Pressione Enter para enviar' },
    'quiz.next': { en: 'Next', pt: 'Próxima' },
    'quiz.correct': { en: 'Correct! ', pt: 'Correto! ' },
    'quiz.wrong': { en: ({ answer }) => `Wrong. The correct answer is "${answer}". `, pt: ({ answer }) => `Errado. A resposta correta é "${answer}". ` },
    'quiz.progress': { en: ({ pct }) => `Progress: ${pct}%`, pt: ({ pct }) => `Progresso: ${pct}%` },
    'quiz.mastered': { en: ({ done, total }) => `Mastered: ${done}/${total}`, pt: ({ done, total }) => `Dominadas: ${done}/${total}` },
    'quiz.dueToday': {
        en: ({ n }) => `${n} ${plural(n, 'item', 'items')} due for review today`,
        pt: ({ n }) => `${n} ${plural(n, 'item', 'itens')} para rever hoje`,
    },
    'quiz.selectCategory': { en: 'Please choose at least one category.', pt: 'Por favor, selecione pelo menos uma categoria.' },
    'quiz.example': { en: ({ text }) => `e.g. ${text}`, pt: ({ text }) => `Ex: ${text}` },

    // ── Results ──
    'result.topMistakes': { en: 'Most common mistakes', pt: 'Erros mais comuns' },
    'result.retry': { en: 'Practise mistakes', pt: 'Praticar erros' },
    'result.retryCount': { en: ({ n }) => `Practise mistakes (${n})`, pt: ({ n }) => `Praticar erros (${n})` },
    'result.restart': { en: 'Restart quiz', pt: 'Reiniciar Quiz' },
    'result.noMistakes': { en: 'Well done! You made no mistakes.', pt: 'Parabéns! Não cometeu nenhum erro.' },
    'result.score': { en: ({ correct, wrong }) => `Correct: ${correct} | Wrong: ${wrong}`, pt: ({ correct, wrong }) => `Corretas: ${correct} | Erros: ${wrong}` },
    'result.accuracyLabel': { en: 'ACCURACY', pt: 'PRECISÃO' },
    'result.accuracy': { en: ({ pct }) => `Accuracy: ${pct}%`, pt: ({ pct }) => `Precisão: ${pct}%` },
    'result.newBest': { en: ({ n }) => `New record! Best score: ${n} correct`, pt: ({ n }) => `Novo recorde! Melhor resultado: ${n} corretas` },
    'result.best': { en: ({ n }) => `Best score: ${n} correct`, pt: ({ n }) => `Melhor resultado: ${n} corretas` },
    'result.mistakeCount': {
        en: ({ n }) => `${n} ${plural(n, 'mistake', 'mistakes')}`,
        pt: ({ n }) => `${n} ${plural(n, 'erro', 'erros')}`,
    },
    'result.milestone': { en: ({ n }) => `Well done! ${n} items mastered! 🎉`, pt: ({ n }) => `Parabéns! ${n} itens dominados! 🎉` },

    // ── Home ──
    'home.greeting': { en: 'Hello! 👋', pt: 'Olá! 👋' },
    'home.subtitle': { en: 'Keep practising today!', pt: 'Continua a praticar hoje!' },
    'home.statMastered': { en: 'Mastered', pt: 'Dominadas' },
    'home.statStreak': { en: 'Day streak', pt: 'Dias seguidos' },
    'home.modules': { en: 'Modules', pt: 'Módulos' },
    'home.srsLink': { en: 'Manage spaced repetition →', pt: 'Gerir repetição espaçada →' },
    'home.practise': { en: 'Practise', pt: 'Praticar' },
    'home.due': { en: ({ n }) => `${n} due for review`, pt: ({ n }) => `${n} por rever` },
    'home.nothingDue': { en: 'Nothing to review', pt: 'Nada por rever' },
    'home.seen': { en: ({ pct }) => `${pct}% seen`, pt: ({ pct }) => `${pct}% visto` },

    // ── Module names ──
    'module.verbs': { en: 'Verb Conjugation', pt: 'Conjugação de Verbos' },
    'module.vocab': { en: 'Vocabulary', pt: 'Vocabulário' },
    'module.gender': { en: 'Gender & Plural', pt: 'Género e Plural' },
    'module.serEstarFicar': { en: 'Ser / Estar / Ficar', pt: 'Ser / Estar / Ficar' },
    'module.contractions': { en: 'Contractions', pt: 'Contrações' },
    'module.subjunctive': { en: 'Subjunctive', pt: 'Conjuntivo' },
    'module.indirectSpeech': { en: 'Indirect Speech', pt: 'Discurso Indireto' },

    // ── Verbs ──
    'verbs.title': { en: 'Portuguese Verb Quiz', pt: 'Quiz dos Verbos Portugueses' },
    'verbs.tensesHeading': { en: 'Choose the tenses to practise', pt: 'Selecione os tempos para praticar' },
    'verbs.tensesHint': {
        en: 'Choose at least one tense. The more tenses, the longer the quiz.',
        pt: 'Selecione pelo menos um tempo verbal. Quanto mais tempos, mais longa a quiz.',
    },
    'verbs.selectTense': { en: 'Please choose at least one tense.', pt: 'Por favor, selecione pelo menos um tempo verbal.' },
    'verbs.participles': { en: 'past participles', pt: 'particípios passados' },
    'verbs.difficulty': { en: 'Difficulty level', pt: 'Nível de dificuldade' },
    'verbs.difficulty.all': { en: 'All', pt: 'Todos' },
    'verbs.difficulty.beginner': { en: 'Beginner', pt: 'Iniciante' },
    'verbs.difficulty.intermediate': { en: 'Intermediate', pt: 'Intermédio' },
    'verbs.difficulty.advanced': { en: 'Advanced', pt: 'Avançado' },
    'verbs.interleaved': { en: 'Interleaved mode (better for retention)', pt: 'Modo intercalado (melhor para retenção)' },
    'verbs.question': {
        en: 'Conjugate the verb {verb} in the {tense} tense for {person}:',
        pt: 'Conjugue o verbo {verb} no tempo {tense} para {person}:',
    },
    'verbs.participleQuestion': {
        en: 'What is the correct past participle of {verb} with the auxiliary verb {aux}?',
        pt: 'Qual é o particípio correto para o verbo {verb} usado com o verbo auxiliar {aux}?',
    },
    'verbs.done': { en: 'Well done! You conjugated every verb correctly.', pt: 'Parabéns! Conjugou corretamente todos os verbos.' },
    'verbs.mistake.verb': { en: 'Verb', pt: 'Verbo' },
    'verbs.mistake.tense': { en: 'Tense', pt: 'Tempo' },
    'verbs.mistake.person': { en: 'Person', pt: 'Pessoa' },
    'verbs.mistake.auxiliary': { en: 'Auxiliary', pt: 'Auxiliar' },
    'verbs.mistake.answer': { en: 'Answer', pt: 'Resposta' },
    'verbs.mistake.count': { en: 'Mistakes', pt: 'Erros' },

    // ── Vocabulary ──
    'vocab.title': { en: 'Portuguese Vocabulary Quiz', pt: 'Quiz de Vocabulário Português' },
    'vocab.directionHeading': { en: 'Choose the translation direction', pt: 'Escolha a direção da tradução' },
    'vocab.enToPt': { en: 'English → Portuguese', pt: 'Inglês → Português' },
    'vocab.ptToEn': { en: 'Portuguese → English', pt: 'Português → Inglês' },
    'vocab.categoriesHeading': { en: 'Choose the categories to practise', pt: 'Selecione as categorias para praticar' },
    'vocab.categoriesHint': {
        en: 'Choose at least one category. Start with 1–2 for a short session.',
        pt: 'Selecione pelo menos uma categoria. Pode começar com 1–2 para uma sessão rápida.',
    },
    'vocab.toPortuguese': { en: 'Translate {word} into Portuguese', pt: 'Traduza {word} em Português' },
    'vocab.toEnglish': { en: 'Translate {word} into English', pt: 'Traduza {word} em Inglês' },
    'vocab.done': { en: 'Well done! You translated every word correctly.', pt: 'Parabéns! Traduziu corretamente todas as palavras.' },

    // ── Gender & plural ──
    'gender.title': { en: 'Gender & Plural Quiz', pt: 'Quiz de Género e Plural' },
    'gender.intro': { en: 'Practise the feminine and plural forms of nouns and adjectives.', pt: 'Pratique as formas femininas e plurais de nomes e adjetivos.' },
    'gender.question': { en: 'What is the {form} of {word}?', pt: 'Qual é o {form} de {word}?' },
    'gender.form.feminino': { en: 'feminine', pt: 'feminino' },
    'gender.form.plural': { en: 'plural', pt: 'plural' },
    'gender.done': { en: 'Well done! You completed every word.', pt: 'Parabéns! Completou todas as palavras.' },

    // ── Ser / estar / ficar ──
    'sef.title': { en: 'Ser / Estar / Ficar Quiz', pt: 'Quiz Ser / Estar / Ficar' },
    'sef.intro': {
        en: 'Complete the sentences with the correct form of <em>ser</em>, <em>estar</em> or <em>ficar</em>. Practise telling apart permanent identity, temporary states and fixed locations.',
        pt: 'Complete as frases com a forma correta de <em>ser</em>, <em>estar</em> ou <em>ficar</em>. Pratique a distinção entre identidade permanente, estado temporário e localização fixa.',
    },
    'sef.placeholder': { en: 'type the verb form', pt: 'escreva a forma verbal' },
    'sef.done': { en: 'Well done! You completed every sentence.', pt: 'Parabéns! Completou todas as frases.' },

    // ── Contractions ──
    'contractions.title': { en: 'Contractions Quiz', pt: 'Quiz de Contrações' },
    'contractions.intro': {
        en: 'Practise contracting the prepositions <em>de</em>, <em>em</em>, <em>a</em> and <em>por</em> with articles and demonstratives. Type the contracted form.',
        pt: 'Pratique as contrações das preposições <em>de</em>, <em>em</em>, <em>a</em> e <em>por</em> com artigos e demonstrativos. Escreva a forma contraída.',
    },
    'contractions.placeholder': { en: 'type the contraction', pt: 'escreva a contração' },
    'contractions.question': { en: 'What is the contraction of {first} + {second}?', pt: 'Qual é a contração de {first} + {second}?' },
    'contractions.done': { en: 'Well done! You completed every contraction.', pt: 'Parabéns! Completou todas as contrações.' },

    // ── Subjunctive ──
    'subjunctive.title': { en: 'Subjunctive Quiz', pt: 'Quiz do Conjuntivo' },
    'subjunctive.intro': {
        en: 'Conjugate the verb shown in the correct subjunctive form. Spot the trigger that calls for the subjunctive and type the right verb form.',
        pt: 'Conjugue o verbo indicado no conjuntivo correto. Identifique o gatilho que exige o conjuntivo e escreva a forma verbal adequada.',
    },
    'subjunctive.trigger': { en: ({ trigger }) => `Trigger: ${trigger}`, pt: ({ trigger }) => `Gatilho: ${trigger}` },
    'subjunctive.placeholder': { en: 'type the verb form', pt: 'escreva a forma verbal' },
    'subjunctive.done': { en: 'Well done! You completed every exercise.', pt: 'Parabéns! Completou todos os exercícios.' },

    // ── Indirect speech ──
    'indirect.title': { en: 'Indirect Speech Quiz', pt: 'Quiz de Discurso Indireto' },
    'indirect.intro': {
        en: 'Turn direct speech into indirect speech. Type the verb form that replaces the highlighted verb in the original sentence.',
        pt: 'Transforme o discurso direto em discurso indireto. Escreva a forma verbal que substitui o verbo em destaque na frase original.',
    },
    'indirect.placeholder': { en: 'verb form in indirect speech', pt: 'forma verbal em discurso indireto' },
    'indirect.question': { en: 'In indirect speech, what does {verb} become?', pt: 'Em discurso indireto, como fica {verb}?' },
    'indirect.done': { en: 'Well done! You completed every exercise.', pt: 'Parabéns! Completou todos os exercícios.' },

    // ── SRS manager ──
    'srs.title': { en: 'Manage Spaced Repetition', pt: 'Gerir Repetição Espaçada' },
    'srs.heading': { en: 'Spaced Repetition', pt: 'Repetição Espaçada' },
    'srs.intro': {
        en: 'See and reset the practice records for each item. Resetting an item deletes its SRS history and starts it from scratch.',
        pt: 'Veja e redefina os registos de prática de cada item. Repor um item remove o histórico SRS e recomeça do zero.',
    },
    'srs.filter': { en: 'Filter items…', pt: 'Filtrar itens…' },
    'srs.resetAll': { en: 'Reset ALL SRS records', pt: 'Repor TODOS os registos SRS' },
    'srs.resetAllConfirm': {
        en: 'Reset ALL SRS records in every module? This cannot be undone.',
        pt: 'Repor TODOS os registos SRS de todos os módulos? Esta ação não pode ser desfeita.',
    },
    'srs.never': { en: 'never', pt: 'nunca' },
    'srs.recorded': {
        en: ({ n }) => `${n} ${plural(n, 'item', 'items')} recorded`,
        pt: ({ n }) => `${n} ${plural(n, 'item registado', 'itens registados')}`,
    },
    'srs.dueToday': { en: ({ n }) => ` · ${n} due today`, pt: ({ n }) => ` · ${n} para rever hoje` },
    'srs.empty': { en: 'No records yet.', pt: 'Sem registos ainda.' },
    'srs.noMatch': { en: 'No item matches the filter.', pt: 'Nenhum item corresponde ao filtro.' },
    'srs.due': { en: 'due', pt: 'para rever' },
    'srs.next': { en: ({ date }) => `next: ${date}`, pt: ({ date }) => `próxima: ${date}` },
    'srs.reset': { en: 'Reset', pt: 'Repor' },
    'srs.resetModule': { en: ({ module }) => `Reset all (${module})`, pt: ({ module }) => `Repor todos (${module})` },
    'srs.resetModuleConfirm': {
        en: ({ n, module }) => `Reset all ${n} records of "${module}"?`,
        pt: ({ n, module }) => `Repor todos os ${n} registos de "${module}"?`,
    },
};

/** @returns {'en'|'pt'} */
export function getLanguage() {
    if (unsavedLanguage) return unsavedLanguage;
    try {
        return localStorage.getItem(STORAGE_KEY) === 'pt' ? 'pt' : DEFAULT_LANGUAGE;
    } catch {
        return DEFAULT_LANGUAGE;
    }
}

/** @param {'en'|'pt'} language */
export function setLanguage(language) {
    if (!LANGUAGES.includes(language)) throw new Error(`Unknown language "${language}"`);
    try {
        localStorage.setItem(STORAGE_KEY, language);
        unsavedLanguage = null;
    } catch {
        unsavedLanguage = language; // still switches, for this page
    }
    document.documentElement.lang = language === 'pt' ? 'pt-PT' : 'en';
    applyTranslations();
    document.dispatchEvent(new window.CustomEvent('languagechange', { detail: { language } }));
}

/**
 * The string for `key` in the current language. Strings with `{name}` slots are filled from
 * `params` (use appendTemplate to slot in elements instead).
 * @param {string} key
 * @param {Object.<string, unknown>} [params]
 * @returns {string}
 */
export function t(key, params = {}) {
    const entry = STRINGS[key];
    if (!entry) throw new Error(`Missing translation "${key}"`);
    const value = entry[getLanguage()];
    if (typeof value === 'function') return value(params);
    return value.replace(/\{(\w+)\}/g, (match, name) => (name in params ? String(params[name]) : match));
}

/**
 * Appends a translated sentence to `el`, putting the given nodes into its `{name}` slots, so the
 * highlighted parts of a question stay elements in either language.
 * @param {HTMLElement} el
 * @param {string} key
 * @param {Object.<string, Node|string>} slots
 */
export function appendTemplate(el, key, slots) {
    const template = STRINGS[key]?.[getLanguage()];
    if (typeof template !== 'string') throw new Error(`"${key}" is not a template`);
    for (const part of template.split(/(\{\w+\})/)) {
        const slot = part.match(/^\{(\w+)\}$/);
        if (slot && slot[1] in slots) el.append(slots[slot[1]]);
        else if (part) el.append(part);
    }
}

/**
 * A content field in the current language: `item.hint_en` in English when present, else
 * `item.hint` (the content files keep the Portuguese in the plain field).
 * @param {Object.<string, any>} item
 * @param {string} field
 * @returns {any}
 */
export function localized(item, field) {
    return getLanguage() === 'en' && item?.[`${field}_en`] ? item[`${field}_en`] : item?.[field];
}

/**
 * Translates static markup: `data-i18n="key"` sets the text, `data-i18n-html="key"` sets trusted
 * markup from STRINGS, and `data-i18n-attr="attr:key;attr:key"` sets attributes.
 * @param {ParentNode} [root]
 */
export function applyTranslations(root = document) {
    for (const el of root.querySelectorAll('[data-i18n]')) el.textContent = t(el.dataset.i18n);
    for (const el of root.querySelectorAll('[data-i18n-html]')) el.innerHTML = t(el.dataset.i18nHtml);
    for (const el of root.querySelectorAll('[data-i18n-attr]')) {
        for (const pair of el.dataset.i18nAttr.split(';')) {
            const [attr, key] = pair.split(':');
            el.setAttribute(attr, t(key));
        }
    }
}

/**
 * Translates the page and adds the EN/PT switch next to the theme toggle. Call once per page.
 */
export function initLanguage() {
    document.documentElement.lang = getLanguage() === 'pt' ? 'pt-PT' : 'en';
    applyTranslations();
    if (document.getElementById('lang-toggle')) return;
    const btn = document.createElement('button');
    btn.id = 'lang-toggle';
    btn.type = 'button';
    const render = () => {
        btn.textContent = getLanguage() === 'en' ? 'EN' : 'PT';
        btn.setAttribute('aria-label', t('lang.switch'));
        btn.title = t('lang.switch');
        btn.lang = getLanguage() === 'en' ? 'pt-PT' : 'en'; // the label is in the other language
    };
    render();
    btn.addEventListener('click', () => {
        setLanguage(getLanguage() === 'en' ? 'pt' : 'en');
        render();
    });
    const theme = document.getElementById('theme-toggle');
    if (theme) theme.after(btn);
    else document.body.prepend(btn);
}
