package com.tabgl.haven.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tabgl.haven.data.LauncherSettings

private data class HavenPalette(
    val primary: Color, val secondary: Color, val tertiary: Color,
    val lightBackground: Color, val lightSurface: Color, val lightVariant: Color,
    val darkBackground: Color, val darkSurface: Color, val darkVariant: Color
)

private fun palette(name: String): HavenPalette = when (name) {
    "ocean" -> HavenPalette(
        Color(0xFF006A70), Color(0xFF4A6265), Color(0xFF49617C),
        Color(0xFFF1FAFA), Color(0xFFFAFFFF), Color(0xFFDCEBEC),
        Color(0xFF0D1F20), Color(0xFF142729), Color(0xFF263B3D)
    )
    "sunset" -> HavenPalette(
        Color(0xFF9C432F), Color(0xFF77574F), Color(0xFF765A20),
        Color(0xFFFFF8F5), Color(0xFFFFFBF9), Color(0xFFF4E2DC),
        Color(0xFF241714), Color(0xFF30201C), Color(0xFF49332D)
    )
    "lavender" -> HavenPalette(
        Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF80558C),
        Color(0xFFFAF7FF), Color(0xFFFFFBFF), Color(0xFFEAE2F3),
        Color(0xFF1D1925), Color(0xFF292333), Color(0xFF393344)
    )
    "mono" -> HavenPalette(
        Color(0xFF555B62), Color(0xFF5C6067), Color(0xFF6B5D4D),
        Color(0xFFF6F6F6), Color(0xFFFFFFFF), Color(0xFFE7E7E7),
        Color(0xFF111315), Color(0xFF1B1E20), Color(0xFF2A2E31)
    )
    else -> HavenPalette(
        Color(0xFF526600), Color(0xFF596248), Color(0xFF006B5D),
        Color(0xFFF7F9ED), Color(0xFFFFFEF7), Color(0xFFE9EED8),
        Color(0xFF171B0D), Color(0xFF202516), Color(0xFF303724)
    )
}

/** The lime Haven has always used as its signature accent. */
private val ACCENT_LIME = Color(0xFFC9FF42)

/**
 * A named accent colour, or null for "auto" — follow the palette instead. Accents now apply to
 * every palette, not just Aurora.
 */
fun accentColor(name: String): Color? = when (name) {
    "lime" -> ACCENT_LIME
    "blue" -> Color(0xFF8AB4F8)
    "coral" -> Color(0xFFFF8A80)
    "violet" -> Color(0xFFD0BCFF)
    else -> null
}

/** Preview swatch used by the settings palette chips. */
fun paletteSwatch(name: String): Color =
    if (name == "aurora") ACCENT_LIME else palette(name).primary

private val HavenShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

/**
 * Picks an accent that stays readable as text and controls on the current background.
 *
 * An explicitly chosen accent wins on every palette; "auto" keeps Aurora's lime signature and
 * lets the other palettes speak for themselves.
 */
private fun primaryFor(settings: LauncherSettings, p: HavenPalette, dark: Boolean): Color {
    val explicit = accentColor(settings.accent)
    if (explicit != null) return if (dark) explicit else lerp(explicit, Color.Black, 0.55f)
    val signature = settings.palette == "aurora"
    val base = if (signature) ACCENT_LIME else p.primary
    return when {
        dark && signature -> base
        dark -> lerp(base, Color.White, 0.5f)
        signature -> lerp(base, Color.Black, 0.55f)
        else -> base
    }
}

private fun readableOn(color: Color): Color =
    if (color.luminance() > 0.5f) Color(0xFF101410) else Color.White

private fun havenScheme(p: HavenPalette, dark: Boolean, primary: Color, amoled: Boolean): ColorScheme {
    val bg = when {
        dark && amoled -> Color.Black
        dark -> p.darkBackground
        else -> p.lightBackground
    }
    val surface = when {
        dark && amoled -> Color(0xFF08090A)
        dark -> p.darkSurface
        else -> p.lightSurface
    }
    val variant = when {
        dark && amoled -> Color(0xFF1B1D1F)
        dark -> p.darkVariant
        else -> p.lightVariant
    }
    val onSurface = if (dark) Color(0xFFE5E2DC) else Color(0xFF191C18)
    val onVariant = if (dark) Color(0xFFBEC8C4) else Color(0xFF454A43)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = lerp(bg, primary, 0.32f),
        onPrimaryContainer = onSurface,
        secondary = p.secondary,
        onSecondary = readableOn(p.secondary),
        secondaryContainer = variant,
        onSecondaryContainer = onSurface,
        tertiary = p.tertiary,
        onTertiary = readableOn(p.tertiary),
        background = bg,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = variant,
        onSurfaceVariant = onVariant,
        surfaceContainerLowest = bg,
        surfaceContainerLow = lerp(bg, surface, 0.5f),
        surfaceContainer = surface,
        surfaceContainerHigh = lerp(surface, variant, 0.5f),
        surfaceContainerHighest = variant,
        outline = onVariant.copy(alpha = 0.6f),
        outlineVariant = variant
    )
}

@Composable
private fun animated(color: Color): Color =
    animateColorAsState(color, spring(stiffness = 420f), label = "haven-color").value

@Composable
fun HavenTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val dynamic = settings.monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val target = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> {
            val p = palette(settings.palette)
            havenScheme(p, dark, primaryFor(settings, p, dark), settings.amoled)
        }
    }
    val scheme = target.copy(
        primary = animated(target.primary),
        primaryContainer = animated(target.primaryContainer),
        secondary = animated(target.secondary),
        secondaryContainer = animated(target.secondaryContainer),
        tertiary = animated(target.tertiary),
        background = animated(target.background),
        surface = animated(target.surface),
        surfaceVariant = animated(target.surfaceVariant),
        surfaceContainerLow = animated(target.surfaceContainerLow),
        surfaceContainer = animated(target.surfaceContainer),
        surfaceContainerHigh = animated(target.surfaceContainerHigh),
        surfaceContainerHighest = animated(target.surfaceContainerHighest)
    )
    SystemBarIcons(dark)
    MaterialTheme(
        colorScheme = scheme,
        shapes = HavenShapes,
        content = content
    )
}

/** Keeps status/navigation bar icons legible against the launcher's own (possibly forced) theme. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
