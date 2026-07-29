#!/usr/bin/env python3
"""Apply the worksheet redesign to ExamScreen.kt"""
import re

FILE = "/home/z/my-project/student-app-rust/android-wrapper/app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt"

with open(FILE, 'r') as f:
    content = f.read()

# --- 1. Replace the grid page block (showGrid section) ---
OLD_START = "            // ── QUESTION GRID PAGE ── clean, simple exam navigation."
OLD_END = "        return\n    }\n}\n"

# Find the start
start_idx = content.find(OLD_START)
if start_idx == -1:
    print("ERROR: Could not find grid page start")
    exit(1)

# Find the end (the closing of ExamScreen function)
end_idx = content.find(OLD_END, start_idx)
if end_idx == -1:
    print("ERROR: Could not find grid page end")
    exit(1)

end_idx += len(OLD_END)

NEW_BLOCK = '''            // ── QUESTION GRID PAGE ── worksheet-style exam navigation.
    if (showGrid) {
        val readingItems = t.items.filter { it.question.blockType != "audio" }
        val listeningItems = t.items.filter { it.question.blockType == "audio" }
        var showSubmitDialog by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        // Filter: null = all, true = answered only, false = unsolved only
        var filterMode by remember { mutableStateOf<Boolean?>(null) }

        // Count answered for live progress
        val readingAnswered = readingItems.count { answers.containsKey(it.question.id) }
        val listeningAnswered = listeningItems.count { answers.containsKey(it.question.id) }
        val totalAnswered = readingAnswered + listeningAnswered
        val totalQuestions = t.items.size
        val totalUnsolved = totalQuestions - totalAnswered
        val mm = timeLeft / 60; val ss = timeLeft % 60
        val timeStr = String.format("%02d:%02d", mm, ss)
        val isLowTime = timeLeft in 1..300
        val timerColor = if (isLowTime) Color(0xFFDC2626) else Color(0xFF1F2937)

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // ── HEADER ROW 1: logo | title | ID | user ───────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                    contentDescription = "DreamKorea",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    t.title.take(28),
                    color = Color(0xFF111111),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "ID",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    testId.take(10),
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    AppState.getUserName().take(12),
                    color = Color(0xFF111111),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Thin divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))

            // ── STATUS ROW 2: Nepal | ALL | SOLVED | UNSOLVED | timer ────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nepal", color = Color(0xFF4B5563), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally))
                // Filter tabs
                StatusTab("ALL", filterMode == null) { filterMode = null }
                StatusTab("SOLVED", filterMode == true) { filterMode = true }
                StatusTab("UNSOLVED", filterMode == false) { filterMode = false }
                // Timer
                Text(
                    timeStr,
                    color = timerColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
            // Thin divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))

            // ── MAIN: Reading LEFT | watermark CENTER | Listening RIGHT ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // Watermark "DREAMKOREA" faint, centered
                Text(
                    "DREAMKOREA",
                    color = Color(0xFF003F73).copy(alpha = 0.06f),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Side-by-side panels
                Row(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reading panel (left)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Section header
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Reading",
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Grid
                        QuestionGridWorksheet(
                            test = t,
                            items = readingItems,
                            answers = answers,
                            currentIdx = currentIdx,
                            sound = sound,
                            haptic = haptic,
                            filterMode = filterMode
                        ) { idx ->
                            currentIdx = idx
                            showGrid = false
                        }
                    }

                    // Listening panel (right)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Listening",
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        QuestionGridWorksheet(
                            test = t,
                            items = listeningItems,
                            answers = answers,
                            currentIdx = currentIdx,
                            sound = sound,
                            haptic = haptic,
                            filterMode = filterMode
                        ) { idx ->
                            currentIdx = idx
                            showGrid = false
                        }
                    }
                }
            }

            // Thin divider above submit
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))

            // ── SUBMIT BUTTON ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showSubmitDialog = true },
                    modifier = Modifier.fillMaxWidth(0.5f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Submit and Finish Exam",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Submit confirmation dialog
        if (showSubmitDialog) {
            val warning = when {
                totalUnsolved == 0 -> "You answered all $totalQuestions questions. Ready to submit!"
                totalUnsolved <= 5 -> "You have $totalUnsolved unanswered question(s). Submit anyway?"
                else -> "Warning: $totalUnsolved questions are still unanswered! Submit anyway?"
            }
            val warningColor = when {
                totalUnsolved == 0 -> Color(0xFF16A34A)
                totalUnsolved <= 5 -> Color(0xFFD97706)
                else -> Color(0xFFDC2626)
            }
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text("Submit Exam?") },
                text = {
                    Column {
                        Text(warning, color = warningColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Reading: $readingAnswered/${readingItems.size} • Listening: $listeningAnswered/${listeningItems.size}",
                            color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitDialog = false
                            if (!submitting) {
                                sound.swoosh(); submitting = true
                                scope.launch {
                                    try { submitResult = submitExamWithFallback(t, answers.toMap()); sound.success() }
                                    catch (e: Exception) { error = "Submit failed." }
                                    submitting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003F73))
                    ) { if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) } else { Text("Submit", color = Color.White) } }
                },
                dismissButton = { OutlinedButton(onClick = { showSubmitDialog = false }) { Text("Cancel") } }
            )
        }
        return
    }
}

/// Worksheet-style status tab (ALL / SOLVED / UNSOLVED). Underlines when selected.
@Composable
private fun StatusTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF1E3A8A) else Color(0xFF6B7280),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 0.dp)
                .height(2.dp)
                .background(Color(0xFF1E3A8A))
        )
    }
}

/// Worksheet-style 5×4 grid — rectangular boxes (4:3), sharp corners, dark borders.
/// Matches the reference screenshot aesthetic: flat, paper-form, no cards/shadows.
@Composable
private fun QuestionGridWorksheet(
    test: TestDetail,
    items: List<TestItemDetail>,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    onPick: (Int) -> Unit,
) {
    val globalIndices = items.mapNotNull { item -> test.items.indexOfFirst { it.question.id == item.question.id }.takeIf { it >= 0 } }
    val cols = 5
    val rowsCount = 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowIdx in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIdx in 0 until cols) {
                    val localIdx = rowIdx * cols + colIdx
                    if (localIdx < items.size) {
                        val globalIdx = globalIndices[localIdx]
                        val isAnswered = answers.containsKey(items[localIdx].question.id)
                        val isCurrent = globalIdx == currentIdx
                        val isFilteredOut = when (filterMode) {
                            true -> !isAnswered
                            false -> isAnswered
                            null -> false
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.33f)  // rectangular, wider than tall (4:3)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.5.dp,
                                    color = when {
                                        isCurrent -> Color(0xFF1E3A8A)
                                        isAnswered -> Color(0xFF1E3A8A)
                                        else -> Color(0xFF333333)
                                    }
                                )
                                .background(
                                    when {
                                        isAnswered -> SolidColor(Color(0xFF1E3A8A))
                                        isCurrent -> SolidColor(Color(0xFFDBEAFE))
                                        else -> SolidColor(Color.White)
                                    }
                                )
                                .alpha(if (isFilteredOut) 0.2f else 1f)
                                .clickable(enabled = !isFilteredOut) {
                                    sound.click()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPick(globalIdx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${globalIdx + 1}",
                                color = when {
                                    isAnswered -> Color.White
                                    isCurrent -> Color(0xFF1E3A8A)
                                    else -> Color(0xFF1F2937)
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    } else {
                        val baseNum = if (globalIndices.isNotEmpty()) globalIndices[0] else 0
                        val displayNum = baseNum + localIdx + 1
                        // Empty box — light gray border, not clickable
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.33f)
                                .border(1.dp, Color(0xFFE5E7EB))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$displayNum",
                                color = Color(0xFFD1D5DB),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}
'''

