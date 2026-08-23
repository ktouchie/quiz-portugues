# European Portuguese Quiz — Mobile App v1 Specification

Status: draft, pending final owner sign-off before GitHub issues are filed.
Source: synthesized from four expert reviews (software engineering, Android/mobile-game UX,
language pedagogy, European Portuguese linguistics) of the existing web app, plus a decision
round with the product owner. See "Decision log" at the bottom for how each call was made.

## 1. Goals

- Port the concept of the existing static web quiz app (`/`) to a native-feeling mobile app,
  reusing the parts of the existing system that are genuinely portable (the SM-2 spaced-repetition
  engine, gamification rules, and the JSON content itself) while rebuilding the UI layer for touch.
- Ship a real, finished v1 rather than a partial port of all seven modules — depth over breadth.
- Keep the web app and mobile app in sync going forward: shared logic and content live in one
  place and both clients consume it.

## 2. Non-goals for v1

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

- **React Native** via **Expo** (managed workflow, EAS Build for both Android and iOS from one
  codebase). Expo is chosen over bare RN because there's no native-module requirement identified
  yet (no custom hardware access beyond haptics/vibration, which Expo covers), and EAS removes the
  need to maintain Xcode/Android Studio build config directly.
- **TypeScript** throughout the shared core and the app, to catch the composite-key/data-shape
  problems the engineering review flagged in the current plain-JS system.
- **expo-sqlite** for on-device persistence (see §6) rather than AsyncStorage, so item records have
  real primary keys and queryable structure instead of flat JSON blobs.
- **Navigation**: `react-navigation` (native-stack).
- Target both **Android and iOS** from launch (per product decision).

## 4. Repository structure (monorepo)

The existing static site stays at the repo root, unchanged in its own deployment story (GitHub
Pages via the existing workflows). A new top-level `mobile/` app is added, and the currently
web-only shared logic is extracted into a platform-agnostic package both sides import.

```
/                          # existing static web app (unchanged deploy story)
  index.html, *.html, *.js, *.json, styles.css, ...
  tests/                   # existing web app tests (unchanged)
/packages/core/            # NEW — platform-agnostic, no DOM/browser API dependencies
  src/
    srs.ts                 # ported from srs.js — SM-2 algorithm, pure functions
    gamification.ts        # ported from gamification.js — streak/milestone/goal logic
    content/
      verbs.ts              # typed loader + normalizer for verbs.json
      vocabulary.ts          # typed loader + normalizer for vocabulary.json
      types.ts               # shared content item types (see §6.1)
  test/                    # vitest unit tests for the above (ported from tests/srs.test.js etc.)
  package.json
/mobile/                   # NEW — Expo React Native app
  app/                     # screens (see §8-9)
  src/
    db/                     # expo-sqlite schema + repositories (see §6.2)
    components/
    theme/                  # ported from styles.css custom properties (see §11)
  package.json              # depends on @quiz-portugues/core via workspace reference
/docs/
  MOBILE_APP_SPEC.md        # this file
package.json                 # root — npm/pnpm workspaces covering packages/core and mobile
```

Content JSON (`verbs.json`, `vocabulary.json`) stays at the repo root as the single source of
truth; `packages/core`'s content loaders read/normalize it, and both the web app and the Expo app
bundle it at build time (no runtime fetch needed on mobile — see §6.3).

This is a structural refactor, not a rewrite of the web app's behavior: `script.js` and
`vocabulary_quiz.js` continue to work as before during the transition, and can be migrated to
import from `packages/core` incrementally rather than as a single risky cutover.

## 5. Content scope (v1)

Two modules only:

1. **Verb Conjugation** — all 26 verbs currently in `verbs.json` with a populated conjugation
   table (the ~25 additional entries that only carry `participios_passados` are out of scope for
   the quiz itself; see §14).
2. **Vocabulary** — all 31 categories / ~548 words in `vocabulary.json`.

