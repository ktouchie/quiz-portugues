# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A static client-side Portuguese language learning application with two quiz modules: verb conjugation and vocabulary translation. No build process — files are served directly.

## Running Locally

Serve the static files with any HTTP server, e.g.:
```
npx serve .
# or
python3 -m http.server
```

No npm dependencies to install.

## Architecture

**Two independent quiz modules**, each with its own HTML + JS file:

- `verb_quiz.html` + `script.js` — Verb conjugation quiz; loads `verbs.json`
- `vocabulary_quiz.html` + `vocabulary_quiz.js` — Vocabulary translation quiz; loads `vocabulary.json`
- `index.html` — Home page linking to both quizzes; fetches `version.txt` to display version

**Data files:**
- `verbs.json` — Structure: `{ verbName: { tense: [person1..person5] } }` for 10+ tenses
- `vocabulary.json` — Structure: `{ category: { portuguese: "english" } }` for multiple categories

**Shared:** `styles.css` applies to all pages.

## Deployment

- **Main branch** auto-deploys to GitHub Pages via `version-bump.yml` (also bumps patch version in `version.txt`)
- **PRs** get preview deployments at `previews/pr-{number}/` via `pr-preview.yml`, which posts the URL as a PR comment
- The `version-bump.yml` workflow triggers on pushes to `master` — note the main branch is actually `main`
