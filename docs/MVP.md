# Haven status

## Shipped (0.7.0)

- HOME activity (`singleTask`), regular launcher entry, Home-press collapses drawer/settings and
  plays the close transition
- PackageManager discovery keyed per activity, live refresh on package/locale changes, per-package
  icon cache invalidation
- Wallpaper-through home with adjustable dim, native clock (three sizes), usage-ranked Frequent row
  (0–5 slots), pin-able dock (3–5 slots, optional background)
- Finger-following drawer, nested pull-to-close, ranked search with a choice of web engine
- Long-press menu (pin, hide, app info, uninstall), hidden-apps management
- DataStore-backed settings, read before first frame (no theme flash); every value validated on
  read and write, plus a reset to defaults
- Bounded off-thread icon cache with adaptive-icon re-masking to the chosen shape
- Material 3 theme with rounded shapes and spring motion, Monet, five palettes, accent colour on
  every palette, AMOLED black, system-bar icon contrast
- Eight icon shapes (superellipse squircle, circle, rounded, cut, teardrop, pebble, arch, hexagon)
  and three icon styles (masked, natural, plate)
- iOS-style open/close transition (zoom or fade, three speeds, master switch), a lifting launch
  tile and press feedback
- Unit tests for search ranking, usage encoding, dock planning and motion specs; CI runs tests,
  debug APKs and an R8 release build

## Deferred

- Widget host, workspace placement, folders, drag-and-drop
- Icon packs, themed (monochrome) icons, backup/restore, gesture editor
- Per-app icon overrides
- Work-profile apps (needs `LauncherApps`), app shortcuts, notification badges
- Optional WebView theme studio (must stay out of the render path)

## Device validation checklist

- HOME selection and launch on API 26, 30, 31, 35; Android 11+ package visibility
- Monet follows wallpaper on API 31+; manual palettes below 31
- Open/close transition on a device with system animations set to 0.5× and off
- Profile drawer drag and icon scroll on 2 GB and 3 GB devices
- Process recreation, uninstalled apps, very large app inventories, RTL