No content re-authoring is required beyond the fixes already applied (see the "Content fixes
applied" section below) — the existing JSON is reused as-is, just re-shaped into the typed schema
in §6.1.

## 6. Data model & persistence

### 6.1 Stable content IDs

Today's system addresses items by pipe-delimited composite strings assembled at runtime
(`` `${verb}|||${tense}|||${personIdx}` ``). This is fragile (silently orphans SRS history if
content is reordered) and doesn't translate cleanly to a typed system. For v1:

- Every quizzable item gets a **stable, deterministic ID** computed once at content-load time from
  its natural key (verb name + tense + person index; category + word for vocabulary) and cached —
  functionally similar to today's composite key, but treated as an opaque typed `ItemId`, not
  parsed apart at runtime.
- `packages/core/src/content/types.ts` defines a common envelope:
  ```ts
  interface QuizItem {
    id: ItemId;
    module: 'verbs' | 'vocabulary';
    prompt: string;
    answer: string;
    // module-specific metadata (tense, person, category, difficulty, etc.)
  }
  ```

### 6.2 SQLite schema

```
srs_records(item_id TEXT PRIMARY KEY, module TEXT, repetitions INT, ease_factor REAL,
            interval_days INT, next_review_at INTEGER, last_quality INT, updated_at INTEGER)
best_scores(module TEXT PRIMARY KEY, best_correct_count INT)
streak_data(id INTEGER PRIMARY KEY CHECK (id = 1), current_streak INT, longest_streak INT,
            last_completed_date TEXT)
seen_milestones(module TEXT, milestone INT, PRIMARY KEY (module, milestone))
```

This directly replaces the `srs_verbs`/`srs_vocab`/`bestScore_*`/`streak_data`/`seen_milestones`
`localStorage` keys with real tables, keyed by the stable `item_id` from §6.1 instead of ad hoc
strings. Schema carries a `schema_version` row from day one so a future migration (e.g., adding
cloud sync) doesn't require a painful retrofit — this was flagged independently by the engineering
review as the single biggest structural gap in the current system.

### 6.3 Offline-first

All content JSON ships inside the app bundle (it's ~100KB total — trivial). No network fetch is
required to use the app; this also sidesteps the current web app's "one `alert()` on fetch failure"
error handling gap.

## 7. SRS engine

Port `srs.js`'s `sm2()` function to `packages/core/src/srs.ts` essentially line-for-line — it's
already pure and side-effect-free, per the engineering review. Behavior for v1 stays as today
(quality 4 / correct-after-mistake 2 / wrong 0) — **not** adopting the pedagogy review's suggested
finer-grained quality scale (0/2/3/4 with latency/attempt-count input) or redefined "mastered"
threshold (`repetitions ≥ 2 && interval ≥ 7 days` instead of `repetitions > 0`) for v1, to keep the
port low-risk and behaviorally identical to the web app users already know. Both are flagged as
strong v1.1 candidates — see §13.

Due-item prioritization (due items sorted before new items) carries over unchanged.

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
  is actually testing), with a **custom accent bar** above the keyboard offering one-tap insertion
  of `á é í ó ú â ê ô ã õ ç` — addresses the mobile-specific friction the game-dev review flagged
  (no EP accented characters on a default mobile keyboard layout). Answer comparison reuses the
  existing `.trim().toLowerCase().normalize('NFC')` logic from `quiz_base.js`.
- **Vocabulary**: tap/multiple-choice (4 options: 1 correct + 3 distractors drawn from the same
  category where possible, falling back to random same-module distractors). Recognition is a
  reasonable proxy for this module and removes typing friction entirely for the higher-volume,
  faster-paced module.
- Wrong-answer feedback keeps the current pattern: show the grammar hint / correct answer in place,
  item stays in the session pool (per `quiz_base.js`'s existing retry-in-pool behavior), user taps
  to continue rather than pressing Enter.
- Results screen: time, accuracy %, top mistakes list, "Practice mistakes" retry button — same
  shape as today's, laid out for a phone screen.

## 10. Gamification

Ported as-is from `gamification.js`: streak (day-based, resets on a missed day), milestone
thresholds (10/25/50/100/250/500 mastered items) with a one-time celebratory banner, and the home
screen shows streak + mastered count exactly as `index.html` does today.

**Game feel — minimal for v1** (per product decision): a haptic tick (Expo Haptics,
`impactAsync(Light)` on correct, a distinct pattern on wrong) plus a basic scale/color
transition on the feedback state. No sound design, no XP/combo meter, no confetti — those are
backlog (§13).

## 11. Theming

Port the dark/light theme from `styles.css`'s CSS custom properties into a small RN theme object
(`mobile/src/theme/`) with the same token names/values, respecting the OS-level light/dark setting
by default (mirroring the current `initTheme` behavior from `common.js`).

## 12. Testing & CI

- `packages/core` gets its own Vitest suite (ported from `tests/srs.test.js`, plus new coverage for
  `gamification.ts` and the content normalizers — the engineering review flagged
  `gamification.js` as currently untested despite being business-logic-dense).
- `mobile/` gets component/screen tests via `@testing-library/react-native` for the session flow
  (Quick Practice happy path, wrong-answer-stays-in-pool, results screen) and a smoke test that the
  app boots and reaches the home screen.
- CI: extend `.github/workflows/ci.yml` (or add a sibling workflow) to run `packages/core` and
  `mobile` test suites alongside the existing web app lint/test job. EAS builds are triggered
  manually / on release tags for v1, not on every push.

## 13. Out of scope for v1 — backlog

Recorded here so they aren't lost, not because they're unimportant:

- Remaining 5 quiz modules (gender & plural, ser/estar/ficar, contractions, subjunctive, indirect
  speech), ported using the same shared-core pattern established by verbs/vocabulary.
- Local notifications (daily due-item digest + streak-at-risk reminder) — flagged by the game-dev
  review as the highest-leverage retention feature not in v1.
- Full "game feel" polish: sound design, in-session combo/XP display, richer animations.
- Cloud backup / accounts / cross-device sync, using the `schema_version`-ready SQLite schema from
  §6.2 as the migration starting point.
- Monetization (revisit once there's usage data).
- Finer-grained SM-2 quality scoring and a stricter "mastered" definition (§7).
- Listening/speaking practice, guided CEFR curriculum path, diagnostic placement test.
- Home-screen widget (streak + due count).

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

Committed to `main`-bound branch ahead of this spec, since they affect data both apps will share:

- `verbs.json`: `estar`'s pretérito mais-que-perfeito wrongly reused `ser`'s participle
  ("tinha sido" → "tinha estado"); `cultivar`/`limpar` had incorrect `participios_passados` forms.
- `gender_quiz.js`: the plural-only quiz question was hardcoded as "plural masculino" even for
  inherently feminine invariable nouns like "mão" — relabeled to gender-neutral "plural" (a code
  fix, not a data fix, since the item's `masculine` field doubles as the generic prompt slot).
- `vocabulary.json`: EP spelling fix (dezenove → dezanove) and several typos/mistranslations
  (cugnada/cugnado, madraste/padraste, veranda, "bom aproveito", madrugada).

## Decision log

| Decision | Choice | Source |
|---|---|---|
| Platform | React Native (Expo) | Product owner |
| v1 module scope | Verb Conjugation + Vocabulary | Product owner |
| Backend/sync | None — local only | Product owner |
| Input model | Hybrid: typed+accent bar for verbs, multiple-choice for vocabulary | Product owner, synthesizing pedagogy + game-dev reviews |
| Monetization | None | Product owner |
| Notifications | None in v1 | Product owner |
| Target OS | Android + iOS at launch | Product owner |
| Repo structure | Monorepo, shared `packages/core` | Product owner (conditional on shareable code — confirmed by engineering review) |
| Content bugs found in review | Fixed immediately | Product owner |
| Game-feel polish | Minimal (haptics + basic animation) | Product owner |
| Session design | Quick Practice (capped, SRS-first, interleaved) default; Advanced full setup available | Product owner, synthesizing pedagogy + game-dev reviews |
