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
