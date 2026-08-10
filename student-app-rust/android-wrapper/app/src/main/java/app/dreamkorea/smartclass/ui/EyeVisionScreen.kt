package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Outcome tracking ──────────────────────────────────────────────────────
enum class AnswerOutcome { PENDING, CORRECT, INCORRECT, SKIPPED }

// Internal mode for the 3-stage flow: Testing (landscape) → Results (portrait)
// → Review (portrait).
private enum class EyeVisionScreenMode { Testing, Results, Review }

data class EyeVisionTest(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val category: String?,
    val level: Int
)

/**
 * EyeVisionScreen — full rewrite for FIX-3.
 *
 * 3-stage flow with a single source of truth for orientation:
 *   • Testing   → landscape, row layout: plate image (left) + numeric keypad (right)
 *   • Results   → portrait, score display + 2 buttons
 *   • Review    → portrait, scrollable list of all questions with answers + outcomes
 *
 * Orientation:
 *   - `LaunchedEffect(Unit)` forces landscape 50ms after entry so the user
 *     lands in the test directly in landscape.
 *   - `DisposableEffect(targetOrientation)` is the single source of truth —
 *     it follows the current mode and rotates to portrait when leaving the
 *     testing stage.
 *
 * State tracking:
 *   - userAnswers    — per-question answer the user typed ("") if skipped
 *   - correctAnswers — per-question correct answer returned by the server
 *   - outcomes       — per-question PENDING / CORRECT / INCORRECT / SKIPPED
 */
@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tests by remember { mutableStateOf<List<EyeVisionTest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Test flow state
    var currentIndex by remember { mutableStateOf(0) }
    var currentAnswer by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(EyeVisionScreenMode.Testing) }
    var checking by remember { mutableStateOf(false) }

    // Per-question tracking (length matches `tests` once loaded)
    var userAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var correctAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var outcomes by remember { mutableStateOf<List<AnswerOutcome>>(emptyList()) }

    // Single source of truth for orientation — landscape while testing,
    // portrait for results / review so the user can read everything.
    val targetOrientation = when (mode) {
        EyeVisionScreenMode.Testing -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        EyeVisionScreenMode.Results, EyeVisionScreenMode.Review -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // Force landscape shortly after entry (50ms delay) so the test opens
    // directly in landscape even if the user came from portrait.
    LaunchedEffect(Unit) {
        delay(50)
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    // Single source of truth — switches orientation whenever the mode changes.
    DisposableEffect(targetOrientation) {
        val activity = context as? Activity
        activity?.requestedOrientation = targetOrientation
        onDispose { /* MainScreen restores orientation when leaving */ }
    }

    fun toAbs(url: String): String =
        if (url.startsWith("http")) url else "https://my-project-five-sepia.vercel.app$url"

    // Load all tests up-front (non-adaptive) so we can run the whole flow
    // client-side and grade at the end.
    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests(null)
            tests = resp.tests.map {
                EyeVisionTest(it.id, it.title, it.description, toAbs(it.imageUrl), it.category, it.level)
            }
            userAnswers = List(resp.tests.size) { "" }
            correctAnswers = List(resp.tests.size) { "" }
            outcomes = List(resp.tests.size) { AnswerOutcome.PENDING }
        } catch (e: Exception) {
            error = "Could not load eye vision tests"
        }
        loading = false
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(theme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = theme.primary)
        }
        return
    }

    if (error.isNotEmpty() || tests.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().background(theme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (error.isNotEmpty()) error else "No eye vision tests yet",
                color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your teacher will add tests here soon",
                color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) {
                Text("Go back")
            }
        }
        return
    }

    // Submit the current answer (or skip) and advance to the next question.
    fun submitAnswer(skip: Boolean) {
        if (checking) return
        if (!skip && currentAnswer.isBlank()) {
            sound.error()
            return
        }
        checking = true
        val test = tests[currentIndex]
        val answerValue = if (skip) "" else currentAnswer.trim()
        scope.launch {
            var outcome = AnswerOutcome.SKIPPED
            var correctAnswerVal = ""
            if (!skip) {
                try {
                    val resp = AppState.api.checkEyeVisionAnswer(test.id, mapOf("answer" to answerValue))
                    outcome = if (resp.correct) AnswerOutcome.CORRECT else AnswerOutcome.INCORRECT
                    correctAnswerVal = resp.correctAnswer
                    if (resp.correct) sound.success() else sound.error()
                } catch (e: Exception) {
                    outcome = AnswerOutcome.INCORRECT
                    correctAnswerVal = "?"
                    sound.error()
                }
            } else {
                sound.click()
            }

            // Record per-question state
            userAnswers = userAnswers.toMutableList().also { it[currentIndex] = answerValue }
            correctAnswers = correctAnswers.toMutableList().also { it[currentIndex] = correctAnswerVal }
            outcomes = outcomes.toMutableList().also { it[currentIndex] = outcome }

            checking = false

            if (currentIndex + 1 < tests.size) {
                currentIndex += 1
                currentAnswer = ""
            } else {
                // Reached the end → go to results (portrait)
                mode = EyeVisionScreenMode.Results
                currentIndex = 0
                currentAnswer = ""
            }
        }
    }

    when (mode) {
        EyeVisionScreenMode.Testing -> TestingScreen(
            theme = theme,
            sound = sound,
            tests = tests,
            currentIndex = currentIndex,
            currentAnswer = currentAnswer,
            checking = checking,
            onAnswerChange = { currentAnswer = it },
            onNext = { submitAnswer(skip = false) },
            onSkip = { submitAnswer(skip = true) },
            onClose = onBack
        )
        EyeVisionScreenMode.Results -> ResultsScreenView(
            theme = theme,
            tests = tests,
            outcomes = outcomes,
            onReview = { mode = EyeVisionScreenMode.Review },
            onHome = onBack
        )
        EyeVisionScreenMode.Review -> ReviewScreenView(
            theme = theme,
            tests = tests,
            userAnswers = userAnswers,
            correctAnswers = correctAnswers,
            outcomes = outcomes,
            onHome = onBack
        )
    }
}

