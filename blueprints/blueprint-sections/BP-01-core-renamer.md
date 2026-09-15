# BP-01 — Core renamer (port + parity proof)

> Phase 1. Highest priority — the product's reason to exist. No metadata/edit work may delay this gate.

## Objective
Pure-Dart `packages/rename_core` that is behavior-identical to `rename_camera_prefixes.py`, proven by golden fixtures + a Python↔Dart parity probe.

## Scope
- `stripLeadingPrefix(name, prefixes, caseSensitive)` — leading-only, `""`-on-empty, `null`-on-no-match.
- `buildPlan(entries, prefixes, recursive, caseSensitive)` — staying-set, 3 collision classes, deterministic sort. Entries are plain `{name, isDir, isFile, exists}` structs (no `dart:io` inside core — testable on host).
- Manifest codec: `original_name,new_name,original_path,new_path,content_uri,status` (superset of Python's 5 cols; `content_uri` empty on desktop).
- Undo-order codec: reverse-order replay with not-found / already-exists skips (mirror `undo_from_manifest`).
- Golden fixtures in `packages/rename_core/test/golden/`: `leading_only.json`, `case_gate.json`, `empty_skip.json`, `collision_trio.json`, `sort_determinism.json`, `recursive.json`. All synthetic names.
- `tools/parity_probe.py` (runs on hub Python, settings untouched): builds a synthetic tree in temp, runs Python `build_plan` (imported from repo script) + `dart run` core dump, diffs JSON. Green = identical op lists.

## Non-scope
No MediaStore, no UI beyond a bare preview list on app-private dir, no templates (Phase 3), no recursion UI beyond a toggle.

## Verification (G1)
Parity green on all six fixture files + `flutter analyze` zero + `flutter test` green + SHA-256 byte-proof for pure renames.

## Reference lineage
Python script (this repo) + Atlas golden-fixture law + tagger serialized-queue + staying-set pattern.
