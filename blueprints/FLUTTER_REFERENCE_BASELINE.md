# FLUTTER_REFERENCE_BASELINE.md — Vision Engine frozen Flutter reference

> MP0 reference freeze. This document + `FLUTTER_REFERENCE_MANIFEST.json`
> (same directory) + tag `flutter-final` jointly define the behavioral
> baseline the Kotlin port is verified against. Freeze policy (§13):
> reference code does not change without an explicit authorization that
> names the discrepancy. Research date: 2026-09-15.

## 1. Reference commit
`main` at MP0 commit (see §13 log pointer; tag `flutter-final` points at
it). Pre-freeze HEAD was `58ef4d5`; MP0 adds only this document, the
manifest, the manifest checker, and tracking ticks — zero behavioral
change (proven by the MP0 diff audit: docs + manifest only).

## 2. Reference tag
Annotated tag `flutter-final` → the MP0 commit. Verify with
`git rev-parse flutter-final^{commit}` and
`git tag -l --format='%(objectname:short) %(contents:subject)'`.

## 3. Toolchain (read from repo/toolchain, not memory)
- Lane: `C:\android\flutter` — Flutter 3.47.0 stable (framework rev
  `4cf2416426`, engine rev `06a2e2a110` 2026-09-03), Dart 3.13.3,
  DevTools 2.60.0 (`flutter --version`, `flutter doctor -v`).
- Java: OpenJDK 25.0.3 (Android Studio JBR,
  `C:\android\Android Studio\jbr\bin\java`) — doctor-verified.
- AGP 9.1.0 + Kotlin Gradle Plugin 2.4.0
  (`app/android/settings.gradle.kts:22-23`); Gradle wrapper 9.3.1
  (`gradle-wrapper.properties`); JVM target 17, source/target 17
  (`app/build.gradle.kts`).
- compileSdk 37 (pinned, permission_handler_android requires it),
  minSdk 26, targetSdk = Flutter-pinned 36, 64-bit ABIs only
  (`app/build.gradle.kts`).
- exifinterface 1.4.1 (only native dep, ADR-003).

## 4. Dependency state
- App hosted deps (from `app/pubspec.yaml`, locked in `app/pubspec.lock`
  — the reproducible record): cupertino_icons, path_provider,
  shared_preferences, permission_handler 13, file_picker 12;
  dev: flutter_test, integration_test, flutter_lints 6. Path deps:
  rename_core, metadata_core, media_library (in-repo, no registry).
- Pure packages: `test` + `lints` dev only. Internal package locks are
  intentionally untracked (`.gitignore`: `packages/*/pubspec.lock`);
  the app lock IS tracked and hashed in the manifest.
- No network/telemetry/analytics/crash SDKs anywhere in the graph.

## 5. Architecture (frozen shape)
`packages/{rename_core,metadata_core,media_library}` (pure Dart, no
Flutter/Android imports) → `app/lib/adapters` (rename, metadata+channel
decode, storage probe) + `app/lib/library` (SAF-shape lister, ThumbStore,
pipeline) → 3 tabs (Rename/Probe/Settings; no Viewer). Kotlin:
`MetadataBridge` (Exif/retriever/frame) + `MainActivity` channel wiring
for `vision_engine/metadata` (probePhoto/probeVideo/getVideoFrame).
SAF-picked folder + raw rename is the validated primary (D2); MediaStore
deferred; read-only; offline-first.

## 6. Behavioral contracts (inventory — specs live in the cited docs)
- Rename: leading-only strip, case-sensitive default, empty-result skip,
  3 collision classes, deterministic sort, dry-run default, manifest +
  reverse-order undo, byte preservation (`VISION_ENGINE_MASTER_
  BLUEPRINT.md` §3; `rename_camera_prefixes.py` is executable truth).
- Metadata: opaque-String exposure, orientation 5–8 / rotation 90/270
  correction in core, EXIF-absent→`"0"`→unknown (orientation),
  dims ≤0→unknown, duration/rotation 0 legitimate, exposure decimal
  canonical form, ISO-6709 parse, GPS local-only (BP-02, K4/K5/K10/K15/
  K16, `extraction-boundary.md`).
- Status: `ok` counts EXIF/container fields only; `partial` = file read,
  zero substantive fields; `unsupported` (no channel call);
  `unreadable` (UNREADABLE/PLATFORM_FAIL coded warnings); programming
  failures stay loud (boundary doc error table).
- Discovery/pipeline: top-level only, MIME table, unsupported-as-record,
  folded ordering, id dedupe first-wins, page slicing, sequential probing,
  sort positions (BP-05, `PHASE_2_RESEARCH_MAP.md`, ADR-004).
