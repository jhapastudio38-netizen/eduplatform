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

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

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
    val logoOffset by animateFloatAsState(
        targetValue = if (logoVisible) 0f else -30f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoOffset"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "formAlpha"
    )

    // Inverted gradient: white/light at top, navy blue at bottom
    // Logo sits in the blue area and blends naturally
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFFF8FAFC), Color(0xFFEFF6FF), NavyBlue.copy(alpha = 0.3f), NavyBlue)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo — sits on the blue part, white circle with subtle glow
            Box(contentAlignment = Alignment.Center) {
                // Soft blue glow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                            )
                        )
                )
                Surface(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(100.dp).scale(logoScale),
                    shadowElevation = 12.dp,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = app.dreamkorea.smartclass.R.drawable.dreamkorea_logo),
                            contentDescription = "DreamKorea Logo",
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // No text — just the logo speaks for itself
            Spacer(modifier = Modifier.height(28.dp))

            // Form card — clean white card floating on blue
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().alpha(formAlpha),
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 5 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 5 })
                        },
                        label = "stepTransition"
                    ) { currentStep ->
                        when (currentStep) {
                            1 -> Step1Details(name, email, phone, loading) { n, e, p ->
                                name = n; email = e; phone = p
                                loading = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        AppState.api.requestOtp(OtpRequest(email))
                                        sound.success()
                                        info = "Code sent to $email"
                                        step = 2
                                    } catch (e: UnknownHostException) { sound.error(); error = "No internet connection." }
                                    catch (e: IOException) { sound.error(); error = "Could not connect to server." }
                                    catch (e: Exception) { sound.error(); error = "Could not send code." }
                                    loading = false
                                }
                            }
                            2 -> Step2Verify(code, email, loading) { c ->
                                code = c
                                loading = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        val resp = AppState.api.verifyOtp(VerifyRequest(contact = email, code = code, role = "STUDENT", name = name, email = email, phone = phone))
                                        if (resp.ok) {
                                            sound.success()
                                            val token = resp.sessionToken ?: "session_via_cookie"
                                            AppState.saveSession(token, resp.user)
                                            AppState.invalidateCache()
                                            onLoginSuccess()
                                        } else { sound.error(); error = "Verification failed." }
                                    } catch (e: UnknownHostException) { sound.error(); error = "No internet connection." }
                                    catch (e: IOException) { sound.error(); error = "Could not connect." }
                                    catch (e: Exception) { sound.error(); error = "Wrong code." }
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

@Composable
private fun Step1Details(name: String, email: String, phone: String, loading: Boolean, onSubmit: (String, String, String) -> Unit) {
    var n by remember { mutableStateOf(name) }
    var e by remember { mutableStateOf(email) }
    var p by remember { mutableStateOf(phone) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Welcome", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Sign up or log in with your email", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Field(label = "Full Name", value = n, onChange = { n = it }, icon = Icons.Default.Person, keyboardType = KeyboardType.Text)
        Spacer(Modifier.height(10.dp))
        Field(label = "Email", value = e, onChange = { e = it }, icon = Icons.Default.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        Field(label = "Phone Number", value = p, onChange = { p = it.filter { c -> c.isDigit() || c == '+' } }, icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone, placeholder = "+977 98XXXXXXXX")
        Spacer(Modifier.height(6.dp))
        Text("Returning user? Use the same email to log in.", color = TextLight, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (n.isBlank()) return@Button
                if (e.isBlank()) return@Button
                if (!isEmail(e)) return@Button
                if (p.isBlank()) return@Button
                if (!isValidPhone(p)) return@Button
                onSubmit(n, e, p)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading && n.isNotBlank() && e.isNotBlank() && p.isNotBlank()
        ) {
            if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            else { Text("Send Verification Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Step2Verify(code: String, email: String, loading: Boolean, onSubmit: (String) -> Unit) {
    var c by remember { mutableStateOf(code) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Verify", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Enter the 6-digit code sent to $email", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
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
        TextButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Resend code", color = NavyBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Change details", color = TextMid, fontSize = 13.sp) }
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
