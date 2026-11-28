# CURRENT_STATE.md — Vision Engine (verified state)

> Updated every session per RULES.md §4.2. Evidence > status.

## Implementation record (2026-11-15–17, main `9c3a2aa`)

- **S1 MP6 device re-run (2026-11-17):** 13/13 instrumented tests
  PASS on **BLU B160V, Android 14** (ADB serial
  `7040016025040287`) — `ExtractorDeviceTest` 6/6,
  `ThumbsDeviceTest` 6/6, `PipelineDeviceTest` 1/1; Gradle `BUILD
  SUCCESSFUL` in 1m 32s; instrumented XML reports independently
  checked. **Hardware substitution:** the S1 spec named the Moto G;
  the run used the BLU B160V instead. No Moto G coverage is claimed.
  **ACCEPTED 2026-11-17 for the tested BLU B160V hardware scope:**
  run head commit `18c45c6` (confirmed); an observed second run was
  `BUILD SUCCESSFUL` in 25 seconds, watched in full by the operator,
  with no folder picker appearing at any point; DCIM isolation is
  supported by source inspection (suites stage bundled synthetic
  fixtures in app-private `ve_mp4`/`ve_mp5`/`ve_mp6`; inspected test
  execution paths contain no DCIM access, user-media renaming, or
  folder-picker launch — source-code evidence of isolation, not a
  complete filesystem-access trace). No additional S1 device run is
  required for this substituted hardware acceptance.
- **S2 native rename port (PR #4, merged 2026-11-16,
  `4ef05e7`):** `vision-core` rename package — strip, `buildPlan`,
  manifest codec (6-column Dart form), dependency-ordered execution,
  undo; 38 tests incl. golden replay of all 6 Dart fixtures;
  JUnitCore OK (93) with the pre-existing suite.
- **S3 SAF picker + grants (PR #5, merged 2026-11-16,
  `2223beb`):** system folder picker (`StartActivityForResult` +
  `ACTION_OPEN_DOCUMENT_TREE`, READ+WRITE persistable grants),
  DataStore grant store (`vision_grants`), folder list UI with
  Re-pick/Forget, lookup-only launch health check. Follow-up fixes:
  coroutines-core explicit dep (`65a27a9`), `preferencesDataStore`
  import (`67275cf`), vision-core dep (`86c47c3`), stale-test
  rewrites (`eb696b9`), manifest activity (`bf0e480`). Host gate
  green on the laptop. Device: grant + discovery proven on B160V
  (Stage A); reboot + reinstall survival verified with the S4B/S4C
  runs (see PR #7); revoke-then-re-pick NOT verified — open item.
- **S4 Renamer screen (PR #6, merged 2026-11-17,
  `5ea6c2c`):** two-tab shell (Folders | Renamer), preview → execute
  → honest manifest → undo via S2 + S3; `renameDocument` mechanism
  with verify-by-re-query; app-private manifest
  (`renames_<epochMillis>.csv`) + `CreateDocument` export; host gate
  BUILD SUCCESSFUL in 1m 40s (four tasks green).
- **PR #7 S4-FIX-01 (merged 2026-11-17, `b0d2b36`):** destination
  preflight via parent enumeration (3 outcomes) + post-rename URI
  preserved for undo; 6 Robolectric backend tests; host gate green.
  Hardware acceptance on B160V at head `4c444db` (recorded as a
  comment on PR #7): S4B synthetic rename/Undo passed (zero-byte
  synthetic files, all four restored with identical SHA-256), S4C
  real-media rename/Undo passed (a real nonempty JPEG and a
  screen-recording MP4 copied into `Pictures/VE_TEST_S4C`, 41.5 MB
  MP4 byte-identical), SAF grants survived reboot + same-signature
  reinstall, app-private manifests survived reinstall, post-rename
  SAF document URI verified, no live DCIM touched. No copied DCIM
  directory was tested; that requirement remains unverified.
- **PR #8 Python safety residuals (merged 2026-11-17,
  `18c45c6`):** exclusive manifest creation (mode `"x"` +
  pre-check), manifest alias guard (exit 2), dependency-aware undo
  (pending-map + vacated-set), 12 adapted safety tests, parity
  probe Dart-path fix + chain-execution assertion; pytest 30 passed
  (18 + 12), flake8 clean, CI pass.
