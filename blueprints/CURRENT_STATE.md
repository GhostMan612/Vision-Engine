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
  `MetadataAdapter`, 18/18 host tests; exifinterface 1.4.1; NO Viewer/UI).
  **2B FORMALLY CLOSED 2026-09-15:** Moto G device suite 5/5 PASS
  (human-pasted `All tests passed!`), single-lane build clean — Invalid
  SDK hash episode resolved. G2 still open (needs Viewer + real-shot
  smoke).
- 2C IN PROGRESS 2026-09-15 (authorized): `packages/media_library`
  (models/ordering/cache, 17/17) + lister + `ThumbStore` + pipeline +
  device suite authored; `getVideoFrame` bridge; ADR-004. NO Viewer UI.
- PLATFORM DECISION 2026-09-15: GQ8=NO (Android-only forever) →
  native Kotlin + Compose is the target; Flutter `main` is the frozen
  reference. Migration blueprint
  (`blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`) + ADR-005 written;
  NO implementation started; 2D stays locked until kickoff.
- MP0 REFERENCE FREEZE 2026-09-15: tag `flutter-final` (see handoff);
  `FLUTTER_REFERENCE_BASELINE.md` + manifest (137 files hashed) +
  `tools/verify_reference.py` (self-check green). Reference code frozen.
- MP1 NATIVE FOUNDATION 2026-09-15: `native/` (vision-core + android +
  app) builds green on frozen toolchain (AGP 9.1.0, Gradle 9.3.1, JBR 25):
  core 2/2, android lib compiles, app APK assembles (8.79 MB), app unit
  2/2 incl. Robolectric Compose launch. Deps: Compose BOM 2024.09.00,
  activity-compose 1.10.1, lifecycle 2.8.7, junit 4.13.2, Robolectric
  4.17. Manifest: zero permissions. NO Viewer, NO MP2.
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
| K14 | Toolchain | TWO Flutter installs shared this project dir + pub cache (`C:\src\flutter` Dart 3.13.0 vs `C:\android\flutter` Dart 3.13.3): stale `hook.dill` reuse failed with "Invalid SDK hash". Fixed environmentally (`flutter clean` + rebuild under one SDK). NOTE 2026-09-15: `C:\src\flutter` no longer present in this environment — single-SDK (`C:\android\flutter`, Dart 3.13.3) going forward; all suites re-verified green under it. | Closed |
| K15 | Device truth | ExifInterface surfaces absent dimensions/orientation as `"0"` (not null): SOF-less JPEGs yield dims `"0"` (real camera files always carry SOF — fixture was at fault, fixed with SOF0); orientation `0` means absent (no camera writes it) → adapter maps to unknown, rotation/duration `0` stay known (legitimate). Retriever ignores mvhd duration without tracks (trackless file reports `0`, not 8340). Status `ok` counts EXIF/container fields only, else `partial` is unreachable. | Closed — 2B triage fix, host green, device rerun pending |
| K16 | Platform form | ExifInterface `getAttribute` renders RATIONAL tags as decimal strings (`1/120` → `0.008333333333333333`, IEEE-deterministic). Core exposure contract is opaque-String-verbatim, so the canonical form is the decimal: device assertions pin it exactly, no rational reconstruction, no approximation. | Closed — exposure fix, host green, device rerun pending |
| K17 | Futures | `Future.whenComplete` awaits a returned Future: a cleanup closure returning `map.remove(key)` (the future itself) self-deadlocks forever with zero diagnostics. Use block bodies for cleanup actions. Proven by m15/m18 isolation after a long zone/binding misdiagnosis. | Closed — 2C `ThumbStore._dedupe` fix |
| K18 | Tests | `await x!` on `tester.runAsync` yields contradictory analyzer diagnostics; always parenthesize `(await tester.runAsync(...))!` (proven by experiment). Related: flutter_test `test()` runs real-async fine — `testWidgets`+`runAsync` is ONLY for widget-pumping tests; pure-logic tests with real IO/engine futures must stay plain `test()`, never convert preemptively. | Closed — 2C host suites |
| K19 | Gates | `flutter test` compiles `test/` only — `integration_test/` breakage is invisible to it. The analyzer DOES cover `integration_test/`, but only if it runs AFTER those files exist. Always re-run `flutter analyze` as the last gate after adding/changing integration files. | Closed — missed `ThumbFetch` import caught on device lane |

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
- ADR-005 PROPOSED 2026-09-15 (GQ8=NO → native Kotlin + Compose target,
  Flutter frozen reference, MP0–MP9 sequence; awaits kickoff).
