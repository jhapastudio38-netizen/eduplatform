package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // Force landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    var tests by remember { mutableStateOf<List<app.dreamkorea.smartclass.api.EyeVisionTestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var currentIdx by remember { mutableStateOf(0) }
    var solvedCount by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var outcomes by remember { mutableStateOf<List<AnswerOutcome>>(emptyList()) }
    var optionsMap by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests(null)
            tests = resp.tests
            outcomes = tests.map { AnswerOutcome.PENDING }
            optionsMap = tests.mapIndexed { idx, test ->
                val correct = test.title.filter { it.isDigit() }.take(2).ifBlank { (1..99).random().toString() }
                val opts = mutableSetOf(correct)
                while (opts.size < 4) { opts.add((1..99).random().toString()) }
                idx to opts.toList().shuffled()
            }.toMap()
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

    // Results screen
    if (showResults) {
        val correct = outcomes.count { it == AnswerOutcome.CORRECT }
        val incorrect = outcomes.count { it == AnswerOutcome.INCORRECT }
        val skipped = outcomes.count { it == AnswerOutcome.SKIPPED }
        val total = tests.size
        val pct = if (total > 0) (correct * 100 / total) else 0

        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Eye Vision Screening Result", color = Color(0xFF1565FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Total", "$total", Color(0xFF1565FF))
                StatCard("Correct", "$correct", Color(0xFF4CAF50))
                StatCard("Incorrect", "$incorrect", Color(0xFFFF5252))
                StatCard("Skipped", "$skipped", Color(0xFFFF9800))
            }
            Spacer(Modifier.height(24.dp))
            Surface(color = Color(0xFF1565FF), shape = RoundedCornerShape(16.dp)) {
                Text("$pct%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("This is a screening result, not a medical diagnosis.\nPlease consult a qualified eye-care professional.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { currentIdx = 0; solvedCount = 0; selectedAnswer = ""; showResults = false; outcomes = tests.map { AnswerOutcome.PENDING } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF)), shape = RoundedCornerShape(12.dp)) { Text("Restart Test", color = Color.White, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("Back to Home") }
            }
        }
        return
    }

    // ── MAIN TEST UI — horizontal like exam ──
    val test = tests[currentIdx]
    val options = optionsMap[currentIdx] ?: emptyList()
    val imgUrl = test.imageUrl.let { if (it.startsWith("http")) it else "https://my-project-five-sepia.vercel.app$it" }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Top bar
        Surface(color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))) {
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text("Total: ${tests.size}", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text("${currentIdx + 1} / ${tests.size}", color = Color(0xFF1565FF), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(Color(0xFFE2E8F0)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text("Solved: $solvedCount", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // Main content: 50% image | 50% options
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Ishihara plate image
                Box(modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
                    coil.compose.AsyncImage(model = imgUrl, contentDescription = "Eye test plate ${currentIdx + 1}", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit)
                }
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                // RIGHT: Options
                Column(modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("What number do you see?", color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    options.forEachIndexed { i, opt ->
                        val isSelected = selectedAnswer == opt
                        Row(modifier = Modifier.fillMaxWidth(0.85f).padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color(0xFF1565FF) else Color.White).border(2.dp, if (isSelected) Color(0xFF1565FF) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp)).clickable(enabled = !isProcessing) { sound.click(); selectedAnswer = opt }, verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = if (isSelected) Color.White else Color(0xFF1565FF), shape = CircleShape, modifier = Modifier.size(36.dp).padding(start = 8.dp)) { Box(contentAlignment = Alignment.Center) { Text("${'A' + i}", color = if (isSelected) Color(0xFF1565FF) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
                            Spacer(Modifier.width(12.dp))
                            Text(opt, color = if (isSelected) Color.White else Color(0xFF1E293B), fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(0.85f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { if (isProcessing) return@OutlinedButton; isProcessing = true; sound.swoosh(); outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.SKIPPED }; selectedAnswer = ""; if (currentIdx < tests.size - 1) currentIdx++ else showResults = true; scope.launch { delay(200); isProcessing = false } }, enabled = !isProcessing, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { Text("Skip", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Button(onClick = { if (isProcessing || selectedAnswer.isBlank()) return@Button; isProcessing = true; sound.click(); scope.launch { try { val resp = AppState.api.checkEyeVisionAnswer(test.id, mapOf("answer" to selectedAnswer.trim())); if (resp.correct) { solvedCount++; outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.CORRECT }; sound.success() } else { outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.INCORRECT }; sound.error() } } catch (e: Exception) { outcomes = outcomes.toMutableList().also { it[currentIdx] = AnswerOutcome.INCORRECT }; sound.error() }; selectedAnswer = ""; if (currentIdx < tests.size - 1) currentIdx++ else showResults = true; delay(200); isProcessing = false } }, enabled = !isProcessing && selectedAnswer.isNotBlank(), modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565FF))) { if (isProcessing) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) } else { Text(if (currentIdx < tests.size - 1) "Next" else "Finish", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}
