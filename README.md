# Haven Launcher

Haven is a lightweight Android launcher that pairs Material 3 colour and expressive-style rounded shapes with spring motion and a restrained, Nothing-style layout. It is built to stay smooth on low-end (1–3 GB RAM) devices.

- **Application ID:** `app.tab.home`
- **Version:** 0.5.0
- **Minimum Android:** 8.0 (API 26) · **ABIs:** `armeabi-v7a`, `arm64-v8a`
- **Stack:** Java host (activity + native clock) with Kotlin/Compose UI

## Features

- Wallpaper-through home screen with native, self-ticking clock (tap to open alarms)
- Real dock: pin/unpin with long-press; defaults to your dialer, messages, camera and browser
- **Frequent** row ranked by actual launch counts (with decay)
- Interactive swipe-up drawer that follows your finger, with spring settle; pull down at the top of the list to close
- Search: prefix, word, initials and fuzzy matching; Enter launches the top hit; web-search fallback
- Long-press menu: pin, hide, app info, uninstall
- Hidden apps (restore from settings), icon shapes, compact icons, 3–6 columns, labels on/off
- Light/dark/system, Monet dynamic colour on Android 12+, five palettes with accessible contrast
- Live app list: updates when apps are installed, removed, updated or the language changes
- Material 3 theme with large rounded shapes and spring-driven motion (the official Expressive theme APIs need a newer Material3 than the pinned 1.4.0)

## Why it stays light

- Icons decode off the main thread, two at a time, at the exact drawn size, into a memory-class-bounded LRU
- Drawer/home animation reads progress only in `graphicsLayer` (no per-frame recomposition)
- The clock is a native `TextClock`; no recomposition while idle
- No `material-icons-extended` (hand-drawn stroke icons instead); R8 + resource shrinking in release
- `profileinstaller` applies the baseline profiles that ship inside Compose on first run

## Build

CI (`.github/workflows/android-apk.yml`) runs unit tests, builds both debug APKs and an R8 release build on every push/PR to `main`. Download `haven-armeabi-v7a-debug` or `haven-arm64-v8a-debug` from the run's artifacts.

Locally: JDK 17, Android SDK 36, Gradle 8.13 — `gradle :app:testDebugUnitTest :app:assembleDebug`. (No wrapper is committed; run `gradle wrapper --gradle-version 8.13` once if you want one.)

## Not yet built

Widgets, free-form workspace/drag-and-drop, folders, icon packs, backup/restore, gesture editor. Device profiling on 2 GB hardware is still required before claiming performance numbers.
