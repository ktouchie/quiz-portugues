# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A static client-side European Portuguese language learning application with three quiz modules. No build process — files are served directly from the repository.

## Running Locally

Serve the static files with any HTTP server, e.g.:
```
npx serve .
# or
python3 -m http.server
```

Install dev dependencies (for linting and tests only):
```
npm install
npm test
npm run lint
```

## Architecture

**Three independent quiz modules**, each with its own HTML + JS file:

- `verb_quiz.html` + `script.js` — Verb conjugation quiz; loads `verbs.json`
- `vocabulary_quiz.html` + `vocabulary_quiz.js` — Vocabulary translation quiz; loads `vocabulary.json`
- `gender_quiz.html` + `gender_quiz.js` — Gender & plural quiz; loads `gender_quiz.json`
- `index.html` — Home page with streak, mastered count, personal goal; fetches `version.txt` to display version

**Shared:**
- `common.js` — `initTheme`, `loadVersion`, `startTimer`, `stopTimer`, `resumeTimer`, `updateTimerDisplay`, `updateBestScore`, `addSelectAll`
- `quiz_base.js` — `QuizBase` class: shared lifecycle (init, startQuiz, nextQuestion, submitAnswer, endQuiz), SRS integration, confidence buttons, audio, progress bar, retry-mistakes
- `config.js` — `PERSONS`, `TENSE_LABELS`, `STORAGE_KEYS`
- `srs.js` — SM-2 spaced repetition: `loadSRSState`, `saveSRSState`, `getItemSRS`, `sm2`, `getDueItems`
- `audio.js` — Web Speech API wrapper: `initAudio`, `isAudioAvailable`, `speak`
- `gamification.js` — `loadStreak`, `updateStreak`, `getTotalMastered`, `checkMilestone`, `showMilestoneBanner`, `loadGoal`, `saveGoal`, `getGoalProgress`
- `grammar_hints.js` — `getVerbHint(tense, verb, third)`, `getGenderHint(category)`
- `styles.css` — Applies to all pages; uses CSS custom properties for dark/light theming

**Data files:**
- `verbs.json` — `{ verbName: { regular, difficulty, tense: [...5 forms...], exemplos: { presente: [...], pretérito: [...] } } }` — 26 conjugation verbs + 25 participios-only verbs; `difficulty`: `"beginner" | "intermediate" | "advanced"`; `exemplos` on 16 high-frequency verbs
- `vocabulary.json` — `{ category: { portuguese: "english" } }`; 31 categories, ~548 words
- `gender_quiz.json` — `{ category: [{ masculine, feminine, plural }] }`; 4 categories, 53 words, 102 quiz items

**SRS state** is persisted per quiz in `localStorage` under keys `srs_verbs`, `srs_vocab`, `srs_gender`.
**Best scores** are persisted under `bestScore_verbs`, `bestScore_vocab`, `bestScore_gender`.
**Gamification** keys: `streak_data`, `seen_milestones`, `goal_data`.

## Quiz mechanics (shared pattern)

All three quizzes share `QuizBase`:
1. Setup screen — select tenses/categories/difficulty; SRS due-count shown
2. Questions drawn randomly; SRS due items sorted first
3. Correct answer → confidence prompt (Fácil/OK/Difícil → SM-2 quality 5/4/3); item retired
4. Wrong answer → grammar hint + example sentence (verb quiz); SM-2 quality 0; item stays in pool
5. Score: `correctCount` / `errorCount` tracked independently; best score = max correctCount
6. Result screen: time, accuracy %, top mistakes, "Praticar erros" retry button
7. Streak updated on quiz completion; milestones checked; goal progress tracked

**Verb quiz extras:** adaptive difficulty filter (beginner/intermediate/advanced), interleaved mode (Fisher-Yates shuffle), example sentences for 16 high-frequency verbs.

## Tooling

- **ESLint**: flat config (`eslint.config.js`), ES2022 modules, browser globals
- **Vitest**: jsdom environment, tests in `tests/`
- **lefthook**: pre-commit runs `npm run lint` + `npm test` via `~/.local/bin/npm`
- **CI**: `.github/workflows/ci.yml` runs lint + test on push/PR to main

## Working Conventions

- After any major update (new module, feature, data change, architecture change), update both `CLAUDE.md` and `README.md` to reflect the current state before committing.

## Deployment

- **Main branch** auto-deploys to GitHub Pages via `.github/workflows/version-bump.yml`, which also auto-bumps the patch version in `version.txt` (skipped if `version.txt` was changed manually in the same push)
- **PRs** get preview deployments at `previews/pr-{number}/` via `.github/workflows/pr-preview.yml`, which posts the URL as a PR comment
