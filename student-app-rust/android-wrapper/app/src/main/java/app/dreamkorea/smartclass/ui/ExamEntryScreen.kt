package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.TestDetail
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pre-exam info screen — shown before the user starts the actual test.
 *
 * Flow: tap a test → ExamEntryScreen (info + Start) → ExamScreen (questions)
 *
 * Shows: title, time, pass mark, question count, block breakdown.
 *
 * Forces landscape on entry so the actual exam runs in the wider horizontal
 * layout — even if the student never enabled "Horizontal Exam Layout" in
 * Settings. Orientation is restored to unspecified when the user leaves.
 */
@Composable
fun ExamEntryScreen(theme: AppTheme, sound: SoundManager, testId: String, onStart: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var test by remember { mutableStateOf<TestDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Fetch the test detail (without correct answers — server only sends
    // options). We need the items count + duration + pass score for the
    // briefing card.
    LaunchedEffect(testId) {
        loading = true
        try {
            val result = withTimeoutOrNull(15_000L) {
                AppState.api.getTestDetail(testId).test
            }
            if (result != null) {
                test = result
            } else {
                error = "Could not load the test. Check your connection."
            }
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                404 -> "This test was not found."
                else -> "Could not load (HTTP ${e.code()})."
            }
        } catch (e: java.io.IOException) {
            error = "No internet connection."
        } catch (e: Exception) {
            error = "Could not load the test."
        } finally {
            loading = false
        }
    }

    // ─── Force landscape on enter, restore on leave ──────────────────────────
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            // Restore portrait / unspecified when leaving the entry screen
            // (either via Start — ExamScreen re-locks — or via Back)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (loading) {
        Column(
            modifier = Modifier.fillMaxSize().background(theme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = theme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Loading exam…", color = theme.subText, fontSize = 13.sp)
        }
        return
    }

    if (error.isNotEmpty() || test == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(theme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CloudOff, null, tint = theme.errorRed, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(error.ifEmpty { "Could not load the test." }, color = theme.darkText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { sound.click(); onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) { Text("Go back") }
        }
        return
    }

    val t = test!!

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                sound.click()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = theme.darkText)
            }
            Spacer(Modifier.width(8.dp))
            Text("Exam Briefing", color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Content card
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = theme.cardBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // DreamKorea logo / brand mark
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF003478), Color(0xFF0064D2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DK", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("DreamKorea SmartClass", color = theme.subText, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        t.title,
                        color = theme.darkText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (!t.description.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(t.description!!, color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))

                    // Stat grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox(theme, Icons.Default.Timer, "${t.durationMin} min", "Time Limit", Modifier.weight(1f))
                        StatBox(theme, Icons.Default.Quiz, "${t.items.size}", "Questions", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox(theme, Icons.Default.CheckCircle, "${t.passScore}%", "Pass Mark", Modifier.weight(1f))
                        val textN = if (t.textBlockEnabled) t.textBlockCount else 0
                        val audioN = if (t.audioBlockEnabled) t.audioBlockCount else 0
                        val blocksLabel = when {
                            textN > 0 && audioN > 0 -> "$textN text + $audioN audio"
                            textN > 0 -> "$textN text"
                            audioN > 0 -> "$audioN audio"
                            else -> "${t.items.size} Q"
                        }
                        StatBox(theme, Icons.Default.Layers, blocksLabel, "Structure", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            sound.swoosh()
                            onStart()
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Exam", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ScreenRotation, null, tint = theme.subText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Your phone will rotate to landscape for the best experience",
                            color = theme.subText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(theme: AppTheme, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = theme.background,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = theme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = theme.subText, fontSize = 10.sp)
        }
    }
}

