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

No npm dependencies to install.

## Architecture

**Three independent quiz modules**, each with its own HTML + JS file:

- `verb_quiz.html` + `script.js` — Verb conjugation quiz; loads `verbs.json`
- `vocabulary_quiz.html` + `vocabulary_quiz.js` — Vocabulary translation quiz; loads `vocabulary.json`
- `gender_quiz.html` + `gender_quiz.js` — Gender & plural quiz; loads `gender_quiz.json`
- `index.html` — Home page linking to all three quizzes; fetches `version.txt` to display version

**Shared:**
- `common.js` — Exported utilities: `initTheme`, `loadVersion`, `startTimer`, `stopTimer`, `updateTimerDisplay`, `updateBestScore`, `addSelectAll`
- `styles.css` — Applies to all pages; uses CSS custom properties for dark/light theming

**Data files:**
- `verbs.json` — Structure: `{ verbName: { regular: bool, tense: [eu, tu, ele/ela/você, nós, eles/elas/vocês] } }` for 10 tenses + `participios_passados`; 51 verbs
- `vocabulary.json` — Structure: `{ category: { portuguese: "english" } }`; 31 categories, ~548 words
- `gender_quiz.json` — Structure: `{ category: [{ masculine, feminine, plural }] }`; 4 categories, 53 words, 102 quiz items

**Best scores** are persisted per quiz in `localStorage` under keys `bestScore_verbs`, `bestScore_vocab`, `bestScore_gender`.

## Quiz mechanics (shared pattern)

All three quizzes follow the same pattern:
1. Setup screen — select tenses/categories (verb & vocab) or click straight through (gender)
2. Questions shown one at a time, randomly drawn from the remaining pool
3. Correct answer advances the counter; wrong answer decrements it (min 0)
4. A word/form is retired from the pool once answered correctly `REQUIRED_CORRECT` times (currently 1)
5. Score tracked live; top mistakes and best score shown on the result screen
6. Timer runs per question, pauses on feedback

## Working Conventions

- After any major update (new module, feature, data change, architecture change), update both `CLAUDE.md` and `README.md` to reflect the current state before committing.

## Deployment

- **Main branch** auto-deploys to GitHub Pages via `.github/workflows/version-bump.yml`, which also auto-bumps the patch version in `version.txt` (skipped if `version.txt` was changed manually in the same push)
- **PRs** get preview deployments at `previews/pr-{number}/` via `.github/workflows/pr-preview.yml`, which posts the URL as a PR comment
