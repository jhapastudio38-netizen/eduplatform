package app.dreamkorea.smartclass.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.notification.StudyAlarmReceiver
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════
// DICTIONARY SCREEN — English↔Korean, Nepali↔Korean search
// Uses a built-in mini-dictionary (no API needed). Students can search
// common words in English, Nepali (Devanagari), or Korean (Hangul).
// ═══════════════════════════════════════════════════════════════════════════

data class DictEntry(
    val english: String,
    val korean: String,
    val koreanRomanized: String,
    val nepali: String,
)

// Mini dictionary — common Korean learning words
val MINI_DICT = listOf(
    DictEntry("hello", "안녕하세요", "annyeonghaseyo", "नमस्ते"),
    DictEntry("thank you", "감사합니다", "gamsahamnida", "धन्यवाद"),
    DictEntry("yes", "네", "ne", "हो"),
    DictEntry("no", "아니요", "aniyo", "होइन"),
    DictEntry("water", "물", "mul", "पानी"),
    DictEntry("food", "음식", "eumsik", "खाना"),
    DictEntry("book", "책", "chaek", "किताब"),
    DictEntry("school", "학교", "hakgyo", "विद्यालय"),
    DictEntry("student", "학생", "haksaeng", "विद्यार्थी"),
    DictEntry("teacher", "선생님", "seonsaengnim", "शिक्षक"),
    DictEntry("friend", "친구", "chingu", "साथी"),
    DictEntry("love", "사랑", "sarang", "माया"),
    DictEntry("family", "가족", "gajok", "परिवार"),
    DictEntry("mother", "어머니", "eomeoni", "आमा"),
    DictEntry("father", "아버지", "abeoji", "बुबा"),
    DictEntry("house", "집", "jip", "घर"),
    DictEntry("time", "시간", "sigan", "समय"),
    DictEntry("day", "날", "nal", "दिन"),
    DictEntry("night", "밤", "bam", "रात"),
    DictEntry("morning", "아침", "achim", "बिहान"),
    DictEntry("good", "좋아요", "joayo", "राम्रो"),
    DictEntry("bad", "나빠요", "nappayo", "नराम्रो"),
    DictEntry("big", "크다", "keuda", "ठूलो"),
    DictEntry("small", "작다", "jakda", "सानो"),
    DictEntry("one", "하나", "hana", "एक"),
    DictEntry("two", "둘", "dul", "दुई"),
    DictEntry("three", "셋", "set", "तीन"),
    DictEntry("four", "넷", "net", "चार"),
    DictEntry("five", "다섯", "daseot", "पाँच"),
    DictEntry("six", "여섯", "yeoseot", "छ"),
    DictEntry("seven", "일곱", "ilgop", "सात"),
    DictEntry("eight", "여덟", "yeodeol", "आठ"),
    DictEntry("nine", "아홉", "ahop", "नौ"),
    DictEntry("ten", "열", "yeol", "दस"),
    DictEntry("money", "돈", "don", "पैसा"),
    DictEntry("price", "가격", "gagyeok", "मूल्य"),
    DictEntry("name", "이름", "ireum", "नाम"),
    DictEntry("age", "나이", "nai", "उमेर"),
    DictEntry("man", "남자", "namja", "मान्छे"),
    DictEntry("woman", "여자", "yeoja", "आइमाई"),
    DictEntry("child", "아이", "ai", "बच्चा"),
    DictEntry("car", "자동차", "jadongcha", "गाडी"),
    DictEntry("bus", "버스", "beoseu", "बस"),
    DictEntry("train", "기차", "gicha", "रेल"),
    DictEntry("airport", "공항", "gonghang", "विमानस्थल"),
    DictEntry("hospital", "병원", "byeongwon", "अस्पताल"),
    DictEntry("doctor", "의사", "uisa", "डाक्टर"),
    DictEntry("market", "시장", "sijang", "बजार"),
    DictEntry("restaurant", "식당", "sikdang", "रेस्टुरेन्ट"),
    DictEntry("bathroom", "화장실", "hwajangsil", "शौचालय"),
    DictEntry("phone", "전화", "jeonhwa", "फोन"),
    DictEntry("computer", "컴퓨터", "keompyuteo", "कम्प्युटर"),
    DictEntry("internet", "인터넷", "inteonet", "इन्टरनेट"),
    DictEntry("study", "공부", "gongbu", "अध्ययन"),
    DictEntry("exam", "시험", "siheom", "परीक्षा"),
    DictEntry("question", "질문", "jilmun", "प्रश्न"),
    DictEntry("answer", "답", "dap", "उत्तर"),
    DictEntry("correct", "맞다", "matda", "सही"),
    DictEntry("wrong", "틀리다", "teullida", "गलत"),
    DictEntry("easy", "쉽다", "swipda", "सजिलो"),
    DictEntry("difficult", "어렵다", "eoryeopda", "गाह्रो"),
    DictEntry("fast", "빠르다", "ppareuda", "छिटो"),
    DictEntry("slow", "느리다", "neurida", "ढिलो"),
    DictEntry("hot", "뜨겁다", "tteugeopda", "तातो"),
    DictEntry("cold", "춥다", "chupda", "चिसो"),
    DictEntry("happy", "행복하다", "haengbokhada", "खुसी"),
    DictEntry("sad", "슬프다", "seulpeuda", "दुखी"),
    DictEntry("tired", "피곤하다", "pigonhada", "थकित"),
    DictEntry("hungry", "배고프다", "baegopeuda", "भोको"),
    DictEntry("thirsty", "목마르다", "mokmareuda", "तिर्खा"),
)

