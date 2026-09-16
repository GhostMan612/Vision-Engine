# Extraction boundary — Phase 2 Work Package 2B

> Read-only. Local-only. No network, no writes, no MediaStore, no DCIM contact.

## Layers

```
metadata_core (pure Dart — models, provenance, corrected dims, codec)
      ^ decoded via photoMetaFromJson / videoMetaFromJson (contract by construction)
MetadataAdapter (Dart, app/lib/adapters/metadata_adapter.dart)
      ^ MethodChannel vision_engine/metadata : probePhoto/probeVideo {path}
MetadataBridge (Kotlin, com.visionengine; ExifInterface + MediaMetadataRetriever)
```

Android types (`ExifInterface`, `MediaMetadataRetriever`, `Uri`, `Context`,
channels) never cross into `metadata_core`. The UI never extracts; it will
call the adapter (Slice 2C or later — not this package).

## Payload shape (deterministic)

`{ fieldName: { value: <string|double|int|null>, provenance: <name> } }` —
identical to the golden JSON shape. Missing key == unknown (pinned by
contract test). MIME is injected Dart-side from a const extension table
(provenance `filesystem`); unknown extension → `unsupported` WITHOUT a
channel call.

## Provenance map

| Source | Provenance |
|---|---|
| EXIF tags (datetime, GPS, orientation, make/model, exposure, dims) | `exif` |
| Retriever keys (duration, dims, rotation, date, location, codec) | `container` |
| File length, MIME-from-extension | `filesystem` |
| Missing, unparseable, failed | `unknown` |

Zero is preserved where legitimate (rotation 0, duration 0, 0-byte size);
dimensions ≤ 0 map to unknown (never a real size).

## Errors (never throw for ordinary missing data)

| Condition | Status |
|---|---|
| ≥1 EXIF/container field known (filesystem facts excluded) | `ok` |
| File read, zero EXIF/container fields | `partial` |
| Unknown extension (no channel call) | `unsupported` |
| Missing/unreadable/corrupt (`UNREADABLE`) | `unreadable` |
| Unexpected platform failure (`PLATFORM_FAIL`) | `unreadable` + coded warning |

`ok` counts substantive fields only: `fileSizeBytes`/`mime` are
filesystem facts, always known for existing files — counting them made
`partial` unreachable (found on Moto G 2026-09-15: a 0-byte file scored
`ok`). Orientation literal `0` maps to unknown: no camera writes it;
ExifInterface surfaces `0` for an absent tag, so keeping it would invent
metadata. Rotation `0` stays known (legitimate); duration `0` stays known
(legitimate for empty media).

`MissingPluginException` (unwired channel) propagates — a programming
failure must stay loud. Retriever is released in `finally`, including
exceptional paths; release failure is logged, never thrown.

## Location safety

GPS/ISO-6709 parsing is local string math. No geocoding, no network import
(the adapter imports `services`, `metadata_core`, `rename_core` path util
only), no transmission. Grep gate for reviewers: no `http` under
`app/lib/adapters/`.

## SAF compatibility (future, no speculative code)

Today the channel takes absolute file paths (the D2-validated raw path).
A future SAF-URI source adds a sibling channel method reusing the SAME
payload shape and decode path — `metadata_core` and the adapter decode
logic do not change. `ContentResolver.getType` can then replace the
extension table for MIME; until then the table is the deterministic rule.

## Known limitations (verified, not assumed)

- HEIC read depends on OS codec coverage; pre-API-28 behavior degrades to
  unknown fields (Moto G is modern — G2 device covers the contract).
- ExifInterface WRITES only JPEG/PNG/WebP — irrelevant here (read-only),
  but constrains Phase 3 HEIF/DNG edits (their problem, flagged early).
- Trak-less/odd containers: duration reports platform `0` (mvhd duration
  is NOT surfaced without tracks — verified on Moto G, do not expect
  mvhd passthrough); dims stay unknown; behavior is graceful by
  construction, exact values covered by host payload tests + G2 real-shot
  smoke.
- ExifInterface `getAttribute` renders RATIONAL tags as decimal strings
  (`1/120` → `0.008333333333333333`); the core exposure contract is
  opaque-String-verbatim, so assertions pin the platform-canonical decimal
  exactly (IEEE-deterministic, no approximation, no reconstruction).
- Android 14+ partial media access is G5/future scope; Phase 2 reads only
  already-granted paths.

## 2C additions (pipeline + thumbnails, read-only)

- Discovery: top-level listing of the user-picked folder only (no
  recursion); files classified by the adapter MIME table; unknown types
  become `unsupported` records (never silently dropped); ordering key is
  `(displayName.toLowerCase(), displayName, id)`; duplicates collapse by
  source id, first wins.
- Records: `MediaRecord` composes `PhotoMeta`/`VideoMeta` + source +
  status + warnings + thumbnail state; JSON-serializable; no Android or
  Flutter types cross the boundary.
- Photo thumbs: Flutter codec `targetWidth` decode (EXIF orientation
  applied by the platform decoder — verified 256x384 on the
  orientation-6 fixture); sources over 64 MB skip deterministically.
- Video thumbs: `getVideoFrame` (first frame, ≤512px side, JPEG-85);
  trackless/undecodable video yields `unavailable`, records intact.
- Cache: FNV key over (source id, size, mtime, dimension); LRU bounded
  at 100 entries / 32 MB; in-flight dedupe shares concurrent fetches;
  cache failure never destroys metadata/discovery results.
- Probing is sequential per page (deterministic; concurrency only on
  measured need). Orchestration lives in `LibraryPipeline`; nothing here
  is Viewer UI.
