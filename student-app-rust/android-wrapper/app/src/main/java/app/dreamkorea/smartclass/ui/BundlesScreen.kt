package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.BundleSummary
import app.dreamkorea.smartclass.api.BundlesResponse
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * BundlesScreen — lists all published packages (QBank / Batch / Exam / Chapter).
 *
 * The student can tap a bundle to see the tests inside it (BundleDetailScreen),
 * or tap a test directly to start it (which routes through ExamEntryScreen so
 * they get the pre-exam briefing + auto-landscape).
 *
 * Bundles group multiple tests into one sellable / assignable package. For
 * example, "TOPIK 1 Complete Question Bank" might contain 10 individual
 * question-bank sets — the student buys / opens the bundle and gets access
 * to all 10 from one place.
 *
 * Performance notes:
 *  • Coil caches images to disk so repeated opens are instant.
 *  • The list is a LazyColumn so only visible rows are rendered — important
 *    for low-end phones with limited memory.
 *  • We avoid nested scroll containers (which Compose handles poorly on
 *    old devices).
 */
@Composable
fun BundlesScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit, onOpenBundle: (String, String) -> Unit, onOpenTest: (String) -> Unit) {
    var bundles by remember { mutableStateOf<List<BundleSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(retryCount) {
        loading = true
        error = ""
        try {
            val result = withTimeoutOrNull(20_000L) { AppState.api.getStudentBundles().bundles }
            if (result != null) bundles = result
            else error = "The request timed out. Check your internet and try again."
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                else -> "Could not load packages (HTTP ${e.code()})."
            }
        } catch (e: java.io.IOException) {
            error = "No internet connection."
        } catch (e: Exception) {
            error = "Could not load packages."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header
        ScreenHeader(theme, sound, "Packages", "Question banks, batches, exam packs", onBack)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bundles) { bundle ->
                BundleCard(theme, sound, bundle, onClick = { onOpenBundle(bundle.id, bundle.title) })
            }
        }
    }
}

@Composable
private fun BundleCard(theme: AppTheme, sound: SoundManager, bundle: BundleSummary, onClick: () -> Unit) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { sound.click(); onClick() },
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Cover image (or icon placeholder)
            Box(
                modifier = Modifier.size(96.dp).background(theme.background),
                contentAlignment = Alignment.Center
            ) {
                if (!bundle.coverUrl.isNullOrBlank()) {
                    val absUrl = if (bundle.coverUrl!!.startsWith("http")) bundle.coverUrl else "https://my-project-five-sepia.vercel.app${bundle.coverUrl}"
                    coil.compose.AsyncImage(
                        model = absUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Inventory2, null, tint = theme.primary, modifier = Modifier.size(36.dp))
                }
            }
            // Content
            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Text(
                    bundle.title,
                    color = theme.darkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!bundle.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bundle.description!!,
                        color = theme.subText,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kind badge
                    val kindLabel = when (bundle.kind) {
                        "qbank" -> "QBank"
                        "batch" -> "Batch"
                        "exam" -> "Exam"
                        "chapter" -> "Chapter"
                        else -> bundle.kind
                    }
                    Surface(color = theme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(kindLabel, color = theme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    // Test count
                    Surface(color = Color(0xFF6A1B9A).copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${bundle.items.size} sets", color = Color(0xFF6A1B9A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (bundle.price > 0) {
                        Text("₩${bundle.price}", color = theme.darkText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Free", color = theme.subText, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─── Bundle Detail — shows the tests inside a bundle ──────────────────────────
@Composable
fun BundleDetailScreen(theme: AppTheme, sound: SoundManager, bundleId: String, bundleTitle: String, onBack: () -> Unit, onOpenTest: (String) -> Unit) {
    var bundle by remember { mutableStateOf<BundleSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(bundleId) {
        loading = true
        try {
            // We fetch the full bundle list and pick the one we want — saves
            // building a separate /api/student/bundles/[id] endpoint and keeps
            // the wire format identical to the list view.
            val all = withTimeoutOrNull(20_000L) { AppState.api.getStudentBundles().bundles }
            if (all != null) {
                bundle = all.find { it.id == bundleId }
                if (bundle == null) error = "This package was not found."
            } else {
                error = "Could not load. Check your internet."
            }
        } catch (e: java.io.IOException) {
            error = "No internet connection."
        } catch (e: Exception) {
            error = "Could not load the package."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, bundleTitle, "Tap a set to start", onBack)

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
            return
        }

        if (error.isNotEmpty() || bundle == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CloudOff, null, tint = theme.errorRed, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(error.ifEmpty { "Could not load the package." }, color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) { Text("Go back") }
            }
            return
        }

        val b = bundle!!
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bundle description header
            if (!b.description.isNullOrBlank()) {
                item {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("About this package", color = theme.subText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(b.description!!, color = theme.darkText, fontSize = 13.sp)
                        }
                    }
                }
            }
            // Tests inside the bundle
            items(b.items) { item ->
                val t = item.test
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { sound.click(); onOpenTest(t.id) },
                    shadowElevation = 2.dp
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Number badge
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(theme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${item.sortOrder}", color = theme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (t.durationMin > 0) {
                                    Text("${t.durationMin} min", color = theme.subText, fontSize = 10.sp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Pass ${t.passScore}%", color = theme.subText, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = theme.subText, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
