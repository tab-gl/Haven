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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherPreferences
import com.tabgl.haven.data.LauncherSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PALETTES = listOf(
    "aurora" to "Aurora",
    "ocean" to "Ocean",
    "sunset" to "Sunset",
    "lavender" to "Lavender",
    "mono" to "Mono"
)

private val ACCENTS = listOf(
    "auto" to "Auto",
    "lime" to "Lime",
    "blue" to "Blue",
    "coral" to "Coral",
    "violet" to "Violet"
)

private val ICON_SCALES = listOf("compact" to "Small", "normal" to "Normal", "large" to "Large")

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
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Every change applies live",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        // ---- Appearance -------------------------------------------------------
        SectionHeader("Appearance")
        Segmented(
            options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
            selected = settings.themeMode
        ) { value -> scope.launch { preferences.setThemeMode(value) } }

        SwitchRow(
            title = "Pure black",
            subtitle = "True-black surfaces in dark mode",
            checked = settings.amoled,
            enabled = settings.themeMode != "light",
            onChange = { scope.launch { preferences.setAmoled(it) } }
        )
        if (monetAvailable) {
            SwitchRow(
                title = "Monet colours",
                subtitle = "Follow your wallpaper's palette",
                checked = settings.monetEnabled,
                onChange = { scope.launch { preferences.setMonetEnabled(it) } }
            )
        }

        SubHeader("Palette")
        PaletteChips(
            options = PALETTES,
            selected = if (monetActive) "" else settings.palette
        ) { value ->
            scope.launch {
                preferences.setPalette(value)
                preferences.setMonetEnabled(false)
            }
        }

        SubHeader("Accent")
        Swatches(
            options = ACCENTS.map { (value, label) ->
                value to (if (value == "auto") paletteSwatch(settings.palette) else accentColor(value) ?: paletteSwatch(settings.palette))
            },
            selected = settings.accent,
            labels = ACCENTS.toMap()
        ) { value -> scope.launch { preferences.setAccent(value) } }

        Divider()

        // ---- Home screen ------------------------------------------------------
        SectionHeader("Home screen")
        SwitchRow(
            title = "Wallpaper",
            subtitle = "Let your wallpaper through the home screen",
            checked = settings.showWallpaper,
            onChange = { scope.launch { preferences.setShowWallpaper(it) } }
        )
        if (settings.showWallpaper) {
            SliderRow(
                title = "Wallpaper dim",
                value = settings.wallpaperDim,
                range = 0..85,
                label = { "$it%" }
            ) { scope.launch { preferences.setWallpaperDim(it) } }
        }
        SwitchRow(
            title = "Haven wordmark",
            subtitle = "Show the name in the top corner",
            checked = settings.showBrand,
            onChange = { scope.launch { preferences.setShowBrand(it) } }
        )
        SwitchRow(
            title = "Clock",
            subtitle = "Tap it to open alarms",
            checked = settings.showClock,
            onChange = { scope.launch { preferences.setShowClock(it) } }
        )
        if (settings.showClock) {
            SubHeader("Clock size")
            Segmented(options = ICON_SCALES, selected = settings.clockScale) { value ->
                scope.launch { preferences.setClockScale(value) }
            }
        }
        SwitchRow(
            title = "Search bar",
            subtitle = "Show the search pill above the dock",
            checked = settings.showSearchPill,
            onChange = { scope.launch { preferences.setShowSearchPill(it) } }
        )

        Divider()

        // ---- Dock & Frequent --------------------------------------------------
        SectionHeader("Dock and Frequent row")
        StepperRow(
            title = "Dock slots",
            subtitle = "How many icons fit in the dock",
            value = settings.dockSlots,
            range = 3..5
        ) { scope.launch { preferences.setDockSlots(it) } }
        SwitchRow(
            title = "Dock background",
            subtitle = "The rounded pill behind the dock",
            checked = settings.dockBackground,
            onChange = { scope.launch { preferences.setDockBackground(it) } }
        )
        SwitchRow(
            title = "Frequent row",
            subtitle = "Your most launched apps, ranked by real use",
            checked = settings.showFrequent,
            onChange = { scope.launch { preferences.setShowFrequent(it) } }
        )
        if (settings.showFrequent) {
            StepperRow(
                title = "Frequent slots",
                subtitle = "Zero hides the row entirely",
                value = settings.frequentCount,
                range = 0..5
            ) { scope.launch { preferences.setFrequentCount(it) } }
        }

        Divider()

        // ---- Icons & grid -----------------------------------------------------
        SectionHeader("Icons and grid")
        SubHeader("Icon shape")
        ShapeChips(selected = settings.iconShape) { value ->
            scope.launch { preferences.setIconShape(value) }
        }

        SubHeader("Icon style")
        Segmented(
            options = listOf("masked" to "Masked", "natural" to "Natural", "plate" to "Plate"),
            selected = settings.iconStyle
        ) { value -> scope.launch { preferences.setIconStyle(value) } }
        Text(
            when (settings.iconStyle) {
                "natural" -> "Icons keep their own shape; the shape above is not applied."
                "plate" -> "Artwork sits inset on a tinted tile, so legacy icons match."
                else -> "Artwork is cropped to the shape above."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SubHeader("Icon size")
        Segmented(options = ICON_SCALES, selected = settings.iconScale) { value ->
            scope.launch { preferences.setIconScale(value) }
        }

        SwitchRow(
            title = "App labels",
            subtitle = "Show names below icons",
            checked = settings.showLabels,
            onChange = { scope.launch { preferences.setShowLabels(it) } }
        )
        if (settings.showLabels) {
            StepperRow(
                title = "Label lines",
                subtitle = "In the app drawer",
                value = settings.labelLines,
                range = 1..2
            ) { scope.launch { preferences.setLabelLines(it) } }
        }
        SliderRow(
            title = "Drawer columns",
            value = settings.columns,
            range = 3..6,
            label = { if (it == 1) "1 column" else "$it columns" }
        ) { scope.launch { preferences.setColumns(it) } }

        Divider()

        // ---- Drawer -----------------------------------------------------------
        SectionHeader("App drawer")
        SliderRow(
            title = "Drawer opacity",
            value = settings.drawerOpacity,
            range = 60..100,
            label = { "$it%" }
        ) { scope.launch { preferences.setDrawerOpacity(it) } }
        SubHeader("Web search")
        ChipGroup(
            options = listOf("google" to "Google", "duckduckgo" to "DuckDuckGo", "brave" to "Brave"),
            selected = settings.searchEngine
        ) { value -> scope.launch { preferences.setSearchEngine(value) } }

        Divider()

        // ---- Motion -----------------------------------------------------------
        SectionHeader("Motion")
        SwitchRow(
            title = "Animations",
            subtitle = "iOS-style open and close transitions",
            checked = settings.animations,
            onChange = { scope.launch { preferences.setAnimations(it) } }
        )
        if (settings.animations) {
            SubHeader("App open and close")
            Segmented(
                options = listOf("zoom" to "Zoom", "fade" to "Fade", "none" to "Off"),
                selected = settings.motionStyle
            ) { value -> scope.launch { preferences.setMotionStyle(value) } }
            SubHeader("Speed")
            Segmented(
                options = listOf("fast" to "Fast", "normal" to "Normal", "calm" to "Calm"),
                selected = settings.motionSpeed
            ) { value -> scope.launch { preferences.setMotionSpeed(value) } }
        }
        SwitchRow(
            title = "Haptics",
            subtitle = "A tick when you long-press an icon",
            checked = settings.haptics,
            onChange = { scope.launch { preferences.setHaptics(it) } }
        )

        // ---- Hidden apps ------------------------------------------------------
        if (hiddenApps.isNotEmpty()) {
            Divider()
            SectionHeader("Hidden apps")
            hiddenApps.forEach { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    TextButton(onClick = { scope.launch { preferences.unhide(app.key) } }) { Text("Show") }
                }
            }
        }

        Divider()

        // ---- Footer -----------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Haven $versionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    "Long-press an app to pin, hide or uninstall",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    if (confirmReset) {
                        scope.launch { preferences.resetAll() }
                        confirmReset = false
                    } else {
                        confirmReset = true
                    }
                }
            ) {
                Text(if (confirmReset) "Tap again to reset" else "Reset")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconShapes.NAMES.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = {
                    // Each chip previews the shape it selects.
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(IconShapes.of(value))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(paletteSwatch(value))
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(
    options: List<Pair<String, Color>>,
    selected: String,
    labels: Map<String, String>,
    onSelect: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (value, color) ->
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (value == selected) Modifier.border(3.dp, colors.onSurface, CircleShape) else Modifier
                    )
                    .semantics { contentDescription = "${labels[value] ?: value} accent" }
                    .clickable(role = Role.RadioButton) { onSelect(value) }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    range: IntRange,
    label: (Int) -> String,
    onCommit: (Int) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val current = local.roundToInt()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                label(current),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0)
        )
    }
}

@Composable
private fun StepperRow(
    title: String,
    subtitle: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            enabled = value > range.first
        ) {
            Icon(HavenIcons.Minus, contentDescription = "Decrease $title")
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            enabled = value < range.last
        ) {
            Icon(HavenIcons.Plus, contentDescription = "Increase $title")
        }
    }
}
