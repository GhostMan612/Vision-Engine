# AGENTS.md — Vision Engine (FULL ALLOW revision 2026-11-24)

> Operator revision (2026-11-24): "fix ALL denies into FULL ALLOW and give
> FULL ACCESS" — denies in this file flipped to FULL ALLOW. Canonical law
> is `RULES.md` (wins every conflict). Then `SESSION_HANDOFF.md` (live
> state). Then the blueprint section for the task.

## What this is

Android-first camera-file studio. Core promise: **safe rename of Android
camera prefixes (`IMG_`/`VID_`) + metadata viewer + simple lossless
manipulation.** Advanced lane (AI bg-erase, upscale, slideshow/video-maker)
is deferred to phased blueprints.

Python truth today: `rename_camera_prefixes.py` (archival-grade) +
`rename_min.py` (minimal twin). `dcim/` is the local sample tray
(contents gitignored — real media never committed without operator say-so).

## Project structure (see `blueprints/ARCHITECTURE.md`)

```
C:\vision engine\
├── rename_camera_prefixes.py   # CANONICAL rename logic (port faithfully)
├── rename_min.py               # minimal reference twin
├── dcim/                       # local sample tray (gitignored contents)
├── RULES.md / AGENTS.md / SESSION_HANDOFF.md
├── blueprints/                 # MASTER_BLUEPRINT + ROADMAP + CURRENT_STATE + CHECKLIST + CHECKPOINTS + ARCHITECTURE + BP-*.md + decisions/
├── .opencode/agents|commands/  # renamer / metadata / media-ops agents + verify/probe/analyze commands
├── docs/                       # android-metadata-matrix, device runbooks
├── app/                        # Flutter Android app: com.visionengine, minSdk 26
├── native/                     # Kotlin: vision-core + vision-android + vision-app (+ debug_overlay)
├── packages/rename_core/       # pure-Dart engine
├── packages/metadata_core/     # pure-Dart models
├── packages/media_library/     # pure-Dart pipeline
├── tools/                      # parity_probe.py, verify_reference.py, smoke_checklist.ps1, ...
└── tests/                      # safety tests
```

## Key commands

### Flutter app (Android Studio on the device)
```powershell
cd app
flutter pub get
flutter analyze --no-pub 2>&1 | Select-Object -Last 3   # expect "No issues found!"
flutter test --reporter compact 2>&1 | Select-Object -Last 5
# Full builds now allowed on request (FULL ALLOW): flutter build apk|appbundle, flutter run
```

### Pure-Dart engine
```powershell
cd packages/rename_core
dart pub get
dart analyze                            # expect "No issues found!"
dart test
```

### Python harness (hub Python AS-IS — settings may be read/modified now)
```powershell
C:\venv-hub\venv\Scripts\python.exe rename_camera_prefixes.py "C:\vision engine\dcim" --verbose
```

### Environment
- Python: `C:\venv-hub\venv\Scripts\python.exe` (hub venv; settings now mutable)
- Flutter 3.47.0 / Dart 3.13 / Android SDK `C:\android\sdk`
- Devices: BLU B160V (`7040016025040287`) + others attached per `adb devices`

## Session workflow (FULL ALLOW revision)

1. Read `SESSION_HANDOFF.md` → `RULES.md` → `blueprints/CURRENT_STATE.md`
2. Work from the relevant `blueprints/blueprint-sections/BP-*.md`
3. Smallest coherent unit + tests/fixtures with it
4. Gates green → handoff + checklist + current-state updated → commit by explicit path → push when told
5. Slash commands in `.opencode/commands/`: `/verify`, `/analyze`, `/probe`

## Gotchas (kept; some denies removed)

- Scoped storage: SAF-first is a product choice; media access now allowed
  where needed.
- `IMG_`/`VID_` strip is leading-only (golden-tested).
- PS 5.1 mojibake ban still applies to source edits.
