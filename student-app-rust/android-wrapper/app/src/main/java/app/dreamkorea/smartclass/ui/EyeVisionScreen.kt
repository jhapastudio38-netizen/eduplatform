package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import app.dreamkorea.smartclass.api.EyeVisionTestItem
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

data class EyeVisionTest(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val category: String?,
    val level: Int
)

/**
 * Eye Vision test screen — landscape test mode with a numeric keypad.
 *
 * Layout:
 *  - Force landscape on entry, single DisposableEffect for orientation.
 *  - Top row: test image (the chart the student reads) — fills available width.
 *  - Middle: 3×4 numeric keypad (1-9, C, 0, ⌫). Uses weight(1f) so it fills the
 *    remaining height. Keys are big and tappable.
 *  - Bottom: Next + Skip buttons (always visible).
 *
 * Results screen: portrait, no stat boxes — only Back to Home.
 * Review screen: portrait, no stat boxes — only Back to Home.
 *
 * The `dreamkorea://auth-callback` redirect URL is NOT used by the eye test —
 * it's purely for Google Sign-In elsewhere in the app.
 */
@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<EyeVisionTest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Test state
    var currentIdx by remember { mutableStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var totalAnswered by remember { mutableStateOf(0) }
    val finished = remember { mutableStateOf(false) }

    fun toAbs(url: String): String =
        if (url.startsWith("http")) url else "https://my-project-five-sepia.vercel.app$url"

    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests(null)
            tests = resp.tests.map {
                EyeVisionTest(it.id, it.title, it.description, toAbs(it.imageUrl), it.category, it.level)
            }
            if (tests.isEmpty()) error = "No eye vision tests available."
        } catch (e: Exception) {
            error = "Could not load eye vision tests."
        }
        loading = false
    }

    // ── ORIENTATION ───────────────────────────────────────────────────
    // Force landscape while taking the test. When results/review show, we
    // switch to portrait. Single DisposableEffect — the value we set is
    // driven by the `finished` flag so re-renders don't pile up multiple
    // orientation requests.
    val targetOrientation = if (finished.value) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    DisposableEffect(targetOrientation) {
        val activity = context as? Activity
        activity?.requestedOrientation = targetOrientation
        onDispose { }
    }

    // ── LOADING ───────────────────────────────────────────────────────
    if (loading) {
        Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.primary)
        }
        return
    }

    // ── ERROR ─────────────────────────────────────────────────────────
    if (error.isNotEmpty()) {
        Column(
            Modifier.fillMaxSize().background(theme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(error, color = theme.subText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) {
                Text("Go back")
            }
        }
        return
    }

    // ── RESULTS / REVIEW (portrait, no stat boxes — only Back to Home) ──
    if (finished.value) {
        EyeVisionResultsScreen(theme, sound, correctCount, totalAnswered, onBack)
        return
    }

    val current = tests.getOrNull(currentIdx)
    if (current == null) {
        // Should never happen, but be defensive
        finished.value = true
        return
    }

    // ── TEST MODE (landscape) ─────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // Top header — thin status bar with question counter + back
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { sound.click(); onBack() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                }
                Text(
                    "Eye Vision Test",
                    color = Color(0xFF003478),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "${currentIdx + 1} / ${tests.size}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main test area — image on left, keypad on right (side by side, landscape)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // LEFT: Test image (the chart/number the student reads)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = current.imageUrl,
                    contentDescription = "Eye vision test image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { FullScreenImageViewer.show(current.imageUrl) },
                    contentScale = ContentScale.Fit
                )
            }

            // RIGHT: typed answer display + numeric keypad (3×4)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                // Typed answer display + result feedback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        typedAnswer.ifBlank { "—" },
                        color = if (resultText != null && isCorrect) Color(0xFF22C55E)
                                else if (resultText != null) Color(0xFFEF4444)
                                else Color(0xFF1E293B),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 3×4 numeric keypad: 1-9, C, 0, ⌫
                // Each row gets weight(1f) so the keypad fills available height.
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (key) {
                                                "C" -> Color(0xFFFEF3C7)
                                                "⌫" -> Color(0xFFFEE2E2)
                                                else -> Color.White
                                            }
                                        )
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .clickable {
                                            sound.click()
                                            resultText = null
                                            when (key) {
                                                "C" -> typedAnswer = ""
                                                "⌫" -> typedAnswer = typedAnswer.dropLast(1)
                                                else -> if (typedAnswer.length < 6) typedAnswer += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        key,
                                        color = when (key) {
                                            "C" -> Color(0xFFB45309)
                                            "⌫" -> Color(0xFFB91C1C)
                                            else -> Color(0xFF1E293B)
                                        },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom action bar: Next + Skip (always visible) ──────────
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Skip button — advances without checking
                OutlinedButton(
                    onClick = {
                        sound.swoosh()
                        typedAnswer = ""
                        resultText = null
                        if (currentIdx < tests.size - 1) {
                            currentIdx++
                        } else {
                            finished.value = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                ) {
                    Text("Skip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                // Next / Submit button — checks the answer
                Button(
                    onClick = {
                        if (typedAnswer.isBlank()) { sound.error(); return@Button }
                        checking = true
                        sound.click()
                        scope.launch {
                            try {
                                val resp = AppState.api.checkEyeVisionAnswer(
                                    current.id,
                                    mapOf("answer" to typedAnswer.trim())
                                )
                                isCorrect = resp.correct
                                resultText = if (resp.correct) "Correct!" else "Answer: ${resp.correctAnswer}"
                                totalAnswered++
                                if (resp.correct) {
                                    correctCount++
                                    sound.success()
                                } else {
                                    sound.error()
                                }
                                // Auto-advance after a short pause so the student sees the result
                                kotlinx.coroutines.delay(900)
                                typedAnswer = ""
                                if (currentIdx < tests.size - 1) {
                                    currentIdx++
                                } else {
                                    finished.value = true
                                }
                            } catch (e: Exception) {
                                resultText = "Could not check answer"
                                isCorrect = false
                                sound.error()
                            }
                            checking = false
                        }
                    },
                    enabled = !checking && typedAnswer.isNotBlank(),
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    if (checking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (currentIdx < tests.size - 1) "Next" else "Finish",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Results screen — portrait, no stat boxes. Shows final score and a single
 * "Back to Home" button. The student is sent here after finishing all tests.
 */
@Composable
private fun EyeVisionResultsScreen(
    theme: AppTheme,
    sound: SoundManager,
    correct: Int,
    total: Int,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { sound.success() }
    Column(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Test Complete",
            color = theme.darkText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$correct correct out of $total",
            color = theme.subText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { sound.click(); onBack() },
            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Home", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
