package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

// Professional color palette — matching the reference design
val NavyBlue = Color(0xFF1E3A8A)
val NavyBlueLight = Color(0xFF3B82F6)
val SuccessGreen = Color(0xFF22C55E)
val WarningOrange = Color(0xFFF59E0B)
val AccentPurple = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFEC4899)
val BgGray = Color(0xFFF8FAFC)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E293B)
val TextMid = Color(0xFF64748B)
val TextLight = Color(0xFF94A3B8)
val DividerColor = Color(0xFFE2E8F0)

// Navigation destinations
sealed class Screen {
    object Home : Screen()
    object Learn : Screen()
    object Books : Screen()
    object Tests : Screen()
    object Videos : Screen()
    object Profile : Screen()
    object LiveRoom : Screen()
    object Settings : Screen()
    object UbtTest : Screen()
    object FreeExam : Screen()
    object Batch : Screen()
    object Results : Screen()
    object QuestionBank : Screen()
    object AudioLessons : Screen()
    object Classroom : Screen()
    object RecordedVideo : Screen()
    object ClassResult : Screen()
    object CourseVideo : Screen()
    object Join : Screen()
    object EyeVision : Screen()
    data class Exam(val testId: String) : Screen()
    data class BookReader(val book: Book) : Screen()
    data class TestList(val filter: String, val title: String) : Screen()
}

// Tab destinations for bottom navigation
enum class BottomTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Exams("Exams", Icons.Default.Quiz),
    Progress("Progress", Icons.Default.BarChart),
    Profile("Profile", Icons.Default.Person),
}

