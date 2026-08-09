package app.dreamkorea.smartclass

import android.Manifest
import android.content.pm.PackageManager
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
        setContent {
            var isLoggedIn by remember { mutableStateOf(AppState.isLoggedIn()) }
            var userName by remember { mutableStateOf(AppState.getUserName()) }

            // Handle deep link from Clerk OAuth redirect
            LaunchedEffect(Unit) {
                handleDeepLink(intent, isLoggedIn = { isLoggedIn }, userName = { userName }, setLoggedIn = { isLoggedIn = it }, setUserName = { userName = it })
            }

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The deep link will be handled by the LaunchedEffect in Compose
    }

    private fun handleDeepLink(
        intent: android.content.Intent?,
        isLoggedIn: () -> Boolean,
        userName: () -> String?,
        setLoggedIn: (Boolean) -> Unit,
        setUserName: (String) -> Unit
    ) {
        val data = intent?.data ?: return
        if (data.scheme == "dreamkorea" && data.host == "auth-callback") {
            // Extract user data from the redirect URL
            val userId = data.getQueryParameter("userId")
            val name = data.getQueryParameter("name")
            val email = data.getQueryParameter("email")
            val phone = data.getQueryParameter("phone")
            val role = data.getQueryParameter("role")

            if (userId != null && email != null) {
                // Save the user profile and log in
                AppState.saveUserProfile(
                    app.dreamkorea.smartclass.api.User(
                        id = userId,
                        name = name,
                        email = email,
                        phone = phone,
                        role = role ?: "STUDENT"
                    )
                )
                AppState.invalidateCache()
                setLoggedIn(true)
                setUserName(AppState.getUserName())
            }
        }
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
