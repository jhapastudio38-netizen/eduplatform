package app.dreamkorea.smartclass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import app.dreamkorea.smartclass.data.AppState
import app.dreamkorea.smartclass.notifications.NotificationService
import app.dreamkorea.smartclass.ui.*
import app.dreamkorea.smartclass.api.User

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) NotificationService.startPolling(this)
    }

    private var googleRedirectData: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            var isLoggedIn by remember { mutableStateOf(AppState.isLoggedIn()) }
            var userName by remember { mutableStateOf(AppState.getUserName()) }

            LaunchedEffect(Unit) {
                googleRedirectData?.let { data ->
                    handleGoogleRedirect(data) { loggedIn, name ->
                        if (loggedIn) {
                            isLoggedIn = true
                            userName = name ?: AppState.getUserName()
                        }
                    }
                    googleRedirectData = null
                }
            }

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) askNotificationPermission()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "dreamkorea" && data.host == "auth-callback") {
            Log.d("MainActivity", "Google OAuth redirect received: $data")
            googleRedirectData = data
        }
    }

    private fun handleGoogleRedirect(data: Uri, onResult: (Boolean, String?) -> Unit) {
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
        if (!sessionToken.isNullOrBlank()) {
            AppState.saveSessionToken(sessionToken)
        }
        AppState.invalidateCache()
        onResult(true, AppState.getUserName())
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) NotificationService.startPolling(this)
            else requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationService.startPolling(this)
        }
    }
}
