package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
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
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * RemoteImage — thin wrapper around Coil AsyncImage with debug logging so we
 * can trace which image URLs are requested. Used by the exam screen for both
 * description images and media images (instead of calling coil.compose.AsyncImage
 * inline everywhere).
 */
@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Log.d("IMG_DEBUG", "RemoteImage called with URL: $url")
    coil.compose.AsyncImage(
        model = url,
        contentDescription = "Question image",
        modifier = modifier,
        contentScale = contentScale,
    )
}

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
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    // ID of the audio option currently playing — used to block other audio
    // options from playing simultaneously (only one audio at a time).
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
                // Default to 50 minutes when durationMin is missing or out of range.
                // Admins sometimes set 0 or absurdly large values; clamp to 1–50.
                val dur = result.durationMin
                timeLeft = (if (dur > 0 && dur <= 50) dur else 50) * 60
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
                fontSize = 16.sp,
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
            Icon(Icons.Default.Quiz, null, tint = theme.subText, modifier = Modifier.size(80.dp))
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // ── 1. TOP STATUS HEADER ── compact white bar with thin border
        Surface(
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                // Section type
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val sectionLabel = if (q.blockType == "audio") "Listening ($listeningCount)" else "Reading ($readingCount)"
                    Text(sectionLabel, color = Color(0xFF003478), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                // Total
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Total: ${t.items.size}", color = Color(0xFF64748B), fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                // Remaining
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Left: $remainingCount", color = Color(0xFF64748B), fontSize = 10.sp)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                // Timer
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val mm = timeLeft / 60; val ss = timeLeft % 60
                    Text(String.format("%02d:%02d", mm, ss), color = Color(0xFF003478), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 2. INSTRUCTION ROW ── question number + FULL stem text (no truncation)
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("${currentIdx + 1}. ", color = Color(0xFF003478), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val displayText = q.stem.ifBlank { q.mediaText ?: "" }
                Text(displayText, color = Color(0xFF1E293B), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = true))
                if (q.isFree) {
                    Spacer(Modifier.width(4.dp))
                    Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(3.dp)) {
                        Text("FREE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                IconButton(onClick = { showGrid = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.GridView, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // ── 3. MAIN CONTENT ── 60% question (left, scrollable) | 40% answers (right, scrollable)
        // DreamKorea logo watermark in background (faded, centered)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Watermark logo at the center of the background
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(280.dp).alpha(0.08f),
                contentScale = ContentScale.Fit
            )
            Row(modifier = Modifier.fillMaxSize()) {
            // LEFT: Question content (60%) — scrollable so long descriptions/images don't get cut.
            // The question STEM is NOT shown here — it's at the top bar now.
            Column(
                modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Description TEXT (if admin set descType=text)
                if (q.descType == "text" && !q.descText.isNullOrBlank()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp)
                    ) {
                        Text(
                            q.descText!!,
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                // Description IMAGE (if admin set a descImageUrl)
                if (!q.descImageUrl.isNullOrBlank()) {
                    val url = q.descImageUrl!!.toAbsoluteUrl()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(200.dp)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { FullScreenImageViewer.show(url) },
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteImage(
                            url = url,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                // Media TEXT (if admin set mediaType=text)
                if (q.mediaType == "text" && !q.mediaText.isNullOrBlank()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp)
                    ) {
                        Text(
                            q.mediaText!!,
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                // Media IMAGE (mediaImageUrl falls back to legacy imageUrl)
                val mediaImgUrl = (q.mediaImageUrl ?: q.imageUrl)?.toAbsoluteUrl()
                if (!mediaImgUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(220.dp)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { FullScreenImageViewer.show(mediaImgUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteImage(
                            url = mediaImgUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                // Question audio (mediaAudioUrl falls back to legacy audioUrl)
                val mediaAudUrl = (q.mediaAudioUrl ?: q.audioUrl)?.toAbsoluteUrl()
                if (!mediaAudUrl.isNullOrBlank()) {
                    // key(q.id) ensures the AudioPlayerCard is FULLY RECREATED when
                    // the question changes — state (playCount, disabled) resets completely.
                    // This fixes the bug where audio was locked from the previous question.
                    key(q.id) {
                        AudioPlayerCard(
                            theme = theme,
                            url = mediaAudUrl,
                            loopCount = q.audioLoop,
                            loopDelaySec = if (q.audioLoopDelay > 0) q.audioLoopDelay else 2,
                            sound = sound,
                            questionId = q.id,
                            playCounts = audioPlayCounts,
                            onPlayingChange = { audioPlaying = it },
                        )
                    }
                }
            }

            // Vertical divider
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))

            // RIGHT: Answer options (40%) — scrollable so long options don't get cut
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = optText },
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
                                Spacer(Modifier.width(10.dp))
                                // Render option text with underlined blank word (if set by admin)
                                Text(
                                    text = buildUnderlinedText(optText, blankWord),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    "image" -> {
                        (0 until minOf(4, q.optionImages.size)).forEach { i ->
                            val imgUrl = q.optionImages[i]; if (imgUrl.isBlank()) return@forEach
                            val absUrl = imgUrl.toAbsoluteUrl()
                            val isSelected = answers[q.id] == absUrl
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = absUrl },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(8.dp))
                                RemoteImage(
                                    url = absUrl,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(6.dp)).clickable { FullScreenImageViewer.show(absUrl) },
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                    "audio" -> {
                        (0 until minOf(4, q.optionAudios.size)).forEach { i ->
                            val audUrl = q.optionAudios[i]; if (audUrl.isBlank()) return@forEach
                            val absUrl = audUrl.toAbsoluteUrl()
                            val optId = "${q.id}-opt-$i"
                            val isSelected = answers[q.id] == absUrl
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = !audioPlaying) { sound.click(); answers[q.id] = absUrl },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSelected) theme.primary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text("${i+1}", color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(8.dp))
                                // key(q.id, i) — recreate when question changes so play count resets.
                                // blocked = true when ANOTHER audio option is currently playing
                                // (so only one audio can play at a time).
                                key(q.id, i) {
                                    AudioPlayerCard(
                                        theme = theme,
                                        url = absUrl,
                                        loopCount = q.audioLoop.coerceAtLeast(1),
                                        loopDelaySec = if (q.audioLoopDelay > 0) q.audioLoopDelay else 2,
                                        sound = sound,
                                        questionId = optId,
                                        playCounts = audioPlayCounts,
                                        blocked = currentlyPlayingId != null && currentlyPlayingId != optId,
                                        onPlayingChange = { playing ->
                                            audioPlaying = playing
                                            currentlyPlayingId = if (playing) optId else null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // end Row
        } // end Box (watermark background)

        // ── 4. BOTTOM NAVIGATION ── compact white bar with thin border.
        // On the LAST question, hide Next/Submit — student must use the grid
        // page's blue Submit button. Prev + All Questions are always available.
        Surface(
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                // Previous (अघिल्लो) — disabled when audio playing
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !audioPlaying) { if (currentIdx > 0) { currentIdx--; sound.click() } }, contentAlignment = Alignment.Center) {
                    Text("अघिल्लो (Prev)", color = if (currentIdx > 0 && !audioPlaying) Color(0xFF003478) else Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                // All questions (सबै प्रश्नहरू) — disabled when audio playing
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !audioPlaying) { sound.click(); showGrid = true }, contentAlignment = Alignment.Center) {
                    Text("सबै प्रश्नहरू (All)", color = if (audioPlaying) Color(0xFFCBD5E1) else Color(0xFF003478), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                // Next (अर्को) — HIDDEN on the last question (use grid's Submit instead)
                if (currentIdx < sortedItems.size - 1) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !audioPlaying) { currentIdx++; sound.click() }, contentAlignment = Alignment.Center) {
                        Text("अर्को (Next)", color = if (audioPlaying) Color(0xFFCBD5E1) else Color(0xFF003478), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

            // ── QUESTION GRID PAGE ── fixed 1364×694 canvas (matches HTML reference)
    if (showGrid) {
        val readingItems = sortedItems.filter { it.question.blockType != "audio" }
        val listeningItems = sortedItems.filter { it.question.blockType == "audio" }
        val isQBank = testId == "qbank-combined" || testId.startsWith("bundle-")
        var showSubmitDialog by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        var filterMode by remember { mutableStateOf<Boolean?>(null) }
        val readingAnswered = readingItems.count { answers.containsKey(it.question.id) }
        val listeningAnswered = listeningItems.count { answers.containsKey(it.question.id) }
        val totalAnswered = readingAnswered + listeningAnswered
        val totalQuestions = t.items.size
        val totalUnsolved = totalQuestions - totalAnswered
        val hh = timeLeft / 3600; val mm = (timeLeft % 3600) / 60; val ss = timeLeft % 60
        val timeStr = String.format("%02d:%02d:%02d", hh, mm, ss)
        val isLowTime = timeLeft in 1..300
        val timerColor = if (isLowTime) Color(0xFFDC2626) else Color(0xFF151515)
        val accentBlue = Color(0xFF1565FF)
        val studentName = AppState.getUserName() ?: "Student"
        val studentEmail = AppState.getUserEmail() ?: ""
        val navGray = Color(0xFFF3F3F3)
        val borderColor = Color(0xFF111111)
        val titleBoxBorder = Color(0xFFC8C8C8)

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // ── User info bar ──
            Row(modifier = Modifier.fillMaxWidth().height(36.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text(studentName, color = Color(0xFF003478), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                Text(studentEmail, color = Color(0xFF333333), fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                Text(t.title.take(10), color = Color(0xFF333333), fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E2E2)))
            // ── Nav row: Nepal | All | Solved | Unsolved | Timer ──
            Row(modifier = Modifier.fillMaxWidth().height(42.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text("Nepal", color = Color(0xFF151515), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                val allSelected = filterMode == null
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (allSelected) navGray else Color.White).clickable { sound.click(); filterMode = null }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("All", color = Color(0xFF151515), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (allSelected) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black)) }
                    }
                }
                val solvedSelected = filterMode == true
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (solvedSelected) navGray else Color.White).clickable { sound.click(); filterMode = true }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Solved", color = Color(0xFF151515), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (solvedSelected) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black)) }
                    }
                }
                val unsolvedSelected = filterMode == false
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (unsolvedSelected) navGray else Color.White).clickable { sound.click(); filterMode = false }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unsolved", color = Color(0xFF151515), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (unsolvedSelected) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black)) }
                    }
                }
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(timeStr, color = timerColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E2E2)))
            // ── Reading + Listening panels ──
            Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reading panel
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).border(2.dp, titleBoxBorder).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("Reading", color = Color(0xFF202020), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(modifier = Modifier.fillMaxWidth().weight(1f).border(4.dp, borderColor, RoundedCornerShape(16.dp)).background(Color.White).padding(6.dp)) {
                        for (rowIdx in 0 until 4) {
                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (colIdx in 0 until 5) {
                                    val localIdx = rowIdx * 5 + colIdx
                                    val displayNum = localIdx + 1
                                    if (localIdx < readingItems.size) {
                                        val globalIdx = localIdx
                                        val item = readingItems[localIdx]
                                        val isAnswered = answers.containsKey(item.question.id)
                                        val isCurrent = globalIdx == currentIdx
                                        val isFilteredOut = when (filterMode) { true -> !isAnswered; false -> isAnswered; null -> false }
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(3.dp, if (isCurrent) accentBlue else borderColor).background(if (isAnswered) accentBlue else Color.White).alpha(if (isFilteredOut) 0.15f else 1f).clickable(enabled = !isFilteredOut) { sound.click(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentIdx = globalIdx; showGrid = false }, contentAlignment = Alignment.Center) {
                                            Text("$displayNum", color = if (isAnswered) Color.White else Color(0xFF171717), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(3.dp, borderColor).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
                                            Text("$displayNum", color = Color(0xFFCCCCCC), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Listening panel
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).border(2.dp, titleBoxBorder).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("Listening", color = Color(0xFF202020), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(modifier = Modifier.fillMaxWidth().weight(1f).border(4.dp, borderColor, RoundedCornerShape(16.dp)).background(Color.White).padding(6.dp)) {
                        for (rowIdx in 0 until 4) {
                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (colIdx in 0 until 5) {
                                    val localIdx = rowIdx * 5 + colIdx
                                    val displayNum = localIdx + 21
                                    if (localIdx < listeningItems.size) {
                                        val globalIdx = readingItems.size + localIdx
                                        val item = listeningItems[localIdx]
                                        val isAnswered = answers.containsKey(item.question.id)
                                        val isCurrent = globalIdx == currentIdx
                                        val isFilteredOut = when (filterMode) { true -> !isAnswered; false -> isAnswered; null -> false }
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(3.dp, if (isCurrent) accentBlue else borderColor).background(if (isAnswered) accentBlue else Color.White).alpha(if (isFilteredOut) 0.15f else 1f).clickable(enabled = !isFilteredOut) { sound.click(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentIdx = globalIdx; showGrid = false }, contentAlignment = Alignment.Center) {
                                            Text("$displayNum", color = if (isAnswered) Color.White else Color(0xFF171717), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(3.dp, borderColor).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
                                            Text("$displayNum", color = Color(0xFFCCCCCC), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ── Submit button ──
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Button(onClick = { sound.click(); showSubmitDialog = true }, modifier = Modifier.fillMaxWidth(0.5f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = accentBlue), shape = RoundedCornerShape(16.dp)) {
                    Text("Submit and Finish Exam", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (showSubmitDialog) {
            val totalUnsolved2 = totalQuestions - totalAnswered
            val warning = when {
                totalUnsolved2 == 0 -> "You answered all $totalQuestions questions. Ready to submit!"
                totalUnsolved2 <= 5 -> "You have $totalUnsolved2 unanswered question(s). Submit anyway?"
                else -> "Warning: $totalUnsolved2 questions are still unanswered! Submit anyway?"
            }
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text("Submit Exam?") },
                text = { Text(warning, fontSize = 13.sp) },
                confirmButton = {
                    Button(onClick = {
                        showSubmitDialog = false
                        if (!submitting) {
                            sound.swoosh(); submitting = true
                            scope.launch {
                                try { submitResult = submitExamWithFallback(t, answers.toMap()); sound.success() }
                                catch (e: Exception) { error = "Submit failed." }
                                submitting = false
                            }
                        }
                    }) {
                        if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
                        else { Text("Submit") }
                    }
                },
                dismissButton = { OutlinedButton(onClick = { showSubmitDialog = false }) { Text("Cancel") } }
            )
        }
        return
    }
}

/// Scale helpers — convert absolute pixel values from the 1364×694 reference
/// canvas into Dp values based on the runtime scale factor.
private fun Int.sc(s: Float): Dp = (this.toFloat() * s).dp
private fun Float.sc(s: Float): Dp = (this * s).dp

/**
 * CanvasBlockPage — renders the exam block-page (question grid) on a fixed
 * 1364×694 canvas, scaled to fit the available screen space. All positions
 * are absolute pixel coordinates that get multiplied by `scale` to produce
 * the actual Dp values used at runtime.
 *
 * Layout (top-down):
 *   ┌────────────────────────────────────────────────────────────────┐
 *   │ ┌──────┬─────────────────────────────────────────────────────┐ │  ← outer 3px frame
 *   │ │ logo │ title | studentId | dreamkorea          (30sp)       │ │  ← top info header
 *   │ │ 138  │ Nepal | All | Solved | Unsolved | Timer (30sp)       │ │  ← nav row
 *   │ └──────┴─────────────────────────────────────────────────────┘ │
 *   │ ┌─────────────────────────┐  ┌─────────────────────────┐       │
 *   │ │ Reading (5×4 grid)      │  │ Listening (5×4 grid)    │       │
 *   │ │ 1..20                   │  │ 21..40                  │       │
 *   │ └─────────────────────────┘  └─────────────────────────┘       │
 *   │                  ┌──────────────────┐                          │
 *   │                  │  Submit (#156BF2)│                          │
 *   │                  └──────────────────┘                          │
 *   └────────────────────────────────────────────────────────────────┘
 *
 * "All" tab is always selected (matches the design — #F3F3F9 bg + 4px black underline).
 */
@Composable
private fun CanvasBlockPage(
    scale: Float,
    title: String,
    studentId: String,
    timeStr: String,
    readingItems: List<TestItemDetail>,
    listeningItems: List<TestItemDetail>,
    isQBank: Boolean,
    allItems: List<TestItemDetail>,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    onFilterChange: (Boolean?) -> Unit,
    onSubmit: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val frameColor = Color(0xFF343434)
    val navTabBg = Color(0xFFF3F3F9)
    val groupBorder = Color(0xFF111111)
    val titleBoxBorder = Color(0xFFC8C8C8)
    val submitBlue = Color(0xFF156BF2)
    val headerSp = (30f * scale).sp
    val titleSp = (25f * scale).sp
    val numSp = (34f * scale).sp

    Box(
        modifier = Modifier
            .width(1364.sc(scale))
            .height(694.sc(scale))
            .border(3.sc(scale), frameColor)
            .background(Color.White)
    ) {
        // Watermark logo behind everything (0.10 alpha)
        Image(
            painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.sc(scale))
                .alpha(0.10f),
            contentScale = ContentScale.Fit
        )

        // ── Top header (height 157): logo (138 wide) | info column (rest) ──
        Row(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().height(157.sc(scale))) {
            // Logo box (138 wide, 157 tall, 3px right border)
            Box(
                modifier = Modifier
                    .width(138.sc(scale))
                    .fillMaxHeight()
                    .border(width = 3.sc(scale), color = frameColor)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                    contentDescription = "DreamKorea Logo",
                    modifier = Modifier.size(80.sc(scale)),
                    contentScale = ContentScale.Fit
                )
            }
            // Info column: title row (top) | nav row (bottom) — each 78 tall
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Title row (30sp): title | studentId | "dreamkorea"
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = Color(0xFF111111),
                        fontSize = headerSp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.sc(scale))
                    )
                    Text("ID: $studentId", color = Color(0xFF444444), fontSize = headerSp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.sc(scale)))
                    Text("dreamkorea", color = Color(0xFF003478), fontSize = headerSp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.sc(scale)))
                }
                // Nav row (30sp): Nepal | All (selected) | Solved | Unsolved | Timer
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab(scale, headerSp, "Nepal", selected = false, onClick = null)
                    NavTab(scale, headerSp, "All", selected = filterMode == null, underline = true, bg = navTabBg, onClick = { onFilterChange(null) })
                    NavTab(scale, headerSp, "Solved", selected = filterMode == true, onClick = { onFilterChange(true) })
                    NavTab(scale, headerSp, "Unsolved", selected = filterMode == false, onClick = { onFilterChange(false) })
                    // Timer (right)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White), contentAlignment = Alignment.Center) {
                        Text(timeStr, color = Color(0xFF222222), fontSize = headerSp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }

        // Horizontal divider under header (3px black line at y=157)
        Box(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().height(3.sc(scale)).offset(y = 157.sc(scale)).background(frameColor))

        // ── Bottom area (height 694 - 157 = 537): Reading | Listening groups + Submit ──
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height((694 - 157).sc(scale))
                .offset(y = 160.sc(scale))
                .padding(8.sc(scale))
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.sc(scale))) {
                if (isQBank) {
                    CanvasGroup(
                        scale = scale,
                        title = "All Questions",
                        titleSp = titleSp,
                        numSp = numSp,
                        items = allItems,
                        baseIdx = 0,
                        startNum = 1,
                        answers = answers,
                        currentIdx = currentIdx,
                        sound = sound,
                        haptic = haptic,
                        filterMode = filterMode,
                        onPick = onPick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                } else {
                    CanvasGroup(
                        scale = scale,
                        title = "Reading",
                        titleSp = titleSp,
                        numSp = numSp,
                        items = readingItems,
                        baseIdx = 0,
                        startNum = 1,
                        answers = answers,
                        currentIdx = currentIdx,
                        sound = sound,
                        haptic = haptic,
                        filterMode = filterMode,
                        onPick = onPick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    CanvasGroup(
                        scale = scale,
                        title = "Listening",
                        titleSp = titleSp,
                        numSp = numSp,
                        items = listeningItems,
                        baseIdx = readingItems.size,
                        startNum = 21,
                        answers = answers,
                        currentIdx = currentIdx,
                        sound = sound,
                        haptic = haptic,
                        filterMode = filterMode,
                        onPick = onPick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            // Submit button (centered, #156BF2)
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.sc(scale)), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.offset(x = 520.sc(scale), y = 605.sc(scale)).width(325.sc(scale)).height(67.sc(scale)),
                    colors = ButtonDefaults.buttonColors(containerColor = submitBlue),
                    shape = RoundedCornerShape(18.sc(scale))
                ) {
                    Text("Submit and Finish Exam", color = Color.White, fontSize = (24f * scale).sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/// Single nav tab in the CanvasBlockPage header. `selected` controls color
/// and (optionally) a 4px black underline. `onClick = null` makes it a static label.
@Composable
private fun NavTab(
    scale: Float,
    sp: androidx.compose.ui.unit.TextUnit,
    label: String,
    selected: Boolean,
    underline: Boolean = false,
    bg: Color = Color.White,
    onClick: (() -> Unit)?,
) {
    val textColor = if (selected) Color(0xFF111111) else Color(0xFF444444)
    val weight = FontWeight.Bold
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(if (selected) bg else Color.White)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = textColor, fontSize = sp, fontWeight = weight)
            if (underline || selected) {
                Box(modifier = Modifier.fillMaxWidth().height(4.sc(scale)).background(Color.Black))
            }
        }
    }
}

/**
 * CanvasGroup — one of the two halves (Reading / Listening) of the block page.
 * Renders a title box (2px #C8C8C8 border, 25sp) and a 5×4 grid of question
 * boxes inside a 4px #111111 border with 20px corner radius.
 *
 * Numbering:
 *   • Reading    → displayNum = localIdx + 1     (1..20)
 *   • Listening  → displayNum = localIdx + 21    (21..40)
 * globalIdx is the index into the full sorted items list (so onPick can jump
 * the exam screen to the correct question).
 */
@Composable
private fun CanvasGroup(
    scale: Float,
    title: String,
    titleSp: androidx.compose.ui.unit.TextUnit,
    numSp: androidx.compose.ui.unit.TextUnit,
    items: List<TestItemDetail>,
    baseIdx: Int,
    startNum: Int,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupBorder = Color(0xFF111111)
    val titleBoxBorder = Color(0xFFC8C8C8)
    val cols = 5
    val rows = 4

    Column(modifier = modifier) {
        // Title box (2px #C8C8C8 border, 25sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.sc(scale))
                .border(2.sc(scale), titleBoxBorder)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(title, color = Color(0xFF111111), fontSize = titleSp, fontWeight = FontWeight.Bold)
        }
        // Group container (4px #111111 border, 20px radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxWidth()
                .border(4.sc(scale), groupBorder, RoundedCornerShape(20.sc(scale)))
                .background(Color.White)
                .padding(8.sc(scale))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.sc(scale))
            ) {
                for (rowIdx in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.sc(scale))) {
                        for (colIdx in 0 until cols) {
                            val localIdx = rowIdx * cols + colIdx
                            val displayNum = startNum + localIdx
                            if (localIdx < items.size) {
                                val globalIdx = baseIdx + localIdx
                                val item = items[localIdx]
                                val isAnswered = answers.containsKey(item.question.id)
                                val isCurrent = globalIdx == currentIdx
                                val isFilteredOut = when (filterMode) {
                                    true -> !isAnswered
                                    false -> isAnswered
                                    null -> false
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .border(3.sc(scale), if (isCurrent) Color(0xFF156BF2) else groupBorder)
                                        .background(if (isAnswered) Color(0xFF156BF2) else Color.White)
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
                                        fontSize = numSp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            } else {
                                // Empty placeholder cell
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .border(3.sc(scale), Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$displayNum", color = Color(0xFFCCCCCC), fontSize = numSp, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
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
            fontSize = 16.sp,
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
                                .fillMaxWidth()
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
                                .fillMaxWidth()
                                .aspectRatio(1f)  // PERFECT SQUARE
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$placeholderNum",
                                color = Color(0xFFCCCCCC),
                                fontSize = 16.sp,
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
            fontWeight = FontWeight.Medium,
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

// ─── Audio player — compact 36dp play-icon-only button ───────────────────────
// loopCount = total number of times to play the audio:
//   0 or 1 = plays once
//   2 = plays twice
//   N = plays N times
//  -1 = infinite loop
//
// `blocked` = true disables the button (another audio is currently playing).
// `unlimited` = true ignores loopCount and lets the user replay indefinitely
//   (used by the review screen so students can listen as many times as they want).
//
// Play count is persisted in the parent `playCounts` map (keyed by `questionId`)
// so navigating away and back doesn't reset it. In review mode (`playCounts` is
// null or `unlimited` is true), a local `localPlayCount` is used instead so the
// UI can still show "playing" state without the parent map.
@Composable
fun AudioPlayerCard(
    theme: AppTheme,
    url: String,
    loopCount: Int,
    loopDelaySec: Int,
    sound: SoundManager,
    questionId: String? = null,
    playCounts: SnapshotStateMap<String, Int>? = null,
    blocked: Boolean = false,
    unlimited: Boolean = false,
    onPlayingChange: ((Boolean) -> Unit)? = null,
) {
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    // Local play count — used when there's no parent playCounts map (review mode)
    var localPlayCount by remember { mutableStateOf(0) }

    val persistentCount = playCounts?.get(questionId) ?: localPlayCount
    LaunchedEffect(isPlaying) {
        onPlayingChange?.invoke(isPlaying)
    }

    val maxPlays = if (loopCount <= 0) 2 else loopCount
    val disabled = !unlimited && persistentCount >= maxPlays
    val scope = rememberCoroutineScope()

    fun incrementPlayCount() {
        if (questionId != null && playCounts != null) {
            playCounts[questionId] = (playCounts[questionId] ?: 0) + 1
        } else {
            localPlayCount++
        }
    }

    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Compact 36dp play-icon-only button — no label, no card wrapper.
    Surface(
        color = Color.Transparent,
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.size(36.dp)
    ) {
        IconButton(
            onClick = {
                if (disabled || blocked) return@IconButton
                sound.click()
                if (isPlaying) {
                    // Don't pause — let it finish. Only allow play, not pause.
                    return@IconButton
                } else {
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
                                val currentCount = playCounts?.get(questionId) ?: localPlayCount
                                if (unlimited || currentCount < maxPlays) {
                                    scope.launch {
                                        if (loopDelaySec > 0) delay(loopDelaySec * 1000L)
                                        val latestCount = playCounts?.get(questionId) ?: localPlayCount
                                        if (unlimited || latestCount < maxPlays) {
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
            },
            enabled = !disabled && !blocked && !isPlaying,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                when {
                    disabled -> Icons.Default.Lock
                    blocked -> Icons.Default.Lock
                    isPlaying -> Icons.Default.VolumeUp
                    else -> Icons.Default.PlayArrow
                },
                null,
                tint = when {
                    disabled || blocked -> Color(0xFFCBD5E1)
                    isPlaying -> Color(0xFF22C55E)
                    else -> theme.primary
                },
                modifier = Modifier.size(24.dp)
            )
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
                        fontWeight = FontWeight.Medium,
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
                        fontWeight = FontWeight.Medium,
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
                            fontWeight = FontWeight.Medium,
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
        // Sorted: Reading (blockType != "audio") first, then Listening (== "audio").
        // Each section has its own colored header.
        if (showReviewSection) {
            val readingReviews = result.review.filter { it.blockType != "audio" }
            val listeningReviews = result.review.filter { it.blockType == "audio" }
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
            // Reading section (blue header)
            if (readingReviews.isNotEmpty()) {
                item {
                    Surface(
                        color = Color(0xFF003478).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().alpha(reviewAlpha)
                    ) {
                        Text(
                            "Reading (${readingReviews.size})",
                            color = Color(0xFF003478),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                itemsIndexed(readingReviews) { idx, review ->
                    ReviewCard(theme, review, idx + 1, sound)
                }
            }
            // Listening section (purple header)
            if (listeningReviews.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF6A1B9A).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().alpha(reviewAlpha)
                    ) {
                        Text(
                            "Listening (${listeningReviews.size})",
                            color = Color(0xFF6A1B9A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                itemsIndexed(listeningReviews) { idx, review ->
                    ReviewCard(theme, review, idx + 1, sound)
                }
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
    val reviewSound = sound ?: rememberSoundManager()
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
                            fontWeight = FontWeight.Medium,
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
                            fontWeight = FontWeight.Medium,
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
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── Description TEXT (if admin set descType=text) ───────────
            if (review.descType == "text" && !review.descText.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        review.descText!!,
                        color = theme.darkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // ── Description IMAGE ──────────────────────────────────────────
            if (!review.descImageUrl.isNullOrBlank()) {
                val url = review.descImageUrl!!.toAbsoluteUrl()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    RemoteImage(
                        url = url,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Media TEXT (if admin set mediaType=text) ────────────────
            if (review.mediaType == "text" && !review.mediaText.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        review.mediaText!!,
                        color = theme.darkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // ── Media IMAGE (mediaImageUrl falls back to legacy imageUrl) ─
            val mediaImgUrl = (review.mediaImageUrl ?: review.imageUrl)?.toAbsoluteUrl()
            if (!mediaImgUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    RemoteImage(
                        url = mediaImgUrl,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // ── Media AUDIO (mediaAudioUrl falls back to legacy audioUrl) ─
            // Review mode: unlimited replays so students can listen as many times as they want.
            val mediaAudUrl = (review.mediaAudioUrl ?: review.audioUrl)?.toAbsoluteUrl()
            if (!mediaAudUrl.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AudioPlayerCard(
                        theme = theme,
                        url = mediaAudUrl,
                        loopCount = review.audioLoop.coerceAtLeast(1),
                        loopDelaySec = review.audioLoopDelay,
                        sound = reviewSound,
                        unlimited = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Question audio (unlimited replays)", color = theme.subText, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Question stem ─────────────────────────────────────────────
            Text(
                review.stem.take(300),
                color = theme.darkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            // ── Options with correct/wrong highlighting ──────────────────
            // For "image" answerType we iterate optionImages, for "audio" we iterate
            // optionAudios, otherwise we iterate the text options list.
            when (review.answerType) {
                "image" -> {
                    review.optionImages.forEachIndexed { i, rawImgUrl ->
                        if (rawImgUrl.isBlank()) return@forEachIndexed
                        val imgUrl = rawImgUrl.toAbsoluteUrl()
                        // Match by URL (the user's answer is stored as the absolute URL)
                        val isUserAns = (review.userAnswer == imgUrl) || (review.userAnswer == rawImgUrl) ||
                            ((review.userAnswer as? List<*>)?.any { it == imgUrl || it == rawImgUrl } == true)
                        val isCorrectAns = i == review.options?.indexOfFirst { it == review.correctAnswer }?.takeIf { it >= 0 } ||
                            (review.correctAnswer == imgUrl) || (review.correctAnswer == rawImgUrl)
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
                                RemoteImage(
                                    url = imgUrl,
                                    modifier = Modifier.weight(1f).height(200.dp).clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Fit,
                                )
                                if (isCorrectAns) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF28A745), modifier = Modifier.size(14.dp))
                                } else if (isUserAns && !isCorrectAns) {
                                    Icon(Icons.Default.Close, null, tint = theme.errorRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                "audio" -> {
                    review.optionAudios.forEachIndexed { i, rawAudUrl ->
                        if (rawAudUrl.isBlank()) return@forEachIndexed
                        val audUrl = rawAudUrl.toAbsoluteUrl()
                        val isUserAns = (review.userAnswer == audUrl) || (review.userAnswer == rawAudUrl) ||
                            ((review.userAnswer as? List<*>)?.any { it == audUrl || it == rawAudUrl } == true)
                        val isCorrectAns = (review.correctAnswer == audUrl) || (review.correctAnswer == rawAudUrl)
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
                                // Unlimited replays during review
                                AudioPlayerCard(
                                    theme = theme,
                                    url = audUrl,
                                    loopCount = 1,
                                    loopDelaySec = 0,
                                    sound = reviewSound,
                                    unlimited = true,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(review.options?.getOrNull(i) ?: "Audio ${'A' + i}", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (isCorrectAns) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF28A745), modifier = Modifier.size(14.dp))
                                } else if (isUserAns && !isCorrectAns) {
                                    Icon(Icons.Default.Close, null, tint = theme.errorRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Text options (answerType = "text" / "choose" / etc.)
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
                        AnswerDisplay(review.userAnswer, theme, reviewSound)
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
                        AnswerDisplay(review.correctAnswer, theme, reviewSound)
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
                userAnswer = ans,
                correctAnswer = q.options?.getOrNull(q.correctOption),
                explanation = q.explanation,
                isCorrect = isCorrect,
                // Pass-through block-based fields so the ReviewCard can render
                // description and media (text/image/audio) and the review list
                // can be sorted into Reading vs Listening.
                answerType = q.answerType,
                blockType = q.blockType,
                descType = q.descType,
                descText = q.descText,
                descImageUrl = q.descImageUrl,
                mediaType = q.mediaType,
                mediaText = q.mediaText,
                mediaImageUrl = q.mediaImageUrl,
                mediaAudioUrl = q.mediaAudioUrl,
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
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            reason,
            color = Color(0xFF64748B),
            fontSize = 16.sp,
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
                    fontWeight = FontWeight.Medium,
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
                            .fillMaxWidth()
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
                                fontWeight = FontWeight.Medium,
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
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Skip button
        TextButton(onClick = { onContinue() }) {
            Text("Skip eye test", color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}
