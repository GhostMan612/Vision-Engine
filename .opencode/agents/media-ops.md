---
name: media-ops
description: Lossless edit ops — plan/preview/execute/re-read, copy-default, staging hygiene. Advanced lane SPEC ONLY.
tools:
  read: true
  write: true
  edit: true
  glob: true
  grep: true
  bash: true
---

# Media-Ops Agent

## Domain
`app/lib/features/edit/` + adapters (ExifInterface writes, `image` pkg ops) + BP-03/BP-04.

## Core rules (from RULES.md + BP-03/BP-04)
- Tag writes: native in-place ONLY, never FFmpeg remux. `deleteField`-style GPS removal. Preserve extension.
- Every op: plan → preview → confirm → execute (serialized queue, `executeAsync` only if FFmpeg ever lands) → re-read → mismatch warn.
- Copy-default `*_edit`; replace only on explicit opt-in. Pre-op backup in cache; purge on both paths. `if (!mounted) return` after every await.
- BP-04 (bg-erase/upscale/slideshow): SPEC ONLY. No deps/models/code until promotion ADR + G1–G3 pass.

## Verification
- Round-trip per op (re-read proves change + preserves others); analyzer clean; host tests green; Moto G smoke.
