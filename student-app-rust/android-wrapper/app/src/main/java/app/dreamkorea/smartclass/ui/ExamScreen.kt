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

    // Load test detail — uses LaunchedEffect's own scope, finally block guarantees loading cleanup
    LaunchedEffect(testId, retryCount) {
        loading = true
        error = ""
        try {
            // 20-second timeout — if the API hangs, show a timeout error
            val result = withTimeoutOrNull(20_000L) {
                AppState.api.getTestDetail(testId).test
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

    // ─── Result screen ──────────────────────────────────────────────────────
    if (submitResult != null) {
        ExamResultScreen(theme, submitResult!!, onExit, sound)
        return
    }

    // ─── EXAM VIEW (landscape) ─────────────────────────────────────────────
    // Two views:
    // 1. GRID OVERVIEW: Reading (1-20) + Listening (21-40) grids, sidebar with
    //    timer/filters/Submit. Student taps a block to start answering.
    // 2. FULL-SCREEN QUESTION: NO grid, NO numbers, NO Reading/Listening labels.
    //    Just: exam title (small), timer (small, top-right), question text,
    //    answer options, Next button. Clean, focused, full screen.
    var examView by remember { mutableStateOf("grid") } // "grid" | "question"

    if (examView == "grid") {
        ExamGridOverview(
            theme = theme,
            sound = sound,
            test = t,
            answers = answers,
            timeLeft = timeLeft,
            submitting = submitting,
            onBlockTap = { idx ->
                currentIdx = idx
                questionFeedback = null
                examView = "question"
            },
            onSubmit = {
                sound.swoosh()
                submitting = true
                scope.launch {
                    try {
                        submitResult = AppState.api.submitTest(t.id, SubmitRequest(answers.toMap()))
                        sound.success()
                    } catch (e: Exception) { error = "Could not submit exam." }
                    submitting = false
                }
            },
            onExit = onExit,
        )
    } else {
        ExamFullScreenQuestion(
            theme = theme,
            sound = sound,
            test = t,
            answers = answers,
            currentIdx = currentIdx,
            questionFeedback = questionFeedback,
            timeLeft = timeLeft,
            submitting = submitting,
            onAnswer = { qid, ans ->
                answers[qid] = ans
                val q = t.items.find { it.question.id == qid }?.question
                if (q != null) {
                    val correctIdx = q.correctOption
                    val isCorrect = when {
                        q.answerType == "text" || q.answerType == "choose" -> q.options?.getOrNull(correctIdx) == ans
                        q.answerType == "image" -> q.optionImages.getOrNull(correctIdx) == ans
                        q.answerType == "audio" -> q.optionAudios.getOrNull(correctIdx) == ans
                        else -> false
                    }
                    questionFeedback = QuestionFeedback(isCorrect, "")
                }
            },
            onNext = {
                if (currentIdx < t.items.size - 1) {
                    currentIdx++
                    questionFeedback = null
                }
            },
            onPrev = {
                if (currentIdx > 0) {
                    currentIdx--
                    questionFeedback = null
                }
            },
            onBackToGrid = {
                examView = "grid"
            },
            onSubmit = {
                sound.swoosh()
                submitting = true
                scope.launch {
                    try {
                        submitResult = AppState.api.submitTest(t.id, SubmitRequest(answers.toMap()))
                        sound.success()
                    } catch (e: Exception) { error = "Could not submit exam." }
                    submitting = false
                }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GRID OVERVIEW — Reading + Listening grids + sidebar with timer/Submit
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExamGridOverview(
    theme: AppTheme,
    sound: SoundManager,
    test: TestDetail,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    timeLeft: Int,
    submitting: Boolean,
    onBlockTap: (Int) -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
) {
    val textItems = test.items.filter { it.question.blockType == "text" }
    val audioItems = test.items.filter { it.question.blockType == "audio" }
    val readingItems = if (textItems.isNotEmpty()) textItems else test.items
    val listeningItems = if (textItems.isNotEmpty()) audioItems else emptyList()
    var filter by remember { mutableStateOf("all") }

    Row(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // LEFT: Grids
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (readingItems.isNotEmpty()) {
                Text("Reading", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                GridSection(theme, sound, readingItems, 0, answers, filter, onBlockTap)
            }
            if (listeningItems.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Listening", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                GridSection(theme, sound, listeningItems, readingItems.size, answers, filter, onBlockTap)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(8.dp),
                enabled = !submitting
            ) {
                if (submitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit and Finish Exam", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // RIGHT: Sidebar
        Surface(color = theme.cardBg, modifier = Modifier.width(130.dp).fillMaxHeight(), shadowElevation = 4.dp) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // DK logo + title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(theme.primary), contentAlignment = Alignment.Center) {
                        Text("DK", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(test.title.take(18), color = theme.subText, fontSize = 8.sp, maxLines = 2, textAlign = TextAlign.Center)
                }
                // Timer + progress
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mm = timeLeft / 60; val ss = timeLeft % 60
                    Text(String.format("%02d:%02d", mm, ss), color = theme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("time left", color = theme.subText, fontSize = 8.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("${answers.size}/${test.items.size}", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("answered", color = theme.subText, fontSize = 8.sp)
                }
                // Filters
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("all" to "All", "solved" to "Solved", "unsolved" to "Unsolved").forEach { (k, label) ->
                        Surface(
                            color = if (filter == k) theme.primary else theme.background,
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.fillMaxWidth().clickable { sound.click(); filter = k }
                        ) {
                            Text(label, color = if (filter == k) Color.White else theme.subText, fontSize = 9.sp, fontWeight = if (filter == k) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    Text("Exit", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun GridSection(
    theme: AppTheme, sound: SoundManager,
    items: List<TestItemDetail>, startIndex: Int,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    filter: String, onBlockTap: (Int) -> Unit,
) {
    val display = items.mapIndexed { i, item -> Triple(startIndex + i, item, answers.containsKey(item.question.id)) }
        .filter { (_, _, answered) -> when (filter) { "solved" -> answered; "unsolved" -> !answered; else -> true } }
    val rows = display.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { (idx, _, answered) ->
                    Surface(
                        color = if (answered) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.size(30.dp).clickable { sound.click(); onBlockTap(idx) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", color = if (answered) Color.White else Color(0xFF666666), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.size(30.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FULL-SCREEN QUESTION — clean, focused, no grid, no numbers
// Just: title (small), timer (small top-right), question, options, Next
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExamFullScreenQuestion(
    theme: AppTheme,
    sound: SoundManager,
    test: TestDetail,
    answers: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Any>,
    currentIdx: Int,
    questionFeedback: QuestionFeedback?,
    timeLeft: Int,
    submitting: Boolean,
    onAnswer: (String, Any) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onBackToGrid: () -> Unit,
    onSubmit: () -> Unit,
) {
    val item = test.items.getOrNull(currentIdx) ?: return
    val q = item.question

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // ─── Header: title (left) + timer (right) + grid button ────────────
        // Small, doesn't affect the main content. Just a thin strip.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Grid button + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToGrid, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.GridView, null, tint = theme.primary, modifier = Modifier.size(16.dp))
                }
                Text(test.title.take(30), color = theme.subText, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            // Right: Timer (small, doesn't affect main content)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = theme.subText, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                val mm = timeLeft / 60; val ss = timeLeft % 60
                Text(String.format("%02d:%02d", mm, ss), color = theme.subText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ─── Main content: question on left, options on right ──────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            // LEFT: Question text + media
            LazyColumn(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Q${currentIdx + 1}", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(q.stem, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                // Media
                if (q.descType == "image" && !q.descImageUrl.isNullOrBlank()) {
                    item { AsyncImage(url = q.descImageUrl!!.toAbsoluteUrl(), modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp).clip(RoundedCornerShape(8.dp))) }
                }
                val mediaImgUrl = (q.mediaImageUrl ?: q.imageUrl)?.toAbsoluteUrl()
                if (!mediaImgUrl.isNullOrBlank()) {
                    item { AsyncImage(url = mediaImgUrl, modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).clip(RoundedCornerShape(8.dp))) }
                }
                val mediaAudUrl = (q.mediaAudioUrl ?: q.audioUrl)?.toAbsoluteUrl()
                if (!mediaAudUrl.isNullOrBlank()) {
                    item { AudioPlayerCard(theme = theme, url = mediaAudUrl, loopCount = q.audioLoop, loopDelaySec = q.audioLoopDelay, sound = sound) }
                }
            }

            // RIGHT: Answer options
            LazyColumn(
                modifier = Modifier.weight(1f).padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    AnswerInputBlock(theme, q, answers[q.id], questionFeedback, sound) { ans ->
                        onAnswer(q.id, ans)
                    }
                }
            }
        }

        // ─── Bottom: Prev / Next (or Submit) — always visible ──────────────
        Surface(color = theme.cardBg, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = currentIdx > 0,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev", fontSize = 13.sp)
                }

                Text("${currentIdx + 1} / ${test.items.size}", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                if (currentIdx < test.items.size - 1) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Next", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = onSubmit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        enabled = !submitting,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Submit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
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
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                                contentScale = ContentScale.Crop
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
