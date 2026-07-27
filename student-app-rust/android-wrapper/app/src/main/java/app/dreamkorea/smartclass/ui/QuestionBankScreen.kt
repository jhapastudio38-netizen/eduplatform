package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.TestItem
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Question Bank Screen — combines ALL questions from ALL published QBank tests
 * into ONE big exam. User taps "Start" and solves everything at once.
 *
 * This is NOT a list of tests. It's a single combined exam.
 */
@Composable
fun QuestionBankScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit, onStartExam: (String) -> Unit, onOpenPackages: () -> Unit = {}) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var questionCount by remember { mutableStateOf(0) }
    var setCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        try {
            // Fetch all QBank tests to show the count
            AppState.invalidateCache(AppState.keyTests("question_bank"))
            val tests = withTimeoutOrNull(20_000L) { AppState.getCachedTests("question_bank") }
            if (tests != null) {
                questionCount = tests.sumOf { it.questionCount }
                setCount = tests.size
            }
        } catch (e: Exception) {
            error = "Could not load question bank."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Question Bank", "All questions combined", onBack)

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
                Icon(Icons.Default.CloudOff, null, tint = theme.errorRed, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(error, color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) { Text("Go back") }
            }
            return
        }

        if (questionCount == 0) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No questions yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Your teacher will add questions here soon.", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            return
        }

        // Show combined QBank info + Start button
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big icon
            Surface(
                color = theme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Quiz, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Question Bank", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("$questionCount questions from $setCount sets", color = theme.subText, fontSize = 14.sp)
            Text("All combined into one exam", color = theme.subText, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$questionCount", color = theme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Questions", color = theme.subText, fontSize = 11.sp)
                    }
                }
                Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$setCount", color = theme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Sets", color = theme.subText, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // Start button — opens the combined exam
            Button(
                onClick = {
                    sound.swoosh()
                    // Use special ID "qbank-combined" — ExamEntryScreen will fetch from /api/student/qbank-combined
                    onStartExam("qbank-combined")
                },
                modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Solving", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
