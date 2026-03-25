const persons = ["eu", "tu", "ele/ela/você", "nós", "eles/elas/vocês"];
const PARTICIPLES_KEY = "participios_passados";
const tenseOptions = [
    { key: "presente", label: "presente" },
    { key: "pretérito", label: "pretérito" },
    { key: "imperfeito", label: "imperfeito" },
    { key: "condicional", label: "condicional" },
    { key: "pretérito mais-que-perfeito", label: "pretérito mais-que-perfeito" },
    { key: "perfeito_composto", label: "perfeito composto" },
    { key: "futuro", label: "futuro" },
    { key: "imperativo", label: "imperativo" },
    { key: "conjuntivo", label: "conjuntivo" },
    { key: "infinitivo pessoal", label: "infinitivo pessoal" },
    { key: PARTICIPLES_KEY, label: "particípios passados" }
];
const REQUIRED_CORRECT = 1;

let verbs = {};
let selectedTenses = [];
let conjugationCounters = {};
let mistakeCounters = {};
let conjugationsToPractice = [];
let promptLookup = {};
let totalScore = 0;
let currentPrompt = null;
let totalConjugationsNeeded = 0;
let conjugationsCompleted = 0;
let timerInterval = null;
let elapsedTime = 0;
let isFeedbackDisplayed = false;

// DOM elements
let scoreDisplay;
let timerDisplay;
let setupSection;
let quizSection;
let resultSection;
let questionElement;
let answerInput;
let feedbackElement;
let answerContainer;
let nextQuestionButton;
let progressBar;
let progressLabel;
let totalScoreElement;
let topMistakesList;

function buildPromptKey(verb, tense, identifier) {
    return `${verb}::${tense}::${identifier}`;
}

document.addEventListener("DOMContentLoaded", async () => {
    cacheDomElements();
    initializeUiState();

    try {
        const response = await fetch('verbs.json');
        if (!response.ok) throw new Error(`Falha ao carregar dados dos verbos (${response.status}).`);
        verbs = await response.json();
        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados dos verbos. Atualize a página e tente novamente.");
    }
});

function cacheDomElements() {
    scoreDisplay = document.getElementById("score-display");
    timerDisplay = document.getElementById("timer-display");
    setupSection = document.getElementById("setup");
    quizSection = document.getElementById("quiz");
    resultSection = document.getElementById("result");
    questionElement = document.getElementById("question");
    answerInput = document.getElementById("answer");
    feedbackElement = document.getElementById("feedback");
    answerContainer = document.getElementById("answer-container");
    nextQuestionButton = document.getElementById("next-question");
    progressBar = document.getElementById("progress-bar");
    progressLabel = document.getElementById("progress-percentage");
    totalScoreElement = document.getElementById("total-score");
    topMistakesList = document.getElementById("top-mistakes");
}

function initializeUiState() {
    updateScoreDisplay();
    updateTimerDisplay();
    updateProgressBar();
}

function initializeQuiz() {
    renderTenseOptions();

    document.getElementById("start-quiz").addEventListener("click", startQuiz);
    document.getElementById("submit-answer").addEventListener("click", submitAnswer);
    nextQuestionButton.addEventListener("click", nextQuestion);
    document.getElementById("restart").addEventListener("click", () => window.location.reload());

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Enter") return;
        if (!quizSection || quizSection.hidden) return;

        if (!isFeedbackDisplayed) {
            submitAnswer();
        } else if (!nextQuestionButton.hidden) {
            nextQuestion();
        }
    });
}

function renderTenseOptions() {
    const tensesContainer = document.getElementById("tenses");
    tensesContainer.innerHTML = "";

    tenseOptions.forEach((tense) => {
        const label = document.createElement("label");
        label.className = "selection-chip";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = tense.key;

        const span = document.createElement("span");
        span.className = "chip-label";
        span.textContent = tense.label;

        checkbox.addEventListener("change", () => {
            label.classList.toggle("active", checkbox.checked);
        });

        label.appendChild(checkbox);
        label.appendChild(span);
        tensesContainer.appendChild(label);
    });
}

