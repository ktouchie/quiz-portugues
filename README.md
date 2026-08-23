# Quiz de Português

A browser-based European Portuguese practice app with seven quiz modules.

## Modules

### Conjugação de Verbos
Practice conjugating 26 verbs across 10 tenses plus compound past (*participios passados*):
presente, pretérito, imperfeito, condicional, pretérito mais-que-perfeito, perfeito composto, futuro, imperativo, conjuntivo, and infinitivo pessoal.

- **Difficulty filter** — choose Iniciante (regular verbs), Intermédio, Avançado, or all
- **Interleaved mode** — shuffles across tenses for better retention
- **Example sentences** — 16 high-frequency verbs show a contextual example sentence after a wrong answer
- Regular verbs highlighted in blue, irregular in purple

### Vocabulário
Translate ~548 words across 31 categories (animals, food, directions, emotions, occupations, and more) in either direction — English → Portuguese or Portuguese → English.

### Género e Plural
Practice the feminine singular and masculine plural forms of 53 nouns and adjectives covering common patterns:
- `-or` endings (ator → atriz / atores)
- `-ão` endings and their three plural patterns (-ões, -ães, -ãos)
- `-l` endings (fácil → fácil / fáceis, espanhol → espanhola / espanhóis)
- Irregular and invariable adjectives (bom → boa / bons, feliz → feliz / felizes)

### Ser / Estar / Ficar
Fill-in-the-blank sentences across four categories: professions and identity (ser), temporary states (estar), location of people vs. fixed places (ficar vs. estar), and change-of-state results (ficar). All examples use European Portuguese patterns — *estar a + infinitivo* for continuous actions, *ficar* for fixed locations.

### Contrações
Practice the full range of European Portuguese preposition contractions — *de/em/a/por* with definite/indefinite articles and with demonstratives (*este/esse/aquele*, *isso/aquilo*, *aqui/ali/aí*). Includes context sentences for each.

### Conjuntivo
Conjugate verbs in the correct conjuntivo form (present, imperfect, or personal future) triggered by expressions of desire, emotion, doubt, necessity, and subordinating conjunctions (*embora*, *quando*, *até que*, *caso*, etc.).

### Discurso Indireto
Transform direct speech into indirect speech by producing the correct backshifted verb form: present → imperfect, *pretérito* → *mais-que-perfeito composto*, *vou fazer* → *ia fazer*, and EP-specific *estar a + inf* continuations.

## Features

- **Spaced repetition (SM-2)** — items are scheduled for review based on performance; due items appear first; quality is implicit (first-try correct = 4, correct after mistakes = 2, wrong = 0)
- **SRS manager** — dedicated page to view all recorded items, see next-review dates, and reset individual items or entire modules
- **Grammar hints** — wrong answers show a rule explanation for the relevant pattern
- **Hover tooltips** — hovering over key Portuguese words in verb and gender quizzes shows the English translation
- **Streak & milestones** — daily practice streak displayed on the home page; milestone banners at 10, 25, 50, 100, 250, 500 mastered items
- **Retry mistakes** — one-click session to re-practice only the items you got wrong
- **Best score** — personal best saved per module in your browser
- **Progress bar** and live score (corrects / errors) during the quiz
- **Accuracy & time** shown on the result screen
- **Dark / light theme** toggle, saved across sessions
- **Keyboard friendly** — Enter to submit or advance

## Running Locally

No build step required for the app itself. Serve the files with any HTTP server:

```bash
python3 -m http.server
# or
npx serve .
```

Then open `http://localhost:8000`.

For development (linting + tests):

```bash
npm install
npm run lint
npm test
```

## Deployment

Pushing to `main` triggers a GitHub Actions workflow that bumps the patch version in `version.txt` and deploys to GitHub Pages. Pull requests get an automatic preview deployment with a link posted as a PR comment.

## Android App (in progress)

A native Android app (Kotlin + Jetpack Compose) is under development in `android/`, starting with the Verb Conjugation and Vocabulary modules. It reuses the same `verbs.json`/`vocabulary.json` content as the web app and reimplements the SM-2 spaced-repetition and gamification logic natively. See `docs/MOBILE_APP_SPEC.md` for the full design and the repo's issues for progress.
