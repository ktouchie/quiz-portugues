// Shared utilities for all quiz pages

export function loadVersion() {
    fetch('version.txt')
        .then(r => r.text())
        .then(v => {
            const el = document.getElementById('version');
            if (el) el.textContent = v.trim();
        })
        .catch(err => console.error('Error fetching version:', err));
}

export function initTheme() {
    const saved = localStorage.getItem('theme');
    const html = document.documentElement;
    html.setAttribute('data-theme', saved || 'dark');

    const btn = document.getElementById('theme-toggle');
    if (!btn) return;

    updateToggleIcon(btn, html.getAttribute('data-theme'));

    btn.addEventListener('click', () => {
        const current = html.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        html.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
        updateToggleIcon(btn, next);
    });
}

function updateToggleIcon(btn, theme) {
    btn.textContent = theme === 'dark' ? '☀' : '☾';
    btn.setAttribute('aria-label', theme === 'dark' ? 'Mudar para modo claro' : 'Mudar para modo escuro');
}

export function padZero(num) {
    return num.toString().padStart(2, '0');
}

export function updateTimerDisplay(timerDisplay, elapsedTime) {
    const minutes = Math.floor(elapsedTime / 60);
    const seconds = elapsedTime % 60;
    timerDisplay.innerText = `Tempo: ${padZero(minutes)}:${padZero(seconds)}`;
}

export function startTimer(state) {
    if (state.timerInterval) clearInterval(state.timerInterval);
    state.timerInterval = setInterval(() => {
        state.elapsedTime++;
        updateTimerDisplay(state.timerDisplay, state.elapsedTime);
    }, 1000);
}

export function addSelectAll(containerId) {
    const container = document.getElementById(containerId);
    const label = document.createElement("label");
    label.className = "select-all-label";
    const cb = document.createElement("input");
    cb.type = "checkbox";
    cb.id = `${containerId}-select-all`;
    label.appendChild(cb);
    label.appendChild(document.createTextNode(" Selecionar tudo"));
    container.prepend(label);

    cb.addEventListener("change", () => {
        container.querySelectorAll(`input[type="checkbox"]:not(#${cb.id})`).forEach(other => {
            other.checked = cb.checked;
        });
    });
}

export function updateBestScore(storageKey, score) {
    const prev = parseInt(localStorage.getItem(storageKey), 10);
    const isRecord = isNaN(prev) || score > prev;
    if (isRecord) localStorage.setItem(storageKey, score);
    const best = isRecord ? score : prev;
    const el = document.getElementById('best-score');
    if (!el) return;
    el.textContent = isRecord
        ? `Novo recorde! Melhor pontuação: ${best}`
        : `Melhor pontuação: ${best}`;
    el.className = isRecord ? 'correct' : '';
}

export function stopTimer(state) {
    if (state.timerInterval) {
        clearInterval(state.timerInterval);
        state.timerInterval = null;
    }
}

export function resumeTimer(state) {
    if (!state.timerInterval) {
        state.timerInterval = setInterval(() => {
            state.elapsedTime++;
            updateTimerDisplay(state.timerDisplay, state.elapsedTime);
        }, 1000);
    }
}
