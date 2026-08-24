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
algorithm, not a redesign. Scoring stays as today (quality 4 / correct-after-mistake 2 / wrong 0),
and gamification's "mastered" count (home screen, milestones) is unchanged — `repetitions > 0`,
ported as-is from `getTotalMastered()` in `gamification.js`.

`srs/Production.kt` adds a **second, stricter, and separately-named** check —
`isReadyForTyping(record)` = `repetitions ≥ 3 && interval ≥ 6 days` — used only to gate an item's
input modality (§9), never gamification's mastered count or milestones. This was originally
considered and deferred in an earlier draft of this spec as a redefinition of "mastered"; it isn't
one — it's a new, independent concept with its own name, precisely to avoid colliding with the
existing (deliberately loose) mastered-count semantics that milestones already depend on. A wrong
answer on a typed item runs `sm2()`'s `quality < 3` branch, which resets `repetitions`/`interval` —
so demotion back to multiple-choice falls out of this check automatically, no separate logic.

Due-item prioritization (due items sorted before new items) carries over unchanged. Unit tests
should assert the same behavior as `tests/srs.test.js`, so the two implementations stay provably
in sync even though they don't share code.

## 8. Session design

- **Quick Practice** (default, primary CTA on each module's home screen): a capped session of
  10–15 questions, restricted to unlocked CEFR tiers (§9). SRS-due items are pulled first; if fewer
  than the cap are due, the rest are filled with new/lower-priority items — favoring the newest
  unlocked ("frontier") tier so it accumulates the review history needed to cross its own unlock
  threshold, falling back to any unlocked tier once the frontier is exhausted — interleaved rather
  than blocked by category/tense within that (per the pedagogy review, interleaving beats blocking
  for retention). This is also what makes a capped session still cover the full unlocked set over
  time — nothing is mastered in one session, it's reached across many Quick Practice sessions as
  the SRS due-queue cycles through everything reachable.
- **Advanced** (secondary entry point, e.g. a "Customize" button): recreates today's web setup
  screen — pick specific tenses/categories/difficulty, no session cap, **not** restricted to
  unlocked CEFR tiers (a learner who wants to deliberately drill a specific advanced tense or
  category can, even before it would organically unlock in Quick Practice). This is where a learner
  who wants to deliberately drill "all preterite forms" or "all Comida vocabulary" goes.
- Session length is configurable in Advanced mode only; Quick Practice's cap is a fixed constant
  for v1 (tunable later, not user-facing).

## 9. Input model & UI

Both content breadth and input difficulty ramp up automatically, per item, rather than being fixed
per module — replacing an earlier draft of this spec that gave Verb Conjugation typed-only input
(with a custom accent bar) and Vocabulary multiple-choice-only input as a permanent per-module
split. Product feedback after using the shipped v1 modules: start everything on multiple choice,
broaden which content is in play as the learner shows they've got the current set down, and only
ask for typed recall once an individual item is genuinely well-known — not as a blanket property of
"being a verb question."

**Content breadth — CEFR tiers.** Every verb, every conjugation *tense*, and every vocabulary
category carries a hand-assigned CEFR level (A1–C2), defined Android-side in `content/CefrTiers.kt`
— an enrichment layer over the existing content JSON, not a change to it, so
`verbs.json`/`vocabulary.json` and the web app's `difficulty` field/adaptive-difficulty filter are
untouched. A verb conjugation item's effective level is the *harder* of its verb's level and its
tense's level (`TENSE_CEFR_LEVEL`, e.g. presente = A1, futuro/condicional/conjuntivo = B1,
infinitivo pessoal = C1) — verb frequency and tense complexity are independent axes, so an A1 verb
like "falar" still has its conjuntivo/mais-que-perfeito forms gated behind B1/B2 rather than opening
up on day one just because the verb itself is elementary. (An earlier version of this only tagged
the verb, not the tense — a real bug caught after the first Quick Practice session surfaced
subjunctive/pluperfect forms of basic verbs to a brand-new learner.) `content/ContentProgression.kt`'s
`unlockedTiers()` opens tiers sequentially: A1 is always unlocked, and each next tier unlocks once
≥80% of the current tier's items have been reviewed correctly at least once (the same "seen" bar
gamification's mastered-count uses). An empty tier (no content assigned yet, e.g. vocabulary's
C1/C2 today) unlocks automatically rather than permanently blocking everything after it — the six
levels are a growth path for content the app doesn't fully populate yet, not a requirement that it
does.

