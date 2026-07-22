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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.*
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

@Composable
fun MainScreen(userName: String, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = LightGray) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(color = White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DreamKorea", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Hi, $userName", color = SubText, fontSize = 13.sp)
                }
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

            // Bottom nav — clean white bar
            Surface(color = White, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                                tint = if (tab == i) Primary else SubText,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(item.label, fontSize = 10.sp, color = if (tab == i) Primary else SubText, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
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

// ─── Home ─────────────────────────────────────────────────────────────────────
@Composable
fun HomeTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = Primary, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Welcome back!", color = White.copy(0.85f), fontSize = 13.sp)
                    Text("Keep learning Korean", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("한국어를 배우세요 — let's begin today's lesson", color = White.copy(0.7f), fontSize = 12.sp)
                }
            }
        }
        item { Text("Quick Actions", color = DarkText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Take Test", Icons.Default.Quiz, Accent)
                QuickAction("Read Book", Icons.Default.Book, Primary)
                QuickAction("Watch", Icons.Default.VideoLibrary, SuccessGreen)
            }
        }
    }
}

@Composable
fun RowScope.QuickAction(label: String, icon: ImageVector, color: Color) {
    Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = DarkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RowScope.StatCard(label: String, value: String, color: Color) {
    Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = SubText, fontSize = 11.sp)
        }
    }
}

// ─── Learn ────────────────────────────────────────────────────────────────────
@Composable
fun LearnTab() {
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { subjects = AppState.api.getSubjects().subjects } catch (_: Exception) {} ; loading = false }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Subjects", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (loading) { item { Text("Loading...", color = SubText) } }
        else if (subjects.isEmpty()) { item { Text("No subjects available yet.", color = SubText, fontSize = 14.sp) } }
        else {
            items(subjects) { s ->
                Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = PrimaryLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Book, null, tint = Primary) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(s.name, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(s.description ?: "", color = SubText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ─── Books ────────────────────────────────────────────────────────────────────
@Composable
fun BooksTab() {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { books = AppState.api.getBooks().books } catch (_: Exception) {} ; loading = false }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Books", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (loading) { item { Text("Loading...", color = SubText) } }
        else if (books.isEmpty()) { item { Text("No books available yet.", color = SubText, fontSize = 14.sp) } }
        else {
            items(books) { b ->
                Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Surface(color = Primary, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(54.dp, 72.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Book, null, tint = White) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(b.title, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (!b.author.isNullOrBlank()) Text("by ${b.author}", color = SubText, fontSize = 12.sp)
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                if (!b.category.isNullOrBlank()) Text(b.category, color = Primary, fontSize = 10.sp)
                                if (!b.level.isNullOrBlank()) { Spacer(Modifier.width(8.dp)); Text(b.level, color = SubText, fontSize = 10.sp) }
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
fun TestsTab() {
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<TestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { tests = AppState.api.getTests().tests } catch (_: Exception) {} ; loading = false }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Tests & Exams", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (loading) { item { Text("Loading...", color = SubText) } }
        else if (tests.isEmpty()) { item { Text("No tests available yet.", color = SubText, fontSize = 14.sp) } }
        else {
            items(tests) { t ->
                Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(t.title, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (t.isExam) "EXAM" else "PRACTICE", color = if (t.isExam) ErrorRed else Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!t.description.isNullOrBlank()) { Spacer(Modifier.height(4.dp)); Text(t.description, color = SubText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text("${t.durationMin} min", color = SubText, fontSize = 11.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("Pass: ${t.passScore}%", color = SubText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Videos ───────────────────────────────────────────────────────────────────
@Composable
fun VideosTab() {
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoLesson>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch { try { videos = AppState.api.getVideoLessons().videos } catch (_: Exception) {} ; loading = false }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Video Lessons", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (loading) { item { Text("Loading...", color = SubText) } }
        else if (videos.isEmpty()) { item { Text("No videos available yet.", color = SubText, fontSize = 14.sp) } }
        else {
            items(videos) { v ->
                Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Surface(color = Color(0xFFFF0000), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.PlayCircle, null, tint = White, modifier = Modifier.size(36.dp)) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(v.title, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.padding(top = 2.dp)) {
                            Text("${v.durationMin} min", color = SubText, fontSize = 11.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("${v.views} views", color = SubText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Profile ──────────────────────────────────────────────────────────────────
@Composable
fun ProfileTab(userName: String, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { stats = AppState.api.getStats().stats } catch (_: Exception) {}
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // ─── Top right: Stats card ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.width(180.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Your Stats",
                        color = DarkText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (loading) {
                        Text("Loading…", color = SubText, fontSize = 11.sp)
                    } else {
                        StatsRow("Exams", "${stats?.totalExamsTaken ?: 0}")
                        Spacer(Modifier.height(4.dp))
                        StatsRow("Avg Score", "${String.format("%.0f", stats?.averageScore ?: 0.0)}%")
                        Spacer(Modifier.height(4.dp))
                        StatsRow("Streak", "${stats?.studyStreakDays ?: 0}d")
                        Spacer(Modifier.height(4.dp))
                        StatsRow("Books", "${stats?.booksRead ?: 0}")
                        Spacer(Modifier.height(4.dp))
                        StatsRow("Audio", "${stats?.audioLessonsCompleted ?: 0}")
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── Profile header ─────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(color = Primary, shape = RoundedCornerShape(36.dp), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(userName.take(2).uppercase(), color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(userName, color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Student", color = SubText, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        // ─── Stats grid (full width) ────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            ProfileStat("${stats?.totalExamsTaken ?: 0}", "Exams")
            ProfileStat("${String.format("%.0f", stats?.averageScore ?: 0.0)}%", "Avg")
            ProfileStat("${stats?.studyStreakDays ?: 0}", "Streak")
            ProfileStat("${stats?.badgesEarned ?: 0}", "Badges")
        }

        Spacer(Modifier.weight(1f))

        // ─── Account info ───────────────────────────────────────────────────
        val userEmail = AppState.user?.email ?: ""
        val userPhone = AppState.user?.phone ?: ""
        if (userEmail.isNotEmpty() || userPhone.isNotEmpty()) {
            Surface(color = White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (userEmail.isNotEmpty()) {
                        Text("Email", color = SubText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(userEmail, color = DarkText, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (userPhone.isNotEmpty()) {
                        Text("Phone", color = SubText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(userPhone, color = DarkText, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { scope.launch { try { AppState.api.logout() } catch (_: Exception) {} ; AppState.clearSession() ; onLogout() } },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        Text("DreamKorea SmartClass v1.0.0", color = SubText, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SubText, fontSize = 11.sp)
        Text(value, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SubText, fontSize = 11.sp)
    }
}
