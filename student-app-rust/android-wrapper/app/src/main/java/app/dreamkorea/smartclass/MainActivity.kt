package app.dreamkorea.smartclass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import app.dreamkorea.smartclass.api.User
import app.dreamkorea.smartclass.data.AppState
import app.dreamkorea.smartclass.notifications.NotificationService
import app.dreamkorea.smartclass.ui.*

class MainActivity : ComponentActivity() {
    // Notification permission launcher (Android 13+)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationService.startPolling(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ── Handle deep link on cold launch ──────────────────────────────
        // dreamkorea://auth-callback?userId=...&name=...&email=...&phone=...&role=...
        handleAuthCallback(intent)
        setContent {
            var isLoggedIn by remember { mutableStateOf(AppState.isLoggedIn()) }
            var userName by remember { mutableStateOf(AppState.getUserName()) }

            // Ask for notification permission + start polling when logged in
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    askNotificationPermission()
                }
            }

            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                    userName = AppState.getUserName()
                })
            } else {
                MainScreen(userName = userName, onLogout = {
                    NotificationService.stopPolling()
                    AppState.clearSession()
                    isLoggedIn = false
                    userName = "Student"
                })
            }
        }
    }

    /**
     * Called when a new Intent is delivered to this singleTask activity while
     * it's already running (i.e. the browser redirects back to the app after a
     * Google sign-in completed in Chrome). We re-read the auth-callback query
     * params and refresh the logged-in state.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
    }

    /**
     * Reads the query params from a `dreamkorea://auth-callback` deep link and
     * saves the user profile. Expected params:
     *   userId, name, email, phone, role
     *
     * The actual session cookie (ep_sid) is set by the server in the
     * Set-Cookie header during the OAuth flow — but since the redirect
     * happens in Chrome (not in our OkHttp client), we mark a session-via-
     * cookie placeholder so isLoggedIn() returns true. The next API call
     * will fail with 401 if the cookie wasn't actually issued; in that case
     * the user will be sent back to the login screen.
     */
    private fun handleAuthCallback(intent: Intent?) {
        if (intent == null) return
        val data: Uri = intent.data ?: return
        if (data.scheme != "dreamkorea" || data.host != "auth-callback") return

        val userId = data.getQueryParameter("userId") ?: return
        val name = data.getQueryParameter("name") ?: ""
        val email = data.getQueryParameter("email") ?: ""
        val phone = data.getQueryParameter("phone") ?: ""
        val role = data.getQueryParameter("role") ?: "STUDENT"

        // Build the user profile and save it. saveUserProfile stores a
        // "session_via_cookie" marker so isLoggedIn() returns true without
        // overwriting any real ep_sid cookie that the OkHttp CookieJar may
        // have captured.
        AppState.saveUserProfile(
            User(
                id = userId,
                name = name.ifBlank { "Student" },
                email = email,
                phone = phone.ifBlank { null },
                role = role,
            )
        )
        AppState.invalidateCache()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                NotificationService.startPolling(this)
            } else {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 and below — no runtime permission needed
            NotificationService.startPolling(this)
        }
    }
}
