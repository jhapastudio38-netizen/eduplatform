package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.EyeVisionTestItem
import app.dreamkorea.smartclass.data.AppState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Ishihara color vision eye-test screen ───────────────────────────────────
// Landscape-only, split-pane UI: left = large plate image, right = stats +
// numeric keypad + Skip/Next controls. After every question is answered or
// skipped, a results screen summarizes the run.

/** Bright blue used for the status box and primary action buttons. */
private val IshiharaBlue = Color(0xFF1565FF)

/** Outcome recorded for each individual plate after the user moves on. */
private enum class AnswerOutcome { PENDING, CORRECT, INCORRECT, SKIPPED }

// `String.toAbsoluteUrl()` is defined in ExamScreen.kt (same package) and reused here.

@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tests by remember { mutableStateOf<List<EyeVisionTestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    var currentQuestion by remember { mutableStateOf(0) }
    var solvedCount by remember { mutableStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var outcomes by remember { mutableStateOf<List<AnswerOutcome>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }

    // ── 1. Forced landscape orientation — restore to portrait on dispose ──
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // ── Load all tests on screen open ──
    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests()
            tests = resp.tests
            outcomes = List(resp.tests.size) { AnswerOutcome.PENDING }
            currentQuestion = 0
            solvedCount = 0
            userAnswer = ""
            showResults = false
        } catch (e: Exception) {
            error = "Could not load eye vision tests. ${e.message ?: ""}"
        }
        loading = false
    }

    // ── Preload next plate image into Coil's memory cache ──
    LaunchedEffect(currentQuestion, tests) {
        if (tests.isNotEmpty() && currentQuestion + 1 < tests.size) {
            val nextUrl = tests[currentQuestion + 1].imageUrl.toAbsoluteUrl()
            val imageLoader = coil.Coil.imageLoader(context)
            imageLoader.enqueue(
                ImageRequest.Builder(context).data(nextUrl).build()
            )
        }
    }

    // ── Loading state ──
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = IshiharaBlue)
        }
        return
    }

    // ── Error state ──
    if (error.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                error,
                color = Color(0xFF333333),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { sound.click(); onBack() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IshiharaBlue)
            ) {
                Text("Go Back", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // ── Empty state ──
    if (tests.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No eye vision tests available",
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your teacher has not added any tests yet.",
                color = Color(0xFF888888),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { sound.click(); onBack() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IshiharaBlue)
            ) {
                Text("Back to Home", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // ── Results screen ──
    if (showResults) {
        EyeVisionResultsScreen(
            total = tests.size,
            correct = solvedCount,
            incorrect = outcomes.count { it == AnswerOutcome.INCORRECT },
            skipped = outcomes.count { it == AnswerOutcome.SKIPPED },
            onRestart = {
                sound.click()
                currentQuestion = 0
                solvedCount = 0
                userAnswer = ""
                outcomes = List(tests.size) { AnswerOutcome.PENDING }
                showResults = false
            },
            onBackHome = {
                sound.click()
                onBack()
            }
        )
        return
    }

    // ── Advance to the next plate, or finish the test ──
    fun advance() {
        if (currentQuestion + 1 >= tests.size) {
            showResults = true
        } else {
            currentQuestion++
        }
    }

    // ── Main split-pane UI ──
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        // ── Left half: large Ishihara plate image ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val test = tests.getOrNull(currentQuestion)
            if (test != null) {
                AsyncImage(
                    model = test.imageUrl.toAbsoluteUrl(),
                    contentDescription = "Ishihara plate ${currentQuestion + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // ── Right half: control panel ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats row: Total on left, Solved on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: ${tests.size}",
                    color = Color(0xFF333333),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Solved: $solvedCount",
                    color = Color(0xFF333333),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Blue progress box — "current / total" centered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = IshiharaBlue,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "${currentQuestion + 1} / ${tests.size}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                    )
                }
            }

            // Answer display field — large current input
            Surface(
                color = Color(0xFFF5F6F8),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userAnswer.isEmpty()) "—" else userAnswer,
                        color = if (userAnswer.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF111111),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Numeric keypad — 3 cols × 4 rows
            Keypad(
                onDigit = { digit ->
                    sound.click()
                    if (userAnswer.length < 4) {
                        userAnswer += digit
                    }
                },
                onBackspace = {
                    sound.click()
                    if (userAnswer.isNotEmpty()) {
                        userAnswer = userAnswer.dropLast(1)
                    }
                }
            )

            // Skip / Next action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip — marks as skipped, advances, clears answer
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        isProcessing = true
                        sound.swoosh()
                        val updated = outcomes.toMutableList()
                        updated[currentQuestion] = AnswerOutcome.SKIPPED
                        outcomes = updated
                        userAnswer = ""
                        advance()
                        scope.launch {
                            delay(180) // brief guard against double-tap
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IshiharaBlue)
                ) {
                    Text("Skip", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                // Next — submits answer to API, advances on response
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        if (userAnswer.isBlank()) {
                            sound.error()
                            return@Button
                        }
                        isProcessing = true
                        sound.click()
                        val test = tests[currentQuestion]
                        val submitted = userAnswer.trim()
                        scope.launch {
                            try {
                                val resp = AppState.api.checkEyeVisionAnswer(
                                    test.id,
                                    mapOf("answer" to submitted)
                                )
                                val updated = outcomes.toMutableList()
                                updated[currentQuestion] =
                                    if (resp.correct) AnswerOutcome.CORRECT else AnswerOutcome.INCORRECT
                                outcomes = updated
                                if (resp.correct) {
                                    solvedCount++
                                    sound.success()
                                } else {
                                    sound.error()
                                }
                            } catch (e: Exception) {
                                // Network/API failure — count as incorrect so flow continues
                                val updated = outcomes.toMutableList()
                                updated[currentQuestion] = AnswerOutcome.INCORRECT
                                outcomes = updated
                                sound.error()
                            }
                            userAnswer = ""
                            advance()
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing && userAnswer.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IshiharaBlue)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Next", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Numeric keypad ────────────────────────────────────────────────────────────

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "." -> { /* disabled — Ishihara answers are integers */ }
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val disabled = key == "."
    Surface(
        color = if (disabled) Color(0xFFE6E8EC) else Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (disabled) 0.dp else 2.dp,
        modifier = modifier
            .height(46.dp)
            .clickable(enabled = !disabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                key == "⌫" -> {
                    Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = Color(0xFF222222),
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Text(
                        key,
                        color = if (disabled) Color(0xFFB0B4BC) else Color(0xFF111111),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Results screen ────────────────────────────────────────────────────────────

@Composable
private fun EyeVisionResultsScreen(
    total: Int,
    correct: Int,
    incorrect: Int,
    skipped: Int,
    onRestart: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Test Complete",
            color = Color(0xFF111111),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        // 2×2 stat grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultStatBox("Total", total, Modifier.weight(1f), Color(0xFF607D8B))
            ResultStatBox("Correct", correct, Modifier.weight(1f), Color(0xFF34A853))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultStatBox("Incorrect", incorrect, Modifier.weight(1f), Color(0xFFEA4335))
            ResultStatBox("Skipped", skipped, Modifier.weight(1f), Color(0xFFFB8C00))
        }

        Spacer(Modifier.height(20.dp))

        // Score percentage banner
        val score = if (total > 0) (correct * 100 / total) else 0
        Surface(
            color = IshiharaBlue,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Score", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("$score%", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Medical disclaimer
        Text(
            "This is a screening result, not a medical diagnosis. " +
                "Please consult a qualified eye-care professional for a comprehensive evaluation.",
            color = Color(0xFF888888),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IshiharaBlue)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Restart Test", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBackHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IshiharaBlue)
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Back to Home", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultStatBox(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$value", color = Color(0xFF111111), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
