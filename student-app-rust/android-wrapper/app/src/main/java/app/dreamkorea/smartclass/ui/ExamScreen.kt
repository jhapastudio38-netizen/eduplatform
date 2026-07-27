package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    var test by remember { mutableStateOf<TestDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var currentIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Any>() }
    var submitResult by remember { mutableStateOf<SubmitResponse?>(null) }
    var submitting by remember { mutableStateOf(false) }
    // Per-question feedback (after answering, before moving on)
    var questionFeedback by remember { mutableStateOf<QuestionFeedback?>(null) }
    // Timer
    var timeLeft by remember { mutableStateOf(0) }
    // Retry trigger — increment to force reload
    var retryCount by remember { mutableStateOf(0) }

    // ─── Force landscape for the entire exam — NO vertical option ────────
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Load test detail
    LaunchedEffect(testId, retryCount) {
        loading = true
        error = ""
        try {
            val result = withTimeoutOrNull(20_000L) {
                if (testId == "qbank-combined") {
                    AppState.api.getQBankCombined().test
                } else {
                    AppState.api.getTestDetail(testId).test
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
                    submitResult = AppState.api.submitTest(currentTest.id, SubmitRequest(answers.toMap()))
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

    // ─── Result screen — show in PORTRAIT (not landscape) ──────────────────
    if (submitResult != null) {
        // Restore portrait for the result screen — user sees results vertically
        DisposableEffect(Unit) {
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDispose { }
        }
        ExamResultScreen(theme, submitResult!!, onExit, sound)
        return
    }

    // ─── TWO-VIEW EXAM (landscape) ────────────────────────────────────────
    var examView by remember { mutableStateOf("grid") }

    if (examView == "grid") {
        ExamOverviewScreen(
            theme = theme, sound = sound, test = t, answers = answers,
            timeLeft = timeLeft, submitting = submitting,
            onBlockTap = { idx -> currentIdx = idx; questionFeedback = null; examView = "question" },
            onSubmit = {
                sound.swoosh(); submitting = true
                scope.launch { try { submitResult = AppState.api.submitTest(t.id, SubmitRequest(answers.toMap())); sound.success() } catch (e: Exception) { error = "Submit failed." }; submitting = false }
            },
            onExit = onExit,
        )
    } else {
        ExamAnswerScreen(
            theme = theme, sound = sound, test = t, answers = answers,
            currentIdx = currentIdx, questionFeedback = null, // NEVER show feedback during exam
            timeLeft = timeLeft, submitting = submitting,
            onAnswer = { qid, ans ->
                answers[qid] = ans
                // Do NOT check correctness during the exam — only after submit
            },
            onNext = { if (currentIdx < t.items.size - 1) { currentIdx++; questionFeedback = null } },
            onPrev = { if (currentIdx > 0) { currentIdx--; questionFeedback = null } },
            onBackToGrid = { examView = "grid" },
            onSubmit = {
                sound.swoosh(); submitting = true
                scope.launch { try { submitResult = AppState.api.submitTest(t.id, SubmitRequest(answers.toMap())); sound.success() } catch (e: Exception) { error = "Submit failed." }; submitting = false }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OVERVIEW SCREEN — Question grid with Reading + Listening panels
// Spec: 5×4 grid, large blocks (~60dp), black borders, top nav bar,
// section headers, Submit button at bottom.
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExamOverviewScreen(
    theme: AppTheme, sound: SoundManager, test: TestDetail,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    timeLeft: Int, submitting: Boolean,
    onBlockTap: (Int) -> Unit, onSubmit: () -> Unit, onExit: () -> Unit,
) {
    val textItems = test.items.filter { it.question.blockType == "text" }
    val audioItems = test.items.filter { it.question.blockType == "audio" }
    val readingItems = if (textItems.isNotEmpty()) textItems else test.items
    val listeningItems = if (textItems.isNotEmpty()) audioItems else emptyList()
    var filter by remember { mutableStateOf("all") }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(6.dp)) {
        // ── TOP NAV BAR ──
        // Logo | Title | All/Solved/Unsolved tabs | Timer
        Surface(border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(theme.primary), contentAlignment = Alignment.Center) {
                    Text("DK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                // Title
                Text(test.title.take(20), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(Modifier.weight(1f))
                // Tabs
                listOf("all" to "All", "solved" to "Solved", "unsolved" to "Unsolved").forEach { (k, label) ->
                    Surface(
                        color = if (filter == k) Color(0xFFE0E0E0) else Color.White,
                        modifier = Modifier.clickable { sound.click(); filter = k }
                    ) {
                        Text(label, color = Color.Black, fontSize = 10.sp, fontWeight = if (filter == k) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Divider
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color.Black))
                Spacer(Modifier.width(8.dp))
                // Timer
                val mm = timeLeft / 60; val ss = timeLeft % 60
                Text(String.format("%02d:%02d", mm, ss), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── SECTION HEADERS ── compact, no overlap
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black), modifier = Modifier.weight(1f)) {
                Text("Reading", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp))
            }
            if (listeningItems.isNotEmpty()) {
                Surface(border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black), modifier = Modifier.weight(1f)) {
                    Text("Listening", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── QUESTION GRIDS ── two panels side by side, 5×4 each, large blocks
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reading panel
            Surface(
                border = androidx.compose.foundation.BorderStroke(3.dp, Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                LargeGridBlocks(theme, sound, readingItems, 0, answers, filter, onBlockTap)
            }
            // Listening panel
            if (listeningItems.isNotEmpty()) {
                Surface(
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    LargeGridBlocks(theme, sound, listeningItems, readingItems.size, answers, filter, onBlockTap)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // ── SUBMIT BUTTON ── centered, large, blue
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally).height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
            shape = RoundedCornerShape(12.dp),
            enabled = !submitting
        ) {
            if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
            else { Text("Submit and Finish Exam", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(4.dp))
        // Exit button (small, bottom)
        OutlinedButton(onClick = onExit, modifier = Modifier.align(Alignment.CenterHorizontally), shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
            Text("Exit", fontSize = 10.sp)
        }
    }
}

// ─── Large Grid Blocks — 5 columns, big blocks (~55dp each) ──────────────────
@Composable
private fun LargeGridBlocks(
    theme: AppTheme, sound: SoundManager,
    items: List<TestItemDetail>, startIndex: Int,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    filter: String, onTap: (Int) -> Unit,
) {
    val display = items.mapIndexed { i, item -> Triple(startIndex + i, item, answers.containsKey(item.question.id)) }
        .filter { (_, _, answered) -> when (filter) { "solved" -> answered; "unsolved" -> !answered; else -> true } }
    val rows = display.chunked(5)
    Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (idx, _, answered) ->
                    Surface(
                        color = if (answered) Color(0xFFC8E6C9) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f).clickable { sound.click(); onTap(idx) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ANSWER SCREEN — Full-screen question with optimized layout
// Spec: top status bar, instruction bar, 60/40 split (question left, answers
// right), centered question card, 4 answers with number circles, bottom nav.
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExamAnswerScreen(
    theme: AppTheme, sound: SoundManager, test: TestDetail,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    currentIdx: Int, questionFeedback: QuestionFeedback?,
    timeLeft: Int, submitting: Boolean,
    onAnswer: (String, Any) -> Unit,
    onNext: () -> Unit, onPrev: () -> Unit,
    onBackToGrid: () -> Unit, onSubmit: () -> Unit,
) {
    val item = test.items.getOrNull(currentIdx) ?: return
    val q = item.question
    val options = q.options ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // ── TOP STATUS BAR ── Reading | Total | Remaining | Timer
        Surface(border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                // Reading count
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Reading", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Total
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Total: ${test.items.size}", color = Color.Black, fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Remaining
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Remaining: ${test.items.size - answers.size}", color = Color.Black, fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Timer
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val mm = timeLeft / 60; val ss = timeLeft % 60
                    Text(String.format("%02d:%02d", mm, ss), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── INSTRUCTION BAR ── question number + grid button
        Surface(border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Q${currentIdx + 1}. ${q.stem.take(60)}", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                IconButton(onClick = onBackToGrid, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.GridView, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))

        // ── MAIN CONTENT ── 60% question | 40% answers
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // LEFT: Question panel (60%)
            Column(
                modifier = Modifier.weight(0.6f).fillMaxHeight().padding(6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Question card — centered, bordered
                Surface(
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        q.stem,
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Media images — scale to FIT (not crop), so nothing gets cut
                if (q.descType == "image" && !q.descImageUrl.isNullOrBlank()) {
                    val descUrl = q.descImageUrl!!.toAbsoluteUrl()
                    coil.compose.AsyncImage(
                        model = descUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 120.dp).clip(RoundedCornerShape(6.dp)).clickable { FullScreenImageViewer.show(descUrl) },
                        contentScale = ContentScale.Fit
                    )
                }
                val mediaImgUrl = (q.mediaImageUrl ?: q.imageUrl)?.toAbsoluteUrl()
                if (!mediaImgUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = mediaImgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 140.dp).clip(RoundedCornerShape(6.dp)).clickable { FullScreenImageViewer.show(mediaImgUrl) },
                        contentScale = ContentScale.Fit
                    )
                }
                val mediaAudUrl = (q.mediaAudioUrl ?: q.audioUrl)?.toAbsoluteUrl()
                if (!mediaAudUrl.isNullOrBlank()) {
                    AudioPlayerCard(theme = theme, url = mediaAudUrl, loopCount = q.audioLoop, loopDelaySec = q.audioLoopDelay, sound = sound)
                }
            }

            // Vertical divider
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))

            // RIGHT: Answer panel (40%) — handles text, image, and audio options
            Column(
                modifier = Modifier.weight(0.4f).fillMaxHeight().padding(6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                when (q.answerType) {
                    "text", "choose" -> {
                        // Text options with number circles
                        (0 until minOf(4, options.size)).forEach { i ->
                            val isSelected = answers[q.id] == options.getOrNull(i)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { sound.click(); onAnswer(q.id, options.getOrNull(i) ?: "") },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = if (isSelected) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(options.getOrNull(i) ?: "", color = Color.Black, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    "image" -> {
                        // Image options — show as thumbnails, tap to select + tap image to zoom
                        (0 until minOf(4, q.optionImages.size)).forEach { i ->
                            val imgUrl = q.optionImages[i]
                            if (imgUrl.isBlank()) return@forEach
                            val absUrl = imgUrl.toAbsoluteUrl()
                            val isSelected = answers[q.id] == absUrl
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { sound.click(); onAnswer(q.id, absUrl) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = if (isSelected) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                // Image thumbnail — tap to open full-screen viewer
                                coil.compose.AsyncImage(
                                    model = absUrl,
                                    contentDescription = "Option ${i + 1}",
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)).clickable { FullScreenImageViewer.show(absUrl) },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    "audio" -> {
                        // Audio options — play button + select
                        (0 until minOf(4, q.optionAudios.size)).forEach { i ->
                            val audUrl = q.optionAudios[i]
                            if (audUrl.isBlank()) return@forEach
                            val absUrl = audUrl.toAbsoluteUrl()
                            val isSelected = answers[q.id] == absUrl
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { sound.click(); onAnswer(q.id, absUrl) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = if (isSelected) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                AudioPlayerCard(theme = theme, url = absUrl, loopCount = 1, loopDelaySec = 0, sound = sound)
                            }
                        }
                    }
                }
            }
        }

        // ── BOTTOM NAVIGATION ── Prev | Next/Submit, aligned right
        Surface(border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onPrev, enabled = currentIdx > 0,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) { Text("Prev", fontSize = 11.sp) }
                Spacer(Modifier.width(8.dp))
                if (currentIdx < test.items.size - 1) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                    ) { Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = onSubmit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        enabled = !submitting,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                        else { Text("Submit", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

data class QuestionFeedback(val isCorrect: Boolean, val correctAnswer: String)

// ─── Audio player with loop support ───────────────────────────────────────────
@Composable
fun AudioPlayerCard(theme: AppTheme, url: String, loopCount: Int, loopDelaySec: Int, sound: SoundManager) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playCount by remember { mutableStateOf(0) }
    val totalPlays = if (loopCount == -1) "∞" else (loopCount + 1).toString()
    val scope = rememberCoroutineScope()

    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = theme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Headphones, null, tint = theme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Audio question", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (loopCount == -1) "Loops continuously"
                        else if (loopCount == 0) "Plays once"
                        else "Plays ${loopCount + 1} times • ${loopDelaySec}s delay",
                        color = theme.subText, fontSize = 11.sp
                    )
                }
                IconButton(onClick = {
                    sound.click()
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        // Play with looping
                        try {
                            mediaPlayer?.release()
                            val mp = android.media.MediaPlayer().apply {
                                setDataSource(url)
                                setOnPreparedListener {
                                    start()
                                    isPlaying = true
                                    playCount = 1
                                }
                                setOnCompletionListener {
                                    if (loopCount == -1 || playCount < loopCount + 1) {
                                        // Schedule next play (with delay if set)
                                        scope.launch {
                                            if (loopDelaySec > 0) delay(loopDelaySec * 1000L)
                                            if (loopCount == -1 || playCount < loopCount + 1) {
                                                playCount++
                                                start()
                                            } else {
                                                isPlaying = false
                                            }
                                        }
                                    } else {
                                        isPlaying = false
                                    }
                                }
                                setOnErrorListener { _, _, _ ->
                                    isPlaying = false
                                    true
                                }
                                prepareAsync()
                            }
                            mediaPlayer = mp
                        } catch (_: Exception) {
                            isPlaying = false
                        }
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = theme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            if (loopCount > 0 || loopCount == -1) {
                Spacer(Modifier.height(6.dp))
                Text("Play $playCount / $totalPlays", color = theme.subText, fontSize = 10.sp)
            }
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
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Result screen ────────────────────────────────────────────────────────────
@Composable
fun ExamResultScreen(theme: AppTheme, result: SubmitResponse, onExit: () -> Unit, sound: SoundManager) {
    LaunchedEffect(Unit) { sound.success() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score card
        item {
            Surface(
                color = theme.cardBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val pct = if (result.maxScore > 0) (result.score * 100 / result.maxScore) else 0
                    val passed = pct >= 40
                    Surface(
                        color = if (passed) Color(0xFF4CAF50) else theme.errorRed,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(80.dp)
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
                    Text("$pct%", color = if (passed) Color(0xFF4CAF50) else theme.errorRed, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text("${result.score} / ${result.maxScore} points", color = theme.subText, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { sound.click(); onExit() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                    ) {
                        Text("Back to tests", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Per-question review
        item {
            Text("Review", color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(result.review) { review ->
            ReviewCard(theme, review)
        }
    }
}

@Composable
fun ReviewCard(theme: AppTheme, review: ReviewItem) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (review.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (review.isCorrect) Color(0xFF4CAF50) else theme.errorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (review.isCorrect) "Correct" else "Incorrect",
                    color = if (review.isCorrect) Color(0xFF4CAF50) else theme.errorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(review.stem.take(200), color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            review.options?.let { opts ->
                opts.forEachIndexed { i, opt ->
                    val isUserAns = (review.userAnswer == opt) || ((review.userAnswer as? List<*>)?.contains(opt) == true)
                    val isCorrectAns = (review.correctAnswer == opt) || ((review.correctAnswer as? List<*>)?.contains(opt) == true)
                    val bg = when {
                        isCorrectAns -> Color(0xFFD4EDDA)
                        isUserAns && !isCorrectAns -> Color(0xFFFFCDD2)
                        else -> Color.Transparent
                    }
                    Surface(color = bg, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${'A' + i}.", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(opt, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (isCorrectAns) Icon(Icons.Default.Check, null, tint = Color(0xFF28A745), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (!review.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = theme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(6.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Explanation", color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(review.explanation, color = theme.darkText, fontSize = 12.sp)
                    }
                }
            }
        }
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
