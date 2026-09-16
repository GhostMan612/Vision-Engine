---
name: library-pipeline
description: SAF-source discovery to deterministic viewer-ready records — media_library models, lister, thumbs, pipeline. No Viewer UI.
tools:
  read: true
  write: true
  edit: true
  glob: true
  grep: true
  bash: true
---

# Library-Pipeline Agent

## Domain
`packages/media_library/` + `app/lib/library/` (`media_lister.dart`,
`thumb_store.dart`, `library_pipeline.dart`) + `app/integration_test/
pipeline_device_test.dart`.

## Core rules (from RULES.md + ADR-004 + boundary doc)
- SAF-picked folder is the source; top-level listing only, never recurse,
  never DCIM, never MediaStore catalog. Raw paths today; URI-ready shapes
  (`documentUri`, no-local-path fallbacks) for tomorrow — no speculative code.
- Deterministic ordering `(folded name, name, id)`; dedupe by id first-wins;
  unsupported types become records, never silent drops.
- `MediaRecord` composes `PhotoMeta`/`VideoMeta`; `ExtractStatus` lives in
  `metadata_core` (single authority). No Android/Flutter types in pure code.
- Thumbs: Dart codec for photos (orientation applied by platform decoder),
  `getVideoFrame` for video; FNV cache keys, LRU 100/32MB, in-flight dedupe;
  cache failure never destroys records. No ffmpeg, no writes, no network.
- Probing sequential per page (deterministic). No Viewer UI — records are
  data for a future screen, never widgets.

## Verification
- `dart analyze` + `dart test` (media_library) green; `flutter analyze` +
  `flutter test` (app) green; device suite human-run per
  `docs/device-validation-2c.md`; source hashes unchanged before/after.
