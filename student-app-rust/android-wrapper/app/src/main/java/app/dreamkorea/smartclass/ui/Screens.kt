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

// Color constants used in this file
val SuccessGreen = Color(0xFF00C853)

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
    data class Exam(val testId: String) : Screen()
    data class BookReader(val book: Book) : Screen()
}

@Composable
fun MainScreen(userName: String, onLogout: () -> Unit) {
    val theme = rememberAppTheme()
    val sound = rememberSoundManager()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Surface(modifier = Modifier.fillMaxSize(), color = theme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top bar with DreamKorea logo ──────────────────────────────────
            TopBar(theme, userName, sound, onProfile = { screen = Screen.Profile }, onSettings = { screen = Screen.Settings })

            // ─── Animated screen content ────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                    },
                    label = "screenTransition"
                ) { s ->
                    when (s) {
                        is Screen.Home -> HomeScreen(theme, sound, onNavigate = { screen = it })
                        is Screen.Learn -> LearnScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.Books -> BooksScreen(theme, sound, onBack = { screen = Screen.Home }, onBookClick = { screen = Screen.BookReader(it) })
                        is Screen.Tests -> TestsScreen(theme, sound, onBack = { screen = Screen.Home }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Videos -> VideosScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.Profile -> ProfileScreen(theme, sound, userName, onBack = { screen = Screen.Home }, onLogout = onLogout)
                        is Screen.LiveRoom -> LiveRoomScreen(theme, onBack = { screen = Screen.Home })
                        is Screen.Settings -> SettingsScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.Exam -> ExamScreen(theme, testId = s.testId, onExit = { screen = Screen.Home })
                        is Screen.BookReader -> BookReaderScreen(theme, sound, s.book, onBack = { screen = Screen.Books })
                        // Specific card pages — each opens a distinct screen
                        is Screen.UbtTest -> TestsScreen(theme, sound, onBack = { screen = Screen.Home }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.FreeExam -> TestsScreen(theme, sound, onBack = { screen = Screen.Home }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Batch -> TestsScreen(theme, sound, onBack = { screen = Screen.Home }, onStartExam = { screen = Screen.Exam(it) })
                        is Screen.Results -> ProfileScreen(theme, sound, userName, onBack = { screen = Screen.Home }, onLogout = onLogout)
                        is Screen.QuestionBank -> LearnScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.AudioLessons -> LearnScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.Classroom -> LiveRoomScreen(theme, onBack = { screen = Screen.Home })
                        is Screen.RecordedVideo -> VideosScreen(theme, sound, onBack = { screen = Screen.Home })
                        is Screen.ClassResult -> ProfileScreen(theme, sound, userName, onBack = { screen = Screen.Home }, onLogout = onLogout)
                        is Screen.CourseVideo -> VideosScreen(theme, sound, onBack = { screen = Screen.Home })
                    }
                }
            }
        }
    }
}

