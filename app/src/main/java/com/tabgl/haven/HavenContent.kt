package com.tabgl.haven

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.tabgl.haven.data.AppRepository
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherPreferences
import com.tabgl.haven.data.LauncherSettings
import com.tabgl.haven.ui.HavenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HavenRoot() {
    val context = LocalContext.current
    val preferences = remember(context) { LauncherPreferences(context) }
    val appRepository = remember(context) { AppRepository(context.packageManager) }
    val settings by preferences.settings.collectAsState(initial = LauncherSettings())
    val loadApps: suspend () -> List<LauncherApp> = { withContext(Dispatchers.IO) { appRepository.loadApps() } }
    val launchApp: (LauncherApp) -> Unit = { app ->
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
    HavenTheme(settings) {
        HavenAppContent(settings, preferences, loadApps, launchApp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HavenAppContent(
    settings: LauncherSettings,
    preferences: LauncherPreferences,
    loadApps: suspend () -> List<LauncherApp>,
    launchApp: (LauncherApp) -> Unit
) {
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = loadApps() }

    BackHandler(drawerOpen || settingsOpen) {
        if (settingsOpen) settingsOpen = false else { drawerOpen = false; search = "" }
    }

    val filtered = remember(apps, search) {
        if (search.isBlank()) apps else apps.filter { it.label.contains(search.trim(), ignoreCase = true) }
    }
    val favorites = remember(apps) { apps.take(4) }

    Box(
        modifier = Modifier.fillMaxSize()
            .animateContentSize(spring(stiffness = 420f))
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(drawerOpen) {
                var distance = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, drag -> distance += drag },
                    onDragEnd = {
                        if (!drawerOpen && distance < -70f) drawerOpen = true
                        if (drawerOpen && distance > 70f) { drawerOpen = false; search = "" }
                        distance = 0f
                    },
                    onDragCancel = { distance = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .padding(horizontal = 22.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HAVEN", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.4.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Your space.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { settingsOpen = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Customize launcher")
                }
            }
            Spacer(Modifier.height(38.dp))
            AndroidView(
                factory = { HavenClockView(it) },
                modifier = Modifier.fillMaxWidth().height(112.dp)
            )
            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FREQUENT", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${apps.size} APPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                favorites.forEach { app ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .clickable { launchApp(app) }.padding(vertical = 13.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        AppIcon(app, Modifier.size(34.dp))
                        if (settings.showLabels) Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f).clickable { drawerOpen = true },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("A little less noise.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        Text("Everything you need, close at hand.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("  Open all apps", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        Text("↑", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .clickable { drawerOpen = true }.padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("  Search apps", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text("⌕", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            }
        }

        AnimatedContent(
            targetState = drawerOpen,
            transitionSpec = {
                (fadeIn(spring(stiffness = 500f)) + slideInVertically(initialOffsetY = { it / 14 }, animationSpec = spring(stiffness = 500f))) togetherWith
                    (fadeOut(spring(stiffness = 500f)) + slideOutVertically(targetOffsetY = { it / 14 }, animationSpec = spring(stiffness = 500f)))
            },
            label = "drawer"
        ) { open ->
            if (open) AppDrawer(
                apps = filtered, query = search, onQuery = { search = it },
                onClose = { drawerOpen = false; search = "" },
                onLaunch = { launchApp(it); drawerOpen = false; search = "" },
                showLabels = settings.showLabels, columns = settings.columns
            )
        }
    }

    if (settingsOpen) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) { SettingsPanel(settings, preferences) }
    }
}

@Composable
private fun AppDrawer(
    apps: List<LauncherApp>, query: String, onQuery: (String) -> Unit,
    onClose: () -> Unit, onLaunch: (LauncherApp) -> Unit,
    showLabels: Boolean, columns: Int
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Close app drawer") }
            Text("All apps", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${apps.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search your apps", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() } }
            )
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
        }
        AnimatedVisibility(visible = query.isNotBlank(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text("${apps.size} matching apps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable { onLaunch(app) }.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIcon(app, Modifier.size(48.dp))
                    if (showLabels) Text(app.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, minLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: LauncherApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { image ->
            image.contentDescription = app.label
            runCatching { image.setImageDrawable(context.packageManager.getApplicationIcon(app.packageName)) }
        }
    )
}

@Composable
private fun SettingsPanel(settings: LauncherSettings, preferences: LauncherPreferences) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                if (settings.themeMode == value) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label) }
                } else {
                    OutlinedButton(onClick = { scope.launch { preferences.setThemeMode(value) } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Monet colours", style = MaterialTheme.typography.titleSmall)
                Text("Use wallpaper colours on Android 12+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = settings.monetEnabled, onCheckedChange = { scope.launch { preferences.setMonetEnabled(it) } })
        }
        Text("Accent colour", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("lime" to Color(0xFFC9FF42), "blue" to Color(0xFF8AB4F8), "coral" to Color(0xFFFF8A80), "violet" to Color(0xFFD0BCFF)).forEach { (name, color) ->
                Box(Modifier.size(42.dp).clip(CircleShape).background(color).clickable {
                    scope.launch { preferences.setAccent(name); preferences.setMonetEnabled(false) }
                })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("App labels", style = MaterialTheme.typography.titleSmall)
                Text("Show names below icons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = settings.showLabels, onCheckedChange = { scope.launch { preferences.setShowLabels(it) } })
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("App grid", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${settings.columns} columns", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Slider(value = settings.columns.toFloat(), onValueChange = { value ->
                scope.launch { preferences.setColumns(value.toInt().coerceIn(3, 6)) }
            }, valueRange = 3f..6f, steps = 2)
        }
        Text("Haven · Early access", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


