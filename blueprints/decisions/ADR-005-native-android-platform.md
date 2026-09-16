# ADR-005 — Native Android platform + migration shape

> Status: PROPOSED 2026-09-15 (records GQ8=NO and the authorized target;
> implementation awaits a migration kickoff authorization).
> Full analysis: `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`.

## Decision
Vision Engine is Android-only (GQ8=NO, operator directive — no iOS
requirement now or in future). Target: pure native Kotlin + Jetpack
Compose. Flutter `main` becomes the frozen reference implementation and
behavioral baseline; it is not deleted, rewritten for convenience, or
retired until §15-style criteria pass. The Viewer will be native (2D is
designed once, in Kotlin — never built in Flutter first).

## Why (compressed)
Mission is Android-storage-deep (SAF/DocumentFile/ContentResolver,
scoped storage, EXIF/retriever/bitmap, performance, deep integration);
golden fixtures make contracts portable; UI surface is still thin
(3 tabs, no Viewer), so now is the cheapest migration point. Native
wins claimed: complexity surface (3 channel methods dissolve), SAF-first
design, toolchain singularity. Explicitly NOT claimed: raw throughput
(decoding is native in both stacks) or toolchain peace (Compose/Kotlin
coupling replaces Flutter/Gradle coupling).

## Responsibility matrix (authoritative deltas; full §4 in blueprint)

| Current | Destination | Preserve |
|---|---|---|
| rename_core + parity_probe + goldens | `vision-core` rename pkg + same probe | 6 goldens byte-identical |
| metadata_core + 3 goldens | `vision-core` models | vectors + unknown/status rules |
| media_library (sort/dedupe/FNV/LRU/page) | `vision-core` library pkg | same canonical strings/keys/bounds |
| MetadataAdapter decode table/rules | `vision-android` table + parser | MIME/kind/ISO-6709/zero rules |
| MetadataBridge + channel (3 methods) | direct `vision-android` calls | 11+10 fields, release discipline |
| ThumbStore/pipeline | coroutines port + ImageDecoder/BitmapFactory | 256px, orientation, 100/32 MB, sequential default |
| Rename/Probe/Settings tabs | Compose screens | behavior |
| flutter_test/integration suites | JUnit4 + Robolectric + Compose UI + instrumented | equal-or-stronger coverage |

## Migration work packages (skeleton; full text in blueprint §11)

- MP0 Reference freeze (tag `flutter-reference-final` + golden digests).
  Exit: tag exists, Flutter green. Rollback: n/a (baseline).
- MP1 Native foundation (frozen toolchain versions, `:core` + `:app`,
  minimal test deps). Exit: empty modules compile + one passing JVM test.
  Rollback: delete scaffold dirs.
- MP2 Domain port (golden replay green). Exit: all ported vectors pass.
  Rollback: revert package.
- MP3 SAF layer (grants + DocumentsContract enumerator + fakes + device
  test). Exit: VE_2C-style device enumeration green. Rollback: revert.
- MP4 Metadata port (FD-backed Exif + retriever + rules; Robolectric +
  fixtures + device parity vs reference outputs). Exit: parity green.
- MP5 Thumbnails/cache (decoder + frame + FNV/LRU; 256x384/256x256
  device vectors). Exit: vectors green, bounds proven.
- MP6 Pipeline (coroutines port, paging, untouched-bytes proof). Exit:
  device pipeline suite green.
- MP7 Native Viewer (2D-native Compose grid/detail per G2). Exit: G2 gate.
- MP8 Parity validation (side-by-side device runs + real shots). Exit:
  G2 + golden-match sign-off.
- MP9 Flutter retirement (§15 criteria → `flutter-final` tag + atomic
  removal). Exit: criteria checklist signed. Rollback: revert commit.

## Non-decisions (explicitly deferred to kickoff)
Compose BOM/Kotlin pins (Q1), Paging 3 vs manual (manual default),
Coil fallback threshold, second OEM device, keeping Flutter building
during migration (recommended yes).
