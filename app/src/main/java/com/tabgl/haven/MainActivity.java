package com.tabgl.haven;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.view.WindowCompat;

/** HOME activity. Hosts a single Compose surface; all UI lives in Kotlin ({@link HavenHost}). */
public final class MainActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        ComposeView content = new ComposeView(this);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        // Composition is set up from Kotlin: calling a @Composable directly from Java skips the
        // Compose compiler's restart/skip machinery and is unsupported.
        HavenHost.setHavenContent(content);
        setContentView(content);

        // Singleton HOME: pressing Home again arrives as a new intent. Collapse the drawer/settings.
        addOnNewIntentListener(intent -> HavenSignals.INSTANCE.onHome());
    }
}
