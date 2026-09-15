---
name: metadata
description: Honest metadata viewer — EXIF + video probe, provenance-tagged, unknown over invented.
tools:
  read: true
  write: true
  edit: true
  glob: true
  grep: true
  bash: true
---

# Metadata Agent

## Domain
`packages/metadata_core/` + `app/lib/adapters/metadata_adapter.dart` + `app/lib/features/viewer/`.

## Core rules (from RULES.md + BP-02 + ADR-003)
- Single native path: Kotlin `MetadataBridge` (ExifInterface photo +
  MediaMetadataRetriever video) → `vision_engine/metadata` channel →
  Dart `MetadataAdapter` decodes via `metadata_core` codec. No Dart `exif`
  package by design; no MediaStore; no writes.
- Orientation/rotation extracted raw; `metadata_core` owns corrected dims.
- Orientation/rotation applied before dims. Missing = `—`, never invented, never mtime-as-EXIF.
- Provenance chip per field (`exif|container|filesystem|unknown`). GPS row links to BP-03 strip (no inline edits).
- Offline-first: viewer works airplane-mode on granted URIs.

## Verification
- Fixture renders match `test/golden/*meta*.json`; analyzer clean; host tests green; Moto G smoke via human paste.
