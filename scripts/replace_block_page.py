#!/usr/bin/env python3
"""Replace the showGrid block (lines 555-816) with a responsive landscape layout."""

with open('/home/z/my-project/student-app-rust/android-wrapper/app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt', 'r') as f:
    lines = f.readlines()

# Lines 555-816 (1-indexed), so indices 554-815
new_block = '''    if (showGrid) {
        val readingItems = sortedItems.filter { it.question.blockType != "audio" }
        val listeningItems = sortedItems.filter { it.question.blockType == "audio" }
        val isQBank = testId == "qbank-combined" || testId.startsWith("bundle-")
        var showSubmitDialog by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        var filterMode by remember { mutableStateOf<Boolean?>(null) }
        val readingAnswered = readingItems.count { answers.containsKey(it.question.id) }
        val listeningAnswered = listeningItems.count { answers.containsKey(it.question.id) }
        val totalAnswered = readingAnswered + listeningAnswered
        val totalQuestions = t.items.size
        val totalUnsolved = totalQuestions - totalAnswered
        val hh = timeLeft / 3600; val mm = (timeLeft % 3600) / 60; val ss = timeLeft % 60
        val timeStr = String.format("%02d : %02d : %02d", hh, mm, ss)
        val isLowTime = timeLeft in 1..300
        val timerColor = if (isLowTime) Color(0xFFDC2626) else Color(0xFF222222)
        val accentBlue = Color(0xFF1A56FF)
        val studentName = AppState.getUserName() ?: "Student"
        val studentEmail = AppState.getUserEmail() ?: ""
        val navGray = Color(0xFFF3F3F3)

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
            val cw = maxWidth.value  // canvas width in dp
            val ch = maxHeight.value // canvas height in dp
            val headerSp = (cw * 0.018f).coerceIn(12f, 22f).sp
            val navSp = (cw * 0.016f).coerceIn(11f, 18f).sp
            val titleSp = (cw * 0.014f).coerceIn(10f, 16f).sp
            val numSp = (cw * 0.014f).coerceIn(10f, 18f).sp
            val submitSp = (cw * 0.013f).coerceIn(10f, 15f).sp
            val gap = (cw * 0.008f).dp
            val pad = (cw * 0.01f).dp

            Column(modifier = Modifier.fillMaxSize().padding(pad).border(2.dp, Color(0xFF333333))) {
                // ── Row 1: Logo + Header (11% height) ──
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.11f)) {
                    // Logo column (9.5% width)
                    Box(modifier = Modifier.fillMaxHeight().width(cw * 0.085f).border(2.dp, Color(0xFF333333)), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo), contentDescription = null, modifier = Modifier.size(cw * 0.06f), contentScale = ContentScale.Fit)
                    }
                    // Header content (90.5% width): title | id | name
                    Row(modifier = Modifier.fillMaxHeight().weight(1f).border(width = 0.dp, color = Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(0.41f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(t.title.take(30), color = Color(0xFF171717), fontSize = headerSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Box(modifier = Modifier.weight(0.27f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(studentEmail.take(15), color = Color(0xFF171717), fontSize = headerSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Box(modifier = Modifier.weight(0.32f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(studentName.take(15), color = Color(0xFF171717), fontSize = headerSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF333333)))
                // ── Row 2: Nav tabs (11% height) ──
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.13f)) {
                    // Empty space under logo
                    Spacer(modifier = Modifier.width(cw * 0.085f).fillMaxHeight())
                    // 5 nav sections
                    val navLabels = listOf("Nepal" to (null as Boolean?), "All" to null, "Solved" to true, "Unsolved" to false)
                    navLabels.forEach { (label, mode) ->
                        val selected = filterMode == mode
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (selected) navGray else Color.White).clickable { sound.click(); filterMode = mode }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, color = Color(0xFF151515), fontSize = navSp, fontWeight = FontWeight.Medium)
                                if (selected) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black)) }
                            }
                        }
                    }
                    // Timer
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(timeStr, color = timerColor, fontSize = navSp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF333333)))
                // ── Row 3: Section titles (8% height) ──
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.1f).padding(horizontal = pad)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = gap / 2).border(1.5.dp, Color(0xFFC8C8C8)), contentAlignment = Alignment.Center) { Text("Reading", color = Color(0xFF202020), fontSize = titleSp, fontWeight = FontWeight.Medium) }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = gap / 2).border(1.5.dp, Color(0xFFC8C8C8)), contentAlignment = Alignment.Center) { Text("Listening", color = Color(0xFF202020), fontSize = titleSp, fontWeight = FontWeight.Medium) }
                }
                Spacer(modifier = Modifier.height(gap))
                // ── Row 4: Question grids (50% height) ──
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f).padding(horizontal = pad)) {
                    // Reading grid
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = gap / 2)) {
                        Box(modifier = Modifier.fillMaxSize().border(2.5.dp, Color(0xFF111111), RoundedCornerShape(cw * 0.012f)).background(Color.White).padding(cw * 0.008f)) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                                for (rowIdx in 0 until 4) {
                                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        for (colIdx in 0 until 5) {
                                            val localIdx = rowIdx * 5 + colIdx
                                            val displayNum = localIdx + 1
                                            if (localIdx < readingItems.size) {
                                                val globalIdx = localIdx
                                                val item = readingItems[localIdx]
                                                val isAnswered = answers.containsKey(item.question.id)
                                                val isCurrent = globalIdx == currentIdx
                                                val isFilteredOut = when (filterMode) { true -> !isAnswered; false -> isAnswered; null -> false }
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).border(2.dp, if (isCurrent) accentBlue else Color(0xFF111111)).background(if (isAnswered) accentBlue else Color.White).alpha(if (isFilteredOut) 0.15f else 1f).clickable(enabled = !isFilteredOut) { sound.click(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentIdx = globalIdx; showGrid = false }, contentAlignment = Alignment.Center) { Text("$displayNum", color = if (isAnswered) Color.White else Color(0xFF171717), fontSize = numSp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium) }
                                            } else {
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).border(2.dp, Color(0xFFEEEEEE)).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) { Text("$displayNum", color = Color(0xFFCCCCCC), fontSize = numSp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Listening grid
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = gap / 2)) {
                        Box(modifier = Modifier.fillMaxSize().border(2.5.dp, Color(0xFF111111), RoundedCornerShape(cw * 0.012f)).background(Color.White).padding(cw * 0.008f)) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                                for (rowIdx in 0 until 4) {
                                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        for (colIdx in 0 until 5) {
                                            val localIdx = rowIdx * 5 + colIdx
                                            val displayNum = localIdx + 21
                                            if (localIdx < listeningItems.size) {
                                                val globalIdx = readingItems.size + localIdx
                                                val item = listeningItems[localIdx]
                                                val isAnswered = answers.containsKey(item.question.id)
                                                val isCurrent = globalIdx == currentIdx
                                                val isFilteredOut = when (filterMode) { true -> !isAnswered; false -> isAnswered; null -> false }
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).border(2.dp, if (isCurrent) accentBlue else Color(0xFF111111)).background(if (isAnswered) accentBlue else Color.White).alpha(if (isFilteredOut) 0.15f else 1f).clickable(enabled = !isFilteredOut) { sound.click(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentIdx = globalIdx; showGrid = false }, contentAlignment = Alignment.Center) { Text("$displayNum", color = if (isAnswered) Color.White else Color(0xFF171717), fontSize = numSp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium) }
                                            } else {
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).border(2.dp, Color(0xFFEEEEEE)).background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) { Text("$displayNum", color = Color(0xFFCCCCCC), fontSize = numSp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // ── Row 5: Submit button (bottom area) ──
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Button(onClick = { showSubmitDialog = true }, modifier = Modifier.fillMaxWidth(0.25f).fillMaxHeight(0.7f), colors = ButtonDefaults.buttonColors(containerColor = accentBlue), shape = RoundedCornerShape(cw * 0.012f)) {
                        Text("Submit and Finish Exam", color = Color.White, fontSize = submitSp, fontWeight = FontWeight.Medium)
                    }
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
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text("Submit Exam?") },
                text = { Text(warning, fontSize = 13.sp) },
                confirmButton = {
                    Button(onClick = {
                        showSubmitDialog = false
                        if (!submitting) {
                            sound.swoosh(); submitting = true
                            scope.launch {
                                try { submitResult = submitExamWithFallback(t, answers.toMap()); sound.success() }
                                catch (e: Exception) { error = "Submit failed." }
                                submitting = false
                            }
                        }
                    }) { if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) } else { Text("Submit") } }
                },
                dismissButton = { OutlinedButton(onClick = { showSubmitDialog = false }) { Text("Cancel") } }
            )
        }
        return
    }
}
'''

# Replace lines 555-816 (indices 554-815)
new_lines = lines[:554] + [new_block] + lines[816:]

with open('/home/z/my-project/student-app-rust/android-wrapper/app/src/main/java/app/dreamkorea/smartclass/ui/ExamScreen.kt', 'w') as f:
    f.writelines(new_lines)

print("Block page replaced with responsive BoxWithConstraints layout")
