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
    val columns: Int = 4,
    val showLabels: Boolean = true,
    val compactIcons: Boolean = false
)

class LauncherPreferences(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val monetEnabled = booleanPreferencesKey("monet_enabled")
        val accent = stringPreferencesKey("accent")
        val columns = intPreferencesKey("columns")
        val showLabels = booleanPreferencesKey("show_labels")
        val compactIcons = booleanPreferencesKey("compact_icons")
    }

    val settings = context.havenDataStore.data.map { prefs ->
        LauncherSettings(
            themeMode = prefs[Keys.themeMode] ?: "system",
            monetEnabled = prefs[Keys.monetEnabled] ?: true,
            accent = prefs[Keys.accent] ?: "lime",
            columns = (prefs[Keys.columns] ?: 4).coerceIn(3, 6),
            showLabels = prefs[Keys.showLabels] ?: true,
            compactIcons = prefs[Keys.compactIcons] ?: false
        )
    }

    suspend fun setThemeMode(value: String) = context.havenDataStore.edit { it[Keys.themeMode] = value }
    suspend fun setMonetEnabled(value: Boolean) = context.havenDataStore.edit { it[Keys.monetEnabled] = value }
    suspend fun setAccent(value: String) = context.havenDataStore.edit { it[Keys.accent] = value }
    suspend fun setColumns(value: Int) = context.havenDataStore.edit { it[Keys.columns] = value.coerceIn(3, 6) }
    suspend fun setShowLabels(value: Boolean) = context.havenDataStore.edit { it[Keys.showLabels] = value }
    suspend fun setCompactIcons(value: Boolean) = context.havenDataStore.edit { it[Keys.compactIcons] = value }
}
