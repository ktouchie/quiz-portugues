# Quiz de Português

A browser-based European Portuguese practice app with three quiz modules.

## Modules

### Conjugação de Verbos
Practice conjugating 51 verbs across 10 tenses plus compound past (*participios passados*):
presente, pretérito, imperfeito, condicional, pretérito mais-que-perfeito, perfeito composto, futuro, imperativo, conjuntivo, and infinitivo pessoal.

- **Difficulty filter** — choose Iniciante (regular verbs), Intermédio, Avançado, or all
- **Interleaved mode** — shuffles across tenses for better retention
- **Example sentences** — 16 high-frequency verbs show a contextual example sentence after a wrong answer (presente and pretérito)
- Regular verbs highlighted in blue, irregular in purple

### Vocabulário
Translate ~548 words across 31 categories (animals, food, directions, emotions, occupations, and more) in either direction — English → Portuguese or Portuguese → English.

### Género e Plural
Practice the feminine singular and masculine plural forms of 53 nouns and adjectives covering common patterns:
- `-or` endings (ator → atriz / atores)
- `-ão` endings and their three plural patterns (-ões, -ães, -ãos)
- `-l` endings (fácil → fácil / fáceis, espanhol → espanhola / espanhóis)
- Irregular and invariable adjectives (bom → boa / bons, feliz → feliz / felizes)

Each form is asked independently at random until all have been answered correctly.

## Features

- **Spaced repetition (SM-2)** — items are scheduled for review based on performance; due items appear first
- **Confidence rating** — after each correct answer, rate Fácil / OK / Difícil to tune the next review date
- **Grammar hints** — wrong answers show a rule explanation for the relevant tense or gender pattern
- **Audio** — 🔊 button reads correct answers aloud using Portuguese TTS (where browser supports it)
- **Streak & milestones** — daily practice streak displayed on the home page; milestone banners at 10, 25, 50, 100, 250, 500 mastered items
- **Personal goal** — set a target (e.g. "100 items in 30 days") and track progress from the home page
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
