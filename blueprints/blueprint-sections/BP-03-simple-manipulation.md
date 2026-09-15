# BP-03 — Simple manipulation (explicit + verified)

> Phase 3. Every byte-changing op is plan → preview → confirm → execute → re-read. Nothing silent.

## Objective
Lossless-first edit set: EXIF date-shift, GPS-strip, orientation-fix, crop, resize/compress, plus rename templates v1.

## Scope
- EXIF ops (in-place, native channel; NEVER FFmpeg remux — tagger Tagging law): date-shift (±min, preserves all other tags; dump-diff proof), GPS-strip (removes GPS IFD; re-read proves gone), orientation-fix (lossless JPEG transform where possible; else explicit re-encode with quality warning + copy-default).
- Pixel ops (`image` pkg): crop (drag rect, confirms dims), resize/compress (long-edge + quality pickers; honest "resize" label — never "AI upscale").
- Policy: default writes `*_edit` copy beside source; in-place replace ONLY on explicit checkbox; pre-op backup in cache until re-read passes; staging purge after (tagger hygiene).
- Rename templates v1: `yyyyMMdd_HHmmss` (+ optional suffix preserve) built on `rename_core.buildPlan` so collisions/manifest/undo come free.
- Each op screen shows: before/after estimate, overwrite policy, provenance note, re-read result (match/mismatch warn).

## Non-scope
No bg-erase, no upscale-models, no slideshow (BP-04). No batch EXIF across folders (single-file first; batch only after G3).

## Verification (G3)
Round-trip per op on synthetic + Moto G files; GPS gone-proof; tag-diff preserves others; analyzer zero + host tests green.

## Reference lineage
Tagger Forge (`writeTags`/`deleteField`, export-preserve-ext, write→read-back, eject/cache-only, mounted guards, `if (!mounted) return`) + Recovery permission-degraded states.
