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
- `index.html` + `home.js` — Home page: streak pill, mastered/streak stat chips, a card per module (due count and progress bar), SRS manager link
- `srs_manager.html` — SRS management page: view/reset records per item or per module

**Shared:**
- `i18n.js` — interface language (English by default, Portuguese optional): `STRINGS` (every UI text in both languages), `t(key, params)`, `appendTemplate` (slots elements into a translated sentence), `localized(item, field)` (picks `hint_en`/`rule_en` in English), `applyTranslations` (static `data-i18n` / `data-i18n-html` / `data-i18n-attr` markup), `initLanguage` (adds the EN/PT pill next to the theme toggle). Switching fires a `languagechange` event that `QuizBase`, the home page and the SRS manager re-render on. Category names have English versions (`CATEGORY_NAMES_EN`, `categoryName`, `categorySpan`; vocabulary ones mirrored in Android's `ui/i18n/CategoryNames.kt`), as do the bracketed notes in subjunctive triggers (`triggerText`); the Portuguese names stay the content keys. Only instructions and explanations are translated, never the Portuguese being learnt
- `common.js` — `initTheme`, `loadVersion`, `startTimer`, `stopTimer`, `resumeTimer`, `updateTimerDisplay`, `updateBestScore`, `addSelectAll`, `getCheckedValues` (ticked boxes, leaving out "Selecionar tudo")
- `quiz_base.js` — `QuizBase` class: shared lifecycle (init, startQuiz, startQuickPractice, nextQuestion, submitAnswer, endQuiz), SRS integration, progress bar, retry-mistakes; abstract methods: `fetchData`, `getSelectedItems`, `renderQuestion`, `getCorrectAnswer`, `formatMistake`, `getLabel`; optional hooks `getAllItems` (enables Quick Practice), `getItemLevel` (CEFR level, gates Quick Practice by unlocked tier) and `getOptions` (multiple choice until the item is typing-ready)
- `practice.js` — pure helpers shared with the Android app's behaviour: `maskAnswer` (blanks the answer in an example shown with a question), `buildQuickPracticePool` (12 items, due first), `buildGatedQuickPracticePool` (only unlocked CEFR tiers, newest tier first), `buildOptions(correct, pools)` (3 distinct distractors, best pool first), `stringSimilarity`, `shuffle`
- `cefr.js` — CEFR levels per verb, tense and category of every module (`verbItemLevel`, `vocabularyCategoryLevel`, `genderCategoryLevel`, `serEstarFicarCategoryLevel`, `contractionsCategoryLevel`, `subjunctiveCategoryLevel`; indirect speech is all `B1`) and `unlockedTiers` (80% rule) — same maps as Android's `CefrTiers.kt` (tests compare them)
- `config.js` — `PERSONS`, `TENSE_LABELS`, `STORAGE_KEYS` (verbs, vocab, gender, serEstarFicar, contractions, subjunctive, indirectSpeech, theme)
- `srs.js` — SM-2 spaced repetition: `loadSRSState`, `saveSRSState`, `getItemSRS`, `sm2`, `getDueItems`, `isReadyForTyping` (3 correct in a row + 6-day interval → typed instead of multiple choice)
- `gamification.js` — `loadStreak`, `updateStreak`, `getTotalMastered`, `checkMilestone`, `showMilestoneBanner`, `loadGoal`, `saveGoal`, `getGoalProgress`
- `grammar_hints.js` — `getVerbHint(tense, verb, third)`, `getGenderHint(category)`, each in the interface language
- `styles.css` — Applies to all pages; "Warm Encourager" tokens on `:root` / `[data-theme="dark"]`, same palette as the Android app

**Data files:**
- `verbs.json` — `{ verbName: { regular, difficulty, tense: [...5 forms...], exemplos: { presente: [...], pretérito: [...] } } }` — 26 conjugation verbs; `difficulty`: `"beginner" | "intermediate" | "advanced"`; `exemplos` on 16 high-frequency verbs
- `vocabulary.json` — `{ category: { portuguese: "english" } }`; 31 categories, ~548 words. Two different Portuguese words never share an English translation (a content test enforces it) — tell them apart with a trailing bracketed note, e.g. baixo `"short (height)"` / curto `"short (length)"`; the note is optional in typed English answers (`answerMatches` / Android `answersMatch`). The only agreed exception is sete e meia / dezanove e trinta ("seven thirty"): asked English → Portuguese, either is accepted and neither is offered as a wrong option for the other. A vocabulary key includes its English, so renaming a word restarts it: its old record is deleted when the home page or the vocabulary quiz loads (`pruneRecords` in `srs.js`; Android `SrsRepository.deleteRecordsNotIn` from the home screen)
- `gender_quiz.json` — `{ category: [{ masculine, feminine, plural, english }] }`; 4 categories, 53 words, 102 quiz items
- `ser_estar_ficar.json` — `{ category: [{ sentence, answer, hint, hint_en, english }] }`; 4 categories, 38 items
- `contractions.json` — `{ category: [{ parts: [prep, article], answer, example, english, hint, hint_en }] }`; 8 categories, 39 items
- `subjunctive_quiz.json` — `{ category: [{ prompt, answer, trigger, hint, hint_en, english }] }`; 6 categories, 38 items
- `indirect_speech.json` — `[{ direct, context, verb_direct, answer, rule, rule_en, indirect_full, english, hint, hint_en }]`; 20 items
- Every `hint` / `rule` (Portuguese) has an English `hint_en` / `rule_en` (a test enforces it)

**SRS state** is persisted per quiz in `localStorage` under keys `srs_verbs`, `srs_vocab`, `srs_gender`, `srs_ser_estar_ficar`, `srs_contractions`, `srs_subjunctive`, `srs_indirect_speech`.
**Best scores** are persisted under `bestScore_*` keys matching the module names.
**Gamification** keys: `streak_data`, `seen_milestones`. **Interface language**: `language` (`en` default, or `pt`).

## Quiz mechanics (shared pattern)

All quizzes share `QuizBase`:
1. Setup screen — select tenses/categories/difficulty; SRS due-count shown
2. Questions drawn randomly; SRS due items sorted first
3. Correct first try → SM-2 quality 4; correct after mistakes → quality 2; wrong → quality 0
4. Wrong answer → grammar hint + example sentence shown; item stays in pool
5. Score: `correctCount` / `errorCount` tracked independently; best score = max correctCount
6. Result screen: time, accuracy %, top mistakes, "Praticar erros" retry button
7. Streak updated on quiz completion; milestones checked

**Quick Practice** (every module): a "Prática Rápida" button on the setup screen starts a 12-item session from the CEFR tiers the learner has unlocked, due items first, then the newest tier — same rule as the Android app. The detailed setup (tenses/categories, difficulty, direction) is folded under "Avançado" and isn't CEFR-gated. Vocabulary's Quick Practice asks Portuguese → English.

**Multiple choice until typing-ready** (every module, every session): each question shows four lettered options until that item passes `isReadyForTyping`, then a text box; a wrong answer sends it back to options. After an answer the options stay on screen with the right one (and a wrong tap) marked. A missed item never comes straight back while others remain.

**Wrong options per module** (same rules as the Android app): verbs — same verb and person in other tenses, then other persons, then other verbs; vocabulary and gender — the most similar spellings; ser/estar/ficar — the same person of the other two verbs (`serEstarFicarVerbDistractors`); contractions — same preposition, then same article/demonstrative; subjunctive — the present indicative of the same verb and person (`indicativeForm`, needs `verbs.json`), then the verb's other answers; indirect speech — the unchanged verb of the original sentence.

**Verb quiz extras:** adaptive difficulty filter (beginner/intermediate/advanced), interleaved mode (Fisher-Yates shuffle), example sentences for 16 high-frequency verbs.

**SRS manager** (`srs_manager.html`): lists all recorded items per module with next-review date; individual or bulk reset.

## Tooling

- **ESLint**: flat config (`eslint.config.js`), ES2022 modules, browser globals
- **Vitest**: jsdom environment, tests in `tests/`
- **lefthook**: pre-commit runs `npm run lint` + `npm test` via `~/.local/bin/npm`
- **CI**: `.github/workflows/ci.yml` runs lint + test on push/PR to main

## Android App (`android/`)

A native Android app (Kotlin + Jetpack Compose, no cross-platform framework). The website is the
web version of the app — same look and features — so every PR ships the Android change *and* its
web equivalent, with tests for both (rollout tracked in epic #59). All seven modules exist on both
apps. Release to the Google Play Store is deferred until the full app is built and tested through
several rounds from the phone (epic #12). Full design in `docs/MOBILE_APP_SPEC.md`; implementation
tracked via the `mobile-app` label on GitHub issues.

- Standard Gradle project: `android/app/src/main/java/com/ktouchie/quizportugues/`, with `srs/`,
  `gamification/`, `data/` (Room), `content/`, and `ui/` sub-packages as they're added.
- **No shared code with the web app** — Kotlin can't consume the web app's JS. `srs.js` and
  `gamification.js` are the *behavioral reference* for the Kotlin ports, not shared modules. The
  two things genuinely shared are the content JSON (`verbs.json`, `vocabulary.json` at the repo
  root, bundled into the APK as assets) and the stable-content-ID convention documented in the
  spec — both apps must derive identical IDs independently.
- Persistence: Room (SQLite) replacing the web app's `localStorage` keys — see spec §6.2 for the
  schema.
- **Gameplay is mastery-gated, per item, not fixed per module** (spec §9), the same on the web:
  every verb, tense and vocabulary category has a CEFR level (`content/CefrTiers.kt`, web
  `cefr.js`) that gates Quick Practice (`unlockedTiers()`: each tier opens once 80% of the previous
  one has been answered correctly at least once). Independently, each item is multiple choice until
  its own SRS record passes `isReadyForTyping()` (`srs/Production.kt`, web `srs.js`: 3 correct in a
  row and a 6-day interval), then typed. Distractors are ranked by confusability (verbs: same verb
  and person in other tenses first; vocabulary: `stringSimilarity`), de-duplicated by
  `pickDistractors` (web `buildOptions`). Typed input relies on the device keyboard's accents.
- **Visual theme: "Direction A — Warm Encourager"** (spec §11), on both apps: cream palette, warm
  amber gradient for the main calls to action alongside the blue accent, big soft-rounded cards.
  Android tokens are in `ui/theme/Color.kt`, building blocks in `ui/common/` (`ModuleCard`,
  `StatChip`, `PromptCard`, `AccuracyRing`, `MilestoneBanner`, `WarmGradientButton`,
  `GradientProgressBar`); the web mirrors them in `styles.css` (`:root` tokens, `.module-card`,
  `.stat-chip`, `#question` prompt card, `#accuracy` ring, lettered `button.option`s).
- **Interface language** (issue #71): `i18n/AppLanguage.kt` + `LanguageSettings` (SharedPreferences, English by default); `ui/i18n/Strings.kt` holds every UI text in both languages (`EnglishStrings` / `PortugueseStrings`, same wording as `i18n.js`), provided through `LocalStrings` / `LocalAppLanguage` from `MainActivity`, with an English / Português switch as the Scaffold's bottom bar. Grammar hints are `LocalizedText(en, pt)`.
- Build: `./gradlew lint test` for CI-equivalent checks; `./gradlew assembleDebug` for an
  installable APK. Requires the Android SDK — not available in this sandbox, so changes here
  can't be build-verified locally; rely on careful review plus the Android CI workflow.

## Working Conventions

- After any major update (new module, feature, data change, architecture change), update both `CLAUDE.md` and `README.md` to reflect the current state before committing.

## Context and task management

GitHub Issues are the canonical source of truth for feature requirements,
UX decisions, implementation requirements, bugs, and acceptance criteria.

Do NOT rely on previous conversation history for project requirements.

Before implementing or modifying a feature:
1. Identify the relevant GitHub Issue(s).
2. Read the issue and its comments.
3. Read only the relevant source files needed for the current task.
4. Treat the issue as authoritative over previous conversation discussion.

When a requirement, UX decision, bug diagnosis, or implementation decision
is established during conversation and is likely to matter later:
- update the relevant GitHub Issue, or
- create a new issue if appropriate.

Do not repeatedly restate the entire project history in conversation.

When a task is complete:
- update the GitHub Issue with the final implementation state,
- record important decisions and remaining work,
- keep the issue concise and actionable.

If previous conversation context conflicts with the GitHub Issue,
stop and ask which should be authoritative.

## Session discipline

Keep the active context focused on the current task.

Do not reread unrelated issues, files, logs, or previous conversation
unless they are required for the current task.

Prefer retrieving information from GitHub Issues and the codebase rather
than relying on conversation history.

When the current task is complete, be prepared for the next task to begin
in a fresh context.

Do not put detailed project history into CLAUDE.md. CLAUDE.md contains rules and stable facts, not the evolving state of individual features. Put evolving state in GitHub Issues.

## Deployment

- **Main branch** auto-deploys to GitHub Pages via `.github/workflows/version-bump.yml`, which also auto-bumps the patch version in `version.txt` (skipped if `version.txt` was changed manually in the same push)
- **PRs** get preview deployments at `previews/pr-{number}/` via `.github/workflows/pr-preview.yml`, which posts the URL as a PR comment

## Versioning and Android releases

- **One version number for both apps**: `version.txt`. The website shows it in its footer; the
  Android build reads it for `versionName` and derives `versionCode` as
  `major * 1_000_000 + minor * 1_000 + patch` (so keep minor/patch below 1000).
- `.github/workflows/android-ci.yml`: lint, unit tests and a debug build on every PR to `main`.
  `version-bump.yml` calls it after bumping `version.txt` on `main`, publishing the APK to the
  rolling **"Latest debug build"** pre-release (`latest-debug` tag).
- **Permanent releases**: push a `vX.Y.Z` tag on a commit whose `version.txt` is `X.Y.Z`; CI publishes
  "Quiz Português vX.Y.Z" with its APK (fails if the tag and `version.txt` disagree).
- Every APK is signed with the committed `android/app/debug.keystore`, so any build installs over any
  other. Not the Play Store — that's deferred (epic #12).
