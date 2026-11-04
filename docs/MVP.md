# Haven status

## Shipped (0.5.0)

- HOME activity (`singleTask`), regular launcher entry, Home-press collapses drawer/settings
- PackageManager discovery keyed per activity, live refresh on package/locale changes
- Wallpaper-through home, native clock, usage-ranked Frequent row, pin-able dock
- Finger-following drawer, nested pull-to-close, ranked search with web fallback
- Long-press menu (pin, hide, app info, uninstall), hidden-apps management
- DataStore-backed settings, read before first frame (no theme flash)
- Bounded off-thread icon cache with adaptive-icon re-masking to the chosen shape
- Material 3 Expressive theme, Monet, five palettes, system-bar icon contrast
- Unit tests for search ranking and usage encoding; CI runs tests, debug APKs and an R8 release build

## Deferred

- Widget host, workspace placement, folders, drag-and-drop
- Icon packs, themed (monochrome) icons, backup/restore, gesture editor
- Work-profile apps (needs `LauncherApps`), app shortcuts, notification badges
- Optional WebView theme studio (must stay out of the render path)

## Device validation checklist

- HOME selection and launch on API 26, 30, 31, 35; Android 11+ package visibility
- Monet follows wallpaper on API 31+; manual palettes below 31
- Profile drawer drag and icon scroll on 2 GB and 3 GB devices
- Process recreation, uninstalled apps, very large app inventories, RTL
