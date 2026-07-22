package app.dreamkorea.smartclass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.dreamkorea.smartclass.data.AppState
import app.dreamkorea.smartclass.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isLoggedIn by remember { mutableStateOf(AppState.isLoggedIn()) }
            var userName by remember { mutableStateOf(AppState.getUserName()) }

            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                    userName = AppState.getUserName()
                })
            } else {
                MainScreen(userName = userName, onLogout = {
                    AppState.clearSession()
                    isLoggedIn = false
                    userName = "Student"
                })
            }
        }
    }
}