content = content[:start_idx] + NEW_BLOCK + content[end_idx:]

# --- 2. Remove old unused helper functions ---
# Remove SectionCard, LegendDot, FilterChip, old QuestionGridSection
old_helpers = [
    '''/// Section card with title, progress count, and accent stripe on the left.
@Composable
private fun SectionCard(''',
    '''/// Small colored dot + label, used in the legend row.
@Composable
private fun LegendDot(''',
    '''/// Pill-shaped filter chip used to toggle between All / Unsolved / Answered views.
@Composable
private fun FilterChip(''',
    '''/// 5×4 grid of question buttons. Left-to-right numbering (1-20 or 21-40).
/// Answered = dark blue bg + white text. Current = amber border. Empty = light gray border.
/// filterMode: null = all, true = answered only, false = unsolved only (others dimmed)
@Composable
private fun QuestionGridSection(''',
]

# Find each old function and remove it (from its doc comment to the closing brace)
for old_func_start in old_helpers:
    idx = content.find(old_func_start)
    if idx == -1:
        print(f"WARNING: Could not find {old_func_start[:50]}")
        continue
    # Find the matching closing brace (the function ends with "\n}\n")
    # We need to find the end of the function - count braces
    brace_count = 0
    started = False
    i = idx
    while i < len(content):
        c = content[i]
        if c == '{':
            brace_count += 1
            started = True
        elif c == '}':
            brace_count -= 1
            if started and brace_count == 0:
                # Found the end - include the closing brace and trailing newline
                end = i + 1
                # Skip trailing whitespace/newlines
                while end < len(content) and content[end] in ' \t\n':
                    end += 1
                content = content[:idx] + content[end:]
                print(f"Removed: {old_func_start[:50]}...")
                break
        i += 1

with open(FILE, 'w') as f:
    f.write(content)

print(f"\nFinal file size: {len(content)} chars, {content.count(chr(10))} lines")
