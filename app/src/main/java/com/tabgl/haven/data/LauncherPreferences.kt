package com.tabgl.haven.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.havenDataStore by preferencesDataStore(name = "haven_preferences")

/**
 * Everything the launcher lets you change. Values are validated on read *and* on write, so a
 * corrupted or downgraded preferences file can never put the UI in an impossible state.
 */
data class LauncherSettings(
    val themeMode: String = "system",
    val monetEnabled: Boolean = true,
    val amoled: Boolean = false,
    /** "auto" follows the palette; otherwise one of the accent names in [LauncherPreferences.ACCENTS]. */
    val accent: String = "auto",
    val palette: String = "aurora",
    val iconShape: String = "squircle",
    /** "masked" (Haven's shape) | "natural" (as the system draws it) | "plate" (inset on a tinted plate). */
    val iconStyle: String = "masked",
    /** "compact" | "normal" | "large" */
    val iconScale: String = "normal",
    val columns: Int = 4,
    val labelLines: Int = 1,
    val showLabels: Boolean = true,
    val showWallpaper: Boolean = true,
    /** 0..85 — how much of the wallpaper is covered by the theme scrim. */
    val wallpaperDim: Int = 50,
    val showBrand: Boolean = true,
    val showClock: Boolean = true,
    /** "compact" | "normal" | "large" */
    val clockScale: String = "normal",
    val showSearchPill: Boolean = true,
    val showFrequent: Boolean = true,
    val frequentCount: Int = 4,
    val dockSlots: Int = 4,
    val dockBackground: Boolean = true,
    /** 60..100 — opacity of the app drawer over the wallpaper. */
    val drawerOpacity: Int = 94,
    val animations: Boolean = true,
    /** "zoom" | "fade" | "none" */
    val motionStyle: String = "zoom",
    /** "fast" | "normal" | "calm" */
    val motionSpeed: String = "normal",
    val haptics: Boolean = true,
    /** "google" | "duckduckgo" | "brave" */
    val searchEngine: String = "google",
    /** Component keys ("package/activity") hidden from the drawer. */
    val hidden: Set<String> = emptySet(),
    /** Ordered dock; null means "use the default dock" (dialer, messages, camera, browser). */
    val pinned: List<String>? = null,
    /** Launch counts per component key; drives the Frequent row. */
    val usage: Map<String, Int> = emptyMap()
)

