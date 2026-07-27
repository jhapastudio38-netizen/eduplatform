package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
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

    // ── PROGRAMMATIC ORIENTATION LOCK ──────────────────────────────────────
    // Force the exam screen to landscape the moment it mounts. Restored to
    // the user's preferred orientation on exit. This guarantees the exam
    // layout (60/40 split, status bar, Nepali nav) renders correctly without
    // the user having to manually rotate the device.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
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
                        AppState.api.submitTest(currentTest.id, SubmitRequest(answers.toMap()))
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
    val item = t.items.getOrNull(currentIdx) ?: return
    val q = item.question
    val options = q.options ?: emptyList()
    val textItems = t.items.filter { it.question.blockType == "text" }
    val readingCount = if (textItems.isNotEmpty()) textItems.size else t.items.size
    val listeningCount = if (textItems.isNotEmpty()) t.items.size - textItems.size else 0
    val answeredCount = answers.size
    val remainingCount = t.items.size - answeredCount
    var showGrid by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // ── 1. TOP STATUS HEADER ── 4 equal sections with vertical dividers
        Surface(border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                // Section type
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val sectionLabel = if (q.blockType == "audio") "Listening ($listeningCount que)" else "Reading ($readingCount que)"
                    Text(sectionLabel, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Total
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Total (${t.items.size})", color = Color.Black, fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Remaining
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Remaining ($remainingCount)", color = Color.Black, fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Timer
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val mm = timeLeft / 60; val ss = timeLeft % 60
                    Text(String.format("%02d:%02d", mm, ss), color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 2. INSTRUCTION ROW ── question number + instruction text + divider
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${currentIdx + 1}. ", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(q.stem.take(80), color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showGrid = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.GridView, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // ── 3. MAIN CONTENT ── 60% question (left) | 40% answers (right)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // LEFT: Question content (60%)
            Column(
                modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Question card
                Surface(
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Text(
                        q.stem,
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Media images — ContentScale.Fit (contain, not cover)
                if (q.descType == "image" && !q.descImageUrl.isNullOrBlank()) {
                    val url = q.descImageUrl!!.toAbsoluteUrl()
                    coil.compose.AsyncImage(
                        model = url, contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 120.dp).clip(RoundedCornerShape(8.dp)).clickable { FullScreenImageViewer.show(url) },
                        contentScale = ContentScale.Fit
                    )
                }
                val mediaImgUrl = (q.mediaImageUrl ?: q.imageUrl)?.toAbsoluteUrl()
                if (!mediaImgUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = mediaImgUrl, contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 140.dp).clip(RoundedCornerShape(8.dp)).clickable { FullScreenImageViewer.show(mediaImgUrl) },
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

            // RIGHT: Answer options (40%)
            Column(
                modifier = Modifier.weight(0.4f).fillMaxHeight().padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                when (q.answerType) {
                    "text", "choose" -> {
                        (0 until minOf(4, options.size)).forEach { i ->
                            val isSelected = answers[q.id] == options.getOrNull(i)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { sound.click(); answers[q.id] = options.getOrNull(i) ?: "" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(options.getOrNull(i) ?: "", color = Color.Black, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    "image" -> {
                        (0 until minOf(4, q.optionImages.size)).forEach { i ->
                            val imgUrl = q.optionImages[i]; if (imgUrl.isBlank()) return@forEach
                            val absUrl = imgUrl.toAbsoluteUrl()
                            val isSelected = answers[q.id] == absUrl
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { sound.click(); answers[q.id] = absUrl },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = if (isSelected) theme.primary else Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(28.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(4.dp))
                                coil.compose.AsyncImage(model = absUrl, contentDescription = "Option ${i+1}", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).clickable { FullScreenImageViewer.show(absUrl) }, contentScale = ContentScale.Fit)
                            }
                        }
                    }
                    "audio" -> {
                        (0 until minOf(4, q.optionAudios.size)).forEach { i ->
                            val audUrl = q.optionAudios[i]; if (audUrl.isBlank()) return@forEach
                            val absUrl = audUrl.toAbsoluteUrl()
                            val isSelected = answers[q.id] == absUrl
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { sound.click(); answers[q.id] = absUrl }, verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = if (isSelected) theme.primary else Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(28.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(4.dp))
                                AudioPlayerCard(theme = theme, url = absUrl, loopCount = 1, loopDelaySec = 0, sound = sound)
                            }
                        }
                    }
                }
            }
        }

        // ── 4. BOTTOM NAVIGATION ── Nepali labels + grid button
        Surface(border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)) {
            Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                // Previous (अघिल्लो)
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { if (currentIdx > 0) { currentIdx--; sound.click() } }, contentAlignment = Alignment.Center) {
                    Text("अघिल्लो (Previous)", color = if (currentIdx > 0) Color.Black else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // All questions (सबै प्रश्नहरू)
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { sound.click(); showGrid = true }, contentAlignment = Alignment.Center) {
                    Text("सबै प्रश्नहरू (All)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                // Next (अर्को) or Submit
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                    if (currentIdx < t.items.size - 1) { currentIdx++; sound.click() }
                    else { sound.swoosh(); submitting = true; scope.launch { try { submitResult = if (t.id == "qbank-combined" || t.id.startsWith("bundle-")) submitCombinedExamWithFallback(t, answers.toMap()) else AppState.api.submitTest(t.id, SubmitRequest(answers.toMap())); sound.success() } catch (e: Exception) { error = "Submit failed." }; submitting = false } }
                }, contentAlignment = Alignment.Center) {
                    if (submitting) { CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
                    else if (currentIdx < t.items.size - 1) { Text("अर्को (Next)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    else { Text("सबमिट (Submit)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    // ── QUESTION GRID OVERLAY ── when user taps "सबै प्रश्नहरू"
    if (showGrid) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showGrid = false }) {
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("All Questions", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showGrid = false }) { Icon(Icons.Default.Close, null, tint = Color.Black) }
                    }
                    Text("$answeredCount / ${t.items.size} answered", color = Color.Gray, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    // Grid — 10 columns
                    val rows = t.items.toList().chunked(10)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(rows.size) { rowIdx ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                rows[rowIdx].forEachIndexed { colIdx, _ ->
                                    val globalIdx = rowIdx * 10 + colIdx
                                    val isAnswered = answers.containsKey(t.items[globalIdx].question.id)
                                    val isCurrent = globalIdx == currentIdx
                                    Surface(
                                        color = when { isCurrent -> theme.primary; isAnswered -> Color(0xFFC8E6C9); else -> Color.White },
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                        modifier = Modifier.weight(1f).aspectRatio(1.3f).clickable { sound.click(); currentIdx = globalIdx; showGrid = false }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text("${globalIdx+1}", color = if (isCurrent) Color.White else Color.Black, fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                    }
                    // Submit button
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showGrid = false; sound.swoosh(); submitting = true; scope.launch { try { submitResult = if (t.id == "qbank-combined" || t.id.startsWith("bundle-")) submitCombinedExamWithFallback(t, answers.toMap()) else AppState.api.submitTest(t.id, SubmitRequest(answers.toMap())); sound.success() } catch (e: Exception) { error = "Submit failed." }; submitting = false } },
                        modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = theme.primary), shape = RoundedCornerShape(8.dp)
                    ) { Text("Submit and Finish", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
fun ExamResultScreen(
    theme: AppTheme,
    result: SubmitResponse,
    onExit: () -> Unit,
    sound: SoundManager,
    examTitle: String = "Exam",
    examDescription: String? = null,
) {
    LaunchedEffect(Unit) { sound.success() }

    // ── Animation states ──────────────────────────────────────────────────
    var showScore by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showReviewSection by remember { mutableStateOf(false) }
    val animatedScore = animateFloatAsState(
        targetValue = if (showScore && result.maxScore > 0) result.score.toFloat() / result.maxScore else 0f,
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

    val pct = if (result.maxScore > 0) (result.score * 100 / result.maxScore) else 0
    val passed = pct >= 40
    val correctCount = result.review.count { it.isCorrect }
    val incorrectCount = result.review.size - correctCount
    val unansweredCount = result.review.count { it.userAnswer == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Exam title + description ──────────────────────────────────────
        item {
            Surface(
                color = theme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        examTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!examDescription.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            examDescription,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
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
                    Text("${result.score} / ${result.maxScore} points", color = theme.subText, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))

                    // ── Stats row (total marks, correct, incorrect, unanswered) ──
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(statsAlpha),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox("Total", "${result.maxScore}", Color(0xFF6A1B9A), Modifier.weight(1f))
                        StatBox("Correct", "$correctCount", Color(0xFF4CAF50), Modifier.weight(1f))
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
            itemsIndexed(result.review) { idx, review ->
                ReviewCard(theme, review, idx + 1)
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
fun ReviewCard(theme: AppTheme, review: ReviewItem, questionNumber: Int = 0) {
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

            // ── Question stem ─────────────────────────────────────────────
            Text(
                review.stem.take(300),
                color = theme.darkText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            // Image (if any)
            if (!review.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                val imgAbs = review.imageUrl!!.toAbsoluteUrl()
                coil.compose.AsyncImage(
                    model = imgAbs,
                    contentDescription = "Question image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Options with correct/wrong highlighting ──────────────────
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
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${'A' + i}.", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(opt, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
                        Text(
                            formatAnswer(review.userAnswer) ?: "—",
                            color = theme.darkText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                        Text(
                            formatAnswer(review.correctAnswer) ?: "—",
                            color = Color(0xFF28A745),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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

    for (item in test.items) {
        val q = item.question
        val ans = answers[q.id]
        var isCorrect = false

        when (q.type) {
            "SINGLE_CHOICE", "TRUE_FALSE", "ONE_WORD", "FILL_BLANK" -> {
                val correctIdx = q.correctOption
                val correctAns = q.options?.getOrNull(correctIdx)
                    ?: q.options?.getOrNull(0)
                    ?: ""
                if (ans is String && correctAns.isNotEmpty() &&
                    ans.trim().equals(correctAns.trim(), ignoreCase = true)) {
                    isCorrect = true
                    score++
                }
            }
            "MULTIPLE_CHOICE" -> {
                // For multiple choice, compare selected indices
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
                type = q.type,
                options = q.options,
                imageUrl = q.imageUrl,
                audioUrl = q.audioUrl,
                audioLoop = q.audioLoop,
                audioLoopDelay = q.audioLoopDelay,
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
        if (e.code() == 404 || e.code() == 500) {
            // Server doesn't know how to handle this combined exam ID —
            // grade it client-side as a fallback
            gradeCombinedExamClientSide(test, answers)
        } else throw e
    }
}
