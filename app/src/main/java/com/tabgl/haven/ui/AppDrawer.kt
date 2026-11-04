package com.tabgl.haven.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tabgl.haven.data.AppSearch
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherSettings
import kotlinx.coroutines.delay

@Composable
fun AppDrawer(
    apps: List<LauncherApp>,
    settings: LauncherSettings,
    dockKeys: Set<String>,
    actions: AppActions,
    autoFocusSearch: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.rank(apps, query) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val iconSize = Metrics.drawerIcon(settings.iconScale)
    val opacity = (settings.drawerOpacity / 100f).coerceIn(0.3f, 1f)

    Column(
        modifier
            .background(colors.background.copy(alpha = if (settings.showWallpaper) opacity else 1f))
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(HavenIcons.Back, contentDescription = "Close app drawer") }
            Text("All apps", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${results.size}", color = colors.primary, style = MaterialTheme.typography.labelLarge)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(HavenIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focus),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val first = results.firstOrNull()
                    if (first != null) actions.launch(first, null)
                    else if (query.isNotBlank()) searchWeb(context, query.trim(), settings.searchEngine)
                }),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text("Search your apps", color = colors.onSurfaceVariant)
                        }
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(HavenIcons.Close, contentDescription = "Clear search")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (results.isEmpty() && query.isNotBlank()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "No apps match “${query.trim()}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { searchWeb(context, query.trim(), settings.searchEngine) }) {
                    Text("Search the web")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.columns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = bottomInset + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.key }) { app ->
                    AppEntry(
                        app = app,
                        iconSize = iconSize,
                        shapeName = settings.iconShape,
                        showLabel = settings.showLabels,
                        inDock = app.key in dockKeys,
                        actions = actions,
                        labelLines = settings.labelLines,
                        haptics = settings.haptics
                    )
                }
            }
        }
    }
}

/** Web fallback when a search matches no installed app. */
private fun searchWeb(context: Context, query: String, engine: String) {
    if (engine == "google") {
        val search = Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, query)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(search) }.isSuccess) return
    }
    val base = when (engine) {
        "duckduckgo" -> "https://duckduckgo.com/?q="
        "brave" -> "https://search.brave.com/search?q="
        else -> "https://www.google.com/search?q="
    }
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(base + Uri.encode(query)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(view) }
}
