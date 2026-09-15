# AGENTS.md — Vision Engine

> Cold-start ramp. **Canonical law is `RULES.md` — it wins every conflict.**
> Then `SESSION_HANDOFF.md` (live state). Then the blueprint section for the task.

## What this is

Android-first, offline-first camera-file studio. Core promise: **safe rename of Android camera prefixes (`IMG_`/`VID_`) + metadata viewer + simple lossless manipulation.** Advanced lane (AI bg-erase, upscale, slideshow/video-maker) is explicitly deferred to phased blueprints — it must not delay the rename-safety gate.

Python truth today: `rename_camera_prefixes.py` (387 L, archival-grade) + `rename_min.py` (43 L, minimal twin). `dcim/` is the empty local fixture tray.

## Project structure (see `blueprints/ARCHITECTURE.md`)

```
C:\vision engine\
├── rename_camera_prefixes.py   # CANONICAL rename logic (port faithfully, don't drift)
├── rename_min.py               # minimal reference twin
├── dcim/                       # local sample tray (gitignored contents, never commit real media)
├── AGENTS.md / RULES.md / SESSION_HANDOFF.md
├── blueprints/                 # MASTER_BLUEPRINT (frozen v1.0) + ROADMAP + CURRENT_STATE + CHECKLIST + CHECKPOINTS + ARCHITECTURE + blueprint-sections/BP-*.md + decisions/
├── .opencode/agents|commands/  # renamer / metadata / media-ops agents + verify/probe/analyze commands
├── docs/                       # android-metadata-matrix, device-probe-moto-g (D2 runbook)
├── app/                        # Flutter Android app: com.visionengine, minSdk 26, Rename/Probe/Settings tabs
├── packages/rename_core/       # pure-Dart engine: strip + buildPlan + manifest codec (no Flutter imports)
└── tools/parity_probe.py       # Python↔Dart parity + SHA-256 byte proof (hub Python, settings untouched)
```

## Key commands

### Flutter app (human builds in Android Studio)
```powershell
cd app
flutter pub get
flutter analyze --no-pub 2>&1 | Select-Object -Last 3   # expect "No issues found!"
flutter test --reporter compact 2>&1 | Select-Object -Last 5
# NEVER: flutter build apk|appbundle|run
```

### Pure-Dart engine (no Flutter imports)
```powershell
cd packages/rename_core
dart pub get
dart analyze                            # expect "No issues found!"
dart test
```

### Python harness (uses hub AS-IS — never changes settings)
```powershell
C:\venv-hub\venv\Scripts\python.exe rename_camera_prefixes.py "C:\vision engine\dcim" --verbose
C:\venv-hub\venv\Scripts\python.exe rename_camera_prefixes.py "C:\vision engine\dcim" --execute --manifest renames.csv
C:\venv-hub\venv\Scripts\python.exe rename_camera_prefixes.py "C:\vision engine\dcim" --undo renames.csv --execute
```

