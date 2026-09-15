# BP-02 — Metadata viewer (read-only first)

> Phase 2. Viewer ships BEFORE any edit op. Honesty over completeness.

## Objective
Read-only grid → detail viewer for device-camera photos/videos with provenance per field.

## Scope
- `packages/metadata_core`: `PhotoMeta` (datetimeOriginal, gps, orientation, make/model, exposure, width/height raw+corrected, size, mime), `VideoMeta` (durationMs, width/height, rotation, creationTime, location, codec), `Provenance {exif, container, filesystem, unknown}`, JSON fixture codec.
- Adapter `app/lib/adapters/metadata_adapter.dart`: photo via `exif` pkg → native `ExifInterface` fallback (HEIF/DNG); video via `MediaMetadataRetriever` platform channel (tagger `id3`/`storage`-channel pattern: small Kotlin bridge, Dart wrapper).
- Viewer UI: thumbnail grid (MediaStore thumbs, cache-first) → detail sheet; orientation-corrected dims with raw-tag badge; missing = `—`; provenance chip per row; GPS row → "Strip…" button routing to BP-03 (never edits inline).
- Fixtures: synthetic EXIF dump JSON + fake video meta JSON (no real media committed).

## Non-scope
No edits, no share, no slideshow. No `ffmpeg_kit` (not needed for viewing).

## Verification (G2)
Fixture renders match expected JSON; no invented values; analyzer zero + host tests green; Moto G smoke on operator shots (pasted evidence, files never committed).

## Reference lineage
Tagger Forge read-path + `probeStreams` verify pattern; Atlas provenance-travels + unknown>invented; Recovery offline-first (viewer works airplane-mode).
