# VEDebugOverlay — reusable smoke-test checklist app (native Kotlin/Compose)

## What it is

A small, standalone debug app (`com.visionengine.debugoverlay` by default)
deployable on a smoke device next to the main app. It shows a checklist of
smoke items — some **automated** (verified by the app itself) and some
**human** (checked in the UI) — and the checklist state is readable via adb.

Design sketch:

```
debug_overlay (Android app, :debug_overlay)
├── MainActivity          — the checklist screen (Compose): checkboxes,
│                           notes, status badge; "Run auto checks" button
├── ChecklistStore        — app-private (SharedPreferences; never DCIM)
├── AutoChecks            — automated legs (see below)
└── ChecklistProvider     — exposes the checklist via
                           content://<authority>/items
```

Automated legs (`AutoChecks`):

- `launch_offline` — starts the main app via `startActivity`, waits for it
  to appear in `ActivityManager.getRunningAppProcesses` (the no-root
  equivalent of `pidof <package>`), restores nothing else.
- `focus_offline` — verifies the main app is in the running app processes.

## Check the checklist via adb

```powershell
# One-liner:
adb shell content query --uri content://com.visionengine.debugoverlay.checklist/items

# Or with the helper script:
tools\smoke_checklist.ps1 [serial] [authority]
```

Output columns: `item_id`, `label`, `status` (OPEN/PASS/FAIL), `checked`
(0/1), `notes`.

## Making it global for another repo

1. **Application id** (so it can coexist): build with
   `.\gradlew.bat :debug_overlay:installDebug -PoverlayAppId=com.yourapp.debugoverlay`
   — the provider authority follows it automatically (`${applicationId}.checklist`).
   Update `tools\smoke_checklist.ps1`'s default `$Authority` if you use a different name.
2. **Main app** it automates: edit `OverlayConfig.MAIN_APP_PACKAGE`
   (`native/debug_overlay/src/main/kotlin/com/visionengine/debugoverlay/OverlayConfig.kt`).
3. **Item set**: edit `DefaultItems`
   (`native/debug_overlay/src/main/kotlin/com/visionengine/debugoverlay/DefaultItems.kt`).
   Set `auto = true` on legs the app can run itself; humans check the rest.

Storage stays app-private — never points at DCIM or the media library.
