package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.QuestionBankQuestion
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Question Bank Screen — browse practice questions by category, answer one at a
 * time with instant feedback. No timer, no grading pressure — just practice.
 */
@Composable
fun QuestionBankScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<QuestionBankQuestion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentIdx by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            error = ""
            try {
                questions = AppState.getCachedQuestionBank()
            } catch (e: java.net.UnknownHostException) {
                error = "No internet connection."
            } catch (e: java.io.IOException) {
                error = "Could not connect to server."
            } catch (e: Exception) {
                error = "Could not load questions."
            }
            loading = false
        }
    }

    // Filter by category
    val filteredQuestions = remember(questions, selectedCategory) {
        if (selectedCategory == null) questions
        else questions.filter { it.category == selectedCategory }
    }
    val categories = remember(questions) {
        questions.map { it.category }.distinct()
    }

    if (loading) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(theme, sound, "Question Bank", "Loading practice questions...", onBack)
            SkeletonListScreen(theme, itemCount = 5)
        }
        return
    }

    if (error.isNotEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.WifiOff, null, tint = theme.errorRed, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(error, color = theme.darkText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) {
                Text("Go back")
            }
        }
        return
    }

    if (questions.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { ScreenHeader(theme, sound, "Question Bank", "Practice questions anytime.", onBack) }
            item { EmptyState(theme, "No questions yet", "Your teacher will add questions here soon.", Icons.Default.Quiz) }
        }
        return
    }

    // Single-question practice view
    val currentQuestion = filteredQuestions.getOrNull(currentIdx)
    if (currentQuestion == null) {
        // Category browse view
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ScreenHeader(theme, sound, "Question Bank", "${questions.size} practice questions. Tap a category to start.", onBack) }

            // All questions card
            item {
                AnimatedListItem(index = 0, theme = theme) {
                    CategoryCard(theme, sound, "All Questions", "${questions.size} questions", Icons.Default.Quiz, theme.primary) {
                        selectedCategory = null
                        currentIdx = 0
                        selectedAnswer = null
                        showFeedback = false
                    }
                }
            }

            // Category cards
            itemsIndexed(categories) { i, cat ->
                AnimatedListItem(index = i + 1, theme = theme) {
                    val count = questions.count { it.category == cat }
                    val color = when (cat) {
                        "Vocabulary" -> Color(0xFF003478)
                        "Grammar" -> Color(0xFFCD2E3A)
                        "Listening" -> Color(0xFF00C853)
                        "Reading" -> Color(0xFF7B1FA2)
                        else -> theme.primary
                    }
                    CategoryCard(theme, sound, cat, "$count questions", Icons.Default.Folder, color) {
                        selectedCategory = cat
                        currentIdx = 0
                        selectedAnswer = null
                        showFeedback = false
                    }
                }
            }
        }
        return
    }

    // ─── Question practice view ──────────────────────────────────────────────
    val q = currentQuestion
    val options = q.options ?: emptyList()
    // Safely extract the correct answer as a string (handles String, List, Number, etc.)
    val correctAnswerStr: String = when (val ca = q.correctAnswer) {
        null -> ""
        is String -> ca
        is List<*> -> ca.firstOrNull()?.toString() ?: ""
        is Number -> ca.toString()
        else -> ca.toString()
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header with category + progress
        Surface(color = theme.primary) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        // If in a question, go back to category list
                        selectedCategory = null
                        currentIdx = 0
                        selectedAnswer = null
                        showFeedback = false
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Question ${currentIdx + 1} of ${filteredQuestions.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            progress = { if (filteredQuestions.size > 0) ((currentIdx + 1f) / filteredQuestions.size).coerceIn(0f, 1f) else 0f },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.width(120.dp).padding(top = 4.dp)
                        )
                    }
                    // Category badge
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            q.category,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Question content — responsive layout for landscape (side-by-side) vs portrait
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                // Landscape: question on left, options on right
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: question stem + image
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { QuestionHeaderBadges(theme, q) }
                        item { QuestionStemCard(theme, q) }
                        if (!q.imageUrl.isNullOrBlank()) {
                            item {
                                Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                    AsyncImageLoader(url = q.imageUrl!!, modifier = Modifier.fillMaxWidth().height(160.dp))
                                }
                            }
                        }
                        if (showFeedback) {
                            item { FeedbackCard(theme, q, isCorrect) }
                        }
                    }
                    // Right: options
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (options.isNotEmpty()) {
                            itemsIndexed(options) { optIdx, opt ->
                                OptionCard(theme, sound, opt, optIdx, selectedAnswer, correctAnswerStr, showFeedback, isSelected = selectedAnswer == opt) {
                                    if (!showFeedback) {
                                        sound.click()
                                        selectedAnswer = opt
                                    }
                                }
                            }
                        } else {
                            item { EmptyState(theme, "No options", "This question type has no multiple-choice options.", Icons.Default.Info) }
                        }
                    }
                }
            } else {
                // Portrait: single column scroll
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
            // Difficulty badge
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val diffColor = when (q.difficulty) {
                        "EASY" -> SuccessGreen
                        "MEDIUM" -> Color(0xFFFFA000)
                        "HARD" -> theme.errorRed
                        else -> theme.subText
                    }
                    Surface(color = diffColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            q.difficulty,
                            color = diffColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(color = theme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            q.type.replace("_", " "),
                            color = theme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Question stem
            item {
                Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        q.stem,
                        color = theme.darkText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Image (if present)
            if (!q.imageUrl.isNullOrBlank()) {
                item {
                    Surface(
                        color = theme.cardBg,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImageLoader(url = q.imageUrl!!, modifier = Modifier.fillMaxWidth().height(180.dp))
                    }
                }
            }

            // Options
            if (options.isNotEmpty()) {
                itemsIndexed(options) { optIdx, opt ->
                    val isSelected = selectedAnswer == opt
                    val isCorrectOpt = showFeedback && opt == correctAnswerStr
                    val isWrongOpt = showFeedback && isSelected && opt != correctAnswerStr

                    val bgColor = when {
                        isCorrectOpt -> SuccessGreen.copy(alpha = 0.15f)
                        isWrongOpt -> theme.errorRed.copy(alpha = 0.15f)
                        isSelected -> theme.primary.copy(alpha = 0.1f)
                        else -> theme.cardBg
                    }
                    val borderColor = when {
                        isCorrectOpt -> SuccessGreen
                        isWrongOpt -> theme.errorRed
                        isSelected -> theme.primary
                        else -> theme.divider
                    }

                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (pressed) 0.98f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "optScale"
                    )

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable {
                                if (!showFeedback) {
                                    sound.click()
                                    pressed = true
                                    selectedAnswer = opt
                                }
                            },
                        shadowElevation = 1.dp
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Option letter
                            Surface(
                                color = if (isCorrectOpt) SuccessGreen else (if (isWrongOpt) theme.errorRed else theme.primary.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                val letter = if (optIdx in 0..3) "ABCD"[optIdx].toString() else "?"
                                Text(
                                    letter,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                opt,
                                color = theme.darkText,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCorrectOpt) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            } else if (isWrongOpt) {
                                Icon(Icons.Default.Cancel, null, tint = theme.errorRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Feedback / explanation
            if (showFeedback) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300))
                    ) {
                        Surface(
                            color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                        null,
                                        tint = if (isCorrect) SuccessGreen else theme.errorRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isCorrect) "Correct!" else "Not quite — correct answer highlighted above.",
                                        color = if (isCorrect) SuccessGreen else theme.errorRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!q.explanation.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Explanation: ${q.explanation}",
                                        color = theme.darkText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
                } // end portrait LazyColumn
            } // end else (portrait)
        } // end BoxWithConstraints

        // Bottom action bar
        Surface(color = theme.white, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!showFeedback) {
                    // Submit button
                    Button(
                        onClick = {
                            if (selectedAnswer != null) {
                                sound.click()
                                isCorrect = selectedAnswer == correctAnswerStr
                                showFeedback = true
                                if (isCorrect) sound.success() else sound.error()
                            }
                        },
                        enabled = selectedAnswer != null,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Check answer", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Next question button
                    Button(
                        onClick = {
                            sound.swoosh()
                            if (currentIdx < filteredQuestions.size - 1) {
                                currentIdx++
                                selectedAnswer = null
                                showFeedback = false
                            } else {
                                // Finished — go back to category list
                                selectedCategory = null
                                currentIdx = 0
                                selectedAnswer = null
                                showFeedback = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            if (currentIdx < filteredQuestions.size - 1) "Next question →" else "Done ✓",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    theme: AppTheme,
    sound: SoundManager,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "catScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { sound.click(); pressed = true; onClick() },
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = theme.subText, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = theme.subText, modifier = Modifier.size(24.dp))
        }
    }
}

// ─── Helper composables for question practice (shared between landscape & portrait) ───

@Composable
private fun QuestionHeaderBadges(theme: AppTheme, q: QuestionBankQuestion) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val diffColor = when (q.difficulty) {
            "EASY" -> SuccessGreen
            "MEDIUM" -> Color(0xFFFFA000)
            "HARD" -> theme.errorRed
            else -> theme.subText
        }
        Surface(color = diffColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
            Text(
                q.difficulty,
                color = diffColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(color = theme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
            Text(
                q.type.replace("_", " "),
                color = theme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun QuestionStemCard(theme: AppTheme, q: QuestionBankQuestion) {
    Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Text(
            q.stem,
            color = theme.darkText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun FeedbackCard(theme: AppTheme, q: QuestionBankQuestion, isCorrect: Boolean) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300))
    ) {
        Surface(
            color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (isCorrect) SuccessGreen else theme.errorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isCorrect) "Correct!" else "Not quite — correct answer highlighted above.",
                        color = if (isCorrect) SuccessGreen else theme.errorRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!q.explanation.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Explanation: ${q.explanation}",
                        color = theme.darkText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    theme: AppTheme,
    sound: SoundManager,
    opt: String,
    optIdx: Int,
    selectedAnswer: String?,
    correctAnswerStr: String,
    showFeedback: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCorrectOpt = showFeedback && opt == correctAnswerStr
    val isWrongOpt = showFeedback && isSelected && opt != correctAnswerStr

    val bgColor = when {
        isCorrectOpt -> SuccessGreen.copy(alpha = 0.15f)
        isWrongOpt -> theme.errorRed.copy(alpha = 0.15f)
        isSelected -> theme.primary.copy(alpha = 0.1f)
        else -> theme.cardBg
    }
    val borderColor = when {
        isCorrectOpt -> SuccessGreen
        isWrongOpt -> theme.errorRed
        isSelected -> theme.primary
        else -> theme.divider
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "optScale"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (isCorrectOpt) SuccessGreen else (if (isWrongOpt) theme.errorRed else theme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                val letter = if (optIdx in 0..3) "ABCD"[optIdx].toString() else "?"
                Text(
                    letter,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                opt,
                color = theme.darkText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isCorrectOpt) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            } else if (isWrongOpt) {
                Icon(Icons.Default.Cancel, null, tint = theme.errorRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
