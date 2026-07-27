package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.BundleSummary
import app.dreamkorea.smartclass.api.CompletedTestInfo
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Question Bank Screen — shows admin-created QBank bundles.
 *
 * Each QBank bundle is a curated set of tests (the admin can pick tests from
 * ANY category: batch, exam, demo, chapter, etc.). The system extracts all
 * questions from those tests and combines them into ONE big exam the student
 * can solve.
 *
 * UI per bundle card (horizontal layout — image on the left, content on the
 * right — so images get more screen space):
 *   • Cover image (left, 140dp wide × full card height)
 *   • Title + description (right)
 *   • "View All Questions" button → opens the combined exam
 *   • "Completed" badge if the student has already submitted this combined exam
 */
@Composable
fun QuestionBankScreen(
    theme: AppTheme,
    sound: SoundManager,
    onBack: () -> Unit,
    onStartExam: (String) -> Unit,
    onOpenPackages: () -> Unit = {}
) {
    var bundles by remember { mutableStateOf<List<BundleSummary>>(emptyList()) }
    var completedMap by remember { mutableStateOf<Map<String, CompletedTestInfo>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(retryCount) {
        loading = true
        error = ""
        try {
            // Fetch QBank bundles (admin-created) — these contain tests the
            // admin chose from any category. Each bundle becomes ONE combined
            // exam on the student side.
            val bundlesResult = withTimeoutOrNull(20_000L) {
                AppState.cachedFresh("bundles_qbank") {
                    AppState.api.getStudentBundles("qbank").bundles
                }
            }

            // Fetch completed tests to mark bundles as "Completed"
            val completedResult = try {
                withTimeoutOrNull(10_000L) { AppState.api.getCompletedTests() }
            } catch (_: Exception) { null }

            if (bundlesResult != null) {
                bundles = bundlesResult
                completedMap = completedResult?.completed ?: emptyMap()
            } else {
                error = "Could not load question bank. Check your connection."
            }
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                else -> "Could not load (HTTP ${e.code()})."
            }
        } catch (e: java.io.IOException) {
            error = "No internet connection."
        } catch (e: Exception) {
            error = "Could not load question bank."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Question Bank", "Pick a set and start solving", onBack)

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
                Button(
                    onClick = { sound.click(); retryCount++ },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) { Text("Retry") }
            }
            return
        }

        if (bundles.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No question banks yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Your teacher will publish question banks here soon.", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(bundles) { bundle ->
                QBankBundleCard(
                    theme = theme,
                    sound = sound,
                    bundle = bundle,
                    isCompleted = completedMap.containsKey("bundle-${bundle.id}"),
                    completedInfo = completedMap["bundle-${bundle.id}"],
                    onStart = {
                        sound.swoosh()
                        onStartExam("bundle-${bundle.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun QBankBundleCard(
    theme: AppTheme,
    sound: SoundManager,
    bundle: BundleSummary,
    isCompleted: Boolean,
    completedInfo: CompletedTestInfo?,
    onStart: () -> Unit
) {
    val totalSets = bundle.items.size

    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        // Horizontal layout — image on the LEFT (large), content on the RIGHT
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // ── LEFT: Cover image — wider so images are clearly visible ─────
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(theme.background),
                contentAlignment = Alignment.Center
            ) {
                if (!bundle.coverUrl.isNullOrBlank()) {
                    val absUrl = if (bundle.coverUrl!!.startsWith("http")) bundle.coverUrl else "https://my-project-five-sepia.vercel.app${bundle.coverUrl}"
                    coil.compose.AsyncImage(
                        model = absUrl,
                        contentDescription = bundle.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Icon placeholder when no image
                    Icon(
                        Icons.Default.Quiz,
                        null,
                        tint = theme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                // Completed badge overlay (top-left of image)
                if (isCompleted) {
                    Surface(
                        color = Color(0xFF22C55E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Done", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── RIGHT: Content ─────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                // Top row: kind badge + score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = theme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "Question Bank",
                            color = theme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isCompleted && completedInfo?.score != null && completedInfo.maxScore != null) {
                        Text(
                            "Score: ${completedInfo.score}/${completedInfo.maxScore}",
                            color = Color(0xFF22C55E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    bundle.title,
                    color = theme.darkText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Description
                if (!bundle.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bundle.description!!,
                        color = theme.subText,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Stats row: set count + price
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF6A1B9A).copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$totalSets sets combined", color = Color(0xFF6A1B9A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (bundle.price > 0) {
                        Text("₩${bundle.price}", color = theme.darkText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Free", color = theme.subText, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // "View All Questions" button — opens the combined exam
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isCompleted) "Solve Again" else "View All Questions",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