- **S5 MP7 Viewer (draft PR #10, branch
  `feat/mp7-native-viewer`, base main `9c3a2aa`, 2026-11-17,
  NOT merged):** the SAF URI gap is closed — `MediaPipeline`
  now probes + thumbs URI-only sources via the existing
  `probePhotoUri` / `probeVideoUri` / `grabFrameDescriptor`
  paths (descriptors closed in `finally` on all paths; 64 MB
  photo cap, 512 video maxSide, 100 / 32 MB cache); new
  `ViewerPipeline` interface, `DetailFields.kt` (11 photo / 10
  video fields, corrected dims, `—` for unknown, provenance
  chip per row, raw-tag badge), `ViewerViewModel` (page size 50,
  generation-guarded cancellation, grant-lost keeps records,
  one retained pipeline across pages, progressive per-record
  publishing, retained Bitmaps capped at 12 with restore on
  demand, tab-hide cancellation), `ViewerScreen`
  (third tab, grid → detail, Strip GPS button disabled as
  Phase 3), instrumented `UriPipelineDeviceTest` (4 tests) +
  test provider (fixtures staged via the instrumentation
  context's private dir). 41 new tests (11 core JVM + 10 URI
  pipeline + 14 ViewModel + 6 screen, incl. 1 renamed
  PipelineTest case); vision-core JUnitCore OK (104) on this
  VM; pytest 30 passed. Independent audit requested changes
  (P0 missing `ensureActive` import — the first standalone
  check had missed it — plus P1 progressive publishing /
  shared pipeline / bitmap bound / tab-hide cancellation and
  P2 raw-tag badge); all addressed in the amended head, host
  gate + device evidence still PENDING — see
  `blueprints/standalone-v1/S5-DEVICE-RUNBOOK.md`. G2 stays OPEN.
