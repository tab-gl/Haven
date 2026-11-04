package com.tabgl.haven.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts real home-screen widgets.
 *
 * The host id must be stable for the life of the installation: it is how the system remembers which
 * widgets belong to Haven, so widgets survive restarts and updates.
 */
@Stable
class HavenWidgets(private val context: Context) {

    private val host = AppWidgetHost(context.applicationContext, HOST_ID)
    private val manager = AppWidgetManager.getInstance(context.applicationContext)

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    /** Reserves an id for the picker to bind. Null when the system refuses (rare). */
    fun allocateId(): Int? = runCatching { host.allocateAppWidgetId() }.getOrNull()

    fun releaseId(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
    }

    /** The intent that lets the system pick and bind a widget on Haven's behalf. */
    fun pickerIntent(id: Int): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        // Some OEM pickers read the raw key rather than the constant.
        putExtra("appWidgetId", id)
    }

    fun createView(id: Int): AppWidgetHostView? {
        val info = runCatching { manager.getAppWidgetInfo(id) }.getOrNull() ?: return null
        return runCatching { host.createView(context.applicationContext, id, info) }.getOrNull()
    }

    /** A human label for the settings list: the provider's app name. */
    fun labelOf(id: Int): String {
        val info = runCatching { manager.getAppWidgetInfo(id) }.getOrNull() ?: return "Widget $id"
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrDefault(info.provider.packageName)
    }

    /** Extracts the bound widget id from the picker's result, or null if the user backed out. */
    fun boundIdFrom(resultCode: Int, data: Intent?): Int? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        val id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        return id.takeIf { it != -1 }
    }

    private companion object {
        /** Arbitrary but permanent: changing it orphans every widget the user has added. */
        const val HOST_ID = 0x4841564E // "HAVN"
    }
}

/**
 * One hosted widget. The view is created once and then left alone — widget updates are pushed by the
 * host, not by Compose.
 */
@Composable
fun WidgetSlot(widgets: HavenWidgets, id: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            widgets.createView(id) ?: TextView(ctx).apply {
                text = "Widget unavailable — long-press to remove it from Customise."
                setPadding(24, 24, 24, 24)
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
