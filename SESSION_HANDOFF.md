# SESSION_HANDOFF.md — Vision Engine (live state)

> Update every session per RULES.md §4.2. Cold-start entry point after RULES.md.

## DOCUMENT MAP — cold-start hooks (read top-to-bottom)

| # | File | Holds | When to read |
|---|------|-------|--------------|
| 0 | `AGENTS.md` (root) | Compact ramp: structure, commands, env, architecture notes | automatic |
| 1 | `RULES.md` | **CANONICAL** operating law: read-only paths, hub-immutable, build boundary, §3 technical laws incl. mojibake ban | EVERY session, before any edit |
| 2 | THIS FILE | Latest deltas, next actions, open decisions, toolchain notes | EVERY session |
| 3 | `blueprints/CURRENT_STATE.md` | Verified per-file map, known-issue registry, toolchain freeze | before writing code |
| 4 | `blueprints/ROADMAP.md` | Phase tracker (0–5) with gates | when planning/phases |
| 5 | `blueprints/VISION_ENGINE_MASTER_BLUEPRINT.md` | Frozen product spec (v1.0 target) | before novel features |
| 6 | `blueprints/ARCHITECTURE.md` | Engine/adapters split, storage flow, metadata flow | structural changes |
| 7 | `blueprints/blueprint-sections/BP-*.md` | Executable task slices per phase | task work |
| — | `rename_camera_prefixes.py`, `rename_min.py` | **Executable truth** for rename semantics — prose never overrides | any rename-semantics question |

Conflict law: RULES.md > other docs; executable files (`*.py`, `pubspec.yaml`, `build.gradle`) > prose.

## Where we are (2026-09-15, Phase 2 Work Package 2B CLOSED host-side — STOPPED)

- **2B DONE, change control holds.** Kotlin `MetadataBridge` + channel +
  `MetadataAdapter` + 18/18 host tests + synthetic fixtures +
  device integration test authored (human Moto G run pending) +
  ADR-003 + boundary/runbook docs. Gates green (below). NO Viewer/UI/
  enumeration/thumbnails/MediaStore/G5/DCIM/writes started.
- **Gates 2B:** metadata_core analyze clean + 19/19; rename_core clean +
  22/22; app analyze clean + 23/23. New dep: exifinterface 1.4.1
  (native, justified ADR-003) + `integration_test` (SDK dev) +
  `metadata_core` path dep. rename_core behavior untouched.

## Where we were (2026-09-15, Phase 2 Slice 1 CLOSED — STOPPED)

- **Slice 1 DONE, change control holds.** `packages/metadata_core` v0.1.0
  (models + provenance + fixture codec + 3 goldens, 18/18 tests, analyze
  clean). GQ3 doc contradiction corrected (ARCHITECTURE ×2, blueprint
  table, matrix K1 — docs only). No Viewer/UI/platform/extraction work
  started. No Slice 2 — awaiting authorization.
- **Gates this slice:** metadata_core analyze clean + 18/18; rename_core
  analyze clean + 22/22; app analyze clean + 5/5. rename_core, app code,
  probes, D2 behavior untouched.

## Where we were (2026-09-15, Phase 2 research session)

- **Phase 2 = Metadata viewer (read-only first) — MAP FROZEN FOR REVIEW,
  NOT IMPLEMENTED.** `blueprints/PHASE_2_RESEARCH_MAP.md` holds the
  A/B/C report + 24-point execution map + 5 open questions (GQ1–GQ5).
  Awaiting operator review → lock → authorize. No production code touched.
- **Git:** already initialized last turn; verified in-sync (`main` ==
  `origin/main` == `01fa466`, clean tree). Nothing to re-init; this
  session commits only the map doc + this handoff.
- **Archaeology verdict:** Phase 2's meaning is unanimous across ROADMAP,
  BP-02, CHECKPOINTS G2, CHECKLIST, AGENTS, handoff (read-only metadata
  viewer). One real tension found: ARCHITECTURE §rename prescribes
  MediaStore for shared collections, but D2 + operator directive froze
  SAF-pick + raw rename — reword queued for next authorized task (D1 in
  the map doc), NOT edited here. Minor: the Python script's internal
  "Phase 1/Phase 2" (plan/execute) is unrelated to project phases.
- **D2 scope honesty (new precision):** D2 proved rename of APP-CREATED
  files in shared storage. Third-party (camera-created) file rename is
  NOT proven — G5/DCIM territory, still deferred, never probed.

- **Phase 0 CLOSED, Phase 1 CLOSED (host side).** `packages/rename_core`
  (pure Dart port) + `tools/parity_probe.py` + `app/` shell
  (`com.visionengine`, minSdk 26, compileSdk 37 pinned) scaffolded, gates
  green. Git initialized + pushed (D5 done). No writes outside
  `C:\vision engine` (+ build cache); hub settings untouched.
- **D1 DECIDED:** `com.visionengine` (namespace + applicationId; MainActivity
  moved to `com.visionengine`, stale `vision_engine` subpackage deleted).
