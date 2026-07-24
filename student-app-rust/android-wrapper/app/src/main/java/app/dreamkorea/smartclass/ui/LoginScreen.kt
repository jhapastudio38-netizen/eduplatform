package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.OtpRequest
import app.dreamkorea.smartclass.api.VerifyRequest
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

// Color constants (Korean flag palette)
val White = Color(0xFFFFFFFF)
val DarkText = Color(0xFF1A1A2E)
val SubText = Color(0xFF6C757D)
val Primary = Color(0xFF003478)
val Accent = Color(0xFFCD2E3A)
val ErrorRed = Color(0xFFE53935)
val Divider = Color(0xFFE0E0E0)

// Email/phone validation helpers
private fun isEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()
private fun isValidPhone(s: String): Boolean {
    val digits = s.replace("\\D".toRegex(), "")
    return digits.length in 7..15
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sound = rememberSoundManager()

    // OTP-only flow: 1 = details, 2 = verify code
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    // Logo entrance animation
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        logoVisible = true
    }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ─── DreamKorea logo with animated entrance ────────────────────────
            Image(
                painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                contentDescription = "DreamKorea Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated title
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DreamKorea", color = DarkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Realize your dream now", color = SubText, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // Info message (green — for success like "OTP sent")
            AnimatedVisibility(
                visible = info.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(info, color = SuccessGreen, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Error message (red — for errors)
            AnimatedVisibility(
                visible = error.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ─── STEP 1: Name + Email + Phone ─────────────────────────────────
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
                },
                label = "stepTransition",
                modifier = Modifier.fillMaxWidth()
            ) { currentStep ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (currentStep == 1) {
                        Text(
                            "Welcome",
                            color = DarkText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Sign up with your email — we'll send you a verification code.",
                            color = SubText,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Full name *") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email *") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                            label = { Text("Phone number *") },
                            placeholder = { Text("+977 98XXXXXXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Returning user? Use the same email — we'll log you in.",
                            color = SubText,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))

                        // Gradient primary button
                        Button(
                            onClick = {
                                if (name.isBlank()) { sound.error(); error = "Please enter your name"; return@Button }
                                if (email.isBlank()) { sound.error(); error = "Please enter your email"; return@Button }
                                if (!isEmail(email)) { sound.error(); error = "Please enter a valid email address"; return@Button }
                                if (phone.isBlank()) { sound.error(); error = "Phone number is required"; return@Button }
                                if (!isValidPhone(phone)) { sound.error(); error = "Please enter a valid phone number (7-15 digits)"; return@Button }
                                loading = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        AppState.api.requestOtp(OtpRequest(email))
                                        sound.success()
                                        info = "Verification code sent to $email"
                                        step = 2
                                    } catch (e: UnknownHostException) {
                                        sound.error()
                                        error = "No internet connection. Please check your network."
                                    } catch (e: IOException) {
                                        sound.error()
                                        error = "Could not connect to server. Please check your internet."
                                    } catch (e: Exception) {
                                        sound.error()
                                        error = "Could not send code. Please try again."
                                    }
                                    loading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !loading && name.isNotBlank() && email.isNotBlank() && phone.isNotBlank()
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Send verification code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ─── STEP 2: OTP verification ───────────────────────────────
                    if (currentStep == 2) {
                        Text(
                            "Enter verification code",
                            color = DarkText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "We sent a 6-digit code to $email",
                            color = SubText,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("6-digit code") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Password, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (code.length < 6) { sound.error(); error = "Please enter all 6 digits"; return@Button }
                                loading = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        val resp = AppState.api.verifyOtp(
                                            VerifyRequest(
                                                contact = email, code = code, role = "STUDENT",
                                                name = name, email = email, phone = phone
                                            )
                                        )
                                        if (resp.ok) {
                                            sound.success()
                                            AppState.saveSession("logged_in", resp.user)
                                            // Invalidate cache so fresh data loads after login
                                            AppState.invalidateCache()
                                            onLoginSuccess()
                                        } else {
                                            sound.error()
                                            error = "Verification failed. Please try again."
                                        }
                                    } catch (e: UnknownHostException) {
                                        sound.error()
                                        error = "No internet connection. Please check your network."
                                    } catch (e: IOException) {
                                        sound.error()
                                        error = "Could not connect to server. Please try again."
                                    } catch (e: Exception) {
                                        sound.error()
                                        error = "Wrong code. Please check and try again."
                                    }
                                    loading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !loading && code.length >= 6
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Verify & continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            sound.click()
                            // Resend code
                            loading = true; error = ""; info = ""
                            scope.launch {
                                try {
                                    AppState.api.requestOtp(OtpRequest(email))
                                    sound.success()
                                    info = "New code sent to $email"
                                } catch (e: Exception) {
                                    sound.error()
                                    error = "Could not resend code."
                                }
                                loading = false
                            }
                        }) {
                            Text("Resend code", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { sound.click(); step = 1; error = ""; info = ""; code = "" }) {
                            Text("← Change details", color = SubText, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

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
