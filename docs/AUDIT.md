# Haven 0.6.0 audit

A read-through of the 0.5.0 source, what was wrong, and what changed. Every item below is either
fixed in this release or explicitly left alone with a reason.

## Correctness

| # | Finding | Fix |
| --- | --- | --- |
| 1 | **Stale icons after an app update.** `IconCache` was keyed by `package/activity@size` and never invalidated, so an updated or reinstalled app kept drawing its old icon until process death. | `AppRepository.changes()` now emits the changed package names and `IconCache.invalidatePackage()` evicts only that package's entries. |
| 2 | **A dock could disappear permanently.** `hide()` strips the hidden app from the pinned list. Hiding or uninstalling every pinned app left `pinned` as a non-empty list that resolved to nothing, so the dock rendered empty with no way back except pinning something. | `DockPlanner.dock()` falls back to the default dock when a non-empty custom dock resolves to nothing. A deliberately emptied dock (empty list) still stays empty. |
| 3 | **The drawer's top row felt stuck.** `NestedScrollConnection.onPreScroll` consumed *every* upward scroll while the drawer was between 0 and 1, before the grid saw it, so the first fling at the top of the list did nothing. | The upward hand-over is gone; only unconsumed downward scroll closes the drawer. |
| 4 | **Phantom button on the home screen.** The whole home surface used `combinedClickable(onClick = {})` purely to get a long-press. That exposed a clickable with no action to TalkBack. | Replaced with `pointerInput { detectTapGestures(onLongPress = …) }`, which keeps the gesture and leaves the accessibility tree clean. |
| 5 | **Hard-coded dock size in two places** (`DOCK_SIZE` in the host, `SLOTS` in the home screen) that could drift apart. | Both come from `settings.dockSlots`, and the planner owns the cap. |
| 6 | **Package broadcasts reloaded twice** for a replacing update, and icon state was not part of the reload at all. | Single conflated flow carrying the changed packages; the reload is debounced once. |

## Smoothness

- Dock and Frequent-row planning moved out of the composables into `data/DockPlanner.kt`, so the
  home screen only remembers finished lists and the logic is unit tested.
- The launch transition and the drawer both drive `graphicsLayer` only, so neither costs a
  recomposition per frame.
- The default dock now tops up with the most-launched apps instead of rendering with gaps.
- Motion is derived once per settings change (`motionSpecFor`) rather than rebuilding springs in
  composition.
- `kotlinOptions` (deprecated in Kotlin 2.2) replaced with the `compilerOptions` DSL.

## Customisation added

Ten new persisted settings, all validated on read *and* write so a corrupted or downgraded
preferences file cannot put the UI in an impossible state:

`amoled`, `iconScale` (replaces the old `compactIcons` boolean, which is migrated on read),
`labelLines`, `wallpaperDim`, `showBrand`, `showClock`, `clockScale`, `showSearchPill`,
`showFrequent`, `frequentCount`, `dockSlots`, `dockBackground`, `drawerOpacity`, `animations`,
`motionStyle`, `motionSpeed`, `haptics`, `searchEngine`.

Accents now apply to all five palettes (previously only Aurora reacted to the accent picker), and
"auto" keeps each palette's own colour. The settings sheet was regrouped into sections with
sliders, steppers and a two-tap reset.

## Motion

`LaunchZoom` tracks one 0..1 value describing how far the launcher has been pushed towards the
launched app. Tapping an icon records its centre in window coordinates, runs the value forward and
fires `startActivity` at 62% — so the zoom is already moving when the app window arrives. Returning
runs the same value back to 0, which reads as the app collapsing into its icon. Icons also dip and
spring back on touch.

## Deliberately not changed

- **Widgets, folders, drag-and-drop, icon packs, backup/restore.** Out of scope for a smoothing
  pass; still listed as deferred in `docs/MVP.md`.
- **`readBlocking()` on first composition.** It runs one DataStore read on the main thread, which is
  what prevents a theme flash before the first frame. It is a single small file read; the
  alternative (start transparent, then animate the theme in) is more visible than the cost.
- **No Gradle wrapper is committed.** CI pins Gradle 8.13 explicitly; adding a wrapper jar is a
  binary in the repo for no gain while the CI path works.
- **`splits` still ships two ARM APKs.** An x86_64 APK would help emulator testing but doubles the
  artifact set; the manifest has no native code, so a universal APK is only a CI convenience.

## Verification

- `gradle :app:testDebugUnitTest` — 25 tests, 0 failures (search ranking, usage encoding, dock
  planning, motion specs).
- `gradle :app:assembleDebug` — both ABI APKs build.
- `gradle :app:assembleRelease` — R8 + resource shrinking build in CI.
- Still needed on hardware: the close transition after a real app launch, drawer drag on a 2 GB
  device, and RTL.
