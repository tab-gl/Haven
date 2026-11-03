# Haven MVP

## First usable milestone

1. Register as the system HOME activity.
2. Discover launchable activities using the launcher intent query.
3. Render a native home surface and launch selected apps.
4. Establish the Haven visual tokens and typography.
5. Verify process recreation and package visibility on API 26+.

## Next implementation steps

- Add app icons with bounded memory/disk caching.
- Replace the temporary all-apps home list with a workspace and dedicated drawer.
- Add search with in-memory filtering.
- Persist workspace placement and preferences.
- Add gesture handling and interruptible native transitions.
- Add Android widget hosting.
- Add the optional WebView theme editor only after the native experience is stable.

## Known MVP gaps

- The current UI is a functional scaffold, not the final visual design.
- App icons, folders, dock, search, widgets, drag-and-drop and persistent layout are not implemented.
- The clock refresh loop is scoped to the visible composable and is cancelled with its lifecycle; production behavior should use a lifecycle-aware ticker and minute-boundary scheduling.
- No automated tests or CI workflow have been added yet.
