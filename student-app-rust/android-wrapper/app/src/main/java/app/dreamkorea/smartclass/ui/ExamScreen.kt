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

/**
 * Exam taking screen — full flow:
 * 1. Loading skeleton
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
    val scope = rememberCoroutineScope()
    val sound = rememberSoundManager()
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

    LaunchedEffect(testId) {
        scope.launch {
            try {
                test = AppState.api.getTestDetail(testId).test
                timeLeft = (test?.durationMin ?: 30) * 60
            } catch (e: retrofit2.HttpException) {
                // HTTP error from server
                error = when (e.code()) {
                    401 -> "Please log in again to take this exam."
                    403 -> "This exam is not active or has ended."
                    404 -> "Exam not found. It may have been removed."
                    else -> "Could not load exam. Please try again."
                }
            } catch (e: java.net.UnknownHostException) {
                error = "No internet connection. Please check your network."
            } catch (e: java.io.IOException) {
                error = "Could not connect to server. Please check your internet."
            } catch (e: Exception) {
                error = "Could not load exam. Please try again."
            }
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
            if (timeLeft == 0 && test != null && submitResult == null && !submitting) {
                submitting = true
                sound.swoosh()
                try {
                    submitResult = AppState.api.submitTest(test!!.id, SubmitRequest(answers.toMap()))
                    sound.success()
                } catch (e: java.net.UnknownHostException) {
                    error = "No internet connection. Please check your network."
                } catch (e: java.io.IOException) {
                    error = "Could not connect to server. Please check your internet."
                } catch (e: Exception) {
                    error = "Could not submit exam. Please try again."
                }
                submitting = false
            }
        }
    }

    if (loading) {
        SkeletonListScreen(theme, itemCount = 4)
        return
    }

    if (error.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Error, null, tint = theme.errorRed, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(error, color = theme.darkText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Go back") }
        }
        return
    }

    val t = test ?: return
    val currentQuestion = t.items.getOrNull(currentIdx)

    // ─── Result screen ──────────────────────────────────────────────────────
    if (submitResult != null) {
        ExamResultScreen(theme, submitResult!!, onExit, sound)
        return
    }

    // ─── Question screen ────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header with timer + progress
        Surface(color = theme.primary, shape = RoundedCornerShape(0.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Question ${currentIdx + 1} of ${t.items.size}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        val progress = (currentIdx + 1f) / t.items.size
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.width(120.dp).padding(top = 4.dp)
                        )
                    }
                    // Timer
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            val mm = timeLeft / 60
                            val ss = timeLeft % 60
                            Text(String.format("%02d:%02d", mm, ss), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(t.title, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }

        // Question content
        if (currentQuestion == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No questions in this test", color = theme.subText)
            }
            return
        }

        val q = currentQuestion.question
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question stem
            item {
                Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = theme.primary, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("${currentIdx + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(q.difficulty, color = theme.subText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("${currentQuestion.points} pts", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(q.stem, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                }
            }

            // Image (if present)
            q.imageUrl?.let { url ->
                item {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        AsyncImage(url = url, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)))
                    }
                }
            }

            // Audio player (if present, with loop)
            q.audioUrl?.let { url ->
                item {
                    AudioPlayerCard(
                        theme = theme,
                        url = url,
                        loopCount = q.audioLoop,
                        loopDelaySec = q.audioLoopDelay,
                        sound = sound
                    )
                }
            }

            // Answer input
            item {
                AnswerInput(
                    theme = theme,
                    question = q,
                    userAnswer = answers[q.id],
                    feedback = questionFeedback,
                    onAnswer = { ans ->
                        sound.click()
                        answers[q.id] = ans
                    }
                )
            }
        }

        // Bottom navigation buttons
        Surface(color = theme.white, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        sound.click()
                        if (currentIdx > 0) {
                            currentIdx--
                            questionFeedback = null
                        }
                    },
                    enabled = currentIdx > 0,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev")
                }

                if (currentIdx < t.items.size - 1) {
                    Button(
                        onClick = {
                            sound.swoosh()
                            currentIdx++
                            questionFeedback = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            sound.swoosh()
                            submitting = true
                            scope.launch {
                                try {
                                    submitResult = AppState.api.submitTest(t.id, SubmitRequest(answers.toMap()))
                                    sound.success()
                                } catch (e: java.net.UnknownHostException) {
                                    error = "No internet connection. Please check your network."
                                } catch (e: java.io.IOException) {
                                    error = "Could not connect to server. Please check your internet."
                                } catch (e: Exception) {
                                    error = "Could not submit exam. Please try again."
                                }
                                submitting = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        enabled = !submitting
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Submit")
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
                            Text(opt, color = theme.darkText, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                            Text(opt, color = theme.darkText, fontSize = 14.sp, modifier = Modifier.weight(1f))
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

// ─── Async image (loads from URL) ─────────────────────────────────────────────
@Composable
fun AsyncImage(url: String, modifier: Modifier = Modifier) {
    // Use Coil or just a placeholder box for now (no external dep)
    // Real implementation would use coil-compose
    Box(
        modifier = modifier.background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text("Image: $url".take(40), color = Color.Gray, fontSize = 9.sp)
        }
    }
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
                            Text(opt, color = theme.darkText, fontSize = 12.sp, modifier = Modifier.weight(1f))
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
