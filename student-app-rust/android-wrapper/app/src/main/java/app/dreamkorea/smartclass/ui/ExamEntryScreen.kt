package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.TestDetail
import app.dreamkorea.smartclass.api.TestItemDetail
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pre-exam info screen — shown before the user starts the actual test.
 *
 * Layout (landscape-friendly — uses Row layout so left side has student info,
 * right side has exam info + buttons):
 * - Outer black border around the entire screen
 * - LEFT: Title + circular avatar + student name + student email
 * - RIGHT: Exam description + exam stats (time/questions/pass) + Get Started + Cancel
 *
 * Forces landscape on entry (BEFORE the loading check so the user sees the
 * loading screen in landscape), restores orientation on leave.
 *
 * Special testId handling:
 *   • "qbank-combined"     → fetches /api/student/qbank-combined
 *   • "bundle-{bundleId}"  → fetches /api/student/bundles/{bundleId}/combined
 *   • anything else        → fetches /api/student/tests/{testId}
 */
@Composable
fun ExamEntryScreen(theme: AppTheme, sound: SoundManager, testId: String, onStart: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var test by remember { mutableStateOf<TestDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var alreadyCompleted by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    var showRotateHint by remember { mutableStateOf(true) }

    // Auto-hide the rotate hint after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        showRotateHint = false
    }

    // ── ORIENTATION ── FORCE LANDSCAPE. No onDispose PORTRAIT —
    // MainScreen handles portrait when screen changes to non-exam.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    LaunchedEffect(testId) {
        loading = true
        try {
            val result = withTimeoutOrNull(30_000L) {
                when {
                    // Combined QBank exam — fetches ALL published question_bank tests as one test
                    testId == "qbank-combined" -> {
                        try {
                            AppState.api.getQBankCombined().test
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) {
                                // Fallback: server doesn't have /qbank-combined yet —
                                // build the combined exam client-side from individual tests
                                buildQBankCombinedClientSide()
                            } else throw e
                        }
                    }
                    // Combined bundle exam — fetches ALL tests in a specific bundle (qbank/batch)
                    // as one combined exam
                    testId.startsWith("bundle-") -> {
                        val bundleId = testId.removePrefix("bundle-")
                        try {
                            AppState.api.getBundleCombined(bundleId).test
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) {
                                // Fallback: server doesn't have /bundles/[id]/combined yet —
                                // build the combined exam client-side from individual tests
                                buildBundleCombinedClientSide(bundleId)
                            } else throw e
                        }
                    }
                    // Normal test — fetch by ID
                    else -> AppState.api.getTestDetail(testId).test
                }
            }
            if (result != null) {
                if (result.items.isEmpty()) {
                    error = "This exam has no questions yet."
                } else {
                    test = result
                    // Check completion status — graded exams can only be taken once
                    // Practice exams (question_bank, bundles, demo) can be retaken
                    val isPractice = testId == "qbank-combined" || testId.startsWith("bundle-") ||
                        result.isExam == false
                    if (!isPractice) {
                        try {
                            val status = AppState.api.getCompletionStatus(testId)
                            alreadyCompleted = status.completed
                            isSubscribed = status.isSubscribed
                        } catch (_: Exception) {
                            // If the endpoint fails, just skip the check
                        }
                    }
                    // Also try to get subscription status
                    try {
                        val sub = AppState.api.getSubscriptionStatus()
                        isSubscribed = sub.isSubscribed
                    } catch (_: Exception) {}
                }
            } else {
                error = "Could not load the test. Check your connection."
            }
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                404 -> "This test was not found. The admin may need to redeploy the backend."
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

    // ── Rotate hint overlay — shows briefly when entering landscape ─────
    if (showRotateHint && !loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { showRotateHint = false },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Rotating phone icon
                Icon(
                    Icons.Default.ScreenRotation,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Rotate to Landscape",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This exam works best in landscape mode.\nTap to continue.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
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
    ) {
        // ── DreamKorea logo at top centre ────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                    contentDescription = "DreamKorea Logo",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "DreamKorea SmartClass",
                    color = Color(0xFF003478),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                Text(t.title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, Color.Black, CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Name of Student: $studentName", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Student Email: $studentEmail", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!t.description.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Text("Exam description", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(t.description!!, color = Color(0xFF333333), fontSize = 12.sp, lineHeight = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { sound.swoosh(); onStart() }, modifier = Modifier.fillMaxWidth(0.65f).height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(10.dp), enabled = !alreadyCompleted, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                    Text(if (alreadyCompleted) "Already Completed" else "Get Started", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { sound.click(); onBack() }, modifier = Modifier.fillMaxWidth(0.65f).height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black), border = BorderStroke(1.5.dp, Color.Black), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
        }
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

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT-SIDE FALLBACK — build a combined exam from individual test details
// when the server's /bundles/[id]/combined endpoint isn't deployed yet.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Fetches all published question_bank tests, then fetches each test's full
 * detail, and combines all items into ONE TestDetail. Used as a fallback
 * when /api/student/qbank-combined returns 404 (server not yet deployed).
 */
internal suspend fun buildQBankCombinedClientSide(): TestDetail {
    val tests = AppState.api.getTests(category = "question_bank").tests
    return combineTestsClientSide(
        testId = "qbank-combined",
        title = "Question Bank — All Questions",
        description = "${tests.size} sets combined",
        testIds = tests.map { it.id },
        durationMin = tests.sumOf { it.durationMin }.coerceAtLeast(60),
    )
}

/**
 * Fetches the bundle list, finds the bundle by ID, then fetches each test's
 * full detail, and combines all items into ONE TestDetail. Used as a
 * fallback when /api/student/bundles/[id]/combined returns 404.
 */
internal suspend fun buildBundleCombinedClientSide(bundleId: String): TestDetail {
    val bundles = AppState.api.getStudentBundles().bundles
    val bundle = bundles.find { it.id == bundleId }
        ?: throw retrofit2.HttpException(
            retrofit2.Response.error<Any>(404, okhttp3.ResponseBody.create(null, "Bundle not found"))
        )
    val testIds = bundle.items.map { it.test.id }
    return combineTestsClientSide(
        testId = "bundle-$bundleId",
        title = bundle.title,
        description = bundle.description ?: "${testIds.size} sets combined",
        testIds = testIds,
        durationMin = bundle.items.sumOf { it.test.durationMin }.coerceAtLeast(60),
    )
}

/**
 * Helper — fetches each test detail and combines all items into ONE
 * TestDetail. Each item keeps its real question ID so the submit endpoint
 * can grade it correctly.
 */
internal suspend fun combineTestsClientSide(
    testId: String,
    title: String,
    description: String,
    testIds: List<String>,
    durationMin: Int,
): TestDetail {
    val allItems = mutableListOf<TestItemDetail>()
    var textCount = 0
    var audioCount = 0
    var order = 1

    for (tid in testIds) {
        try {
            val detail = AppState.api.getTestDetail(tid).test
            for (item in detail.items) {
                allItems.add(
                    TestItemDetail(
                        id = "${testId}-item-${order}",
                        order = order,
                        points = item.points,
                        question = item.question,
                    )
                )
                if (item.question.blockType == "text") textCount++ else audioCount++
                order++
            }
        } catch (_: Exception) {
            // Skip tests that fail to load — don't break the whole combined exam
        }
    }

    return TestDetail(
        id = testId,
        title = title,
        description = description,
        durationMin = durationMin,
        isExam = false,
        passScore = 0,
        textBlockCount = textCount,
        audioBlockCount = audioCount,
        textBlockEnabled = true,
        audioBlockEnabled = true,
        showAllBlocks = false, // combined exams: only show created questions, no blanks
        items = allItems,
    )
}
