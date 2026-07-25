package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.LiveSessionData
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

@Composable
fun JoinScreen(theme: AppTheme, sound: SoundManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<LiveSessionData?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        ScreenHeader(theme, sound, "Join Live Session", "Enter the code your teacher gave you", onBack)

        if (session != null) {
            // ─── Success: show meeting link ──────────────────────────────
            val s = session!!
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(s.title, color = theme.darkText, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (!s.hostName.isNullOrBlank()) {
                    Text("Hosted by ${s.hostName}", color = theme.subText, fontSize = 14.sp)
                }
                if (!s.description.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.description!!, color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(24.dp))
                if (!s.credentials.isNullOrBlank()) {
                    Surface(color = Color(0xFFF2F2F7), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Meeting Credentials", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.credentials!!, color = theme.darkText, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Button(
                    onClick = {
                        sound.click()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s.meetingUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Meeting Link", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { session = null; code = "" }) {
                    Text("Join another session", color = theme.primary, fontSize = 13.sp)
                }
            }
        } else {
            // ─── Code entry form ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.VideoCall,
                    null,
                    tint = theme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Enter Session Code", color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Ask your teacher for the 6-character code", color = theme.subText, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Join Code") },
                    placeholder = { Text("e.g. DK2024") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.darkText,
                        unfocusedTextColor = theme.darkText,
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.divider,
                        cursorColor = theme.primary,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Key, null, tint = theme.subText, modifier = Modifier.size(20.dp)) }
                )

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = theme.errorRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (code.length < 3) { sound.error(); error = "Enter a valid code"; return@Button }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                val resp = AppState.api.joinLiveSession(mapOf("joinCode" to code))
                                if (resp.ok && resp.session != null) {
                                    sound.success()
                                    session = resp.session
                                } else {
                                    sound.error()
                                    error = resp.error ?: "Invalid code or session has ended"
                                }
                            } catch (e: Exception) {
                                sound.error()
                                error = "Could not connect. Check your internet."
                            }
                            loading = false
                        }
                    },
                    enabled = !loading && code.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Join Session", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
