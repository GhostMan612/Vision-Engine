# CURRENT_STATE.md — Vision Engine (verified state)

> Updated every session per RULES.md §4.2. Evidence > status.

## Snapshot (2026-09-14, scaffold session)

- **Repo:** `C:\vision engine\` — git `main` tracks
  `github.com/GhostMan612/Vision-Engine` (init + push 2026-09-15:
  `fc4317f` scaffold, `28d7205` README merge). Contents:
  `rename_camera_prefixes.py`, `rename_min.py`, `dcim/` (empty),
  operating docs, `blueprints/`, `docs/`, `.opencode/`,
  `packages/rename_core/` (v0.1.0, 22/22 tests, analyze clean),
  `tools/parity_probe.py` (GREEN), `app/` (vision_engine, 5/5 tests,
  analyze clean, NEVER built — human builds in Android Studio),
  `packages/metadata_core/` (v0.1.0, 19/19 tests, analyze clean —
  Phase 2 Slice 1, models/provenance/codec/goldens only),
  2B extraction layer (Kotlin `MetadataBridge` + channel +
  `MetadataAdapter`, 18/18 host tests; device integration test authored,
  human run pending; exifinterface 1.4.1; NO Viewer/UI).
- **Gates:** G0 CLOSED, G1 CLOSED (host). D2 CLOSED/PASS on Moto G 2025
  (2026-09-15): RAW_RENAME WORKS on shared `Pictures/VE_TEST` (2/2 renamed,
  2/2 undo, cleaned, COMPLETE ×2 runs); photos/videos granted,
  storage denied (expected 33+). G2–G5 not started (G5 DCIM round-trip
  explicitly out of scope until tasked).
- **Toolchain (device-proven 2026-09-15):** `compileSdk = 37` pinned —
  `permission_handler_android` requires SDK 37 (template default 36 fails
  `compileDebugJavaWithJavac`). AGP 9.1.0 + Gradle 9.3.1 + debug APK on
  Moto G. minSdk 26 unchanged.

## Known-issue registry (check before touching the area)

| ID | Area | Issue | Status |
|----|------|-------|--------|
| K1 | Storage | Scoped storage: raw `File.rename` assumed blocked on shared DCIM (Android 10+) — but D2 PROVED it WORKS on Moto G 2025 in a user-picked folder (`Pictures/VE_TEST`). SAF-pick + raw rename is the validated primary; MediaStore deferred. Emulator ≠ device. | Closed 2026-09-15 — D2 evidence |
| K2 | Rename | Intra-batch + staying-put collisions must BOTH be checked (Python does both; naive ports miss one). | Pinned by BP-01 golden |
| K3 | Rename | Case gate: `img_`/`vid_` lowercase must NOT match by default. | Pinned by BP-01 golden |
| K4 | Metadata | EXIF orientation lies about W×H; viewer must correct before display. | Pinned by BP-02 fixture |
| K5 | Metadata | HEIF/DNG EXIF coverage differs Dart-`exif` vs native ExifInterface — fallback required. | Open — BP-02 owns |
| K6 | Edits | FFmpeg remux for tag-only writes destroys lossless guarantee — banned (tagger law). | Rule in RULES.md §3 |
| K7 | Tooling | PS 5.1 text pipelines corrupt source (mojibake + letter swaps). Editor tools only. | Rule in RULES.md §3 |
| K8 | Env | Hub Python 3.14 has few wheels (no TF). ML prototyping needs isolated 3.12 env in project lane. | Rule in RULES.md §1.2 |
| K9 | Tests | Real dart:io awaits in widget tests hang FakeAsync — wrap setup in `tester.runAsync` (hit 2026-09-14, Atlas law confirmed). | Closed — `app/test/widget_test.dart` |
| K10 | Dart | `List.join(',')` has no spaces; `List.toString()` does — never compare a join against a toString-shaped literal. Compare element-wise. | Closed — `storage_probe.dart` |
| K11 | Deps | file_picker 12 removed `FilePicker.platform` — use static `FilePicker.getDirectoryPath`. | Closed — `probe_screen.dart` |
| K12 | Build | `permission_handler_android` requires compileSdk 37 (template pins 36 → `compileDebugJavaWithJavac` fails). Pin 37 with justification comment; toolchain bumps stay in dedicated sessions (ADR-002). | Closed 2026-09-15 — device-proven debug build |
| K13 | Tests | Plain `test()` using MethodChannel mocks needs `TestWidgetsFlutterBinding.ensureInitialized()` first line in `main()` or every test errors with binding-not-initialized. | Closed — `metadata_adapter_test.dart` |

## Decisions log (pointer — full text in `decisions/`)

- ADR-001 ACCEPTED 2026-09-14 (engine/adapters split — scaffold conforms).
- ADR-002 ACCEPTED 2026-09-14 (dep ceiling — v1 set: rename_core path,
  path_provider, shared_preferences, permission_handler, file_picker 12.3.0).
- D1 CLOSED (`com.visionengine`). D2 CLOSED/PASS (raw rename via
  user-picked folder validated; MediaStore deferred, rename_core frozen).
  D3/D4 deferred. D5 done (pushed; explicit-path discipline).
  GQ1–GQ5 locked 2026-09-15 (SAF source, bounds thumbs, docs corrected,
  Viewer visible when read-only exists, Moto G only). Phase 2 Slices 1+2B
  CLOSED host-side; device 2B run + Viewer slice NOT authorized — STOPPED.
- ADR-003 ACCEPTED 2026-09-15 (single native extraction path, no Dart
  exif pkg, exifinterface 1.4.1 pin, §24 build leg delegated to operator
  per RULES supremacy).
