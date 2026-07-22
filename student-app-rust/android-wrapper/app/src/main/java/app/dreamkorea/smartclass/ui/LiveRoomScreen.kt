package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

/**
 * Live Room screen — student enters a 6-char code to join a live class.
 * Teacher/admin generates codes from the web admin panel.
 *
 * Note: For video calls, we use a web-based approach (the room's recordingUrl
 * or a third-party embed like Jitsi). Full WebRTC implementation would require
 * additional native libraries.
 */
@Composable
fun LiveRoomScreen(theme: AppTheme, onBack: () -> Unit) {
    val sound = rememberSoundManager()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var joinedRoom by remember { mutableStateOf<JoinedRoom?>(null) }

    if (joinedRoom != null) {
        LiveRoomView(theme = theme, room = joinedRoom!!, onLeave = {
            sound.click()
            joinedRoom = null
        })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background).padding(20.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { sound.click(); onBack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = theme.darkText)
            }
            Text("Live Classroom", color = theme.darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))

        // Join card
        Surface(color = theme.cardBg, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = theme.primary.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.VideoCall, null, tint = theme.primary, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Join a Live Class", color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Enter the 6-character code your teacher gave you", color = theme.subText, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                // Code input — 6 boxes style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0 until 6).forEach { i ->
                        val char = code.getOrNull(i)?.toString() ?: ""
                        Surface(
                            color = if (char.isNotEmpty()) theme.primary.copy(alpha = 0.1f) else theme.lightGray,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (char.isNotEmpty()) theme.primary else theme.divider
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { /* focus hidden input */ }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(char, color = theme.darkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (i < 5) Spacer(Modifier.width(6.dp))
                    }
                }
                // Hidden input that captures typing
                OutlinedTextField(
                    value = code,
                    onValueChange = { v ->
                        sound.click()
                        code = v.filter { it.isLetterOrDigit() }.uppercase().take(6)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Room code") },
                    placeholder = { Text("ABC123") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.darkText,
                        unfocusedTextColor = theme.darkText,
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.divider,
                        cursorColor = theme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = theme.errorRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (code.length < 6) { error = "Enter all 6 characters"; return@Button }
                        joining = true; error = ""
                        scope.launch {
                            try {
                                val res = AppState.api.joinLiveRoom(mapOf("roomCode" to code))
                                val room = res.room
                                if (room != null) {
                                    joinedRoom = JoinedRoom(
                                        code = code,
                                        title = room.title,
                                        description = room.description,
                                        hostName = "Teacher",
                                        attendeeCount = 0
                                    )
                                    sound.success()
                                } else {
                                    error = res.error ?: "Room not found"
                                    sound.error()
                                }
                            } catch (e: Exception) {
                                error = "Could not join. Check the code and try again."
                                sound.error()
                            }
                            joining = false
                        }
                    },
                    enabled = code.length == 6 && !joining,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    if (joining) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Login, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Join Class", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("How it works", color = theme.darkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        InfoCard(theme, "1.", "Teacher creates a live room", "Admin or teacher generates a 6-char code from the admin panel.")
        Spacer(Modifier.height(8.dp))
        InfoCard(theme, "2.", "Share the code", "Teacher shares the code with students (in class or via message).")
        Spacer(Modifier.height(8.dp))
        InfoCard(theme, "3.", "Students join here", "Enter the code above to join the live audio/video class.")
    }
}

data class JoinedRoom(
    val code: String,
    val title: String,
    val description: String?,
    val hostName: String,
    val attendeeCount: Int
)

@Composable
fun LiveRoomView(theme: AppTheme, room: JoinedRoom, onLeave: () -> Unit) {
    val sound = rememberSoundManager()
    var micOn by remember { mutableStateOf(false) }
    var cameraOn by remember { mutableStateOf(false) }
    var handRaised by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header
        Surface(color = theme.primary) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(room.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Host: ${room.hostName} • ${room.attendeeCount} in room", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                    Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(6.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Video area (placeholder — would be WebRTC in production)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = theme.primary, shape = CircleShape, modifier = Modifier.size(80.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(room.hostName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Audio + Video call", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                if (!cameraOn && !micOn) {
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Tap mic/camera below to participate",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                if (handRaised) {
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Color(0xFFFFA726), shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PanTool, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hand raised", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Controls bar
        Surface(color = theme.white, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mic toggle
                ControlButton(
                    theme = theme,
                    icon = if (micOn) Icons.Default.Mic else Icons.Default.MicOff,
                    label = if (micOn) "Mic On" else "Mic Off",
                    active = micOn,
                    onClick = { sound.click(); micOn = !micOn }
                )
                // Camera toggle
                ControlButton(
                    theme = theme,
                    icon = if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (cameraOn) "Cam On" else "Cam Off",
                    active = cameraOn,
                    onClick = { sound.click(); cameraOn = !cameraOn }
                )
                // Raise hand
                ControlButton(
                    theme = theme,
                    icon = Icons.Default.PanTool,
                    label = "Raise Hand",
                    active = handRaised,
                    onClick = { sound.click(); handRaised = !handRaised }
                )
                // Leave
                ControlButton(
                    theme = theme,
                    icon = Icons.Default.CallEnd,
                    label = "Leave",
                    active = false,
                    danger = true,
                    onClick = { sound.swoosh(); onLeave() }
                )
            }
        }
    }
}

@Composable
fun ControlButton(theme: AppTheme, icon: ImageVector, label: String, active: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = when {
                danger -> theme.errorRed
                active -> theme.primary
                else -> theme.lightGray
            },
            shape = CircleShape,
            modifier = Modifier.size(48.dp).clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, null, tint = if (danger || active) Color.White else theme.darkText, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = theme.subText, fontSize = 9.sp)
    }
}

@Composable
fun InfoCard(theme: AppTheme, num: String, title: String, body: String) {
    Surface(color = theme.cardBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(12.dp)) {
            Surface(color = theme.primary, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(num, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = theme.subText, fontSize = 11.sp)
            }
        }
    }
}