// ─── TESTING SCREEN (landscape) ──────────────────────────────────────────────
// Row layout: LEFT = Ishihara plate image (fills space),
//             RIGHT = answer box + 3×4 numeric keypad + Next/Skip buttons.
@Composable
private fun TestingScreen(
    theme: AppTheme,
    sound: SoundManager,
    tests: List<EyeVisionTest>,
    currentIndex: Int,
    currentAnswer: String,
    checking: Boolean,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // ── Top bar: question counter + close button ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { sound.click(); onClose() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, "Close test", tint = theme.darkText, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Question ${currentIndex + 1} of ${tests.size}",
                color = theme.darkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Surface(color = theme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    "Level ${tests[currentIndex].level}",
                    color = theme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // ── Main row: image (left) + keypad (right) ──────────────────────
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LEFT: Ishihara plate image — fills available space
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Slide animation between questions
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            (slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(300)) { w -> w } +
                                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300))) togetherWith
                                    (slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(300)) { w -> -w } +
                                            fadeOut(animationSpec = androidx.compose.animation.core.tween(300)))
                        },
                        label = "plateSlide"
                    ) { idx ->
                        val url = tests[idx].imageUrl
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = "Ishihara plate ${idx + 1}",
                            modifier = Modifier.fillMaxSize().clickable { FullScreenImageViewer.show(url) },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // RIGHT: Answer display + keypad + buttons — keypad uses weight(1f)
            // to fill all remaining vertical space.
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Answer display box at top
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, theme.primary),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = if (currentAnswer.isEmpty()) "Enter the number you see" else currentAnswer,
                            color = if (currentAnswer.isEmpty()) theme.subText else theme.darkText,
                            fontSize = if (currentAnswer.isEmpty()) 13.sp else 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Numeric keypad: 3×4 grid (1-9, C, 0, ⌫) — fills height with weight(1f)
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (rowIdx in 0 until 4) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (colIdx in 0 until 3) {
                                val key = keys[rowIdx * 3 + colIdx]
                                Surface(
                                    color = when (key) {
                                        "C" -> Color(0xFFFFEBEE)
                                        "⌫" -> Color(0xFFFFF3E0)
                                        else -> theme.cardBg
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, theme.divider),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            sound.click()
                                            when (key) {
                                                "C" -> onAnswerChange("")
                                                "⌫" -> onAnswerChange(
                                                    if (currentAnswer.isNotEmpty()) currentAnswer.dropLast(1)
                                                    else currentAnswer
                                                )
                                                else -> if (currentAnswer.length < 3) onAnswerChange(currentAnswer + key)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            key,
                                            color = when (key) {
                                                "C" -> theme.errorRed
                                                "⌫" -> Color(0xFFE65100)
                                                else -> theme.darkText
                                            },
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Next + Skip buttons at bottom (always visible, not inside scroll)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !checking,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.darkText)
                    ) {
                        Text("Skip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !checking && currentAnswer.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                    ) {
                        if (checking) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (currentIndex + 1 < tests.size) "Next" else "Finish",
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── RESULTS SCREEN (portrait) ───────────────────────────────────────────────
// Shows score (correct/total) + 2 buttons. No stat boxes.
@Composable
private fun ResultsScreenView(
    theme: AppTheme,
    tests: List<EyeVisionTest>,
    outcomes: List<AnswerOutcome>,
    onReview: () -> Unit,
    onHome: () -> Unit
) {
    val correct = outcomes.count { it == AnswerOutcome.CORRECT }
    val total = tests.size
    val pct = if (total > 0) (correct * 100) / total else 0

    Column(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(24.dp).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Visibility,
            null,
            tint = theme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Test Complete",
            color = theme.darkText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("Your score", color = theme.subText, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "$correct / $total",
            color = theme.primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text("$pct% correct", color = theme.subText, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onReview,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
        ) {
            Text("Review Answers", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.darkText)
        ) {
            Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── REVIEW SCREEN (portrait) ────────────────────────────────────────────────
// Scrollable list of all questions. Each shows: plate image, "Your Answer",
// "Correct Answer", and a CORRECT/INCORRECT badge. No stat boxes.
@Composable
private fun ReviewScreenView(
    theme: AppTheme,
    tests: List<EyeVisionTest>,
    userAnswers: List<String>,
    correctAnswers: List<String>,
    outcomes: List<AnswerOutcome>,
    onHome: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Review Answers",
                color = theme.darkText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Scrollable list of all questions
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tests) { idx, test ->
                val outcome = outcomes.getOrNull(idx) ?: AnswerOutcome.PENDING
                val userAns = userAnswers.getOrNull(idx) ?: ""
                val correctAns = correctAnswers.getOrNull(idx) ?: ""

                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Question ${idx + 1}",
                                color = theme.subText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            // CORRECT / INCORRECT / SKIPPED badge
                            val (badgeColor, badgeText) = when (outcome) {
                                AnswerOutcome.CORRECT -> Color(0xFF4CAF50) to "CORRECT"
                                AnswerOutcome.INCORRECT -> Color(0xFFFF5252) to "INCORRECT"
                                AnswerOutcome.SKIPPED -> Color(0xFFFF9800) to "SKIPPED"
                                AnswerOutcome.PENDING -> Color(0xFF9E9E9E) to "PENDING"
                            }
                            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    badgeText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Plate image (tap to view full-screen)
                        coil.compose.AsyncImage(
                            model = test.imageUrl,
                            contentDescription = "Plate ${idx + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { FullScreenImageViewer.show(test.imageUrl) },
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Your Answer: ${if (userAns.isBlank()) "—" else userAns}",
                            color = theme.darkText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Correct Answer: $correctAns",
                            color = theme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Back to Home button at bottom
        Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Button(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) {
                Text("Back to Home", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