- **D2 CLOSED/PASS (2026-09-15, Moto G 2025, human device run).**
  `RAW_RENAME: WORKS` on real shared storage
  (`/storage/emulated/0/Pictures/VE_TEST`): to_rename=2, renamed=2,
  errors=0, undo_restored=2, cleaned=true, `SHARED_PROBE: COMPLETE`
  (folder-picker path confirmed in a second identical run).
  Permissions observed: photos=granted, videos=granted, storage=denied
  (expected on API 33+, not a failure). No MediaStore fallback justified —
  raw rename via user-picked folder stays the validated path; rename_core
  and rename behavior UNCHANGED per operator directive. Real DCIM never
  touched.
- **Build fix (human-applied, recorded here):** first `assembleDebug`
  failed — `permission_handler_android` requires SDK 37 while the template
  pinned `flutter.compileSdkVersion` (36). Fixed by `compileSdk = 37`
  (SDK 37 already installed; AGP 9.1.0 + Gradle 9.3.1, debug APK launched).
  Pinned + justified in `build.gradle.kts` + ADR-002 scope. Incidental:
  SDK XML v4 vs cmdline-tools warning (Studio/cmdline skew, benign).
- **Python truth unchanged.** `rename_camera_prefixes.py` /
  `rename_min.py` / `dcim/` untouched.
- **Toolchain notes:** template minSdk default is 24 (pinned 26 per BP-05);
  template compileSdk is `flutter.compileSdkVersion` = 36 on Flutter 3.47
  (matches BP-05). Resolved: file_picker 12.3.0 (static
  `FilePicker.getDirectoryPath`, no `.platform`), permission_handler +
  path_provider + shared_preferences via `flutter pub add`.

## What shipped this session (delta)

- `packages/rename_core` v0.1.0: `stripLeadingPrefix` + `buildPlan`
  (staying-set, 3 collision classes, deterministic sort) + manifest CSV codec
  (superset: +`content_uri`) + undo-order codec. 22/22 host tests
  (strip/plan/manifest units + 6 golden fixtures). `dart analyze` clean.
- `tools/parity_probe.py`: 6/6 Python↔Dart plan cases PASS + SHA-256
  byte-proof PASS (`parity: GREEN`). Chain-rename case (`IMG_VID_a` →
  `VID_a` → `a`) proven identical on both sides.
- `app/` (vision_engine): Rename tab (seed → preview → execute → manifest →
  undo on app-private `vision_demo`), Probe tab (D2: app-private + shared
  probes, permission status/request, copyable report), Settings tab
  (prefixes/case/recursive persisted). `LocalRenameAdapter` (preview/execute/
  undo over dart:io) + `storage_probe` (self-creating/self-cleaning
  VE_PROBE dirs). 5/5 `flutter test`, `flutter analyze` clean. NEVER built.
- `docs/device-probe-moto-g.md`: exact human steps for the Moto G D2 run.
- Lessons banked: widget-test dart:io needs `tester.runAsync` (K9);
  `List.join(',')` ≠ `List.toString()` in comparisons (K10); file_picker 12
  API change (K11).

## Next actions

1. **Operator:** run `docs/device-validation-2b.md` (`flutter test
   integration_test` on Moto G) and paste results; then authorize the
   Viewer slice or redirect. No code until authorized.
2. D3/D4 stay deferred (no ffmpeg/tflite until G1–G3 + promotion ADR).
3. Git discipline stands: explicit paths, gates-only messages, no force-push.

## Open decisions (deferred, do NOT implement yet)

- D1: DECIDED `com.visionengine` (shipped 2026-09-14).
- D2: CLOSED/PASS 2026-09-15 — raw rename via user-picked folder is the
  validated path; MediaStore adapter deferred (not justified by evidence).
- D3: `ffmpeg_kit_flutter` inclusion — ONLY for BP-04 slideshow/concat; never for tag writes (tagger law). Needs ADR + size-budget sign-off (~40MB/ABI).
- D4: On-device bg-erase engine (TFLite U²-Net vs وا cloud) — deferred to BP-04; no dep until Phases 1–3 pass.
- D5: DONE 2026-09-15 — repo initialized, 81 files committed by explicit
  path (`fc4317f`), GitHub README merged (`28d7205`), pushed to
  `https://github.com/GhostMan612/Vision-Engine` (`main` tracks
  `origin/main`). Discipline from here: explicit paths only, gates-only
  messages, no force-push.

## Toolchain notes (frozen)

- `C:\venv-hub\venv` Python 3.14.6 is the ONLY interpreter for harness scripts. No TF wheels on 3.14 — ML prototyping gets its own `.venv-tf` (Python 3.12) inside the project lane if ever needed (Recovery pattern).
- Flutter 3.47 / Dart 3.13 / `compileSdk 37` (pinned, device-proven) /
  `minSdk 26` / `targetSdk 36` (Flutter-pinned) / AGP 9.1.0 / Gradle 9.3.1.
- PS 5.1 mojibake ban in force for all source edits.
