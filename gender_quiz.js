import { loadVersion, initTheme, startTimer, stopTimer, updateTimerDisplay, updateBestScore } from './common.js';

// Each item is one form of one word: { masculine, label, answer, key }
let words = {};
let itemsToPractice = [];
let itemCounters = {};
let mistakeCounters = {};
let currentItem = null;
let totalScore = 0;
let itemsCompleted = 0;
let totalItemsNeeded = 0;
let isFeedbackDisplayed = false;
const REQUIRED_CORRECT = 1;

const timerState = { timerInterval: null, elapsedTime: 0, timerDisplay: null };

document.addEventListener("DOMContentLoaded", async () => {
    initTheme();
    loadVersion();

    try {
        const response = await fetch('gender_quiz.json');
        if (!response.ok) throw new Error("Failed to load gender quiz data.");
        words = await response.json();
        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados.");
    }
});

function initializeQuiz() {
    document.getElementById("start-quiz").addEventListener("click", startQuiz);
    document.getElementById("submit-answer").addEventListener("click", submitAnswer);
    document.getElementById("next-question").addEventListener("click", nextQuestion);
    document.getElementById("restart").addEventListener("click", () => location.reload());

    timerState.timerDisplay = document.getElementById("timer-display");
    updateTimerDisplay(timerState.timerDisplay, 0);
    updateScoreDisplay();

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Enter") return;
        if (!isFeedbackDisplayed) {
            document.getElementById("submit-answer").click();
        } else {
            document.getElementById("next-question").click();
        }
    });
}

function buildItems(selectedCategories) {
    const items = [];
    for (const category of selectedCategories) {
        words[category].forEach((word, index) => {
            if (word.feminine !== null) {
                items.push({
                    key: `${category}|||${index}|||f`,
                    masculine: word.masculine,
                    label: "feminino",
                    answer: word.feminine,
                });
            }
            items.push({
                key: `${category}|||${index}|||p`,
                masculine: word.masculine,
                label: "plural masculino",
                answer: word.plural,
            });
        });
    }
    return items;
}

function startQuiz() {
    const allItems = buildItems(Object.keys(words));
    itemsToPractice = allItems.map(item => item.key);
    itemCounters = {};
    mistakeCounters = {};
    const itemMap = {};
    allItems.forEach(item => {
        itemCounters[item.key] = 0;
        mistakeCounters[item.key] = 0;
        itemMap[item.key] = item;
    });
    // Store item data on the module-level variable
    Object.assign(itemData, itemMap);

    totalItemsNeeded = itemsToPractice.length * REQUIRED_CORRECT;
    itemsCompleted = 0;
    totalScore = 0;

    document.getElementById("setup").classList.add("hidden");
    document.getElementById("quiz").classList.remove("hidden");
    updateProgressBar();
    nextQuestion();
    startTimer(timerState);
}

const itemData = {};

function nextQuestion() {
    if (itemsToPractice.length === 0) {
        endQuiz();
        return;
    }
    isFeedbackDisplayed = false;

    const randomIndex = Math.floor(Math.random() * itemsToPractice.length);
    currentItem = itemData[itemsToPractice[randomIndex]];

    document.getElementById("question").innerHTML =
        `Qual é o <strong class="person-color-1">${currentItem.label}</strong> de <strong class="person-color-3">${currentItem.masculine}</strong>?`;

    document.getElementById("answer").value = "";
    document.getElementById("feedback").innerHTML = "";
    document.getElementById("answer-container").classList.remove("hidden");
    document.getElementById("feedback").classList.add("hidden");
    document.getElementById("next-question").classList.add("hidden");
    document.getElementById("answer").focus();

    startTimer(timerState);
}

function submitAnswer() {
    const userAnswer = document.getElementById("answer").value.trim().toLowerCase().normalize('NFC');
    const correctAnswer = currentItem.answer.toLowerCase().normalize('NFC');
    const key = currentItem.key;
    const prevCount = itemCounters[key];

    if (userAnswer === correctAnswer) {
        document.getElementById("feedback").innerHTML = "Correto!";
        document.getElementById("feedback").className = "correct";
        itemCounters[key]++;
        totalScore++;
        if (itemCounters[key] >= REQUIRED_CORRECT) {
            const index = itemsToPractice.indexOf(key);
            if (index > -1) itemsToPractice.splice(index, 1);
        }
    } else {
        document.getElementById("feedback").innerHTML =
            `Errado. A resposta correta é <strong>${currentItem.answer}</strong>.`;
        document.getElementById("feedback").className = "incorrect";
        if (itemCounters[key] > 0) itemCounters[key]--;
        totalScore--;
        mistakeCounters[key]++;
    }

    updateScoreDisplay();

    const countChange = itemCounters[key] - prevCount;
    itemsCompleted = Math.max(0, Math.min(totalItemsNeeded, itemsCompleted + countChange));
    updateProgressBar();

    document.getElementById("answer-container").classList.add("hidden");
    document.getElementById("feedback").classList.remove("hidden");
    document.getElementById("next-question").classList.remove("hidden");
    isFeedbackDisplayed = true;

    stopTimer(timerState);
}

function updateProgressBar() {
    const pct = totalItemsNeeded > 0 ? Math.max(0, Math.min(100, (itemsCompleted / totalItemsNeeded) * 100)) : 0;
    document.getElementById("progress-bar").style.width = pct + "%";
    document.getElementById("progress-percentage").innerText = `Progresso: ${pct.toFixed(2)}%`;
}

function endQuiz() {
    stopTimer(timerState);
    document.getElementById("quiz").classList.add("hidden");
    document.getElementById("result").classList.remove("hidden");
    document.getElementById("total-score").innerText = `Sua pontuação total é: ${totalScore}`;
    updateBestScore('bestScore_gender', totalScore);

    const sortedMistakes = Object.entries(mistakeCounters).sort((a, b) => b[1] - a[1]);
    const topMistakes = sortedMistakes.filter(([, count]) => count > 0).slice(0, 10);
    const topMistakesList = document.getElementById("top-mistakes");
    topMistakesList.innerHTML = "";

    if (topMistakes.length > 0) {
        topMistakes.forEach(([key, mistakes]) => {
            const item = itemData[key];
            const li = document.createElement("li");
            li.innerHTML = `${item.masculine} → ${item.label}: <strong>${item.answer}</strong> &nbsp;<span style="color:var(--text-muted)">(${mistakes} erro${mistakes > 1 ? 's' : ''})</span>`;
            topMistakesList.appendChild(li);
        });
    } else {
        topMistakesList.innerHTML = "<li>Parabéns! Não cometeu nenhum erro.</li>";
    }
}

function updateScoreDisplay() {
    document.getElementById("score-display").innerText = `Pontuação: ${totalScore}`;
}
