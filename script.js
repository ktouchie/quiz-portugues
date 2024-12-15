const persons = ["eu", "tu", "ele/ela/você", "nós", "eles/elas/vocês"];
const tenses = ["presente", "pretérito", "imperfeito", "condicional", "pretérito mais-que-perfeito", "perfeito composto", "futuro", "imperativo", "conjuntivo", "infinitivo pessoal"];

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
let timerInterval = null;
let elapsedTime = 0; // in seconds
let isFeedbackDisplayed = false;

// DOM elements
let scoreDisplay, timerDisplay;

document.addEventListener("DOMContentLoaded", async () => {
    // Load verbs data
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
    // Populate tenses selection
    const tensesDiv = document.getElementById("tenses");
    tenses.forEach((tense, index) => {
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = tense;
        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(tense));
        tensesDiv.appendChild(label);
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
    selectedTenses = Array.from(document.querySelectorAll("#tenses input:checked")).map(input => input.value);
    if (selectedTenses.length === 0) {
        alert("Please select at least one tense.");
        return;
    }

    // Initialize counters
    totalConjugationsNeeded = 0;
    conjugationsCompleted = 0;
    for (let verb in verbs) {
        for (let tense of selectedTenses) {
            if (verbs[verb][tense]) {
                for (let personIdx = 0; personIdx < persons.length; personIdx++) {
                    if (verbs[verb][tense][personIdx].length > 0) {
                        let key = `${verb}-${tense}-${personIdx}`;
                        conjugationCounters[key] = 0;
                        mistakeCounters[key] = 0;
                        conjugationsToPractice.push(key);

                        // Update total conjugations needed
                        totalConjugationsNeeded += requiredCorrect;
                    }
                }
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
    if (conjugationsToPractice.length === 0) {
        endQuiz();
        return;
    }
    isFeedbackDisplayed = false;
    const randomIndex = Math.floor(Math.random() * conjugationsToPractice.length);
    currentKey = conjugationsToPractice[randomIndex];
    const [verb, tense, personIdx] = currentKey.split("-");
    const person = persons[personIdx];

    // Color coding classes
    const verbClass = verbs[verb].regular ? 'regular-verb' : 'irregular-verb';
    const tenseClass = `tense-color-${tenses.indexOf(tense)}`;
    const personClass = `person-color-${personIdx}`;

    document.getElementById("question").innerHTML = `Conjugue o verbo <strong class="${verbClass}">${verb}</strong> no tempo <strong class="${tenseClass}">${tense}</strong> para <strong class="${personClass}">${person}</strong>:`;
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
    const [verb, tense, personIdx] = currentKey.split("-");
    const correctAnswer = verbs[verb][tense][personIdx];

    let prevCount = conjugationCounters[currentKey];

    if (userAnswer.normalize('NFC') === correctAnswer.toLowerCase().normalize('NFC')) {
        document.getElementById("feedback").innerHTML = "Correto!";
        document.getElementById("feedback").className = "correct";
        conjugationCounters[currentKey]++;
        totalScore++;

        if (conjugationCounters[currentKey] >= requiredCorrect) {
            const index = conjugationsToPractice.indexOf(currentKey);
            if (index > -1) {
                conjugationsToPractice.splice(index, 1);
            }
        }
    } else {
        document.getElementById("feedback").innerHTML = `Errado. A resposta correta é '${correctAnswer}'.`;
        document.getElementById("feedback").className = "incorrect";
        if (conjugationCounters[currentKey] > 0) {
            conjugationCounters[currentKey]--;
        }
        totalScore--;
        mistakeCounters[currentKey]++;
    }

    // Update the score display
    updateScoreDisplay();

    // Update progress based on the change in conjugation counters
    let countChange = conjugationCounters[currentKey] - prevCount;
    conjugationsCompleted += countChange;
    conjugationsCompleted = Math.max(0, Math.min(totalConjugationsNeeded, conjugationsCompleted)); // Clamp between 0 and totalConjugationsNeeded

    updateProgressBar();

    // Hide answer box and button, show feedback
    document.getElementById("answer-container").style.display = "none";
    document.getElementById("feedback").style.display = "block";
    document.getElementById("next-question").style.display = "block";
    isFeedbackDisplayed = true;

    stopTimer();
}

function updateProgressBar() {
    let progressPercentage = (conjugationsCompleted / totalConjugationsNeeded) * 100;
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
            const [verb, tense, personIdx] = key.split("-");
            const person = persons[personIdx];
            const correctAnswer = verbs[verb][tense][personIdx];
            const li = document.createElement("li");
            li.innerHTML = `${index + 1}. Verbo: <strong>${verb}</strong>, Tempo: <strong>${tense}</strong>, Pessoa: <strong>${person}</strong>, Resposta correta: '<strong>${correctAnswer}</strong>', Erros: ${mistakes}`;
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