- **Remaining limitations (2026-11-17):** no Moto G coverage for
  the S1 run; S3 revoke/re-pick unverified; S4 stage (b)
  copied-DCIM requirement unverified; S4 stage (c) live DCIM
  never run (needs Glenn's separate go-ahead); symlinks
  undetectable through SAF (`skippedSymlink` stays 0 on that path);
  no native Gradle CI — only `python-app.yml`; S1 terminal
  transcripts, ADB device listing, and XML audit were supplied in
  the operator conversation but have not yet been archived in
  GitHub. S5: host gate + instrumented Viewer tests + real-shot
  read-only smoke all pending; `tools/verify_reference.py`
  fails on clean main `9c3a2aa` (28 files, same list as on the
  S5 branch — pre-existing, caused by PRs #1–#9 moving pinned
  files, not by the S5 branch); the video URI retriever
  defaults over a `FileDescriptor` source are host-gate-
  verified, not proven on this VM.
- **Next:** S5 host gate + device evidence (PR #10, draft), then
  S6 MP8/MP9 after the S5 merge, on Glenn's nod. LICENSE is a
  separate owner decision (no LICENSE on main).

## Historical snapshot (2026-09-14 scaffold; subsequent dated entries below)

- **Standalone v1 spec (2026-11-15):** Glenn decided Vision
  standalone first (finish the renamer + viewer before lending the
  engine to Tagger/IndiOSINT). Spec lives in
  `blueprints/standalone-v1/` — S1 MP6 device re-run → S2 native
  rename port → S3 SAF picker/grants → S4 Renamer screen → S5 MP7
  Viewer → S6 MP8 parity + MP9 Flutter retirement, plus
  RECOMMENDATIONS (REC-01–REC-11, risks R-01–R-11) and a master
  CHECKLIST. PR #1 (`5f78388`, Python fixes, 18 tests) and PR #2
  (`d103ef6`, CI fixes) are merged on main. The MP6 device run is
  still the outstanding Glenn-side item (S1); no native rename,
  viewer, or folder picker exists yet.
  **Superseded 2026-11-17:** the MP6 device run is done (13/13 on
  BLU B160V, see Implementation record above); native rename (PR
  #4), folder picker + grants (PR #5), and the Renamer screen (PR
  #6) are all merged on main. The sentence above is kept for the
  historical record only.

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
  HISTORICAL ONLY: at that moment no native implementation had started;
  subsequent MP0–MP6 implementation below supersedes this line.
- MP0 REFERENCE FREEZE 2026-09-15: tag `flutter-final` (see handoff);
  `FLUTTER_REFERENCE_BASELINE.md` + manifest (137 files hashed) +
  `tools/verify_reference.py` (self-check green). Reference code frozen.
- MP1 NATIVE FOUNDATION 2026-09-15: `native/` (vision-core + android +
  app) builds green on frozen toolchain (AGP 9.1.0, Gradle 9.3.1, JBR 25):
  core 2/2, android lib compiles, app APK assembles (8.79 MB), app unit
  2/2 incl. Robolectric Compose launch. Deps: Compose BOM 2024.09.00,
  activity-compose 1.10.1, lifecycle 2.8.7, junit 4.13.2, Robolectric
  4.17. Manifest: zero permissions. NO Viewer, NO MP2.
- MP2 DOMAIN PORT 2026-09-15: vision-core holds metadata/media domain
  (48/48 JVM tests incl. golden replay of all 9 Flutter fixtures +
  Dart-byte-identical FNV vectors); Long used for int fields (Dart int
  is 64-bit — 4 GB+ file sizes survive); hand-rolled test-only JSON
  parser (zero new deps); boundary test intact. NO MP3, NO Viewer.
- MP3 SAF DISCOVERY 2026-09-15: `SafDiscovery` (DocumentsContract-direct
  single query, top-level only) + core `MimeTypes` (Turkish-proof) +
  17/17 Robolectric fake-provider tests; junit+robolectric test-scoped
  in android module (same pinned versions). NO MP4, NO Viewer.
- MP6 PIPELINE 2026-09-15: `MediaPipeline` (sequential loadPage +
  thumbnailFor over MP4/MP5 layers, URI-less degradation, guarded reads)
  + 7/7 JVM tests; instrumented `PipelineDeviceTest` authored+compiled,
  DEVICE RUN BLOCKED (Moto G not visible to adb at session time — rerun
  per `docs/device-validation-mp6.md`). NO Viewer, NO MP7.
  **Superseded 2026-11-17:** the device run is done — 13/13 PASS on
  BLU B160V, Android 14 (Extractor 6/6, Thumbs 6/6, Pipeline 1/1),
  Gradle BUILD SUCCESSFUL in 1m 32s, XML reports checked. Hardware
  substitution for the Moto G named in the S1 spec; no Moto G
  coverage claimed. **ACCEPTED 2026-11-17** for the tested BLU
  B160V hardware scope (run head commit `18c45c6`; observed
  second run BUILD SUCCESSFUL in 25s, watched in full by the
  operator, no folder picker appeared). NO Viewer, NO MP7 (S5
  next, on Glenn's nod).
- MP4 EXTRACTION 2026-09-15: `ExifReader`/`VideoReader` mapping +
  `AndroidMetadataExtractor` (file/URI, guarded lifecycle) + 15/15 JVM
  tests + **device suite 6/6 PASS on Moto G (agent-run over adb)**;
  exifinterface dep declared in native module (same 1.4.1 pin);
  test-only androidx.test artifacts (cached versions). NO MP5, NO Viewer.
- MP5 THUMBNAILS 2026-09-15: `PhotoThumbs` (sampled decode + explicit
  Matrix rotation + exact width-256 + JPEG-85) + `VideoFrames`
  (first-frame ≤512px, guarded lifecycle) + cache wiring; 12/12 JVM
  tests; instrumented `ThumbsDeviceTest` 6/6 PASS on Moto G (agent-run);
  missing-asset setup failure diagnosed + fixed same session. NO Viewer,
  NO pipeline.
- 2C DEVICE CLOSED 2026-09-15: Flutter `pipeline_device_test` 6/6 PASS
  on Moto G (agent-run; folded-sort expectation corrected to verified
  truth). Full Flutter device story now green alongside native suites.
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
| K20 | Tooling | Never open the same file for read+write in one Python expression (`open(p,'w').write(open(p).read())` truncates before reading — silently empties the file). Read fully, close, then write; verify byte counts after every scripted touch. | Closed — caught by post-touch grep, files restored byte-exact |
| K21 | Toolchain | `ffmpeg_kit_flutter 6.0.3` pins an artifact not served by repo.maven.apache.org and uses removed v1 embedding; `ffmpeg_kit_flutter_new 5.0.0` + Google's Maven Central mirror fixes both. `tflite_flutter` needs Java/Kotlin target alignment to 17; `options.release` is rejected by AGP. | Closed 2026-11-27 — ADR-011 |

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
- ADR-005 recorded as PROPOSED on 2026-09-15; MP0–MP6 subsequently
  progressed on the native track. Formal acceptance signature not evidenced.
  See decision record for the documented implementation-vs-approval gap.

### Phase 4 (2026-11-24) — ADR-006 + G4

- BP-04 promoted per ADR-006: 4B (honest resize) — already shipped in Phase 3
  (`app/lib/features/manip/pixel_ops.dart`, tests green). No new code.
- 4A: cloud consent path (ADR-007) + on-device engine (ADR-010, 244 KB bundled
  segmenter, `MaskFn` seam, local flood-fill fallback when LiteRT allocation
  fails on B160V, `_edit.png` copy, source-byte proof, engine toggle).
- 4C: v1 GIF (ADR-008) + v2 MP4 via `ffmpeg_kit_flutter_new 5.0.0` (ADR-009/011)
  with optional user-supplied music.
- Gates: `dart analyze` (rename_core) No issues found!; `flutter analyze` No issues
  found!; `dart test` 29/29; `flutter test` 64/64 (3 erase + 3 slideshow + 2 DCIM);
  `flutter test integration_test -d 7040016025040287` 9/9 PASS (B160V); native
  Robolectric green. Evidence collected before the end-of-session cache purge.

### Phase 5 (2026-11-24) — DCIM round-trip (bounded, byte-preservation OK)

- Synthetic `IMG_20261124_a/b.jpg` created on B160V (`/sdcard/DCIM/VE_RT5`, device cleaned).
- Hub Python rename executed on pulled copy: `RENAMED 2/2, errors=0, collisions=0`.
- SHA-256 of both files identical before/after (pure-rename byte-proof OK).
- App-on-device UI leg added 2026-11-27 (`DcimScreen` + `DcimRenameService`);
  the 2026-11-24 leg itself drove only the host Python rename.

- Rename templates v1: `buildTemplates` + `templateName` in
  `packages/rename_core/lib/src/templates.dart`; `buildPlan` gained the
  additive optional `nameMapper` param (defaulted, behavior unchanged —
  verified by the full existing suite, 29/29 incl. golden + parity probe
  GREEN). Tests: `packages/rename_core/test/templates_test.dart`.
- Native EXIF ops: `ExifOps.kt` in `native/vision-android/src/main/kotlin/
  com/visionengine/android/meta/` (shiftDateTime, stripGps, setOrientation;
  all write→read-back verified) + `ExifOpsTest.kt` (Robolectric, green).
- Flutter host: `app/lib/adapters/manip_adapter.dart` +
  `app/lib/features/manip/{manip_models,pixel_ops,manip_screen}.dart` +
  `vision_engine/exif_ops` MethodChannel in `app/android/app/src/main/
  kotlin/com/visionengine/MainActivity.kt` + new Manip screen tab in
  `app/lib/main.dart`. Pixel ops via new `image` dep (4.13.1 in pubspec).
- Gates this session: `dart test` rename_core 29/29; `flutter analyze`
  "No issues found!"; `flutter test` 60/60 (incl. 3 erase + 3 slideshow tests); native
  `:vision-android:testDebugUnitTest` BUILD SUCCESSFUL; `tools/parity_probe.py`
  GREEN. `tools/verify_reference.py`: pre-existing BROKEN (22 files) from
  S5/S6 evolution; the new Phase 3 files join the frozen-manifest mismatch.
