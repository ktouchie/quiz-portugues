import { loadVersion, initTheme, startTimer, stopTimer, updateTimerDisplay, updateBestScore, addSelectAll } from './common.js';

const persons = ["eu", "tu", "ele/ela/você", "nós", "eles/elas/vocês"];

// Display labels for tenses (human-readable)
const tenseLabels = {
    "presente":                    "presente",
    "pretérito":                   "pretérito",
    "imperfeito":                  "imperfeito",
    "condicional":                 "condicional",
    "pretérito mais-que-perfeito": "pretérito mais-que-perfeito",
    "perfeito_composto":           "perfeito composto",
    "futuro":                      "futuro",
    "imperativo":                  "imperativo",
    "conjuntivo":                  "conjuntivo",
    "infinitivo pessoal":          "infinitivo pessoal",
};

const tenses = Object.keys(tenseLabels);

let verbs = {};
let selectedTenses = [];
let requiredCorrect = 1;
let conjugationCounters = {};
let mistakeCounters = {};
let conjugationsToPractice = [];
let totalScore = 0;
let currentKey = null;
let totalConjugationsNeeded = 0;
let conjugationsCompleted = 0;
let isFeedbackDisplayed = false;

// Timer state
const timerState = { timerInterval: null, elapsedTime: 0, timerDisplay: null };

document.addEventListener("DOMContentLoaded", async () => {
    initTheme();
    loadVersion();

    try {
        const response = await fetch('verbs.json');
        if (!response.ok) throw new Error("Failed to load verbs data.");
        verbs = await response.json();
        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados dos verbos.");
    }
});

function initializeQuiz() {
    const tensesDiv = document.getElementById("tenses");
    tenses.forEach(tense => {
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = tense;
        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(" " + tenseLabels[tense]));
        tensesDiv.appendChild(label);
    });

    addSelectAll("tenses");

    // Particípios passados option
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.value = "participios_passados";
    const label = document.createElement("label");
    label.appendChild(checkbox);
    label.appendChild(document.createTextNode(" particípios passados"));
    tensesDiv.appendChild(label);

    document.getElementById("start-quiz").addEventListener("click", startQuiz);
    document.getElementById("submit-answer").addEventListener("click", submitAnswer);
    document.getElementById("next-question").addEventListener("click", nextQuestion);
    document.getElementById("restart").addEventListener("click", () => location.reload());

    timerState.timerDisplay = document.getElementById("timer-display");
    updateTimerDisplay(timerState.timerDisplay, 0);
    updateScoreDisplay();

    document.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            if (!isFeedbackDisplayed) {
                document.getElementById("submit-answer").click();
            } else {
                document.getElementById("next-question").click();
            }
        }
    });
}

function startQuiz() {
    selectedTenses = Array.from(document.querySelectorAll("#tenses input:checked")).map(input => input.value);
    if (selectedTenses.length === 0) {
        alert("Por favor, selecione pelo menos um tempo verbal.");
        return;
    }

    totalConjugationsNeeded = 0;
    conjugationsCompleted = 0;

    for (let verb in verbs) {
        for (let tense of selectedTenses) {
            if (tense === "participios_passados" && verbs[verb].participios_passados) {
                ["ter", "ser", "estar"].forEach(aux => {
                    const key = `${verb}-participios_passados-${aux}`;
                    conjugationCounters[key] = 0;
                    mistakeCounters[key] = 0;
                    conjugationsToPractice.push(key);
                    totalConjugationsNeeded += requiredCorrect;
                });
            } else if (verbs[verb][tense]) {
                for (let personIdx = 0; personIdx < persons.length; personIdx++) {
                    if (verbs[verb][tense][personIdx] && verbs[verb][tense][personIdx].length > 0) {
                        const key = `${verb}-${tense}-${personIdx}`;
                        conjugationCounters[key] = 0;
                        mistakeCounters[key] = 0;
                        conjugationsToPractice.push(key);
                        totalConjugationsNeeded += requiredCorrect;
                    }
                }
            }
        }
    }

    document.getElementById("setup").classList.add("hidden");
    document.getElementById("quiz").classList.remove("hidden");
    updateProgressBar();
    nextQuestion();
    startTimer(timerState);
}

