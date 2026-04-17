import { loadVersion, initTheme, startTimer, stopTimer, updateTimerDisplay, updateBestScore } from './common.js';

// Delimiter unlikely to appear in any word or category name
const SEP = "|||";

let vocabulary = {};
let selectedCategories = [];
let ENtoPT = true;
let requiredCorrect = 1;
let vocabCounters = {};
let mistakeCounters = {};
let vocabToPractice = [];
let totalScore = 0;
let currentKey = null;
let totalWordsNeeded = 0;
let wordsCompleted = 0;
let isFeedbackDisplayed = false;

// Timer state
const timerState = { timerInterval: null, elapsedTime: 0, timerDisplay: null };

document.addEventListener("DOMContentLoaded", async () => {
    initTheme();
    loadVersion();

    try {
        const response = await fetch('vocabulary.json');
        if (!response.ok) throw new Error("Failed to load vocabulary data.");
        vocabulary = await response.json();
        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados do vocabulário.");
    }
});

function initializeQuiz() {
    const categoryDiv = document.getElementById("categories");
    Object.keys(vocabulary).forEach(category => {
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = category;
        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(" " + category));
        categoryDiv.appendChild(label);
    });

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
    selectedCategories = Array.from(document.querySelectorAll("#categories input:checked")).map(input => input.value);
    if (selectedCategories.length === 0) {
        alert("Por favor, selecione pelo menos uma categoria.");
        return;
    }

    ENtoPT = document.querySelector("input[name='translation-direction']:checked").value === "true";

    totalWordsNeeded = 0;
    wordsCompleted = 0;

    for (const category of selectedCategories) {
        for (const pt_word in vocabulary[category]) {
            if (vocabulary[category].hasOwnProperty(pt_word)) {
                const en_word = vocabulary[category][pt_word];
                const key = `${category}${SEP}${pt_word}${SEP}${en_word}`;
                vocabCounters[key] = 0;
                mistakeCounters[key] = 0;
                vocabToPractice.push(key);
                totalWordsNeeded += requiredCorrect;
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
    if (vocabToPractice.length === 0) {
        endQuiz();
        return;
    }
    isFeedbackDisplayed = false;

    const randomIndex = Math.floor(Math.random() * vocabToPractice.length);
    currentKey = vocabToPractice[randomIndex];
    const [, pt_word, en_word] = currentKey.split(SEP);

    if (ENtoPT) {
        document.getElementById("question").innerHTML =
            `Traduza <strong class="person-color-3">${en_word}</strong> em Português`;
    } else {
        document.getElementById("question").innerHTML =
            `Traduza <strong class="person-color-3">${pt_word}</strong> em Inglês`;
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
    const [, pt_word, en_word] = currentKey.split(SEP);
    const correctAnswer = ENtoPT ? pt_word : en_word;

    const prevCount = vocabCounters[currentKey];

    if (userAnswer.normalize('NFC') === correctAnswer.toLowerCase().normalize('NFC')) {
        document.getElementById("feedback").innerHTML = "Correto!";
        document.getElementById("feedback").className = "correct";
        vocabCounters[currentKey]++;
        totalScore++;

        if (vocabCounters[currentKey] >= requiredCorrect) {
            const index = vocabToPractice.indexOf(currentKey);
            if (index > -1) vocabToPractice.splice(index, 1);
        }
    } else {
        document.getElementById("feedback").innerHTML = `Errado. A resposta correta é '${correctAnswer}'.`;
        document.getElementById("feedback").className = "incorrect";
        if (vocabCounters[currentKey] > 0) vocabCounters[currentKey]--;
        totalScore--;
        mistakeCounters[currentKey]++;
    }

    updateScoreDisplay();

    const countChange = vocabCounters[currentKey] - prevCount;
    wordsCompleted = Math.max(0, Math.min(totalWordsNeeded, wordsCompleted + countChange));
    updateProgressBar();

    document.getElementById("answer-container").classList.add("hidden");
    document.getElementById("feedback").classList.remove("hidden");
    document.getElementById("next-question").classList.remove("hidden");
    isFeedbackDisplayed = true;

    stopTimer(timerState);
}

function updateProgressBar() {
    const pct = Math.max(0, Math.min(100, (wordsCompleted / totalWordsNeeded) * 100));
    document.getElementById("progress-bar").style.width = pct + "%";
    document.getElementById("progress-percentage").innerText = `Progresso: ${pct.toFixed(2)}%`;
}

function endQuiz() {
    stopTimer(timerState);
    document.getElementById("quiz").classList.add("hidden");
    document.getElementById("result").classList.remove("hidden");
    document.getElementById("total-score").innerText = `Sua pontuação total é: ${totalScore}`;
    updateBestScore('bestScore_vocab', totalScore);

    const sortedMistakes = Object.entries(mistakeCounters).sort((a, b) => b[1] - a[1]);
    const topMistakes = sortedMistakes.filter(item => item[1] > 0).slice(0, 10);
    const topMistakesList = document.getElementById("top-mistakes");
    topMistakesList.innerHTML = "";

    if (topMistakes.length > 0) {
        topMistakes.forEach(([key, mistakes], index) => {
            const [category, pt_word, en_word] = key.split(SEP);
            const li = document.createElement("li");
            li.innerHTML = `${index + 1}. Categoria: <strong>${category}</strong>, PT: <strong>${pt_word}</strong>, EN: <strong>${en_word}</strong>, Erros: ${mistakes}`;
            topMistakesList.appendChild(li);
        });
    } else {
        topMistakesList.innerHTML = "<li>Parabéns! Você não cometeu nenhum erro.</li>";
    }
}

function updateScoreDisplay() {
    document.getElementById("score-display").innerText = `Pontuação: ${totalScore}`;
}
