package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.api.OtpRequest
import app.dreamkorea.smartclass.api.VerifyRequest
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

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
    val sound = rememberSoundManager()
    val context = LocalContext.current
    // 3 login modes: OTP_SIGNUP (new user), OTP_LOGIN (returning user via OTP), PASSWORD (email+pass)
    var mode by remember { mutableStateOf("OTP_SIGNUP") }

    // OTP flow state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1=details, 2=OTP, 3=password setup
    // Password login state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            // Taegeuk logo
            Box(
                modifier = Modifier.size(64.dp).background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(48.dp).background(Accent, RoundedCornerShape(24.dp)))
                Box(modifier = Modifier.size(48.dp).background(Primary, RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)))
                Text("한", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("DreamKorea", color = DarkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("SmartClass", color = SubText, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(36.dp))

            // Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = mode == "OTP_SIGNUP",
                    onClick = { sound.click(); mode = "OTP_SIGNUP"; step = 1; error = "" },
                    label = { Text("Sign up", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == "PASSWORD",
                    onClick = { sound.click(); mode = "PASSWORD"; error = "" },
                    label = { Text("Sign in", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Error display
            AnimatedVisibility(
                visible = error.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ─── PASSWORD LOGIN MODE ───────────────────────────────────────────
            if (mode == "PASSWORD") {
                Text("Welcome back", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("Sign in with your email and password", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { sound.click(); passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = SubText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (loginEmail.isBlank()) { error = "Please enter your email"; return@Button }
                        if (!isEmail(loginEmail)) { error = "Please enter a valid email address"; return@Button }
                        if (loginPassword.isBlank()) { error = "Please enter your password"; return@Button }
                        loading = true; error = ""
                        scope.launch {
                            try {
                                val res = AppState.api.loginCredentials(
                                    mapOf("username" to loginEmail, "password" to loginPassword)
                                )
                                if (res.ok) {
                                    sound.success()
                                    AppState.saveSession("logged_in", res.user)
                                    onLoginSuccess()
                                } else {
                                    sound.error()
                                    error = res.error ?: "Invalid email or password"
                                }
                            } catch (e: UnknownHostException) {
                                sound.error()
                                error = "No internet connection. Please check your network."
                            } catch (e: IOException) {
                                sound.error()
                                error = "Network error. Please check your internet and try again."
                            } catch (e: Exception) {
                                sound.error()
                                error = "Could not connect to server. Try again."
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading && loginEmail.isNotBlank() && loginPassword.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Sign in", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { sound.click(); mode = "OTP_SIGNUP"; step = 1; error = "" }) {
                    Text("Don't have an account? Sign up", color = Primary, fontSize = 12.sp)
                }
            }

            // ─── OTP SIGNUP MODE ───────────────────────────────────────────────
            if (mode == "OTP_SIGNUP") {
                // Step 1: Name + Email + Phone
                if (step == 1) {
                    Text("Create your account", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("We'll send a verification code to your email.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Full name *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
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
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Phone is required for verification.", color = SubText, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) { sound.error(); error = "Please enter your name"; return@Button }
                            if (email.isBlank()) { sound.error(); error = "Please enter your email"; return@Button }
                            if (!isEmail(email)) { sound.error(); error = "Please enter a valid email address"; return@Button }
                            if (phone.isBlank()) { sound.error(); error = "Phone number is required"; return@Button }
                            if (!isValidPhone(phone)) { sound.error(); error = "Please enter a valid phone number (7-15 digits)"; return@Button }
                            loading = true; error = ""
                            scope.launch {
                                try {
                                    AppState.api.requestOtp(OtpRequest(email))
                                    sound.success()
                                    step = 2
                                } catch (e: UnknownHostException) {
                                    sound.error()
                                    error = "No internet connection. Please check your network."
                                } catch (e: IOException) {
                                    sound.error()
                                    error = "Network error. Please check your internet and try again."
                                } catch (e: Exception) {
                                    sound.error()
                                    error = "Could not send OTP. Try again."
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !loading && name.isNotBlank() && email.isNotBlank() && phone.isNotBlank()
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Send verification code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { sound.click(); mode = "PASSWORD"; error = "" }) {
                        Text("Already have an account? Sign in", color = Primary, fontSize = 12.sp)
                    }
                }

                // Step 2: OTP verification
                if (step == 2) {
                    Text("Enter verification code", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("We sent a 6-digit code to $email", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("6-digit code") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Password, null, tint = SubText, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (code.length < 6) { sound.error(); error = "Please enter all 6 digits"; return@Button }
                            loading = true; error = ""
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
                                        // If user didn't set password, ask them to
                                        step = 3
                                    } else {
                                        sound.error()
                                        error = "Verification failed. Please try again."
                                    }
                                } catch (e: UnknownHostException) {
                                    sound.error()
                                    error = "No internet connection. Please check your network."
                                } catch (e: IOException) {
                                    sound.error()
                                    error = "Network error. Please try again."
                                } catch (e: Exception) {
                                    sound.error()
                                    error = "Wrong code. Please check and try again."
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !loading && code.length >= 6
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Verify", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { sound.click(); step = 1; error = ""; code = "" }) {
                        Text("← Change details", color = Primary, fontSize = 12.sp)
                    }
                }

                // Step 3: Set password (after OTP verified)
                if (step == 3) {
                    Text("Set a password", color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Use this with your email ($email) to sign in faster next time.", color = SubText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { sound.click(); passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = SubText, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (password.length < 6) { sound.error(); error = "Password must be at least 6 characters"; return@Button }
                            if (password != confirmPassword) { sound.error(); error = "Passwords do not match"; return@Button }
                            loading = true; error = ""
                            scope.launch {
                                try {
                                    // Re-verify OTP with password to set it
                                    val resp = AppState.api.verifyOtp(
                                        VerifyRequest(
                                            contact = email, code = code, role = "STUDENT",
                                            name = name, email = email, phone = phone, password = password
                                        )
                                    )
                                    if (resp.ok) {
                                        sound.success()
                                        AppState.saveSession("logged_in", resp.user)
                                        onLoginSuccess()
                                    } else {
                                        sound.error()
                                        error = "Could not set password. Try again."
                                    }
                                } catch (e: Exception) {
                                    sound.error()
                                    error = "Network error. Please try again."
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !loading && password.length >= 6 && password == confirmPassword
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Set password & continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        sound.click()
                        // Skip password — just login with the OTP session we already have
                        scope.launch {
                            try {
                                val me = AppState.api.getMe()
                                if (me.user != null) {
                                    AppState.saveSession("logged_in", me.user)
                                    onLoginSuccess()
                                }
                            } catch (_: Exception) {
                                error = "Could not complete login. Try again."
                            }
                        }
                    }) {
                        Text("Skip for now", color = SubText, fontSize = 12.sp)
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
