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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Canvas
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

// Email/phone validation helpers
private fun isEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()
private fun isPhone(s: String) = s.replace("\\D".toRegex(), "").length in 7..15

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Step 1: contact (email/phone) + name
    // Step 2: OTP code
    // Step 3: supplementary info (phone if email OTP, email if phone OTP)
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var suppContact by remember { mutableStateOf("") } // the other contact field
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Taegeuk logo (Korean flag mark)
            Box(
                modifier = Modifier.size(64.dp).background(White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val r = w.coerceAtMost(h) / 2f - 4f
                    // Outer circle
                    drawCircle(color = Primary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f), radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    // Taegeuk — top half red, bottom half blue (simplified)
                    drawArc(
                        color = Accent,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                    )
                    drawArc(
                        color = Primary,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                    )
                }
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

            // ─── Step 1: Contact + Name ─────────────────────────────────────────
            if (step == 1) {
                Text("Sign up / Sign in", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("Enter your name and email or phone number to receive a verification code.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
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
                Spacer(modifier = Modifier.height(12.dp))

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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPhone(contact)) KeyboardType.Phone else KeyboardType.Email
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) { error = "Please enter your name"; return@Button }
                        if (contact.isBlank()) { error = "Please enter your email or phone number"; return@Button }
                        if (!isEmail(contact) && !isPhone(contact)) {
                            error = "Please enter a valid email or phone number"; return@Button
                        }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                AppState.api.requestOtp(OtpRequest(contact))
                                step = 2
                            } catch (e: Exception) { error = "Connection error. Please check your internet." }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading && name.isNotBlank() && contact.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Send verification code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ─── Step 2: OTP code ───────────────────────────────────────────────
            if (step == 2) {
                Text("Enter verification code", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("We sent a 6-digit code to $contact. Check your email inbox.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
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
                                val resp = AppState.api.verifyOtp(
                                    VerifyRequest(contact = contact, code = code, role = "STUDENT", name = name)
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
                    Text("← Change email/phone", color = Primary, fontSize = 13.sp)
                }
            }
        }
    }
}
