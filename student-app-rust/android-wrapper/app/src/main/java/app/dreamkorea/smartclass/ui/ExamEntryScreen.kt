package app.dreamkorea.smartclass.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
 * Spec:
 *  - White background, thin dark border panel (96% width, 92% height)
 *  - Title centered at top (24sp bold black)
 *  - Circular profile icon (50dp, dark background, white person icon)
 *  - "Name of Student: {name}" and "Student Email: {email}" (12sp semibold)
 *  - "Exam description" heading (12sp bold)
 *  - Description text (12sp, max 5 lines, left-aligned)
 *  - "Get Started" button (blue #1A73E8, 130x34dp, white text)
 *  - "Cancel" button (white, dark border, 110x34dp)
 *  - Buttons side by side in a Row
 *  - Everything centered, fits on screen, no scroll
 *  - DreamKorea watermark behind (200dp, 5% alpha)
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

    // ── ORIENTATION ── FORCE LANDSCAPE for the exam entry screen so it
    // matches the exam itself (no jarring rotation when the user taps
    // "Get Started"). MainScreen restores portrait on leave.
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
                    testId == "qbank-combined" -> {
                        try { AppState.api.getQBankCombined().test }
                        catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) buildQBankCombinedClientSide() else throw e
                        }
                    }
                    testId.startsWith("bundle-") -> {
                        val bundleId = testId.removePrefix("bundle-")
                        try { AppState.api.getBundleCombined(bundleId).test }
                        catch (e: retrofit2.HttpException) {
                            if (e.code() == 404) buildBundleCombinedClientSide(bundleId) else throw e
                        }
                    }
                    else -> AppState.api.getTestDetail(testId).test
                }
            }
            if (result != null) {
                if (result.items.isEmpty()) error = "This exam has no questions yet."
                else test = result
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

    // ── Outer screen: white with DreamKorea watermark behind ──────────
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Watermark logo behind the panel — 200dp, 5% alpha
        Image(
            painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
            contentDescription = null,
            modifier = Modifier.size(200.dp).alpha(0.05f),
            contentScale = ContentScale.Fit
        )

        // ── Panel: 96% width, 92% height, thin dark border ────────────
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFF1F2937)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Title centered at top ──
                Text(
                    t.title,
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(20.dp))

                // ── Circular profile icon ── 50dp, dark bg, white person icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2937)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.height(10.dp))

                // ── Student name + email ── 12sp semibold
                Text(
                    "Name of Student: $studentName",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Student Email: $studentEmail",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // ── "Exam description" heading ── 12sp bold
                Text(
                    "Exam description",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(Modifier.height(6.dp))

                // ── Description text ── 12sp, max 5 lines, left-aligned
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        t.description?.takeIf { it.isNotBlank() } ?: "No description provided.",
                        color = Color.Black,
                        fontSize = 12.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Buttons row ── Get Started (130x34) + Cancel (110x34)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Get Started — blue #1A73E8, 130x34, white text
                    Button(
                        onClick = { sound.swoosh(); onStart() },
                        modifier = Modifier.width(130.dp).height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Get Started",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Cancel — white, dark border, 110x34
                    OutlinedButton(
                        onClick = { sound.click(); onBack() },
                        modifier = Modifier.width(110.dp).height(34.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1F2937)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
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
        showAllBlocks = false,
        items = allItems,
    )
}