@Composable
fun MainScreen(userName: String, onLogout: () -> Unit) {
    val theme = rememberAppTheme()
    val sound = rememberSoundManager()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var activeTab by remember { mutableStateOf(BottomTab.Home) }

    fun navigateTo(s: Screen) {
        screen = s
        // Update active tab based on screen
        activeTab = when (s) {
            is Screen.Home -> BottomTab.Home
            is Screen.Profile -> BottomTab.Profile
            is Screen.Results -> BottomTab.Progress
            is Screen.UbtTest, is Screen.FreeExam, is Screen.Batch,
            is Screen.Tests, is Screen.TestList, is Screen.QuestionBank,
            is Screen.Exam -> BottomTab.Exams
            else -> activeTab
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgGray) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top bar — hidden on Exam, BookReader, and main tab screens ──
            val showTopBar = when (screen) {
                is Screen.Exam -> false
                is Screen.BookReader -> false
                is Screen.Home -> false
                is Screen.UbtTest, is Screen.FreeExam, is Screen.Batch -> false
                is Screen.TestList -> false
                is Screen.Tests -> false
                is Screen.Profile -> false
                is Screen.Results -> false
                is Screen.QuestionBank -> false
                is Screen.Join -> false
                is Screen.EyeVision -> false
                is Screen.Books -> false
                is Screen.Settings -> false
                else -> true
            }
            if (showTopBar) {
                TopBar(theme, userName, sound, onProfile = { navigateTo(Screen.Profile) }, onSettings = { screen = Screen.Settings })
            }

            // ─── Screen content ─────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(200, easing = FastOutSlowInEasing))
                    },
                    label = "screenTransition"
                ) { s ->
                    when (s) {
                        is Screen.Home -> HomeScreen(theme, sound, userName, onNavigate = { navigateTo(it) })
                        is Screen.Learn -> LearnScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.Books -> BooksScreen(theme, sound, onBack = { navigateTo(Screen.Home) }, onBookClick = { screen = Screen.BookReader(it) })
                        is Screen.Tests -> TestsScreen(theme, sound, filter = "all", title = "All Exams", onBack = { navigateTo(Screen.Home) }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Videos -> VideosScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.Profile -> ProfileScreen(theme, sound, userName, onBack = { navigateTo(Screen.Home) }, onLogout = onLogout)
                        is Screen.LiveRoom -> LiveRoomScreen(theme, onBack = { navigateTo(Screen.Home) })
                        is Screen.Settings -> SettingsScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.Exam -> ExamScreen(theme, testId = s.testId, onExit = { navigateTo(Screen.Home) })
                        is Screen.BookReader -> BookReaderScreen(theme, sound, s.book, onBack = { screen = Screen.Books })
                        is Screen.UbtTest -> TestsScreen(theme, sound, filter = "exam", title = "UBT Exams", onBack = { navigateTo(Screen.Home) }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.FreeExam -> TestsScreen(theme, sound, filter = "demo", title = "Demo Exams", onBack = { navigateTo(Screen.Home) }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Batch -> TestsScreen(theme, sound, filter = "batch", title = "Batch Exams", onBack = { navigateTo(Screen.Home) }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Results -> ResultsScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.QuestionBank -> QuestionBankScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.AudioLessons -> LearnScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.Classroom -> LiveRoomScreen(theme, onBack = { navigateTo(Screen.Home) })
                        is Screen.RecordedVideo -> VideosScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.ClassResult -> ResultsScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.CourseVideo -> VideosScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.TestList -> TestsScreen(theme, sound, filter = s.filter, title = s.title, onBack = { navigateTo(Screen.Home) }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Join -> JoinScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                        is Screen.EyeVision -> EyeVisionScreen(theme, sound, onBack = { navigateTo(Screen.Home) })
                    }
                }
            }

            // ─── Bottom navigation bar ──────────────────────────────────────
            val showBottomBar = when (screen) {
                is Screen.Exam -> false
                is Screen.BookReader -> false
                is Screen.Settings -> false
                else -> true
            }
            if (showBottomBar) {
                BottomNavBar(activeTab = activeTab, onTabClick = { tab ->
                    sound.click()
                    when (tab) {
                        BottomTab.Home -> navigateTo(Screen.Home)
                        BottomTab.Exams -> navigateTo(Screen.Tests)
                        BottomTab.Progress -> navigateTo(Screen.Results)
                        BottomTab.Profile -> navigateTo(Screen.Profile)
                    }
                })
            }
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────
@Composable
fun BottomNavBar(activeTab: BottomTab, onTabClick: (BottomTab) -> Unit) {
    Surface(
        color = CardWhite,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.values().forEach { tab ->
                val isActive = tab == activeTab
                val color = if (isActive) NavyBlue else TextLight
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabClick(tab) }.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        tab.icon,
                        tab.label,
                        tint = color,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────
@Composable
fun TopBar(theme: AppTheme, userName: String, sound: SoundManager, onProfile: () -> Unit, onSettings: () -> Unit) {
    Surface(color = CardWhite, shadowElevation = 0.dp, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = "DreamKorea Logo",
                modifier = Modifier.size(38.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
            Text("DreamKorea", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { sound.click(); onProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, "Profile", tint = TextDark, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { sound.click(); onSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = TextDark, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// Async image loader — uses Coil
@Composable
fun AsyncImageLoader(url: String, modifier: Modifier = Modifier) {
    if (url.isBlank()) {
        Box(modifier = modifier.background(NavyBlue.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = NavyBlue.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
        }
        return
    }
    coil.compose.AsyncImage(model = url, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

// ─── HOME SCREEN — Professional EdTech design ─────────────────────────────────
@Composable
fun HomeScreen(theme: AppTheme, sound: SoundManager, userName: String, onNavigate: (Screen) -> Unit) {
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var homeCards by remember {
        mutableStateOf<List<HomeCard>>(AppState.getCachedNow<List<HomeCard>>(AppState.KEY_HOME_CARDS) ?: emptyList())
    }
    var loading by remember { mutableStateOf(homeCards.isEmpty()) }

    LaunchedEffect(Unit) {
        if (homeCards.isNotEmpty()) loading = false
        try {
            homeCards = AppState.getCachedHomeCards()
            stats = AppState.api.getStats().stats
        } catch (_: Exception) {}
        finally { loading = false }
    }

    val effectiveCards = remember(homeCards, loading) {
        if (loading) emptyList()
        else if (homeCards.isNotEmpty()) homeCards
        else listOf(
            HomeCard(key = "ubt_test", title = "UBT Test", section = "test", sortOrder = 0, route = "tests", imageUrl = ""),
            HomeCard(key = "demo_exam", title = "Demo Exam", section = "test", sortOrder = 1, route = "tests", imageUrl = ""),
            HomeCard(key = "batch", title = "Batch Exam", section = "test", sortOrder = 2, route = "tests", imageUrl = ""),
            HomeCard(key = "chapter_exam", title = "Chapter Exam", section = "test", sortOrder = 3, route = "tests", imageUrl = ""),
            HomeCard(key = "results", title = "Results", section = "test", sortOrder = 4, route = "results", imageUrl = ""),
            HomeCard(key = "all_books", title = "Books", section = "resources", sortOrder = 0, route = "books", imageUrl = ""),
            HomeCard(key = "question_bank", title = "Question Bank", section = "resources", sortOrder = 1, route = "questionbank", imageUrl = ""),
            HomeCard(key = "eye_vision", title = "Eye Vision", section = "resources", sortOrder = 2, route = "eyevision", imageUrl = ""),
            HomeCard(key = "join", title = "Join Live", section = "resources", sortOrder = 3, route = "join", imageUrl = "")
        )
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NavyBlue)
        }
        return
    }

    val sections = effectiveCards.groupBy { it.section }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ─── Hero card — navy blue with greeting + progress ─────────────
        item {
            Surface(
                color = NavyBlue,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shadowElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hello, $userName", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Let's learn Korean together", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Level ${if ((stats?.totalExamsTaken ?: 0) > 5) 2 else 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text("${stats?.totalExamsTaken ?: 0} exams", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text("${stats?.studyStreakDays ?: 0} day streak", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val progress = ((stats?.averageScore ?: 0.0) / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${String.format("%.0f", stats?.averageScore ?: 0.0)}% average score", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }

        // ─── Quick Access grid (4 colorful buttons) ──────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuickAccessBtn("UBT", NavyBlue, Icons.Default.School) { onNavigate(Screen.UbtTest) }
                QuickAccessBtn("Demo", SuccessGreen, Icons.Default.Quiz) { onNavigate(Screen.FreeExam) }
                QuickAccessBtn("Books", WarningOrange, Icons.Default.Book) { onNavigate(Screen.Books) }
                QuickAccessBtn("Eye Test", AccentPurple, Icons.Default.Visibility) { onNavigate(Screen.EyeVision) }
            }
        }

        // ─── Today's goal bar ────────────────────────────────────────────
        item {
            Surface(
                color = CardWhite,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp),
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Today's Goal", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${stats?.totalQuestionsAnswered ?: 0} answered", color = TextMid, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    val goalProgress = ((stats?.totalQuestionsAnswered ?: 0).coerceAtMost(20) / 20f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { goalProgress },
                        color = NavyBlue,
                        trackColor = DividerColor,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${stats?.totalQuestionsAnswered ?: 0} / 20 questions", color = TextMid, fontSize = 11.sp)
                }
            }
        }

        // ─── Section: Free Exams ─────────────────────────────────────────
        val testCards = sections["test"] ?: emptyList()
        if (testCards.isNotEmpty()) {
            item {
                Text("Free Exams", color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            val rows = testCards.chunked(2)
            rows.forEach { rowCards ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { card ->
                            HomeCardItem(theme, sound, card, modifier = Modifier.weight(1f)) {
                                val dest = when (card.key) {
                                    "ubt_test" -> Screen.UbtTest
                                    "demo_exam" -> Screen.FreeExam
                                    "batch" -> Screen.Batch
                                    "chapter_exam" -> Screen.TestList("chapter", "Chapter Exams")
                                    "results" -> Screen.Results
                                    else -> when (card.route) {
                                        "tests" -> Screen.Tests
                                        "results" -> Screen.Results
                                        else -> Screen.Tests
                                    }
                                }
                                onNavigate(dest)
                            }
                        }
                        if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ─── Section: Tools & Resources ──────────────────────────────────
        val resourceCards = sections["resources"] ?: emptyList()
        if (resourceCards.isNotEmpty()) {
            item {
                Text("Tools & Resources", color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            // Grid of resource cards
            val resRows = resourceCards.chunked(2)
            resRows.forEach { rowCards ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { card ->
                            HomeCardItem(theme, sound, card, modifier = Modifier.weight(1f)) {
                                val dest = when (card.key) {
                                    "all_books" -> Screen.Books
                                    "question_bank" -> Screen.QuestionBank
                                    "eye_vision" -> Screen.EyeVision
                                    "join" -> Screen.Join
                                    "course_video" -> Screen.CourseVideo
                                    "audio_lessons" -> Screen.AudioLessons
                                    "recorded_video" -> Screen.RecordedVideo
                                    else -> when (card.route) {
                                        "books" -> Screen.Books
                                        "videos" -> Screen.Videos
                                        "learn" -> Screen.Learn
                                        "live" -> Screen.Join
                                        "eyevision" -> Screen.EyeVision
                                        "questionbank" -> Screen.QuestionBank
                                        "join" -> Screen.Join
                                        else -> Screen.Tests
                                    }
                                }
                                onNavigate(dest)
                            }
                        }
                        if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ─── Footer ──────────────────────────────────────────────────────
        item {
            Text("DreamKorea SmartClass", color = TextLight, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

// ─── Quick Access Button ──────────────────────────────────────────────────────
@Composable
fun QuickAccessBtn(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quickScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { pressed = true; onClick() }.scale(scale).padding(4.dp),
    ) {
        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Home Card Item — professional card design ────────────────────────────────
@Composable
fun HomeCardItem(theme: AppTheme, sound: SoundManager, card: HomeCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().aspectRatio(0.85f).scale(scale),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
    ) {
        Column(modifier = Modifier.fillMaxSize().clickable { sound.click(); pressed = true; onClick() }) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(BgGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImageLoader(url = card.imageUrl ?: "", modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    card.title,
                    color = TextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ─── Screen header (back button + title) ──────────────────────────────────────
@Composable
fun ScreenHeader(theme: AppTheme, sound: SoundManager, title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { sound.click(); onBack() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TextDark, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = TextMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── Learn Screen ─────────────────────────────────────────────────────────────
@Composable
fun LearnScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { subjects = AppState.api.getSubjects().subjects } catch (_: Exception) {}
        finally { loading = false }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NavyBlue) }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(theme, sound, "Subjects", "Choose a subject to start learning", onBack) }
        if (subjects.isEmpty()) {
            item { EmptyState(theme, "No subjects yet", "Check back soon.", Icons.Default.School) }
        } else {
            itemsIndexed(subjects) { i, s ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = NavyBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.School, null, tint = NavyBlue, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(s.description ?: "", color = TextMid, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextLight, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Books Screen ─────────────────────────────────────────────────────────────
@Composable
fun BooksScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit, onBookClick: (Book) -> Unit = {}) {
    var books by remember {
        mutableStateOf<List<Book>>(AppState.getCachedNow<List<Book>>(AppState.KEY_BOOKS) ?: emptyList())
    }
    var loading by remember { mutableStateOf(books.isEmpty()) }

    LaunchedEffect(Unit) {
        try { books = AppState.getCachedBooks() } catch (_: Exception) {}
        finally { loading = false }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NavyBlue) }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(theme, sound, "Books", "Digital library for Korean learning", onBack) }
        if (books.isEmpty()) {
            item { EmptyState(theme, "No books yet", "Your teacher will add books here soon.", Icons.Default.Book) }
        } else {
            itemsIndexed(books) { i, b ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Surface(color = NavyBlue, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(54.dp, 72.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.title, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (!b.author.isNullOrBlank()) Text("by ${b.author}", color = TextMid, fontSize = 11.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!b.category.isNullOrBlank()) { InfoChip(theme, b.category, NavyBlue); Spacer(Modifier.width(6.dp)) }
                                    if (!b.level.isNullOrBlank()) { InfoChip(theme, b.level, AccentPurple); Spacer(Modifier.width(6.dp)) }
                                    if (b.pageCount != null) InfoChip(theme, "${b.pageCount}p", TextMid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Tests Screen ─────────────────────────────────────────────────────────────
@Composable
fun TestsScreen(theme: AppTheme, sound: SoundManager, filter: String = "all", title: String = "All Exams", onBack: () -> Unit, onStartExam: (String) -> Unit) {
    var tests by remember {
        mutableStateOf<List<TestItem>>(AppState.getCachedNow<List<TestItem>>(AppState.keyTests(filter)) ?: emptyList())
    }
    var loading by remember { mutableStateOf(tests.isEmpty()) }
    var error by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(filter, retryCount) {
        if (tests.isNotEmpty()) loading = false else loading = true
        error = ""
        try {
            if (retryCount > 0) AppState.invalidateCache(AppState.keyTests(filter))
            val result = kotlinx.coroutines.withTimeoutOrNull(20_000L) { AppState.getCachedTests(filter) }
            if (result != null) tests = result
            else if (tests.isEmpty()) error = "The request timed out. Check your internet and try again."
        } catch (e: retrofit2.HttpException) {
            if (tests.isEmpty()) error = when (e.code()) { 401 -> "Your session has expired. Please log out and sign in again." else -> "Could not load tests (HTTP ${e.code()})." }
        } catch (e: java.net.UnknownHostException) { if (tests.isEmpty()) error = "No internet connection." }
        catch (e: java.net.SocketTimeoutException) { if (tests.isEmpty()) error = "The request timed out." }
        catch (e: java.io.IOException) { if (tests.isEmpty()) error = "Network error." }
        catch (e: Exception) { if (tests.isEmpty()) error = "Unexpected error." }
        finally { loading = false }
    }

    if (loading) {
        Column(Modifier.fillMaxSize()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NavyBlue, trackColor = NavyBlue.copy(alpha = 0.1f))
            ScreenHeader(theme, sound, title, "Tap a test to start.", onBack)
        }
        return
    }

    if (error.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CloudOff, null, tint = TextLight, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Couldn't load tests", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(error, color = TextMid, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(10.dp)) { Text("Go back") }
                Button(onClick = { sound.click(); retryCount++ }, colors = ButtonDefaults.buttonColors(containerColor = NavyBlue), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Retry")
                }
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(theme, sound, title, if (tests.isEmpty()) "No tests available." else "Tap a test to start.", onBack) }
        if (tests.isEmpty()) {
            item { EmptyState(theme, "Nothing here yet", "Your teacher will add content to this section soon.", Icons.Default.Quiz) }
        } else {
            itemsIndexed(tests) { i, t -> AnimatedListItem(index = i, theme = theme) { TestCard(theme, sound, t, onClick = { onStartExam(t.id) }) } }
        }
    }
}

// ─── Test Card ────────────────────────────────────────────────────────────────
@Composable
fun TestCard(theme: AppTheme, sound: SoundManager, t: TestItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "testScale")
    val accentColor = if (t.isExam) AccentPink else NavyBlue
    Surface(
        color = CardWhite, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { sound.click(); pressed = true; onClick() },
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(6.dp).height(100.dp).background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f)))))
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title, color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Surface(color = accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(if (t.isExam) "EXAM" else "PRACTICE", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                if (!t.description.isNullOrBlank()) { Spacer(Modifier.height(4.dp)); Text(t.description, color = TextMid, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoChipWithIcon(theme, Icons.Default.Timer, "${t.durationMin} min", NavyBlue); Spacer(Modifier.width(6.dp))
                    InfoChipWithIcon(theme, Icons.Default.CheckCircle, "Pass ${t.passScore}%", SuccessGreen)
                    if (t.questionCount > 0) { Spacer(Modifier.width(6.dp)); InfoChipWithIcon(theme, Icons.Default.Quiz, "${t.questionCount} Q", AccentPurple) }
                    Spacer(Modifier.weight(1f))
                    Text("Start", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Videos Screen ────────────────────────────────────────────────────────────
@Composable
fun VideosScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    var videos by remember { mutableStateOf<List<VideoLesson>>(AppState.getCachedNow<List<VideoLesson>>(AppState.KEY_VIDEOS) ?: emptyList()) }
    var loading by remember { mutableStateOf(videos.isEmpty()) }

    LaunchedEffect(Unit) {
        try { videos = AppState.getCachedVideos() } catch (_: Exception) {}
        finally { loading = false }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NavyBlue) }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(theme, sound, "Video Lessons", "Watch and learn Korean", onBack) }
        if (videos.isEmpty()) {
            item { EmptyState(theme, "No videos yet", "Your teacher will add videos here soon.", Icons.Default.VideoLibrary) }
        } else {
            itemsIndexed(videos) { i, v ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(color = AccentPink, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(v.title, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InfoChipWithIcon(theme, Icons.Default.Timer, "${v.durationMin} min", NavyBlue); Spacer(Modifier.width(6.dp))
                                if (!v.level.isNullOrBlank()) { InfoChip(theme, v.level, AccentPurple); Spacer(Modifier.width(6.dp)) }
                                if (!v.category.isNullOrBlank()) { InfoChip(theme, v.category, TextMid); Spacer(Modifier.width(6.dp)) }
                                Spacer(Modifier.weight(1.dp))
                                Text("${v.views} views", color = TextMid, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Profile Screen ───────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(theme: AppTheme, sound: SoundManager, userName: String, onBack: () -> Unit, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { stats = AppState.api.getStats().stats } catch (_: Exception) {}
        finally { loading = false }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Profile", color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Profile header card
        item {
            Surface(color = CardWhite, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = NavyBlue, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(userName.take(2).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(userName, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Student", color = TextMid, fontSize = 12.sp)
                }
            }
        }

        // Stats grid
        item {
            Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Progress", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (loading) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(4) { SkeletonBox(theme, Modifier.size(60.dp, 40.dp)) } }
                    } else {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            ProfileStat(theme, "${stats?.totalExamsTaken ?: 0}", "Exams")
                            ProfileStat(theme, "${String.format("%.0f", stats?.averageScore ?: 0.0)}%", "Avg")
                            ProfileStat(theme, "${stats?.studyStreakDays ?: 0}", "Streak")
                            ProfileStat(theme, "${stats?.badgesEarned ?: 0}", "Badges")
                        }
                    }
                }
            }
        }

        // Account info
        item {
            Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Account", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    val userEmail = AppState.user?.email ?: ""
                    val userPhone = AppState.user?.phone ?: ""
                    if (userEmail.isNotEmpty()) { StatRow(theme, "Email", userEmail, Icons.Default.Email); Spacer(Modifier.height(6.dp)) }
                    if (userPhone.isNotEmpty()) { StatRow(theme, "Phone", userPhone, Icons.Default.Phone) }
                }
            }
        }

        // Contact info
        item {
            Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Contact DreamKorea", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    ContactRow(theme, Icons.Default.Phone, "023-591658", "tel:023591658"); Spacer(Modifier.height(8.dp))
                    ContactRow(theme, Icons.Default.Phone, "9852677658", "tel:9852677658"); Spacer(Modifier.height(8.dp))
                    ContactRow(theme, Icons.Default.Phone, "9765308000", "tel:9765308000"); Spacer(Modifier.height(10.dp))
                    Divider(color = DividerColor, thickness = 0.5.dp); Spacer(Modifier.height(10.dp))
                    ContactRow(theme, Icons.Default.LocationOn, "Krishithok Road, Birtamod, Jhapa", "geo:26.67,87.99?q=Krishithok+Road+Birtamod+Jhapa+Nepal"); Spacer(Modifier.height(10.dp))
                    Divider(color = DividerColor, thickness = 0.5.dp); Spacer(Modifier.height(10.dp))
                    ContactRow(theme, Icons.Default.Language, "DreamKorea SmartClass", "https://my-project-five-sepia.vercel.app")
                }
            }
        }

        // Logout
        item {
            Button(
                onClick = { sound.click(); scope.launch { try { AppState.api.logout() } catch (_: Exception) {}; AppState.clearSession(); onLogout() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Sign out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item { Text("DreamKorea SmartClass v2.1.0", color = TextLight, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────
@Composable
fun InfoChip(theme: AppTheme, text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun InfoChipWithIcon(theme: AppTheme, icon: ImageVector, text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(10.dp)); Spacer(Modifier.width(2.dp))
            Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatRow(theme: AppTheme, label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
            Text(label, color = TextMid, fontSize = 12.sp)
        }
        Text(value, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ContactRow(theme: AppTheme, icon: ImageVector, text: String, link: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
            context.startActivity(intent)
        }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
        Text(text, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = TextLight, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ProfileStat(theme: AppTheme, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NavyBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMid, fontSize = 10.sp)
    }
}

@Composable
fun SkeletonBox(theme: AppTheme, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).background(DividerColor))
}

@Composable
fun EmptyState(theme: AppTheme, title: String, body: String, icon: ImageVector) {
    Surface(color = CardWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = TextLight, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp))
            Text(title, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
            Text(body, color = TextMid, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AnimatedListItem(index: Int, theme: AppTheme, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 40L); visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300))) {
        content()
    }
}

// ─── Book Reader Screen ───────────────────────────────────────────────────────
@Composable
fun BookReaderScreen(theme: AppTheme, sound: SoundManager, book: Book, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgGray)) {
        ScreenHeader(theme, sound, book.title, "Tap 'Open PDF' to read", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(color = NavyBlue, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(120.dp, 160.dp), shadowElevation = 4.dp) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(book.title, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (!book.author.isNullOrBlank()) Text("by ${book.author}", color = TextMid, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            if (!book.pdfUrl.isNullOrBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = { sound.click(); val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(book.pdfUrl)); context.startActivity(intent) },
                    modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = NavyBlue), shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Open PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("No PDF available", color = TextMid, fontSize = 13.sp)
            }
        }
    }
}
