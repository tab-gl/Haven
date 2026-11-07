# Haven audit — 0.5.0 → 0.10.0

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
| 6 | **"Squircle" was really a circle.** `RoundedCornerShape(34.dp)` is larger than the half-width of a 42 dp dock icon, so the radius collapsed to a circle at home sizes while looking right only in the drawer. Corner radii are now percentages of the icon, and the squircle is a real superellipse path. |
| 7 | **Package broadcasts reloaded twice** for a replacing update, and icon state was not part of the reload at all. | Single conflated flow carrying the changed packages; the reload is debounced once. |

## Smoothness

- Dock and Frequent-row planning moved out of the composables into `data/DockPlanner.kt`, so the
  home screen only remembers finished lists and the logic is unit tested.
- The launch transition and the drawer both drive `graphicsLayer` only, so neither costs a
  recomposition per frame.
- The default dock now tops up with the most-launched apps instead of rendering with gaps.
- Motion is derived once per settings change (`motionSpecFor`) rather than rebuilding springs in
  composition.
- `kotlinOptions` (deprecated in Kotlin 2.2) replaced with the `compilerOptions` DSL.

## Icon shapes and styles (0.7.0)

- Eight shapes: squircle (superellipse), circle, rounded, cut, teardrop, pebble, arch, hexagon.
  All percentage-based, so the same shape renders identically at 32 dp and 60 dp.