- Thumbnails: 256px target (orientation-applied; small sources upscale),
  ≤512px JPEG-85 video frames, 64 MB source cap, FNV(id,size,mtime,dim)
  keys, LRU 100/32 MB, in-flight dedupe, cache failure never destroys
  records (`docs/extraction-boundary.md` §2C, K17).
- Privacy: no INTERNET permission, no MANAGE_EXTERNAL_STORAGE, no
  MediaStore enumeration, no writes/renames/deletes, synthetic fixtures
  only (manifest + `RULES.md` §1.3).

## 7. Test baseline (executed 2026-09-15 for this freeze)
- `dart analyze` clean + `dart test`: rename_core 22/22, metadata_core
  20/20, media_library 17/17.
- `flutter analyze --no-pub` clean + `flutter test`: app 44/44
  (21 adapter incl. fake-channel suites, 3 lister, 10 thumb incl. real
  Skia decode 256x384, 5 pipeline incl. byte-identical sources, 2 probe,
  2 rename-flow, 1 widget).
- Host total: 103/103 green. Suites re-run green at every slice; counts
  recorded per commit message per repo law.

## 8. Device baseline (repo evidence only — nothing claimed beyond it)
- 2B extraction suite: 5/5 PASS on Moto G 2025 (human-pasted
  `All tests passed!`, `SESSION_HANDOFF.md`; runbook
  `docs/device-validation-2b.md`). D2 SAF raw-rename PASS with same-device
  evidence (2/2 rename, 2/2 undo, cleaned).
- 2C pipeline device suite (`pipeline_device_test`, 2C runbook
  `docs/device-validation-2c.md`): human rerun OUTSTANDING at freeze
  time — explicitly NOT claimed. First human run surfaced the
  `ThumbFetch` import gap (fixed, K19); rerun pending.
- Known warnings at freeze: SDK XML v4 skew (benign, K12 context);
  Mali/GRALLOC log noise on launch (benign, recorded in tagger lineage).

## 9. Fixture inventory
- Generator: `tools/make_fixtures.py` (pure stdlib, self-verifying):
  `ve_exif.jpg` 324B (hand EXIF: orientation 6, 1/120, GPS, SOF0
  3000x4000), `ve_plain.png` 454B (16x16, no EXIF),
  `ve_minimal.mp4` 150B (ftyp+mvhd 1000/8340, no tracks),
  `ve_thumb.jpg` 194B (grayscale + SOS scan, SOF 24x16, orientation 6).
- Golden JSON: rename_core 6 files, metadata_core 3 files (replayed by
  the Kotlin port per migration blueprint §10).
- Python truth scripts: `rename_camera_prefixes.py` (387 L),
  `rename_min.py` (43 L); `tools/parity_probe.py` (6/6 + byte proof).

## 10. Security/privacy baseline
Permissions: READ_MEDIA_IMAGES/VIDEO + READ_EXTERNAL_STORAGE capped at
32; nothing else. Static posture verifiable by grep (no `http` under
`app/lib/adapters/`, no INTERNET in manifest, no analytics deps in
locks). Dynamic posture: airplane-mode-capable (offline-first),
untouched-bytes proofs in probe + pipeline tests, self-cleaning probes.

## 11. Known limitations (verified, carried into migration)
HEIC-on-old-OS degrades to unknown; trak-less video yields duration-0 +
unavailable thumbs; Android 14 partial-access out of scope; `flutter
test` never compiles `integration_test/` (K19 — analyzer is the gate);
dual-SDK hook staleness if two Flutter installs share the dir (K14 —
single lane at freeze); `await x!` needs parens (K18).

## 12. Known unresolved items (NOT resolved by this freeze)
- 2C device rerun outstanding (import fix pushed, awaiting human run).
- G2 open (needs native-or-Flutter Viewer + real-shot smoke per the
  platform verdict — native per ADR-005).
- MP0–MP9 migration sequence authorized as blueprint only; MP1 NOT
  started.

## 13. Rules governing future reference changes + log pointer
- The reference is FROZEN. Native work must not modify these files
  except by explicit authorization naming the discrepancy; a proven
  reference bug is documented as a discrepancy and handled through the
  migration decision process — never silently patched.
- MP0 commit log: this document + `FLUTTER_REFERENCE_MANIFEST.json` +
  `tools/verify_reference.py` + tracking ticks. Diff audit at commit
  time: docs/manifest/tooling only, zero behavioral deltas (behavioral
  tree identical to `58ef4d5` — the audit is part of the commit record).
- Re-verify any checkout any time: `python3 tools/verify_reference.py`
  (hub: `C:\venv-hub\venv\Scripts\python.exe`), exit 0 = intact.
