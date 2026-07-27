package app.dreamkorea.smartclass.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.TestItem
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Question Bank Screen — now lists QBank TESTS (created in admin under
 * "Question Bank" with testCategory="question_bank") instead of standalone
 * practice questions.
 *
 * This matches what the admin sees: every QBank test created in the admin
 * panel shows up here. Tapping a test opens ExamEntryScreen → the same
 * auto-landscape + block-grid flow as a regular exam, but QBank tests have
 * no timer pressure (the time limit is just informational).
 *
 * Why this changed:
 *  Previously the student QBank API read from Question.inQuestionBank=true,
 *  but admin never set that flag — admin created QBank TESTS instead. So
 *  the two sides never matched. Now both read from the same source: tests
 *  with testCategory="question_bank".
 */
@Composable
fun QuestionBankScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit, onStartExam: (String) -> Unit, onOpenPackages: () -> Unit = {}) {
    var tests by remember { mutableStateOf<List<TestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(retryCount) {
        loading = true
        error = ""
        try {
            // Always fetch fresh — admin may have just published a new QBank test
            AppState.invalidateCache(AppState.keyTests("question_bank"))
            val result = withTimeoutOrNull(20_000L) { AppState.getCachedTests("question_bank") }
            if (result != null) tests = result
            else error = "The request timed out. Check your internet and try again."
        } catch (e: retrofit2.HttpException) {
            error = when (e.code()) {
                401 -> "Your session has expired. Please log in again."
                else -> "Could not load question bank (HTTP ${e.code()})."
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
        if (loading) {
            Column(Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.primary,
                    trackColor = theme.primary.copy(alpha = 0.1f),
                )
                ScreenHeader(theme, sound, "Question Bank", "Loading practice tests...", onBack)
                SkeletonListScreen(theme, itemCount = 5)
            }
            return
        }

        if (error.isNotEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CloudOff, null, tint = theme.errorRed.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Couldn't load", color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(error, color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, shape = RoundedCornerShape(10.dp)) { Text("Go back") }
                    Button(
                        onClick = { sound.click(); retryCount++ },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    ScreenHeader(theme, sound, "Question Bank", if (tests.isEmpty()) "No practice tests yet." else "${tests.size} practice tests · tap one to start", onBack)
                }
            }

            if (tests.isEmpty()) {
                item { EmptyState(theme, "No question bank tests yet", "Your teacher will publish practice tests here soon.", Icons.Default.Quiz) }
            } else {
                itemsIndexed(tests) { i, t ->
                    AnimatedListItem(index = i, theme = theme) {
                        QBankTestCard(theme, sound, t, onClick = { onStartExam(t.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QBankTestCard(theme: AppTheme, sound: SoundManager, t: TestItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "qbankScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { sound.click(); pressed = true; onClick() },
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(6.dp).height(100.dp).background(
                    Brush.verticalGradient(listOf(theme.primary, theme.primary.copy(alpha = 0.4f)))
                )
            )
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.title,
                        color = theme.darkText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(color = theme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("PRACTICE", color = theme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                if (!t.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(t.description!!, color = theme.subText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoChipWithIcon(theme, Icons.Default.Timer, "${t.durationMin} min", theme.primary); Spacer(Modifier.width(6.dp))
                    if (t.questionCount > 0) {
                        InfoChipWithIcon(theme, Icons.Default.Quiz, "${t.questionCount} Q", Color(0xFF6A1B9A))
                        Spacer(Modifier.width(6.dp))
                    }
                    InfoChipWithIcon(theme, Icons.Default.School, "Pass ${t.passScore}%", Color(0xFF00695C))
                    Spacer(Modifier.weight(1f))
                    Text("Start", color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
