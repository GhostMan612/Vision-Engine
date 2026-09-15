# ADR-003 — Single native Android extraction path (2B)

> Status: ACCEPTED 2026-09-15 (Work Package 2B authorization). Supersedes the
> mechanism bullet of BP-02 ("`exif` pkg → native fallback") only; BP-02's
> contracts (models, provenance, read-only, gates) stand unchanged.

## Context
BP-02 proposed photo extraction via the Dart `exif` package with a native
`ExifInterface` fallback for HEIF/DNG. The 2B authorization (§5) directs
implementation via "the repository's approved Android/AndroidX mechanism",
and §19 demands reusing existing dependencies and justifying any addition.

## Decision
ONE extraction path, fully native: Kotlin `MetadataBridge`
(`ExifInterface` for photos, `MediaMetadataRetriever` for video) behind the
narrow `vision_engine/metadata` MethodChannel (`probePhoto`/`probeVideo`).
The Dart `MetadataAdapter` decodes channel payloads into `metadata_core`
models by reusing the fixture codec, so contract conformance is structural.
No Dart `exif` package is added.

## Rationale
- Coverage: `exif` (Dart) reads JPEG well but not HEIF/DNG; ExifInterface
  reads JPEG/PNG/WebP/HEIC/DNG/RAW (verified against AndroidX docs
  2026-09-15). A Dart-first path would still need the native fallback,
  doubling provenance behavior to test and risking fallback drift.
- Precision: ExifInterface reports exactly which tags exist, so `exif`
  provenance is exact rather than inferred.
- Surface: zero new hosted Dart dependencies; one narrow native dependency
  (below) instead of a dual-path design.

## Native dependency
`androidx.exifinterface:exifinterface:1.4.1` in `app/build.gradle.kts`.
Verified Jetpack release (2025-04-23, HEIC fixes included); requires
compileSdk 34+ (we pin 37); minSdk floor satisfied by our minSdk 26.
`MediaMetadataRetriever` needs no dependency (framework API).

## Consequences
- `metadata_core` stays dependency-free; all Android types remain in
  `app/android/.../MetadataBridge.kt` + `MainActivity.kt` wiring.
- Channel payload shape == golden JSON shape, so goldens double as
  channel-payload fixtures and host tests validate the decode path
  without a device.
- Kotlin correctness past careful API review rests on the human device
  run (`integration_test/extraction_device_test.dart`); RULES §1.5
  forbids host builds, so §24's `flutter build apk --debug` leg is
  delegated to the operator runbook (`docs/device-validation-2b.md`).
  Repo law wins over work-package text per RULES supremacy (§2.3 of 2B).
