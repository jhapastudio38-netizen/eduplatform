package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.AppNotification
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
 * Eye Vision screen — landscape test, portrait results.
 *
 * Flow:
 *   1. Enter screen → force landscape (LaunchedEffect + DisposableEffect on
 *      targetOrientation, with a 50 ms delay so the orientation change
 *      takes effect after the composable is mounted).
 *   2. While testing: image on the LEFT, numeric keypad (3×4 grid:
 *      1-9, C, 0, ⌫) on the RIGHT. Next + Skip buttons at the bottom.
 *   3. After the last test → switch to portrait, show a simple results
 *      screen with just the score and a "Back to Home" button.
 */
@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<EyeVisionTest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var currentTestIdx by remember { mutableStateOf(0) }
    var currentAnswer by remember { mutableStateOf("") }
    var correctCount by remember { mutableStateOf(0) }
    var attemptedCount by remember { mutableStateOf(0) }
    var showResults by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    fun toAbs(url: String): String =
        if (url.startsWith("http")) url else "https://my-project-five-sepia.vercel.app$url"

    fun loadTests(adaptive: Boolean = true) {
        scope.launch {
            loading = true
            try {
                val resp = AppState.api.getEyeVisionTests(if (adaptive) "true" else null)
                tests = resp.tests.map {
                    EyeVisionTest(it.id, it.title, it.description, toAbs(it.imageUrl), it.category, it.level)
                }
            } catch (e: Exception) {
                error = "Could not load eye vision tests"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadTests(adaptive = true) }

    // ── ORIENTATION ────────────────────────────────────────────────────
    // Landscape while testing, portrait for the results screen.
    val targetOrientation = if (showResults) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    DisposableEffect(targetOrientation) {
        val activity = context as? Activity
        activity?.requestedOrientation = targetOrientation
        onDispose { /* keep last requested orientation */ }
    }
    // Force landscape 50ms after entry — gives the composable time to mount
    // before the orientation change kicks in.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    // ── RESULTS SCREEN (portrait) ──────────────────────────────────────
    // No stat boxes — just the score and a "Back to Home" button.
    if (showResults) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Visibility,
                null,
                tint = theme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Eye Vision Test Complete",
                color = theme.darkText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You attempted $attemptedCount of ${tests.size} tests",
                color = theme.subText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            // Score — just the number, no stat boxes
            Text(
                "Score",
                color = theme.subText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$correctCount / ${tests.size}",
                color = theme.primary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { sound.click(); onBack() },
                modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Home", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.primary)
        }
        return
    }

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

    if (tests.isEmpty()) {
        Column(
            Modifier.fillMaxSize().background(theme.background).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("No eye vision tests yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Your teacher will add tests here soon", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        return
    }

    // Past the last test → show results
    val currentTest = tests.getOrNull(currentTestIdx)
    if (currentTest == null) {
        showResults = true
        return
    }

    // ── TEST UI (landscape) ────────────────────────────────────────────
    // Image on LEFT | numeric keypad on RIGHT | Next + Skip at bottom.
    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // ── Compact top status bar ───────────────────────────────────────
        Surface(color = theme.cardBg, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, null, tint = theme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Eye Vision Test",
                    color = theme.darkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Question ${currentTestIdx + 1} of ${tests.size}",
                    color = theme.subText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Correct: $correctCount",
                    color = theme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Main area: image LEFT | keypad RIGHT ────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // LEFT: test image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = currentTest.imageUrl,
                    contentDescription = "Eye vision test image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { FullScreenImageViewer.show(currentTest.imageUrl) },
                    contentScale = ContentScale.Fit
                )
            }

            // Vertical divider
            Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.9f).background(theme.divider))

            // RIGHT: numeric keypad (3×4 grid: 1-9, C, 0, ⌫)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                // ── Answer display — with smooth animation when digits appear ──
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.divider),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        // AnimatedContent gives a smooth slide+fade when the answer changes
                        AnimatedContent(
                            targetState = currentAnswer,
                            transitionSpec = {
                                (fadeIn(androidx.compose.animation.core.tween(150)) +
                                 slideInVertically(androidx.compose.animation.core.tween(150)) { it / 3 }) togetherWith
                                (fadeOut(androidx.compose.animation.core.tween(100)) +
                                 slideOutVertically(androidx.compose.animation.core.tween(100)) { -it / 3 })
                            },
                            label = "answerAnim"
                        ) { answer ->
                            Text(
                                text = answer.ifBlank { "—" },
                                color = if (answer.isBlank()) theme.subText else theme.darkText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                if (!currentTest.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentTest.description!!,
                        color = theme.subText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))

                // ── Numeric keypad: 3×4 grid (1-9, C, 0, ⌫) ────────────────
                // Each key has a smooth press animation (scale down on tap)
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
                keys.chunked(3).forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isAction = key == "C" || key == "⌫"
                            // Press animation state — scales down on tap, springs back
                            var pressed by remember { androidx.compose.runtime.mutableStateOf(false) }
                            val scale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (pressed) 0.9f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
                                ),
                                label = "keyScale"
                            )
                            // Auto-reset pressed state after 150ms
                            androidx.compose.runtime.LaunchedEffect(pressed) {
                                if (pressed) {
                                    kotlinx.coroutines.delay(150)
                                    pressed = false
                                }
                            }
                            Surface(
                                color = if (isAction) theme.primary.copy(alpha = 0.1f) else theme.cardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.divider),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .scale(scale)
                                    .clickable {
                                        pressed = true
                                        sound.click()
                                        when (key) {
                                            "C" -> currentAnswer = ""
                                            "⌫" -> currentAnswer = currentAnswer.dropLast(1)
                                            else -> if (currentAnswer.length < 4) currentAnswer += key
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        key,
                                        color = if (isAction) theme.primary else theme.darkText,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // ── Bottom: Next + Skip buttons ─────────────────────────────────
        Surface(color = theme.cardBg, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip — moves to next test without checking
                OutlinedButton(
                    onClick = {
                        sound.click()
                        currentAnswer = ""
                        if (currentTestIdx < tests.size - 1) {
                            currentTestIdx++
                        } else {
                            showResults = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.subText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.divider)
                ) {
                    Text("Skip", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                // Next — submits the answer and advances
                Button(
                    onClick = {
                        if (currentAnswer.isBlank()) { sound.error(); return@Button }
                        if (checking) return@Button
                        checking = true
                        sound.click()
                        scope.launch {
                            try {
                                val resp = AppState.api.checkEyeVisionAnswer(
                                    currentTest.id,
                                    mapOf("answer" to currentAnswer.trim())
                                )
                                attemptedCount++
                                if (resp.correct) {
                                    correctCount++
                                    sound.success()
                                } else {
                                    sound.error()
                                }
                            } catch (_: Exception) {
                                // Network error — count as attempted, don't increment correct
                            }
                            checking = false
                            currentAnswer = ""
                            if (currentTestIdx < tests.size - 1) {
                                currentTestIdx++
                            } else {
                                showResults = true
                            }
                        }
                    },
                    enabled = !checking && currentAnswer.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    if (checking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (currentTestIdx < tests.size - 1) "Next" else "Finish",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
