package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.EyeVisionTestItem
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// EYE VISION SCREEN — exam-style UI
// Admin adds images with correct answers. Student sees the image and
// selects from number options (0-9, 10, 21, 79, 99, 100, etc.)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    var tests by remember { mutableStateOf<List<EyeVisionTestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var currentIdx by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        try {
            val resp = AppState.api.getEyeVisionTests()
            tests = resp.tests
        } catch (e: Exception) {
            error = "Could not load eye vision tests."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Eye Vision Test", "Identify what you see", onBack)

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.primary)
                }
            }
            error.isNotEmpty() -> {
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
            }
            tests.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No eye vision tests yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Your teacher will add tests here soon.", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            finished -> {
                // Result
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = if (correctCount >= tests.size / 2) Color(0xFF22C55E) else Color(0xFFEF4444),
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (correctCount >= tests.size / 2) Icons.Default.Check else Icons.Default.Close,
                                null, tint = Color.White, modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Eye Vision Complete", color = theme.darkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("$correctCount / ${tests.size} correct", color = theme.subText, fontSize = 16.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                val test = tests[currentIdx]
                val imageUrl = if (test.imageUrl.startsWith("http")) test.imageUrl else "https://my-project-five-sepia.vercel.app${test.imageUrl}"

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress
                    Text(
                        "Test ${currentIdx + 1} of ${tests.size}  •  Score: $correctCount",
                        color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))

                    // Image
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        shadowElevation = 3.dp
                    ) {
                        coil.compose.AsyncImage(
                            model = imageUrl,
                            contentDescription = "Eye vision test image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("What do you see?", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    // Number options — 0-9, 10, 21, 79, 99, 100
                    val options = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "21", "79", "99", "100")
                    val rows = options.chunked(5)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { num ->
                                val isSelected = selectedAnswer == num
                                val isCorrect = feedback != null && num == test.title
                                val isWrong = isSelected && feedback != null && num != test.title
                                Surface(
                                    color = when {
                                        isCorrect -> Color(0xFF22C55E)
                                        isWrong -> Color(0xFFEF4444)
                                        isSelected -> theme.primary
                                        else -> theme.cardBg
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        when {
                                            isCorrect -> Color(0xFF22C55E)
                                            isWrong -> Color(0xFFEF4444)
                                            isSelected -> theme.primary
                                            else -> theme.divider
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.2f)
                                        .clickable {
                                            if (feedback == null) {
                                                sound.click()
                                                selectedAnswer = num
                                                scope.launch {
                                                    try {
                                                        val resp = AppState.api.checkEyeVisionAnswer(test.id, mapOf("answer" to num))
                                                        if (resp.correct) {
                                                            sound.success()
                                                            correctCount++
                                                            feedback = "Correct!"
                                                        } else {
                                                            sound.error()
                                                            feedback = "Wrong — answer was ${resp.correctAnswer}"
                                                        }
                                                    } catch (e: Exception) {
                                                        // Fallback: check locally
                                                        if (num == test.title) {
                                                            sound.success()
                                                            correctCount++
                                                            feedback = "Correct!"
                                                        } else {
                                                            sound.error()
                                                            feedback = "Wrong — answer was ${test.title}"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            num,
                                            color = if (isCorrect || isWrong || isSelected) Color.White else theme.darkText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // Feedback
                    if (feedback != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            feedback!!,
                            color = if (feedback!!.startsWith("Correct")) Color(0xFF22C55E) else Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                sound.click()
                                if (currentIdx < tests.size - 1) {
                                    currentIdx++
                                    selectedAnswer = null
                                    feedback = null
                                } else {
                                    finished = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                            modifier = Modifier.fillMaxWidth(0.7f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (currentIdx < tests.size - 1) "Next" else "Finish",
                                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
