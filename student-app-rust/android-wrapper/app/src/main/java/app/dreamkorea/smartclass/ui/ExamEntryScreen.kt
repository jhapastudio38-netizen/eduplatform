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

    // ── ORIENTATION ── FORCE LANDSCAPE.
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
    val studentName = AppState.getUserName() ?: "Student"
    val studentEmail = AppState.getUserEmail() ?: ""

    // ── LANDSCAPE EXAM OVERVIEW — vertical stacked layout ──
    // Light grey background, large white panel (97% width, 94% height) centered.
    // Top→bottom: DreamKorea title, profile avatar, user info, exam description, buttons.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // White exam panel — 97% width, 94% height, thin dark border
        Box(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .background(Color.White)
                .border(1.5.dp, Color(0xFF222222))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── 1. DreamKorea title (centered, bold) ──
                Text(
                    t.title,
                    color = Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ── 2. Profile avatar (circular, dark) ──
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }

                Spacer(Modifier.height(10.dp))

                // ── 3. User info (name + email) ──
                Text(
                    "Name of Student: $studentName",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Student Email: $studentEmail",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // ── 4. Exam description ──
                Text(
                    "Exam description",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    t.description ?: "No description available.",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(Modifier.height(18.dp))

                // ── 5. Buttons (horizontal row) ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button (secondary — white with dark border)
                    OutlinedButton(
                        onClick = { sound.click(); onBack() },
                        modifier = Modifier.width(140.dp).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF333333))
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Get Started button (primary — blue)
                    Button(
                        onClick = { sound.swoosh(); onStart() },
                        modifier = Modifier.width(160.dp).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !alreadyCompleted
                    ) {
                        Text(
                            if (alreadyCompleted) "Already Completed" else "Get Started",
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
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
