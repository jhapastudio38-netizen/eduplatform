# DreamKorea Android — Worklog

## FIX-3 — Eye test, exam overview, audio

**Date:** Auto-applied by sub-agent (Task ID: FIX-3)
**Target:** `student-app-rust/android-wrapper/`

### 1. EyeVisionScreen.kt — complete rewrite
File: `app/src/main/java/app/dreamkorea/smartclass/ui/EyeVisionScreen.kt`

Replaced the previous adaptive single-card list with a 3-stage flow:

- **`enum class AnswerOutcome { PENDING, CORRECT, INCORRECT, SKIPPED }`** — public, as specified.
- **Testing mode (landscape):**
  - `LaunchedEffect(Unit)` forces landscape 50ms after entry.
  - `DisposableEffect(targetOrientation)` is the single source of truth — landscape while testing, portrait for results/review.
  - Row layout: left = Ishihara plate `AsyncImage` (fills space, tap → full-screen viewer).
  - Right column: answer display box (60dp) → 3×4 numeric keypad (`1-9, C, 0, ⌫`) using `Modifier.weight(1f)` per row to fill height → Next + Skip buttons (always visible).
  - `AnimatedContent` with `slideInHorizontally`/`slideOutHorizontally` + `fadeIn`/`fadeOut` for question-to-question transitions (300ms).
  - Top bar: question counter ("Question N of M") + close button + Level chip.
- **Results mode (portrait):**
  - Big score "correct / total" + percent.
  - "Review Answers" + "Back to Home" buttons.
  - No stat boxes.
- **Review mode (portrait):**
  - Scrollable `LazyColumn` of every question.
  - Each row: plate image, "Your Answer: X", "Correct Answer: Y", CORRECT / INCORRECT / SKIPPED badge.
  - "Back to Home" button pinned to the bottom.
  - No stat boxes.
- **State tracking:**
  - `userAnswers: List<String>`, `correctAnswers: List<String>`, `outcomes: List<AnswerOutcome>` — sized to match `tests`, indexed by question.
  - Answer per question is submitted via `AppState.api.checkEyeVisionAnswer(testId, {answer})`; on success the server-returned `correctAnswer` is stored. Skipped questions store `""` and `AnswerOutcome.SKIPPED`.

### 2. ExamEntryScreen.kt — bigger buttons + description
File: `app/src/main/java/app/dreamkorea/smartclass/ui/ExamEntryScreen.kt`

- **Get Started button:** switched from `fillMaxWidth(0.7f).height(44.dp)` to `Modifier.width(170.dp).height(42.dp)`; font already 14sp.
- **Cancel button:** switched from `fillMaxWidth(0.7f).height(40.dp)` to `Modifier.width(140.dp).height(42.dp)`; fontSize `13.sp → 14.sp` and added `FontWeight.SemiBold`.
- **Description text:** `fontSize 12.sp → 14.sp`, `lineHeight = 18.sp`, `maxLines = 6`, with `TextOverflow.Ellipsis`.
- **Description heading:** added an explicit "About this exam" heading label above the description box at 14sp / Bold (the previous code had no separate heading).

### 3. ExamScreen.kt — audio gap default 2 seconds
File: `app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt`

In `AudioPlayerCard` calls for the exam question audio (line ~443) and option audio (line ~514):
- `loopDelaySec = q.audioLoopDelay` → `loopDelaySec = if (q.audioLoopDelay > 0) q.audioLoopDelay else 2`

Result: when the admin hasn't set a custom `audioLoopDelay`, students now get a 2-second gap between audio replays by default.

### 4. AudioPlayerCard — better play button + animation
File: `app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt`

- Added `isReview: Boolean = false` parameter to `AudioPlayerCard`.
- Replaced the old `IconButton` with a circular `Box` (48dp circle, `androidx.compose.foundation.shape.CircleShape`, primary-colored background).
- Icon: 24dp `Icons.Default.GraphicEq` while playing, `Icons.Default.PlayArrow` otherwise. Removed the `Icons.Default.Lock` icon entirely.
- Pulsing animation while playing via `rememberInfiniteTransition`: scale 1.0 → 1.2 → 1.0, 600ms repeat (300ms tween with `RepeatMode.Reverse`).
- When disabled (exam mode, play limit reached): `.alpha(0.3f)` dim, `PlayArrow` icon.
- When `isReview = true`: never disabled, always full color — student can replay as many times as they want (also bypassed the `maxPlays` check inside `setOnCompletionListener`).
- Updated the 3 review-mode call sites (question audio, option audio, and `AnswerDisplay` audio) to pass `isReview = true`.

### 5. Image loading speed
File: `app/src/main/java/app/dreamkorea/smartclass/DreamKoreaApp.kt`

Optimized the Coil `ImageLoader`:
- Memory cache: 25% of app memory (unchanged).
- Disk cache: `50MB → 100MB`.
- `respectCacheHeaders(false)` (unchanged).
- Removed `.crossfade(true)` / `.crossfade(200)` for instant display.
- Added an `OkHttpClient` with 10s connect timeout and 15s read timeout, attached via `.okHttpClient(...)`.
- Removed the now-unused `coil.request.CachePolicy` import.

### 6. Screens.kt — EyeVision nav bar
File: `app/src/main/java/app/dreamkorea/smartclass/ui/Screens.kt`

Added `is Screen.EyeVision -> false` to the `showBottomBar` `when` block so the bottom navigation bar is hidden while the eye-vision test is running (the test forces landscape and shouldn't show the portrait-only nav bar).

---

### Files changed
1. `app/src/main/java/app/dreamkorea/smartclass/ui/EyeVisionScreen.kt` — full rewrite.
2. `app/src/main/java/app/dreamkorea/smartclass/ui/ExamEntryScreen.kt` — bigger buttons + description.
3. `app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt` — `loopDelaySec` default + new play button + `isReview` plumbing.
4. `app/src/main/java/app/dreamkorea/smartclass/DreamKoreaApp.kt` — Coil ImageLoader optimization.
5. `app/src/main/java/app/dreamkorea/smartclass/ui/Screens.kt` — hide bottom bar on EyeVision.

### Next actions
- Build the APK (`./gradlew assembleDebug` from `android-wrapper/`) on a host with the Android SDK installed (`sdk.dir` in `local.properties`).
- Smoke-test the eye-vision flow end-to-end: enter → landscape keypad → answer/skip a few questions → results screen → review screen → back home.
- Verify the new circular play button animates and that review-mode audio replays indefinitely.
- Confirm images load instantly (no crossfade) and that the 100MB disk cache survives app restarts.