- Three styles: `masked` (crop to shape, adaptive icons rendered full-bleed), `natural` (leave the
  system's mask alone) and `plate` (artwork inset 16% on a tinted tile so legacy icons match).
- The style is part of the icon cache key, so switching styles re-decodes instead of showing stale
  artwork, and `invalidatePackage` still drops every size of a changed package.

## Motion refinements (0.7.0)

- The tapped tile lifts (up to 1.34×) while the surface zooms past it, so the app reads as growing
  out of that specific icon rather than out of the screen.
- Opening is stiffer than closing (1.25× vs 0.8×) and fires `startActivity` at 55% instead of 62%:
  leaving feels instant, arriving can breathe.
- The return uses a lighter damping ratio (0.78) for a small settle instead of a dead stop, and the
  zoomed surface holds 28% alpha at full progress so the return never cuts in from black.
- The home recedes further behind the drawer (0.94 scale, 45% dim, 2% lift) while the drawer rises
  with a touch more travel and scale.

## 0.8.0 — motion feels, search sources, pages and widgets

### Motion presets

`motionFeel` selects the *shape* of the transition, not just its speed:

| Feel | stiffness | damping | close damping | zoom | fade | launch at | icon lift |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pixel | 430 | 0.86 | 0.80 | 0.14 | 0.42 | 0.50 | 0.90 |
| One UI | 560 | 0.92 | 0.95 | 0.10 | 0.55 | 0.45 | 0.50 |
| Classic | 380 | 0.88 | 0.78 | 0.18 | 0.72 | 0.55 | 0.34 |

Pixel leans on the shared element (the icon grows a long way, the surface barely fades); One UI
leans on speed and a crisp stop. `motionSpeed` still multiplies stiffness on top of whichever feel
is chosen.

### Search sources

- **Calculator** — `MathEval` is a hand-written recursive-descent parser over an alphabet of digits,
  `+ - * / % ^ ( )`. No `ScriptEngine`, no reflection, so a pasted string can never execute.
  Incomplete expressions (`12+`) deliberately return nothing rather than flashing a result.
- **Contacts** — `CommonDataKinds.Phone`, de-duplicated per person (the phone table repeats a row per
  number), capped at four, tapping dials with `ACTION_DIAL` so nothing is placed without a second tap.
- **Files** — searches the images/video/audio collections rather than `MediaStore.Files`: on API 33+
  the Files collection is restricted to the app's own files, while the media collections are readable
  with the `READ_MEDIA_*` permissions actually granted. `LIKE` wildcards in the query are escaped
  with an explicit `ESCAPE` clause.
- Every lookup is permission-guarded, returns an empty list instead of throwing, and runs on
  `Dispatchers.IO` behind a 220 ms debounce.

### Pages and widgets

- Up to four pages in a `HorizontalPager`; the off-screen page drifts, shrinks and dims for a
  parallax slide, all read inside `graphicsLayer` so paging never recomposes page content.
- `HavenWidgets` wraps an `AppWidgetHost` with a **permanent host id** — changing it would orphan
  every widget the user has added. Widgets are bound through the system picker
  (`ACTION_APPWIDGET_PICK`), which performs the privileged bind on Haven's behalf, so no
  `BIND_APPWIDGET` permission is needed.
- Widget ids and their page are persisted as `id:page` lines; removing a page clamps any widget on it
  back into range instead of hiding it.

### Wallpaper

Two paths: the photo picker (`PickVisualMedia`, no storage permission) decoded down to a 2560 px edge
and applied to `FLAG_SYSTEM` only, or a hand-off to `ACTION_SET_WALLPAPER`.

## 0.8.1 — the empty dock

Reported from a device recording: after changing the icon shape to **Pebble**, the dock kept its
pill and its height but showed no icons at all.

| # | Finding | Fix |
| --- | --- | --- |
| 1 | **An over-rounded shape is not a shape.** Pebble shipped as 46 / 56 / 42 / 58 percent. Two corners share every edge, so the top edge asked for 102% and the left edge for 104% of the icon. Compose does not clamp this; the outline degenerates, and on device that is a clip with nothing left inside it. The settings chip still drew *something* (a circle), which is why it looked fine in the picker. | Corner radii are now data (`IconCorners`), Pebble is 46 / 50 / 42 / 50, and `IconCorners.fits()` is asserted in a unit test for every rounded shape. |
| 2 | **The placeholder was invisible.** A tile with no bitmap drew a `surfaceVariant` square — the exact colour of the dock pill — so an icon that was still decoding, or that failed, made the dock look empty rather than busy. | The tile now shows the app's initial on `surfaceContainerHighest`. A missing icon is legible from now on. |
| 3 | **One stalled decode could freeze every icon.** `IconCache.load` held one of two semaphore permits across an unbounded `getActivityIcon` call, and only caught `Exception`. | The decode is bounded at 4 s and catches `Throwable`, so one bad drawable can no longer starve the queue. |
| 4 | **Icons re-decoded on every size change.** The cache key used the exact pixel size. | Sizes are bucketed to 8 px, and the dock and Frequent icons are preloaded as soon as the app list arrives, so the home screen never opens on placeholders. |
| 5 | **The widget picker leaked ids and failed silently.** A cancelled pick never released the reserved `AppWidgetHost` id, and a pick that returned no id did nothing at all. | Reserved ids are released on cancel, a bound id is verified with `getAppWidgetInfo` before it is stored, and both failure paths now say so. |

## 0.10.0 — the launch animation is gone, and so is its lag

The launcher-side open/close transition was removed outright. It was the wrong trade: `startActivity`
was deferred until the spring reached 50%, so every app launch paid 90–160 ms of extra latency, and
the transition itself faded and scaled the whole surface.

| # | Finding | Fix |
| --- | --- | --- |
| 1 | **The launch transition was the lag.** `LaunchZoom` fired `startActivity` at 45–55% of a spring; on the Balanced preset that is roughly 110 ms of dead time before the app window even starts opening. | Deleted. `startActivity` runs on the tap frame. Android's own window transition is the transition. |
| 2 | **Full-surface alpha forced an offscreen buffer.** The pager faded each page (`alpha = 1 - 0.45·d`), the home faded behind the drawer (`alpha = 1 - 0.55·p`) and the drawer faded in (`alpha = p`). An alpha below 1 on a layer makes Compose composite it through `saveLayer` — a full-screen texture allocation per frame, on every frame of a swipe. | All three are transform-only now. The drawer's own background hides the home, and a single solid-colour rectangle provides the dim. |
| 3 | **Every tile registered a layout callback.** `AppEntry` used `onGloballyPositioned` to report its centre for the zoom origin — a layout-phase callback for every icon on screen. | Removed with the transition. |
| 4 | **Press feedback recomposed the tile.** `animateFloatAsState` was read in composition, so a press re-ran the tile (and its icon) on every animation frame. | An `Animatable` read inside `graphicsLayer`: the dip animates with zero recomposition. |
| 5 | **The drawer left fractional offsets behind.** `animate` stops within the spring's visibility threshold, not exactly on the target. | The settle writes the exact target after the animation, so the drawer is never 0.4% open. |
| 6 | **Motion settings had nothing left to control.** `motionStyle` (zoom/fade/none) and `motionFeel` (pixel/oneui/classic) described a transition that no longer exists. | Removed. `motionSpeed` survives as the drawer/pager spring: Snappy / Balanced / Relaxed. |

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
