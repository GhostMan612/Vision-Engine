# ADR-004 — Phase 2C pipeline shape (2C)

> Status: ACCEPTED 2026-09-15 (Work Package 2C authorization). Supplements
> ADR-001 (engine/adapters split) and ADR-003 (single native extraction
> path). No toolchain, permission, or manifest changes.

## Context
2C needs SAF-source discovery, deterministic records, and thumbnails
without contaminating `metadata_core`, duplicating extraction, or
pre-building a Viewer, database, or service.

## Decisions

1. NEW pure package `packages/media_library` (depends on `metadata_core`
   path only): `MediaSource` (id + filePath/documentUri identity, exactly
   one required), `MediaKind` (photo/video/unsupported), `MediaRecord`
   (composes `PhotoMeta`/`VideoMeta`, never duplicates), `ThumbInfo`
   states, deterministic ordering (folded-name, name, id), FNV cache keys
   over source identity + size + mtime + dimension, bounded LRU cache,
   page slicing. App gains a path dep on it (narrow, structural).
2. `ExtractStatus` moves into `metadata_core` (`src/status.dart`) so
   records share 2B's exact status semantics instead of duplicating the
   enum. The adapter re-exports it: zero behavior change, zero test churn
   (proven: all suites green before building on it).
3. Thumbnails split by capability, not symmetry: photos decode in Dart
   via Flutter's codec (`targetWidth`, EXIF applied by the platform
   decoder — verified 256x384 on portrait-flagged fixture); video frames
   come from `MediaMetadataRetriever.getFrameAtTime` via a narrow
   `getVideoFrame` channel addition (first frame, longest side capped at
   512, JPEG-85). No `ffmpeg`, no new hosted deps, no `image` package.
4. Orchestration (`LibraryPipeline`: discover → page → probe → records;
   thumbnails on demand) lives in the app over injected fakes-friendly
   seams. Pages probe SEQUENTIALLY (deterministic; concurrency deferred
   until measured need — explicit YAGNI call). No database, no service,
   no background work, no MediaStore catalog.
5. Viewer UI is explicitly NOT part of this decision; records serialize
   to JSON so the future Viewer consumes data, never platform objects.

## Consequences
- `metadata_core` gains one enum (additive, semantics untouched).
- App test files disambiguate the two `MediaKind`s (`hide` + prefixes).
- Cache defaults (100 entries / 32 MB / 256px / 64 MB source cap) are
  constants with unit-pinned eviction; device run validates behavior.