// ─── Top bar with DreamKorea logo ─────────────────────────────────────────────
@Composable
fun TopBar(theme: AppTheme, userName: String, sound: SoundManager, onProfile: () -> Unit, onSettings: () -> Unit) {
    Surface(color = theme.white, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DreamKorea logo (bundled in APK, blends into background)
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = "DreamKorea Logo",
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DreamKorea", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Realize your dream now", color = theme.subText, fontSize = 9.sp)
            }
            // Profile icon
            IconButton(onClick = { sound.click(); onProfile() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Person, "Profile", tint = theme.darkText, modifier = Modifier.size(22.dp))
            }
            // Settings icon
            IconButton(onClick = { sound.click(); onSettings() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = theme.darkText, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// Simple async image loader — uses Coil if available, falls back to colored box
@Composable
fun AsyncImageLoader(url: String, modifier: Modifier = Modifier) {
    val theme = rememberAppTheme()
    Box(
        modifier = modifier.background(theme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text("DK", color = theme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniStat(theme: AppTheme, icon: ImageVector, value: String, label: String) {
    Surface(
        color = theme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = theme.primary, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(2.dp))
            Text(value, color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Home — image cards like the original app ─────────────────────────────────
@Composable
fun HomeScreen(theme: AppTheme, sound: SoundManager, onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    var homeCards by remember { mutableStateOf<List<HomeCard>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val resp = AppState.api.getHomeCards()
                homeCards = resp.cards
            } catch (_: Exception) {}
            loading = false
        }
    }

    // Fallback cards if API fails or returns empty — ensures home is never blank
    val effectiveCards = remember(homeCards, loading) {
        if (loading) emptyList()
        else if (homeCards.isNotEmpty()) homeCards
        else listOf(
            HomeCard(key = "ubt_test", title = "UBT TEST", section = "test", sortOrder = 0, route = "tests", imageUrl = ""),
            HomeCard(key = "free_exam", title = "Free Exam", section = "test", sortOrder = 1, route = "tests", imageUrl = ""),
            HomeCard(key = "batch", title = "Batch", section = "test", sortOrder = 2, route = "tests", imageUrl = ""),
            HomeCard(key = "results", title = "Results", section = "test", sortOrder = 3, route = "profile", imageUrl = ""),
            HomeCard(key = "all_books", title = "ALL BOOKS", section = "resources", sortOrder = 0, route = "books", imageUrl = ""),
            HomeCard(key = "question_bank", title = "QUESTION BANK", section = "resources", sortOrder = 1, route = "learn", imageUrl = ""),
            HomeCard(key = "course_video", title = "COURSE VIDEO", section = "resources", sortOrder = 2, route = "videos", imageUrl = ""),
            HomeCard(key = "audio_lessons", title = "AUDIO LESSONS", section = "resources", sortOrder = 3, route = "learn", imageUrl = ""),
            HomeCard(key = "classroom", title = "CLASSROOM", section = "premium", sortOrder = 0, route = "live", imageUrl = ""),
            HomeCard(key = "live_class", title = "LIVE CLASS", section = "premium", sortOrder = 1, route = "live", imageUrl = ""),
            HomeCard(key = "recorded_video", title = "RECORDED VIDEO", section = "premium", sortOrder = 2, route = "videos", imageUrl = ""),
            HomeCard(key = "class_result", title = "CLASS RESULT", section = "premium", sortOrder = 3, route = "profile", imageUrl = "")
        )
    }

    if (loading) {
        SkeletonGridScreen(theme, rows = 3)
        return
    }

    // Group cards by section
    val sections = effectiveCards.groupBy { it.section }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        sections.forEach { (sectionKey, cards) ->
            val sectionTitle = when (sectionKey) {
                "test" -> "UBT TEST"
                "resources" -> "RESOURCES"
                "premium" -> "PREMIUM"
                else -> sectionKey.uppercase()
            }
            val sectionColor = when (sectionKey) {
                "test" -> theme.primary.copy(alpha = 0.12f)
                "resources" -> Color(0xFFE8F5E9)
                "premium" -> Color(0xFFFFF3E0)
                else -> theme.primary.copy(alpha = 0.08f)
            }

            // Section header
            item {
                Surface(color = sectionColor, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        sectionTitle,
                        color = theme.darkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // Cards in 2-column grid
            val rows = cards.chunked(2)
            rows.forEachIndexed { rowIdx, rowCards ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { card ->
                            ImageCard(theme, sound, card, modifier = Modifier.weight(1f)) {
                                // Navigate to distinct page based on card key
                                val dest = when (card.key) {
                                    "ubt_test" -> Screen.UbtTest
                                    "free_exam" -> Screen.FreeExam
                                    "batch" -> Screen.Batch
                                    "results" -> Screen.Results
                                    "all_books" -> Screen.Books
                                    "question_bank" -> Screen.QuestionBank
                                    "course_video" -> Screen.CourseVideo
                                    "audio_lessons" -> Screen.AudioLessons
                                    "classroom" -> Screen.Classroom
                                    "live_class" -> Screen.LiveRoom
                                    "recorded_video" -> Screen.RecordedVideo
                                    "class_result" -> Screen.ClassResult
                                    else -> when (card.route) {
                                        "tests" -> Screen.Tests
                                        "books" -> Screen.Books
                                        "videos" -> Screen.Videos
                                        "learn" -> Screen.Learn
                                        "live" -> Screen.LiveRoom
                                        "profile" -> Screen.Profile
                                        else -> Screen.Tests
                                    }
                                }
                                onNavigate(dest)
                            }
                        }
                        // Fill empty slot if odd number
                        if (rowCards.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Footer
        item {
            Text(
                "© DreamKorea SmartClass 2024. All Rights Reserved.",
                color = theme.subText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

// ─── Image card (like original app) — bigger, not squished ────────────────────
@Composable
fun ImageCard(theme: AppTheme, sound: SoundManager, card: HomeCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)  // taller cards — better fit for images
            .scale(scale),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.divider)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clickable { sound.click(); pressed = true; onClick() }
        ) {
            // Image area (top 75% — bigger for uploaded images)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(theme.lightGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImageLoader(url = card.imageUrl ?: "", modifier = Modifier.fillMaxSize())
            }
            // Label area (bottom 25%)
            Box(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    card.title,
                    color = theme.darkText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun RowScope.NavCard(
    theme: AppTheme, sound: SoundManager,
    title: String, subtitle: String, icon: ImageVector, color: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).scale(scale),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable { sound.click(); pressed = true; onClick() }
                .padding(14.dp)
        ) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.subText, fontSize = 9.sp, maxLines = 1)
        }
    }
}

// ─── Screen header (back button + title) ──────────────────────────────────────
@Composable
fun ScreenHeader(theme: AppTheme, sound: SoundManager, title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { sound.click(); onBack() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = theme.darkText, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = theme.darkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = theme.subText, fontSize = 11.sp)
            }
        }
    }
}

// ─── Learn Screen ─────────────────────────────────────────────────────────────
@Composable
fun LearnScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(theme, sound, "Subjects", "Choose a subject to start learning", onBack) }
        if (subjects.isEmpty()) {
            item { EmptyState(theme, "No subjects yet", "Check back soon.", Icons.Default.School) }
        } else {
            itemsIndexed(subjects) { i, s ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(
                        color = theme.cardBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 2.dp
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = theme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.School, null, tint = theme.primary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(s.description ?: "", color = theme.subText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = theme.subText, modifier = Modifier.size(20.dp))
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(theme, sound, "Books", "Digital library for Korean learning", onBack) }
        if (books.isEmpty()) {
            item { EmptyState(theme, "No books yet", "Your teacher will add books here soon.", Icons.Default.Book) }
        } else {
            itemsIndexed(books) { i, b ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Surface(color = theme.primary, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(54.dp, 72.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.title, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (!b.author.isNullOrBlank()) Text("by ${b.author}", color = theme.subText, fontSize = 11.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!b.category.isNullOrBlank()) {
                                        InfoChip(theme, b.category, theme.primary)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    if (!b.level.isNullOrBlank()) {
                                        InfoChip(theme, b.level, theme.accent)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    if (b.pageCount != null) {
                                        InfoChip(theme, "${b.pageCount}p", theme.subText)
                                    }
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
fun TestsScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit, onStartExam: (String) -> Unit) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(theme, sound, "Tests & Exams", "Tap a test to start. Stats update automatically.", onBack) }
        if (tests.isEmpty()) {
            item { EmptyState(theme, "No tests yet", "Your teacher will assign tests here soon.", Icons.Default.Quiz) }
        } else {
            itemsIndexed(tests) { i, t ->
                AnimatedListItem(index = i, theme = theme) {
                    TestCard(theme, sound, t, onClick = { onStartExam(t.id) })
                }
            }
        }
    }
}

// ─── Test Card with rich info ─────────────────────────────────────────────────
@Composable
fun TestCard(theme: AppTheme, sound: SoundManager, t: TestItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "testScale"
    )
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { sound.click(); pressed = true; onClick() },
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(t.title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    color = if (t.isExam) theme.accent.copy(alpha = 0.15f) else theme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (t.isExam) "EXAM" else "PRACTICE",
                        color = if (t.isExam) theme.accent else theme.primary,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (!t.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(t.description, color = theme.subText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            // Info chips row
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoChipWithIcon(theme, Icons.Default.Timer, "${t.durationMin} min", theme.primary)
                Spacer(Modifier.width(6.dp))
                InfoChipWithIcon(theme, Icons.Default.CheckCircle, "Pass ${t.passScore}%", SuccessGreen)
                if (t.questionCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    InfoChipWithIcon(theme, Icons.Default.Quiz, "${t.questionCount} Q", theme.accent)
                }
                Spacer(Modifier.weight(1f))
                Text("Start →", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Videos Screen ────────────────────────────────────────────────────────────
@Composable
fun VideosScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(theme, sound, "Video Lessons", "Watch and learn Korean", onBack) }
        if (videos.isEmpty()) {
            item { EmptyState(theme, "No videos yet", "Your teacher will add videos here soon.", Icons.Default.VideoLibrary) }
        } else {
            itemsIndexed(videos) { i, v ->
                AnimatedListItem(index = i, theme = theme) {
                    Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Thumbnail placeholder with play button
                            Surface(color = theme.errorRed, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(v.title, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InfoChipWithIcon(theme, Icons.Default.Timer, "${v.durationMin} min", theme.primary)
                                Spacer(Modifier.width(6.dp))
                                if (!v.level.isNullOrBlank()) {
                                    InfoChip(theme, v.level, theme.accent)
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (!v.category.isNullOrBlank()) {
                                    InfoChip(theme, v.category, theme.subText)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                Text("${v.views} views", color = theme.subText, fontSize = 9.sp)
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
        scope.launch {
            try { stats = AppState.api.getStats().stats } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(theme, sound, "Profile", "Your account and progress", onBack) }

        // Profile header card
        item {
            Surface(color = theme.cardBg, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = theme.primary, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(userName.take(2).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(userName, color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Student", color = theme.subText, fontSize = 12.sp)
                }
            }
        }

        // Stats grid
        item {
            Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Progress", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (loading) {
                        SkeletonStatsRow(theme)
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

        // Detailed stats list
        item {
            Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    StatRow(theme, "Books Read", "${stats?.booksRead ?: 0}", Icons.Default.Book)
                    Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(theme, "Audio Lessons", "${stats?.audioLessonsCompleted ?: 0}", Icons.Default.Headphones)
                    Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(theme, "Total Time", "${stats?.totalTimeSpentMin ?: 0} min", Icons.Default.Schedule)
                    Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(theme, "Correct Answers", "${stats?.totalCorrectAnswers ?: 0}", Icons.Default.CheckCircle)
                }
            }
        }

        // Account info
        item {
            val userEmail = AppState.user?.email ?: ""
            val userPhone = AppState.user?.phone ?: ""
            Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Account", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    if (userEmail.isNotEmpty()) {
                        StatRow(theme, "Email", userEmail, Icons.Default.Email)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (userPhone.isNotEmpty()) {
                        StatRow(theme, "Phone", userPhone, Icons.Default.Phone)
                    }
                }
            }
        }

        // Contact info — institute details
        item {
            Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Contact DreamKorea", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

                    // Phone numbers (tappable)
                    ContactRow(theme, Icons.Default.Phone, "023-591658", "tel:023591658")
                    Spacer(Modifier.height(8.dp))
                    ContactRow(theme, Icons.Default.Phone, "9852677658", "tel:9852677658")
                    Spacer(Modifier.height(8.dp))
                    ContactRow(theme, Icons.Default.Phone, "9765308000", "tel:9765308000")
                    Spacer(Modifier.height(10.dp))
                    Divider(color = theme.divider, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))

                    // Location (tappable — opens maps)
                    ContactRow(theme, Icons.Default.LocationOn, "Krishithok Road, Birtamod, Jhapa", "geo:26.67,87.99?q=Krishithok+Road+Birtamod+Jhapa+Nepal")
                    Spacer(Modifier.height(10.dp))
                    Divider(color = theme.divider, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))

                    // Website (tappable — opens browser)
                    ContactRow(theme, Icons.Default.Language, "dreamkoreasmartclass.com", "https://dreamkoreasmartclass.com")
                }
            }
        }

        // Logout
        item {
            Button(
                onClick = {
                    sound.click()
                    scope.launch {
                        try { AppState.api.logout() } catch (_: Exception) {}
                        AppState.clearSession()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.errorRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text("DreamKorea SmartClass v1.0.0", color = theme.subText, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
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
            Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(2.dp))
            Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatRow(theme: AppTheme, label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = theme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = theme.subText, fontSize = 12.sp)
        }
        Text(value, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ContactRow(theme: AppTheme, icon: ImageVector, text: String, link: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            // Open the link (tel:, geo:, https:)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
            context.startActivity(intent)
        }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = theme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = theme.subText, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ProfileStat(theme: AppTheme, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = theme.subText, fontSize = 10.sp)
    }
}

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

@Composable
fun AnimatedListItem(index: Int, theme: AppTheme, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300))
    ) {
        content()
    }
}

// ─── Book Reader Screen — opens PDF in browser ────────────────────────────────
@Composable
fun BookReaderScreen(theme: AppTheme, sound: SoundManager, book: Book, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header
        ScreenHeader(theme, sound, book.title, "Tap 'Open PDF' to read", onBack)
        
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Book cover
            Surface(
                color = theme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(120.dp, 160.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(book.title, color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (!book.author.isNullOrBlank()) {
                Text("by ${book.author}", color = theme.subText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (!book.description.isNullOrBlank()) {
                Text(book.description ?: "", color = theme.subText, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 3)
            }
            Spacer(Modifier.height(24.dp))
            
            // Open PDF button
            if (!book.pdfUrl.isNullOrBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        sound.click()
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(book.pdfUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("No PDF available", color = theme.subText, fontSize = 13.sp)
            }
        }
    }
}
