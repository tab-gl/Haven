package com.tabgl.haven.ui

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tabgl.haven.data.AppSearch
import com.tabgl.haven.data.ContactHit
import com.tabgl.haven.data.DeviceSearch
import com.tabgl.haven.data.FileHit
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherSettings
import com.tabgl.haven.data.MathEval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AppDrawer(
    apps: List<LauncherApp>,
    settings: LauncherSettings,
    dockKeys: Set<String>,
    actions: AppActions,
    autoFocusSearch: Boolean,
    onClose: () -> Unit,
    zoom: LaunchZoom,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.rank(apps, query) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val trimmed = query.trim()
    val mathResult = remember(trimmed, settings.searchCalculator) {
        if (settings.searchCalculator && MathEval.looksLikeMath(trimmed)) {
            MathEval.evaluate(trimmed)?.let { MathEval.format(it) }
        } else {
            null
        }
    }

    var contacts by remember { mutableStateOf(emptyList<ContactHit>()) }
    var files by remember { mutableStateOf(emptyList<FileHit>()) }
    LaunchedEffect(trimmed, settings.searchContacts, settings.searchFiles) {
        if (trimmed.isBlank()) {
            contacts = emptyList()
            files = emptyList()
            return@LaunchedEffect
        }
        delay(220) // let the user finish typing before touching the content providers
        contacts = if (settings.searchContacts) {
            withContext(Dispatchers.IO) { DeviceSearch.contacts(context, trimmed) }
        } else {
            emptyList()
        }
        files = if (settings.searchFiles) {
            withContext(Dispatchers.IO) { DeviceSearch.files(context, trimmed) }
        } else {
            emptyList()
        }
    }

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
    val hasSections = mathResult != null || contacts.isNotEmpty() || files.isNotEmpty()

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
            Text("Search", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
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
                    when {
                        first != null -> actions.launch(first, null)
                        trimmed.isNotBlank() -> searchWeb(context, trimmed, settings.searchEngine)
                    }
                }),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text("Apps, contacts, files or 2+2", color = colors.onSurfaceVariant)
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
        Spacer(Modifier.height(14.dp))

        if (results.isEmpty() && trimmed.isNotBlank() && !hasSections) {
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Nothing matches “$trimmed”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { searchWeb(context, trimmed, settings.searchEngine) }) {
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
                mathResult?.let { value ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CalculatorCard(expression = trimmed, result = value) {
                            copyToClipboard(context, value)
                        }
                    }
                }

                if (contacts.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Contacts") }
                    items(contacts, key = { "contact-${it.name}-${it.number}" }, span = { GridItemSpan(maxLineSpan) }) { contact ->
                        ResultRow(
                            title = contact.name,
                            subtitle = contact.number.orEmpty(),
                            onClick = { dial(context, contact.number) }
                        )
                    }
                }

                if (files.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Files") }
                    items(files, key = { "file-${it.uri}" }, span = { GridItemSpan(maxLineSpan) }) { file ->
                        ResultRow(
                            title = file.name,
                            subtitle = humanSize(file.sizeBytes),
                            onClick = { openFile(context, file) }
                        )
                    }
                }

                if (hasSections && results.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Apps") }
                }

                items(results, key = { it.key }) { app ->
                    AppEntry(
                        app = app,
                        iconSize = iconSize,
                        shapeName = settings.iconShape,
                        showLabel = settings.showLabels,
                        inDock = app.key in dockKeys,
                        actions = actions,
                        labelLines = settings.labelLines,
                        haptics = settings.haptics,
                        iconStyle = settings.iconStyle,
                        zoom = zoom
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun CalculatorCard(expression: String, result: String, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.primaryContainer)
            .clickable(onClick = onCopy)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            expression,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            result,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Tap to copy",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Haven result", value))
    Toast.makeText(context, "Copied $value", Toast.LENGTH_SHORT).show()
}

private fun dial(context: Context, number: String?) {
    val target = number?.takeIf { it.isNotBlank() } ?: return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(target)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openFile(context: Context, file: FileHit) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(file.uri, file.mimeType)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024 * 1024)} GB"
}

/** Web fallback when a search matches nothing on the device. */
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
