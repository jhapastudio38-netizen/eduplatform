package app.dreamkorea.smartclass.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dreamkorea.smartclass.data.AppState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// ─── Theme ─────────────────────────────────────────────────────────────────────

/**
 * AppTheme — derives all colors from the user's chosen primary color.
 * Korean flag palette is the default: blue (#003478) primary, red (#CD2E3A) accent.
 */
data class AppTheme(
    val primary: Color,
    val accent: Color,
    val isDark: Boolean,
) {
    // Refined dark mode palette — true dark with good contrast
    val white get() = if (isDark) Color(0xFF1C1C24) else Color(0xFFFFFFFF)        // surface (top bar, cards)
    val lightGray get() = if (isDark) Color(0xFF0D0D12) else Color(0xFFF6F7F9)    // background
    val midGray get() = if (isDark) Color(0xFF2C2C38) else Color(0xFFE9ECEF)      // shimmer
    val darkText get() = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1A1A2E)     // primary text
    val subText get() = if (isDark) Color(0xFFAEAEC0) else Color(0xFF6C757D)      // secondary text
    val primaryLight get() = lerp(primary, if (isDark) Color(0xFF2C2C38) else Color.White, 0.85f)
    val errorRed get() = Color(0xFFFF5252)  // brighter red for dark mode visibility
    val successGreen get() = Color(0xFF4CD964) // brighter green for dark mode
    val divider get() = if (isDark) Color(0xFF383844) else Color(0xFFE8E8EE)
    val cardBg get() = if (isDark) Color(0xFF232330) else Color(0xFFFFFFFF)       // cards
    val background get() = lightGray
}

/** Global theme revision counter — bump to force re-composition of all UI. */
val themeRevision = mutableStateOf(0)

/** Returns the current theme. Re-composes when settings change. */
@Composable
fun rememberAppTheme(): AppTheme {
    val primary = AppState.getThemeColor()
    val isDark = AppState.isDarkMode()
    val accent = Color(0xFFCD2E3A)
    // Read themeRevision so we re-compose when it changes
    val rev = themeRevision.value
    return remember(primary, isDark, rev) { AppTheme(primary, accent, isDark) }
}

/** Call after updating any setting to force the UI to re-render with new theme. */
fun notifySettingsChanged() {
    themeRevision.value++
}

// ─── Animation helpers ─────────────────────────────────────────────────────────

/** Fade-in + slide-up animation for content entering the screen. */
@Composable
fun Modifier.fadeInSlideUp(
    delayMs: Int = 0,
    enabled: Boolean = true
): Modifier {
    if (!enabled || !AppState.areAnimationsEnabled()) return this
    val transition = rememberInfiniteTransition(label = "fadeIn")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.alpha(1f) // simplification — full enter animation handled by AnimatedVisibility
}

// ─── Skeleton loaders ──────────────────────────────────────────────────────────

/** Shimmer effect for skeleton placeholders. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    theme: AppTheme? = null
) {
    val t = theme ?: rememberAppTheme()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnim"
    )
    val baseColor = t.midGray
    val highlightColor = lerp(baseColor, t.divider, translateAnim)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(highlightColor)
    )
}

/** Skeleton placeholder for a list item (icon + 2 text lines). */
@Composable
fun SkeletonListItem(theme: AppTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(44.dp), cornerRadius = 10.dp, theme = theme)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), theme = theme)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(11.dp), theme = theme)
        }
    }
}

/** Skeleton placeholder for a card with image + title. */
@Composable
fun SkeletonCard(theme: AppTheme, imageHeight: Dp = 100.dp) {
    Surface(
        color = theme.cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().height(imageHeight),
                cornerRadius = 8.dp,
                theme = theme
            )
            Spacer(Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp), theme = theme)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp), theme = theme)
        }
    }
}

/** Skeleton placeholder for a stats grid. */
@Composable
fun SkeletonStatsRow(theme: AppTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            ShimmerBox(
                modifier = Modifier.weight(1f).height(70.dp),
                cornerRadius = 12.dp,
                theme = theme
            )
        }
    }
}

/** Full skeleton screen for a list-based tab. */
@Composable
fun SkeletonListScreen(theme: AppTheme, itemCount: Int = 6) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(0.5f).height(24.dp),
            cornerRadius = 4.dp,
            theme = theme
        )
        Spacer(Modifier.height(16.dp))
        Surface(color = theme.cardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
            Column {
                repeat(itemCount) {
                    SkeletonListItem(theme)
                    if (it < itemCount - 1) {
                        Divider(color = theme.divider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

/** Full skeleton screen for a grid-based tab. */
@Composable
fun SkeletonGridScreen(theme: AppTheme, rows: Int = 3) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(0.4f).height(24.dp),
            cornerRadius = 4.dp,
            theme = theme
        )
        Spacer(Modifier.height(16.dp))
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    ShimmerBox(
                        modifier = Modifier.weight(1f).height(160.dp),
                        cornerRadius = 12.dp,
                        theme = theme
                    )
                }
            }
        }
    }
}
