# Haven MVP implementation

## Included

- Launcher HOME activity and app-list entry point.
- PackageManager-backed app discovery and explicit activity launching.
- Native Compose home surface and lazy app drawer.
- Search-as-you-type, configurable grid columns and optional app labels.
- Swipe-up drawer opening and Android back dismissal.
- DataStore-persisted appearance and layout settings.
- Android 12+ Monet dynamic colours with manual accent fallback.
- Adaptive icon and dark/light/system appearance.

## Deferred

- True usage-ranked favourites and user-managed dock pinning.
- Workspace placement, folders, drag-and-drop and widget host.
- Icon packs, per-app hiding, backup/restore and full gesture editor.
- Optional WebView theme studio. It must remain isolated from the launcher rendering path.

## Validation checklist

- Build debug and release variants with JDK 17 / SDK 36.
- Confirm HOME selection and regular launcher icon entry on API 26, 30, 31 and 35.
- Verify app discovery on Android 11+ package visibility behavior.
- Verify Monet follows wallpaper palette on API 31+ and manual colours work below API 31.
- Profile scrolling and drawer animation on 2 GB and 3 GB devices.
- Test process recreation, missing/uninstalled apps and large app inventories.
