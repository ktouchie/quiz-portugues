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
let timerInterval = null;
let elapsedTime = 0; // in seconds
let isFeedbackDisplayed = false;

// DOM elements
let scoreDisplay, timerDisplay;

document.addEventListener("DOMContentLoaded", async () => {
    // Load vocabulary data
    try {
        const response = await fetch('vocabulary.json');
        if (!response.ok) throw new Error("Failed to load vocabulary data.");
        vocabulary = await response.json();

        initializeQuiz();
    } catch (error) {
        console.error(error);
        alert("Erro ao carregar os dados dos vocabulário.");
    }

    // Load version
    fetch('version.txt')
        .then(response => response.text())
        .then(version => {
            console.log(version);
            const versionElement = document.getElementById('version');
            if (versionElement) {
                versionElement.textContent = version.trim();
            } else {
                console.warn('Version element not found');
            }
        })
        .catch(err => {
            console.error('Error fetching version:', err);
        });
});

function initializeQuiz() {
    // Populate vocab selection
    const categoryDiv = document.getElementById("categories");
    const categories = Object.keys(vocabulary)
    categories.forEach((category, index) => {
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = category;
        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(category));
        categoryDiv.appendChild(label);
    });

    // Attach event listeners to buttons
    document.getElementById("start-quiz").addEventListener("click", startQuiz);
    document.getElementById("submit-answer").addEventListener("click", submitAnswer);
    document.getElementById("next-question").addEventListener("click", nextQuestion);
    document.getElementById("restart").addEventListener("click", () => location.reload());

    // Initialize score and timer displays
    scoreDisplay = document.getElementById("score-display");
    timerDisplay = document.getElementById("timer-display");

    // Update displays
    updateScoreDisplay();
    updateTimerDisplay();

    document.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            if (!isFeedbackDisplayed) {
                // If feedback isn't displayed, trigger the "Enviar" button
                document.getElementById("submit-answer").click();
            } else {
                // If feedback is displayed, trigger the "Próxima" button
                document.getElementById("next-question").click();
            }
        }
    });

}

function startQuiz() {
    selectedCategories = Array.from(document.querySelectorAll("#categories input:checked")).map(input => input.value);
    if (selectedCategories.length === 0) {
        alert("Please select at least one category.");
        return;
    }

    ENtoPT = document.querySelector("input[name='translation-direction']:checked").value;

    // Initialize counters
    totalWordsNeeded = 0;
    wordsCompleted = 0;
    for (let category of selectedCategories) {
        for (let pt_word in vocabulary[category]) {
            if (vocabulary[category].hasOwnProperty(pt_word)) {
                let key = `${category}_${pt_word}_${vocabulary[category][pt_word]}`;
                vocabCounters[key] = 0;
                mistakeCounters[key] = 0;
                vocabToPractice.push(key);

                // Update total words needed
                totalWordsNeeded += requiredCorrect;
            }
        }
    }

    document.getElementById("setup").style.display = "none";
    document.getElementById("quiz").style.display = "block";
    updateProgressBar();
    nextQuestion();
    startTimer();
}

function nextQuestion() {
    if (vocabToPractice.length === 0) {
        endQuiz();
        return;
    }
    isFeedbackDisplayed = false;
    const randomIndex = Math.floor(Math.random() * vocabToPractice.length);
    currentKey = vocabToPractice[randomIndex];
    const [category, pt_word, en_word] = currentKey.split("_");

    if (ENtoPT) {
        document.getElementById("question").innerHTML = `Traduza <strong class="person-color-3">${en_word}</strong> em Português`
    } else {
        document.getElementById("question").innerHTML = `Traduza <strong class="person-color-3">${pt_word}</strong> em Inglês`;
    }
    document.getElementById("answer").value = "";
    document.getElementById("feedback").innerHTML = "";
    document.getElementById("answer-container").style.display = "block"; // Show answer box and button
    document.getElementById("feedback").style.display = "none"; // Hide feedback message
    document.getElementById("next-question").style.display = "none";

    // Automatically focus on the answer input field
    document.getElementById("answer").focus();

    startTimer();
}

