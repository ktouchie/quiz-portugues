# Quiz de Português

A browser-based European Portuguese practice app with three quiz modules.

## Modules

### Conjugação de Verbos
Practice conjugating 51 verbs across 10 tenses plus compound past (*participios passados*):
presente, pretérito, imperfeito, condicional, pretérito mais-que-perfeito, perfeito composto, futuro, imperativo, conjuntivo, and infinitivo pessoal.

Select any combination of tenses to practice. Regular verbs are highlighted in blue, irregular in purple.

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

- **Spaced repetition**: wrong answers re-enter the pool; a word is retired only after a correct answer
- **Best score**: your personal best is saved per module in your browser and shown after each session
- **Progress bar** and live score during the quiz
- **Top mistakes** summary at the end
- **Dark / light theme** toggle, saved across sessions
- **Keyboard friendly**: Enter to submit or advance, Tab between fields

## Running Locally

No build step or dependencies required. Serve the files with any HTTP server:

```bash
python3 -m http.server
# or
npx serve .
```

Then open `http://localhost:8000`.

## Deployment

Pushing to `main` triggers a GitHub Actions workflow that bumps the patch version in `version.txt` and deploys to GitHub Pages. Pull requests get an automatic preview deployment with a link posted as a PR comment.
