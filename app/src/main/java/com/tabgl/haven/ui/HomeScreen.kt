package com.tabgl.haven.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tabgl.haven.HavenClockView
import com.tabgl.haven.data.LauncherApp
import com.tabgl.haven.data.LauncherSettings

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
    val dockIcon = Metrics.dockIcon(settings.iconScale)
    val frequentIcon = Metrics.frequentIcon(settings.iconScale)
    val frequentSlots = settings.frequentCount.coerceIn(0, 5)
    val dockSlots = settings.dockSlots.coerceIn(3, 5)
    val showFrequent = settings.showFrequent && frequentSlots > 0 && frequent.isNotEmpty()

    Column(
        modifier
            // Long-press on empty space opens settings. A plain pointer handler keeps the
            // accessibility tree clean (the old empty `clickable` announced a phantom button).
            .pointerInput(settings.haptics) {
                detectTapGestures(onLongPress = {
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSettings()
                })
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showBrand) {
                Text(
                    "HAVEN",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.4.sp,
                    color = colors.primary,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(HavenIcons.Tune, contentDescription = "Customize launcher")
            }
        }

        if (settings.showClock) {
            Spacer(Modifier.height(28.dp))
            AndroidView(
                factory = { ctx ->
                    HavenClockView(ctx).apply {
                        setOnClickListener {
                            runCatching {
                                ctx.startActivity(
                                    Intent(AlarmClock.ACTION_SHOW_ALARMS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                },
                update = {
                    it.setColors(clockPrimary, clockSecondary)
                    it.setTextSizes(
                        Metrics.clockTime(settings.clockScale),
                        Metrics.clockDate(settings.clockScale)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(Metrics.clockHeight(settings.clockScale))
            )
        }

        Spacer(Modifier.weight(1f))

        if (showFrequent) {
            Text(
                "FREQUENT",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.6.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shown = frequent.take(frequentSlots)
                shown.forEach { app ->
                    AppEntry(
                        app = app,
                        iconSize = frequentIcon,
                        shapeName = settings.iconShape,
                        showLabel = settings.showLabels,
                        inDock = false,
                        actions = actions,
                        haptics = settings.haptics,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(frequentSlots - shown.size) { Spacer(Modifier.weight(1f)) }
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
                .then(
                    if (settings.dockBackground) {
                        Modifier.background(colors.surfaceVariant.copy(alpha = 0.78f))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val shownDock = dock.take(dockSlots)
            shownDock.forEach { app ->
                AppEntry(
                    app = app,
                    iconSize = dockIcon,
                    shapeName = settings.iconShape,
                    showLabel = false,
                    inDock = true,
                    actions = actions,
                    haptics = settings.haptics,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(dockSlots - shownDock.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))

        if (settings.showSearchPill) {
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
}
