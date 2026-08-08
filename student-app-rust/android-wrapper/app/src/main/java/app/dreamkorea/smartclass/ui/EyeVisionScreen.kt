package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AnswerOutcome { PENDING, CORRECT, INCORRECT, SKIPPED }

@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tests by remember { mutableStateOf<List<app.dreamkorea.smartclass.api.EyeVisionTestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var currentIdx by remember { mutableStateOf(0) }
    var solvedCount by remember { mutableStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var outcomes by remember { mutableStateOf<List<AnswerOutcome>>(emptyList()) }
    var userAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var correctAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var showFinishConfirm by remember { mutableStateOf(false) }

    // Force landscape for the test AND results (only review is portrait).
    // This prevents the orientation glitch when switching between test and review.
    DisposableEffect(showReview) {
        val activity = context as? Activity
        if (!showReview) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests(null)
            tests = resp.tests
            outcomes = tests.map { AnswerOutcome.PENDING }
            userAnswers = tests.map { "" }
            correctAnswers = tests.map { "" }
        } catch (e: Exception) { errorMsg = "Could not load eye vision tests" }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF1565FF))
        }
        return
    }

    if (errorMsg.isNotEmpty()) {
        Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(errorMsg, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF))) { Text("Go Back") }
        }
        return
    }

    if (tests.isEmpty()) {
        Column(Modifier.fillMaxSize().background(Color.White).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Visibility, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("No eye vision tests yet", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        return
    }

    // ── REVIEW SCREEN — portrait, no stat boxes, only Back to Home ──
    if (showReview) {
        DisposableEffect(Unit) {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onDispose {
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(220.dp).alpha(0.12f),
                contentScale = ContentScale.Fit
            )
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Top bar
                Surface(color = Color(0xFF1565FF)) {
                    Row(modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Eye Test Review", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${tests.size} questions", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Each question card — optimized for portrait mobile
                tests.forEachIndexed { idx, t ->
                    val outcome = outcomes.getOrNull(idx) ?: AnswerOutcome.SKIPPED
                    val userAns = userAnswers.getOrNull(idx) ?: ""
                    val correctAns = correctAnswers.getOrNull(idx) ?: ""
                    val tImgUrl = t.imageUrl.let { if (it.startsWith("http")) it else "https://my-project-five-sepia.vercel.app$it" }
                    val statusText = when (outcome) {
                        AnswerOutcome.CORRECT -> "CORRECT"
                        AnswerOutcome.INCORRECT -> "INCORRECT"
                        AnswerOutcome.SKIPPED -> "SKIPPED"
                        AnswerOutcome.PENDING -> "NOT ANSWERED"
                    }
                    val statusColor = when (outcome) {
                        AnswerOutcome.CORRECT -> Color(0xFF4CAF50)
                        AnswerOutcome.INCORRECT -> Color(0xFFFF5252)
                        else -> Color(0xFFFF9800)
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Q${idx + 1}", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Surface(color = statusColor, shape = RoundedCornerShape(4.dp)) {
                                    Text(statusText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            coil.compose.AsyncImage(
                                model = tImgUrl,
                                contentDescription = "Plate ${idx + 1}",
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Your Answer:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (userAns.isBlank()) "—" else userAns, color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF0FDF4), shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Correct Answer:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (correctAns.isBlank()) "—" else correctAns, color = Color(0xFF16A34A), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // Bottom button — only Back to Home
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF))) {
                    Text("Back to Home", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        return
    }

    // ── RESULTS SCREEN — landscape (same as test) ──
    if (showResults) {
        val correct = outcomes.count { it == AnswerOutcome.CORRECT }
        val incorrect = outcomes.count { it == AnswerOutcome.INCORRECT }
        val total = tests.size
        val pct = if (total > 0) (correct * 100 / total) else 0

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(240.dp).alpha(0.12f),
                contentScale = ContentScale.Fit
            )
            Column(modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Spacer(Modifier.height(20.dp))
                Text("Eye Vision Screening Result", color = Color(0xFF1565FF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Surface(color = Color(0xFF1565FF), shape = RoundedCornerShape(16.dp)) {
                    Text("$pct%", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Score: $correct / $total", color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Text("This is a screening result, not a medical diagnosis.\nPlease consult a qualified eye-care professional.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showReview = true }, shape = RoundedCornerShape(12.dp)) { Text("Review Answers") }
                    OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("Back to Home") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        return
    }

    // Finish confirmation dialog
    if (showFinishConfirm) {
        val pending = outcomes.count { it == AnswerOutcome.PENDING }
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Submit Eye Test?") },
            text = { Text(if (pending == 0) "Submit your answers now?" else "You have $pending unanswered question(s). Submit anyway?", fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = {
                    showFinishConfirm = false
                    if (pending > 0) {
                        outcomes = outcomes.mapIndexed { i, o -> if (o == AnswerOutcome.PENDING) AnswerOutcome.SKIPPED else o }
                    }
                    showResults = true
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF))) { Text("Submit") }
            },
            dismissButton = { OutlinedButton(onClick = { showFinishConfirm = false }) { Text("Cancel") } }
        )
    }

    val test = tests[currentIdx]
    val imgUrl = test.imageUrl.let { if (it.startsWith("http")) it else "https://my-project-five-sepia.vercel.app$it" }
    val isLast = currentIdx == tests.size - 1

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Top bar (compact, like exam)
        Surface(color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))) {
            Row(modifier = Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Total: ${tests.size}", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("${currentIdx + 1} / ${tests.size}", color = Color(0xFF1565FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("Solved: $solvedCount", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // Main content: image (left 55%) | keyboard (right 45%) with watermark
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(220.dp).alpha(0.12f),
                contentScale = ContentScale.Fit
            )
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Ishihara plate image
                Box(modifier = Modifier.weight(0.55f).fillMaxHeight().padding(12.dp), contentAlignment = Alignment.Center) {
                    coil.compose.AsyncImage(
                        model = imgUrl,
                        contentDescription = "Eye test plate ${currentIdx + 1}",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))

                // RIGHT: Answer input + numeric keyboard — fills available height
                Column(modifier = Modifier.weight(0.45f).fillMaxHeight().padding(10.dp)) {
                    Text("What number do you see?", color = Color(0xFF1E293B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    // Answer display
                    Box(modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).border(2.dp, Color(0xFF1565FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text(
                            if (typedAnswer.isBlank()) "Type your answer" else typedAnswer,
                            color = if (typedAnswer.isBlank()) Color(0xFF94A3B8) else Color(0xFF0F172A),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Numeric keyboard — 3 rows of 3 (1-9), then 0 + Delete, fills remaining space
                    val keyColor = Color(0xFF1565FF)

                    // Row 1: 1 2 3
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("1", "2", "3").forEach { k ->
                            KeyButton(k, keyColor, Modifier.weight(1f).fillMaxHeight()) { sound.click(); if (typedAnswer.length < 3) typedAnswer += k }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    // Row 2: 4 5 6
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("4", "5", "6").forEach { k ->
                            KeyButton(k, keyColor, Modifier.weight(1f).fillMaxHeight()) { sound.click(); if (typedAnswer.length < 3) typedAnswer += k }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    // Row 3: 7 8 9
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("7", "8", "9").forEach { k ->
                            KeyButton(k, keyColor, Modifier.weight(1f).fillMaxHeight()) { sound.click(); if (typedAnswer.length < 3) typedAnswer += k }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    // Row 4: 0 (weight 1) + Delete (weight 2)
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        KeyButton("0", keyColor, Modifier.weight(1f).fillMaxHeight()) { sound.click(); if (typedAnswer.length < 3) typedAnswer += "0" }
                        OutlinedButton(
                            onClick = { sound.click(); if (typedAnswer.isNotEmpty()) typedAnswer = typedAnswer.dropLast(1) },
                            modifier = Modifier.weight(2f).fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Backspace, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // Next / Submit + Skip
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (isProcessing) return@OutlinedButton
                                isProcessing = true; sound.swoosh()
                                outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.SKIPPED }
                                typedAnswer = ""
                                if (currentIdx < tests.size - 1) currentIdx++ else showFinishConfirm = true
                                scope.launch { delay(150); isProcessing = false }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Skip", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (isProcessing || typedAnswer.isBlank()) return@Button
                                isProcessing = true; sound.click()
                                scope.launch {
                                    try {
                                        val resp = AppState.api.checkEyeVisionAnswer(test.id, mapOf("answer" to typedAnswer.trim()))
                                        userAnswers = userAnswers.toMutableList().also { it[currentIdx] = typedAnswer.trim() }
                                        correctAnswers = correctAnswers.toMutableList().also { it[currentIdx] = resp.correctAnswer.ifBlank { typedAnswer.trim() } }
                                        if (resp.correct) {
                                            solvedCount++
                                            outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.CORRECT }
                                            sound.success()
                                        } else {
                                            outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.INCORRECT }
                                            sound.error()
                                        }
                                    } catch (e: Exception) {
                                        userAnswers = userAnswers.toMutableList().also { it[currentIdx] = typedAnswer.trim() }
                                        outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.INCORRECT }
                                        sound.error()
                                    }
                                    typedAnswer = ""
                                    if (currentIdx < tests.size - 1) currentIdx++ else showFinishConfirm = true
                                    delay(150)
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing && typedAnswer.isNotBlank(),
                            modifier = Modifier.weight(2f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isLast) "Submit" else "Next", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
