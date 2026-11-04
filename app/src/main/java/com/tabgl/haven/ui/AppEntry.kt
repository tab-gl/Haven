package com.tabgl.haven.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tabgl.haven.data.LauncherApp

/** Everything an app tile can do. Built once at the root so tiles stay cheap and skippable. */
@Stable
class AppActions(
    val launch: (LauncherApp) -> Unit,
    val info: (LauncherApp) -> Unit,
    val uninstall: (LauncherApp) -> Unit,
    val togglePin: (LauncherApp) -> Unit,
    val hide: (LauncherApp) -> Unit
)

/** An icon (+ optional label) that launches on tap and opens the app menu on long-press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(
    app: LauncherApp,
    iconSize: Dp,
    shapeName: String,
    showLabel: Boolean,
    inDock: Boolean,
    actions: AppActions,
    modifier: Modifier = Modifier,
    labelLines: Int = 1
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = { actions.launch(app) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    }
                )
                .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = app.label })
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AppIcon(app, iconSize, shapeName)
            if (showLabel) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = labelLines,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (inDock) "Remove from dock" else "Pin to dock") },
                onClick = { menuOpen = false; actions.togglePin(app) }
            )
            DropdownMenuItem(
                text = { Text("Hide") },
                onClick = { menuOpen = false; actions.hide(app) }
            )
            DropdownMenuItem(
                text = { Text("App info") },
                onClick = { menuOpen = false; actions.info(app) }
            )
            if (!app.isSystem) {
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = { menuOpen = false; actions.uninstall(app) }
                )
            }
        }
    }
}
