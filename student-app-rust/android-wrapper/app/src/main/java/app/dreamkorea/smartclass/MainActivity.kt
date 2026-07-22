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
import androidx.compose.ui.graphics.Color
import app.dreamkorea.smartclass.data.AppState
import app.dreamkorea.smartclass.ui.*
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isLoggedIn by remember { mutableStateOf(false) }
            var userName by remember { mutableStateOf("Student") }
            var checked by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val name = AppState.nameFlow().first()
                if (name.isNotEmpty()) {
                    isLoggedIn = true
                    userName = name
                }
                checked = true
            }

            if (!checked) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BgDark),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent)
                }
            } else if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                })
            } else {
                MainScreen(userName = userName, onLogout = {
                    isLoggedIn = false
                    userName = "Student"
                })
            }
        }
    }
}
