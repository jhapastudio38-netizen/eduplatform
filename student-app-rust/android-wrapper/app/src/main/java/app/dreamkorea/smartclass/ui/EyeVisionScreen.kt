package app.dreamkorea.smartclass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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

@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<EyeVisionTest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    // Adaptive state — surfaced from the API
    var currentLevel by remember { mutableStateOf(1) }
    var statsAccuracy by remember { mutableStateOf(0) }
    var statsAttempts by remember { mutableStateOf(0) }
    var statsStreak by remember { mutableStateOf(0) }
    var leveledUpBanner by remember { mutableStateOf(false) }
    var leveledDownBanner by remember { mutableStateOf(false) }

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
                currentLevel = resp.level
                statsAccuracy = resp.stats.accuracy
                statsAttempts = resp.stats.totalAttempts
                statsStreak = resp.stats.consecutiveCorrect
            } catch (e: Exception) {
                error = "Could not load eye vision tests"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadTests(adaptive = true) }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Eye Vision Test", "Adaptive — Level $currentLevel", onBack)

        // Adaptive stats banner — only shown once the student has attempted at least 1 test
        if (statsAttempts > 0) {
            Surface(
                color = theme.cardBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Level $currentLevel of 5",
                            color = theme.darkText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Streak: $statsStreak · Accuracy: $statsAccuracy% · $statsAttempts attempts",
                            color = theme.subText,
                            fontSize = 12.sp
                        )
                    }
                    // Level-up / level-down chips fire for a moment after each attempt
                    AnimatedVisibility(
                        visible = leveledUpBanner,
                        enter = fadeIn() + scaleIn(initialScale = 0.7f),
                        exit = fadeOut() + scaleOut(targetScale = 0.7f)
                    ) {
                        Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(8.dp)) {
                            Text("Level up! 🎉", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    AnimatedVisibility(
                        visible = leveledDownBanner,
                        enter = fadeIn() + scaleIn(initialScale = 0.7f),
                        exit = fadeOut() + scaleOut(targetScale = 0.7f)
                    ) {
                        Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(8.dp)) {
                            Text("Easier next 👍", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
            return
        }

        if (error.isNotEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
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
                Modifier.fillMaxSize().padding(32.dp),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tests) { test ->
                EyeVisionTestCard(
                    theme = theme,
                    sound = sound,
                    test = test,
                    onResult = { leveledUp, leveledDown, accuracy, attempts, streak, level ->
                        // Update banner + stats so the UI reflects the new adaptive state
                        if (leveledUp) {
                            leveledUpBanner = true
                            sound.success()
                        } else if (leveledDown) {
                            leveledDownBanner = true
                        }
                        // Auto-hide the banner after 1.6 seconds
                        scope.launch {
                            kotlinx.coroutines.delay(1600)
                            leveledUpBanner = false
                            leveledDownBanner = false
                        }
                        currentLevel = level
                        statsAccuracy = accuracy
                        statsAttempts = attempts
                        statsStreak = streak
                        // If the level changed, reload tests at the new level
                        // after a short delay so the user sees the banner first
                        if (leveledUp || leveledDown) {
                            scope.launch {
                                kotlinx.coroutines.delay(1800)
                                loadTests(adaptive = true)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EyeVisionTestCard(
    theme: AppTheme,
    sound: SoundManager,
    test: EyeVisionTest,
    onResult: (leveledUp: Boolean, leveledDown: Boolean, accuracy: Int, attempts: Int, streak: Int, level: Int) -> Unit
) {
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(test.title, color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                // Level chip
                Surface(
                    color = when (test.level) {
                        1 -> Color(0xFFE8F5E9); 2 -> Color(0xFFFFF9C4); 3 -> Color(0xFFFFE0B2)
                        4 -> Color(0xFFFFCCBC); else -> Color(0xFFFFCDD2)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "L${test.level}",
                        color = Color(0xFF424242),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (!test.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(test.description!!, color = theme.subText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Image — tap to view full-screen
            coil.compose.AsyncImage(
                model = test.imageUrl,
                contentDescription = "Eye vision test image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        // Open full-screen image viewer
                        FullScreenImageViewer.show(test.imageUrl)
                    },
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(12.dp))

            // Answer input
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it; result = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type what you see") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.darkText,
                    unfocusedTextColor = theme.darkText,
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.divider,
                    cursorColor = theme.primary,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(20.dp)) }
            )

            result?.let { r ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(r, color = if (isCorrect) Color(0xFF34C759) else theme.errorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (answer.isBlank()) { sound.error(); return@Button }
                    checking = true; result = null
                    sound.click()
                    scope.launch {
                        try {
                            val resp = AppState.api.checkEyeVisionAnswer(
                                test.id,
                                mapOf("answer" to answer.trim())
                            )
                            isCorrect = resp.correct
                            result = if (resp.correct) "Correct! 🎉" else "Incorrect — answer was: ${resp.correctAnswer}"
                            onResult(resp.leveledUp, resp.leveledDown, resp.stats.accuracy, resp.stats.totalAttempts, resp.stats.consecutiveCorrect, resp.nextLevel)
                            if (resp.correct) sound.success() else sound.error()
                        } catch (e: Exception) {
                            result = "Could not check answer — try again"
                            isCorrect = false
                            sound.error()
                        }
                        checking = false
                    }
                },
                enabled = !checking && answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) {
                if (checking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check Answer", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
