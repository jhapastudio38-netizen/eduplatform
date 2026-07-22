package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.OtpRequest
import app.dreamkorea.smartclass.api.VerifyRequest
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

// Clean white professional palette (matches the Korean flag theme)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF8F9FA)
val MidGray = Color(0xFFE9ECEF)
val DarkText = Color(0xFF1A1A2E)
val SubText = Color(0xFF6C757D)
val Primary = Color(0xFF003478)        // Korean flag blue
val PrimaryLight = Color(0xFFE8F0FF)
val Accent = Color(0xFFCD2E3A)         // Korean flag red
val ErrorRed = Color(0xFFE53935)
val Divider = Color(0xFFE0E0E0)
val CardBg = Color(0xFFFFFFFF)
val SuccessGreen = Color(0xFF00C853)

// Email validation helper
private fun isEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()
// Phone validation: digits only, 7-15 chars, optionally starts with +
private fun isValidPhone(s: String): Boolean {
    val digits = s.replace("\\D".toRegex(), "")
    return digits.length in 7..15
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Step 1: name + email (for OTP) + phone (mandatory)
    // Step 2: OTP code
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            // Taegeuk logo (Korean flag mark)
            Box(
                modifier = Modifier.size(64.dp).background(White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Accent, RoundedCornerShape(24.dp))
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Primary, RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp))
                )
                Text("한", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("DreamKorea", color = DarkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("SmartClass", color = SubText, fontSize = 12.sp, letterSpacing = 2.sp)
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

            // ─── Step 1: Name + Email + Phone ──────────────────────────────────
            if (step == 1) {
                Text("Sign up / Sign in", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("Enter your details. We'll send a verification code to your email.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                // Full name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Email (for OTP delivery)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Phone (mandatory)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Phone number *") },
                    placeholder = { Text("+977 98XXXXXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Phone is required for verification and account recovery.",
                    color = SubText, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) { error = "Please enter your name"; return@Button }
                        if (email.isBlank()) { error = "Please enter your email"; return@Button }
                        if (!isEmail(email)) { error = "Please enter a valid email address"; return@Button }
                        if (phone.isBlank()) { error = "Phone number is required"; return@Button }
                        if (!isValidPhone(phone)) { error = "Please enter a valid phone number (7-15 digits)"; return@Button }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                AppState.api.requestOtp(OtpRequest(email))
                                step = 2
                            } catch (e: Exception) { error = "Connection error. Please check your internet." }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading && name.isNotBlank() && email.isNotBlank() && phone.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Send verification code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ─── Step 2: OTP code ───────────────────────────────────────────────
            if (step == 2) {
                Text("Enter verification code", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("We sent a 6-digit code to $email. Check your email inbox.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("6-digit code") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
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
                                val resp = AppState.api.verifyOtp(
                                    VerifyRequest(
                                        contact = email,
                                        code = code,
                                        role = "STUDENT",
                                        name = name,
                                        email = email,
                                        phone = phone
                                    )
                                )
                                if (resp.ok) {
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
                    Text("← Change details", color = Primary, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Reusable text field colors
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DarkText,
    unfocusedTextColor = DarkText,
    focusedBorderColor = Primary,
    unfocusedBorderColor = Divider,
    focusedLabelColor = Primary,
    unfocusedLabelColor = SubText,
    cursorColor = Primary
)
