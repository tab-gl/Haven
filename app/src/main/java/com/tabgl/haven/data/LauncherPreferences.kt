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

data class LauncherSettings(
    val themeMode: String = "system",
    val monetEnabled: Boolean = true,
    val accent: String = "lime",
    val palette: String = "aurora",
    val iconShape: String = "squircle",
    val columns: Int = 4,
    val showLabels: Boolean = true,
    val compactIcons: Boolean = false,
    val showWallpaper: Boolean = true,
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
        val accent = stringPreferencesKey("accent")
        val palette = stringPreferencesKey("palette")
        val iconShape = stringPreferencesKey("icon_shape")
        val columns = intPreferencesKey("columns")
        val showLabels = booleanPreferencesKey("show_labels")
        val compactIcons = booleanPreferencesKey("compact_icons")
        val showWallpaper = booleanPreferencesKey("show_wallpaper")
        val hidden = stringSetPreferencesKey("hidden_apps")
        val pinned = stringPreferencesKey("pinned_apps")
        val usage = stringPreferencesKey("usage")
    }

    val settings: Flow<LauncherSettings> = store.data.map { prefs ->
        LauncherSettings(
            themeMode = prefs[Keys.themeMode]?.takeIf { it in THEME_MODES } ?: "system",
            monetEnabled = prefs[Keys.monetEnabled] ?: true,
            accent = prefs[Keys.accent]?.takeIf { it in ACCENTS } ?: "lime",
            palette = prefs[Keys.palette]?.takeIf { it in PALETTES } ?: "aurora",
            iconShape = prefs[Keys.iconShape]?.takeIf { it in SHAPES } ?: "squircle",
            columns = (prefs[Keys.columns] ?: 4).coerceIn(3, 6),
            showLabels = prefs[Keys.showLabels] ?: true,
            compactIcons = prefs[Keys.compactIcons] ?: false,
            showWallpaper = prefs[Keys.showWallpaper] ?: true,
            hidden = prefs[Keys.hidden] ?: emptySet(),
            pinned = prefs[Keys.pinned]?.let { decodeList(it) },
            usage = UsageCodec.decode(prefs[Keys.usage])
        )
    }

    /** One-shot read used before the first frame so the launcher never flashes the default theme. */
    fun readBlocking(): LauncherSettings =
        runCatching { runBlocking { settings.first() } }.getOrDefault(LauncherSettings())

    suspend fun setThemeMode(value: String) = store.edit {
        it[Keys.themeMode] = value.takeIf { v -> v in THEME_MODES } ?: "system"
    }

    suspend fun setMonetEnabled(value: Boolean) = store.edit { it[Keys.monetEnabled] = value }

    suspend fun setAccent(value: String) = store.edit {
        it[Keys.accent] = value.takeIf { v -> v in ACCENTS } ?: "lime"
    }

    suspend fun setPalette(value: String) = store.edit {
        it[Keys.palette] = value.takeIf { v -> v in PALETTES } ?: "aurora"
    }

    suspend fun setIconShape(value: String) = store.edit {
        it[Keys.iconShape] = value.takeIf { v -> v in SHAPES } ?: "squircle"
    }

    suspend fun setColumns(value: Int) = store.edit { it[Keys.columns] = value.coerceIn(3, 6) }
    suspend fun setShowLabels(value: Boolean) = store.edit { it[Keys.showLabels] = value }
    suspend fun setCompactIcons(value: Boolean) = store.edit { it[Keys.compactIcons] = value }
    suspend fun setShowWallpaper(value: Boolean) = store.edit { it[Keys.showWallpaper] = value }

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

    companion object {
        const val MAX_PINNED = 4
        private val THEME_MODES = setOf("system", "light", "dark")
        private val ACCENTS = setOf("lime", "blue", "coral", "violet")
        private val PALETTES = setOf("aurora", "ocean", "sunset", "lavender", "mono")
        private val SHAPES = setOf("squircle", "circle", "rounded", "square")

        private fun decodeList(raw: String): List<String> = raw.split('\n').filter { it.isNotBlank() }
    }
}