class LauncherPreferences(context: Context) {
    private val store = context.applicationContext.havenDataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val monetEnabled = booleanPreferencesKey("monet_enabled")
        val amoled = booleanPreferencesKey("amoled")
        val accent = stringPreferencesKey("accent")
        val palette = stringPreferencesKey("palette")
        val iconShape = stringPreferencesKey("icon_shape")
        val iconStyle = stringPreferencesKey("icon_style")
        val iconScale = stringPreferencesKey("icon_scale")
        val legacyCompactIcons = booleanPreferencesKey("compact_icons")
        val columns = intPreferencesKey("columns")
        val labelLines = intPreferencesKey("label_lines")
        val showLabels = booleanPreferencesKey("show_labels")
        val showWallpaper = booleanPreferencesKey("show_wallpaper")
        val wallpaperDim = intPreferencesKey("wallpaper_dim")
        val showBrand = booleanPreferencesKey("show_brand")
        val showClock = booleanPreferencesKey("show_clock")
        val clockScale = stringPreferencesKey("clock_scale")
        val showSearchPill = booleanPreferencesKey("show_search_pill")
        val showFrequent = booleanPreferencesKey("show_frequent")
        val frequentCount = intPreferencesKey("frequent_count")
        val dockSlots = intPreferencesKey("dock_slots")
        val dockBackground = booleanPreferencesKey("dock_background")
        val drawerOpacity = intPreferencesKey("drawer_opacity")
        val animations = booleanPreferencesKey("animations")
        val motionStyle = stringPreferencesKey("motion_style")
        val motionSpeed = stringPreferencesKey("motion_speed")
        val haptics = booleanPreferencesKey("haptics")
        val searchEngine = stringPreferencesKey("search_engine")
        val hidden = stringSetPreferencesKey("hidden_apps")
        val pinned = stringPreferencesKey("pinned_apps")
        val usage = stringPreferencesKey("usage")
    }

    val settings: Flow<LauncherSettings> = store.data.map { prefs ->
        LauncherSettings(
            themeMode = prefs[Keys.themeMode].oneOf(THEME_MODES, "system"),
            monetEnabled = prefs[Keys.monetEnabled] ?: true,
            amoled = prefs[Keys.amoled] ?: false,
            accent = prefs[Keys.accent].oneOf(ACCENTS, "auto"),
            palette = prefs[Keys.palette].oneOf(PALETTES, "aurora"),
            iconShape = prefs[Keys.iconShape].oneOf(SHAPES, "squircle"),
            iconStyle = prefs[Keys.iconStyle].oneOf(ICON_STYLES, "masked"),
            iconScale = prefs[Keys.iconScale].oneOf(SCALES, "normal").let { scale ->
                // 0.5.x only had a boolean "compact icons" toggle.
                if (prefs[Keys.iconScale] == null && prefs[Keys.legacyCompactIcons] == true) "compact" else scale
            },
            columns = (prefs[Keys.columns] ?: 4).coerceIn(3, 6),
            labelLines = (prefs[Keys.labelLines] ?: 1).coerceIn(1, 2),
            showLabels = prefs[Keys.showLabels] ?: true,
            showWallpaper = prefs[Keys.showWallpaper] ?: true,
            wallpaperDim = (prefs[Keys.wallpaperDim] ?: 50).coerceIn(0, 85),
            showBrand = prefs[Keys.showBrand] ?: true,
            showClock = prefs[Keys.showClock] ?: true,
            clockScale = prefs[Keys.clockScale].oneOf(SCALES, "normal"),
            showSearchPill = prefs[Keys.showSearchPill] ?: true,
            showFrequent = prefs[Keys.showFrequent] ?: true,
            frequentCount = (prefs[Keys.frequentCount] ?: 4).coerceIn(0, 5),
            dockSlots = (prefs[Keys.dockSlots] ?: 4).coerceIn(3, 5),
            dockBackground = prefs[Keys.dockBackground] ?: true,
            drawerOpacity = (prefs[Keys.drawerOpacity] ?: 94).coerceIn(60, 100),
            animations = prefs[Keys.animations] ?: true,
            motionStyle = prefs[Keys.motionStyle].oneOf(MOTION_STYLES, "zoom"),
            motionSpeed = prefs[Keys.motionSpeed].oneOf(MOTION_SPEEDS, "normal"),
            haptics = prefs[Keys.haptics] ?: true,
            searchEngine = prefs[Keys.searchEngine].oneOf(SEARCH_ENGINES, "google"),
            hidden = prefs[Keys.hidden] ?: emptySet(),
            pinned = prefs[Keys.pinned]?.let { decodeList(it) },
            usage = UsageCodec.decode(prefs[Keys.usage])
        )
    }

    /** One-shot read used before the first frame so the launcher never flashes the default theme. */
    fun readBlocking(): LauncherSettings =
        runCatching { runBlocking { settings.first() } }.getOrDefault(LauncherSettings())

    suspend fun setThemeMode(value: String) = store.edit { it[Keys.themeMode] = value.oneOf(THEME_MODES, "system") }
    suspend fun setMonetEnabled(value: Boolean) = store.edit { it[Keys.monetEnabled] = value }
    suspend fun setAmoled(value: Boolean) = store.edit { it[Keys.amoled] = value }
    suspend fun setAccent(value: String) = store.edit { it[Keys.accent] = value.oneOf(ACCENTS, "auto") }
    suspend fun setPalette(value: String) = store.edit { it[Keys.palette] = value.oneOf(PALETTES, "aurora") }
    suspend fun setIconShape(value: String) = store.edit { it[Keys.iconShape] = value.oneOf(SHAPES, "squircle") }
    suspend fun setIconStyle(value: String) = store.edit { it[Keys.iconStyle] = value.oneOf(ICON_STYLES, "masked") }
    suspend fun setIconScale(value: String) = store.edit { it[Keys.iconScale] = value.oneOf(SCALES, "normal") }
    suspend fun setColumns(value: Int) = store.edit { it[Keys.columns] = value.coerceIn(3, 6) }
    suspend fun setLabelLines(value: Int) = store.edit { it[Keys.labelLines] = value.coerceIn(1, 2) }
    suspend fun setShowLabels(value: Boolean) = store.edit { it[Keys.showLabels] = value }
    suspend fun setShowWallpaper(value: Boolean) = store.edit { it[Keys.showWallpaper] = value }
    suspend fun setWallpaperDim(value: Int) = store.edit { it[Keys.wallpaperDim] = value.coerceIn(0, 85) }
    suspend fun setShowBrand(value: Boolean) = store.edit { it[Keys.showBrand] = value }
    suspend fun setShowClock(value: Boolean) = store.edit { it[Keys.showClock] = value }
    suspend fun setClockScale(value: String) = store.edit { it[Keys.clockScale] = value.oneOf(SCALES, "normal") }
    suspend fun setShowSearchPill(value: Boolean) = store.edit { it[Keys.showSearchPill] = value }
    suspend fun setShowFrequent(value: Boolean) = store.edit { it[Keys.showFrequent] = value }
    suspend fun setFrequentCount(value: Int) = store.edit { it[Keys.frequentCount] = value.coerceIn(0, 5) }
    suspend fun setDockSlots(value: Int) = store.edit { it[Keys.dockSlots] = value.coerceIn(3, 5) }
    suspend fun setDockBackground(value: Boolean) = store.edit { it[Keys.dockBackground] = value }
    suspend fun setDrawerOpacity(value: Int) = store.edit { it[Keys.drawerOpacity] = value.coerceIn(60, 100) }
    suspend fun setAnimations(value: Boolean) = store.edit { it[Keys.animations] = value }
    suspend fun setMotionStyle(value: String) = store.edit { it[Keys.motionStyle] = value.oneOf(MOTION_STYLES, "zoom") }
    suspend fun setMotionSpeed(value: String) = store.edit { it[Keys.motionSpeed] = value.oneOf(MOTION_SPEEDS, "normal") }
    suspend fun setHaptics(value: Boolean) = store.edit { it[Keys.haptics] = value }
    suspend fun setSearchEngine(value: String) = store.edit { it[Keys.searchEngine] = value.oneOf(SEARCH_ENGINES, "google") }

    suspend fun setPinned(keys: List<String>) = store.edit {
        it[Keys.pinned] = keys.distinct().take(MAX_PINNED).joinToString("\n")
    }

    suspend fun hide(key: String) = store.edit { p ->
        p[Keys.hidden] = (p[Keys.hidden] ?: emptySet()) + key
        p[Keys.pinned]?.let { raw ->
            p[Keys.pinned] = decodeList(raw).filterNot { it == key }.joinToString("\n")
        }
    }

    suspend fun unhide(key: String) = store.edit { p ->
        p[Keys.hidden] = (p[Keys.hidden] ?: emptySet()) - key
    }

    suspend fun recordLaunch(key: String) = store.edit { p ->
        p[Keys.usage] = UsageCodec.encode(UsageCodec.bump(UsageCodec.decode(p[Keys.usage]), key))
    }

    /** Back to factory settings; hidden apps and the custom dock are cleared too. */
    suspend fun resetAll() = store.edit { it.clear() }

    companion object {
        /** The dock is capped at the largest dock the UI offers. */
        const val MAX_PINNED = 5

        val THEME_MODES = setOf("system", "light", "dark")
        val ACCENTS = setOf("auto", "lime", "blue", "coral", "violet")
        val PALETTES = setOf("aurora", "ocean", "sunset", "lavender", "mono")
        val SHAPES = setOf(
            "squircle", "circle", "rounded", "square", "teardrop", "pebble", "arch", "hexagon"
        )
        val ICON_STYLES = setOf("masked", "natural", "plate")
        val SCALES = setOf("compact", "normal", "large")
        val MOTION_STYLES = setOf("zoom", "fade", "none")
        val MOTION_SPEEDS = setOf("fast", "normal", "calm")
        val SEARCH_ENGINES = setOf("google", "duckduckgo", "brave")

        private fun decodeList(raw: String): List<String> = raw.split('\n').filter { it.isNotBlank() }

        private fun String?.oneOf(allowed: Set<String>, fallback: String): String =
            if (this != null && this in allowed) this else fallback
    }
}