function nextQuestion() {
    if (conjugationsToPractice.length === 0) {
        endQuiz();
        return;
    }
    isFeedbackDisplayed = false;

    const randomIndex = Math.floor(Math.random() * conjugationsToPractice.length);
    currentKey = conjugationsToPractice[randomIndex];

    if (currentKey.includes("participios_passados")) {
        const [verb, , aux] = currentKey.split("-");
        document.getElementById("question").innerHTML =
            `Qual é o particípio correto para o verbo ` +
            `<strong class="irregular-verb">${verb}</strong> usado com o verbo auxiliar ` +
            `<strong class="${aux}">${aux}</strong>?`;
    } else {
        const [verb, tense, personIdx] = currentKey.split("-");
        const person = persons[personIdx];
        const verbClass = verbs[verb].regular ? 'regular-verb' : 'irregular-verb';
        const tenseClass = `tense-color-${tenses.indexOf(tense)}`;
        const personClass = `person-color-${personIdx}`;
        document.getElementById("question").innerHTML =
            `Conjugue o verbo <strong class="${verbClass}">${verb}</strong> no tempo ` +
            `<strong class="${tenseClass}">${tenseLabels[tense] || tense}</strong> para ` +
            `<strong class="${personClass}">${person}</strong>:`;
    }

    document.getElementById("answer").value = "";
    document.getElementById("feedback").innerHTML = "";
    document.getElementById("answer-container").classList.remove("hidden");
    document.getElementById("feedback").classList.add("hidden");
    document.getElementById("next-question").classList.add("hidden");
    document.getElementById("answer").focus();

    startTimer(timerState);
}

function submitAnswer() {
    const userAnswer = document.getElementById("answer").value.trim().toLowerCase();
    let correctAnswer;

    if (currentKey.includes("participios_passados")) {
        const [verb, , aux] = currentKey.split("-");
        correctAnswer = verbs[verb].participios_passados[aux];
    } else {
        const [verb, tense, personIdx] = currentKey.split("-");
        correctAnswer = verbs[verb][tense][personIdx];
    }

    const prevCount = conjugationCounters[currentKey];

    if (userAnswer.normalize('NFC') === correctAnswer.toLowerCase().normalize('NFC')) {
        document.getElementById("feedback").innerHTML = "Correto!";
        document.getElementById("feedback").className = "correct";
        conjugationCounters[currentKey]++;
        totalScore++;

        if (conjugationCounters[currentKey] >= requiredCorrect) {
            const index = conjugationsToPractice.indexOf(currentKey);
            if (index > -1) conjugationsToPractice.splice(index, 1);
        }
    } else {
        document.getElementById("feedback").innerHTML = `Errado. A resposta correta é '${correctAnswer}'.`;
        document.getElementById("feedback").className = "incorrect";
        if (conjugationCounters[currentKey] > 0) conjugationCounters[currentKey]--;
        totalScore--;
        mistakeCounters[currentKey]++;
    }

    updateScoreDisplay();

    const countChange = conjugationCounters[currentKey] - prevCount;
    conjugationsCompleted = Math.max(0, Math.min(totalConjugationsNeeded, conjugationsCompleted + countChange));
    updateProgressBar();

    document.getElementById("answer-container").classList.add("hidden");
    document.getElementById("feedback").classList.remove("hidden");
    document.getElementById("next-question").classList.remove("hidden");
    isFeedbackDisplayed = true;

    stopTimer(timerState);
}

function updateProgressBar() {
    const pct = Math.max(0, Math.min(100, (conjugationsCompleted / totalConjugationsNeeded) * 100));
    document.getElementById("progress-bar").style.width = pct + "%";
    document.getElementById("progress-percentage").innerText = `Progresso: ${pct.toFixed(2)}%`;
}

function endQuiz() {
    stopTimer(timerState);
    document.getElementById("quiz").classList.add("hidden");
    document.getElementById("result").classList.remove("hidden");
    document.getElementById("total-score").innerText = `Sua pontuação total é: ${totalScore}`;
    updateBestScore('bestScore_verbs', totalScore);

    const sortedMistakes = Object.entries(mistakeCounters).sort((a, b) => b[1] - a[1]);
    const topMistakes = sortedMistakes.filter(item => item[1] > 0).slice(0, 10);
    const topMistakesList = document.getElementById("top-mistakes");
    topMistakesList.innerHTML = "";

    if (topMistakes.length > 0) {
        topMistakes.forEach(([key, mistakes], index) => {
            const li = document.createElement("li");
            if (key.includes("participios_passados")) {
                const [verb, , aux] = key.split("-");
                const correctAnswer = verbs[verb].participios_passados[aux];
                li.innerHTML = `${index + 1}. Verbo: <strong>${verb}</strong>, Tempo: <strong>particípios passados</strong>, Auxiliar: <strong>${aux}</strong>, Resposta correta: '<strong>${correctAnswer}</strong>', Erros: ${mistakes}`;
            } else {
                const [verb, tense, personIdx] = key.split("-");
                const person = persons[personIdx];
                const correctAnswer = verbs[verb][tense][personIdx];
                li.innerHTML = `${index + 1}. Verbo: <strong>${verb}</strong>, Tempo: <strong>${tenseLabels[tense] || tense}</strong>, Pessoa: <strong>${person}</strong>, Resposta correta: '<strong>${correctAnswer}</strong>', Erros: ${mistakes}`;
            }
            topMistakesList.appendChild(li);
        });
    } else {
        topMistakesList.innerHTML = "<li>Parabéns! Você não cometeu nenhum erro.</li>";
    }
}

function updateScoreDisplay() {
    document.getElementById("score-display").innerText = `Pontuação: ${totalScore}`;
}
