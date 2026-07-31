    // ── QUESTION GRID PAGE ── matches reference design. Shown FIRST when the
    // exam starts. Layout (landscape):
    //   • LEFT: vertical blue "Submit and Finish Exam" bar
    //   • CENTER: watermark logo (faint) + two grids (Reading 1-20, Listening 21-40)
    //     each grid = 4 columns, column-major (1-5 down col 1, 6-10 down col 2, ...)
    //   • RIGHT: sidebar with exam title, All/Solved/Unsolved tabs, timer
    // Portrait: same layout but stacked vertically.
    if (showGrid) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
        val totalQ = t.items.size
        // Split questions into Reading (text block) and Listening (audio block)
        val readingItems = t.items.filter { it.question.blockType != "audio" }
        val listeningItems = t.items.filter { it.question.blockType == "audio" }
        // If no block type info, just split in half
        val readingList = if (readingItems.isNotEmpty()) readingItems else t.items.take((totalQ + 1) / 2)
        val listeningList = if (listeningItems.isNotEmpty()) listeningItems else t.items.drop((totalQ + 1) / 2)

        // Tab filter state
        var tabFilter by remember { mutableStateOf(0) } // 0=All, 1=Solved, 2=Unsolved

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ── Watermark logo at center (faint) ────────────────────────────
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(200.dp).alpha(0.06f),
                contentScale = ContentScale.Fit
            )

            if (isLandscape) {
                // ── LANDSCAPE LAYOUT ── Submit bar | Grids | Sidebar ──────────
                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT: Vertical Submit bar
                    if (answeredCount > 0) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF003478))
                                .clickable {
                                    if (!submitting) {
                                        sound.swoosh()
                                        submitting = true
                                        scope.launch {
                                            try {
                                                submitResult = if (t.id == "qbank-combined" || t.id.startsWith("bundle-")) {
                                                    submitCombinedExamWithFallback(t, answers.toMap())
                                                } else {
                                                    AppState.api.submitTest(t.id, SubmitRequest(answers.toMap()))
                                                }
                                                sound.success()
                                            } catch (e: Exception) {
                                                error = "Submit failed: ${e.message ?: "unknown error"}"
                                            }
                                            submitting = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            } else {
                                Text(
                                    "Submit and Finish Exam",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.rotate(-90f),
                                )
                            }
                        }
                    }

                    // CENTER: Grids (Reading + Listening) — scrollable
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reading grid
                        if (readingList.isNotEmpty()) {
                            item {
                                Text("Reading", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            item { QuestionGridSection(t, readingList, answers, currentIdx, tabFilter, sound) { idx ->
                                currentIdx = idx; showGrid = false
                            } }
                        }
                        // Listening grid
                        if (listeningList.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Listening", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            item { QuestionGridSection(t, listeningList, answers, currentIdx, tabFilter, sound) { idx ->
                                currentIdx = idx; showGrid = false
                            } }
                        }
                    }

                    // RIGHT: Sidebar
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Exam title
                        Text(
                            t.title.take(30),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Student name
                        Text(
                            AppState.getUserName(),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        // Timer
                        val mm = timeLeft / 60; val ss = timeLeft % 60
                        Text(
                            String.format("%02d:%02d", mm, ss),
                            color = Color(0xFF003478),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        // Tabs: All / Solved / Unsolved
                        TabButton("All", tabFilter == 0) { tabFilter = 0 }
                        TabButton("Solved", tabFilter == 1) { tabFilter = 1 }
                        TabButton("Unsolved", tabFilter == 2) { tabFilter = 2 }
                        Spacer(Modifier.height(4.dp))
                        Text("$answeredCount / $totalQ", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        // Exit button at bottom
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(2.dp, Color.Black)
                                .background(Color.White)
                                .clickable { sound.click(); onExit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Exit", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ── PORTRAIT LAYOUT ── grids on top, sidebar below ───────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top bar with title + timer
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.title.take(25), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val mm = timeLeft / 60; val ss = timeLeft % 60
                            Text(String.format("%02d:%02d", mm, ss), color = Color(0xFF003478), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Tabs
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TabButton("All", tabFilter == 0) { tabFilter = 0 }
                            TabButton("Solved", tabFilter == 1) { tabFilter = 1 }
                            TabButton("Unsolved", tabFilter == 2) { tabFilter = 2 }
                        }
                    }
                    // Reading grid
                    if (readingList.isNotEmpty()) {
                        item { Text("Reading", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        item { QuestionGridSection(t, readingList, answers, currentIdx, tabFilter, sound) { idx ->
                            currentIdx = idx; showGrid = false
                        } }
                    }
                    // Listening grid
                    if (listeningList.isNotEmpty()) {
                        item { Text("Listening", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        item { QuestionGridSection(t, listeningList, answers, currentIdx, tabFilter, sound) { idx ->
                            currentIdx = idx; showGrid = false
                        } }
                    }
                    // Submit + Exit
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).height(48.dp).border(2.dp, Color.Black).background(Color.White).clickable { sound.click(); onExit() },
                                contentAlignment = Alignment.Center
                            ) { Text("Exit", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (answeredCount > 0) {
                                Box(
                                    modifier = Modifier.weight(2f).height(48.dp).background(Color(0xFF003478)).clickable {
                                        if (!submitting) {
                                            sound.swoosh(); submitting = true
                                            scope.launch {
                                                try {
                                                    submitResult = if (t.id == "qbank-combined" || t.id.startsWith("bundle-")) submitCombinedExamWithFallback(t, answers.toMap()) else AppState.api.submitTest(t.id, SubmitRequest(answers.toMap()))
                                                    sound.success()
                                                } catch (e: Exception) { error = "Submit failed: ${e.message ?: "unknown error"}" }
                                                submitting = false
                                            }
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (submitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 3.dp) }
                                    else { Text("Submit ($answeredCount)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }
}

/// Renders a 4-column grid of square question boxes with column-major
/// numbering (1-5 down col 1, 6-10 down col 2, ...). Applies the tab filter
/// (All / Solved / Unsolved) and highlights answered/current questions.
@Composable
private fun QuestionGridSection(
    test: TestDetail,
    items: List<TestItemDetail>,
    answers: SnapshotStateMap<String, Any>,
    currentIdx: Int,
    tabFilter: Int,
    sound: SoundManager,
    onPick: (Int) -> Unit,
) {
    val globalIndices = items.mapNotNull { item -> test.items.indexOfFirst { it.question.id == item.question.id }.takeIf { it >= 0 } }
    val cols = 4
    val rowsCount = (items.size + cols - 1) / cols

    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (rowIdx in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (colIdx in 0 until cols) {
                        val localIdx = colIdx * rowsCount + rowIdx
                        if (localIdx < items.size) {
                            val globalIdx = globalIndices[localIdx]
                            val isAnswered = answers.containsKey(items[localIdx].question.id)
                            val isCurrent = globalIdx == currentIdx
                            val visible = when (tabFilter) {
                                1 -> isAnswered
                                2 -> !isAnswered
                                else -> true
                            }
                            if (visible) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .border(2.dp, Color.Black)
                                        .background(if (isAnswered) Color.Black else Color.White)
                                        .clickable { sound.click(); onPick(globalIdx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${globalIdx + 1}",
                                        color = if (isAnswered) Color.White else Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .border(1.5.dp, Color.Black)
            .background(if (selected) Color(0xFF003478) else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
