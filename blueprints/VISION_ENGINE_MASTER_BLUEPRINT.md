# VISION_ENGINE_MASTER_BLUEPRINT.md — v1.0 (FROZEN 2026-09-14)

> Frozen product spec. No substantive edits without explicit architect directive (Atlas frozen-blueprint law). Deltas go to `ROADMAP.md` + `decisions/`.

## 1. Vision

**Vision Engine** is an Android-first, offline-first camera-file studio. It does three things well and defers everything else:

1. **Safe rename** of Android camera default prefixes (`IMG_`/`VID_` → stripped, archival-grade).
2. **Honest metadata viewer** for device-camera photos/videos (EXIF + container metadata, provenance-tagged).
3. **Simple lossless manipulation** (date-shift, GPS-strip, orientation-fix, crop, resize/compress — all explicit, all verified).

Deferred explicitly (spec only, no code until Phases 1–3 gates pass): AI bg-eraser, resolution upscale, slideshow/video-maker with music.

## 2. Non-goals (v1.0)

- No cloud sync, no accounts, no analytics, no ads.
- No auto-organize/AI-tagging, no duplicate-finder, no gallery replacement.
- No social sharing pipeline beyond Android share-sheet.
- No iOS/web/desktop targets. Android only (Moto G is truth; emulator is a hint).

## 3. Canonical rename semantics (ported faithfully from `rename_camera_prefixes.py`)

The Python script is executable truth. The Dart port MUST be behavior-identical:

| Rule | Python source | Dart obligation |
|------|---------------|-----------------|
| Leading-only strip | `strip_leading_prefix`: `startswith` at pos 0 only | `MY_IMG_x.jpg` untouched; mid-string `IMG_` ignored |
| Default prefixes | `("IMG_","VID_")`, `--prefixes` override | Same default; custom list supported in settings (advanced) |
| Case-sensitive default | exact `IMG_`/`VID_`; `img_` skipped unless `--ignore-case` | Same; UI toggle defaults OFF |
| Empty-result skip | `IMG_` alone → `""` → skip + warn | Same (never produce empty filename) |
| Dirs/symlinks never renamed | `is_dir`/`is_file` gates | Same (MediaStore+Dart `FileSystemEntity` gates) |
| Never overwrite | 3 collision classes: target-exists-on-disk, staying-put, intra-batch dup → skip | Same 3 classes, same skip-messages |
| Deterministic order | sort by name (`iterdir` sorted; plan sorted by `old_name`) | Sort all plans by source name |
| Dry-run default | no `--execute` → change nothing; exit 2 if collisions/skips | Preview screen is default; Execute requires explicit confirm |
| Manifest + undo | CSV (`original_name,new_name,original_path,new_path,status`); undo reverses in reverse order with not-found/already-exists skips | Same CSV schema (adds `content_uri` column on Android); Undo screen replays it |
| Metadata-preserving | `os.rename` same-filesystem | Raw rename in the user-picked folder (D2-validated; no re-encode, no remux, bytes preserved) |
| Recursive opt-in | `--recursive` via `rglob` | Folder-tree toggle, default OFF (top folder only) |
| Verbose | `--verbose` lists every decision | Preview list shows every decision; collapsed summary otherwise |

HDR/motion suffixes (`_HDR`, `_BURST…`, `_MP`, `.mp4`) are preserved verbatim — the remainder after the prefix is opaque.

## 4. Metadata viewer (BP-02)

- **Photos (JPEG/HEIF/DNG):** EXIF via `exif` + native `ExifInterface` fallback: datetime-original, GPS, orientation, make/model, exposure, dimensions (orientation-corrected), file size, MIME.
- **Video (MP4/MOV):** `MediaMetadataRetriever` bridge: duration, width/height, rotation, creation time, location (if present), codec.
- **UX:** read-only grid → detail sheet; "unknown" shown as `—` (never invented); provenance chip per field (EXIF vs container vs file-system); orientation applied before dimensions.
- **Privacy:** GPS row has one-tap "strip" shortcut (routes to BP-03 explicit op, never silent).

## 5. Simple manipulation (BP-03) — explicit + verified

| Op | Guarantee |
|----|-----------|
| EXIF date-shift (±h/m) | In-place, preserves all other tags; write→read-back check |
| GPS strip | Removes GPS IFD only; verifies gone on re-read |
| Orientation-fix (lossless rotate + reset tag) | JPEG lossless transform where possible; else explicit re-encode with quality warning |
| Crop / resize / compress | Explicit quality/size picker; never overwrites source unless operator picks "replace" (default = copy `*_edit`); original retained |
| Rename templates (future) | Timestamp-based (`yyyyMMdd_HHmmss`) built on rename_core plan pipeline |

Every byte-changing op: plan → preview → confirm → execute → re-read → report. Failures roll back to pre-op copy.

## 6. Deferred advanced lane (BP-04, spec only)

- **AI bg-eraser:** on-device TFLite (U²-Net/RMBG, isolated env, <20MB model) preferred; cloud fallback only with explicit consent. Spec only.
- **Upscale:** Real-ESRGAN-class needs GPU — deferred; v1 ships `image`-package high-quality Lanczos resize only (honest labeling: "resize", never "AI upscale" until real model lands).
- **Slideshow/video-maker:** photo list + music → MP4 via `ffmpeg_kit` concat + `aac` audio; needs ADR (size + GPL) + Moto G perf gate. Spec only.

## 7. Android shell (BP-05)

- `com.visionengine` (proposal, D1), `minSdk 26 / compileSdk 36`, 64-bit ABIs (`arm64-v8a,x86_64`).
- Permissions: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` (33+), `READ_EXTERNAL_STORAGE` (≤32), no `MANAGE_EXTERNAL_STORAGE` unless evidence demands it (D2).
- Rename path: SAF folder grant (DCIM picker, persisted URI) + MediaStore `DISPLAY_NAME` update; `createWriteRequest` consent on 11+; raw `File.rename` only for app-private cache.
- Human builds in Android Studio; analyzer+test gates here.

## 8. Quality bars (gates in `CHECKPOINTS.md`)

- Rename parity: Python↔Dart golden fixtures identical (leading-only, case gate, empty skip, 3 collisions, sort).
- Analyzer zero + host tests green for every slice.
- Lossless proof: byte-hash unchanged for pure renames; tag round-trip verified for edits.
- Device truth: Moto G smoke for every storage-touching slice (emulator insufficient for MediaStore consent paths).

## 9. Reference lineage (read-only sources, credit)

- Safety model: `rename_camera_prefixes.py` (this repo) + tagger Forge `write→read-back` + staging hygiene.
- Workflow: pathfinder/nodes cold-start + session-end laws; mantle token discipline; Atlas ADR + frozen-blueprint + golden-fixture laws.
- Storage/media: tagger `build.gradle` (26/36/64-bit), permission_handler runtime, FFmpeg-async-only, asset-size ceiling.
- Offline/data: Recovery offline-first + analyzer-zero + `.venv-tf` isolation; Atlas engine/adapters + provenance-travels.
