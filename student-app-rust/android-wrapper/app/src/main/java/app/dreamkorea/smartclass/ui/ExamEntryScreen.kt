package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // ── ORIENTATION ── FORCE LANDSCAPE for the exam overview. The phone
    // auto-rotates to landscape when the overview opens, and restores on exit.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
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
                    // Check completion status (for non-practice exams only)
                    if (testId != "qbank-combined" && !testId.startsWith("bundle-")) {
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
            modifier = Modifier.weight(1f).padding(16.dp)
        ) {
            // Landscape-friendly layout: Row with LEFT (student) + RIGHT (exam)
            Row(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── LEFT: Student info ────────────────────────────────────
                Column(
                    modifier = Modifier.weight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title at top
                Text(
                    t.title,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(Modifier.height(16.dp))

                // Profile icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF003478)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(8.dp))

                // Student name
                Text(
                    studentName,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                // Student email
                Text(
                    studentEmail,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ── Vertical divider ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.7f)
                    .background(Color(0xFFCCCCCC))
            )

            // ── RIGHT: Exam info + buttons ────────────────────────────────
            Column(
                modifier = Modifier.weight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Exam description
                if (!t.description.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        modifier = Modifier.fillMaxWidth()
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

                // Exam stats (time, questions, pass mark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("${t.durationMin} min", "Time", Modifier.weight(1f))
                    StatChip("${t.items.size}", "Questions", Modifier.weight(1f))
                    StatChip("${t.items.size * 2.5}", "Marks", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // ── Block system overview ─────────────────────────────────
                // Shows the text block count + audio block count so the student
                // knows what to expect (e.g. "20 Reading + 20 Listening")
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Exam Structure",
                            color = Color(0xFF003478),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Reading block
                            Surface(
                                color = Color(0xFF003478).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MenuBook, null, tint = Color(0xFF003478), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${t.textBlockCount}",
                                            color = Color(0xFF003478),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            "Reading",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                            // Listening block
                            Surface(
                                color = Color(0xFFEF6C00).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Headphones, null, tint = Color(0xFFEF6C00), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${t.audioBlockCount}",
                                            color = Color(0xFFEF6C00),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            "Listening",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Already completed warning (non-subscribers can only take once) ──
                if (alreadyCompleted && !isSubscribed) {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFC107)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Already Completed", color = Color(0xFF856404), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("You've already taken this exam. Subscribe to unlock unlimited retakes.", color = Color(0xFF856404), fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Subscriber badge
                if (isSubscribed) {
                    Surface(color = Color(0xFF8B5CF6), shape = RoundedCornerShape(8.dp)) {
                        Text("SUBSCRIBER — Unlimited Access", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Get Started button (or "Retake" if already completed)
                Button(
                    onClick = { sound.swoosh(); onStart() },
                    modifier = Modifier.fillMaxWidth(0.7f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003478)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !(alreadyCompleted && !isSubscribed)
                ) {
                    Text(
                        if (alreadyCompleted && !isSubscribed) "Already Completed" else if (alreadyCompleted) "Retake Exam" else "Get Started",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Cancel button
                OutlinedButton(
                    onClick = { sound.click(); onBack() },
                    modifier = Modifier.fillMaxWidth(0.7f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Cancel", fontSize = 13.sp)
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
        items = allItems,
    )
}
