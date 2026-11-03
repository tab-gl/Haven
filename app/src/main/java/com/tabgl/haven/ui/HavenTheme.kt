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

private data class HavenPalette(
    val primary: Color, val secondary: Color, val tertiary: Color,
    val lightBackground: Color, val lightSurface: Color, val lightVariant: Color,
    val darkBackground: Color, val darkSurface: Color, val darkVariant: Color
)

private fun palette(name: String): HavenPalette = when (name) {
    "ocean" -> HavenPalette(Color(0xFF006A70), Color(0xFF4A6265), Color(0xFF49617C),
        Color(0xFFF1FAFA), Color(0xFFFAFFFF), Color(0xFFDCEBEC),
        Color(0xFF0D1F20), Color(0xFF142729), Color(0xFF263B3D))
    "sunset" -> HavenPalette(Color(0xFF9C432F), Color(0xFF77574F), Color(0xFF765A20),
        Color(0xFFFFF8F5), Color(0xFFFFFBF9), Color(0xFFF4E2DC),
        Color(0xFF241714), Color(0xFF30201C), Color(0xFF49332D))
    "lavender" -> HavenPalette(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF80558C),
        Color(0xFFFAF7FF), Color(0xFFFFFBFF), Color(0xFFEAE2F3),
        Color(0xFF1D1925), Color(0xFF292333), Color(0xFF393344))
    "mono" -> HavenPalette(Color(0xFF555B62), Color(0xFF5C6067), Color(0xFF6B5D4D),
        Color(0xFFF6F6F6), Color(0xFFFFFFFF), Color(0xFFE7E7E7),
        Color(0xFF111315), Color(0xFF1B1E20), Color(0xFF2A2E31))
    else -> HavenPalette(Color(0xFF526600), Color(0xFF596248), Color(0xFF006B5D),
        Color(0xFFF7F9ED), Color(0xFFFFFEF7), Color(0xFFE9EED8),
        Color(0xFF171B0D), Color(0xFF202516), Color(0xFF303724))
}

private fun accent(name: String): Color = when (name) {
    "blue" -> Color(0xFF8AB4F8)
    "coral" -> Color(0xFFFF8A80)
    "violet" -> Color(0xFFD0BCFF)
    else -> Color(0xFFC9FF42)
}

@Composable
fun HavenTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val p = palette(settings.palette)
    val dynamic = settings.monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val target = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = if (settings.palette == "aurora") accent(settings.accent) else p.primary,
            secondary = p.secondary, tertiary = p.tertiary,
            background = p.darkBackground, surface = p.darkSurface,
            surfaceVariant = p.darkVariant, onBackground = Color(0xFFE5E2DC),
            onSurface = Color(0xFFE5E2DC), onSurfaceVariant = Color(0xFFBEC8C4)
        )
        else -> lightColorScheme(
            primary = if (settings.palette == "aurora") accent(settings.accent) else p.primary,
            secondary = p.secondary, tertiary = p.tertiary,
            background = p.lightBackground, surface = p.lightSurface,
            surfaceVariant = p.lightVariant, onBackground = Color(0xFF191C18),
            onSurface = Color(0xFF191C18), onSurfaceVariant = Color(0xFF454A43)
        )
    }
    val primary by animateColorAsState(target.primary, spring(stiffness = 420f), label = "Palette primary")
    val secondary by animateColorAsState(target.secondary, spring(stiffness = 420f), label = "Palette secondary")
    val tertiary by animateColorAsState(target.tertiary, spring(stiffness = 420f), label = "Palette tertiary")
    val background by animateColorAsState(target.background, spring(stiffness = 420f), label = "Theme background")
    val surface by animateColorAsState(target.surface, spring(stiffness = 420f), label = "Theme surface")
    val variant by animateColorAsState(target.surfaceVariant, spring(stiffness = 420f), label = "Theme variant")
    val animated = target.copy(
        primary = primary, secondary = secondary, tertiary = tertiary,
        background = background, surface = surface, surfaceVariant = variant
    )
    MaterialTheme(colorScheme = animated, content = content)
}
