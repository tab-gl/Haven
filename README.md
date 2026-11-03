# Haven Launcher

Haven is a lightweight Android launcher combining the restrained visual language of Nothing OS with the familiar utility of Pixel Launcher.

- **Application ID:** `app.tab.home`
- **Version:** 0.4.0
- **Languages:** Java + Kotlin
- **UI architecture:** Native Android Views hosted alongside Jetpack Compose
- **Minimum Android:** 8.0 (API 26)
- **Architectures:** `armeabi-v7a` and `arm64-v8a`

## Hybrid architecture

- **Java:** Android `ComponentActivity` host and a platform-native `TextClock`/date view.
- **Kotlin:** Compose launcher screens, app drawer, settings, app discovery, and preferences.
- **Interop:** `ComposeView` hosts Compose; Compose `AndroidView` embeds the Java clock and native app icons.
- **Motion:** Spring-driven drawer transitions, animated search feedback, content-size transitions, and animated theme colors.
- **Monet:** Android 12+ wallpaper-derived colors in Compose and system Monet color resources in the native clock. Custom accents remain available when Monet is disabled or unavailable.

## Features

- Minimal home screen with live native clock and date
- App drawer with live search
- Installed app icons and launching
- Swipe-up drawer gesture
- Monet dynamic colors on Android 12+
- Light, dark, and system themes
- Custom accent palettes
- Adjustable app grid (3–6 columns)
- Persistent launcher preferences
- Lazy app grid and background app discovery

## Build APKs

GitHub Actions builds separate debug APKs for both ARM architectures on pushes to `main`, pull requests, and manual workflow dispatch.

1. Open the repository's **Actions** tab.
2. Select **Android APKs** and open the latest successful run.
3. Download `haven-armeabi-v7a-debug` or `haven-arm64-v8a-debug`.

The workflow installs Gradle 8.13 directly because this repository does not commit a Gradle wrapper.

## Scope

Widgets, drag-and-drop workspace editing, folders, icon packs, backup/restore, and advanced gesture customization remain future work.

## Validation

A successful GitHub Actions run is required before treating the APKs as verified. Device-level animation profiling and low-memory testing remain necessary before claiming performance targets.
