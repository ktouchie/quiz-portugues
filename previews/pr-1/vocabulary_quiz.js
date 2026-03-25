const REQUIRED_CORRECT = 1;
const KEY_SEPARATOR = '::';

let vocabulary = {};
let selectedCategories = [];
let translateEnToPt = true;
let vocabCounters = {};
let mistakeCounters = {};
let vocabPrompts = [];
let promptLookup = {};
let totalScore = 0;
let currentPrompt = null;
let totalWordsNeeded = 0;
let wordsCompleted = 0;
let timerInterval = null;
let elapsedTime = 0;
let isFeedbackDisplayed = false;

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

function buildPromptKey(category, ptWord, enWord) {
    return [category, ptWord, enWord].join(KEY_SEPARATOR);
}

document.addEventListener("DOMContentLoaded", async () => {
    cacheDomElements();
    initializeUiState();

    try {
        const response = await fetch('vocabulary.json');
        if (!response.ok) throw new Error(`Falha ao carregar vocabulário (${response.status}).`);
        vocabulary = await response.json();
        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados do vocabulário. Atualize a página para tentar novamente.");
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
    renderCategoryOptions();

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

function renderCategoryOptions() {
    const categoriesContainer = document.getElementById("categories");
    categoriesContainer.innerHTML = "";

    const categories = Object.keys(vocabulary).sort((a, b) => a.localeCompare(b));

    categories.forEach((category) => {
        const label = document.createElement("label");
        label.className = "selection-chip";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = category;

        const span = document.createElement("span");
        span.className = "chip-label";
        span.textContent = category;

        checkbox.addEventListener("change", () => {
            label.classList.toggle("active", checkbox.checked);
        });

        label.appendChild(checkbox);
        label.appendChild(span);
        categoriesContainer.appendChild(label);
    });
}

function startQuiz() {
    selectedCategories = Array.from(document.querySelectorAll("#categories input:checked"))
        .map((input) => input.value);

    if (selectedCategories.length === 0) {
        alert("Selecione pelo menos uma categoria para começar.");
        return;
    }

    const directionValue = document.querySelector("input[name='translation-direction']:checked")?.value;
    translateEnToPt = directionValue === "true";

    resetQuizState();
    populatePrompts();

    if (vocabPrompts.length === 0) {
        alert("Não encontramos palavras para as categorias selecionadas. Escolha outras categorias e tente novamente.");
        return;
    }

    setupSection.hidden = true;
    quizSection.hidden = false;
    resultSection.hidden = true;

    updateProgressBar();
    nextQuestion();
}

function resetQuizState() {
    vocabCounters = {};
    mistakeCounters = {};
    vocabPrompts = [];
    promptLookup = {};
    totalWordsNeeded = 0;
    wordsCompleted = 0;
    totalScore = 0;
    currentPrompt = null;
    elapsedTime = 0;
    stopTimer();

    updateScoreDisplay();
    updateTimerDisplay();
    updateProgressBar();
}

function populatePrompts() {
    selectedCategories.forEach((category) => {
        const entries = Object.entries(vocabulary[category] || {});
        entries.forEach(([ptWord, enWord]) => {
            if (!ptWord || !enWord) return;
            const key = buildPromptKey(category, ptWord, enWord);
            const prompt = {
                key,
                category,
                ptWord,
                enWord
            };
            registerPrompt(prompt);
        });
    });
}

function registerPrompt(prompt) {
    vocabPrompts.push(prompt);
    promptLookup[prompt.key] = prompt;
    vocabCounters[prompt.key] = 0;
    mistakeCounters[prompt.key] = 0;
    totalWordsNeeded += REQUIRED_CORRECT;
}

function nextQuestion() {
    if (vocabPrompts.length === 0) {
        endQuiz();
        return;
    }

    isFeedbackDisplayed = false;
    const randomIndex = Math.floor(Math.random() * vocabPrompts.length);
    currentPrompt = vocabPrompts[randomIndex];

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

    if (translateEnToPt) {
        questionElement.innerHTML = `Traduza <strong class="person-color-3">${prompt.enWord}</strong> para Português.`;
    } else {
        questionElement.innerHTML = `Traduza <strong class="person-color-3">${prompt.ptWord}</strong> para Inglês.`;
    }
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
    const previousCount = vocabCounters[key];

    if (userAnswer === correctAnswer) {
        feedbackElement.textContent = "Resposta correta!";
        feedbackElement.className = "correct";
        vocabCounters[key] += 1;
        totalScore += 1;

        if (vocabCounters[key] >= REQUIRED_CORRECT) {
            const index = vocabPrompts.findIndex((item) => item.key === key);
            if (index > -1) {
                vocabPrompts.splice(index, 1);
            }
        }
    } else {
        feedbackElement.textContent = `Não foi desta. A resposta correta é "${getCorrectAnswer(currentPrompt)}".`;
        feedbackElement.className = "incorrect";
        if (vocabCounters[key] > 0) {
            vocabCounters[key] -= 1;
        }
        totalScore -= 1;
        mistakeCounters[key] += 1;
    }

    updateScoreDisplay();

    const countChange = vocabCounters[key] - previousCount;
    wordsCompleted = clamp(wordsCompleted + countChange, 0, totalWordsNeeded);
    updateProgressBar();

    answerContainer.hidden = true;
    nextQuestionButton.hidden = false;
    isFeedbackDisplayed = true;
    stopTimer();
}

function getCorrectAnswer(prompt) {
    return translateEnToPt ? prompt.ptWord : prompt.enWord;
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
        li.innerHTML = `${index + 1}. Categoria <strong>${prompt.category}</strong> — PT: <strong>${prompt.ptWord}</strong>, EN: <strong>${prompt.enWord}</strong> · erros: ${count}`;
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

    const progress = totalWordsNeeded === 0 ? 0 : (wordsCompleted / totalWordsNeeded) * 100;
    const clamped = clamp(progress, 0, 100);
    progressBar.style.width = `${clamped}%`;
    progressLabel.textContent = `Progresso: ${clamped.toFixed(2)}%`;
}

function padZero(num) {
    return num.toString().padStart(2, "0");
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}
