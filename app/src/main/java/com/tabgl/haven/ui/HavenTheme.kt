package com.tabgl.haven.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tabgl.haven.data.LauncherSettings

private val Obsidian = Color(0xFF101114)
private val Graphite = Color(0xFF1B1D21)
private val Porcelain = Color(0xFFF1F0EC)
private val Blue = Color(0xFF8AB4F8)
private val Coral = Color(0xFFFF8A80)

@Composable
fun HavenTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val targetScheme = when {
        settings.monetEnabled && dynamicAvailable && dark -> dynamicDarkColorScheme(context)
        settings.monetEnabled && dynamicAvailable -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = accent(settings.accent), onPrimary = Obsidian,
            secondary = Color(0xFFBFC8D8), background = Obsidian,
            surface = Graphite, surfaceVariant = Color(0xFF292C32),
            onBackground = Porcelain, onSurface = Porcelain,
            onSurfaceVariant = Color(0xFFB7BAC2)
        )
        else -> lightColorScheme(
            primary = accent(settings.accent), onPrimary = Color(0xFF182000),
            secondary = Color(0xFF52606F), background = Color(0xFFF7F6F2),
            surface = Color(0xFFFFFEFA), surfaceVariant = Color(0xFFE9E7E0),
            onBackground = Color(0xFF191B18), onSurface = Color(0xFF191B18)
        )
    }
    val primary by animateColorAsState(targetScheme.primary, spring(stiffness = 360f), label = "Monet primary")
    val background by animateColorAsState(targetScheme.background, spring(stiffness = 360f), label = "Theme background")
    val surface by animateColorAsState(targetScheme.surface, spring(stiffness = 360f), label = "Theme surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, spring(stiffness = 360f), label = "Theme foreground")
    val animatedScheme = targetScheme.copy(primary = primary, background = background, surface = surface, onSurface = onSurface)
    MaterialTheme(colorScheme = animatedScheme, content = content)
}

fun accent(name: String): Color = when (name) {
    "blue" -> Blue
    "coral" -> Coral
    "violet" -> Color(0xFFD0BCFF)
    else -> Color(0xFFC9FF42)
}
