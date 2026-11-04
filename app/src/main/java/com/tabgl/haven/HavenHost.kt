@file:JvmName("HavenHost")

package com.tabgl.haven

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import com.tabgl.haven.data.AppRepository
import com.tabgl.haven.data.IconCache
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherPreferences
import com.tabgl.haven.data.LauncherSettings
import com.tabgl.haven.ui.AppActions
import com.tabgl.haven.ui.AppDrawer
import com.tabgl.haven.ui.DrawerController
import com.tabgl.haven.ui.HavenTheme
import com.tabgl.haven.ui.HomeScreen
import com.tabgl.haven.ui.LocalIconCache
import com.tabgl.haven.ui.SettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entry point called from [MainActivity] (Java). */
fun setHavenContent(view: ComposeView) {
    view.setContent { HavenRoot() }
}

/** Cross-boundary signals from the Java activity into Compose. */
object HavenSignals {
    var homeTick by mutableIntStateOf(0)
        private set

    fun onHome() {
        homeTick++
    }
}

private const val DOCK_SIZE = 4

@Composable
private fun HavenRoot() {
    val activityContext = LocalContext.current
    val appContext = remember(activityContext) { activityContext.applicationContext }
    val preferences = remember(appContext) { LauncherPreferences(appContext) }
    val repository = remember(appContext) { AppRepository(appContext) }
    val iconCache = remember(appContext) { IconCache(appContext) }
    val initialSettings = remember(preferences) { preferences.readBlocking() }
    val settings by preferences.settings.collectAsState(initial = initialSettings)

    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var defaultDock by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(repository) {
        apps = withContext(Dispatchers.IO) { repository.loadApps() }
        defaultDock = withContext(Dispatchers.IO) { repository.defaultDockPackages() }
        repository.changes().collectLatest {
            delay(300)
            apps = withContext(Dispatchers.IO) { repository.loadApps() }
            defaultDock = withContext(Dispatchers.IO) { repository.defaultDockPackages() }
        }
    }

    CompositionLocalProvider(LocalIconCache provides iconCache) {
        HavenTheme(settings) {
            HavenScreen(settings, preferences, apps, defaultDock)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HavenScreen(
    settings: LauncherSettings,
    preferences: LauncherPreferences,
    apps: List<LauncherApp>,
    defaultDock: List<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawer = remember { DrawerController(scope) }
    var settingsOpen by remember { mutableStateOf(false) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    val visible = remember(apps, settings.hidden) { apps.filterNot { it.key in settings.hidden } }
    val hiddenApps = remember(apps, settings.hidden) { apps.filter { it.key in settings.hidden } }
    val dock = remember(visible, settings.pinned, defaultDock) { buildDock(visible, settings.pinned, defaultDock) }
    val dockKeys = remember(dock) { dock.mapTo(HashSet<String>()) { it.key } }
    val frequent = remember(visible, settings.usage, dock) { buildFrequent(visible, settings.usage, dockKeys) }
    val currentDockKeys by rememberUpdatedState(dock.map { it.key })

    val actions = remember(context, preferences, scope, drawer) {
        AppActions(
            launch = { app ->
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(app.packageName, app.activityName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                if (runCatching { context.startActivity(intent) }.isSuccess) {
                    scope.launch { preferences.recordLaunch(app.key) }
                }
                drawer.close()
            },
            info = { app ->
                openSystemScreen(
                    context,
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
                )
            },
            uninstall = { app ->
                openSystemScreen(context, Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
            },
            togglePin = { app ->
                val current = currentDockKeys
                val next = if (app.key in current) current - app.key else (current + app.key).takeLast(DOCK_SIZE)
                scope.launch { preferences.setPinned(next) }
            },
            hide = { app -> scope.launch { preferences.hide(app.key) } }
        )
    }

    val drawerVisible by remember { derivedStateOf { drawer.progress > 0.001f } }

    val homeTick = HavenSignals.homeTick
    LaunchedEffect(homeTick) {
        if (homeTick > 0) {
            settingsOpen = false
            drawer.close()
        }
    }

    // Always handled so Back never finishes the HOME activity.
    BackHandler {
        if (settingsOpen) settingsOpen = false else if (drawerVisible) drawer.close()
    }

    val scrim = MaterialTheme.colorScheme.background.copy(alpha = if (settings.showWallpaper) 0.5f else 1f)
    Box(
        Modifier
            .fillMaxSize()
            .background(scrim)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
    ) {
        HomeScreen(
            settings = settings,
            dock = dock,
            frequent = frequent,
            appCount = visible.size,
            actions = actions,
            onOpenDrawer = { focusSearch -> drawer.open(focusSearch) },
            onOpenSettings = { settingsOpen = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = drawer.progress
                    alpha = 1f - p
                    scaleX = 1f - 0.04f * p
                    scaleY = 1f - 0.04f * p
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> drawer.dragBy(-delta / heightPx) },
                    onDragStopped = { velocity -> drawer.settle(-velocity / heightPx) }
                )
        )

        if (drawerVisible) {
            AppDrawer(
                apps = visible,
                settings = settings,
                dockKeys = dockKeys,
                actions = actions,
                autoFocusSearch = drawer.autoFocusSearch,
                onClose = { drawer.close() },
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        val p = drawer.progress
                        alpha = p
                        translationY = (1f - p) * size.height * 0.1f
                    }
                    .nestedScroll(remember(drawer) { drawer.connection { heightPx } })
            )
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SettingsSheet(settings, hiddenApps, preferences, versionName(context))
        }
    }
}

private fun buildDock(visible: List<LauncherApp>, pinned: List<String>?, defaults: List<String>): List<LauncherApp> {
    if (pinned != null) {
        val byKey = visible.associateBy { it.key }
        return pinned.mapNotNull { byKey[it] }.take(DOCK_SIZE)
    }
    val out = ArrayList<LauncherApp>(DOCK_SIZE)
    for (pkg in defaults) {
        val match = visible.firstOrNull { it.packageName == pkg }
        if (match != null && match !in out) out.add(match)
        if (out.size == DOCK_SIZE) break
    }
    return out
}

private fun buildFrequent(
    visible: List<LauncherApp>,
    usage: Map<String, Int>,
    dockKeys: Set<String>
): List<LauncherApp> =
    visible.asSequence()
        .filter { it.key !in dockKeys && (usage[it.key] ?: 0) > 0 }
        .sortedByDescending { usage[it.key] ?: 0 }
        .take(4)
        .toList()

private fun openSystemScreen(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Suppress("DEPRECATION")
private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "dev" }
