package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.AppNotification
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

data class EyeVisionTest(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val category: String?
)

@Composable
fun EyeVisionScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tests by remember { mutableStateOf<List<EyeVisionTest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Use the eye-vision API
                val resp = AppState.api.getEyeVisionTests()
                tests = resp.tests.map {
                    EyeVisionTest(it.id, it.title, it.description, it.imageUrl, it.category)
                }
            } catch (e: Exception) {
                error = "Could not load eye vision tests"
            }
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Eye Vision Test", "Type what you see in each image", onBack)

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
            return
        }

        if (error.isNotEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error, color = theme.subText, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) {
                    Text("Go back")
                }
            }
            return
        }

        if (tests.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No eye vision tests yet", color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Your teacher will add tests here soon", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tests) { test ->
                EyeVisionTestCard(theme, sound, test)
            }
        }
    }
}

@Composable
fun EyeVisionTestCard(theme: AppTheme, sound: SoundManager, test: EyeVisionTest) {
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(test.title, color = theme.darkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!test.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(test.description!!, color = theme.subText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Image
            coil.compose.AsyncImage(
                model = if (test.imageUrl.startsWith("http")) test.imageUrl else "https://my-project-five-sepia.vercel.app${test.imageUrl}",
                contentDescription = "Eye vision test image",
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(12.dp))

            // Answer input
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it; result = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type what you see") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.darkText,
                    unfocusedTextColor = theme.darkText,
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.divider,
                    cursorColor = theme.primary,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Visibility, null, tint = theme.subText, modifier = Modifier.size(20.dp)) }
            )

            result?.let { r ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = if (r.startsWith("Correct") || r.startsWith("Passed")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(r, color = if (r.startsWith("Correct") || r.startsWith("Passed")) Color(0xFF34C759) else theme.errorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (answer.isBlank()) { sound.error(); return@Button }
                    checking = true; result = null
                    sound.click()
                },
                enabled = !checking && answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) {
                if (checking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check Answer", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
