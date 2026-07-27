package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Layout (matches reference app):
 * - Outer black border around the entire screen
 * - Title at top centre ("Exam Title")
 * - Circular profile/avatar icon below title
 * - Student name centred below icon
 * - Student email centred below name
 * - Exam description on the left-middle (with proper padding so text doesn't cut off)
 * - "Get Started" blue button (centre-lower)
 * - "Cancel" white outlined button below Get Started
 *
 * Forces landscape on entry, restores on leave.
 */
@Composable
fun ExamEntryScreen(theme: AppTheme, sound: SoundManager, testId: String, onStart: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var test by remember { mutableStateOf<TestDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(testId) {
        loading = true
        try {
            val result = withTimeoutOrNull(20_000L) {
                when {
                    // Combined QBank exam — fetches ALL published question_bank tests as one test
                    testId == "qbank-combined" -> AppState.api.getQBankCombined().test
                    // Combined bundle exam — fetches ALL tests in a specific bundle (qbank/batch)
                    // as one combined exam
                    testId.startsWith("bundle-") -> {
                        val bundleId = testId.removePrefix("bundle-")
                        AppState.api.getBundleCombined(bundleId).test
                    }
                    // Normal test — fetch by ID
                    else -> AppState.api.getTestDetail(testId).test
                }
            }
            if (result != null) test = result
            else error = "Could not load the test. Check your connection."
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

    // Force landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (loading) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF003478))
            Spacer(Modifier.height(12.dp))
            Text("Loading exam…", color = Color.Gray, fontSize = 13.sp)
        }
        return
    }

    if (error.isNotEmpty() || test == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CloudOff, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(error.ifEmpty { "Could not load the test." }, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { sound.click(); onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003478))) {
                Text("Go back")
            }
        }
        return
    }

    val t = test!!
    val studentName = AppState.getUserName()
    val studentEmail = AppState.getUserEmail()

    // ── OUTER BORDER: thin black rectangle around the entire screen ──────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .border(2.dp, Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // ── TITLE at top centre ──────────────────────────────────────────
        Text(
            t.title,
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        Spacer(Modifier.height(12.dp))

        // ── PROFILE ICON (circular avatar) ───────────────────────────────
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF003478)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(6.dp))

        // ── STUDENT NAME centred below icon ──────────────────────────────
        Text(
            studentName,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // ── STUDENT EMAIL centred below name ─────────────────────────────
        Text(
            studentEmail,
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // ── EXAM DESCRIPTION (left-middle, with proper padding) ──────────
        if (!t.description.isNullOrBlank()) {
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    t.description!!,
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── EXAM STATS (time, questions, pass mark) ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip("${t.durationMin} min", "Time", Modifier.weight(1f))
            StatChip("${t.items.size}", "Questions", Modifier.weight(1f))
            StatChip("${t.passScore}%", "Pass", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ── GET STARTED button (blue, centre-lower) ──────────────────────
        Button(
            onClick = { sound.swoosh(); onStart() },
            modifier = Modifier.fillMaxWidth(0.5f).height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003478)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Get Started", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        // ── CANCEL button (white outlined, below Get Started) ────────────
        OutlinedButton(
            onClick = { sound.click(); onBack() },
            modifier = Modifier.fillMaxWidth(0.5f).height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Text("Cancel", fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color(0xFF003478), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 9.sp)
        }
    }
}
