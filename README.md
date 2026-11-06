# Haven Launcher

Haven is a lightweight Android launcher that pairs Material 3 colour and expressive-style rounded shapes with spring motion and a restrained, Nothing-style layout. It is built to stay smooth on low-end (1–3 GB RAM) devices.

- **Application ID:** `app.tab.home`
- **Version:** 0.9.0
- **Minimum Android:** 8.0 (API 26) · **ABIs:** `armeabi-v7a`, `arm64-v8a`
- **Stack:** Java host (activity + native clock) with Kotlin/Compose UI

## Features

- Wallpaper-through home screen with native, self-ticking clock (tap to open alarms)
- Real dock: pin/unpin with long-press; defaults to your dialer, messages, camera and browser
- **Frequent** row ranked by actual launch counts (with decay)
- Interactive swipe-up drawer that follows your finger, with spring settle; pull down at the top of the list to close
- Search: prefix, word, initials and fuzzy matching; Enter launches the top hit; web-search fallback
- **Search sources:** a calculator that evaluates as you type, contacts, and media files — each off
  until you turn it on, each behind its own permission request
- **Home pages:** up to four pages of widgets with a parallax slide and page dots
- **Widgets:** a real `AppWidgetHost`, so any installed widget can be added from the system picker
- **Wallpaper:** set from the gallery (home screen only) or hand off to the system picker
- Long-press menu: pin, hide, app info, uninstall
- Live app list: updates when apps are installed, removed, updated or the language changes

### Icon shapes and styles

Eight shapes — squircle, circle, rounded, cut, teardrop, pebble, arch, hexagon — all defined as a
percentage of the icon, so a shape looks identical in the dock, the Frequent row and the drawer.
The squircle is a real superellipse, not a rounded rectangle.

Three styles decide the artwork treatment: **Masked** crops the icon to the chosen shape,
**Natural** leaves the system's own mask alone, and **Plate** insets the artwork on a tinted tile so
legacy non-adaptive icons match the rest of the grid.

### Motion

Three **feels**, modelled on the launchers people ask for by name:

| Feel | What it does |
| --- | --- |
| **Pixel** | Springy. The tapped icon grows a long way towards the app, the surface barely fades, and the return settles with a little bounce. |
| **One UI** | Crisp. Smaller scale, more fade, higher stiffness, almost no bounce — a snap rather than a glide. |
| **Classic** | Haven's original softer fade. |

- **Open:** the launcher zooms out of the icon you tapped — that tile lifts as the rest of the
  surface zooms past it — and the app takes over mid-transition.
- **iOS-style close:** returning runs the same value back to 0, so the launcher springs back in from
  the icon with a little bounce rather than cutting in.
- **Press feedback:** icons dip and spring back on touch instead of flashing a ripple.
- Three styles (Zoom / Fade / Off), three speeds (Fast / Normal / Calm) and a master switch, so
  motion can be turned off entirely for accessibility or battery.

### Customisation

Everything below is live-previewed in the settings sheet (long-press the home screen or tap the tune icon).

| Group | Options |
| --- | --- |
| Appearance | System/Light/Dark · pure-black (AMOLED) · Monet dynamic colour · five palettes · accent colour (auto, lime, blue, coral, violet) on **every** palette |
| Wallpaper | show/hide · dim 0–85% |
| Home screen | Haven wordmark · clock on/off and size · search pill |
| Dock & Frequent | dock slots 3–5 · dock background · Frequent row on/off and 0–5 slots |
| Icons & grid | eight icon shapes · three icon styles (masked / natural / plate) · three icon sizes · labels on/off · 1–2 label lines · 3–6 drawer columns |
| Drawer | opacity 60–100% · web-search engine (Google, DuckDuckGo, Brave) |
| Pages & widgets | 1–4 home pages · add and remove hosted widgets |
| Search sources | calculator · contacts · files (each permission-gated) |
| Wallpaper | set from gallery · system wallpaper picker |
| Motion | animations on/off · Zoom/Fade/Off · Pixel/One UI/Classic · Fast/Normal/Calm · haptics |
| Performance | Auto / Lean / Full — device-aware icon cache, decode concurrency and page pre-composition |
| Also | hidden-apps manager, one-tap reset to defaults |

## Why it stays light

- Icons decode off the main thread, two at a time, at the exact drawn size, into a memory-class-bounded LRU; a package update evicts only that package's icons
- Drawer, home and the launch transition read their progress only in `graphicsLayer` (no per-frame recomposition)
- The clock is a native `TextClock`; no recomposition while idle
- No `material-icons-extended` (hand-drawn stroke icons instead); R8 + resource shrinking in release
- `profileinstaller` applies the baseline profiles that ship inside Compose on first run

## Build

CI (`.github/workflows/android-apk.yml`) runs unit tests, builds both debug APKs and an R8 release build on every push/PR to `main`. Download `haven-armeabi-v7a-debug` or `haven-arm64-v8a-debug` from the run's artifacts.

Locally: JDK 17, Android SDK 36, Gradle 8.13 — `gradle :app:testDebugUnitTest :app:assembleDebug`. (No wrapper is committed; run `gradle wrapper --gradle-version 8.13` once if you want one.)

## Not yet built

Free-form widget placement and drag-and-drop, folders, icon packs, themed (monochrome) icons, backup/restore, gesture editor. Device profiling on 2 GB hardware is still required before claiming performance numbers.

See [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) for the performance architecture, [`docs/AUDIT.md`](docs/AUDIT.md) for the review history and [`docs/MVP.md`](docs/MVP.md) for status.
