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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

    // ════════════════════════════════════════════════════════════════════════
    // PIXEL-PERFECT EXAM START SCREEN
    // Fixed design canvas: 1364 × 693 px (same as the block/question screen).
    // The entire canvas scales proportionally — children keep their pixel sizes.
    // ════════════════════════════════════════════════════════════════════════
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        val cw = maxWidth.value
        val ch = maxHeight.value
        // Same scaling method as the question/block screen — 1364×693 canvas
        val scale = minOf(cw / 1364f, ch / 693f).coerceAtLeast(0.15f)
        val sdp: (Float) -> Dp = { v -> (v * scale).dp }
        val ssp: (Float) -> TextUnit = { v -> (v * scale).sp }

        // ── OUTER FRAME: 2px solid #222, no radius, 18px margin from screen edge ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(sdp(18f))
                .border(width = sdp(2f), color = Color(0xFF222222))
        ) {
            // ── PAGE TITLE: centered at top, ~30px, weight 700, #080808 ──
            // Position: center X ≈ 682 (canvas center), top ≈ 48px
            Text(
                text = t.title,
                color = Color(0xFF080808),
                fontSize = ssp(29f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sdp(40f), bottom = sdp(20f))
                    .align(Alignment.TopCenter)
            )

            // ── STUDENT PROFILE COLUMN: centered, below title ──
            // Profile icon center Y ≈ 154 (relative to canvas), size 118×118
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sdp(90f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── PROFILE ICON ── clean white circle with black outline.
                // Uses Icons.Default.AccountCircle which is a Material Design
                // icon that renders as a circle with a person silhouette inside.
                // No black box, no overflow — clean and professional.
                Box(
                    modifier = Modifier
                        .size(sdp(140f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(width = sdp(6f), color = Color(0xFF111111)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color(0xFF111111),
                        modifier = Modifier.fillMaxSize().padding(sdp(4f))
                    )
                }

                Spacer(Modifier.height(sdp(16f)))

                // ── "Name of Student: dreamkorea" — centered, 26px, weight 700 ──
                Text(
                    text = "Name of Student: $studentName",
                    color = Color(0xFF111111),
                    fontSize = ssp(26f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(sdp(10f)))

                // ── "Student Email: dreamkoreaubt@gmail.com" — centered, 26px, weight 700 ──
                Text(
                    text = "Student Email: $studentEmail",
                    color = Color(0xFF111111),
                    fontSize = ssp(26f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // ── EXAM DESCRIPTION SECTION — LEFT ALIGNED at x≈99, y≈314 ──
            // Heading "Exam description" (26px, weight 700)
            // Body text (27px, weight 400, left-aligned, wraps to 2 lines)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = sdp(99f), top = sdp(314f))
                    .width(sdp(1290f))
            ) {
                // Heading
                Text(
                    text = "Exam description",
                    color = Color(0xFF111111),
                    fontSize = ssp(26f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(sdp(8f)))
                // Body — use the test description if set by admin,
                // otherwise show a default placeholder
                val descText = if (!t.description.isNullOrBlank()) {
                    t.description!!
                } else {
                    "This test will be Proceeded for ${t.durationMin}minutes without break. It has all ${t.items.size} questions and reading test is from 1 to 20, listening test is from 21 to 40. Listening test will be played two times."
                }
                Text(
                    text = descText,
                    color = Color(0xFF111111),
                    fontSize = ssp(27f),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    lineHeight = ssp(34f),
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // ── ACTION BUTTONS — centered, below description ──
            // Get Started: 259×69px, #1e73ea, radius 17-19px, 25px white bold text
            // Cancel:      259×70px, white, 2px solid #333, radius 16-18px, 24px bold text
            // Vertical gap between buttons: ~33px
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sdp(420f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Get Started button
                Button(
                    onClick = { sound.swoosh(); onStart() },
                    modifier = Modifier
                        .width(sdp(259f))
                        .height(sdp(69f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E73EA),
                        disabledContainerColor = Color(0xFF1E73EA).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(sdp(18f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    enabled = !alreadyCompleted
                ) {
                    Text(
                        text = if (alreadyCompleted) "Already Completed" else "Get Started",
                        color = Color.White,
                        fontSize = ssp(25f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(sdp(33f)))

                // Cancel button
                OutlinedButton(
                    onClick = { sound.click(); onBack() },
                    modifier = Modifier
                        .width(sdp(259f))
                        .height(sdp(70f)),
                    shape = RoundedCornerShape(sdp(17f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF111111)
                    ),
                    border = BorderStroke(sdp(2f), Color(0xFF333333)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF111111),
                        fontSize = ssp(24f),
                        fontWeight = FontWeight.Bold
                    )
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
