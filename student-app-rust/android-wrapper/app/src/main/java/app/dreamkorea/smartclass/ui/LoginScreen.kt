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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

private fun isEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()
private fun isValidPhone(s: String): Boolean {
    val digits = s.replace("\\D".toRegex(), "")
    return digits.length in 7..15
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sound = rememberSoundManager()

    // Mode: "signup" or "login"
    var mode by remember { mutableStateOf("signup") }

    // Signup state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1=details, 2=OTP

    // Login state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animations
    var logoVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        logoVisible = true
        kotlinx.coroutines.delay(300)
        formVisible = true
    }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "formAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEFF6FF), NavyBlue.copy(alpha = 0.3f), NavyBlue))
    )) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Surface(color = Color.White, shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(100.dp).scale(logoScale), shadowElevation = 12.dp) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Image(painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                        contentDescription = "Logo", modifier = Modifier.size(64.dp), contentScale = ContentScale.Fit)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Mode toggle
            AnimatedVisibility(visible = logoVisible, enter = fadeIn(tween(500))) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(selected = mode == "signup", onClick = { sound.click(); mode = "signup"; step = 1; error = ""; info = "" },
                        label = { Text("Sign Up", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyBlue, selectedLabelColor = Color.White))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = mode == "login", onClick = { sound.click(); mode = "login"; error = ""; info = "" },
                        label = { Text("Sign In", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyBlue, selectedLabelColor = Color.White))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form card
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().alpha(formAlpha), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Messages
                    AnimatedVisibility(visible = info.isNotEmpty(), enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(info, color = SuccessGreen, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    AnimatedVisibility(visible = error.isNotEmpty(), enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                        Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(error, color = Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    AnimatedContent(targetState = mode, transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 5 }) togetherWith
                        (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 5 })
                    }, label = "modeTransition") { currentMode ->
                        when (currentMode) {
                            "signup" -> {
                                AnimatedContent(targetState = step, transitionSpec = {
                                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 5 }) togetherWith
                                    (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 5 })
                                }, label = "stepTransition") { currentStep ->
                                    when (currentStep) {
                                        1 -> SignupStep1(name, email, phone, password, passwordVisible, loading) { n, e, p, pw ->
                                            name = n; email = e; phone = p; password = pw
                                            loading = true; error = ""; info = ""
                                            scope.launch {
                                                try {
                                                    AppState.api.requestOtp(OtpRequest(email))
                                                    sound.success(); info = "Code sent to $email"; step = 2
                                                } catch (e: UnknownHostException) { sound.error(); error = "No internet." }
                                                catch (e: IOException) { sound.error(); error = "Could not connect." }
                                                catch (e: Exception) { sound.error(); error = "Could not send code." }
                                                loading = false
                                            }
                                        }
                                        2 -> SignupStep2(code, email, loading, onBack = { step = 1 }, onSubmit = { c ->
                                            code = c; loading = true; error = ""; info = ""
                                            scope.launch {
                                                try {
                                                    val resp = AppState.api.verifyOtp(VerifyRequest(
                                                        contact = email, code = code, role = "STUDENT",
                                                        name = name, email = email, phone = phone, password = password
                                                    ))
                                                    if (resp.ok) {
                                                        sound.success()
                                                        val token = resp.sessionToken ?: "session_via_cookie"
                                                        AppState.saveSession(token, resp.user)
                                                        AppState.invalidateCache()
                                                        onLoginSuccess()
                                                    } else { sound.error(); error = "Verification failed." }
                                                } catch (e: UnknownHostException) { sound.error(); error = "No internet." }
                                                catch (e: IOException) { sound.error(); error = "Could not connect." }
                                                catch (e: Exception) { sound.error(); error = "Wrong code." }
                                                loading = false
                                            }
                                        }
                                    }
                                }
                            }
                            "login" -> LoginStep(loginEmail, loginPassword, passwordVisible, loading) { e, pw ->
                                loginEmail = e; loginPassword = pw
                                loading = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        val resp = AppState.api.loginCredentials(mapOf("username" to loginEmail, "password" to loginPassword))
                                        if (resp.ok) {
                                            sound.success()
                                            // Cookie jar already captured the session cookie
                                            // Save user profile without overwriting the token
                                            AppState.saveUserProfile(resp.user)
                                            AppState.invalidateCache()
                                            onLoginSuccess()
                                        } else {
                                            sound.error()
                                            error = when {
                                                resp.error?.contains("password", ignoreCase = true) == true -> "Wrong password."
                                                resp.error?.contains("not found", ignoreCase = true) == true -> "No account found."
                                                resp.error?.contains("suspended", ignoreCase = true) == true -> "Account suspended."
                                                else -> "Wrong email or password."
                                            }
                                        }
                                    } catch (e: UnknownHostException) { sound.error(); error = "No internet." }
                                    catch (e: IOException) { sound.error(); error = "Could not connect." }
                                    catch (e: Exception) { sound.error(); error = "Login failed." }
                                    loading = false
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// SIGNUP STEP 1: Name + Email + Phone + Password
@Composable
private fun SignupStep1(name: String, email: String, phone: String, password: String, passwordVisible: Boolean, loading: Boolean, onSubmit: (String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf(name) }
    var e by remember { mutableStateOf(email) }
    var p by remember { mutableStateOf(phone) }
    var pw by remember { mutableStateOf(password) }
    var pwVisible by remember { mutableStateOf(passwordVisible) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Create Account", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Sign up with email and set a password", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Field("Full Name", n, { n = it }, Icons.Default.Person, KeyboardType.Text)
        Spacer(Modifier.height(10.dp))
        Field("Email", e, { e = it }, Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        Field("Phone", p, { p = it.filter { c -> c.isDigit() || c == '+' } }, Icons.Default.Phone, KeyboardType.Phone, "+977 98XXXXXXXX")
        Spacer(Modifier.height(10.dp))
        // Password field
        OutlinedTextField(
            value = pw, onValueChange = { pw = it }, label = { Text("Password (min 6 chars)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor, cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMid, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }) {
                    Icon(if (pwVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextMid, modifier = Modifier.size(18.dp))
                }
            },
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (n.isBlank()) return@Button
                if (e.isBlank() || !isEmail(e)) return@Button
                if (p.isBlank() || !isValidPhone(p)) return@Button
                if (pw.length < 6) return@Button
                onSubmit(n, e, p, pw)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading && n.isNotBlank() && e.isNotBlank() && p.isNotBlank() && pw.length >= 6
        ) {
            if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            else { Text("Send Verification Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// SIGNUP STEP 2: OTP
@Composable
private fun SignupStep2(code: String, email: String, loading: Boolean, onBack: () -> Unit, onSubmit: (String) -> Unit) {
    var c by remember { mutableStateOf(code) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Verify", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Enter the 6-digit code sent to $email", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = c, onValueChange = { c = it.filter { ch -> ch.isDigit() }.take(6) },
            modifier = Modifier.fillMaxWidth(), label = { Text("6-digit code") }, singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor, cursorColor = NavyBlue),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Password, null, tint = TextMid, modifier = Modifier.size(20.dp)) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (c.length >= 6) onSubmit(c) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading && c.length >= 6
        ) {
            if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            else { Text("Verify & Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { /* resend */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Resend code", color = NavyBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Change details", color = TextMid, fontSize = 13.sp)
        }
    }
}

// LOGIN: Email + Password
@Composable
private fun LoginStep(email: String, password: String, passwordVisible: Boolean, loading: Boolean, onSubmit: (String, String) -> Unit) {
    var e by remember { mutableStateOf(email) }
    var pw by remember { mutableStateOf(password) }
    var pwVisible by remember { mutableStateOf(passwordVisible) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Welcome Back", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Sign in with your email and password", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Field("Email", e, { e = it }, Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pw, onValueChange = { pw = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor, cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMid, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }) {
                    Icon(if (pwVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextMid, modifier = Modifier.size(18.dp))
                }
            },
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (e.isNotBlank() && pw.isNotBlank()) onSubmit(e, pw)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading && e.isNotBlank() && pw.isNotBlank()
        ) {
            if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            else { Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Forgot password? Use Sign Up with same email to reset.", color = TextLight, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, keyboardType: KeyboardType, placeholder: String? = null) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextDark, unfocusedTextColor = TextDark, focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor, cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icon, null, tint = TextMid, modifier = Modifier.size(20.dp)) }
    )
}
