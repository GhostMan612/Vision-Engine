# native/ — Vision Engine pure-native Android track (MP1 foundation)

> Flutter `app/` is the frozen reference (`flutter-final` tag). This tree
> is the migration target. See `blueprints/NATIVE_ANDROID_MIGRATION_
> BLUEPRINT.md` and `blueprints/decisions/ADR-005-native-android-
> platform.md`. MP1 scope: foundation only — no SAF listing, no metadata
> extraction, no thumbnails, no Viewer.

## Modules (dependency direction strictly app → android → core)

- `vision-core` — pure JVM domain contracts. NO `android.*`,
  `androidx.*`, Compose, or Flutter imports (enforced by
  `ModuleBoundaryTest`, which fails the build on violation).
  MP2 content: Provenance, MetaField, PhotoMeta/VideoMeta (corrected
  dims), ExtractStatus, MediaKind, MediaSource, ThumbInfo, MediaRecord,
  ordering/dedupe/paging, FNV keys (byte-compatible with Dart),
  LRU cache policy, status rules, Map codec — all golden-replayed
  against the Flutter fixtures.
- `vision-android` — Android platform layer home (empty in MP1 by
  design; SAF/Exif/retriever land here in later packages).
- `vision-app` — Compose application: `MainActivity` + `AppRoot` +
  `MainViewModel` (name + readiness state only; no media UI).

## Toolchain (frozen — see MP1 authorization §2, no upgrades)

AGP 9.1.0 · KGP 2.4.0 (android + jvm + compose plugin, same version) ·
Gradle 9.3.1 · JVM target 17 · compileSdk 37 · minSdk 26 · targetSdk 36.
Compose libraries via BOM 2024.09.00; activity-compose 1.10.1;
lifecycle-viewmodel-compose + lifecycle-runtime-compose 2.8.7; JUnit
4.13.2; Robolectric 4.17 (tests only). Every version is literal in the
module build files — no catalogs, no dynamic versions.

## Commands (run in `native/`; JDK comes from Android Studio JBR)

```powershell
.\gradlew.bat :vision-core:test
.\gradlew.bat :vision-android:testDebugUnitTest
.\gradlew.bat :vision-app:assembleDebug
.\gradlew.bat :vision-app:testDebugUnitTest
```

CLI validation builds on a bare shell need the SDK locations this
machine does not export globally (its system JAVA_HOME points at a
nonexistent Studio path). Set them process-locally per command —
never system-wide, never in tracked files:

```powershell
$env:JAVA_HOME = "C:\android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\android\sdk"
```

(Android Studio itself needs neither; it resolves both on its own.)

## MP1 deviations from the migration blueprint (evidence-pinned)

- AGP 9.1 ships Kotlin support built in: the explicit
  `org.jetbrains.kotlin.android` plugin declaration fails the build, so
  modules declare only their AGP plugin (+ compose compiler plugin) and
  configure `kotlin { compilerOptions { jvmTarget JVM_17 } }`. Same KGP
  2.4.0 underneath — no version changed.
- Robolectric 4.17 on JDK 25 needs test-JVM flags
  (`--enable-native-access` + `--add-opens` for `java.lang` and
  `jdk.internal.access`); without them the Compose test dies in
  `AndroidInterceptors` file-descriptor interception. Flags are
  test-scoped only.

## Invariants (audited per work package)

1. `vision-core` never imports Android/Compose/Flutter (test-enforced).
2. Dependencies point inward only (app → android → core).
3. Manifest requests zero permissions (grep-gated).
4. No network/telemetry/analytics dependencies (lockfile audit).
5. No MediaStore, database, service, or Viewer until their authorized
   work package.

## MP4 extraction (`vision-android/meta`, read-only, local-only)
- `ExifReader`: ExifInterface → `PhotoMeta` (11 fields, verbatim
  strings, GPS decimals, orientation/dimension normalization, size/mime
  injected with filesystem provenance). `VideoReader`: retriever string
  map → `VideoMeta` (ms duration, ISO-6709 split, verbatim date/codec).
  Status via core rules; per-field warnings; missing → unknown.
