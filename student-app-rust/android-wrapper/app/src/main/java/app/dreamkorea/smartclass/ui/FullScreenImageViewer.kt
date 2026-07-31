package app.dreamkorea.smartclass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Full-screen image viewer — single shared instance for the whole app.
 *
 * Usage from any Composable:
 *   FullScreenImageViewer.show("https://.../image.png")
 *
 * And in your top-level Composable tree (e.g. inside MainActivity):
 *   FullScreenViewerOverlay(theme)
 *
 * The overlay handles pinch-to-zoom, pan, and tap-to-dismiss. It floats above
 * every other Composable as a transparent layer when active.
 */
object FullScreenImageViewer {
    private val _state = MutableStateFlow<String?>(null)
    val state: StateFlow<String?> = _state

    fun show(url: String) { _state.value = url }
    fun hide() { _state.value = null }
}

@Composable
fun FullScreenViewerOverlay(theme: AppTheme) {
    val urlState by FullScreenImageViewer.state.collectAsState()
    val url = urlState ?: return

    // Per-show zoom/pan state — resets each time a new image is shown
    var scale by remember(url) { mutableStateOf(1f) }
    var offsetX by remember(url) { mutableStateOf(0f) }
    var offsetY by remember(url) { mutableStateOf(0f) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // Image layer — handles pinch + pan
            coil.compose.AsyncImage(
                model = url,
                contentDescription = "Full-screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(url) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(url) {
                        // Tap-to-dismiss — but only when not zoomed in
                        detectTapGestures(
                            onTap = {
                                if (scale <= 1.05f) FullScreenImageViewer.hide()
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )

            // Close button — always visible, top-right
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .pointerInput(url) {
                        detectTapGestures(onTap = { FullScreenImageViewer.hide() })
                    }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Hint text — bottom center
            Text(
                "Pinch to zoom · tap to close",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
