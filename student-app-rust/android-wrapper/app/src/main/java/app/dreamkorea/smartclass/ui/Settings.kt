package app.dreamkorea.smartclass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState

/**
 * Settings bottom sheet — appears as overlay with theme customization,
 * text size, dark mode, animation toggle, notifications.
 */
@Composable
fun SettingsSheet(theme: AppTheme, onDismiss: () -> Unit) {
    var localTheme by remember { mutableStateOf(AppState.getThemeColor()) }
    var darkMode by remember { mutableStateOf(AppState.isDarkMode()) }
    var animations by remember { mutableStateOf(AppState.areAnimationsEnabled()) }
    var notifications by remember { mutableStateOf(AppState.areNotificationsEnabled()) }
    var textSize by remember { mutableStateOf(AppState.getTextSizeMultiplier()) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(300))
    ) {
        Surface(
            color = theme.white,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with grab handle
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(theme.midGray))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", color = theme.darkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = theme.subText)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ─── Theme color ────────────────────────────────────────────
                    item {
                        SettingsSection(theme, "Theme Color", Icons.Default.Palette) {
                            Text("Pick your app's primary color", color = theme.subText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemeColorSwatch(theme, "003478", "Korean Blue", localTheme == Color(0xFF003478)) {
                                    localTheme = Color(0xFF003478); AppState.setThemeColor("003478")
                                }
                                ThemeColorSwatch(theme, "CD2E3A", "Korean Red", localTheme == Color(0xFFCD2E3A)) {
                                    localTheme = Color(0xFFCD2E3A); AppState.setThemeColor("CD2E3A")
                                }
                                ThemeColorSwatch(theme, "00695C", "Teal", localTheme == Color(0xFF00695C)) {
                                    localTheme = Color(0xFF00695C); AppState.setThemeColor("00695C")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemeColorSwatch(theme, "6A1B9A", "Purple", localTheme == Color(0xFF6A1B9A)) {
                                    localTheme = Color(0xFF6A1B9A); AppState.setThemeColor("6A1B9A")
                                }
                                ThemeColorSwatch(theme, "E65100", "Orange", localTheme == Color(0xFFE65100)) {
                                    localTheme = Color(0xFFE65100); AppState.setThemeColor("E65100")
                                }
                                ThemeColorSwatch(theme, "2E7D32", "Green", localTheme == Color(0xFF2E7D32)) {
                                    localTheme = Color(0xFF2E7D32); AppState.setThemeColor("2E7D32")
                                }
                            }
                        }
                    }

                    // ─── Display ────────────────────────────────────────────────
                    item {
                        SettingsSection(theme, "Display", Icons.Default.Brightness6) {
                            ToggleRow(theme, "Dark Mode", "Easier on eyes at night", darkMode) {
                                darkMode = it; AppState.setDarkMode(it)
                            }
                            Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                            Text("Text Size", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Adjust font size throughout the app", color = theme.subText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextSizeOption(theme, "S", 0.85f, textSize == 0.85f) { textSize = 0.85f; AppState.setTextSizeMultiplier(0.85f) }
                                TextSizeOption(theme, "M", 1.0f, textSize == 1.0f) { textSize = 1.0f; AppState.setTextSizeMultiplier(1.0f) }
                                TextSizeOption(theme, "L", 1.15f, textSize == 1.15f) { textSize = 1.15f; AppState.setTextSizeMultiplier(1.15f) }
                                TextSizeOption(theme, "XL", 1.3f, textSize == 1.3f) { textSize = 1.3f; AppState.setTextSizeMultiplier(1.3f) }
                            }
                        }
                    }

                    // ─── Experience ─────────────────────────────────────────────
                    item {
                        SettingsSection(theme, "Experience", Icons.Default.Smartphone) {
                            ToggleRow(theme, "Animations", "Smooth transitions and effects", animations) {
                                animations = it; AppState.setAnimationsEnabled(it)
                            }
                            Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                            ToggleRow(theme, "Notifications", "Exam reminders and updates", notifications) {
                                notifications = it; AppState.setNotificationsEnabled(it)
                            }
                        }
                    }

                    // ─── About ──────────────────────────────────────────────────
                    item {
                        SettingsSection(theme, "About", Icons.Default.Info) {
                            InfoRow(theme, "Version", "1.0.0")
                            Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                            InfoRow(theme, "Build", "2026.07.22")
                            Divider(color = theme.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                            InfoRow(theme, "Server", "dreamkoreasmartclass.com")
                        }
                    }

                    item {
                        Surface(
                            color = theme.cardBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("DreamKorea SmartClass", color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("드림코리아 스마트클래스", color = theme.subText, fontSize = 11.sp)
                                Text("Birtamod, Jhapa, Nepal", color = theme.subText, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save & Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(theme: AppTheme, title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = theme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = theme.darkText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeColorSwatch(theme: AppTheme, hex: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val color = try { Color(("FF$hex").toLong(16)) } catch (_: Exception) { Color(0xFF003478) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = if (selected) theme.primary else theme.subText, fontSize = 9.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun RowScope.TextSizeOption(theme: AppTheme, label: String, value: Float, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) theme.primary else theme.lightGray,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f).height(38.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                color = if (selected) Color.White else theme.darkText,
                fontSize = (12.sp * value),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ToggleRow(theme: AppTheme, title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(title, color = theme.darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.subText, fontSize = 11.sp)
        }
        Switch(checked = value, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = theme.primary))
    }
}

@Composable
private fun InfoRow(theme: AppTheme, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.subText, fontSize = 12.sp)
        Text(value, color = theme.darkText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
