package app.dreamkorea.smartclass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState
import kotlinx.coroutines.launch

/**
 * Login Screen — three tabs: Sign In / Sign Up / Forgot Password.
 *
 * Sign In: email + password → POST /api/auth/credentials
 * Sign Up: name + email + phone + password → POST /api/auth/signup (mode=student)
 *          — no OTP needed, password is set immediately
 * Forgot:  email → POST /api/auth/request-reset → enter 6-digit code →
 *          new password → POST /api/auth/reset-password
 *
 * Default tab: Sign In. The DK logo animates in on mount. Background is a
 * navy gradient (matching the brand). Form card sits on a white surface with
 * a strong shadow for separation.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sound = rememberSoundManager()

    // Tab: "login" | "signup" | "forgot"
    var mode by remember { mutableStateOf("login") }

    // Sign Up state
    var suName by remember { mutableStateOf("") }
    var suEmail by remember { mutableStateOf("") }
    var suPhone by remember { mutableStateOf("") }
    var suPassword by remember { mutableStateOf("") }

    // Login state
    var liEmail by remember { mutableStateOf("") }
    var liPassword by remember { mutableStateOf("") }

    // Forgot password state
    var fpEmail by remember { mutableStateOf("") }
    var fpCode by remember { mutableStateOf("") }
    var fpNewPassword by remember { mutableStateOf("") }
    var fpStep by remember { mutableStateOf(1) } // 1=enter email, 2=enter code+new password

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

            Spacer(modifier = Modifier.height(20.dp))
            Text("DreamKorea SmartClass", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Learn Korean anywhere", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Three-tab toggle: Sign In / Sign Up / Forgot
            AnimatedVisibility(visible = logoVisible, enter = fadeIn(tween(500))) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf("login" to "Sign In", "signup" to "Sign Up", "forgot" to "Forgot").forEach { (key, label) ->
                        if (key != "login" && key != "signup") Spacer(Modifier.width(8.dp))
                        if (key == "signup") Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = mode == key,
                            onClick = { sound.click(); mode = key; error = ""; info = ""; fpStep = 1 },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form card
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().alpha(formAlpha), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Info banner
                    if (info.isNotEmpty()) {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(info, color = SuccessGreen, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    // Error banner
                    if (error.isNotEmpty()) {
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
                            "login" -> LoginTab(
                                email = liEmail, password = liPassword, passwordVisible = passwordVisible, loading = loading,
                                onEmailChange = { liEmail = it }, onPasswordChange = { liPassword = it },
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onSubmit = {
                                    if (liEmail.isBlank() || liPassword.isBlank()) { error = "Enter email and password"; return@LoginTab }
                                    loading = true; error = ""; info = ""
                                    scope.launch {
                                        try {
                                            val resp = AppState.api.loginCredentials(mapOf("username" to liEmail.trim(), "password" to liPassword))
                                            if (resp.ok) {
                                                sound.success()
                                                AppState.saveUserProfile(resp.user)
                                                AppState.invalidateCache()
                                                onLoginSuccess()
                                            } else {
                                                sound.error()
                                                error = resp.error ?: "Wrong email or password."
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            sound.error()
                                            error = extractHttpError(e) ?: "Wrong email or password."
                                        } catch (e: java.net.UnknownHostException) { sound.error(); error = "No internet connection." }
                                        catch (e: java.io.IOException) { sound.error(); error = "Could not connect." }
                                        catch (e: Exception) { sound.error(); error = "Login failed: ${e.message ?: "unknown"}" }
                                        loading = false
                                    }
                                }
                            )
                            "signup" -> SignupTab(
                                name = suName, email = suEmail, phone = suPhone, password = suPassword,
                                passwordVisible = passwordVisible, loading = loading,
                                onNameChange = { suName = it }, onEmailChange = { suEmail = it },
                                onPhoneChange = { suPhone = it }, onPasswordChange = { suPassword = it },
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onSubmit = {
                                    if (suName.isBlank()) { error = "Name is required"; return@SignupTab }
                                    if (suEmail.isBlank() || !suEmail.contains("@")) { error = "Enter a valid email"; return@SignupTab }
                                    if (suPassword.length < 6) { error = "Password must be at least 6 characters"; return@SignupTab }
                                    loading = true; error = ""; info = ""
                                    scope.launch {
                                        try {
                                            val resp = AppState.api.signup(mapOf(
                                                "mode" to "student",
                                                "name" to suName.trim(),
                                                "email" to suEmail.trim().lowercase(),
                                                "phone" to suPhone.trim(),
                                                "password" to suPassword
                                            ))
                                            if (resp.ok) {
                                                sound.success()
                                                AppState.saveUserProfile(resp.user)
                                                AppState.invalidateCache()
                                                onLoginSuccess()
                                            } else {
                                                sound.error()
                                                error = resp.error ?: "Signup failed."
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            sound.error()
                                            error = extractHttpError(e) ?: "Signup failed."
                                        } catch (e: java.net.UnknownHostException) { sound.error(); error = "No internet connection." }
                                        catch (e: java.io.IOException) { sound.error(); error = "Could not connect." }
                                        catch (e: Exception) { sound.error(); error = "Signup failed: ${e.message ?: "unknown"}" }
                                        loading = false
                                    }
                                }
                            )
                            "forgot" -> ForgotTab(
                                email = fpEmail, code = fpCode, newPassword = fpNewPassword,
                                passwordVisible = passwordVisible, loading = loading, step = fpStep,
                                onEmailChange = { fpEmail = it }, onCodeChange = { fpCode = it },
                                onNewPasswordChange = { fpNewPassword = it },
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onRequestCode = {
                                    if (fpEmail.isBlank() || !fpEmail.contains("@")) { error = "Enter a valid email"; return@ForgotTab }
                                    loading = true; error = ""; info = ""
                                    scope.launch {
                                        try {
                                            AppState.api.requestReset(mapOf("email" to fpEmail.trim().lowercase()))
                                            sound.success()
                                            info = "If an account exists, a reset code was sent to ${fpEmail.trim()}."
                                            fpStep = 2
                                        } catch (e: java.net.UnknownHostException) { sound.error(); error = "No internet connection." }
                                        catch (e: java.io.IOException) { sound.error(); error = "Could not connect." }
                                        catch (e: Exception) { sound.error(); error = "Request failed." }
                                        loading = false
                                    }
                                },
                                onReset = {
                                    if (fpCode.length != 6) { error = "Enter the 6-digit code"; return@ForgotTab }
                                    if (fpNewPassword.length < 6) { error = "New password must be at least 6 characters"; return@ForgotTab }
                                    loading = true; error = ""; info = ""
                                    scope.launch {
                                        try {
                                            val resp = AppState.api.resetPassword(mapOf(
                                                "email" to fpEmail.trim().lowercase(),
                                                "code" to fpCode.trim(),
                                                "newPassword" to fpNewPassword
                                            ))
                                            if (resp.ok) {
                                                sound.success()
                                                info = "Password reset! You can now sign in with your new password."
                                                mode = "login"
                                                liEmail = fpEmail
                                                fpStep = 1
                                                fpCode = ""; fpNewPassword = ""
                                            } else {
                                                sound.error()
                                                error = resp.error ?: "Reset failed."
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            sound.error()
                                            error = extractHttpError(e) ?: "Reset failed."
                                        } catch (e: java.net.UnknownHostException) { sound.error(); error = "No internet connection." }
                                        catch (e: java.io.IOException) { sound.error(); error = "Could not connect." }
                                        catch (e: Exception) { sound.error(); error = "Reset failed: ${e.message ?: "unknown"}" }
                                        loading = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Google Sign-In button (visible on all tabs) ──
            val context = LocalContext.current

            GoogleSignInButton(loading = loading) {
                if (loading) return@GoogleSignInButton
                // Opens the Vercel-hosted Clerk sign-in page in Chrome Custom Tab
                // After sign-in, the browser redirects to dreamkorea://auth-callback
                GoogleSignInHelper.signInWithGoogle(context)
            }
        }
    }
}

// ─── Login tab ───────────────────────────────────────────────────────────
@Composable
private fun LoginTab(
    email: String, password: String, passwordVisible: Boolean, loading: Boolean,
    onEmailChange: (String) -> Unit, onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit, onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Welcome Back", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Sign in with your email and password", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Field("Email", email, onEmailChange, Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        PasswordField("Password", password, onPasswordChange, passwordVisible, onTogglePassword)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign In", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Sign Up tab ─────────────────────────────────────────────────────────
@Composable
private fun SignupTab(
    name: String, email: String, phone: String, password: String,
    passwordVisible: Boolean, loading: Boolean,
    onNameChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit, onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit, onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Create Account", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Sign up with email and set a password", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Field("Full Name", name, onNameChange, Icons.Default.Person, KeyboardType.Text)
        Spacer(Modifier.height(10.dp))
        Field("Email", email, onEmailChange, Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        Field("Phone (optional)", phone, onPhoneChange, Icons.Default.Phone, KeyboardType.Phone, "+977 98XXXXXXXX")
        Spacer(Modifier.height(10.dp))
        PasswordField("Password (min 6 chars)", password, onPasswordChange, passwordVisible, onTogglePassword)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create Account", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Forgot Password tab ─────────────────────────────────────────────────
@Composable
private fun ForgotTab(
    email: String, code: String, newPassword: String,
    passwordVisible: Boolean, loading: Boolean, step: Int,
    onEmailChange: (String) -> Unit, onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit, onTogglePassword: () -> Unit,
    onRequestCode: () -> Unit, onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(if (step == 1) "Reset Password" else "Enter Code & New Password", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(if (step == 1) "We'll send a 6-digit code to your email" else "Check your email for the 6-digit code", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        if (step == 1) {
            Field("Email", email, onEmailChange, Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRequestCode,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !loading
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Send Reset Code", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Read-only email display
            Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, null, tint = TextMid, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(email, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Code field
            OutlinedTextField(
                value = code, onValueChange = { onCodeChange(it.filter { c -> c.isDigit() }.take(6)) },
                label = { Text("6-digit code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark, unfocusedTextColor = TextDark,
                    focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor,
                    cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Password, null, tint = TextMid, modifier = Modifier.size(20.dp)) }
            )
            Spacer(Modifier.height(10.dp))
            PasswordField("New Password (min 6 chars)", newPassword, onNewPasswordChange, passwordVisible, onTogglePassword)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !loading
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Reset Password", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Reusable field helpers ──────────────────────────────────────────────
@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, keyboardType: KeyboardType, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark, unfocusedTextColor = TextDark,
            focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor,
            cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icon, null, tint = TextMid, modifier = Modifier.size(20.dp)) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = TextLight, fontSize = 13.sp) } } else null
    )
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark, unfocusedTextColor = TextDark,
            focusedBorderColor = NavyBlue, unfocusedBorderColor = DividerColor,
            cursorColor = NavyBlue, focusedLabelColor = NavyBlue, unfocusedLabelColor = TextMid
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMid, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextMid, modifier = Modifier.size(18.dp))
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

/**
 * Extract the human-readable error message from a Retrofit HttpException.
 *
 * The server returns errors as JSON: {"error": "Email already registered"}
 * We parse the response body to pull out the "error" field. If parsing fails
 * or the body is empty, we return null so the caller can use a fallback.
 */
private fun extractHttpError(e: retrofit2.HttpException): String? {
    return try {
        val raw = e.response()?.errorBody()?.string()
        if (raw.isNullOrBlank()) return null
        val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
        json.get("error")?.asString
    } catch (_: Exception) {
        null
    }
}

// ─── Google Sign-In Button ────────────────────────────────────────────────
@Composable
private fun GoogleSignInButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        enabled = !loading
    ) {
        if (loading) {
            CircularProgressIndicator(color = NavyBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Real Google "G" logo
                Image(
                    painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.google_logo),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(12.dp))
                Text("Sign in with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
