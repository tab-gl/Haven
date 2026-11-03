package com.tabgl.haven

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HavenBackground = Color(0xFF101114)
private val HavenSurface = Color(0xFF1B1D21)
private val HavenAccent = Color(0xFFC9FF42)
private val HavenText = Color(0xFFF1F0EC)
private val HavenMuted = Color(0xFF9B9DA4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = HavenAccent,
                    onPrimary = Color(0xFF171910),
                    background = HavenBackground,
                    surface = HavenSurface,
                    onBackground = HavenText,
                    onSurface = HavenText
                )
            ) {
                HavenHome()
            }
        }
    }
}

private data class LaunchableApp(
    val label: String,
    val packageName: String
)

@Composable
private fun HavenHome() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var currentTime by remember { mutableStateOf(formatTime()) }
    var currentDate by remember { mutableStateOf(formatDate()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(context.packageManager) }
        while (true) {
            currentTime = formatTime()
            currentDate = formatDate()
            kotlinx.coroutines.delay(30_000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = HavenBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = currentTime,
                    color = HavenText,
                    fontSize = 64.sp,
                    lineHeight = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-3).sp
                )
                Text(
                    text = currentDate,
                    color = HavenMuted,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR APPS", color = HavenMuted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("${apps.size}", color = HavenAccent, style = MaterialTheme.typography.labelMedium)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HavenSurface, RoundedCornerShape(18.dp))
                            .clickable {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(HavenAccent, RoundedCornerShape(50))
                        )
                        Text(
                            text = app.label,
                            modifier = Modifier.padding(start = 14.dp),
                            color = HavenText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Text(
                text = "HAVEN  ·  A BETTER PLACE TO START",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = HavenMuted,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.1.sp
            )
        }
    }
}

private fun loadLaunchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, 0)
        .map { resolveInfo ->
            LaunchableApp(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

private fun formatTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
private fun formatDate(): String = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
