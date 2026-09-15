# ADR-001 — Engine/adapters split (Atlas pattern)

> Status: ACCEPTED 2026-09-14 (scaffold conforms: `packages/rename_core` has zero Flutter/Android imports; `app/lib/adapters/` owns all dart:io + plugins).

## Context
Rename parity must be provable on host without Android; viewer/edit logic must survive storage-backend swaps (SAF vs MediaStore vs app-private).

## Decision
`packages/rename_core` + `packages/metadata_core` are pure Dart (no Flutter, Android, FFmpeg, `dart:io` file mutation). All platform I/O lives in `app/lib/adapters/`. `app → packages`, never reverse.

## Consequences
- Golden/parity tests run with `flutter test` on host (fast, no device).
- Storage-strategy change (D2) touches adapters only; core fixtures stay green.
- New packages/deps require a new ADR before code.
