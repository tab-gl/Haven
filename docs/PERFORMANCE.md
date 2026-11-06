# Haven performance architecture

A response to the "animation-rich, memory-light" proposal, and what 0.9.0 actually ships.

## The decisions asked for

| Question | Answer | Why |
| --- | --- | --- |
| **Minimum Android version** | **8.0 (API 26)** — unchanged | It is already Haven's `minSdk`. Dropping to 7.0 buys almost nothing: API 26 is where adaptive icons, notification channels and the modern `ShortcutManager` land, and Haven already uses adaptive-icon rendering and `TextClock`. Going to 10 would cost real device reach for no API Haven needs. |
| **Primary priority** | **Flagship smoothness with a low-RAM fallback** | "Maximum compatibility" is a *constraint*, not a priority — and it is already satisfied by targeting 26. The interesting engineering is keeping one design language while spending less memory on it, which is what the profile below does. |

## What I agree with

- **Measure before rewriting.** The proposal's targets (30–60 MB idle, < 1 s cold start, 60 FPS, < 50 ms touch, < 15 MB APK) are reasonable *targets*. Haven's current APK is **12.1 MB**, so the size target is already met without giving anything up.
- **Animate transforms and alpha, never blur or shadows.** Haven already does exactly this: the launch transition, the drawer and the pager parallax are all read inside `graphicsLayer` lambdas, so a running animation costs a layer update and zero recomposition.
- **Stable item identities, downsampled icons, memory-aware caching.** Already true: icons decode at the drawn size into an LRU bounded by the heap class, the drawer grid is keyed by component name, and a package update evicts only that package.
- **Two performance profiles.** This is the right idea, and it is now built — see below.
- **No persistent services or polling.** Haven has no service, no alarm, no polling loop; the app list updates from package broadcasts and the clock is a native `TextClock` that ticks itself.

## Where I disagree

### "Don't build every screen with Compose"

Haven already takes this advice, and more precisely than a blanket rule would:

| Surface | Implementation | Why |
| --- | --- | --- |
| Clock | native `TextClock` in a `LinearLayout` | ticks without a single Compose recomposition |
| Widgets | native `AppWidgetHostView` | the system draws them; Compose only positions them |
| Home, drawer, settings | Compose | gesture + state-heavy surfaces where Compose is *less* code and less bug-prone |

Rewriting the remaining three screens in Views is not an optimisation, it is a rewrite: it would discard the drawer gesture handling, the launch transition and every fix up to 0.9.0, and it would take weeks before the first frame was drawn again. The measurable costs of Compose here are startup and idle memory, and neither is fixed by changing the toolkit — they are fixed by doing less work, which is what the profile does.

### "Native property animations" instead of Compose animation

Both run on the same `Choreographer` frame callback. Compose's animation cost is *recomposition*, and Haven has already eliminated it on every animated surface by reading state only inside `graphicsLayer`. Moving to `ObjectAnimator` would not change the frame cost; it would only change who writes the code.

### "Room, only if needed"

Not needed, and it will not be. A launcher's persistent state is a few dozen scalars plus two small lists (hidden apps, widgets). DataStore handles that in one file read. Adding SQLite would add ~1 MB and a schema migration surface for no query Haven makes.

### Things I cannot promise

The proposal's RAM figures are targets, not measurements, and I have not profiled Haven on hardware. Compose's baseline (composition runtime, snapshot state, the Skia-backed renderer) puts a floor under idle memory that a pure-View launcher does not have — plausibly 20–30 MB. Anyone quoting "30–60 MB idle" for a Compose launcher should verify it on a 1 GB device with `adb shell dumpsys meminfo app.tab.home` before it becomes a release criterion. **I am not going to claim Haven hits those numbers until it is measured on your hardware.**

## What 0.9.0 ships: device-aware profiles

`data/PerformanceProfile.kt` resolves one of two profiles and everything else reads its traits.

| Trait | Lean | Full |
| --- | --- | --- |
| Icon LRU budget | 2 MB | heap/14, clamped to 4–12 MB |
| Concurrent decodes | 1 | 2 |
| Largest icon bitmap | 144 px | 192 px |
| Preload dock + Frequent | no | yes |
| Neighbouring pages composed | 0 | 1 |

Detection: `ActivityManager.isLowRamDevice` first, then a heap class of 96 MB or less. A device with 1 GB of RAM normally sets the low-RAM flag, which is why the flag is trusted over a hard-coded RAM threshold.

The user can override it — **Customise → Performance → Auto / Lean / Full** — and the sheet states exactly what the resolved profile is spending.

### Memory pressure

`IconCache` now implements `ComponentCallbacks2`:

- `TRIM_MEMORY_UI_HIDDEN` → the LRU is trimmed to half its budget (the launcher is in the background; the icons will be re-decoded when they are next drawn).
- `TRIM_MEMORY_RUNNING_LOW` and above → the cache is emptied outright.

A launcher that ignores trim signals is a launcher the system kills, and being killed is the one thing that reliably makes a home screen feel slow.

## What is still not done, and why

- **Baseline profiles.** `profileinstaller` is already a dependency and applies the profiles shipped inside Compose, but Haven has no profile of its own. Generating one needs a macrobenchmark run on a device — it cannot be produced from a build machine.
- **JankStats / Perfetto numbers.** Both need hardware. Adding the dependency without a device to read it from would be decoration.
- **Cold-start measurement.** The current design reads DataStore once on the main thread before the first frame, deliberately, to avoid a theme flash. Whether that is under a second on a 1 GB device is a measurement, not a decision.
