#!/usr/bin/env python3
"""Rebuild the exam grid page to match the HTML reference exactly."""
import re

FILE = "/home/z/my-project/student-app-rust/android-wrapper/app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt"

with open(FILE, 'r') as f:
    content = f.read()

# Find the grid page block start
START_MARKER = "            // ── QUESTION GRID PAGE ── worksheet-style exam navigation."
END_MARKER = "        return\n    }\n}\n"

start_idx = content.find(START_MARKER)
if start_idx == -1:
    print("ERROR: Could not find grid page start")
    exit(1)

end_idx = content.find(END_MARKER, start_idx)
if end_idx == -1:
    print("ERROR: Could not find grid page end")
    exit(1)
end_idx += len(END_MARKER)

NEW_BLOCK = '''            // ── QUESTION GRID PAGE ── matches HTML reference (4-col square grid, blue #1a56ff)
    if (showGrid) {
        val readingItems = t.items.filter { it.question.blockType != "audio" }
        val listeningItems = t.items.filter { it.question.blockType == "audio" }
        var showSubmitDialog by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        // Filter: null = all, true = solved only, false = unsolved only
        var filterMode by remember { mutableStateOf<Boolean?>(null) }

        val readingAnswered = readingItems.count { answers.containsKey(it.question.id) }
        val listeningAnswered = listeningItems.count { answers.containsKey(it.question.id) }
        val totalAnswered = readingAnswered + listeningAnswered
        val totalQuestions = t.items.size
        val totalUnsolved = totalQuestions - totalAnswered
        // HH:MM:SS timer format (matches HTML reference)
        val hh = timeLeft / 3600; val mm = (timeLeft % 3600) / 60; val ss = timeLeft % 60
        val timeStr = String.format("%02d:%02d:%02d", hh, mm, ss)
        val isLowTime = timeLeft in 1..300
        val timerColor = if (isLowTime) Color(0xFFDC2626) else Color(0xFF222222)
        val accentBlue = Color(0xFF1A56FF)

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            // ── TABS ROW: All | Solved | UnSolved (with blue underline on active) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RefTab("All", filterMode == null, accentBlue) { filterMode = null }
                RefTab("Solved", filterMode == true, accentBlue) { filterMode = true }
                RefTab("UnSolved", filterMode == false, accentBlue) { filterMode = false }
            }
            // Thin border under tabs
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E2E2)))

            // ── TIMER (large, centered, monospace) ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    timeStr,
                    color = timerColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E2E2)))

            // ── MAIN AREA: Reading LEFT | Listening RIGHT (with watermark bg) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
            ) {
                // Watermark logo in background
                Image(
                    painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .alpha(0.04f),
                    contentScale = ContentScale.Fit
                )

                // Side-by-side scrollable panels
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reading panel (left)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Section label
                        Text(
                            "Reading",
                            color = Color(0xFF333333),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        // Scrollable grid with border
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .border(2.dp, Color(0xFF111111), RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            QuestionGridRef(
                                test = t,
                                items = readingItems,
                                answers = answers,
                                currentIdx = currentIdx,
                                sound = sound,
                                haptic = haptic,
                                filterMode = filterMode,
                                accentBlue = accentBlue
                            ) { idx ->
                                currentIdx = idx
                                showGrid = false
                            }
                        }
                    }

                    // Listening panel (right)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            "Listening",
                            color = Color(0xFF333333),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .border(2.dp, Color(0xFF111111), RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            QuestionGridRef(
                                test = t,
                                items = listeningItems,
                                answers = answers,
                                currentIdx = currentIdx,
                                sound = sound,
                                haptic = haptic,
                                filterMode = filterMode,
                                accentBlue = accentBlue
                            ) { idx ->
                                currentIdx = idx
                                showGrid = false
                            }
                        }
                    }
                }
            }

            // ── SUBMIT BUTTON (blue pill, full-width-ish, at bottom) ─────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showSubmitDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "Submit and Finish Exam",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
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
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                    ) { if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) } else { Text("Submit", color = Color.White) } }
                },
                dismissButton = { OutlinedButton(onClick = { showSubmitDialog = false }) { Text("Cancel") } }
            )
        }
        return
    }
}

/// Reference-style tab: flex:1, centered, blue bottom border (3dp) when active.
@Composable
private fun RefTab(label: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (active) Color(0xFFF2F2F2) else Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = if (active) Color(0xFF111111) else Color(0xFF444444),
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 3.dp else 0.dp)
                .background(accent)
        )
    }
}

/// Reference-style 4-column grid of square cells with 2px black borders.
/// Solved = blue fill + white text. Current = blue border + glow.
/// Uses continuous numbering: Reading 1-20, Listening 21-40 (from globalIdx).
@Composable
private fun QuestionGridRef(
    test: TestDetail,
    items: List<TestItemDetail>,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    sound: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    filterMode: Boolean?,
    accentBlue: Color,
    onPick: (Int) -> Unit,
) {
    val globalIndices = items.mapNotNull { item -> test.items.indexOfFirst { it.question.id == item.question.id }.takeIf { it >= 0 } }
    val cols = 4  // 4 columns per the HTML reference (5 rows × 4 cols = 20 questions)

    // Compute rows needed (always 5 for 20 questions, but dynamic for safety)
    val rowsCount = (items.size + cols - 1) / cols

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
                                .aspectRatio(1f)  // perfect square (matches HTML aspect-ratio: 1)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = 2.dp,
                                    color = if (isAnswered) accentBlue else Color(0xFF111111),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(if (isAnswered) accentBlue else Color.White)
                                .alpha(if (isFilteredOut) 0.15f else 1f)
                                .clickable(enabled = !isFilteredOut) {
                                    sound.click()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPick(globalIdx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${globalIdx + 1}",
                                color = if (isAnswered) Color.White else Color(0xFF111111),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        // Empty placeholder cell (keeps grid aligned)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            val baseNum = if (globalIndices.isNotEmpty()) globalIndices[0] else 0
                            Text(
                                "${baseNum + localIdx + 1}",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp,
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

# Remove old StatusTab and QuestionGridWorksheet functions (no longer used)
old_funcs_to_remove = [
    "/// Worksheet-style status tab (ALL / SOLVED / UNSOLVED). Underlines when selected.\n@Composable\nprivate fun StatusTab(",
    "/// Worksheet-style 5×4 grid — rectangular boxes (4:3), sharp corners, dark borders.\n@Composable\nprivate fun QuestionGridWorksheet(",
]

for old_func in old_funcs_to_remove:
    idx = content.find(old_func)
    if idx == -1:
        print(f"WARNING: Could not find {old_func[:60]}")
        continue
    # Find matching closing brace
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
                end = i + 1
                while end < len(content) and content[end] in ' \t\n':
                    end += 1
                content = content[:idx] + content[end:]
                print(f"Removed: {old_func[:60]}...")
                break
        i += 1

with open(FILE, 'w') as f:
    f.write(content)

print(f"\nFinal file: {content.count(chr(10))} lines")
