package app.dreamkorea.smartclass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/// Full-screen image viewer — tap an image to open it full-screen.
object FullScreenImageViewer {
    private var currentUrl = mutableStateOf<String?>(null)

    fun show(url: String) {
        currentUrl.value = url
    }

    @Composable
    fun Composable() {
        val url = currentUrl.value
        if (url != null) {
            Dialog(onDismissRequest = { currentUrl.value = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { currentUrl.value = null },
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = url,
                        contentDescription = "Full screen image",
                        modifier = Modifier.fillMaxSize(0.95f),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { currentUrl.value = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
