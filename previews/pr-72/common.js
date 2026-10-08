// Shared utilities for all quiz pages
import { t } from './i18n.js';

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
    document.addEventListener('languagechange', () => updateToggleIcon(btn, html.getAttribute('data-theme')));

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
    btn.setAttribute('aria-label', t(theme === 'dark' ? 'theme.toLight' : 'theme.toDark'));
}

export function padZero(num) {
    return num.toString().padStart(2, '0');
}

export function updateTimerDisplay(timerDisplay, elapsedTime) {
    const minutes = Math.floor(elapsedTime / 60);
    const seconds = elapsedTime % 60;
    timerDisplay.innerText = t('quiz.time', { time: `${padZero(minutes)}:${padZero(seconds)}` });
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
    const text = document.createElement("span");
    text.dataset.i18n = "selectAll";
    text.textContent = t("selectAll");
    label.append(cb, " ", text);
    container.prepend(label);

    cb.addEventListener("change", () => {
        container.querySelectorAll(`input[type="checkbox"]:not(#${cb.id})`).forEach(other => {
            other.checked = cb.checked;
        });
    });
}

/**
 * Values of the ticked checkboxes in a container, leaving out its "Selecionar tudo" box.
 * @param {string} containerId
 * @returns {string[]}
 */
export function getCheckedValues(containerId) {
    return Array.from(
        document.querySelectorAll(`#${containerId} input[type="checkbox"]:checked:not(#${containerId}-select-all)`),
    ).map(input => input.value);
}

export function updateBestScore(storageKey, score) {
    const prev = parseInt(localStorage.getItem(storageKey), 10);
    const isRecord = isNaN(prev) || score > prev;
    if (isRecord) localStorage.setItem(storageKey, score);
    const best = isRecord ? score : prev;
    const el = document.getElementById('best-score');
    if (!el) return;
    el.textContent = t(isRecord ? 'result.newBest' : 'result.best', { n: best });
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
