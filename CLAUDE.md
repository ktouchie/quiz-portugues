# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A static client-side European Portuguese language learning application with seven quiz modules plus an SRS manager. No build process — files are served directly from the repository.

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

**Seven independent quiz modules**, each with its own HTML + JS file:

- `verb_quiz.html` + `script.js` — Verb conjugation; loads `verbs.json`
- `vocabulary_quiz.html` + `vocabulary_quiz.js` — Vocabulary translation; loads `vocabulary.json`
- `gender_quiz.html` + `gender_quiz.js` — Gender & plural; loads `gender_quiz.json`
- `ser_estar_ficar_quiz.html` + `ser_estar_ficar_quiz.js` — Fill-blank: ser/estar/ficar; loads `ser_estar_ficar.json`
- `contractions_quiz.html` + `contractions_quiz.js` — Preposition contractions; loads `contractions.json`
- `subjunctive_quiz.html` + `subjunctive_quiz.js` — Conjuntivo conjugation; loads `subjunctive_quiz.json`
- `indirect_speech_quiz.html` + `indirect_speech_quiz.js` — Discurso indireto verb forms; loads `indirect_speech.json`
- `index.html` — Home page with streak, mastered count, links to all modules and SRS manager
- `srs_manager.html` — SRS management page: view/reset records per item or per module

**Shared:**
- `common.js` — `initTheme`, `loadVersion`, `startTimer`, `stopTimer`, `resumeTimer`, `updateTimerDisplay`, `updateBestScore`, `addSelectAll`
- `quiz_base.js` — `QuizBase` class: shared lifecycle (init, startQuiz, nextQuestion, submitAnswer, endQuiz), SRS integration, progress bar, retry-mistakes; abstract methods: `fetchData`, `getSelectedItems`, `renderQuestion`, `getCorrectAnswer`, `formatMistake`, `getLabel`
- `config.js` — `PERSONS`, `TENSE_LABELS`, `STORAGE_KEYS` (verbs, vocab, gender, serEstarFicar, contractions, subjunctive, indirectSpeech, theme)
- `srs.js` — SM-2 spaced repetition: `loadSRSState`, `saveSRSState`, `getItemSRS`, `sm2`, `getDueItems`
- `gamification.js` — `loadStreak`, `updateStreak`, `getTotalMastered`, `checkMilestone`, `showMilestoneBanner`, `loadGoal`, `saveGoal`, `getGoalProgress`
- `grammar_hints.js` — `getVerbHint(tense, verb, third)`, `getGenderHint(category)`
- `styles.css` — Applies to all pages; uses CSS custom properties for dark/light theming

**Data files:**
- `verbs.json` — `{ verbName: { regular, difficulty, tense: [...5 forms...], exemplos: { presente: [...], pretérito: [...] } } }` — 26 conjugation verbs; `difficulty`: `"beginner" | "intermediate" | "advanced"`; `exemplos` on 16 high-frequency verbs
- `vocabulary.json` — `{ category: { portuguese: "english" } }`; 31 categories, ~548 words
- `gender_quiz.json` — `{ category: [{ masculine, feminine, plural, english }] }`; 4 categories, 53 words, 102 quiz items
- `ser_estar_ficar.json` — `{ category: [{ sentence, answer, hint, english }] }`; 4 categories, 38 items
- `contractions.json` — `{ category: [{ parts: [prep, article], answer, example, english, hint }] }`; 8 categories, 39 items
- `subjunctive_quiz.json` — `{ category: [{ prompt, answer, trigger, hint, english }] }`; 6 categories, 38 items
- `indirect_speech.json` — `[{ direct, context, verb_direct, answer, rule, indirect_full, english, hint }]`; 20 items

**SRS state** is persisted per quiz in `localStorage` under keys `srs_verbs`, `srs_vocab`, `srs_gender`, `srs_ser_estar_ficar`, `srs_contractions`, `srs_subjunctive`, `srs_indirect_speech`.
**Best scores** are persisted under `bestScore_*` keys matching the module names.
**Gamification** keys: `streak_data`, `seen_milestones`.

## Quiz mechanics (shared pattern)

All quizzes share `QuizBase`:
1. Setup screen — select tenses/categories/difficulty; SRS due-count shown
2. Questions drawn randomly; SRS due items sorted first
3. Correct first try → SM-2 quality 4; correct after mistakes → quality 2; wrong → quality 0
4. Wrong answer → grammar hint + example sentence shown; item stays in pool
5. Score: `correctCount` / `errorCount` tracked independently; best score = max correctCount
6. Result screen: time, accuracy %, top mistakes, "Praticar erros" retry button
7. Streak updated on quiz completion; milestones checked