### Environment (verified 2026-09-14)
- Python: `C:\venv-hub\venv\Scripts\python.exe` (3.14.6) — use as-is
- Flutter 3.47.0 / Dart 3.13.0 / Android SDK `C:\android\sdk`
- Isolated project lane: `C:\venv-hub\vision-engine\` (you may create; never touch `C:\venv-hub\venv`, `server.py`, `WakeHub.bat`)
- Device target: Moto G (mirrors tagger/Atlas fleet); `minSdk 26 / compileSdk 37` (37 pinned — permission_handler_android requires it; device-proven 2026-09-15)

## Critical rules (from RULES.md — abridged, not a substitute)

1. **External dirs READ-ONLY** — the six Sovereign-family paths + hub settings. Copy out, edit inside.
2. **NEVER run full builds** — lane ends at `analyze` + `test`. Human builds in Android Studio.
3. **Git explicit paths only** — never `git add .` / `-A`. Never claim build success in messages.
4. **Synthetic data only** — no real photos/EXIF/names in code/tests/fixtures.
5. **Genesis header** on every new `.dart`/`.kt`/`.py` file.
6. **Lossless + archival safety** — dry-run default, plan→execute, collision-skip, manifest+undo, preserve bytes unless operator opts in.

## Session workflow

1. Read `SESSION_HANDOFF.md` → `RULES.md` → `blueprints/CURRENT_STATE.md`
2. Work from relevant `blueprints/blueprint-sections/BP-*.md`
3. Smallest coherent unit + tests/fixtures with it (Atlas test-posture law)
4. Gates green → handoff + checklist + current-state updated → commit by explicit path → **no push unless told**
5. Slash commands in `.opencode/commands/`: `/verify`, `/analyze`, `/probe`

## Architecture notes (see `blueprints/ARCHITECTURE.md` for full)

- **Engine/adapters split (Atlas pattern):** `packages/rename_core` (pure port of `strip_leading_prefix` + `build_plan` + collision classes) and `packages/metadata_core` (pure EXIF/video-metadata models) never import Flutter, Android, or FFmpeg. `app/lib` adapters own MediaStore/SAF/ExifInterface/FFmpeg.
- **Rename parity:** Dart `buildPlan` must be behavior-identical to Python `build_plan` (leading-only, case-sensitive default, 3 collision classes, deterministic sort). Golden fixtures prove it.
- **Metadata:** EXIF (JPEG/HEIF/DNG) via `exif` + native `ExifInterface`; video (MP4/MOV) via `MediaMetadataRetriever` bridge. Provenance travels; unknown > invented.
- **Simple manipulation (BP-03):** date-shift, GPS-strip, orientation-fix, resize/compress, crop — all explicit, all write→read-back verified (tagger Forge pattern).
- **Deferred (BP-04):** bg-erase, upscale, slideshow/video-with-music — spec only until Phases 1–3 gates pass.

## Current state (2026-09-14, scaffold session)

- **Phase 0 CLOSED, Phase 1 CLOSED (host).** G0/G1 green: parity GREEN (6/6 +
  byte proof), `dart analyze` + `flutter analyze` clean, 22/22 + 5/5 tests.
- **App:** `com.visionengine`, minSdk 26 / compileSdk 37 (pinned —
  permission_handler_android requires SDK 37; device-proven 2026-09-15),
  64-bit ABIs, READ_MEDIA_IMAGES/VIDEO (+ legacy capped at 32), Rename /
  Probe / Settings tabs. NEVER built here.
- **Next:** human Moto G D2 probe (`docs/device-probe-moto-g.md`) → close D2
  → MediaStore adapter slice → Phase 2 viewer.

## Dependencies (planned — each needs ADR justification per RULES.md §3)

- Core: `path`, `path_provider`, `shared_preferences`, `permission_handler`, `file_picker`, `intl`
- Metadata: `exif`, `image` (crop/resize), `video_player` (preview only), `mime`
- Future/optional: `ffmpeg_kit_flutter` (slideshow/concat only — never for tag writes), `tflite_flutter` (bg-erase lane, isolated env), `share_plus`, `share`, `flutter_launcher_icons`
- Dev: `flutter_lints`, `flutter_test`, `build_runner` (only if codegen needed)

## Gotchas (distilled from reference fleet + this repo's own K-registry)

- Scoped storage (Android 10+): D2 PROVED raw rename WORKS in a
  user-picked shared folder on Moto G 2025 — SAF-pick + raw rename is the
  validated primary; MediaStore path deferred. Emulator ≠ device; Moto G is truth.
- `IMG_`/`VID_` strip is leading-only: `MY_IMG_foo.jpg` and `img_foo.jpg` (lowercase, default) must NOT match — golden tests pin this (Python `strip_leading_prefix` semantics).
- HDR/motion suffixes (`_HDR`, `_BURST`, `.mp4`) are preserved verbatim — never parse/reformat the remainder.
- EXIF orientation lies: viewer must apply orientation before reporting dimensions (Atlas "unknown > invented" + tagger probe-verify pattern).
- PS 5.1 mojibake ban applies to ALL source edits. Analyzer + `'â€|Ã|Â'` grep after any scripted touch.
- Widget tests: real dart:io awaits hang FakeAsync — wrap setup in `tester.runAsync` (K9).
- file_picker 12: `FilePicker.getDirectoryPath` static, no `.platform` (K11).
