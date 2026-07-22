package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─── Colors ───────────────────────────────────────────────────────────────────

val BgDark = Color(0xFF0B1120)
val BgCard = Color(0xFF1E293B)
val Accent = Color(0xFF10B981)
val AccentDark = Color(0xFF0D9488)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// ─── Login Screen ─────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var contact by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var devCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.linearGradient(listOf(Accent, AccentDark)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("DK", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DreamKorea", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("SmartClass", color = TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))

        if (error.isNotEmpty()) {
            Text(error, color = Color(0xFFFCA5A5), fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (step == 1) {
            Text("Sign in", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enter your email or phone to receive a code.", color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Email or phone", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (contact.isBlank()) { error = "Enter email or phone"; return@Button }
                    loading = true; error = ""
                    scope.launch {
                        try {
                            val resp = app.dreamkorea.smartclass.data.AppState.api.requestOtp(
                                app.dreamkorea.smartclass.api.OtpRequest(contact)
                            )
                            devCode = resp.devCode ?: ""
                            step = 2
                        } catch (e: Exception) { error = "Failed: ${e.message}" }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = buttonColors(),
                enabled = !loading && contact.isNotBlank()
            ) { if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Send code") }
        }

        if (step == 2) {
            Text("Verify identity", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enter the 6-digit code sent to $contact", color = TextSecondary, fontSize = 13.sp)
            if (devCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Dev code: $devCode", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text("6-digit code", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (code.length < 6) { error = "Enter 6 digits"; return@Button }
                    loading = true; error = ""
                    scope.launch {
                        try {
                            val resp = app.dreamkorea.smartclass.data.AppState.api.verifyOtp(
                                app.dreamkorea.smartclass.api.VerifyRequest(contact, code)
                            )
                            if (resp.ok) {
                                // The session cookie is set by the server — we need to capture it
                                // For simplicity, store the user info
                                app.dreamkorea.smartclass.data.AppState.saveSession(
                                    "session_active", resp.user
                                )
                                onLoginSuccess()
                            } else { error = "Verification failed" }
                        } catch (e: Exception) { error = "Failed: ${e.message}" }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = buttonColors(),
                enabled = !loading && code.length >= 6
            ) { if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Verify & continue") }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { step = 1; error = ""; code = "" }) {
                Text("Back", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Accent,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Accent
)

@Composable
private fun buttonColors() = ButtonDefaults.buttonColors(
    containerColor = Accent,
    contentColor = Color.White
)
