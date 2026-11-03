package com.tabgl.haven.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.havenDataStore by preferencesDataStore(name = "haven_preferences")

data class LauncherSettings(
    val themeMode: String = "system",
    val monetEnabled: Boolean = true,
    val accent: String = "lime",
    val palette: String = "aurora",
    val iconShape: String = "squircle",
    val columns: Int = 4,
    val showLabels: Boolean = true,
    val compactIcons: Boolean = false
)

class LauncherPreferences(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val monetEnabled = booleanPreferencesKey("monet_enabled")
        val accent = stringPreferencesKey("accent")
        val palette = stringPreferencesKey("palette")
        val iconShape = stringPreferencesKey("icon_shape")
        val columns = intPreferencesKey("columns")
        val showLabels = booleanPreferencesKey("show_labels")
        val compactIcons = booleanPreferencesKey("compact_icons")
    }

    val settings = context.havenDataStore.data.map { prefs ->
        LauncherSettings(
            themeMode = prefs[Keys.themeMode] ?: "system",
            monetEnabled = prefs[Keys.monetEnabled] ?: true,
            accent = prefs[Keys.accent] ?: "lime",
            palette = prefs[Keys.palette] ?: "aurora",
            iconShape = prefs[Keys.iconShape] ?: "squircle",
            columns = (prefs[Keys.columns] ?: 4).coerceIn(3, 6),
            showLabels = prefs[Keys.showLabels] ?: true,
            compactIcons = prefs[Keys.compactIcons] ?: false
        )
    }

    suspend fun setThemeMode(value: String) = context.havenDataStore.edit {
        it[Keys.themeMode] = value.takeIf { v -> v in setOf("system", "light", "dark") } ?: "system"
    }
    suspend fun setMonetEnabled(value: Boolean) = context.havenDataStore.edit { it[Keys.monetEnabled] = value }
    suspend fun setAccent(value: String) = context.havenDataStore.edit {
        it[Keys.accent] = value.takeIf { v -> v in setOf("lime", "blue", "coral", "violet") } ?: "lime"
    }
    suspend fun setPalette(value: String) = context.havenDataStore.edit {
        it[Keys.palette] = value.takeIf { v -> v in setOf("aurora", "ocean", "sunset", "lavender", "mono") } ?: "aurora"
    }
    suspend fun setIconShape(value: String) = context.havenDataStore.edit {
        it[Keys.iconShape] = value.takeIf { v -> v in setOf("squircle", "circle", "rounded", "square") } ?: "squircle"
    }
    suspend fun setColumns(value: Int) = context.havenDataStore.edit { it[Keys.columns] = value.coerceIn(3, 6) }
    suspend fun setShowLabels(value: Boolean) = context.havenDataStore.edit { it[Keys.showLabels] = value }
    suspend fun setCompactIcons(value: Boolean) = context.havenDataStore.edit { it[Keys.compactIcons] = value }
}
