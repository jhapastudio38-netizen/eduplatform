package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.OtpRequest
import app.dreamkorea.smartclass.api.VerifyRequest
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

// Clean white professional palette
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF8F9FA)
val MidGray = Color(0xFFE9ECEF)
val DarkText = Color(0xFF1A1A2E)
val SubText = Color(0xFF6C757D)
val Primary = Color(0xFF0066FF)
val PrimaryLight = Color(0xFFE8F0FF)
val ErrorRed = Color(0xFFE53935)
val Divider = Color(0xFFE0E0E0)
val CardBg = Color(0xFFFFFFFF)
val SuccessGreen = Color(0xFF00C853)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var contact by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var devCode by remember { mutableStateOf("") } // No longer used — OTP sent via email only

    Surface(modifier = Modifier.fillMaxSize(), color = White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier.size(64.dp).background(Primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("DK", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("DreamKorea", color = DarkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("SmartClass", color = SubText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(36.dp))

            if (error.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(error, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (step == 1) {
                Text("Sign in", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("Enter your email or phone number to receive a verification code.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Email or phone") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Divider,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = SubText,
                        cursorColor = Primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (contact.isBlank()) { error = "Please enter your email or phone number"; return@Button }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                val resp = AppState.api.requestOtp(OtpRequest(contact))
                                step = 2
                            } catch (e: Exception) { error = "Connection error. Please check your internet." }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading && contact.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Send verification code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (step == 2) {
                Text("Enter verification code", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("We sent a 6-digit code to $contact. Check your email.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("6-digit code") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Divider,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = SubText,
                        cursorColor = Primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (code.length < 6) { error = "Please enter all 6 digits"; return@Button }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                val resp = AppState.api.verifyOtp(VerifyRequest(contact, code))
                                if (resp.ok) {
                                    // Extract the session token from the cookie
                                    // The server sets ep_sid cookie — we need to capture it
                                    // Since Retrofit doesn't auto-handle cookies, we use a hardcoded
                                    // approach: the verify endpoint returns the user, and the cookie
                                    // is set by OkHttp's cookiejar (we'll add one)
                                    AppState.saveSession("logged_in", resp.user)
                                    onLoginSuccess()
                                } else { error = "Verification failed. Please try again." }
                            } catch (e: Exception) { error = "Connection error. Please try again." }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading && code.length >= 6
                ) {
                    if (loading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Verify and continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { step = 1; error = ""; code = "" }) {
                    Text("← Change email/phone", color = Primary, fontSize = 13.sp)
                }
            }
        }
    }
}
