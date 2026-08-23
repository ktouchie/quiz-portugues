# European Portuguese Quiz — Android App v1 Specification

Status: draft, pending final owner sign-off before GitHub issues are filed.
Source: synthesized from four expert reviews (software engineering, Android/mobile-game UX,
language pedagogy, European Portuguese linguistics) of the existing web app, plus a decision
round with the product owner. See "Decision log" at the bottom for how each call was made.

**Revision note:** the app was originally speced as a React Native (Expo) app targeting Android +
iOS. The product owner pivoted to a fully native Android app (Kotlin + Jetpack Compose), Android
only, no iOS. This revision reflects that pivot throughout.

## 1. Goals

- Port the concept of the existing static web quiz app (`/`) to a native Android app, reusing what
  the software-engineering review found genuinely portable — the SM-2 spaced-repetition algorithm,
  gamification rules, and the JSON content itself — while rebuilding the UI natively.
- Ship a real, finished v1 rather than a partial port of all seven modules — depth over breadth.
- The product owner develops and validates this app entirely from their phone, with no desktop/
  laptop in the loop. Every push to `main` must produce an installable APK reachable from the
  phone with nothing more than a browser — this drives the CI design in §12.

## 2. Non-goals for v1

- **No iOS.** A physical iPhone requires an Apple Developer Program account for any installable
  build (TestFlight or ad-hoc) — out of scope entirely, not deferred.