**Verb quiz extras:** adaptive difficulty filter (beginner/intermediate/advanced), interleaved mode (Fisher-Yates shuffle), example sentences for 16 high-frequency verbs.

**SRS manager** (`srs_manager.html`): lists all recorded items per module with next-review date; individual or bulk reset.

## Tooling

- **ESLint**: flat config (`eslint.config.js`), ES2022 modules, browser globals
- **Vitest**: jsdom environment, tests in `tests/`
- **lefthook**: pre-commit runs `npm run lint` + `npm test` via `~/.local/bin/npm`
- **CI**: `.github/workflows/ci.yml` runs lint + test on push/PR to main

## Android App (`android/`)

A native Android app (Kotlin + Jetpack Compose, no cross-platform framework) is being built
alongside the web app, targeting all seven web quiz modules. Verb Conjugation and Vocabulary
shipped first to prove out the architecture (mastery-gated CEFR tiers, MC-until-typing-ready input,
the warm theme, shared UI components); the remaining five — Gender & Plural, Ser/Estar/Ficar,
Contractions, Subjunctive, Indirect Speech (GitHub epics #51–#55) — reuse that same architecture.
Release to the Google Play Store is explicitly deferred until the full app is built and tested
through several rounds from the phone — release-prep work (signed build, store listing, icon/splash
assets, epic #12) is intentionally parked until then. Full design in `docs/MOBILE_APP_SPEC.md`;
implementation tracked via the `mobile-app` label on GitHub issues.

- Standard Gradle project: `android/app/src/main/java/com/ktouchie/quizportugues/`, with `srs/`,
  `gamification/`, `data/` (Room), `content/`, and `ui/` sub-packages as they're added.
- **No shared code with the web app** — Kotlin can't consume the web app's JS. `srs.js` and
  `gamification.js` are the *behavioral reference* for the Kotlin ports, not shared modules. The
  two things genuinely shared are the content JSON (`verbs.json`, `vocabulary.json`,
  `gender_quiz.json` at the repo root, bundled into the APK as assets) and the stable-content-ID
  convention documented in the spec — both apps must derive identical IDs independently.
- Persistence: Room (SQLite) replacing the web app's `localStorage` keys — see spec §6.2 for the
  schema.
- **Gameplay is mastery-gated, per item, not fixed per module** (spec §9): every verb and
  vocabulary category carries an Android-only CEFR tag (`content/CefrTiers.kt`) that gates content
  breadth (`content/ContentProgression.kt`'s `unlockedTiers()`, sequential unlock at 80% "seen" per
  tier); independently, each item renders multiple-choice or typed based on its own SRS record
  crossing `srs/Production.kt`'s `isReadyForTyping()` bar — deliberately a different, stricter
  check than gamification's loose "mastered" count. No custom accent-bar keyboard; typed input
  relies on the device keyboard's own accent long-press.
- **Visual theme intentionally diverges from the web app** (spec §11): "Direction A — Warm
  Encourager" (cream palette, warm amber gradient accent alongside the existing blue, big
  soft-rounded cards), chosen by the product owner from 3 mockup directions drafted as a Claude
  Design canvas. `ui/theme/Color.kt`'s tokens are no longer a 1:1 port of `styles.css`; shared
  warm-styled building blocks live in `ui/common/` (`ModuleCard`, `StatChip`, `PromptCard`,
  `AccuracyRing`, `MilestoneBanner`, `WarmGradientButton`, `GradientProgressBar`).
- Build: `./gradlew lint test` for CI-equivalent checks; `./gradlew assembleDebug` for an
  installable APK. Requires the Android SDK — not available in this sandbox, so changes here
  can't be build-verified locally; rely on careful review plus the Android CI workflow.

## Working Conventions

- After any major update (new module, feature, data change, architecture change), update both `CLAUDE.md` and `README.md` to reflect the current state before committing.

## Deployment

- **Main branch** auto-deploys to GitHub Pages via `.github/workflows/version-bump.yml`, which also auto-bumps the patch version in `version.txt` (skipped if `version.txt` was changed manually in the same push)
- **PRs** get preview deployments at `previews/pr-{number}/` via `.github/workflows/pr-preview.yml`, which posts the URL as a PR comment
