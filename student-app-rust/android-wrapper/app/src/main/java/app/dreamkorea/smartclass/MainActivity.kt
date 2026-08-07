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
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) NotificationService.startPolling(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isLoggedIn by remember { mutableStateOf(AppState.isLoggedIn()) }
            var userName by remember { mutableStateOf(AppState.getUserName()) }
            LaunchedEffect(isLoggedIn) { if (isLoggedIn) askNotificationPermission() }
            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = { isLoggedIn = true; userName = AppState.getUserName() })
            } else {
                MainScreen(userName = userName, onLogout = {
                    NotificationService.stopPolling(); AppState.clearSession(); isLoggedIn = false; userName = "Student"
                })
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) NotificationService.startPolling(this)
            else requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else NotificationService.startPolling(this)
    }
}
