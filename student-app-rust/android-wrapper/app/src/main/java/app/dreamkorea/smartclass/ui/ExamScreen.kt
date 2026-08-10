package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/// Bottom-only border helper — draws a line at the bottom edge of the composable.
/// Used for the header/nav row bottom separators (2px #222).
private fun Modifier.borderBottom(width: Dp, color: Color): Modifier =
    this.then(
        Modifier.drawBehind {
            val w = width.toPx()
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height - w / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height - w / 2),
                strokeWidth = w
            )
        }
    )

/**
 * Exam taking screen — full flow:
 * 1. Loading skeleton (with timeout + retry)
 * 2. Question-by-question with audio (loops N times), image, options
 * 3. Click sound on every interaction
 * 4. Wrong → shows correct answer in red
 * 5. Correct → green confirmation
 * 6. Auto-advances to next question
 * 7. Final score + review screen
 * 8. Stats auto-update via /api/student/tests/[id]/submit
 */
@Composable
fun ExamScreen(theme: AppTheme, testId: String, onExit: () -> Unit) {
    val sound = rememberSoundManager()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var test by remember { mutableStateOf<TestDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var currentIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Any>() }
    // Persistent audio play counts per question ID — survives navigation.
    // Prevents cheat where student navigates away and back to reset plays.
    val audioPlayCounts = remember { mutableStateMapOf<String, Int>() }
    // When audio is playing, disable navigation buttons
    var audioPlaying by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<SubmitResponse?>(null) }
    var submitting by remember { mutableStateOf(false) }
    // Per-question feedback (after answering, before moving on)
    var questionFeedback by remember { mutableStateOf<QuestionFeedback?>(null) }
    // Timer
    var timeLeft by remember { mutableStateOf(0) }

    // ── ORIENTATION ── FORCE LANDSCAPE. No onDispose PORTRAIT —
    // MainScreen handles portrait when screen changes to non-exam.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    // Retry trigger — increment to force reload
    var retryCount by remember { mutableStateOf(0) }

    // Load test detail — uses LaunchedEffect's own scope, finally block guarantees loading cleanup
    // ─── Back button handler — show exit warning during exam ──────────────
    var showExitDialog by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = !loading && error.isEmpty()) {
        showExitDialog = true
    }
    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Exam?") },
            text = { Text("Your exam is in progress. Are you sure you want to exit? Your answers will be lost.") },
            confirmButton = { Button(onClick = { showExitDialog = false; onExit() }) { Text("Exit") } },
            dismissButton = { OutlinedButton(onClick = { showExitDialog = false }) { Text("Continue Exam") } }
        )
    }

    LaunchedEffect(testId, retryCount) {
        loading = true
        error = ""
        try {
            // 30-second timeout — combined exams may need multiple requests
            val result = withTimeoutOrNull(30_000L) {
                when {
                    // Combined QBank exam — fetches ALL published question_bank tests as one test
                    testId == "qbank-combined" -> {
                        try {
                            AppState.api.getQBankCombined().test
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) buildQBankCombinedClientSide()
                            else throw e
                        }
                    }
                    // Combined bundle exam — fetches ALL tests in a specific bundle (qbank/batch)
                    testId.startsWith("bundle-") -> {
                        val bundleId = testId.removePrefix("bundle-")
                        try {
                            AppState.api.getBundleCombined(bundleId).test
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) buildBundleCombinedClientSide(bundleId)
                            else throw e
                        }
                    }
                    // Normal test — fetch by ID
                    else -> AppState.api.getTestDetail(testId).test
                }
            }
            if (result != null) {
                test = result
                timeLeft = (result.durationMin.coerceAtLeast(1)) * 60
            } else {
                error = "The request timed out. Check your internet connection and try again."
            }
        } catch (e: retrofit2.HttpException) {
            // Try to read the actual error message from the response body
            val rawBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            error = when (e.code()) {
                401 -> "Your session has expired. Please log out and sign in again."
                403 -> "This exam is not available right now."
                404 -> "This test could not be found. It may have been removed."
                500 -> "Server error. Please try again in a moment.${if (rawBody != null) " ($rawBody)" else ""}"
                else -> "Could not load the test (HTTP ${e.code()}).${if (rawBody != null) " $rawBody" else ""}"
            }
        } catch (e: java.net.UnknownHostException) {
            error = "No internet connection. Please check your network and try again."
        } catch (e: java.net.SocketTimeoutException) {
            error = "The request timed out. Please try again."
        } catch (e: java.io.IOException) {
            error = "Network error: ${e.message ?: "Could not connect to server."}"
        } catch (e: Exception) {
            error = "Unexpected error: ${e.message ?: "Please try again."}"
        } finally {
            // GUARANTEED: loading is always reset, even if the coroutine is cancelled
            loading = false
        }
    }

    // Countdown timer
    LaunchedEffect(test, submitResult) {
        if (test != null && submitResult == null) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            // Auto-submit when timer reaches zero
            val currentTest = test
            if (timeLeft == 0 && currentTest != null && submitResult == null && !submitting) {
                submitting = true
                sound.swoosh()
                try {
                    // Use fallback for combined exams (qbank-combined / bundle-{id})
                    submitResult = if (currentTest.id == "qbank-combined" || currentTest.id.startsWith("bundle-")) {
                        submitCombinedExamWithFallback(currentTest, answers.toMap())
                    } else {
                        submitExamWithFallback(currentTest, answers.toMap())
                    }
                    sound.success()
                } catch (e: java.net.UnknownHostException) {
                    error = "No internet connection. Could not submit your answers."
                } catch (e: java.io.IOException) {
                    error = "Network error. Could not submit your answers."
                } catch (e: Exception) {
                    error = "Could not submit: ${e.message ?: "Unknown error"}"
                }
                submitting = false
            }
        }
    }

    if (loading) {
        // Loading skeleton with a subtle "Loading..." label so users know it's working
        Column(Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = theme.primary,
                trackColor = theme.primary.copy(alpha = 0.1f),
            )
            SkeletonListScreen(theme, itemCount = 4)
        }
        return
    }

    if (error.isNotEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CloudOff,
                null,
                tint = theme.errorRed.copy(alpha = 0.7f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Couldn't load the test",
                color = theme.darkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                color = theme.subText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Test ID: $testId",
                color = theme.subText.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Go back") }
                Button(
                    onClick = {
                        sound.click()
                        retryCount++ // triggers LaunchedEffect reload
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
        return
    }

    val t = test ?: return
    // Guard: if the test has no questions, show a friendly message instead of crashing
    if (t.items.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Quiz, null, tint = theme.subText, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("This test has no questions yet.", color = theme.darkText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) { Text("Go back") }
        }
        return
    }

    val currentQuestion = t.items.getOrNull(currentIdx)

    // ─── Result screen ──────────────────────────────────────────────────────
    if (submitResult != null) {
        ExamResultScreen(theme, submitResult!!, onExit, sound, examTitle = t.title, examDescription = t.description)
        return
    }

    // ═══ EXAM UI — spec-compliant landscape layout ═══
    // Sort items: Reading (text) first, then Listening (audio)
    val sortedItems = t.items.sortedWith(compareBy(
        { if (it.question.blockType == "audio") 1 else 0 },
        { it.question.blockNumber }
    ))
    val item = sortedItems.getOrNull(currentIdx) ?: return
    val q = item.question
    val options = q.options ?: emptyList()
    val textItems = sortedItems.filter { it.question.blockType != "audio" }
    val readingCount = if (textItems.isNotEmpty()) textItems.size else sortedItems.size
    val listeningCount = if (textItems.isNotEmpty()) sortedItems.size - textItems.size else 0
    val answeredCount = answers.size
    val remainingCount = sortedItems.size - answeredCount
    // Start with the grid view so the student can pick which question to answer
    // first. They tap a number → answer → return to grid → pick next → submit.
    var showGrid by remember { mutableStateOf(true) }

    // ── QUESTION GRID PAGE ── shown first when entering the exam.
    // The question display below only shows when showGrid = false.
    // CRITICAL: Both layouts must NOT render simultaneously — that causes
    // a crash (two full-screen Columns competing for the same space).
    if (showGrid) {
        val readingItems = sortedItems.filter { it.question.blockType != "audio" }
        val listeningItems = sortedItems.filter { it.question.blockType == "audio" }
        val isQBank = testId == "qbank-combined" || testId.startsWith("bundle-")
        val showAllBlocks = t.showAllBlocks
        var showSubmitDialog by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        var filterMode by remember { mutableStateOf<Boolean?>(null) }

        val readingAnswered = readingItems.count { answers.containsKey(it.question.id) }
        val listeningAnswered = listeningItems.count { answers.containsKey(it.question.id) }
        val totalAnswered = readingAnswered + listeningAnswered
        val totalQuestions = sortedItems.size
        val totalUnsolved = totalQuestions - totalAnswered
        val hh = timeLeft / 3600; val mm = (timeLeft % 3600) / 60; val ss = timeLeft % 60
        val timeStr = String.format("%02d:%02d:%02d", hh, mm, ss)

        val userEmail = AppState.getUserEmail()
        val userName = AppState.getUserName()

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            // ══ PIXEL-PERFECT BLOCK PAGE ══
            // Fixed design canvas: 1364 × 693 px (same as exam entry screen).
            // The entire canvas scales proportionally — children keep pixel sizes.
            val cw = maxWidth.value
            val ch = maxHeight.value
            val scale = minOf(cw / 1364f, ch / 693f).coerceAtLeast(0.15f)
            val sdp: (Float) -> Dp = { v -> (v * scale).dp }
            val ssp: (Float) -> TextUnit = { v -> (v * scale).sp }
            // Answered/current question color — same blue as submit button
            val accentBlue = Color(0xFF1673E8)

            // ── OUTER FRAME: 2px solid #222222, no radius, 18px margin ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(sdp(18f))
                    .border(width = sdp(2f), color = Color(0xFF222222))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ══ 1. HEADER + NAV AREA (120px tall — compact so grids fit) ══
                    // Logo column (137px wide) spans BOTH header row (60px) and
                    // nav row (60px) on the left. Right side has 2 stacked rows.
                    Row(modifier = Modifier.fillMaxWidth().height(sdp(120f))) {
                        // ── Logo column: 137px wide, 120px tall ──
                        Box(
                            modifier = Modifier.width(sdp(137f)).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                                contentDescription = "DreamKorea Logo",
                                modifier = Modifier.fillMaxSize().padding(sdp(10f)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // ── Right side: header row (60px) + nav row (60px) ──
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // ── Header row: 60px, 3 centered labels, bottom border 2px #222 ──
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sdp(60f))
                                    .borderBottom(sdp(2f), Color(0xFF222222)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // LEFT: exam title
                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(
                                        t.title.take(40),
                                        color = Color(0xFF111111),
                                        fontSize = ssp(30f),
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                // CENTER: user email (student ID code)
                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(
                                        userEmail.take(40),
                                        color = Color(0xFF111111),
                                        fontSize = ssp(30f),
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                // RIGHT: user name
                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(
                                        userName.take(40),
                                        color = Color(0xFF111111),
                                        fontSize = ssp(30f),
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // ── Nav row: 60px, 5 items, bottom border 2px #222 ──
                            // No vertical separators between items (per spec)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sdp(60f))
                                    .borderBottom(sdp(2f), Color(0xFF222222)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nepal — same style, not clickable
                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text("Nepal", color = Color(0xFF111111), fontSize = ssp(29f), fontWeight = FontWeight.Normal)
                                }
                                // All — active by default, #F4F4F4 bg + 4px black underline
                                NavTab("All", filterMode == null, sdp, ssp, Color(0xFF111111), Color(0xFFF4F4F4)) { filterMode = null }
                                // Solved
                                NavTab("Solved", filterMode == true, sdp, ssp, Color(0xFF111111), Color(0xFFF4F4F4)) { filterMode = true }
                                // Unsolved
                                NavTab("Unsolved", filterMode == false, sdp, ssp, Color(0xFF111111), Color(0xFFF4F4F4)) { filterMode = false }
                                // Timer
                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(
                                        timeStr,
                                        color = Color(0xFF111111),
                                        fontSize = ssp(29f),
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // ══ 2. SECTION TITLE ROW (45px — compact so grids fit) ══
                    // Two bordered boxes: Reading (left) | Listening (right)
                    if (!isQBank) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(sdp(45f)).padding(horizontal = sdp(18f)),
                            horizontalArrangement = Arrangement.spacedBy(sdp(18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(sdp(2f), Color(0xFFC6C6C6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Reading", color = Color(0xFF222222), fontSize = ssp(22f), fontWeight = FontWeight.Normal)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(sdp(2f), Color(0xFFC6C6C6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Listening", color = Color(0xFF222222), fontSize = ssp(22f), fontWeight = FontWeight.Normal)
                            }
                        }
                    }

                    // ══ 3. QUESTION GRID PANELS ══
                    // Two rounded panels (3px #222 border, 18px radius) with
                    // 5×4 grid of question buttons inside each.
                    // Panels use weight(1f) to fill all remaining vertical space.
                    if (isQBank) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = sdp(18f), vertical = sdp(4f))) {
                            Box(
                                modifier = Modifier
                                    .weight(1f).fillMaxWidth()
                                    .border(sdp(3f), Color(0xFF222222), RoundedCornerShape(sdp(18f)))
                                    .padding(sdp(8f))
                            ) {
                                QuestionGridScaled(
                                    items = sortedItems,
                                    sortedItems = sortedItems,
                                    test = t,
                                    answers = answers,
                                    currentIdx = currentIdx,
                                    sound = sound,
                                    haptic = haptic,
                                    filterMode = filterMode,
                                    scale = scale,
                                    showAllBlocks = showAllBlocks,
                                    accentBlue = accentBlue
                                ) { idx ->
                                    currentIdx = idx
                                    showGrid = false
                                }
                            }
                        }
                    } else {
                        // Exam: Reading LEFT | Listening RIGHT
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = sdp(18f), vertical = sdp(4f)),
                            horizontalArrangement = Arrangement.spacedBy(sdp(18f))
                        ) {
                            // Reading panel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(sdp(3f), Color(0xFF222222), RoundedCornerShape(sdp(18f)))
                                    .padding(sdp(8f))
                            ) {
                                QuestionGridScaled(
                                    items = readingItems,
                                    sortedItems = sortedItems,
                                    test = t,
                                    answers = answers,
                                    currentIdx = currentIdx,
                                    sound = sound,
                                    haptic = haptic,
                                    filterMode = filterMode,
                                    scale = scale,
                                    showAllBlocks = showAllBlocks,
                                    accentBlue = accentBlue
                                ) { idx ->
                                    currentIdx = idx
                                    showGrid = false
                                }
                            }
                            // Listening panel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(sdp(3f), Color(0xFF222222), RoundedCornerShape(sdp(18f)))
                                    .padding(sdp(8f))
                            ) {
                                QuestionGridScaled(
                                    items = listeningItems,
                                    sortedItems = sortedItems,
                                    test = t,
                                    answers = answers,
                                    currentIdx = currentIdx,
                                    sound = sound,
                                    haptic = haptic,
                                    filterMode = filterMode,
                                    scale = scale,
                                    showAllBlocks = showAllBlocks,
                                    accentBlue = accentBlue
                                ) { idx ->
                                    currentIdx = idx
                                    showGrid = false
                                }
                            }
                        }
                    }

                    // ══ 4. SUBMIT BUTTON: 328×68px, #1673E8, 18px radius ══
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(sdp(8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showSubmitDialog = true },
                            modifier = Modifier.width(sdp(328f)).height(sdp(68f)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1673E8)),
                            shape = RoundedCornerShape(sdp(18f)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(
                                "Submit and Finish Exam",
                                color = Color.White,
                                fontSize = ssp(25f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Submit confirmation dialog ──
            if (showSubmitDialog) {
                val warning = when {
                    totalUnsolved == 0 -> "You answered all $totalQuestions questions. Ready to submit!"
                    totalUnsolved <= 5 -> "You have $totalUnsolved unanswered question(s). Submit anyway?"
                    else -> "Warning: $totalUnsolved questions are still unanswered! Submit anyway?"
                }
                val warningColor = when {
                    totalUnsolved == 0 -> Color(0xFF16A34A)
                    totalUnsolved <= 5 -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
                AlertDialog(
                    onDismissRequest = { showSubmitDialog = false },
                    title = { Text("Submit Exam?") },
                    text = {
                        Column {
                            Text(warning, color = warningColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Reading: $readingAnswered/${readingItems.size} • Listening: $listeningAnswered/${listeningItems.size}",
                                color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitDialog = false
                                if (!submitting) {
                                    sound.swoosh(); submitting = true
                                    scope.launch {
                                        try { submitResult = submitExamWithFallback(t, answers.toMap()); sound.success() }
                                        catch (e: Exception) { error = "Submit failed." }
                                        submitting = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087CF0))
                        ) { if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) } else { Text("Submit", color = Color.White) } }
                    },
                    dismissButton = { OutlinedButton(onClick = { showSubmitDialog = false }) { Text("Cancel") } }
                )
            }
        }
        return
    } // end if (showGrid)

    // ═══ QUESTION DISPLAY PAGE ═══
    // Shown when showGrid = false (after the student taps a question number).
    // Layout: top status header + instruction row + content (question left,
    // answers right) + bottom navigation.
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // ── 1. TOP STATUS HEADER ── compact white bar with thin border
        Surface(
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val sectionLabel = if (q.blockType == "audio") "Listening ($listeningCount)" else "Reading ($readingCount)"
                    Text(sectionLabel, color = Color(0xFF003478), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Total: ${t.items.size}", color = Color(0xFF64748B), fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Left: $remainingCount", color = Color(0xFF64748B), fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val mm = timeLeft / 60; val ss = timeLeft % 60
                    Text(String.format("%02d:%02d", mm, ss), color = Color(0xFF003478), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 2. INSTRUCTION ROW ──
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${currentIdx + 1}. ", color = Color(0xFF003478), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(q.stem.take(80), color = Color(0xFF1E293B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (q.isFree) {
                    Spacer(Modifier.width(4.dp))
                    Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(3.dp)) {
                        Text("FREE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showGrid = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.GridView, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // ── 3. MAIN CONTENT ── 60% question | 40% answers
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(280.dp).alpha(0.08f),
                contentScale = ContentScale.Fit
            )
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Question content (60%)
                Column(
                    modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!q.title.isNullOrBlank()) {
                        Text(q.title, color = Color(0xFF003478), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.92f).padding(bottom = 4.dp))
                    }
                    if (q.descType == "image" && !q.descImageUrl.isNullOrBlank()) {
                        val url = q.descImageUrl!!.toAbsoluteUrl()
                        coil.compose.AsyncImage(
                            model = url, contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)).clickable { FullScreenImageViewer.show(url) },
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (!q.descText.isNullOrBlank()) {
                        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(0.92f)) {
                            Text(q.descText, color = Color(0xFF1E293B), fontSize = 20.sp, textAlign = TextAlign.Start, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    val mediaImgUrl = (q.mediaImageUrl ?: q.imageUrl)?.toAbsoluteUrl()
                    if (!mediaImgUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = mediaImgUrl, contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 220.dp).clip(RoundedCornerShape(8.dp)).clickable { FullScreenImageViewer.show(mediaImgUrl) },
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (!q.mediaText.isNullOrBlank()) {
                        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(0.92f)) {
                            Text(q.mediaText, color = Color(0xFF1E293B), fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    val mediaAudUrl = (q.mediaAudioUrl ?: q.audioUrl)?.toAbsoluteUrl()
                    if (!mediaAudUrl.isNullOrBlank()) {
                        key(q.id) {
                            AudioPlayerCard(
                                theme = theme, url = mediaAudUrl,
                                loopCount = q.audioLoop,
                                loopDelaySec = if (q.audioLoopDelay > 0) q.audioLoopDelay else 2,
                                sound = sound, questionId = q.id,
                                playCounts = audioPlayCounts,
                                onPlayingChange = { audioPlaying = it }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                // RIGHT: Answer options (40%)
                Column(
                    modifier = Modifier.weight(0.4f).fillMaxHeight().padding(8.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    when (q.answerType) {
                        "text", "choose" -> {
                            (0 until minOf(4, options.size)).forEach { i ->
                                val isSelected = answers[q.id] == options.getOrNull(i)
                                val optText = options.getOrNull(i) ?: ""
                                val blankWord = q.optionBlanks.getOrNull(i)?.takeIf { it.isNotBlank() }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = optText },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isSelected) theme.primary else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = buildUnderlinedText(optText, blankWord), color = Color.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        "image" -> {
                            (0 until minOf(4, q.optionImages.size)).forEach { i ->
                                val imgUrl = q.optionImages[i]; if (imgUrl.isBlank()) return@forEach
                                val absUrl = imgUrl.toAbsoluteUrl()
                                val isSelected = answers[q.id] == absUrl
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = absUrl },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = if (isSelected) theme.primary else Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(34.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    coil.compose.AsyncImage(model = absUrl, contentDescription = "Option ${i+1}", modifier = Modifier.size(80.dp).clip(RoundedCornerShape(4.dp)).clickable { FullScreenImageViewer.show(absUrl) }, contentScale = ContentScale.Fit)
                                }
                            }
                        }
                        "audio" -> {
                            (0 until minOf(4, q.optionAudios.size)).forEach { i ->
                                val audUrl = q.optionAudios[i]; if (audUrl.isBlank()) return@forEach
                                val absUrl = audUrl.toAbsoluteUrl()
                                val isSelected = answers[q.id] == absUrl
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = absUrl }, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = if (isSelected) theme.primary else Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(34.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    key(q.id, i) {
                                        AudioPlayerCard(theme = theme, url = absUrl, loopCount = q.audioLoop.coerceAtLeast(1), loopDelaySec = q.audioLoopDelay, sound = sound, questionId = "${q.id}-opt-$i", playCounts = audioPlayCounts, onPlayingChange = { audioPlaying = it })
                                    }
                                }
                            }
                        }
                    }
                }
            } // end Row
        } // end Box (watermark background)

        // ── 4. BOTTOM NAVIGATION ──
        Surface(
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { if (currentIdx > 0) { currentIdx--; sound.click() } }, contentAlignment = Alignment.Center) {
                    Text("अघिल्लो (Prev)", color = if (currentIdx > 0) Color(0xFF003478) else Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { sound.click(); showGrid = true }, contentAlignment = Alignment.Center) {
                    Text("सबै प्रश्नहरू (All)", color = Color(0xFF003478), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (currentIdx < sortedItems.size - 1) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                        if (currentIdx < sortedItems.size - 1) { currentIdx++; sound.click() }
                    }, contentAlignment = Alignment.Center) {
                        if (submitting) {
                            CircularProgressIndicator(color = Color(0xFF003478), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("अर्को (Next)", color = Color(0xFF003478), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    } // end Column (question display)
}

/// Nav tab — centered label, subtle background + 4px black underline when active.
/// The caller must provide a weight modifier (e.g. Modifier.weight(1f)) from
/// the parent RowScope, since weight is not available outside RowScope.
@Composable
private fun RowScope.NavTab(
    label: String,
    active: Boolean,
    sdp: (Float) -> Dp,
    ssp: (Float) -> TextUnit,
    textColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (active) activeBg else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                label,
                color = textColor,
                fontSize = ssp(29f),
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(sdp(2f)))
            // 4px black bottom indicator — full tab width when active
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (active) sdp(4f) else 0.dp)
                    .background(Color(0xFF111111))
            )
        }
    }
}

/// Section header — 68dp tall, 1.5px #B8B8B8 border.
@Composable
private fun SectionHeader(label: String, sdp: (Float) -> Dp, ssp: (Float) -> TextUnit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(sdp(69f)),
        border = androidx.compose.foundation.BorderStroke(sdp(2f), Color(0xFFC6C6C6))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                label,
                color = Color(0xFF222222),
                fontSize = ssp(28f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/// Scaled 5-column grid of question buttons.
/// - 3px border #222222, 36sp numbers, NO rounded corners (square)
/// - globalIndices maps items → sortedItems (NOT test.items), because
///   currentIdx indexes into sortedItems.
@Composable
private fun QuestionGridScaled(
    items: List<TestItemDetail>,
    sortedItems: List<TestItemDetail>,
    test: TestDetail,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    scale: Float,
    showAllBlocks: Boolean,
    accentBlue: Color,
    onPick: (Int) -> Unit,
) {
    val sdp: (Float) -> Dp = { v -> (v * scale).dp }
    val ssp: (Float) -> TextUnit = { v -> (v * scale).sp }
    // IMPORTANT: map each item to its index in sortedItems (NOT test.items)
    val globalIndices = items.mapNotNull { item ->
        sortedItems.indexOfFirst { it.question.id == item.question.id }.takeIf { it >= 0 }
    }
    val cols = 5  // 5 columns per spec

    // Determine total cells to render (with optional block padding)
    val expectedTotal = if (showAllBlocks) {
        val isAudio = items.isNotEmpty() && items[0].question.blockType == "audio"
        val blockCount = if (isAudio) test.audioBlockCount else test.textBlockCount
        maxOf(blockCount, items.size)
    } else {
        items.size
    }
    val rowsCount = (expectedTotal + cols - 1) / cols

    // NO verticalScroll — all 4 rows must fit on screen.
    // Each Row uses weight(1f) so 4 rows share the vertical space equally.
    // Buttons use weight(1f) for width and fillMaxHeight() so they fill
    // available space without forcing square aspect ratio.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(sdp(6f))
    ) {
        for (rowIdx in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(sdp(6f))
            ) {
                for (colIdx in 0 until cols) {
                    val localIdx = rowIdx * cols + colIdx
                    if (localIdx < items.size && localIdx < globalIndices.size) {
                        val globalIdx = globalIndices[localIdx]
                        val q = items[localIdx].question
                        // Display number: Reading 1-20, Listening 21-40 (blockNumber + 20 for audio)
                        val displayNum = if (q.blockType == "audio") {
                            (q.blockNumber.takeIf { it > 0 } ?: (localIdx + 1)) + 20
                        } else {
                            q.blockNumber.takeIf { it > 0 } ?: (localIdx + 1)
                        }
                        val isAnswered = answers.containsKey(items[localIdx].question.id)
                        val isCurrent = globalIdx == currentIdx
                        val isFilteredOut = when (filterMode) {
                            true -> !isAnswered
                            false -> isAnswered
                            null -> false
                        }
                        // Question button: 3px border #222222, fills available space (no aspectRatio)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(
                                    width = sdp(3f),
                                    color = when {
                                        isCurrent -> accentBlue
                                        isAnswered -> accentBlue
                                        else -> Color(0xFF222222)
                                    },
                                    shape = androidx.compose.ui.graphics.RectangleShape
                                )
                                .background(if (isAnswered) accentBlue else Color.White)
                                .alpha(if (isFilteredOut) 0.15f else 1f)
                                .clickable(enabled = !isFilteredOut) {
                                    sound.click()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPick(globalIdx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$displayNum",
                                color = if (isAnswered) Color.White else Color(0xFF111111),
                                fontSize = ssp(32f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else if (showAllBlocks) {
                        // Empty placeholder cell
                        val isAudioGrid = items.isNotEmpty() && items[0].question.blockType == "audio"
                        val placeholderNum = if (isAudioGrid) localIdx + 21 else localIdx + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(sdp(1.5f), Color(0xFFEEEEEE))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$placeholderNum",
                                color = Color(0xFFCCCCCC),
                                fontSize = ssp(20f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

/// Reference-style tab: flex:1, centered, blue bottom border (3dp) when active.
@Composable
private fun RefTab(label: String, active: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (active) Color(0xFFF2F2F2) else Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = if (active) Color(0xFF111111) else Color(0xFF444444),
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 3.dp else 0.dp)
                .background(accent)
        )
    }
}

/// Reference-style 4-column grid of PERFECT SQUARE cells with 2px black borders.
/// Solved = blue fill + white text. Current = blue border + glow.
/// Uses continuous numbering: Reading 1-20, Listening 21-40 (from globalIdx).
/// showAllBlocks: true = pad grid to expected count (20) with blank cells so
/// the user sees which questions are added vs blank.
/// false = only show cells for questions that actually exist.
/// Cells ALWAYS maintain 1:1 aspect ratio (perfect square) regardless of count.
/// Grid is scrollable so all blocks are reachable.
@Composable
private fun QuestionGridRef(
    test: TestDetail,
    items: List<TestItemDetail>,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    accentBlue: Color,
    showAllBlocks: Boolean,
    onPick: (Int) -> Unit,
) {
    val globalIndices = items.mapNotNull { item -> test.items.indexOfFirst { it.question.id == item.question.id }.takeIf { it >= 0 } }
    val cols = 4  // 4 columns per the HTML reference
    val gridScrollState = rememberScrollState()

    // Determine total cells to render
    val expectedTotal = if (showAllBlocks) {
        val isAudio = items.isNotEmpty() && items[0].question.blockType == "audio"
        val blockCount = if (isAudio) test.audioBlockCount else test.textBlockCount
        maxOf(blockCount, items.size)
    } else {
        items.size
    }
    val rowsCount = (expectedTotal + cols - 1) / cols

    // Scrollable grid — cells are PERFECT SQUARES (aspectRatio 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(gridScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowIdx in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIdx in 0 until cols) {
                    val localIdx = rowIdx * cols + colIdx
                    if (localIdx < items.size) {
                        val globalIdx = globalIndices[localIdx]
                        // DISPLAY NUMBER: Reading shows 1-20, Listening shows 21-40
                        // Uses blockNumber (1-20 within each block) + 20 offset for audio
                        val q = items[localIdx].question
                        val displayNum = if (q.blockType == "audio") {
                            (q.blockNumber.takeIf { it > 0 } ?: (localIdx + 1)) + 20
                        } else {
                            q.blockNumber.takeIf { it > 0 } ?: (localIdx + 1)
                        }
                        val isAnswered = answers.containsKey(items[localIdx].question.id)
                        val isCurrent = globalIdx == currentIdx
                        val isFilteredOut = when (filterMode) {
                            true -> !isAnswered
                            false -> isAnswered
                            null -> false
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)  // PERFECT SQUARE — always 1:1
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.5.dp,
                                    color = when {
                                        isCurrent -> accentBlue
                                        isAnswered -> accentBlue
                                        else -> Color(0xFF111111)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(if (isAnswered) accentBlue else Color.White)
                                .alpha(if (isFilteredOut) 0.15f else 1f)
                                .clickable(enabled = !isFilteredOut) {
                                    sound.click()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPick(globalIdx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$displayNum",
                                color = if (isAnswered) Color.White else Color(0xFF111111),
                                fontSize = 15.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    } else if (showAllBlocks) {
                        // Empty placeholder cell — perfect square
                        // Reading placeholders: 1-20, Listening placeholders: 21-40
                        val isAudioGrid = items.isNotEmpty() && items[0].question.blockType == "audio"
                        val placeholderNum = if (isAudioGrid) localIdx + 21 else localIdx + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)  // PERFECT SQUARE
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$placeholderNum",
                                color = Color(0xFFCCCCCC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    } else {
                        // showAllBlocks=false, no question — invisible spacer (perfect square)
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .border(1.5.dp, Color.Black)
            .background(if (selected) Color(0xFF003478) else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

data class QuestionFeedback(val isCorrect: Boolean, val correctAnswer: String)

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black), modifier = Modifier.size(14.dp)) {}
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.Black, fontSize = 10.sp)
    }
}

// ─── Audio player with loop support ───────────────────────────────────────────
// loopCount = total number of times to play the audio:
//   0 = default (play 2 times, with 2s gap)
//   1 = play once (no loop) — used in review mode
//   2 = play twice
//   N = play N times
//
// GLOBAL AUDIO: Only one AudioPlayerCard can play at a time. When a new
// audio starts, any previously playing audio is stopped. This is tracked
// via the AudioRegistry companion object.
//
// PERSISTENT play count (playCounts map) prevents the student from
// cheating by navigating away and back to reset plays. In review mode
// (playCounts = null), a local count is used instead.
@Composable
fun AudioPlayerCard(
    theme: AppTheme,
    url: String,
    loopCount: Int,
    loopDelaySec: Int,
    sound: SoundManager,
    questionId: String? = null,
    playCounts: SnapshotStateMap<String, Int>? = null,
    onPlayingChange: ((Boolean) -> Unit)? = null,
    unlimited: Boolean = false,
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    // Local play count — used when playCounts is null (review mode)
    var localPlayCount by remember { mutableStateOf(0) }

    // PERSISTENT play count — stored in parent map (survives navigation).
    val persistentCount = playCounts?.get(questionId) ?: localPlayCount
    // Sync isPlaying with parent callback
    LaunchedEffect(isPlaying) {
        onPlayingChange?.invoke(isPlaying)
    }

    // maxPlays: loopCount=1 → play once, loopCount=0 → play twice (default), else play N times
    // unlimited=true → no limit (review mode — user can play as much as they want)
    val maxPlays = when {
        unlimited -> Int.MAX_VALUE
        loopCount == 1 -> 1
        loopCount <= 0 -> 2
        else -> loopCount
    }
    val disabled = !unlimited && persistentCount >= maxPlays
    val scope = rememberCoroutineScope()

    fun incrementPlayCount() {
        if (questionId != null && playCounts != null) {
            playCounts[questionId] = (playCounts[questionId] ?: 0) + 1
        } else {
            localPlayCount++
        }
    }

    // ── GLOBAL AUDIO: stop this player when another AudioPlayerCard starts ──
    DisposableEffect(url) {
        AudioRegistry.registerPlayer(url) {
            // Callback: another player started → stop this one
            if (isPlaying) {
                try { mediaPlayer?.let { it.stop(); it.reset() } } catch (_: Exception) {}
                isPlaying = false
            }
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            AudioRegistry.unregisterPlayer(url)
        }
    }

    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
                // Play button — always looks enabled (no dimming) but ignores
                // clicks while this audio is playing. Other audio buttons are
                // also always visible at full opacity.
                IconButton(
                    onClick = {
                        if (disabled) return@IconButton
                        if (isPlaying) return@IconButton // ignore tap while playing
                        sound.click()
                        // GLOBAL: stop any other currently playing audio
                        AudioRegistry.stopAllExcept(url)
                        try {
                            mediaPlayer?.release()
                            val mp = android.media.MediaPlayer().apply {
                                setDataSource(url)
                                setOnPreparedListener {
                                    start()
                                    isPlaying = true
                                    incrementPlayCount()
                                }
                                setOnCompletionListener {
                                    // Unlimited mode (review) — never auto-replay, just stop
                                    if (unlimited) {
                                        isPlaying = false
                                        return@setOnCompletionListener
                                    }
                                    val currentCount = if (questionId != null && playCounts != null)
                                        playCounts?.get(questionId) ?: 0
                                    else localPlayCount
                                    if (currentCount < maxPlays) {
                                        scope.launch {
                                            // 2 second default gap between plays
                                            val delaySec = if (loopDelaySec > 0) loopDelaySec else 2
                                            delay(delaySec * 1000L)
                                            val latestCount = if (questionId != null && playCounts != null)
                                                playCounts?.get(questionId) ?: 0
                                            else localPlayCount
                                            if (latestCount < maxPlays) {
                                                incrementPlayCount()
                                                start()
                                            } else {
                                                isPlaying = false
                                            }
                                        }
                                    } else {
                                        isPlaying = false
                                    }
                                }
                            setOnErrorListener { mp, what, extra ->
                                android.util.Log.e("AudioPlayer", "Error: what=$what extra=$extra url=$url")
                                isPlaying = false
                                try { mp.reset() } catch (_: Exception) {}
                                true
                            }
                            prepareAsync()
                        }
                        mediaPlayer = mp
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                },
                // Always enabled (no dimming) — clicks are ignored while playing
                enabled = !disabled
            ) {
                Icon(
                    if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                    null,
                    tint = if (disabled) theme.primary.copy(alpha = 0.3f) else theme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/// Global registry — ensures only one AudioPlayerCard plays at a time.
/// When a new audio starts, it calls stopAllExcept() to stop all others.
private object AudioRegistry {
    private val players = mutableMapOf<String, () -> Unit>()

    fun registerPlayer(url: String, stopCallback: () -> Unit) {
        players[url] = stopCallback
    }

    fun unregisterPlayer(url: String) {
        players.remove(url)
    }

    fun stopAllExcept(currentUrl: String) {
        players.forEach { (url, stop) ->
            if (url != currentUrl) stop()
        }
    }
}

// ─── Answer input (different per question type) ───────────────────────────────
@Composable
fun AnswerInput(
    theme: AppTheme,
    question: QuestionDetail,
    userAnswer: Any?,
    feedback: QuestionFeedback?,
    onAnswer: (Any) -> Unit
) {
    val options = question.options ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            question.type == "SINGLE_CHOICE" || question.type == "TRUE_FALSE" -> {
                options.forEachIndexed { i, opt ->
                    val selected = userAnswer == opt
                    val isCorrectFeedback = feedback?.let { feedback.correctAnswer == opt }
                    val isWrongSelected = feedback != null && selected && !feedback.isCorrect
                    val bgColor = when {
                        isCorrectFeedback == true -> Color(0xFFD4EDDA)
                        isWrongSelected -> Color(0xFFFFCDD2)
                        selected -> theme.primary.copy(alpha = 0.1f)
                        else -> theme.cardBg
                    }
                    val borderColor = when {
                        isCorrectFeedback == true -> Color(0xFF28A745)
                        isWrongSelected -> theme.errorRed
                        selected -> theme.primary
                        else -> theme.divider
                    }
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (feedback == null) onAnswer(opt)
                        }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (selected) theme.primary else Color.Transparent,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.size(20.dp)
                            ) {
                                if (selected) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(opt, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (isCorrectFeedback == true) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF28A745))
                            } else if (isWrongSelected) {
                                Icon(Icons.Default.Cancel, null, tint = theme.errorRed)
                            }
                        }
                    }
                }
            }
            question.type == "MULTIPLE_CHOICE" -> {
                val selectedList = (userAnswer as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                options.forEachIndexed { i, opt ->
                    val selected = selectedList.contains(opt)
                    Surface(
                        color = if (selected) theme.primary.copy(alpha = 0.1f) else theme.cardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) theme.primary else theme.divider),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val newList = if (selected) selectedList - opt else selectedList + opt
                            onAnswer(newList)
                        }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (selected) theme.primary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(20.dp)
                            ) {
                                if (selected) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(opt, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            else -> {
                // ONE_WORD / SHORT_ANSWER / FILL_BLANK
                OutlinedTextField(
                    value = (userAnswer as? String) ?: "",
                    onValueChange = { onAnswer(it) },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.darkText,
                        unfocusedTextColor = theme.darkText,
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.divider,
                        cursorColor = theme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }
    }
}

// ─── Async image (loads from URL using Coil) ─────────────────────────────────
@Composable
fun AsyncImage(url: String, modifier: Modifier = Modifier) {
    val theme = rememberAppTheme()
    if (url.isBlank()) {
        Box(modifier = modifier.background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
        return
    }
    coil.compose.AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

// ─── Result screen ────────────────────────────────────────────────────────────
@Composable
fun ExamResultScreen(
    theme: AppTheme,
    result: SubmitResponse,
    onExit: () -> Unit,
    sound: SoundManager,
    examTitle: String = "Exam",
    examDescription: String? = null,
) {
    val context = LocalContext.current

    // ── FORCE PORTRAIT for the result screen ────────────────────────────
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    LaunchedEffect(Unit) { sound.success() }

    // ── MARKS CALCULATION ──────────────────────────────────────────────
    // Each question = 2.5 marks. Total marks = questionCount × 2.5.
    // Example: 40 questions = 100 marks total.
    // The server returns score = number of correct answers, so we convert
    // to marks by multiplying by 2.5.
    val MARKS_PER_QUESTION = 2.5
    val correctCount = result.review.count { it.isCorrect }
    val incorrectCount = result.review.size - correctCount
    val unansweredCount = result.review.count { it.userAnswer == null }
    val totalQuestions = result.review.size
    val totalMarks = totalQuestions * MARKS_PER_QUESTION
    val obtainedMarks = correctCount * MARKS_PER_QUESTION
    val pct = if (totalMarks > 0) (obtainedMarks / totalMarks * 100).toInt() else 0
    val passed = pct >= 40

    // ── Animation states ──────────────────────────────────────────────────
    var showScore by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showReviewSection by remember { mutableStateOf(false) }
    val animatedScore = animateFloatAsState(
        targetValue = if (showScore && totalMarks > 0) (obtainedMarks / totalMarks).toFloat() else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scoreAnim",
    )
    val statsAlpha by animateFloatAsState(
        targetValue = if (showStats) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 300),
        label = "statsAlpha",
    )
    val reviewAlpha by animateFloatAsState(
        targetValue = if (showReviewSection) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "reviewAlpha",
    )

    LaunchedEffect(Unit) {
        delay(200)
        showScore = true
        delay(400)
        showStats = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── DreamKorea logo at top centre ──────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                        contentDescription = "DreamKorea Logo",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "DreamKorea SmartClass",
                        color = theme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ── Exam title + description ──────────────────────────────────────
        item {
            Surface(
                color = theme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        examTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (!examDescription.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            examDescription,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "Exam Completed",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        // ── Score card (animated) ─────────────────────────────────────────
        item {
            Surface(
                color = theme.cardBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pass/Fail icon (animated scale-in)
                    val iconScale by animateFloatAsState(
                        targetValue = if (showScore) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "iconScale",
                    )
                    Surface(
                        color = if (passed) Color(0xFF4CAF50) else theme.errorRed,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(80.dp).scale(iconScale)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (passed) Icons.Default.Check else Icons.Default.Close,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (passed) "Congratulations!" else "Keep practicing",
                        color = theme.darkText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    // Animated percentage
                    val animatedPct = (animatedScore.value * 100).toInt()
                    Text(
                        "$animatedPct%",
                        color = if (passed) Color(0xFF4CAF50) else theme.errorRed,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${obtainedMarks} / $totalMarks marks", color = theme.subText, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))

                    // ── Stats row (total marks, correct, incorrect, unanswered) ──
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(statsAlpha),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox("Total", "$totalMarks", Color(0xFF6A1B9A), Modifier.weight(1f))
                        StatBox("Obtained", "$obtainedMarks", Color(0xFF4CAF50), Modifier.weight(1f))
                        StatBox("Wrong", "$incorrectCount", theme.errorRed, Modifier.weight(1f))
                        StatBox("Skipped", "$unansweredCount", Color(0xFFFF9800), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))

                    // ── Action buttons ────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sound.click(); showReviewSection = !showReviewSection },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.primary)
                        ) {
                            Icon(
                                if (showReviewSection) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (showReviewSection) "Hide Review" else "Review Exam",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = { sound.click(); onExit() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                        ) {
                            Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back to Tests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── Per-question review (collapsible) ─────────────────────────────
        if (showReviewSection) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().alpha(reviewAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.List, null, tint = theme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Question Review (${result.review.size})",
                        color = theme.darkText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            itemsIndexed(result.review.sortedWith(compareBy { if (it.blockType == "audio") 1 else 0 })) { idx, review ->
                ReviewCard(theme, review, idx + 1, sound)
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ReviewCard(theme: AppTheme, review: ReviewItem, questionNumber: Int = 0, sound: SoundManager? = null) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── Header: question number + correct/incorrect badge ─────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (questionNumber > 0) {
                    Surface(
                        color = if (review.isCorrect) Color(0xFF4CAF50).copy(alpha = 0.15f) else theme.errorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Q$questionNumber",
                            color = if (review.isCorrect) Color(0xFF4CAF50) else theme.errorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (review.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (review.isCorrect) Color(0xFF4CAF50) else theme.errorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (review.isCorrect) "Correct" else "Incorrect",
                    color = if (review.isCorrect) Color(0xFF4CAF50) else theme.errorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (review.userAnswer == null) {
                    Surface(color = Color(0xFFFF9800).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "Skipped",
                            color = Color(0xFFFF9800),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ── Question title (if set by admin) ────────────────────────
            if (!review.title.isNullOrBlank()) {
                Surface(
                    color = theme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        review.title,
                        color = theme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── Question stem ─────────────────────────────────────────────
            Text(
                review.stem.take(300),
                color = theme.darkText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            // ── Description section (after stem) ───────────────────────────
            // Description image — if descType == "image" && descImageUrl not blank
            if (review.descType == "image" && !review.descImageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                val url = review.descImageUrl!!.toAbsoluteUrl()
                coil.compose.AsyncImage(
                    model = url,
                    contentDescription = "Description image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            // Description text — left-aligned, 13sp
            if (!review.descText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        review.descText,
                        color = theme.darkText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // ── Media section (image / text / audio) ───────────────────────
            // Media image — uses mediaImageUrl ?: imageUrl
            val mediaImgUrl = (review.mediaImageUrl ?: review.imageUrl)?.toAbsoluteUrl()
            if (!mediaImgUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                coil.compose.AsyncImage(
                    model = mediaImgUrl,
                    contentDescription = "Question image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            // Media text — center-aligned, 13sp
            if (!review.mediaText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        review.mediaText,
                        color = theme.darkText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            // Media audio — uses mediaAudioUrl ?: audioUrl
            // Review audio: unlimited plays (exam is finished), one at a time
            val mediaAudUrl = (review.mediaAudioUrl ?: review.audioUrl)?.toAbsoluteUrl()
            if (!mediaAudUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                AudioPlayerCard(
                    theme = theme,
                    url = mediaAudUrl,
                    loopCount = 1,
                    loopDelaySec = 0,
                    sound = sound ?: rememberSoundManager(),
                    unlimited = true
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Options with correct/wrong highlighting ──────────────────
            // If option is an image URL, render as image; otherwise render as text.
            review.options?.let { opts ->
                opts.forEachIndexed { i, opt ->
                    val isUserAns = (review.userAnswer == opt) || ((review.userAnswer as? List<*>)?.contains(opt) == true)
                    val isCorrectAns = (review.correctAnswer == opt) || ((review.correctAnswer as? List<*>)?.contains(opt) == true)
                    val bg = when {
                        isCorrectAns -> Color(0xFFD4EDDA)
                        isUserAns && !isCorrectAns -> Color(0xFFFFCDD2)
                        else -> Color.Transparent
                    }
                    val borderColor = when {
                        isCorrectAns -> Color(0xFF28A745)
                        isUserAns && !isCorrectAns -> theme.errorRed
                        else -> Color(0xFFE0E0E0)
                    }
                    // Check if this option is an image URL or audio URL
                    val optImg = review.optionImages.getOrNull(i)?.takeIf { it.isNotBlank() }
                    val optAud = review.optionAudios.getOrNull(i)?.takeIf { it.isNotBlank() }
                    val isImageUrl = optImg != null || opt.startsWith("http") || opt.startsWith("/api/files") || opt.startsWith("/uploads")
                    val isAudioUrl = optAud != null
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${'A' + i}.", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            if (isAudioUrl) {
                                val audUrl = optAud!!.toAbsoluteUrl()
                                AudioPlayerCard(theme = theme, url = audUrl, loopCount = 1, loopDelaySec = 0, sound = sound ?: rememberSoundManager())
                            } else if (isImageUrl) {
                                // Render as image — use optionImages if available, otherwise the option text IS the URL
                                val imgUrl = (optImg ?: opt).toAbsoluteUrl()
                                coil.compose.AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Option ${'A' + i}",
                                    modifier = Modifier.weight(1f).heightIn(max = 120.dp).clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                // Render as text
                                Text(opt, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            }
                            if (isCorrectAns) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF28A745), modifier = Modifier.size(14.dp))
                            } else if (isUserAns && !isCorrectAns) {
                                Icon(Icons.Default.Close, null, tint = theme.errorRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // ── User answer vs correct answer summary ─────────────────────
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = theme.errorRed.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Your Answer", color = theme.subText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        AnswerDisplay(review.userAnswer, theme, sound)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Correct Answer", color = Color(0xFF4CAF50), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        AnswerDisplay(review.correctAnswer, theme, sound)
                    }
                }
            }

            // ── Explanation ───────────────────────────────────────────────
            if (!review.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = theme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(6.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = theme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Explanation", color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(review.explanation, color = theme.darkText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Formats an answer (String or List<String>) for display. */
private fun formatAnswer(answer: Any?): String? {
    if (answer == null) return null
    return when (answer) {
        is String -> answer
        is List<*> -> answer.joinToString(", ")
        else -> answer.toString()
    }
}

/// Renders an answer — shows media (image/audio) for URL answers, text otherwise.
@Composable
private fun AnswerDisplay(answer: Any?, theme: AppTheme, sound: SoundManager? = null) {
    if (answer == null) {
        Text("—", color = theme.subText, fontSize = 12.sp)
        return
    }
    when (answer) {
        is String -> {
            val url = answer.trim()
            if (url.startsWith("http") && (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png") || url.contains(".gif") || url.contains(".webp"))) {
                coil.compose.AsyncImage(
                    model = url, contentDescription = "Answer image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (url.startsWith("http") && (url.contains(".mp3") || url.contains(".wav") || url.contains(".ogg") || url.contains(".m4a"))) {
                AudioPlayerCard(theme = theme, url = url, loopCount = 1, loopDelaySec = 0, sound = sound ?: rememberSoundManager())
            } else {
                Text(url, color = theme.darkText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        is List<*> -> {
            Column { answer.forEach { Text(it?.toString() ?: "", color = theme.darkText, fontSize = 12.sp) } }
        }
        else -> Text(answer.toString(), color = theme.darkText, fontSize = 12.sp)
    }
}

@Composable
fun AnswerInputBlock(
    theme: AppTheme,
    question: QuestionDetail,
    userAnswer: Any?,
    feedback: QuestionFeedback?,
    sound: SoundManager,
    onAnswer: (Any) -> Unit
) {
    val options = question.options ?: emptyList()
    val optionImgs = question.optionImages
    val optionAuds = question.optionAudios

    when (question.answerType) {
        "text", "choose" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { i, opt ->
                    val selected = userAnswer == opt
                    val isCorrectFeedback = feedback?.let { i == question.correctOption }
                    val isWrongSelected = feedback != null && selected && !feedback.isCorrect
                    val bgColor = when {
                        isCorrectFeedback == true -> Color(0xFFD4EDDA)
                        isWrongSelected -> Color(0xFFFFCDD2)
                        selected -> theme.primary.copy(alpha = 0.1f)
                        else -> theme.cardBg
                    }
                    val borderColor = when {
                        isCorrectFeedback == true -> Color(0xFF28A745)
                        isWrongSelected -> theme.errorRed
                        selected -> theme.primary
                        else -> theme.divider
                    }
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (feedback == null) { sound.click(); onAnswer(opt) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = if (selected) theme.primary else Color.Transparent, shape = RoundedCornerShape(50), modifier = Modifier.size(20.dp)) {
                                if (selected) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) } }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(opt, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        "image" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                optionImgs.forEachIndexed { i, imgUrl ->
                    if (imgUrl.isBlank()) return@forEachIndexed
                    val selected = userAnswer == imgUrl
                    val isCorrectFeedback = feedback?.let { i == question.correctOption }
                    val isWrongSelected = feedback != null && selected && !feedback.isCorrect
                    val borderColor = when {
                        isCorrectFeedback == true -> Color(0xFF28A745)
                        isWrongSelected -> theme.errorRed
                        selected -> theme.primary
                        else -> theme.divider
                    }
                    Surface(
                        color = if (selected) theme.primary.copy(alpha = 0.05f) else theme.cardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (feedback == null) { sound.click(); onAnswer(imgUrl) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            coil.compose.AsyncImage(
                                model = imgUrl.toAbsoluteUrl(),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(question.options?.getOrNull(i) ?: "Option ${'A' + i}", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            if (selected) { Icon(Icons.Default.CheckCircle, null, tint = theme.primary, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
        "audio" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                optionAuds.forEachIndexed { i, audUrl ->
                    if (audUrl.isBlank()) return@forEachIndexed
                    val selected = userAnswer == audUrl
                    val isCorrectFeedback = feedback?.let { i == question.correctOption }
                    val isWrongSelected = feedback != null && selected && !feedback.isCorrect
                    val borderColor = when {
                        isCorrectFeedback == true -> Color(0xFF28A745)
                        isWrongSelected -> theme.errorRed
                        selected -> theme.primary
                        else -> theme.divider
                    }
                    Surface(
                        color = if (selected) theme.primary.copy(alpha = 0.05f) else theme.cardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (feedback == null) { sound.click(); onAnswer(audUrl) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(question.options?.getOrNull(i) ?: "Audio ${'A' + i}", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            if (selected) { Icon(Icons.Default.CheckCircle, null, tint = theme.primary, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
        else -> {
            // Fallback: text options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { i, opt ->
                    val selected = userAnswer == opt
                    Surface(
                        color = if (selected) theme.primary.copy(alpha = 0.1f) else theme.cardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) theme.primary else theme.divider),
                        modifier = Modifier.fillMaxWidth().clickable { if (feedback == null) { sound.click(); onAnswer(opt) } }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(opt, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

fun String.toAbsoluteUrl(): String {
    if (this.startsWith("http://") || this.startsWith("https://")) return this
    return "https://my-project-five-sepia.vercel.app" + (if (this.startsWith("/")) this else "/$this")
}

/**
 * Builds an AnnotatedString where the [blankWord] is underlined within [text].
 * If blankWord is null/empty, returns the plain text.
 * The underlined word is case-insensitive matched.
 */
fun buildUnderlinedText(text: String, blankWord: String?): androidx.compose.ui.text.AnnotatedString {
    if (blankWord.isNullOrBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val idx = text.indexOf(blankWord, ignoreCase = true)
    if (idx < 0) return androidx.compose.ui.text.AnnotatedString(text)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
            append(text.substring(idx, idx + blankWord.length))
        }
        append(text.substring(idx + blankWord.length))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT-SIDE SUBMIT FALLBACK — when the server's submit endpoint doesn't
// know how to handle combined exam IDs (qbank-combined / bundle-{id}), we
// grade the exam locally using the question data we already have loaded.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Grades a combined exam client-side. Used as a fallback when
 * /api/student/tests/[testId]/submit returns 404 for combined exam IDs.
 *
 * Grades SINGLE_CHOICE, TRUE_FALSE, ONE_WORD, FILL_BLANK, MULTIPLE_CHOICE.
 * Subjective types (SHORT_ANSWER, LONG_ANSWER, MATCHING) are marked as
 * incorrect (the student would need a teacher to review them).
 */
private fun gradeCombinedExamClientSide(
    test: TestDetail,
    answers: Map<String, Any>,
): SubmitResponse {
    var score = 0
    val review = mutableListOf<ReviewItem>()

    // Normalize URL — strip origin so /api/files/... matches https://.../api/files/...
    fun normalizeUrl(val_: Any?): Any? {
        if (val_ is String) {
            return try {
                if (val_.startsWith("http://") || val_.startsWith("https://")) {
                    java.net.URI(val_).path
                } else val_
            } catch (_: Exception) { val_ }
        }
        return val_
    }

    for (item in test.items) {
        val q = item.question
        val ans = normalizeUrl(answers[q.id])
        var isCorrect = false

        when (q.type) {
            "SINGLE_CHOICE", "TRUE_FALSE", "ONE_WORD", "FILL_BLANK" -> {
                val correctIdx = q.correctOption
                val correctAns = normalizeUrl(q.options?.getOrNull(correctIdx)
                    ?: q.options?.getOrNull(0)
                    ?: "") as? String ?: ""
                if (ans is String && correctAns.isNotEmpty() &&
                    ans.trim().equals(correctAns.trim(), ignoreCase = true)) {
                    isCorrect = true
                    score++
                }
            }
            "MULTIPLE_CHOICE" -> {
                val correctIdx = q.correctOption
                if (ans is String && ans.toIntOrNull() == correctIdx) {
                    isCorrect = true
                    score++
                }
            }
        }

        review.add(
            ReviewItem(
                questionId = q.id,
                stem = q.stem,
                title = q.title,
                type = q.type,
                options = q.options,
                optionImages = q.optionImages,
                optionAudios = q.optionAudios,
                imageUrl = q.imageUrl,
                audioUrl = q.audioUrl,
                audioLoop = q.audioLoop,
                audioLoopDelay = q.audioLoopDelay,
                descType = q.descType,
                descText = q.descText,
                descImageUrl = q.descImageUrl,
                mediaType = q.mediaType,
                mediaText = q.mediaText,
                mediaImageUrl = q.mediaImageUrl,
                mediaAudioUrl = q.mediaAudioUrl,
                answerType = q.answerType,
                blockType = q.blockType,
                userAnswer = ans,
                correctAnswer = q.options?.getOrNull(q.correctOption),
                explanation = q.explanation,
                isCorrect = isCorrect,
            )
        )
    }

    val maxScore = test.items.size
    val pct = if (maxScore > 0) (score.toDouble() / maxScore) * 100 else 0.0

    // Eye vision recommendation — same logic as server
    val eyeVision = if (maxScore > 0) {
        val mistakesPct = 100 - pct
        when {
            mistakesPct > 30 -> {
                val count = if (mistakesPct >= 70) 5 else if (mistakesPct >= 50) 4 else 3
                EyeVisionRecommendation(
                    show = true,
                    count = count,
                    reason = "You made ${Math.round(mistakesPct)}% mistakes. Let's check your eye vision with $count quick tests.",
                )
            }
            mistakesPct >= 15 -> {
                EyeVisionRecommendation(
                    show = true,
                    count = 2,
                    reason = "You made ${Math.round(mistakesPct)}% mistakes. A quick eye vision check is recommended.",
                )
            }
            else -> EyeVisionRecommendation()
        }
    } else EyeVisionRecommendation()

    return SubmitResponse(
        score = score,
        maxScore = maxScore,
        graded = true,
        submissionId = "client-graded-${System.currentTimeMillis()}",
        review = review,
        eyeVision = eyeVision,
        completed = true,
    )
}

/**
 * Submits a combined exam with fallback: tries the server first, and if the
 * server returns 404 (endpoint not deployed for combined IDs), grades the
 * exam client-side instead.
 */
private suspend fun submitCombinedExamWithFallback(
    test: TestDetail,
    answers: Map<String, Any>,
): SubmitResponse {
    return try {
        AppState.api.submitTest(test.id, SubmitRequest(answers))
    } catch (e: retrofit2.HttpException) {
        // ANY server error (404, 500, 403, etc.) → fall back to client-side grading.
        gradeCombinedExamClientSide(test, answers)
    } catch (e: Exception) {
        // Network errors, timeouts, etc. → also fall back to client-side grading
        // so the student always gets a result.
        gradeCombinedExamClientSide(test, answers)
    }
}

/**
 * Submits ANY exam (combined or normal) with client-side fallback.
 * If the server returns ANY error, grades locally so the student always
 * gets a result. Used by all submit buttons to prevent HTTP errors.
 */
private suspend fun submitExamWithFallback(
    test: TestDetail,
    answers: Map<String, Any>,
): SubmitResponse {
    return try {
        AppState.api.submitTest(test.id, SubmitRequest(answers))
    } catch (e: retrofit2.HttpException) {
        gradeCombinedExamClientSide(test, answers)
    } catch (e: Exception) {
        gradeCombinedExamClientSide(test, answers)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EYE TEST GATE — shows when user skips 2+ questions
// Displays a number (0-9) in a gradient circle and asks the user to
// identify it. Simple vision screening before showing results.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EyeTestGateScreen(
    theme: AppTheme,
    sound: SoundManager,
    count: Int,
    reason: String,
    onContinue: () -> Unit,
) {
    // Generate random numbers 0-9 for the test
    var currentTestIdx by remember { mutableStateOf(0) }
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    val testNumbers = remember { (1..count).map { (0..9).random() } }

    if (currentTestIdx >= testNumbers.size) {
        // All tests done — continue to result
        onContinue()
        return
    }

    val currentNumber = testNumbers[currentTestIdx]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Header
        Text(
            "Eye Vision Check",
            color = Color(0xFF003478),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            reason,
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Test ${currentTestIdx + 1} of ${testNumbers.size}",
            color = Color(0xFF003478),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(24.dp))

        // ── Number display circle ──────────────────────────────────────
        // Gradient circle with the number — simulates an eye chart
        Surface(
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(200.dp),
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(Color(0xFF003478), Color(0xFF1E40AF), Color(0xFF3B82F6))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$currentNumber",
                    color = Color.White,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "What number do you see?",
            color = Color(0xFF1E293B),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        // ── Number selection grid 0-9 ──────────────────────────────────
        // 5 columns × 2 rows = 10 buttons (0-9)
        val numberRows = listOf(0, 1, 2, 3, 4).chunked(1) to listOf(5, 6, 7, 8, 9).chunked(1)
        val allNumbers = (0..9).toList()
        val rows = allNumbers.chunked(5)

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { num ->
                    val isSelected = selectedNumber == num
                    Surface(
                        color = if (isSelected) Color(0xFF003478) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF003478)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable {
                                sound.click()
                                selectedNumber = num
                                // Auto-advance after selection
                                if (num == currentNumber) {
                                    sound.success()
                                }
                                // Move to next test after a short delay
                                selectedNumber = num
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                "$num",
                                color = if (isSelected) Color.White else Color(0xFF003478),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ── Next / Skip button ─────────────────────────────────────────
        Button(
            onClick = {
                sound.swoosh()
                currentTestIdx++
                selectedNumber = null
            },
            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003478)),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedNumber != null,
        ) {
            Text(
                if (currentTestIdx < testNumbers.size - 1) "Next" else "Finish",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Skip button
        TextButton(onClick = { onContinue() }) {
            Text("Skip eye test", color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}
