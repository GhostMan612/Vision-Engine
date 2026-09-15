# Android metadata matrix (photos/video, device-camera reality)

> Source patterns: tagger Forge/Workbench (native tag writes, probe-verify), Recovery offline-first, Atlas unknown>invented. No real media committed — all values below are field names, not samples.

## Photos

| Format | EXIF read (Dart `exif`) | Native `ExifInterface` fallback | Notes |
|--------|------------------------|----------------------------------|-------|
| JPEG | Full (datetime, GPS, orientation, make/model, exposure) | Full | Orientation MUST be applied before W×H (K4). |
| HEIF (`.heic`) | Partial/none (version-dependent) | Primary path on Android 10+ | Viewer labels provenance `container` when EXIF missing. |
| DNG (RAW) | Partial (TIFF tags) | Primary for maker-notes | Never parse maker-notes as facts — show `—` unless decoded. |
| PNG/WebP (screenshots/edits) | No EXIF (ancillary chunks) | N/A — file-system dates only | Show mtime labeled as filesystem, never as EXIF datetime. |

EXIF edits (BP-03): in-place via ExifInterface; `deleteField`-style removal for GPS-strip (never `setField("",…)` — tagger law); write→read-back every time.

## Video

| Field | Source | Notes |
|-------|--------|-------|
| Duration, W×H, rotation | `MediaMetadataRetriever` (`METADATA_KEY_*`) | Rotation applied before display dims. |
| Creation time | `METADATA_KEY_DATE` if present, else filesystem | Label which one. |
| Location | `METADATA_KEY_LOCATION` if present | Rare on stock camera; `—` is normal. |
| Codec/container | `FFprobeKit` ONLY if ffmpeg ADR lands (BP-04); else mime + extension | Never guess codec from extension. |

## Scoped-storage rename reality (K1)

- Android 10+: raw `File.rename` on shared DCIM fails. Path: SAF persisted folder URI → MediaStore `DISPLAY_NAME` update → `createWriteRequest` consent (11+) → verify via re-query.
- Timestamps: `DATE_TAKEN`/`DATE_ADDED` preserved by `DISPLAY_NAME` update (no re-encode); never promise mtime preservation on all OEMs — verify on Moto G and record.
- HDR/motion suffixes and burst tails are opaque remainder — never reformat.