function submitAnswer() {
    const userAnswer = document.getElementById("answer").value.trim().toLowerCase();
    const [category, pt_word, en_word] = currentKey.split("_");
    let correctAnswer = pt_word
    if (!ENtoPT) {
        correctAnswer = en_word
    }

    let prevCount = vocabCounters[currentKey];

    if (userAnswer.normalize('NFC') === correctAnswer.toLowerCase().normalize('NFC')) {
        document.getElementById("feedback").innerHTML = "Correto!";
        document.getElementById("feedback").className = "correct";
        vocabCounters[currentKey]++;
        totalScore++;

        if (vocabCounters[currentKey] >= requiredCorrect) {
            const index = vocabToPractice.indexOf(currentKey);
            if (index > -1) {
                vocabToPractice.splice(index, 1);
            }
        }
    } else {
        document.getElementById("feedback").innerHTML = `Errado. A resposta correta é '${correctAnswer}'.`;
        document.getElementById("feedback").className = "incorrect";
        if (vocabCounters[currentKey] > 0) {
            vocabCounters[currentKey]--;
        }
        totalScore--;
        mistakeCounters[currentKey]++;
    }

    // Update the score display
    updateScoreDisplay();

    // Update progress based on the change in conjugation counters
    let countChange = vocabCounters[currentKey] - prevCount;
    wordsCompleted += countChange;
    wordsCompleted = Math.max(0, Math.min(totalWordsNeeded, wordsCompleted)); // Clamp between 0 and totalWordsNeeded

    updateProgressBar();

    // Hide answer box and button, show feedback
    document.getElementById("answer-container").style.display = "none";
    document.getElementById("feedback").style.display = "block";
    document.getElementById("next-question").style.display = "block";
    isFeedbackDisplayed = true;

    stopTimer();
}

function updateProgressBar() {
    let progressPercentage = (wordsCompleted / totalWordsNeeded) * 100;
    progressPercentage = Math.max(0, Math.min(100, progressPercentage)); // Clamp between 0 and 100

    document.getElementById("progress-bar").style.width = progressPercentage + "%";
    document.getElementById("progress-percentage").innerText = `Progresso: ${progressPercentage.toFixed(2)}%`;
}

function endQuiz() {
    stopTimer();
    document.getElementById("quiz").style.display = "none";
    document.getElementById("result").style.display = "block";
    document.getElementById("total-score").innerText = `Sua pontuação total é: ${totalScore}`;

    const sortedMistakes = Object.entries(mistakeCounters).sort((a, b) => b[1] - a[1]);
    const topMistakes = sortedMistakes.filter(item => item[1] > 0).slice(0, 10);

    const topMistakesList = document.getElementById("top-mistakes");
    topMistakesList.innerHTML = "";

    if (topMistakes.length > 0) {
        topMistakes.forEach(([key, mistakes], index) => {
            const [category, pt_word, en_word] = key.split("_");
            const li = document.createElement("li");
            li.innerHTML = `${index + 1}. Categoria: <strong>${category}</strong>, PT: <strong>${pt_word}</strong>, EN: <strong>${en_word}</strong>, Erros: ${mistakes}`;
            topMistakesList.appendChild(li);
        });
    } else {
        topMistakesList.innerHTML = "<li>Parabéns! Você não cometeu nenhum erro.</li>";
    }
}

function updateScoreDisplay() {
    scoreDisplay.innerText = `Pontuação: ${totalScore}`;
}

function startTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
    }
    timerInterval = setInterval(() => {
        elapsedTime++;
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
    let minutes = Math.floor(elapsedTime / 60);
    let seconds = elapsedTime % 60;
    timerDisplay.innerText = `Tempo: ${padZero(minutes)}:${padZero(seconds)}`;
}

function padZero(num) {
    return num.toString().padStart(2, '0');
}
