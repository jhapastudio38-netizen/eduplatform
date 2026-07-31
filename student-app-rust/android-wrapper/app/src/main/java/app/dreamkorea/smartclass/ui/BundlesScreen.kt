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
 * BundlesScreen — lists all published packages of a given kind (qbank / batch).
 *
 * The student sees each package as a card with:
 *   • Cover image on the LEFT (140dp wide × full card height — so images are
 *     clearly visible and not tiny)
 *   • Title + description on the right
 *   • Set count
 *   • "View All Questions" button → opens the combined exam (extracts all
 *     questions from all sets in the package and combines them into ONE exam)
 *   • "Completed" badge if already submitted
 *
 * The combined exam ID format is `bundle-{bundleId}` — ExamEntryScreen and
 * ExamScreen both detect this prefix and fetch from
 * /api/student/bundles/{bundleId}/combined.
 */
@Composable
fun BundlesScreen(
    theme: AppTheme,
    sound: SoundManager,
    onBack: () -> Unit,
    onOpenBundle: (String, String) -> Unit,
    onOpenTest: (String) -> Unit,
    initialKind: String? = null
) {
    var bundles by remember { mutableStateOf<List<BundleSummary>>(emptyList()) }
    var completedMap by remember { mutableStateOf<Map<String, CompletedTestInfo>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    // Title based on kind filter
    val screenTitle = when (initialKind) {
        "qbank" -> "Question Bank"
        "batch" -> "Batch Packages"
        else -> "Packages"
    }
    val screenSubtitle = when (initialKind) {
        "batch" -> "All batch exam questions combined"
        "qbank" -> "All question sets combined"
        else -> "Browse all packages"
    }

    LaunchedEffect(retryCount) {
        loading = true
        error = ""
        try {
            val result = withTimeoutOrNull(20_000L) {
                AppState.cachedFresh("bundles_${initialKind ?: "all"}") {
                    AppState.api.getStudentBundles(initialKind).bundles
                }
            }
            val completedResult = try {
                withTimeoutOrNull(10_000L) { AppState.api.getCompletedTests() }
            } catch (_: Exception) { null }

            if (result != null) {
                bundles = result
                completedMap = completedResult?.completed ?: emptyMap()
            } else {
                error = "The request timed out. Check your internet and try again."
            }
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                else -> "Could not load (HTTP ${e.code()})."
            }
        } catch (e: java.io.IOException) {
            error = "No internet connection."
        } catch (e: Exception) {
            error = "Could not load."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, screenTitle, screenSubtitle, onBack)

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
                Icon(Icons.Default.Inventory2, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No packages yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Your teacher will publish packages here soon.", color = theme.subText, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(bundles) { bundle ->
                BundleCardCombined(
                    theme = theme,
                    sound = sound,
                    bundle = bundle,
                    isCompleted = completedMap.containsKey("bundle-${bundle.id}"),
                    completedInfo = completedMap["bundle-${bundle.id}"],
                    onStart = {
                        sound.swoosh()
                        // Open the combined exam — uses special "bundle-{id}" test ID
                        onOpenTest("bundle-${bundle.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun BundleCardCombined(
    theme: AppTheme,
    sound: SoundManager,
    bundle: BundleSummary,
    isCompleted: Boolean,
    completedInfo: CompletedTestInfo?,
    onStart: () -> Unit
) {
    val totalSets = bundle.items.size
    val kindLabel = when (bundle.kind) {
        "qbank" -> "Question Bank"
        "batch" -> "Batch"
        "exam" -> "Exam"
        "chapter" -> "Chapter"
        else -> bundle.kind
    }
    val kindColor = when (bundle.kind) {
        "batch" -> Color(0xFFEF6C00)
        "qbank" -> theme.primary
        else -> Color(0xFF6A1B9A)
    }

    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        // Horizontal layout — image LEFT (large), content RIGHT
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
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        tint = kindColor,
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
                    Surface(color = kindColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            kindLabel,
                            color = kindColor,
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
                    } else if (bundle.price > 0) {
                        Text("₩${bundle.price}", color = theme.darkText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Free", color = theme.subText, fontSize = 10.sp)
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

                // Stats row: set count
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

// ─── Bundle Detail — fallback screen (kept for compatibility, but no longer
// used since the BundlesScreen now opens combined exams directly) ──────────
@Composable
fun BundleDetailScreen(theme: AppTheme, sound: SoundManager, bundleId: String, bundleTitle: String, onBack: () -> Unit, onOpenTest: (String) -> Unit) {
    // If somehow routed here, just open the combined exam directly.
    LaunchedEffect(bundleId) {
        sound.swoosh()
        onOpenTest("bundle-$bundleId")
    }
    Column(modifier = Modifier.fillMaxSize().background(theme.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = theme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Loading combined exam…", color = theme.subText, fontSize = 13.sp)
    }
}
