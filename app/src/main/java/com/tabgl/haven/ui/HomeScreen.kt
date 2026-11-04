package com.tabgl.haven.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tabgl.haven.HavenClockView
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherSettings

private const val SLOTS = 4

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    settings: LauncherSettings,
    dock: List<LauncherApp>,
    frequent: List<LauncherApp>,
    appCount: Int,
    actions: AppActions,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val clockPrimary = colors.onBackground.toArgb()
    val clockSecondary = colors.onSurfaceVariant.toArgb()
    val dockIcon = if (settings.compactIcons) 34.dp else 42.dp
    val frequentIcon = if (settings.compactIcons) 32.dp else 38.dp

    Column(
        modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSettings()
                },
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "HAVEN",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.4.sp,
                color = colors.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(HavenIcons.Tune, contentDescription = "Customize launcher")
            }
        }
        Spacer(Modifier.height(28.dp))
        AndroidView(
            factory = { ctx ->
                HavenClockView(ctx).apply {
                    setOnClickListener {
                        runCatching {
                            ctx.startActivity(
                                Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            },
            update = { it.setColors(clockPrimary, clockSecondary) },
            modifier = Modifier.fillMaxWidth().height(112.dp)
        )

        Spacer(Modifier.weight(1f))

        if (frequent.isNotEmpty()) {
            Text(
                "FREQUENT",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.6.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequent.take(SLOTS).forEach { app ->
                    AppEntry(
                        app = app, iconSize = frequentIcon, shapeName = settings.iconShape,
                        showLabel = settings.showLabels, inDock = false, actions = actions,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(SLOTS - frequent.take(SLOTS).size) { Spacer(Modifier.weight(1f)) }
            }
        } else {
            Text(
                "Swipe up for all $appCount apps",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.78f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dock.take(SLOTS).forEach { app ->
                AppEntry(
                    app = app, iconSize = dockIcon, shapeName = settings.iconShape,
                    showLabel = false, inDock = true, actions = actions,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(SLOTS - dock.take(SLOTS).size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.78f))
                .clickable { onOpenDrawer(true) }
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(HavenIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text("Search apps", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}