@Composable
fun DictionaryScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var searchLang by remember { mutableStateOf(0) } // 0=All, 1=English, 2=Korean, 3=Nepali

    val results = remember(query, searchLang) {
        if (query.isBlank()) MINI_DICT
        else {
            val q = query.trim().lowercase()
            MINI_DICT.filter { entry ->
                when (searchLang) {
                    1 -> entry.english.lowercase().contains(q)
                    2 -> entry.korean.contains(query.trim()) || entry.koreanRomanized.lowercase().contains(q)
                    3 -> entry.nepali.contains(query.trim())
                    else -> entry.english.lowercase().contains(q) ||
                        entry.korean.contains(query.trim()) ||
                        entry.koreanRomanized.lowercase().contains(q) ||
                        entry.nepali.contains(query.trim())
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Dictionary", "Search English / Korean / Nepali", onBack)

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Type a word…") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
        )

        // Language filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "English", "Korean", "Nepali").forEachIndexed { idx, label ->
                FilterChip(
                    selected = searchLang == idx,
                    onClick = { searchLang = idx },
                    label = { Text(label, fontSize = 12.sp) },
                )
            }
        }

        Text(
            "${results.size} words found",
            color = theme.subText,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { entry ->
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Korean word (main)
                        Text(
                            entry.korean,
                            color = theme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            entry.koreanRomanized,
                            color = theme.subText,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("English", color = theme.subText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(entry.english, color = theme.darkText, fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Nepali", color = theme.subText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(entry.nepali, color = theme.darkText, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GRAMMAR SCREEN — Korean grammar lessons reference
// ═══════════════════════════════════════════════════════════════════════════

data class GrammarLesson(
    val title: String,
    val korean: String,
    val explanation: String,
    val examples: List<String>,
)

val GRAMMAR_LESSONS = listOf(
    GrammarLesson(
        "Subject Particle - 은/는 (eun/neun)",
        "은 / 는",
        "Used to mark the topic/subject of a sentence. Use '은' after a consonant, '는' after a vowel.",
        listOf(
            "저는 학생이에요. (I am a student.)",
            "이것은 책이에요. (This is a book.)",
            "그 사람은 선생님이에요. (That person is a teacher.)",
        ),
    ),
    GrammarLesson(
        "Subject Particle - 이/가 (i/ga)",
        "이 / 가",
        "Used to mark the grammatical subject. Use '이' after a consonant, '가' after a vowel.",
        listOf(
            "책이 있어요. (There is a book.)",
            "누가 왔어요? (Who came?)",
            "비가 와요. (It's raining.)",
        ),
    ),
    GrammarLesson(
        "Object Particle - 을/를 (eul/reul)",
        "을 / 를",
        "Used to mark the object of a sentence. Use '을' after a consonant, '를' after a vowel.",
        listOf(
            "밥을 먹어요. (I eat food.)",
            "책을 읽어요. (I read a book.)",
            "물을 마셔요. (I drink water.)",
        ),
    ),
    GrammarLesson(
        "To Be - 이에요/예요 (ieyo/yeyo)",
        "이에요 / 예요",
        "Means 'is/am/are'. Use '이에요' after a consonant, '예요' after a vowel.",
        listOf(
            "학생이에요. (Is a student.)",
            "의사예요. (Is a doctor.)",
            "친구예요. (Is a friend.)",
        ),
    ),
    GrammarLesson(
        "To Have - 있어요 (isseoyo)",
        "있어요",
        "Means 'to have' or 'there is/are'.",
        listOf(
            "시간이 있어요. (I have time.)",
            "책이 있어요. (There is a book.)",
            "돈이 있어요? (Do you have money?)",
        ),
    ),
    GrammarLesson(
        "To Not Have - 없어요 (eopseoyo)",
        "없어요",
        "Means 'to not have' or 'there isn't/aren't'.",
        listOf(
            "시간이 없어요. (I don't have time.)",
            "책이 없어요. (There is no book.)",
            "돈이 없어요. (I don't have money.)",
        ),
    ),
    GrammarLesson(
        "Past Tense - 았/었어요 (at/eosseoyo)",
        "았/었어요",
        "Added to verb stems to make past tense. Use '았어요' after verbs with 'ㅏ/ㅗ', '었어요' otherwise.",
        listOf(
            "먹었어요. (Ate.)",
            "갔어요. (Went.)",
            "공부했어요. (Studied.)",
        ),
    ),
    GrammarLesson(
        "Future Tense - (으)ㄹ 거예요 ((eu)l geoyeyo)",
        "(으)ㄹ 거예요",
        "Added to verb stems to express future plans or intentions.",
        listOf(
            "갈 거예요. (Will go.)",
            "먹을 거예요. (Will eat.)",
            "공부할 거예요. (Will study.)",
        ),
    ),
    GrammarLesson(
        "Negative - 안/지 않아요 (an/ji anayo)",
        "안 / 지 않아요",
        "Two ways to make negatives: '안 + verb' or 'verb + 지 않아요'.",
        listOf(
            "안 가요. (Don't go.)",
            "가지 않아요. (Don't go.)",
            "안 먹어요. (Don't eat.)",
        ),
    ),
    GrammarLesson(
        "Want to - 고 싶어요 (go sipeoyo)",
        "고 싶어요",
        "Added to verb stems to express 'want to do something'.",
        listOf(
            "먹고 싶어요. (Want to eat.)",
            "가고 싶어요. (Want to go.)",
            "사고 싶어요. (Want to buy.)",
        ),
    ),
)

@Composable
fun GrammarScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Korean Grammar", "${GRAMMAR_LESSONS.size} lessons", onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(GRAMMAR_LESSONS.size) { idx ->
                val lesson = GRAMMAR_LESSONS[idx]
                val isExpanded = expanded == idx
                Surface(
                    color = theme.cardBg,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable { sound.click(); expanded = if (isExpanded) null else idx },
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = theme.primary.copy(alpha = 0.12f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("${idx + 1}", color = theme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lesson.title, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(lesson.korean, color = theme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = theme.subText,
                            )
                        }
                        if (isExpanded) {
                            Spacer(Modifier.height(10.dp))
                            Surface(color = theme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp)) {
                                Text(lesson.explanation, color = theme.darkText, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Examples:", color = theme.subText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            lesson.examples.forEach { ex ->
                                Text("  • $ex", color = theme.darkText, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ALARMS SCREEN — set study reminder alarms
// Uses Android AlarmManager to schedule notifications at a specific time.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AlarmsScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("Study Korean") }
    var alarmSet by remember { mutableStateOf(false) }
    var setAlarms by remember { mutableStateOf(listOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Study Alarms", "Set reminders to study", onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Time picker
            item {
                Surface(color = theme.cardBg, shape = RoundedCornerShape(14.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Set Study Time", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Hour
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { hour = (hour + 23) % 24 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                                Text(String.format("%02d", hour), color = theme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { hour = (hour + 1) % 24 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                            }
                            Text(":", color = theme.darkText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            // Minute
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { minute = (minute + 59) % 60 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                                Text(String.format("%02d", minute), color = theme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { minute = (minute + 1) % 60 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Label") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }

            // Set alarm button
            item {
                Button(
                    onClick = {
                        sound.swoosh()
                        try {
                            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val intent = Intent(context, StudyAlarmReceiver::class.java).apply {
                                putExtra("label", label)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                            val calendar = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                if (get(Calendar.HOUR_OF_DAY) < Calendar.getInstance().get(Calendar.HOUR_OF_DAY) ||
                                    (get(Calendar.HOUR_OF_DAY) == Calendar.getInstance().get(Calendar.HOUR_OF_DAY) &&
                                     get(Calendar.MINUTE) <= Calendar.getInstance().get(Calendar.MINUTE))) {
                                    add(Calendar.DAY_OF_YEAR, 1) // tomorrow
                                }
                            }
                            alarmMgr.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                AlarmManager.INTERVAL_DAY,
                                pendingIntent,
                            )
                            alarmSet = true
                            val timeStr = String.format("%02d:%02d", hour, minute)
                            setAlarms = setAlarms + "$timeStr — $label"
                        } catch (e: Exception) {
                            // Fallback — just show in the list
                            val timeStr = String.format("%02d:%02d", hour, minute)
                            setAlarms = setAlarms + "$timeStr — $label"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                ) {
                    Icon(Icons.Default.Alarm, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Set Daily Alarm", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Set alarms list
            if (setAlarms.isNotEmpty()) {
                item {
                    Text("Active Alarms", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                items(setAlarms) { alarmStr ->
                    Surface(
                        color = theme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, null, tint = theme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(alarmStr, color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Tips
            item {
                Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Study Tips", color = Color(0xFFF57C00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("• Study 20-30 minutes daily at the same time", color = Color(0xFF5D4037), fontSize = 12.sp)
                        Text("• Review vocabulary before sleeping", color = Color(0xFF5D4037), fontSize = 12.sp)
                        Text("• Practice speaking with a partner", color = Color(0xFF5D4037), fontSize = 12.sp)
                        Text("• Take mock exams weekly", color = Color(0xFF5D4037), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