**Input modality — typing readiness.** Independently of content breadth, each item that's actually
in play renders multiple-choice or typed based on `isReadyForTyping()` (§7) on that item's own SRS
record — a brand-new item, or one that hasn't yet racked up 3 correct reviews with a 6+ day
interval, is multiple-choice; once it crosses that bar, it starts appearing as typed; a wrong typed
answer demotes it back to multiple-choice automatically via the SM-2 reset. This applies uniformly
to both modules — Vocabulary items graduate to typed recall the same way Verb Conjugation items do.

- **Multiple-choice** (both modules): 4 options — 1 correct + 3 distractors, picked for genuine
  confusability rather than being obviously wrong (product feedback: the first version's distractors
  were too easy to eliminate by elimination alone).
  - **Verb Conjugation**: ranked hardest-first — the same verb's own form in a *different tense,
    same person* (shares the stem, differs only in the ending actually being tested; e.g. for
    "eu ___" (fazer, presente) = "faço", offering "fiz"/"fazia"/"farei" rather than "fazes"/"faz"),
    falling back to other persons of the same verb+tense, then finally other verbs' forms of the
    same tense/person (only reached when a verb+tense genuinely lacks enough distinct forms, e.g.
    imperativo has no "eu" form).
  - **Vocabulary**: ranked by `content/StringSimilarity.kt`'s Levenshtein-based similarity score
    against the correct item's Portuguese word, checked two ways — same-language look-alikes (e.g.
    "irmã" as a distractor for "irmão") and false-friend-style cross-language look-alikes (e.g.
    "constipation" as a distractor for "constipação", which actually means "a cold" in EP) — then
    sampled from the top-8 shortlist so the same word doesn't repeat identical distractors every
    attempt.
- **Typed** (both modules, once an item is typing-ready): free-text input relying on the device
  keyboard's own long-press accent picker (every stock Android/Gboard keyboard already offers
  `á é í ó ú â ê ô ã õ ç` this way) — no in-app accent bar. Answer comparison reuses the existing
  `.trim().toLowerCase().normalize('NFC')` logic from `quiz_base.js` as its behavioral reference,
  reimplemented in Kotlin (`content/AnswerMatching.kt`).
- Progress bar and counter (e.g. "4/12") reflect items *permanently cleared* (`correctCount`), not
  which question is currently on screen — a wrong answer requeues the item rather than shrinking the
  pool, so a "current question number" metric both overstates progress before it's earned and never
  advances on a miss. Only reaches `totalQuestions`/`totalQuestions` once the last item is answered
  correctly, immediately before the session ends (fixes an earlier off-by-one where the bar looked
  "done" a question early).
