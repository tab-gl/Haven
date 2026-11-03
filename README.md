# Haven Launcher

Haven is an Android home-screen project combining the restrained hardware-led visual language of Nothing OS with the useful, familiar foundations of Pixel Launcher. It is built with Kotlin, Jetpack Compose and Material 3, with Android Monet wallpaper colours on supported devices.

## MVP features

- HOME role entry point and a normal app-drawer launch entry.
- Native home surface with clock, date, frequent apps and app-drawer entry.
- App drawer with live name filtering and configurable 3–6 column grid.
- Launch installed apps through their resolved launcher activities.
- Android 12+ Monet dynamic light/dark colour schemes, with a custom palette fallback.
- System, light and dark appearance modes.
- Accent selection, app-label visibility and persisted preferences using DataStore.
- Swipe-up drawer gesture and system back handling.
- Adaptive launcher icon.

## Stack

- Kotlin + Jetpack Compose
- Material 3 components and expressive layout principles
- DataStore Preferences
- Minimum Android API 26; Monet dynamic colours on API 31+
- Compile SDK 36; target SDK 35

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 36 installed. Sync Gradle and run the `app` configuration. A Gradle wrapper is not committed yet, so Android Studio may prompt to generate one or you can run `gradle wrapper` using a compatible local Gradle installation.

## Performance decisions

- The home screen and app drawer are native Compose UI; no WebView is in the interaction path.
- App discovery runs on `Dispatchers.IO` and the drawer uses a lazy grid.
- The app list is loaded when the launcher enters the foreground, not polled continuously.
- No background service, network permission, analytics SDK or unnecessary runtime permission is included.
- App icons are loaded by Android's package manager as needed.

## Current MVP boundaries

This is a functional foundation, not a feature-complete commercial launcher. Workspace drag-and-drop, folders, user-managed dock pins, widgets, icon-pack support, backup/restore, per-app hiding, gesture customization, wallpaper picker integration and a web-based theme editor remain future work. App usage ranking is not inferred from launch history; the current frequent row uses a deterministic initial selection.

## Inspiration

Haven takes broad product cues from Nothing OS and Pixel Launcher. It does not use their proprietary assets, icons, source code or branding.

## License

No license has been selected. All rights remain with their respective owners until a license is added.