function startQuiz() {
    selectedTenses = Array.from(document.querySelectorAll("#tenses input:checked"))
        .map((input) => input.value);

    if (selectedTenses.length === 0) {
        alert("Selecione pelo menos um tempo verbal para iniciar.");
        return;
    }

    resetQuizState();
    populatePrompts();

    if (conjugationsToPractice.length === 0) {
        alert("Não há conjugação disponível para as escolhas feitas. Tente selecionar outros tempos.");
        return;
    }

    setupSection.hidden = true;
    quizSection.hidden = false;
    resultSection.hidden = true;

    updateProgressBar();
    nextQuestion();
}

function resetQuizState() {
    conjugationCounters = {};
    mistakeCounters = {};
    conjugationsToPractice = [];
    promptLookup = {};
    totalConjugationsNeeded = 0;
    conjugationsCompleted = 0;
    totalScore = 0;
    currentPrompt = null;
    elapsedTime = 0;
    stopTimer();

    updateScoreDisplay();
    updateTimerDisplay();
    updateProgressBar();
}

function populatePrompts() {
    const verbNames = Object.keys(verbs);
    verbNames.forEach((verb) => {
        selectedTenses.forEach((tenseKey) => {
            registerPromptsForVerb(verb, tenseKey);
        });
    });
}

function registerPromptsForVerb(verb, tenseKey) {
    if (tenseKey === PARTICIPLES_KEY) {
        const participles = verbs[verb]?.[PARTICIPLES_KEY];
        if (!participles) return;

        ["ter", "ser", "estar"].forEach((aux) => {
            const answer = participles[aux];
            if (!answer) return;

            const key = buildPromptKey(verb, tenseKey, aux);
            const prompt = {
                key,
                verb,
                tense: tenseKey,
                aux,
                displayTense: getTenseLabel(tenseKey)
            };
            registerPrompt(prompt);
        });
        return;
    }

    const conjugations = verbs[verb]?.[tenseKey];
    if (!Array.isArray(conjugations)) return;

    conjugations.forEach((form, index) => {
        if (!form) return;
        const key = buildPromptKey(verb, tenseKey, index);
        const prompt = {
            key,
            verb,
            tense: tenseKey,
            personIndex: index,
            displayTense: getTenseLabel(tenseKey)
        };
        registerPrompt(prompt);
    });
}

function registerPrompt(prompt) {
    conjugationsToPractice.push(prompt);
    promptLookup[prompt.key] = prompt;
    conjugationCounters[prompt.key] = 0;
    mistakeCounters[prompt.key] = 0;
    totalConjugationsNeeded += REQUIRED_CORRECT;
}

function getTenseLabel(key) {
    const tense = tenseOptions.find((item) => item.key === key);
    return tense ? tense.label : key;
}

function nextQuestion() {
    if (conjugationsToPractice.length === 0) {
        endQuiz();
        return;
    }

    isFeedbackDisplayed = false;
    const randomIndex = Math.floor(Math.random() * conjugationsToPractice.length);
    currentPrompt = conjugationsToPractice[randomIndex];

    renderQuestion(currentPrompt);

    answerInput.value = "";
    answerInput.disabled = false;
    answerContainer.hidden = false;
    feedbackElement.textContent = "";
    feedbackElement.className = "";
    nextQuestionButton.hidden = true;

    answerInput.focus();
    startTimer();
}

function renderQuestion(prompt) {
    if (!prompt) return;

    if (prompt.tense === PARTICIPLES_KEY) {
        questionElement.innerHTML = `Qual é o particípio do verbo <strong class="${getVerbClass(prompt.verb)}">${prompt.verb}</strong> com o auxiliar <strong class="${prompt.aux}">${prompt.aux}</strong>?`;
        return;
    }

    const person = persons[prompt.personIndex];
    questionElement.innerHTML = `Conjugue <strong class="${getVerbClass(prompt.verb)}">${prompt.verb}</strong> no tempo <strong class="tense-highlight">${prompt.displayTense}</strong> para <strong class="person-color-${prompt.personIndex}">${person}</strong>.`;
}