- `AndroidMetadataExtractor`: file-path + content-URI variants (FD
  opened late, closed after extraction; retriever released in `finally`
  on all paths). No writes, no network, no geocoding.
- Proven on JVM (real fixture files + scripted retriever shadows) and
  on device (`ExtractorDeviceTest`, human-run per
   `docs/device-validation-mp4.md`).

## SAF discovery (MP3 — `vision-android`)

- Entry: `SafDiscovery(resolver-usage)` — production wires
  `SafDiscovery.withResolver(contentResolver)`; tests inject scripted
  query lambdas. The seam exposes exactly one platform call (single
  child query), so read-only behavior is structural, not promised.
- One `DocumentsContract` child query per discovery with the narrow
  projection (ID, display name, MIME, size, modified, flags);
  DocumentsContract-direct, never DocumentFile tree traversal.
- Top-level children only; directories skipped + counted; virtual
  documents become `unsupported` records with warnings; null display
  names fall back to document IDs; per-row failures skip + count, never
  kill discovery; cursors closed via `use {}`.
- Identity = document URI string (authority-scoped, stable per
  DocumentsContract); ordering/dedupe reuse `vision-core`
  (`planDiscovery`); MIME classification lives in core (`MimeTypes`,
  locale-proofed) with provider-MIME-first, extension fallback.
- Robolectric suite scripts a fake provider (MatrixCursor rows, nulls,
  throws, missing columns, two authorities): 17 tests pin enumeration,
  classification, ordering, dedupe, errors, and single-query/read-only
  behavior. No device, no real filesystem, no network.

## MP5 thumbnails (`vision-android/thumbs`, read-only, local-only)

- `PhotoThumbs.decodeThumbnail(bytes, orientation)`: bounds decode →
  power-of-2 sampling (longest side ≤1024) → explicit Matrix rotation
  (3/6/8 + flips/transposes; BitmapFactory ignores EXIF, so this is the
  sole orientation authority) → exact width-256 scale → JPEG-85. Sources
  over 64 MB skip; any decode failure returns null, never throws.
  Verified: orientation-6 yields 256x384; small sources upscale to
  target-256 (decoder behavior, pinned).
- `VideoFrames.grabFrame(path)`: first-frame `getFrameAtTime` + scale to
  ≤512px side + JPEG-85; retriever released in `finally`; trackless or
  missing files yield null. No ffmpeg, no remux, no writes.
- Cache policy/keys live in `vision-core` (FNV byte-compatible, LRU
  100/32 MB); MP5 wires decode→put→get (proven by round-trip tests).
- Proven on JVM (Robolectric real-decodes fixtures; scripted retriever
  frames) and on device (`ThumbsDeviceTest`, agent-run per
  `docs/device-validation-mp5.md`).

## MP6 pipeline (`vision-android/pipeline`, read-only, local-only)

- `MediaPipeline(resolver)`: `loadPage` slices the caller-supplied
  ordered sources and probes each SEQUENTIALLY (deterministic; no
  coroutines yet — concurrency only on measured need) into
  `MediaRecord`s via the MP4 extractor; `thumbnailFor` fetches
  photo/video thumbs through the bounded `ThumbnailCache`, never
  touching unsupported records' probe path.
- URI-less (file-backed) and URI-shaped (documentUri-only, no local
  path) sources degrade deterministically to `unreadable` records +
  `unavailable` thumbs — no probe attempted, no crash.
- File reads are guarded (`unavailable` on vanished files); decode and
  frame calls are guarded (`failed`/`unavailable`); cache failure never
  destroys records. Untouched-bytes proof is a test concern (hash
  snapshots), not a pipeline promise.
- Proven on JVM (7 pipeline tests: records, paging, thumbs incl. cache
  hits, missing/URI-less sources, untouched snapshots) and on device
  (`PipelineDeviceTest`, runbook `docs/device-validation-mp6.md`).
