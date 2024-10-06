const verbs = {
    "ser": {
        "presente": ["sou", "és", "é", "somos", "são"],
        "pretérito": ["fui", "foste", "foi", "fomos", "foram"],
        "imperfeito": ["era", "eras", "era", "éramos", "eram"],
        "condicional": ["seria", "serias", "seria", "seríamos", "seriam"],
        "pretérito mais-que-perfeito": ["tinha sido", "tinhas sido", "tinha sido", "tínhamos sido", "tinham sido"],
        "perfeito_composto": ["tenho sido", "tens sido", "tem sido", "temos sido", "têm sido"],
        "futuro": ["serei", "serás", "será", "seremos", "serão"],
        "imperativo": ["seja", "sê", "seja", "sejamos", "sejam"],
    },
    "estar": {
        "presente": ["estou", "estás", "está", "estamos", "estão"],
        "pretérito": ["estive", "estiveste", "esteve", "estivemos", "estiveram"],
        "imperfeito": ["estava", "estavas", "estava", "estávamos", "estavam"],
        "condicional": ["estaria", "estarias", "estaria", "estaríamos", "estariam"],
        "pretérito mais-que-perfeito": ["tinha sido", "tinhas sido", "tinha sido", "tínhamos sido", "tinham sido"],
        "perfeito_composto": ["tenho estado", "tens estado", "tem estado", "temos estado", "têm estado"],
        "futuro": ["estarei", "estarás", "estará", "estaremos", "estarão"],
        "imperativo": ["esteja", "está", "esteja", "estejamos", "estejam"],
    },
    "falar": {
        "presente": ["falo", "falas", "fala", "falamos", "falam"],
        "pretérito": ["falei", "falaste", "falou", "falamos", "falaram"],
        "imperfeito": ["falava", "falavas", "falava", "falávamos", "falavam"],
        "condicional": ["falaria", "falarias", "falaria", "falaríamos", "falariam"],
        "pretérito mais-que-perfeito": ["tinha falado", "tinhas falado", "tinha falado", "tínhamos falado", "tinham falado"],
        "perfeito_composto": ["tenho falado", "tens falado", "tem falado", "temos falado", "têm falado"],
        "futuro": ["falarei", "falarás", "falará", "falaremos", "falarão"],
        "imperativo": ["fale", "fala", "fale", "falemos", "falem"],
    },
    "comer": {
        "presente": ["como", "comes", "come", "comemos", "comem"],
        "pretérito": ["comi", "comeste", "comeu", "comemos", "comeram"],
        "imperfeito": ["comia", "comias", "comia", "comíamos", "comiam"],
        "condicional": ["comeria", "comerias", "comeria", "comeríamos", "comeriam"],
        "pretérito mais-que-perfeito": ["tinha comido", "tinhas comido", "tinha comido", "tínhamos comido", "tinham comido"],
        "perfeito_composto": ["tenho comido", "tens comido", "tem comido", "temos comido", "têm comido"],
        "futuro": ["comerei", "comerás", "comerá", "comeremos", "comerão"],
        "imperativo": ["coma", "come", "coma", "comamos", "comam"],
    },
    "partir": {
        "presente": ["parto", "partes", "parte", "partimos", "partem"],
        "pretérito": ["parti", "partiste", "partiu", "partimos", "partiram"],
        "imperfeito": ["partia", "partias", "partia", "partíamos", "partiam"],
        "condicional": ["partiria", "partirias", "partiria", "partiríamos", "partiriam"],
        "pretérito mais-que-perfeito": ["tinha partido", "tinhas partido", "tinha partido", "tínhamos partido", "tinham partido"],
        "perfeito_composto": ["tenho partido", "tens partido", "tem partido", "temos partido", "têm partido"],
        "futuro": ["partirei", "partirás", "partirá", "partiremos", "partirão"],
        "imperativo": ["parta", "parte", "parta", "partamos", "partam"],
    },
    "dizer": {
        "presente": ["digo", "dizes", "diz", "dizemos", "dizem"],
        "pretérito": ["disse", "disseste", "disse", "dissemos", "disseram"],
        "imperfeito": ["dizia", "dizias", "dizia", "dizíamos", "diziam"],
        "condicional": ["diria", "dirias", "diria", "diríamos", "diriam"],
        "pretérito mais-que-perfeito": ["tinha dito", "tinhas dito", "tinha dito", "tínhamos dito", "tinham dito"],
        "perfeito_composto": ["tenho dito", "tens dito", "tem dito", "temos dito", "têm dito"],
        "futuro": ["direi", "dirás", "dirá", "diremos", "dirão"],
        "imperativo": ["diga", "diz", "diga", "digamos", "digam"],
    },
    "fazer": {
        "presente": ["faço", "fazes", "faz", "fazemos", "fazem"],
        "pretérito": ["fiz", "fizeste", "fez", "fizemos", "fizeram"],
        "imperfeito": ["fazia", "fazias", "fazia", "fazíamos", "faziam"],
        "condicional": ["faria", "farias", "faria", "faríamos", "fariam"],
        "pretérito mais-que-perfeito": ["tinha feito", "tinhas feito", "tinha feito", "tínhamos feito", "tinham feito"],
        "perfeito_composto": ["tenho feito", "tens feito", "tem feito", "temos feito", "têm feito"],
        "futuro": ["farei", "farás", "fará", "faremos", "farão"],
        "imperativo": ["faça", "faz", "faça", "façamos", "façam"],
    },
    "trazer": {
        "presente": ["trago", "trazes", "traz", "trazemos", "trazem"],
        "pretérito": ["trouxe", "trouxeste", "trouxe", "trouxemos", "trouxeram"],
        "imperfeito": ["trazia", "trazias", "trazia", "trazíamos", "traziam"],
        "condicional": ["traria", "trarias", "traria", "traríamos", "trariam"],
        "pretérito mais-que-perfeito": ["tinha trazido", "tinhas trazido", "tinha trazido", "tínhamos trazido", "tinham trazido"],
        "perfeito_composto": ["tenho trazido", "tens trazido", "tem trazido", "temos trazido", "têm trazido"],
        "futuro": ["trarei", "trarás", "trará", "traremos", "trarão"],
        "imperativo": ["traga", "traz", "traga", "tragamos", "tragam"],
    },
    "perder": {
        "presente": ["perco", "perdes", "perde", "perdemos", "perdem"],
        "pretérito": ["perdi", "perdeste", "perdeu", "perdemos", "perderam"],
        "imperfeito": ["perdia", "perdias", "perdia", "perdíamos", "perdiam"],
        "condicional": ["perderia", "perderias", "perderia", "perderíamos", "perderiam"],
        "pretérito mais-que-perfeito": ["tinha perdido", "tinhas perdido", "tinha perdido", "tínhamos perdido", "tinham perdido"],
        "perfeito_composto": ["tenho perdido", "tens perdido", "tem perdido", "temos perdido", "têm perdido"],
        "futuro": ["perderei", "perderás", "perderá", "perderemos", "perderão"],
        "imperativo": ["perca", "perde", "perca", "percamos", "percam"],
    },
    "poder": {
        "presente": ["posso", "podes", "pode", "podemos", "podem"],
        "pretérito": ["pude", "pudeste", "pôde", "pudemos", "puderam"],
        "imperfeito": ["podia", "podias", "podia", "podíamos", "podiam"],
        "condicional": ["poderia", "poderias", "poderia", "poderíamos", "poderiam"],
        "pretérito mais-que-perfeito": ["tinha podido", "tinhas podido", "tinha podido", "tínhamos podido", "tinham podido"],
        "perfeito_composto": ["tenho podido", "tens podido", "tem podido", "temos podido", "têm podido"],
        "futuro": ["poderei", "poderás", "poderá", "poderemos", "poderão"],
        "imperativo": ["possa", "pode", "possa", "possamos", "possam"],
    },
    "saber": {
        "presente": ["sei", "sabes", "sabe", "sabemos", "sabem"],
        "pretérito": ["soube", "soubeste", "soube", "soubemos", "souberam"],
        "imperfeito": ["sabia", "sabias", "sabia", "sabíamos", "sabiam"],
        "condicional": ["saberia", "saberias", "saberia", "saberíamos", "saberiam"],
        "pretérito mais-que-perfeito": ["tinha sabido", "tinhas sabido", "tinha sabido", "tínhamos sabido", "tinham sabido"],
        "perfeito_composto": ["tenho sabido", "tens sabido", "tem sabido", "temos sabido", "têm sabido"],
        "futuro": ["saberei", "saberás", "saberá", "saberemos", "saberão"],
        "imperativo": ["saiba", "sabe", "saiba", "saibamos", "saibam"],
    },
    "dormir": {
        "presente": ["durmo", "dormes", "dorme", "dormimos", "dormem"],
        "pretérito": ["dormi", "dormiste", "dormiu", "dormimos", "dormiram"],
        "imperfeito": ["dormia", "dormias", "dormia", "dormíamos", "dormiam"],
        "condicional": ["dormiria", "dormirias", "dormiria", "dormiríamos", "dormiriam"],
        "pretérito mais-que-perfeito": ["tinha dormido", "tinhas dormido", "tinha dormido", "tínhamos dormido", "tinham dormido"],
        "perfeito_composto": ["tenho dormido", "tens dormido", "tem dormido", "temos dormido", "têm dormido"],
        "futuro": ["dormirei", "dormirás", "dormirá", "dormiremos", "dormirão"],
        "imperativo": ["durma", "dorme", "durma", "durmamos", "durmam"],
    },
    "ouvir": {
        "presente": ["ouço", "ouves", "ouve", "ouvimos", "ouvem"],
        "pretérito": ["ouvi", "ouviste", "ouviu", "ouvimos", "ouviram"],
        "imperfeito": ["ouvia", "ouvias", "ouvia", "ouvíamos", "ouviam"],
        "condicional": ["ouviria", "ouvirias", "ouviria", "ouviríamos", "ouviriam"],
        "pretérito mais-que-perfeito": ["tinha ouvido", "tinhas ouvido", "tinha ouvido", "tínhamos ouvido", "tinham ouvido"],
        "perfeito_composto": ["tenho ouvido", "tens ouvido", "tem ouvido", "temos ouvido", "têm ouvido"],
        "futuro": ["ouvirei", "ouvirás", "ouvirá", "ouviremos", "ouvirão"],
        "imperativo": ["ouça", "ouve", "ouça", "ouçamos", "ouçam"],
    },
    "pedir": {
        "presente": ["peço", "pedes", "pede", "pedimos", "pedem"],
        "pretérito": ["pedi", "pediste", "pediu", "pedimos", "pediram"],
        "imperfeito": ["pedia", "pedias", "pedia", "pedíamos", "pediam"],
        "condicional": ["pediria", "pedirias", "pediria", "pediríamos", "pediriam"],
        "pretérito mais-que-perfeito": ["tinha pedido", "tinhas pedido", "tinha pedido", "tínhamos pedido", "tinham pedido"],
        "perfeito_composto": ["tenho pedido", "tens pedido", "tem pedido", "temos pedido", "têm pedido"],
        "futuro": ["pedirei", "pedirás", "pedirá", "pediremos", "pedirão"],
        "imperativo": ["peça", "pede", "peça", "peçamos", "peçam"],
    },
    "querer": {
        "presente": ["quero", "queres", "quer", "queremos", "querem"],
        "pretérito": ["quis", "quiseste", "quis", "quisemos", "quiseram"],
        "imperfeito": ["queria", "querias", "queria", "queríamos", "queriam"],
        "condicional": ["quereria", "quererias", "quereria", "quereríamos", "quereriam"],
        "pretérito mais-que-perfeito": ["tinha querido", "tinhas querido", "tinha querido", "tínhamos querido", "tinham querido"],
        "perfeito_composto": ["tenho querido", "tens querido", "tem querido", "temos querido", "têm querido"],
        "futuro": ["quererei", "quererás", "quererá", "quereremos", "quererão"],
        "imperativo": ["queira", "quer", "queira", "queiramos", "queiram"],
    },
    "dar": {
        "presente": ["dou", "dás", "dá", "damos", "dão"],
        "pretérito": ["dei", "deste", "deu", "demos", "deram"],
        "imperfeito": ["dava", "davas", "dava", "dávamos", "davam"],
        "condicional": ["daria", "darias", "daria", "daríamos", "dariam"],
        "pretérito mais-que-perfeito": ["tinha dado", "tinhas dado", "tinha dado", "tínhamos dado", "tinham dado"],
        "perfeito_composto": ["tenho dado", "tens dado", "tem dado", "temos dado", "têm dado"],
        "futuro": ["darei", "darás", "dará", "daremos", "darão"],
        "imperativo": ["dê", "dá", "dê", "demos", "deem"],
    },
    "ler": {
        "presente": ["leio", "lês", "lê", "lemos", "leem"],
        "pretérito": ["li", "leste", "leu", "lemos", "leram"],
        "imperfeito": ["lia", "lias", "lia", "líamos", "liam"],
        "condicional": ["leria", "lerias", "leria", "leríamos", "leriam"],
        "pretérito mais-que-perfeito": ["tinha lido", "tinhas lido", "tinha lido", "tínhamos lido", "tinham lido"],
        "perfeito_composto": ["tenho lido", "tens lido", "tem lido", "temos lido", "têm lido"],
        "futuro": ["lerei", "lerás", "lerá", "leremos", "lerão"],
        "imperativo": ["leia", "lê", "leia", "leiamos", "leiam"],
    },
    "ver": {
        "presente": ["vejo", "vês", "vê", "vemos", "veem"],
        "pretérito": ["vi", "viste", "viu", "vimos", "viram"],
        "imperfeito": ["via", "vias", "via", "víamos", "viam"],
        "condicional": ["veria", "verias", "veria", "veríamos", "veriam"],
        "pretérito mais-que-perfeito": ["tinha visto", "tinhas visto", "tinha visto", "tínhamos visto", "tinham visto"],
        "perfeito_composto": ["tenho visto", "tens visto", "tem visto", "temos visto", "têm visto"],
        "futuro": ["verei", "verás", "verá", "veremos", "verão"],
        "imperativo": ["veja", "vê", "veja", "vejamos", "vejam"],
    },
    "ter": {
        "presente": ["tenho", "tens", "tem", "temos", "têm"],
        "pretérito": ["tive", "tiveste", "teve", "tivemos", "tiveram"],
        "imperfeito": ["tinha", "tinhas", "tinha", "tínhamos", "tinham"],
        "condicional": ["teria", "terias", "teria", "teríamos", "teriam"],
        "pretérito mais-que-perfeito": ["tinha tido", "tinhas tido", "tinha tido", "tínhamos tido", "tinham tido"],
        "perfeito_composto": ["tenho tido", "tens tido", "tem tido", "temos tido", "têm tido"],
        "futuro": ["terei", "terás", "terá", "teremos", "terão"],
        "imperativo": ["tenha", "tem", "tenha", "tenhamos", "tenham"],
    },
    "vir": {
        "presente": ["venho", "vens", "vem", "vimos", "vêm"],
        "pretérito": ["vim", "vieste", "veio", "viemos", "vieram"],
        "imperfeito": ["vinha", "vinhas", "vinha", "vínhamos", "vinham"],
        "condicional": ["viria", "virias", "viria", "viríamos", "viriam"],
        "pretérito mais-que-perfeito": ["tinha vindo", "tinhas vindo", "tinha vindo", "tínhamos vindo", "tinham vindo"],
        "perfeito_composto": ["tenho vindo", "tens vindo", "tem vindo", "temos vindo", "têm vindo"],
        "futuro": ["virei", "virás", "virá", "viremos", "virão"],
        "imperativo": ["venha", "vem", "venha", "venhamos", "venham"],
    },
    "ir": {
        "presente": ["vou", "vais", "vai", "vamos", "vão"],
        "pretérito": ["fui", "foste", "foi", "fomos", "foram"],
        "imperfeito": ["ia", "ias", "ia", "íamos", "iam"],
        "condicional": ["iria", "irias", "iria", "iríamos", "iriam"],
        "pretérito mais-que-perfeito": ["tinha ido", "tinhas ido", "tinha ido", "tínhamos ido", "tinham ido"],
        "perfeito_composto": ["tenho ido", "tens ido", "tem ido", "temos ido", "têm ido"],
        "futuro": ["irei", "irás", "irá", "iremos", "irão"],
        "imperativo": ["vá", "vai", "vá", "vamos", "vão"],
    },
    "sair": {
        "presente": ["saio", "sais", "sai", "saímos", "saem"],
        "pretérito": ["saí", "saíste", "saiu", "saímos", "saíram"],
        "imperfeito": ["saía", "saías", "saía", "saíamos", "saíam"],
        "condicional": ["sairia", "sairias", "sairia", "sairíamos", "sairiam"],
        "pretérito mais-que-perfeito": ["tinha saído", "tinhas saído", "tinha saído", "tínhamos saído", "tinham saído"],
        "perfeito_composto": ["tenho saído", "tens saído", "tem saído", "temos saído", "têm saído"],
        "futuro": ["sairei", "sairás", "sairá", "sairemos", "sairão"],
        "imperativo": ["saia", "sai", "saia", "saiamos", "saiam"],
    },
    "pôr": {
        "presente": ["ponho", "pões", "põe", "pomos", "põem"],
        "pretérito": ["pus", "puseste", "pôs", "pusemos", "puseram"],
        "imperfeito": ["punha", "punhas", "punha", "púnhamos", "punham"],
        "condicional": ["poria", "porias", "poria", "poríamos", "poriam"],
        "pretérito mais-que-perfeito": ["tinha posto", "tinhas posto", "tinha posto", "tínhamos posto", "tinham posto"],
        "perfeito_composto": ["tenho posto", "tens posto", "tem posto", "temos posto", "têm posto"],
        "futuro": ["porei", "porás", "porá", "poremos", "porão"],
        "imperativo": ["ponha", "põe", "ponha", "ponhamos", "ponham"],
    },
};

