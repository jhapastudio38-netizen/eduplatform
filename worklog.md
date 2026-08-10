# DreamKorea SmartClass — Worklog

## FIX-ALL — Exam, Eye Test, Google Login (Android wrapper)

**Task ID:** FIX-ALL
**Scope:** `student-app-rust/android-wrapper/app/src/main/java/app/dreamkorea/smartclass/`

### Summary

Fixed 15 issues across the DreamKorea Android wrapper:

1. **ExamScreen.kt — Question Display (Issue #1)**
   - `verticalArrangement` changed `Center` → `Top` on the LEFT content column
   - Removed the stem card (the `q.stem.ifBlank { q.mediaText ?: "" }` Surface)
   - Stem now shows ONLY in the instruction row at the top
   - Added description block: image (heightIn max=200.dp), text Surface with `Color(0xFFF8FAFC)`, left-aligned, 18sp, FontWeight.Medium
   - Added media block: image (heightIn max=220.dp), text Surface with `Color(0xFFF8FAFC)`, center-aligned, 18sp, FontWeight.Medium
   - Media AudioPlayerCard now uses `loopDelaySec = if (q.audioLoopDelay > 0) q.audioLoopDelay else 2`

2. **ExamScreen.kt — AudioPlayerCard (Issue #2)**
   - Added new params: `onPlayStart`, `stopToken`, `isReview`, `externallyBlocked`
   - Added `var localPlayCount by remember { mutableStateOf(0) }` for when `playCounts` is null
   - `disabled = if (isReview) false else persistentCount >= maxPlays`
   - Added `LaunchedEffect(stopToken)` to stop this audio when another starts (only-one-audio-at-a-time)
   - `onPlayStart?.invoke()` called inside `setOnPreparedListener`
   - Icon is always `Icons.Default.PlayArrow`, dimmed to 0.3 alpha when disabled (never dimmed when `isReview`)
   - `enabled = !disabled && !isPlaying && !externallyBlocked`
   - Removed the Row with headphones icon and "Tap to play" text — just an IconButton in a Box (size 36dp)

3. **ExamScreen.kt — Options (Issue #3)**
   - Text option fontSize: 13sp → 16sp
   - Option circle: 40dp → 44dp; number text 15sp → 18sp
   - Image options: circle 28dp → 34dp; image 48dp → 80dp; number 10sp → 14sp
   - All option rows now `clickable(enabled = true)` — always clickable even during audio
   - Nav buttons (Prev/All/Next) remain `clickable(enabled = !audioPlaying)`
   - On the LAST question, the Submit button is gone — only Prev and All Questions remain. The student opens the grid to submit.

4. **ExamScreen.kt — Review Audio (Issue #4)**
   - All review AudioPlayerCard calls now use `isReview = true, loopCount = 1, loopDelaySec = 0`

5. **ExamScreen.kt — ReviewCard (Issue #5)**
   - After the stem text, added:
     - Description image (descType=="image" + descImageUrl) → AsyncImage heightIn max=160.dp
     - Description text (descText) → Surface with `Color(0xFFF8FAFC)`, left-aligned, 13sp
     - Media image (mediaImageUrl ?: imageUrl) → AsyncImage heightIn max=180.dp
     - Media text (mediaText) → Surface with `Color(0xFFF8FAFC)`, center-aligned, 13sp
     - Media audio (mediaAudioUrl ?: audioUrl) → AudioPlayerCard(isReview = true)

6. **ExamScreen.kt — Review Sorting (Issue #6)**
   - `val sortedReview = result.review.sortedWith(compareBy { if (it.blockType == "audio") 1 else 0 })` — Reading first, then Listening

7. **DreamKoreaApi.kt — ReviewItem (Issue #7)**
   - Added fields to `ReviewItem`: `descType`, `descText`, `descImageUrl`, `mediaType`, `mediaText`, `mediaImageUrl`, `mediaAudioUrl`, `answerType`, `blockType` (all with defaults like "none"/null/"text")
   - Updated `gradeCombinedExamClientSide` to populate the new fields from `QuestionDetail`

8. **DreamKoreaApi.kt — API Endpoints (Issue #8)**
   - `signup` endpoint already present (`api/auth/signup`)
   - Added `googleLogin` endpoint → `@POST("api/auth/google") suspend fun googleLogin(@Body body: Map<String, String>): CredentialsResponse`

9. **EyeVisionScreen.kt — Complete Rewrite (Issue #9)**
   - Landscape test mode with numeric keypad (3×4 grid: 1-9, C, 0, ⌫)
   - Keypad uses `weight(1f)` to fill available height
   - Next + Skip buttons always visible at the bottom
   - Portrait results screen with no stat boxes — only Back to Home
   - Single `DisposableEffect(targetOrientation)` for orientation (landscape while testing, portrait for results)
   - No `dreamkorea://auth-callback` redirect URL needed for eye test

10. **ExamEntryScreen.kt — Exam Overview Rewrite (Issue #10)**
    - White background, thin dark border panel (96% width, 92% height)
    - Title centered at top (24sp bold black)
    - Circular profile icon (50dp, dark `#1F2937` background, white person icon)
    - "Name of Student: {name}" and "Student Email: {email}" (12sp semibold)
    - "Exam description" heading (12sp bold)
    - Description text (12sp, max 5 lines, left-aligned)
    - "Get Started" button (blue `#1A73E8`, 130×34dp, white text)
    - "Cancel" button (white, dark border, 110×34dp)
    - Buttons side-by-side in a Row
    - DreamKorea watermark behind (200dp, 5% alpha)
    - No scroll — fits on screen

11. **LoginScreen.kt — Google Sign-In (Issue #11)**
    - Added "Sign in with Google" button at the bottom of the login screen
    - Opens `https://my-project-five-sepia.vercel.app/api/auth/google-mobile` in a Chrome Custom Tab (with `ACTION_VIEW` fallback)
    - Uses real Google logo (`R.drawable.google_logo` — added as a vector drawable with the 4-color G logo)
    - Visible on all tabs (Sign In / Sign Up / Forgot)
    - Added `androidx.browser:browser:1.8.0` dependency in `app/build.gradle.kts`

12. **LoginScreen.kt — Signup (no OTP) (Issue #12)**
    - Signup tab now calls `AppState.api.signup(mapOf("name" to ..., "email" to ..., "phone" to ..., "password" to ...))` — no `"mode"` key, no OTP

13. **MainActivity.kt — Deep Link Handler (Issue #13)**
    - Added `handleAuthCallback(intent)` for cold-launch deep links
    - Added `onNewIntent` override for warm-launch deep links (browser redirects while the app is running)
    - Reads `userId`, `name`, `email`, `phone`, `role` query params from `dreamkorea://auth-callback`
    - Saves user profile via `AppState.saveUserProfile` and invalidates cache

14. **AndroidManifest.xml — Deep Link (Issue #14)**
    - Added intent-filter on MainActivity for `dreamkorea://auth-callback`:
      - `android.intent.action.VIEW`
      - `android.intent.category.DEFAULT`
      - `android.intent.category.BROWSABLE`
      - `android:scheme="dreamkorea" android:host="auth-callback"`

15. **Screens.kt — EyeVision Nav Bar (Issue #15)**
    - Added `is Screen.EyeVision -> false` to the `showBottomBar` check (no bottom nav on the eye test screen)

### New Files
- `app/src/main/res/drawable/google_logo.xml` — Vector drawable for the 4-color Google "G" logo

### Modified Files
- `app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt`
- `app/src/main/java/app/dreamkorea/smartclass/ui/ExamEntryScreen.kt`
- `app/src/main/java/app/dreamkorea/smartclass/ui/EyeVisionScreen.kt`
- `app/src/main/java/app/dreamkorea/smartclass/ui/LoginScreen.kt`
- `app/src/main/java/app/dreamkorea/smartclass/ui/Screens.kt`
- `app/src/main/java/app/dreamkorea/smartclass/api/DreamKoreaApi.kt`
- `app/src/main/java/app/dreamkorea/smartclass/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

### Notes / Follow-ups
- The server side must implement `POST /api/auth/google-mobile` to redirect to `dreamkorea://auth-callback?userId=...&name=...&email=...&phone=...&role=...` after a successful Google OAuth flow
- The server side should also expose `POST /api/auth/google` (used by `googleLogin` in the API interface) for any programmatic Google token exchange
- The `AudioPlayerCard`'s `stopToken` / `externallyBlocked` params are wired into the function signature but the ExamScreen caller sites don't yet pass values for them — they default to null/false. If "only one audio plays at a time" behavior is needed across multiple audio players in the same question (e.g. media audio + audio options), wire the parent's `audioPlaying`/`stopToken` state through those params
