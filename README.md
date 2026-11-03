# Haven Launcher

Haven is a lightweight Android home-screen launcher combining the restrained, hardware-led visual language of Nothing OS with the useful familiarity of Pixel Launcher.

- **Application ID:** `app.tab.home`
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Minimum Android:** 8.0 (API 26)
- **Target Android:** API 35
- **Architectures:** `armeabi-v7a` and `arm64-v8a`

## Features

- Minimal home screen with clock and date
- App drawer with live search
- Installed app icons and launching
- Swipe-up drawer gesture
- Monet dynamic colours on Android 12+
- Light, dark, and system themes
- Custom accent palettes on older Android versions
- Adjustable app grid (3–6 columns)
- Persistent launcher preferences
- Low-overhead app discovery and lazy app grid

## Build APKs

GitHub Actions builds two separate debug APKs on pushes to `main`, pull requests, and manual workflow dispatch.

1. Open the repository's **Actions** tab.
2. Select **Android APKs** and open a successful run.
3. Download either `haven-armeabi-v7a-debug` or `haven-arm64-v8a-debug`.

The workflow installs Gradle 8.13 directly because this repository does not currently commit a Gradle wrapper.

For a local build with Gradle 8.13 installed:

```bash
gradle --no-daemon :app:assembleDebug
```

APK outputs are written to `app/build/outputs/apk/debug/`. ABI splits are enabled, so the build emits architecture-specific APKs rather than a universal APK.

## Project structure

```
app/
  src/main/
    java/com/tabgl/haven/
      data/
      ui/
      MainActivity.kt
    res/
  build.gradle.kts
gradle/
  libs.versions.toml
.github/
  workflows/
    android-apk.yml
docs/
  MVP.md
```

## Scope

The current MVP focuses on a fast, configurable launcher foundation. Widgets, drag-and-drop workspace editing, folders, icon packs, backup/restore, and advanced gesture customization remain future work.

## Validation

The GitHub Actions workflow is the reproducible build check. A green workflow run is required before treating generated APKs as verified.
