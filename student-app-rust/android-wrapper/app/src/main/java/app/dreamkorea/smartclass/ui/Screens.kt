package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

// ─── Main Screen with bottom nav ──────────────────────────────────────────────

@Composable
fun MainScreen(userName: String, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DreamKorea", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Hi, $userName", color = TextSecondary, fontSize = 14.sp)
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeTab()
                1 -> LearnTab()
                2 -> BooksTab()
                3 -> TestsTab()
                4 -> VideosTab()
                5 -> ProfileTab(userName, onLogout)
            }
        }

        // Bottom nav
        Row(
            modifier = Modifier.fillMaxWidth().background(BgCard).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            navItems().forEachIndexed { i, item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { tab = i }.padding(8.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (tab == i) Accent else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(item.label, fontSize = 10.sp, color = if (tab == i) Accent else TextSecondary)
                }
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

// ─── Home Tab ─────────────────────────────────────────────────────────────────

@Composable
fun HomeTab() {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<HomeResponse?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { stats = AppState.api.getSubjects().let { HomeResponse(0,0,0,7) } } catch (_: Exception) {}
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Hero card
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp).background(
                    Brush.linearGradient(listOf(Accent, AccentDark)),
                    RoundedCornerShape(20.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome back!", color = Color.White.copy(0.85f), fontSize = 13.sp)
                    Text("Keep learning Korean", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Text("Quick Stats", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Lessons", "0", Accent)
                StatCard("Tests", "0", Color(0xFFF59E0B))
                StatCard("Streak", "7", Color(0xFFFB923C))
            }
        }
    }
}

@Composable
fun RowScope.StatCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

// ─── Learn Tab ────────────────────────────────────────────────────────────────

@Composable
fun LearnTab() {
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { subjects = AppState.api.getSubjects().subjects } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Subjects", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (loading) {
            item { Text("Loading...", color = TextSecondary) }
        } else if (subjects.isEmpty()) {
            item { Text("No subjects yet.", color = TextSecondary) }
        } else {
            items(subjects) { subject ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(
                                Brush.linearGradient(listOf(Accent, AccentDark)),
                                RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Book, null, tint = Color.White) }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(subject.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(subject.description ?: "", color = TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ─── Books Tab ────────────────────────────────────────────────────────────────

@Composable
fun BooksTab() {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { books = AppState.api.getBooks().books } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Korean Learning Books", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (loading) {
            item { Text("Loading...", color = TextSecondary) }
        } else if (books.isEmpty()) {
            item { Text("No books available yet.", color = TextSecondary) }
        } else {
            items(books) { book ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        // Book cover placeholder
                        Box(
                            modifier = Modifier.size(60.dp, 80.dp).background(
                                Brush.linearGradient(listOf(Accent, AccentDark)),
                                RoundedCornerShape(8.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Book, null, tint = Color.White) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(book.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (!book.author.isNullOrBlank()) {
                                Text("by ${book.author}", color = TextSecondary, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                if (!book.category.isNullOrBlank()) {
                                    Text(" ${book.category} ", color = Accent, fontSize = 10.sp)
                                }
                                if (!book.level.isNullOrBlank()) {
                                    Text(" ${book.level} ", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Tests Tab ────────────────────────────────────────────────────────────────

@Composable
fun TestsTab() {
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<TestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { tests = AppState.api.getTests().tests } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Tests & Exams", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (loading) {
            item { Text("Loading...", color = TextSecondary) }
        } else if (tests.isEmpty()) {
            item { Text("No tests available yet.", color = TextSecondary) }
        } else {
            items(tests) { test ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(test.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (test.isExam) "EXAM" else "PRACTICE", color = if (test.isExam) Color(0xFFFCA5A5) else Color(0xFF93C5FD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!test.description.isNullOrBlank()) {
                            Text(test.description, color = TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text("${test.durationMin} min", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Pass: ${test.passScore}%", color = TextSecondary, fontSize = 11.sp)
                            if (test.questionCount > 0) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("${test.questionCount} Q", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Videos Tab ───────────────────────────────────────────────────────────────

@Composable
fun VideosTab() {
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoLesson>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { videos = AppState.api.getVideoLessons().videos } catch (_: Exception) {}
            loading = false
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Video Lessons", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (loading) {
            item { Text("Loading...", color = TextSecondary) }
        } else if (videos.isEmpty()) {
            item { Text("No videos available yet.", color = TextSecondary) }
        } else {
            items(videos) { video ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Thumbnail placeholder
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp).background(
                                Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
                                RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(video.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("${video.durationMin} min", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${video.views} views", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Profile Tab ──────────────────────────────────────────────────────────────

@Composable
fun ProfileTab(userName: String, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        // Avatar
        Box(
            modifier = Modifier.size(80.dp).background(
                Brush.linearGradient(listOf(Accent, AccentDark)),
                RoundedCornerShape(40.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(userName.take(2).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(userName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Student", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(32.dp))

        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("7", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Streak", color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("12", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Lessons", color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("3", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Badges", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout
        Button(
            onClick = {
                scope.launch {
                    try { AppState.api.logout() } catch (_: Exception) {}
                    AppState.clearSession()
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) { Text("Sign out") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DreamKorea SmartClass v1.0.0", color = TextSecondary, fontSize = 11.sp)
    }
}
