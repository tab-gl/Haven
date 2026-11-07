@file:JvmName("HavenHost")

package com.tabgl.haven

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tabgl.haven.data.AppRepository
import com.tabgl.haven.data.DockPlanner
import com.tabgl.haven.data.IconCache
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherPreferences
import com.tabgl.haven.data.LauncherSettings
import com.tabgl.haven.data.PerformanceProfiles
import com.tabgl.haven.data.Profile
import com.tabgl.haven.ui.AppActions
import com.tabgl.haven.ui.AppDrawer
import com.tabgl.haven.ui.DrawerController
import com.tabgl.haven.ui.HavenTheme
import com.tabgl.haven.ui.HavenWidgets
import com.tabgl.haven.ui.HomeScreen
import com.tabgl.haven.ui.Metrics
import com.tabgl.haven.ui.LocalIconCache
import com.tabgl.haven.ui.SettingsSheet
import com.tabgl.haven.ui.motionSpecFor
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

@Composable
private fun HavenRoot() {
    val activityContext = LocalContext.current
    val appContext = remember(activityContext) { activityContext.applicationContext }
    val preferences = remember(appContext) { LauncherPreferences(appContext) }
    val repository = remember(appContext) { AppRepository(appContext) }
    val initialSettings = remember(preferences) { preferences.readBlocking() }
    val settings by preferences.settings.collectAsState(initial = initialSettings)

    // Device-aware profile: the same design language, less memory spent on it. Built after the
    // settings read so a "lean"/"full" override is honoured before the first icon is decoded.
    val profile = remember(settings.performanceMode, appContext) {
        PerformanceProfiles.resolve(appContext, settings.performanceMode)
    }
    val traits = remember(profile, appContext) { PerformanceProfiles.traits(appContext, profile) }
    val iconCache = remember(appContext, traits) { IconCache(appContext, traits) }
    val widgets = remember(appContext) { HavenWidgets(appContext) }
    DisposableEffect(widgets) {
        widgets.startListening()
        onDispose { widgets.stopListening() }
    }

    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var defaultDock by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(repository) {
        apps = withContext(Dispatchers.IO) { repository.loadApps() }
        defaultDock = withContext(Dispatchers.IO) { repository.defaultDockPackages() }
        repository.changes().collectLatest { changed ->
            delay(250)
            // Icons of an updated or reinstalled package are stale: drop them so they re-decode.
            if (changed.isNotEmpty()) {
                withContext(Dispatchers.IO) { changed.forEach(iconCache::invalidatePackage) }
            }
            apps = withContext(Dispatchers.IO) { repository.loadApps() }
            defaultDock = withContext(Dispatchers.IO) { repository.defaultDockPackages() }
        }
    }

    CompositionLocalProvider(LocalIconCache provides iconCache) {
        HavenTheme(settings) {
            HavenScreen(settings, preferences, apps, defaultDock, widgets, traits)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HavenScreen(
    settings: LauncherSettings,
    preferences: LauncherPreferences,
    apps: List<LauncherApp>,
    defaultDock: List<String>,
    widgets: HavenWidgets,
    traits: com.tabgl.haven.data.PerfTraits
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawer = remember { DrawerController(scope) }
    val motion = remember(settings.animations, settings.motionSpeed) { motionSpecFor(settings) }
    LaunchedEffect(motion) { drawer.updateSpec(motion) }
    var settingsOpen by remember { mutableStateOf(false) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    val visible = remember(apps, settings.hidden) { apps.filterNot { it.key in settings.hidden } }
    val hiddenApps = remember(apps, settings.hidden) { apps.filter { it.key in settings.hidden } }
    val dock = remember(visible, settings.pinned, defaultDock, settings.dockSlots, settings.usage) {
        DockPlanner.dock(visible, settings.pinned, defaultDock, settings.dockSlots, settings.usage)
    }
    val dockKeys = remember(dock) { dock.mapTo(HashSet<String>()) { it.key } }
    val frequent = remember(visible, settings.usage, dockKeys, settings.frequentCount, settings.showFrequent) {
        DockPlanner.frequent(
            visible = visible,
            usage = settings.usage,
            excluded = dockKeys,
            count = if (settings.showFrequent) settings.frequentCount else 0
        )
    }
    val currentDockKeys by rememberUpdatedState(dock.map { it.key })
    val dockSlots = settings.dockSlots

    val actions = remember(context, preferences, scope, drawer, dockSlots) {
        AppActions(
            // Straight to the app on the tap frame. No launcher-side transition: Android's own
            // window animation is the transition, and any delay we add is latency the user feels.
            launch = { app ->
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(app.packageName, app.activityName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                if (runCatching { context.startActivity(intent) }.isSuccess) {
                    scope.launch { preferences.recordLaunch(app.key) }
                }
            },
            info = { app ->
                openSystemScreen(
                    context,
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.packageName, null)
                    )
                )
            },
            uninstall = { app ->
                openSystemScreen(context, Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
            },
            togglePin = { app ->
                val current = currentDockKeys
                val next = if (app.key in current) {
                    current - app.key
                } else {
                    (current + app.key).takeLast(dockSlots)
                }
                scope.launch { preferences.setPinned(next) }
            },
            hide = { app -> scope.launch { preferences.hide(app.key) } }
        )
    }

    // Warm the dock and Frequent icons before they are first drawn, so the home screen never opens
    // on a row of placeholders while eight decodes queue up behind the first frame.
    val iconCache = LocalIconCache.current
    val density = LocalDensity.current
    LaunchedEffect(dock, frequent, settings.iconScale, settings.iconStyle, traits) {
        if (!traits.preloadDock) return@LaunchedEffect
        val dockPx = with(density) { Metrics.dockIcon(settings.iconScale).roundToPx() }
        val frequentPx = with(density) { Metrics.frequentIcon(settings.iconScale).roundToPx() }
        withContext(Dispatchers.IO) {
            dock.forEach { iconCache.load(it, dockPx, settings.iconStyle) }
            frequent.forEach { iconCache.load(it, frequentPx, settings.iconStyle) }
        }
    }

    val widgetLabels = remember(settings.widgets) { settings.widgets.map { it.id to widgets.labelOf(it.id) } }
    val pages = settings.pages.coerceIn(1, 4)
    val pagerState = rememberPagerState(pageCount = { pages })

    // The system picker binds the widget for us: Haven only has to reserve the id first.
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    val widgetPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val reserved = pendingWidgetId
        pendingWidgetId = null
        val id = widgets.boundIdFrom(result.resultCode, result.data)
        when {
            id != null && widgets.isBound(id) ->
                scope.launch { preferences.addWidget(id, pagerState.currentPage) }
            id != null -> {
                // The picker claimed a widget but the system did not actually bind it.
                widgets.releaseId(id)
                toast(context, "That widget could not be added")
            }
            else -> {
                // Backed out, or an OEM picker that returns no id: never leak the reserved slot.
                reserved?.let { widgets.releaseId(it) }
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    toast(context, "The system picker returned no widget")
                }
            }
        }
    }

    val drawerVisible by remember { derivedStateOf { drawer.progress > 0.001f } }

    // Coming back from an app: collapse whatever was open. The drawer slides away rather than
    // snapping, so the return reads as the launcher settling rather than a jump cut.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (settingsOpen) settingsOpen = false
        if (drawer.progress > 0.001f) drawer.close()
    }

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

    val dim = settings.wallpaperDim / 100f
    val background = MaterialTheme.colorScheme.background
    val scrim = if (settings.showWallpaper) background.copy(alpha = dim) else background

    Box(
        Modifier
            .fillMaxSize()
            .background(scrim)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
    ) {
        Box(Modifier.fillMaxSize()) {
            HomeScreen(
                settings = settings,
                dock = dock,
                frequent = frequent,
                appCount = visible.size,
                actions = actions,
                onOpenDrawer = { focusSearch -> drawer.open(focusSearch) },
                onOpenSettings = { settingsOpen = true },
                widgets = widgets,
                widgetRefs = settings.widgets,
                pagerState = pagerState,
                beyondViewportPages = traits.beyondViewportPages,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The home recedes as the drawer sheet comes up — scale only. Fading the
                        // home would put the whole screen in an offscreen buffer on every frame.
                        val p = drawer.progress
                        val recede = 1f - 0.06f * p
                        scaleX = recede
                        scaleY = recede
                        translationY = -size.height * 0.02f * p
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> drawer.dragBy(-delta / heightPx) },
                        onDragStopped = { velocity -> drawer.settle(-velocity / heightPx) }
                    )
            )

            if (drawerVisible) {
                // A single solid rectangle, not a faded copy of the home: one quad to blend, and
                // it gives the drawer something to sit on in both themes.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (drawer.progress * 0.38f).coerceIn(0f, 1f) }
                        .background(Color.Black)
                )
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
                            // Slide and scale only: the drawer's own background is what hides the
                            // home, so nothing here needs an offscreen buffer.
                            val p = drawer.progress
                            translationY = (1f - p) * size.height * 0.14f
                            val s = 0.965f + 0.035f * p
                            scaleX = s
                            scaleY = s
                        }
                        .nestedScroll(remember(drawer) { drawer.connection { heightPx } })
                )
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SettingsSheet(
                settings = settings,
                hiddenApps = hiddenApps,
                preferences = preferences,
                versionName = versionName(context),
                profile = traits.profile,
                widgetLabels = widgetLabels,
                onAddWidget = {
                    val id = widgets.allocateId()
                    if (id == null) {
                        toast(context, "Could not reserve a widget slot")
                    } else {
                        pendingWidgetId = id
                        runCatching { widgetPicker.launch(widgets.pickerIntent(id)) }
                            .onFailure {
                                pendingWidgetId = null
                                widgets.releaseId(id)
                                toast(context, "No widget picker on this device")
                            }
                    }
                },
                onRemoveWidget = { id ->
                    widgets.releaseId(id)
                    scope.launch { preferences.removeWidget(id) }
                }
            )
        }
    }
}

private fun toast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

private fun openSystemScreen(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Suppress("DEPRECATION")
private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "dev" }