function submitAnswer() {
    if (!currentPrompt || isFeedbackDisplayed) {
        return;
    }

    const rawAnswer = answerInput.value.trim();
    if (!rawAnswer) {
        feedbackElement.textContent = "Introduza uma resposta antes de continuar.";
        feedbackElement.className = "incorrect";
        return;
    }

    const userAnswer = rawAnswer.toLowerCase().normalize('NFC');
    const correctAnswer = getCorrectAnswer(currentPrompt).toLowerCase().normalize('NFC');

    const key = currentPrompt.key;
    const previousCount = conjugationCounters[key];

    if (userAnswer === correctAnswer) {
        feedbackElement.textContent = "Resposta correta!";
        feedbackElement.className = "correct";
        conjugationCounters[key] += 1;
        totalScore += 1;

        if (conjugationCounters[key] >= REQUIRED_CORRECT) {
            const index = conjugationsToPractice.findIndex((item) => item.key === key);
            if (index > -1) {
                conjugationsToPractice.splice(index, 1);
            }
        }
    } else {
        feedbackElement.textContent = `Não foi desta. A forma correta é "${getCorrectAnswer(currentPrompt)}".`;
        feedbackElement.className = "incorrect";
        if (conjugationCounters[key] > 0) {
            conjugationCounters[key] -= 1;
        }
        totalScore -= 1;
        mistakeCounters[key] += 1;
    }

    updateScoreDisplay();

    const countChange = conjugationCounters[key] - previousCount;
    conjugationsCompleted = clamp(conjugationsCompleted + countChange, 0, totalConjugationsNeeded);
    updateProgressBar();

    answerContainer.hidden = true;
    nextQuestionButton.hidden = false;
    isFeedbackDisplayed = true;
    stopTimer();
}

function getCorrectAnswer(prompt) {
    if (prompt.tense === PARTICIPLES_KEY) {
        return verbs[prompt.verb]?.[PARTICIPLES_KEY]?.[prompt.aux] || "";
    }

    const conjugations = verbs[prompt.verb]?.[prompt.tense];
    if (!Array.isArray(conjugations)) {
        return "";
    }

    return conjugations[prompt.personIndex] || "";
}

function endQuiz() {
    stopTimer();
    quizSection.hidden = true;
    resultSection.hidden = false;

    totalScoreElement.textContent = `Sua pontuação total foi: ${totalScore}`;

    const mistakes = Object.entries(mistakeCounters)
        .filter(([, count]) => count > 0)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);

    topMistakesList.innerHTML = "";

    if (mistakes.length === 0) {
        const li = document.createElement("li");
        li.textContent = "Parabéns! Não cometeu nenhum erro.";
        topMistakesList.appendChild(li);
        return;
    }

    mistakes.forEach(([key, count], index) => {
        const prompt = promptLookup[key];
        if (!prompt) return;

        const li = document.createElement("li");
        const answer = getCorrectAnswer(prompt);

        if (prompt.tense === PARTICIPLES_KEY) {
            li.innerHTML = `${index + 1}. Verbo <strong>${prompt.verb}</strong> com auxiliar <strong>${prompt.aux}</strong> — correto: <strong>${answer}</strong> · erros: ${count}`;
        } else {
            const person = persons[prompt.personIndex];
            li.innerHTML = `${index + 1}. <strong>${prompt.verb}</strong> (${prompt.displayTense}, ${person}) — correto: <strong>${answer}</strong> · erros: ${count}`;
        }

        topMistakesList.appendChild(li);
    });
}

function updateScoreDisplay() {
    if (scoreDisplay) {
        scoreDisplay.textContent = `Pontuação: ${totalScore}`;
    }
}

function startTimer() {
    stopTimer();
    timerInterval = setInterval(() => {
        elapsedTime += 1;
        updateTimerDisplay();
    }, 1000);
}

function stopTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
}

function updateTimerDisplay() {
    if (!timerDisplay) return;

    const minutes = Math.floor(elapsedTime / 60);
    const seconds = elapsedTime % 60;
    timerDisplay.textContent = `Tempo: ${padZero(minutes)}:${padZero(seconds)}`;
}

function updateProgressBar() {
    if (!progressBar || !progressLabel) return;

    const progress = totalConjugationsNeeded === 0 ? 0 : (conjugationsCompleted / totalConjugationsNeeded) * 100;
    const clamped = clamp(progress, 0, 100);
    progressBar.style.width = `${clamped}%`;
    progressLabel.textContent = `Progresso: ${clamped.toFixed(2)}%`;
}

function getVerbClass(verb) {
    return verbs[verb]?.regular ? "regular-verb" : "irregular-verb";
}

function padZero(num) {
    return num.toString().padStart(2, "0");
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}
