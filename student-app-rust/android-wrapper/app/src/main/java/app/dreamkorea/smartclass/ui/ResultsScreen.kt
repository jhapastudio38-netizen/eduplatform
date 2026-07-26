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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

data class ResultItem(
    val title: String,
    val score: Int,
    val maxScore: Int,
    val submittedAt: String
)

@Composable
fun ResultsScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<ResultItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val resp = AppState.api.getStats()
                // Stats don't have individual results, so we show summary stats
                val stats = resp.stats
                // Build summary from stats
                results = listOfNotNull(
                    if (stats.totalExamsTaken > 0) ResultItem("Total Exams Taken", stats.totalExamsTaken, stats.totalExamsTaken, "") else null,
                    if (stats.totalQuestionsAnswered > 0) ResultItem("Questions Answered", stats.totalCorrectAnswers, stats.totalQuestionsAnswered, "") else null,
                    if (stats.totalExamsTaken > 0) ResultItem("Average Score", (stats.averageScore).toInt(), 100, "") else null,
                )
            } catch (_: Exception) {}
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Results", "Your exam performance", onBack)

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
            return
        }

        if (results.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.BarChart, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No results yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Take an exam to see your results here", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results) { r ->
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(r.title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${r.score}", color = theme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(" / ${r.maxScore}", color = theme.subText, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            if (r.maxScore > 0) {
                                val pct = (r.score * 100 / r.maxScore)
                                val passed = pct >= 40
                                Surface(
                                    color = if (passed) Color(0xFF34C759).copy(alpha = 0.15f) else theme.errorRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "$pct%",
                                        color = if (passed) Color(0xFF34C759) else theme.errorRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
