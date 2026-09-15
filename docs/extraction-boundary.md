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
| ≥1 field known | `ok` |
| File read, nothing known | `partial` |
| Unknown extension (no channel call) | `unsupported` |
| Missing/unreadable/corrupt (`UNREADABLE`) | `unreadable` |
| Unexpected platform failure (`PLATFORM_FAIL`) | `unreadable` + coded warning |

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
- Trak-less/odd containers: duration may still parse (mvhd), dims stay
  unknown; behavior is graceful by construction, exact values covered by
  host payload tests + G2 real-shot smoke.
- Android 14+ partial media access is G5/future scope; Phase 2 reads only
  already-granted paths.
