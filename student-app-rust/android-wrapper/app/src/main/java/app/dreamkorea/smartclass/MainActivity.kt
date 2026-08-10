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

    // Tracks whether the user is logged in. Observed by the Compose tree so
    // that a deep-link login (Google OAuth) flips the UI from LoginScreen to
    // MainScreen without a restart.
    private val isLoggedInState = mutableStateOf(AppState.isLoggedIn())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle any deep link that launched the app (cold start).
        handleAuthCallback(intent)
        setContent {
            // Read the mutable state so Compose re-composes when login changes.
            var isLoggedIn by isLoggedInState
            val userName = remember(isLoggedIn) { AppState.getUserName() }

            // Ask for notification permission + start polling when logged in
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    askNotificationPermission()
                }
            }

            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                    isLoggedInState.value = true
                })
            } else {
                MainScreen(userName = userName, onLogout = {
                    NotificationService.stopPolling()
                    AppState.clearSession()
                    isLoggedIn = false
                    isLoggedInState.value = false
                })
            }
        }
    }

    // Warm launch — app already running, Android delivers the deep link here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
    }

    /**
     * Parses a `dreamkorea://auth-callback?userId=...&name=...&email=...&phone=...&role=...`
     * deep link, saves the user profile via AppState, and flips isLoggedIn so
     * the UI switches from LoginScreen to MainScreen.
     *
     * Silently ignores URIs that don't match the auth-callback host.
     */
    private fun handleAuthCallback(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "dreamkorea" || data.host != "auth-callback") return

        val userId = data.getQueryParameter("userId") ?: return
        val name = data.getQueryParameter("name")
        val email = data.getQueryParameter("email") ?: ""
        val phone = data.getQueryParameter("phone")
        val role = data.getQueryParameter("role") ?: "STUDENT"
        val sessionToken = data.getQueryParameter("sessionToken")

        AppState.saveUserProfile(
            User(
                id = userId,
                name = name,
                email = email,
                phone = phone,
                role = role,
            )
        )
        // Save the session token so API calls are authenticated
        if (!sessionToken.isNullOrBlank()) {
            AppState.saveSessionToken(sessionToken)
        }
        AppState.invalidateCache()
        isLoggedInState.value = true
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