- Wrong-answer feedback keeps the current pattern: show the grammar hint / correct answer in place,
  item stays in the session pool (per `quiz_base.js`'s existing retry-in-pool behavior), user taps
  to continue.
- Results screen: time, accuracy %, top mistakes list — laid out for a phone screen. The web app's
  "Praticar erros" retry button is **not yet implemented** here (§13) — both session ViewModels
  track per-item mistake counts for the mistakes list, but neither exposes a way to re-launch a
  session scoped to just those items yet.

## 10. Gamification

Reimplemented in Kotlin using `gamification.js` as the behavioral reference: streak (day-based,
resets on a missed day), milestone thresholds (10/25/50/100/250/500 mastered items) with a
one-time celebratory banner, and the home screen shows streak + mastered count exactly as
`index.html` does today.

**Game feel — minimal for v1** (per product decision): a haptic tick (Android `Vibrator`/
`HapticFeedbackConstants`, correct vs. wrong distinguishable) plus a basic scale/color transition
on the feedback state. No sound design, no XP/combo meter, no confetti — those are backlog (§13).

## 11. Theming

**Superseded.** The theme originally ported the web app's `styles.css` tokens 1:1 into a Compose
Material 3 `ColorScheme`. After using that shipped v1 build, the product owner asked for a real
visual redesign: three mockup directions were drafted (a Claude Design canvas — Home, a
multiple-choice question, a typed question, and Results, in each direction), and **"Direction A —
Warm Encourager"** was chosen: a cream/toast palette, big soft-rounded cards, and a warm amber
gradient accent alongside the existing blue, aimed at a more encouraging, celebratory, low-pressure
feel than the original bare Material defaults.

- `ui/theme/Color.kt` — new light/dark token sets (`LightBg`/`DarkBg` etc.) plus a warm-gradient
  accent pair (`*WarmAccentStart`/`*WarmAccentEnd`) with no equivalent in the web app's palette.
  This is a **deliberate, permanent divergence** from `styles.css` — the Android app's visual
  identity is now its own, not required to track the web app's tokens going forward. `correct`/
  `incorrect` semantics and the blue accent's role as the primary-action color carry over
  unchanged; background, surface, border, text, and the warm accent do not.
- `ui/theme/Theme.kt` — large corner-radius `Shapes` (22-28dp, well above Material 3 defaults) and
  bolder headline/title/label typography, matching the mockup's shape language.
- `ui/common/`: shared warm-styled building blocks used across Home, both module homes, both
  session screens, and both results screens — `ModuleCard` (icon, due count, mastery progress bar,
  one tap target for the whole card), `StatChip`, `PromptCard`, `MultipleChoiceOptions` (lettered
  badge options, the correct one highlighted once answered), `TypedAnswerInput`, `WarmGradientButton`,
  `GradientProgressBar`, `AccuracyRing` (a custom-drawn donut on the results screens), and
  `MilestoneBanner`.
- Dark mode gets its own warm-dark palette (not just an inverted light palette) — a warm near-black
  background/surface rather than the previous cool `#0F1117`/`#1A1D27`, keeping the same amber
  accent since it already reads well on a dark ground.
- Dynamic color (Android 12+) is still deliberately not offered — the palette is a chosen brand
  identity now, not meant to shift with wallpaper.

## 12. Testing & CI

This section is the one that matters most for the product owner's phone-only workflow: **every
push to `main` must end with an installable APK reachable from a phone browser, with no desktop
step in between.**

### 12.1 Tests

- `android/app/src/test/`: JUnit unit tests for the ported SM-2 algorithm and gamification logic
  (mirroring `tests/srs.test.js`'s cases for behavioral parity with the web app), tests for the
  content loaders/normalizers (stable ID generation from §6.1), and tests for the mastery-gating
  system in §9 — typing readiness, tier-unlock thresholds, and a coverage guard asserting every
  verb/vocabulary category in the real content JSON has a CEFR tag.
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

- "Praticar erros" retry button on the results screens (§9) — re-launch a session scoped to just
  the current session's mistakes, mirroring the web app's retry-mistakes flow.

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
- Finer-grained SM-2 quality scoring (latency/attempt-count input to `sm2()`, beyond today's
  4/2/0) — the mastery-gated modality/tier system in §9 replaces the other half of this line item
  from an earlier draft (a stricter "mastered" definition), so only the quality-scale part remains
  open.
- Listening/speaking practice, diagnostic placement test.
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
| Input model | Mastery-gated per item: multiple-choice until an item is typing-ready (§9), then typed; no custom accent bar, relies on the device keyboard's own accent long-press | Product owner, after using the shipped v1 modules |
| Content progression | CEFR tiers (A1–C2), sequential unlock at 80% "seen" per tier, Android-only enrichment layer over the shared content JSON | Product owner |
| Visual direction | "Direction A — Warm Encourager" (cream palette, warm amber gradient accent, big soft-rounded cards), chosen from 3 mockup directions | Product owner, from a Claude Design canvas |
| Monetization | None | Product owner |
| Notifications | None in v1 | Product owner |
| Repo structure | Monorepo, `android/` directory, no shared code package (content JSON + conventions only) | Product owner + engineering review, revised for the Kotlin pivot |
| Content bugs found in review | Fixed immediately | Product owner |
| Game-feel polish | Minimal (haptics + basic animation) | Product owner |
| Session design | Quick Practice (capped, SRS-first, interleaved) default; Advanced full setup available | Product owner, synthesizing pedagogy + game-dev reviews |
| CI pre-release builds | Every push to `main` builds a debug-signed APK as the final CI step, published to a rolling GitHub Release for phone-only installation | Product owner (develops entirely from phone, no desktop in the loop) |
