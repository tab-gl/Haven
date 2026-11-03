# Haven Launcher

A premium, lightweight Android launcher built with Material 3 Expressive principles. Haven is designed to feel considered and responsive on everyday Android hardware, including low-end devices.

> **Status:** Early MVP scaffold. Not ready to replace a daily-use launcher yet.

## MVP scope

- Native Android launcher entry point (HOME activity).
- App discovery through launcher activities exposed by `PackageManager`.
- Simple home surface with a clock and launchable app list.
- Native Compose UI with Material 3 foundations and an expressive design direction.
- A clear boundary for future optional web-based customization. The home screen and app drawer will remain native.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin / Gradle Kotlin DSL
- Minimum Android API 26 (Android 8.0)

## Build

Open the project in a current stable Android Studio installation and let Gradle sync. The Android SDK and a compatible JDK 17 installation are required.

```bash
./gradlew assembleDebug
```

The Gradle wrapper is not included in this initial scaffold. Android Studio can generate it, or run `gradle wrapper` with a locally installed Gradle distribution.

## Design and performance principles

- Keep the launcher surface native; do not put the home screen or app drawer in a WebView.
- Do package discovery and other potentially blocking work away from the main thread.
- Avoid polling and unnecessary background services.
- Keep animation effects restrained and provide a reduced-motion path.
- Measure memory and frame timing on real 2 GB and 3 GB devices before making performance claims.

## Current limitations

This scaffold is the starting point for the MVP, not a complete launcher. Workspace persistence, drag-and-drop, folders, widgets, icon packs, theme editing, backup/restore, and production-grade animation choreography are not implemented yet.

## License

License has not been selected yet. Do not assume this project is open source under a particular license until one is added.