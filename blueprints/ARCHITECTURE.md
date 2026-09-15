# ARCHITECTURE.md — Vision Engine

## 1. Engine / adapters split (Atlas law, non-negotiable)

```
packages/rename_core/      # PURE Dart: strip, plan, collisions, manifest codec. No flutter/material, no dart:io File ops beyond Path math, no MediaStore, no FFmpeg.
packages/metadata_core/    # PURE Dart: PhotoMeta/VideoMeta models, provenance enum, EXIF-field codec, golden JSON fixtures. No platform imports.
app/lib/
  adapters/                # SAF + MediaStore + ExifInterface + MediaMetadataRetriever + File I/O. ONLY place platform types appear.
  features/rename/         # Preview → confirm → execute → undo screens wired to rename_core.
  features/viewer/         # Grid → detail wired to metadata_core.
  features/edit/           # BP-03 ops (each: plan → preview → execute → re-read).
  core/                    # Serialized job queue (tagger FFmpegExecutor pattern), permission gate, share/logging.
tools/                     # Python/Dart probe harnesses (photo counting, EXIF dumps, fixture generators). Run via C:\venv-hub, settings untouched.
```

Dependency direction: `app → packages`. Never reverse. New package or hosted dep requires `blueprints/decisions/ADR-NNN-*.md` first.

## 2. Rename pipeline (mirrors `build_plan` + `execute_plan`)

```
pick folder (SAF, persisted URI)
  → enumerate (top-only default; recursive opt-in; dirs skipped; sort by name)
  → rename_core.buildPlan(entries, prefixes, caseSensitive)
      ├─ classify: no-prefix → staying set | empty-result → skip | candidate
      └─ collisions: (1) dst exists on disk & not in batch → skip
                     (2) dst in staying set → skip
                     (3) dst already claimed in batch → skip
  → PREVIEW (dry-run default; every decision listed; counts: scanned/to_rename/clean/dirs/collisions)
  → manifest write (CSV: original_name,new_name,original_path,new_path,content_uri,status)
  → CONFIRM → execute (re-check dst at exec time — TOCTOU guard; raw rename in the user-picked folder, D2-validated primary; one serialized queue)
  → report (renamed count, collision-skips, errors) → UNDO available from manifest (reverse order)
```

Pure logic (`strip`, 3 collision classes, sort) lives in `rename_core` and is golden-tested against Python fixtures. Only the final `src→dst` commit lives in the adapter: raw `File.rename` in the user-picked folder (D2-validated primary for app-created files on Moto G 2025). MediaStore is a deferred alternative platform integration, not the current path. Behavior against pre-existing third-party media is G5 territory — never assumed from D2.

## 3. Metadata flow (provenance travels)

```
source file → adapter probe
  ├─ photo: `exif` (Dart) → fallback native ExifInterface (HEIF/DNG coverage)
  └─ video: MediaMetadataRetriever bridge (duration, W×H, rotation, creation, location)
→ metadata_core model { field, value, provenance: exif|container|filesystem|unknown }
→ viewer: orientation-corrected dimensions; unknown rendered as —; GPS row links to BP-03 strip op
```

Never invent: missing EXIF datetime ≠ file mtime (show both, labeled). Never silently "fix" orientation — display corrected + badge the raw tag.

## 4. Edit flow (BP-03, tagger Forge pattern)

```
select op (date-shift | gps-strip | orient-fix | crop | resize/compress)
  → plan (explicit params; source hash for pure-rename proof)
  → preview (before/after bytes estimate; overwrite policy: default copy `*_edit`, replace only on opt-in)
  → confirm → execute (native in-place for EXIF; `image` pkg for pixels; serialized queue)
  → re-read (write→read-back) → mismatch = warn + keep pre-op backup
  → staging hygiene: temp copies purged; user originals never deleted without confirm
```

FFmpeg (if ever added for BP-04) owns ONLY concat/encode; NEVER tag writes (tagger Tagging law).

## 5. Storage & permissions (BP-05)

- SAF folder picker for DCIM (persisted `takePersistableUriPermission`); MediaStore query for grid; `createWriteRequest`/`createTrashRequest` consent on Android 11+.
- Runtime perms via `permission_handler`: `photos`/`videos` (or `READ_MEDIA_*`), checked before every batch; graceful degraded state when denied (viewer works on already-granted URIs).
- Cache discipline: probe outputs, thumbnails, pre-op backups under `getTemporaryDirectory`; purged on success/failure paths.

## 6. Test posture (Atlas test-posture law)

- Always here: `packages/*/test` golden + unit (Python-parity fixtures in `test/golden/`) + `app/test` widget (host, fake/temp-dir sourced).
- Only on explicit ask: `integration_test/` (MediaStore rename round-trip on Moto G).
- Never weaken a test to satisfy implementation. Fix the code; if the test is wrong, fix the test and say so.
