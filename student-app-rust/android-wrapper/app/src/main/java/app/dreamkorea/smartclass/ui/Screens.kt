package app.dreamkorea.smartclass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

@Composable
fun MainScreen(userName: String, onLogout: () -> Unit) {
    val theme = rememberAppTheme()
    val sound = rememberSoundManager()
    var tab by remember { mutableStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }
    var currentExamId by remember { mutableStateOf<String?>(null) }
    var liveRoomOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = theme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // If exam is active, show exam screen instead of tabs
            if (currentExamId != null) {
                ExamScreen(theme = theme, testId = currentExamId!!, onExit = { currentExamId = null })
                return@Surface
            }
            // If live room is open
            if (liveRoomOpen) {
                LiveRoomScreen(theme = theme, onBack = { liveRoomOpen = false })
                return@Surface
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar — clean, minimal (just logo + greeting + settings)
                Surface(color = theme.white, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Taegeuk logo mark only (no text)
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(theme.primary)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
                                    .background(theme.accent)
                            )
                        }
                        // Greeting centered
                        Text("Hi, $userName 👋", color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        // Settings gear
                        IconButton(onClick = { sound.click(); settingsOpen = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = theme.darkText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Animated content swap
                Box(modifier = Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HomeTab(theme, onNavigate = { tab = it }, onStartExam = { currentExamId = it }, onOpenLiveRoom = { liveRoomOpen = true })
                        1 -> LearnTab(theme)
                        2 -> BooksTab(theme)
                        3 -> TestsTab(theme, onStartExam = { currentExamId = it })
                        4 -> VideosTab(theme)
                        5 -> ProfileTab(theme, userName, onLogout)
                    }
                }

                // Bottom nav
                Surface(color = theme.white, shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        navItems().forEachIndexed { i, item ->
                            val isSelected = tab == i
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "navScale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { tab = i }.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) theme.primary else theme.subText,
                                    modifier = Modifier.size(22.dp).scale(scale)
                                )
                                Text(
                                    item.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) theme.primary else theme.subText,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Settings sheet overlay
            if (settingsOpen) {
                SettingsSheet(theme = theme, onDismiss = { settingsOpen = false })
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)
private fun navItems() = listOf(
    NavItem("Home", Icons.Default.Home),
    NavItem("Learn", Icons.Default.School),
    NavItem("Books", Icons.Default.Book),
    NavItem("Tests", Icons.Default.Quiz),
    NavItem("Videos", Icons.Default.VideoLibrary),
    NavItem("Profile", Icons.Default.Person)
)

// ─── Home — all features accessible from here ─────────────────────────────────
@Composable
fun HomeTab(theme: AppTheme, onNavigate: (Int) -> Unit, onStartExam: (String) -> Unit = {}, onOpenLiveRoom: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var statsLoading by remember { mutableStateOf(true) }
    var recentTests by remember { mutableStateOf<List<TestItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                stats = AppState.api.getStats().stats
                recentTests = AppState.api.getTests().tests.take(3)
            } catch (_: Exception) {}
            statsLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero card with gradient
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(theme.primary, lerp(theme.primary, Color.Black, 0.3f))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Text("안녕하세요!", color = Color.White.copy(0.85f), fontSize = 14.sp)
                        Text("Welcome back", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Let's continue your Korean journey today.", color = Color.White.copy(0.75f), fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Row {
                            Button(
                                onClick = { onNavigate(3) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = theme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text("Take a Test", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { onNavigate(1) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text("Learn", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Stats row (live from API)
        item {
            Text("Your Progress", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        item {
            if (statsLoading) {
                SkeletonStatsRow(theme)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMini(theme, "${stats?.totalExamsTaken ?: 0}", "Exams", Icons.Default.Quiz)
                    StatMini(theme, "${String.format("%.0f", stats?.averageScore ?: 0.0)}%", "Avg", Icons.Default.TrendingUp)
                    StatMini(theme, "${stats?.studyStreakDays ?: 0}d", "Streak", Icons.Default.LocalFireDepartment)
                }
            }
        }

        // Quick access grid — ALL features from home
        item {
            Text("Quick Access", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        item {
            // 2x3 grid of feature cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard(theme, "Subjects", "Browse all lessons", Icons.Default.School, theme.primary) { onNavigate(1) }
                    FeatureCard(theme, "Books", "Digital library", Icons.Default.Book, theme.accent) { onNavigate(2) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard(theme, "Tests", "Exams & practice", Icons.Default.Quiz, Color(0xFFFF9800)) { onNavigate(3) }
                    FeatureCard(theme, "Videos", "Video lessons", Icons.Default.VideoLibrary, Color(0xFFE91E63)) { onNavigate(4) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard(theme, "Profile", "Your account & stats", Icons.Default.Person, Color(0xFF607D8B)) { onNavigate(5) }
                    FeatureCard(theme, "Audio", "Listening practice", Icons.Default.Headphones, Color(0xFF009688)) { onNavigate(1) }
                }
                // Live class card (full width)
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLiveRoom() },
                    shadowElevation = 2.dp
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = theme.accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.VideoCall, null, tint = theme.accent, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Join Live Class", color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Enter the 6-char code from your teacher", color = theme.subText, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = theme.subText)
                    }
                }
            }
        }

        // Daily tip card
        item {
            Surface(
                color = theme.cardBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 1.dp
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = theme.primaryLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Lightbulb, null, tint = theme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Tip", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Practice 한글 (Hangul) for 10 minutes daily — consistency beats intensity.", color = theme.subText, fontSize = 11.sp, maxLines = 2)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun RowScope.StatMini(theme: AppTheme, value: String, label: String, icon: ImageVector) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = theme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = theme.subText, fontSize = 10.sp)
        }
    }
}

@Composable
fun RowScope.FeatureCard(
    theme: AppTheme,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "featureScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).scale(scale),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.subText, fontSize = 10.sp, maxLines = 1)
        }
    }
}

// ─── Learn ────────────────────────────────────────────────────────────────────
@Composable
fun LearnTab(theme: AppTheme) {
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { subjects = AppState.api.getSubjects().subjects } catch (_: Exception) {} ; loading = false }
    }

    if (loading) {
        SkeletonListScreen(theme, itemCount = 5)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Subjects", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item {
            Text("Choose a subject to start learning Korean", color = theme.subText, fontSize = 12.sp)
        }
        if (subjects.isEmpty()) {
            item {
                EmptyState(theme, "No subjects yet", "Check back soon — your instructor is adding content.", Icons.Default.School)
            }
        } else {
            itemsIndexed(subjects) { i, s ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(
                        color = theme.cardBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 1.dp
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = theme.primaryLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Book, null, tint = theme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(s.name, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(s.description ?: "", color = theme.subText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Wraps content with a fade-in + slide-up animation. */
@Composable
fun AnimatedListItem(index: Int, theme: AppTheme, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300)
        )
    ) {
        content()
    }
}

// ─── Books ────────────────────────────────────────────────────────────────────
@Composable
fun BooksTab(theme: AppTheme) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { books = AppState.api.getBooks().books } catch (_: Exception) {} ; loading = false }
    }

    if (loading) {
        SkeletonListScreen(theme, itemCount = 5)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Books", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Text("Digital library for Korean learning", color = theme.subText, fontSize = 12.sp) }

        if (books.isEmpty()) {
            item {
                EmptyState(theme, "No books yet", "Your instructor will add books here soon.", Icons.Default.Book)
            }
        } else {
            itemsIndexed(books) { i, b ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Surface(color = theme.primary, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(54.dp, 72.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Book, null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(b.title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (!b.author.isNullOrBlank()) Text("by ${b.author}", color = theme.subText, fontSize = 12.sp)
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    if (!b.category.isNullOrBlank()) Text(b.category, color = theme.primary, fontSize = 10.sp)
                                    if (!b.level.isNullOrBlank()) { Spacer(Modifier.width(8.dp)); Text(b.level, color = theme.subText, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Tests ────────────────────────────────────────────────────────────────────
@Composable
fun TestsTab(theme: AppTheme, onStartExam: (String) -> Unit = {}) {
    val sound = rememberSoundManager()
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<TestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { tests = AppState.api.getTests().tests } catch (_: Exception) {} ; loading = false }
    }

    if (loading) {
        SkeletonListScreen(theme, itemCount = 5)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Tests & Exams", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Text("Tap a test to start. Your stats will update automatically.", color = theme.subText, fontSize = 12.sp) }

        if (tests.isEmpty()) {
            item {
                EmptyState(theme, "No tests yet", "Your instructor will assign tests here soon.", Icons.Default.Quiz)
            }
        } else {
            itemsIndexed(tests) { i, t ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(
                        color = theme.cardBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            sound.click()
                            onStartExam(t.id)
                        },
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(t.title, color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (t.isExam) theme.accent.copy(alpha = 0.15f) else theme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (t.isExam) "EXAM" else "PRACTICE",
                                        color = if (t.isExam) theme.accent else theme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (!t.description.isNullOrBlank()) { Spacer(Modifier.height(4.dp)); Text(t.description, color = theme.subText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, null, tint = theme.subText, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${t.durationMin} min", color = theme.subText, fontSize = 11.sp)
                                Spacer(Modifier.width(16.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = theme.subText, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pass: ${t.passScore}%", color = theme.subText, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                Text("Start →", color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Videos ───────────────────────────────────────────────────────────────────
@Composable
fun VideosTab(theme: AppTheme) {
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoLesson>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { videos = AppState.api.getVideoLessons().videos } catch (_: Exception) {} ; loading = false }
    }

    if (loading) {
        SkeletonGridScreen(theme, rows = 3)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Video Lessons", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Text("Watch and learn Korean", color = theme.subText, fontSize = 12.sp) }

        if (videos.isEmpty()) {
            item {
                EmptyState(theme, "No videos yet", "Your instructor will add video lessons soon.", Icons.Default.VideoLibrary)
            }
        } else {
            itemsIndexed(videos) { i, v ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(color = theme.errorRed, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(v.title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.padding(top = 2.dp)) {
                                Text("${v.durationMin} min", color = theme.subText, fontSize = 11.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("${v.views} views", color = theme.subText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Profile ──────────────────────────────────────────────────────────────────
@Composable
fun ProfileTab(theme: AppTheme, userName: String, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { stats = AppState.api.getStats().stats } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: profile header with stats badge at right
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                // Avatar + name
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = theme.primary, shape = RoundedCornerShape(36.dp), modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(userName.take(2).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(userName, color = theme.darkText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Student", color = theme.subText, fontSize = 12.sp)
                }
                // Stats card (top-right)
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.width(150.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Your Stats", color = theme.darkText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        if (loading) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp), theme = theme)
                            Spacer(Modifier.height(4.dp))
                            ShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp), theme = theme)
                        } else {
                            StatsRow(theme, "Exams", "${stats?.totalExamsTaken ?: 0}")
                            StatsRow(theme, "Avg", "${String.format("%.0f", stats?.averageScore ?: 0.0)}%")
                            StatsRow(theme, "Streak", "${stats?.studyStreakDays ?: 0}d")
                            StatsRow(theme, "Books", "${stats?.booksRead ?: 0}")
                            StatsRow(theme, "Audio", "${stats?.audioLessonsCompleted ?: 0}")
                        }
                    }
                }
            }
        }

        // Big stats grid
        item {
            Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceEvenly) {
                    ProfileStat(theme, "${stats?.totalExamsTaken ?: 0}", "Exams")
                    ProfileStat(theme, "${String.format("%.0f", stats?.averageScore ?: 0.0)}%", "Avg")
                    ProfileStat(theme, "${stats?.studyStreakDays ?: 0}", "Streak")
                    ProfileStat(theme, "${stats?.badgesEarned ?: 0}", "Badges")
                }
            }
        }

        // Account info
        item {
            val userEmail = AppState.user?.email ?: ""
            val userPhone = AppState.user?.phone ?: ""
            Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (userEmail.isNotEmpty()) {
                        Text("Email", color = theme.subText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(userEmail, color = theme.darkText, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (userPhone.isNotEmpty()) {
                        Text("Phone", color = theme.subText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(userPhone, color = theme.darkText, fontSize = 13.sp)
                    }
                }
            }
        }

        // Logout
        item {
            Button(
                onClick = { scope.launch { try { AppState.api.logout() } catch (_: Exception) {} ; AppState.clearSession() ; onLogout() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.errorRed),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }

        item {
            Text("DreamKorea SmartClass v1.0.0", color = theme.subText, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatsRow(theme: AppTheme, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.subText, fontSize = 11.sp)
        Text(value, color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileStat(theme: AppTheme, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = theme.subText, fontSize = 11.sp)
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
fun EmptyState(theme: AppTheme, title: String, body: String, icon: ImageVector) {
    Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = theme.subText.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = theme.subText, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
