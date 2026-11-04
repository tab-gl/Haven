package com.tabgl.haven.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherPreferences
import com.tabgl.haven.data.LauncherSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: LauncherSettings,
    hiddenApps: List<LauncherApp>,
    preferences: LauncherPreferences,
    versionName: String
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val monetAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val monetActive = monetAvailable && settings.monetEnabled
    var columns by remember(settings.columns) { mutableFloatStateOf(settings.columns.toFloat()) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Make it yours", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Appearance")
        Segmented(
            options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
            selected = settings.themeMode
        ) { value -> scope.launch { preferences.setThemeMode(value) } }

        if (monetAvailable) {
            SwitchRow("Monet colours", "Follow your wallpaper's colours", settings.monetEnabled) {
                scope.launch { preferences.setMonetEnabled(it) }
            }
        }

        SectionTitle("Palette")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "aurora" to "Aurora", "ocean" to "Ocean", "sunset" to "Sunset",
                "lavender" to "Lavender", "mono" to "Mono"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = !monetActive && settings.palette == value,
                    onClick = {
                        scope.launch {
                            preferences.setPalette(value)
                            preferences.setMonetEnabled(false)
                        }
                    },
                    label = { Text(label) },
                    leadingIcon = {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(paletteSwatch(value)))
                    }
                )
            }
        }
        if (!monetActive && settings.palette == "aurora") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("lime", "blue", "coral", "violet").forEach { name ->
                    val selected = settings.accent == name
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor(name))
                            .then(
                                if (selected) Modifier.border(3.dp, colors.onSurface, CircleShape) else Modifier
                            )
                            .semantics { contentDescription = "$name accent" }
                            .clickable(role = Role.RadioButton) {
                                scope.launch { preferences.setAccent(name) }
                            }
                    )
                }
            }
        }
        SwitchRow("Show wallpaper", "Let your wallpaper through the home screen", settings.showWallpaper) {
            scope.launch { preferences.setShowWallpaper(it) }
        }

        SectionTitle("Icons")
        Segmented(
            options = listOf("squircle" to "Squircle", "circle" to "Circle", "rounded" to "Round", "square" to "Cut"),
            selected = settings.iconShape
        ) { value -> scope.launch { preferences.setIconShape(value) } }
        SwitchRow("App labels", "Show names below icons", settings.showLabels) {
            scope.launch { preferences.setShowLabels(it) }
        }
        SwitchRow("Compact icons", "Smaller icons, more breathing room", settings.compactIcons) {
            scope.launch { preferences.setCompactIcons(it) }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("App grid", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "${columns.roundToInt()} columns",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Slider(
                value = columns,
                onValueChange = { columns = it },
                onValueChangeFinished = { scope.launch { preferences.setColumns(columns.roundToInt()) } },
                valueRange = 3f..6f,
                steps = 2
            )
        }

        if (hiddenApps.isNotEmpty()) {
            SectionTitle("Hidden apps")
            hiddenApps.forEach { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { scope.launch { preferences.unhide(app.key) } }) { Text("Show") }
                }
            }
        }

        Text(
            "Haven $versionName · long-press apps to pin, hide or uninstall",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.first == selected,
                onClick = { onSelect(option.first) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {},
                label = { Text(option.second, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