const persons = ["eu", "tu", "ele/ela/você", "nós", "eles/elas/vocês"];
const tenses = ["presente", "pretérito", "imperfeito", "condicional", "pretérito mais-que-perfeito", "perfeito composto", "futuro", "imperativo"];

let selectedTenses = [];
let requiredCorrect = 3;
let conjugationCounters = {};
let mistakeCounters = {};
let conjugationsToPractice = [];
let totalScore = 0;
let currentKey = null;
let totalConjugationsNeeded = 0;
let conjugationsCompleted = 0;
let scoreDisplay;

document.addEventListener("DOMContentLoaded", () => {
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

    document.getElementById("start-quiz").addEventListener("click", startQuiz);
    document.getElementById("submit-answer").addEventListener("click", submitAnswer);
    document.getElementById("restart").addEventListener("click", () => location.reload());

    // Add event listener for the "Enter" key on the answer input field
    document.getElementById("answer").addEventListener("keydown", function(event) {
        if (event.key === "Enter") {
            event.preventDefault(); // Prevent default action (if any)
            submitAnswer();
        }
    });

    // Initialize the score display
    scoreDisplay = document.getElementById("score-display");
    updateScoreDisplay();
});

function startQuiz() {
    selectedTenses = Array.from(document.querySelectorAll("#tenses input:checked")).map(input => input.value);
    if (selectedTenses.length === 0) {
        alert("Please select at least one tense.");
        return;
    }
    requiredCorrect = parseInt(document.getElementById("required-correct").value) || 3;

    // Initialize counters
    totalConjugationsNeeded = 0;
    conjugationsCompleted = 0;
    for (let verb in verbs) {
        for (let tense of selectedTenses) {
            if (verbs[verb][tense]) {
                for (let personIdx = 0; personIdx < persons.length; personIdx++) {
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

    document.getElementById("setup").style.display = "none";
    document.getElementById("quiz").style.display = "block";
    updateProgressBar();
    nextQuestion();
}

function nextQuestion() {
    if (conjugationsToPractice.length === 0) {
        endQuiz();
        return;
    }
    const randomIndex = Math.floor(Math.random() * conjugationsToPractice.length);
    currentKey = conjugationsToPractice[randomIndex];
    const [verb, tense, personIdx] = currentKey.split("-");
    const person = persons[personIdx];
    document.getElementById("question").innerHTML = `Conjugue o verbo <strong>${verb}</strong> no tempo <strong>${tense}</strong> para <strong>${person}</strong>:`;
    document.getElementById("answer").value = "";
    document.getElementById("feedback").innerHTML = "";

    // Automatically focus on the answer input field
    document.getElementById("answer").focus();
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

    // Update progress based on the change in conjugation counters
    let countChange = conjugationCounters[currentKey] - prevCount;
    conjugationsCompleted += countChange;
    conjugationsCompleted = Math.max(0, Math.min(totalConjugationsNeeded, conjugationsCompleted)); // Clamp between 0 and totalConjugationsNeeded

    updateProgressBar();

    // Update the score display whenever the score changes
    updateScoreDisplay();

    setTimeout(nextQuestion, 1000);
}

function updateProgressBar() {
    let progressPercentage = (conjugationsCompleted / totalConjugationsNeeded) * 100;
    progressPercentage = Math.max(0, Math.min(100, progressPercentage)); // Clamp between 0 and 100

    document.getElementById("progress-bar").style.width = progressPercentage + "%";
    document.getElementById("progress-percentage").innerText = `Progresso: ${progressPercentage.toFixed(2)}%`;
}

function updateScoreDisplay() {
    scoreDisplay.innerText = `Pontuação: ${totalScore}`;
}

function endQuiz() {
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