- No backend, accounts, or cross-device sync. Progress is local to the device (same trust model
  as today's `localStorage`, just backed by a real embedded database).
- No monetization (no ads, no subscription, no IAP).
- No push/local notifications.
- No social or competitive features (leaderboards, friends, sharing).
- No listening/speaking/pronunciation practice.
- No modules beyond Verb Conjugation and Vocabulary (the other five — gender & plural,
  ser/estar/ficar, contractions, subjunctive, indirect speech — are backlog, not cut).
- No guided/gated CEFR curriculum path — module and content selection stays free-choice, as today.

## 3. Tech stack

- **Kotlin**, native Android, no cross-platform framework.
- **Jetpack Compose** (Material 3) for the entire UI layer.
- **Jetpack Navigation Compose** for screen navigation.
- **Room** (built on SQLite) for on-device persistence — see §6.
- **Gradle** build system — this is also what makes the CI pre-release pipeline in §12
  straightforward: a GitHub Actions runner with the Android SDK can build an installable APK
  directly with `./gradlew assembleDebug`, no third-party build-cloud service required.
- Android `Vibrator` / `HapticFeedbackConstants` APIs for haptics (no Expo/RN dependency).
- Minimum SDK: to be set when the project is scaffolded (#7.1) — target recent Android versions
  only, no legacy-device support requirement was raised.

## 4. Repository structure (monorepo)

The existing static site stays at the repo root, unchanged in its own deployment story (GitHub
Pages via the existing workflows). A new top-level `android/` directory holds the native Kotlin
app as a standard Gradle project.

```
/                          # existing static web app (unchanged deploy story)
  index.html, *.html, *.js, *.json, styles.css, ...
  tests/                   # existing web app tests (unchanged)
/android/                  # NEW — native Kotlin app (Gradle project)
  app/
    src/main/java/.../     # Kotlin source
      srs/                  # SM-2 algorithm, ported from srs.js (see §7)
      gamification/          # streak/milestone/goal logic, ported from gamification.js
      data/                  # Room entities, DAOs, repositories (see §6)
      content/                # loaders/normalizers for the bundled verbs.json / vocabulary.json
      ui/                     # Compose screens, per module
    src/main/assets/         # bundled verbs.json, vocabulary.json (see §6.3)
    src/test/                # JUnit unit tests (SRS, gamification, content loaders)
    src/androidTest/         # Compose UI tests
  build.gradle.kts, settings.gradle.kts
/docs/
  MOBILE_APP_SPEC.md        # this file
```

Unlike the original React Native plan, there is **no shared code package** between the web app and
the Android app — Kotlin cannot consume the web app's JavaScript/TypeScript, and there is no
cross-platform runtime bridging them. What *is* shared is the **content JSON** (`verbs.json`,
`vocabulary.json`, at the repo root) and a small set of **conventions** both apps must honor by
hand — most importantly, stable content-item IDs (§6.1). The SM-2 and gamification logic are
reimplemented natively in Kotlin, using `srs.js`/`gamification.js` as the behavioral reference —
see §7.

This is a structural addition, not a rewrite of the web app's behavior: `script.js`,
`vocabulary_quiz.js`, `srs.js`, and `gamification.js` are untouched by this work.

## 5. Content scope (v1)

Two modules only:

1. **Verb Conjugation** — all 26 verbs currently in `verbs.json` with a populated conjugation
   table (the ~25 additional entries that only carry `participios_passados` are out of scope for
   the quiz itself; see §14).
2. **Vocabulary** — all 31 categories / ~548 words in `vocabulary.json`.

No content re-authoring is required beyond the fixes already applied (see the "Content fixes
applied" section below) — the existing JSON is reused as-is, bundled into the Android app as
assets and parsed at runtime into the typed models described in §6.1.

## 6. Data model & persistence

### 6.1 Stable content IDs

Today's web app addresses items by pipe-delimited composite strings assembled at runtime
(`` `${verb}|||${tense}|||${personIdx}` ``). This is fragile (silently orphans SRS history if
content is reordered) and both apps now parse the same JSON independently, so the derivation rule
needs to be a documented, shared **convention** rather than shared code:

- Every quizzable item gets a **stable, deterministic ID** computed once at content-load time from
  its natural key (verb name + tense + person index; category + word for vocabulary), documented
  here so the Android Kotlin loader and (if the web app is ever updated to match) the web JS loader
  derive identical IDs independently.
- The Android app's Kotlin data classes mirror this: a `QuizItem` with a stable `id: String`,
  `module` type, prompt/answer fields, and module-specific metadata (tense, person, category,
  difficulty, etc.).

### 6.2 Room schema

```
srs_records(item_id TEXT PRIMARY KEY, module TEXT, repetitions INT, ease_factor REAL,
            interval_days INT, next_review_at INTEGER, last_quality INT, updated_at INTEGER)
best_scores(module TEXT PRIMARY KEY, best_correct_count INT)
streak_data(id INTEGER PRIMARY KEY CHECK (id = 1), current_streak INT, longest_streak INT,
            last_completed_date TEXT)
seen_milestones(milestone INTEGER PRIMARY KEY)
```

This directly replaces the `srs_verbs`/`srs_vocab`/`bestScore_*`/`streak_data`/`seen_milestones`
`localStorage` keys with real Room entities/DAOs, keyed by the stable `item_id` from §6.1 instead
of ad hoc strings. Room's schema versioning (`@Database(version = ...)`) is used from day one so a
future migration (e.g., adding cloud sync) doesn't require a painful retrofit.

### 6.3 Offline-first

`verbs.json` and `vocabulary.json` ship inside the APK under `app/src/main/assets/` (it's ~100KB
total — trivial). No network fetch is required to use the app.

## 7. SRS engine

Port `srs.js`'s `sm2()` function to Kotlin (`android/app/.../srs/`) — it's already pure and
side-effect-free, per the engineering review, so this is a direct line-for-line translation of the
algorithm, not a redesign. Behavior for v1 stays as today (quality 4 / correct-after-mistake 2 /
wrong 0) — **not** adopting the pedagogy review's suggested finer-grained quality scale (0/2/3/4
with latency/attempt-count input) or redefined "mastered" threshold (`repetitions ≥ 2 && interval
≥ 7 days` instead of `repetitions > 0`) for v1, to keep behavior identical to the web app users
already know. Both are flagged as strong v1.1 candidates — see §13.

Due-item prioritization (due items sorted before new items) carries over unchanged. Unit tests
should assert the same behavior as `tests/srs.test.js`, so the two implementations stay provably
in sync even though they don't share code.

## 8. Session design

- **Quick Practice** (default, primary CTA on each module's home screen): a capped session of
  10–15 questions. SRS-due items are pulled first; if fewer than the cap are due, the rest are
  filled with new/lower-priority items, interleaved rather than blocked by category/tense (per the
  pedagogy review, interleaving beats blocking for retention, and this is also what makes a capped
  session still cover the full 26-verb/548-word set over time — mastery isn't reached in one
  session, it's reached across many Quick Practice sessions as the SRS due-queue cycles through
  everything).
- **Advanced** (secondary entry point, e.g. a "Customize" button): recreates today's web setup
  screen — pick specific tenses/categories/difficulty, no session cap. This is where a learner who
  wants to deliberately drill "all preterite forms" or "all Comida vocabulary" goes.
- Session length is configurable in Advanced mode only; Quick Practice's cap is a fixed constant
  for v1 (tunable later, not user-facing).

## 9. Input model & UI

- **Verb Conjugation**: typed free-text input (preserves the production/recall skill this module
  is actually testing), with a **custom accent bar** above the keyboard (a Compose row of tap
  targets) offering one-tap insertion of `á é í ó ú â ê ô ã õ ç` — addresses the mobile-specific
  friction the game-dev review flagged (no EP accented characters on a default mobile keyboard
  layout). Answer comparison reuses the existing `.trim().toLowerCase().normalize('NFC')` logic
  from `quiz_base.js` as its behavioral reference, reimplemented in Kotlin.
- **Vocabulary**: tap/multiple-choice (4 options: 1 correct + 3 distractors drawn from the same
  category where possible, falling back to random same-module distractors). Recognition is a
  reasonable proxy for this module and removes typing friction entirely for the higher-volume,
  faster-paced module.
- Wrong-answer feedback keeps the current pattern: show the grammar hint / correct answer in place,
  item stays in the session pool (per `quiz_base.js`'s existing retry-in-pool behavior), user taps
  to continue.
- Results screen: time, accuracy %, top mistakes list, "Practice mistakes" retry button — same
  shape as today's, laid out for a phone screen.

## 10. Gamification

Reimplemented in Kotlin using `gamification.js` as the behavioral reference: streak (day-based,
resets on a missed day), milestone thresholds (10/25/50/100/250/500 mastered items) with a
one-time celebratory banner, and the home screen shows streak + mastered count exactly as
`index.html` does today.

**Game feel — minimal for v1** (per product decision): a haptic tick (Android `Vibrator`/
`HapticFeedbackConstants`, correct vs. wrong distinguishable) plus a basic scale/color transition
on the feedback state. No sound design, no XP/combo meter, no confetti — those are backlog (§13).

## 11. Theming

Port the dark/light theme from `styles.css`'s CSS custom properties into a Compose Material 3
theme (`ColorScheme`) with equivalent token values, respecting the OS-level light/dark setting by
default (mirroring the current `initTheme` behavior from `common.js`).

## 12. Testing & CI

This section is the one that matters most for the product owner's phone-only workflow: **every
push to `main` must end with an installable APK reachable from a phone browser, with no desktop
step in between.**

### 12.1 Tests

- `android/app/src/test/`: JUnit unit tests for the ported SM-2 algorithm and gamification logic
  (mirroring `tests/srs.test.js`'s cases for behavioral parity with the web app), plus tests for
  the content loaders/normalizers (stable ID generation from §6.1).
- `android/app/src/androidTest/`: Compose UI tests for the Quick Practice happy path, the
  wrong-answer-stays-in-pool behavior, and the results screen.

### 12.2 CI pipeline (new workflow, e.g. `.github/workflows/android-ci.yml`)

Runs independently of the existing web-app `ci.yml` (different toolchain — JVM/Android SDK vs.
Node), triggered **on every push to `main`**:

1. **Lint** — `./gradlew lint` (and ktlint/detekt if adopted).
2. **Unit tests** — `./gradlew test`.
3. **Instrumented/Compose UI tests** — `./gradlew connectedAndroidTest` (or Robolectric-based
   equivalents if instrumented tests prove too slow/flaky for CI).
4. **Final step, only if 1–3 all pass: build and publish a pre-release APK.**
   - Build a debug-signed APK: `./gradlew assembleDebug` (no release keystore or secrets required
     — debug signing is sufficient for sideloading on a personal device with "install from unknown
     sources" enabled; this is deliberately separate from the signed release build in §13/epic
     "Release prep", which is for eventual Play Store submission).
   - Publish the APK as the asset on a **rolling GitHub Release** (fixed tag, e.g.
     `android-preview-latest`, marked as a pre-release, asset overwritten each run — via an action
     like `softprops/action-gh-release` or `ncipollo/release-action` with `GITHUB_TOKEN`, no extra
     secrets needed). This gives a **stable, bookmarkable URL** the product owner can open on their
     phone at any time to download and install the newest build — no need to dig through Actions
     run history.
   - The workflow run summary also links directly to the release for convenience.

This means: push to `main` → CI lints, tests, and (if green) builds → a fresh APK is one tap away
on the product owner's phone within a few minutes, every time.

## 13. Out of scope for v1 — backlog

Recorded here so they aren't lost, not because they're unimportant:

- iOS, if ever revisited — would need a decision on native Swift vs. a cross-platform rewrite,
  since the Android app is not built on a cross-platform framework.
- Remaining 5 quiz modules (gender & plural, ser/estar/ficar, contractions, subjunctive, indirect
  speech), ported using the same Kotlin/Compose patterns established by verbs/vocabulary.
- Local notifications (daily due-item digest + streak-at-risk reminder) — flagged by the game-dev
  review as the highest-leverage retention feature not in v1.
- Full "game feel" polish: sound design, in-session combo/XP display, richer animations.
- Cloud backup / accounts / cross-device sync, using the Room schema-versioning from §6.2 as the
  migration starting point.
- Monetization (revisit once there's usage data).
- Finer-grained SM-2 quality scoring and a stricter "mastered" definition (§7).
- Listening/speaking practice, guided CEFR curriculum path, diagnostic placement test.
- Home-screen widget (streak + due count).
- Signed release build + Google Play Store submission (separate from the CI pre-release APK in
  §12.2 — tracked under the "Release prep" epic).

## 14. Known content gaps (not v1-blocking, tracked for future content work)

From the language-accuracy review, out of scope for the Verb/Vocabulary v1 app but worth tracking:

- No tu/você/o senhor formality-register content.
- No clitic pronoun placement content (proclisis/mesoclisis/enclisis).
- ~25 `verbs.json` entries carry only `participios_passados` with no full conjugation table or
  `difficulty` tag — decide whether to complete them, drop them, or spin them into a dedicated
  "irregular participles" mini-module (relevant once that content is prioritized, not for v1's
  Verb Conjugation module which only surfaces the 26 fully-tagged verbs).
- Gendered vocabulary pairs (occupations, family terms) are inconsistently paired in
  `vocabulary.json`.

## Content fixes already applied (pre-mobile-port cleanup)

Committed ahead of this spec, since they affect data both apps will share:

- `verbs.json`: `estar`'s pretérito mais-que-perfeito wrongly reused `ser`'s participle
  ("tinha sido" → "tinha estado"); `cultivar`/`limpar` had incorrect `participios_passados` forms.
- `gender_quiz.js`: the plural-only quiz question was hardcoded as "plural masculino" even for
  inherently feminine invariable nouns like "mão" — relabeled to gender-neutral "plural" (a code
  fix, not a data fix, since the item's `masculine` field doubles as the generic prompt slot).
- `vocabulary.json`: EP spelling fix (dezenove → dezanove) and several typos/mistranslations
  (cugnada/cugnado → cunhada/cunhado, madraste/padraste → madrasta/padrasto, veranda → varanda,
  "bom aproveito" → "bom apetite"), and a mistranslation (madrugada is pre-dawn, not dusk).

## Decision log

| Decision | Choice | Source |
|---|---|---|
| Platform | Native Android (Kotlin + Jetpack Compose) | Product owner (pivoted from React Native/Expo) |
| Target OS | Android only — no iOS | Product owner |
| v1 module scope | Verb Conjugation + Vocabulary | Product owner |
| Backend/sync | None — local only (Room) | Product owner |
| Input model | Hybrid: typed+accent bar for verbs, multiple-choice for vocabulary | Product owner, synthesizing pedagogy + game-dev reviews |
| Monetization | None | Product owner |
| Notifications | None in v1 | Product owner |
| Repo structure | Monorepo, `android/` directory, no shared code package (content JSON + conventions only) | Product owner + engineering review, revised for the Kotlin pivot |
| Content bugs found in review | Fixed immediately | Product owner |
| Game-feel polish | Minimal (haptics + basic animation) | Product owner |
| Session design | Quick Practice (capped, SRS-first, interleaved) default; Advanced full setup available | Product owner, synthesizing pedagogy + game-dev reviews |
| CI pre-release builds | Every push to `main` builds a debug-signed APK as the final CI step, published to a rolling GitHub Release for phone-only installation | Product owner (develops entirely from phone, no desktop in the loop) |
