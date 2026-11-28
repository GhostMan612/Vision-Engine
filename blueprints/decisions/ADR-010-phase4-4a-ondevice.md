# ADR-010 — Phase 4 slice 4A, on-device TFLite eraser (2026-11-27)

## Context
ADR-007 shipped a consent-gated cloud-fallback eraser. BP-04's on-device
TFLite RMBG/U²-Net candidate stayed deferred: no model, no dep, and no
offline path. Operator granted full authorization on 2026-11-27 to finish
all deferred items.

## Decision
- Add `tflite_flutter 0.12.1`.
- Bundle `app/assets/models/selfie_segmenter.tflite` (249,537 bytes,
  MediaPipe selfie segmenter, float16). It is an on-device person/foreground
  segmenter, not a general-purpose RMBG/U²-Net — labelled honestly.
- `TfliteEraseEngine` seam (`MaskFn`) in `app/lib/features/erase/tflite_mask.dart`:
  shape-driven input/output, softmax for 2-channel masks, nearest-neighbour
  mask resize; always writes `<base>_edit.png` and re-reads the source to
  prove its bytes are unchanged. No network, no consent needed for local.
- B160V device finding (2026-11-27): the bundled MediaPipe float16 model loads
  but `TfLiteInterpreterAllocateTensors` fails under LiteRT 1.4.0; a
  deterministic border-seed flood-fill `heuristicMask` is used as an automatic
  offline fallback, so on-device erase still works. TFLite 0.11.0 was tried and
  reverted (duplicate Android namespace with AGP 9). No quality claim is made
  for the fallback; general-object RMBG still needs a compatible model.
- `EraseService` default engine is `onDevice`; cloud remains explicit
  (`EraseEngine.cloud`) and keeps ADR-007 consent gating. If the model is
  unavailable, the local path returns a warning and performs no upload.
- APK JVM alignment (`JVM 17` for plugin subprojects) is recorded in ADR-011.

## Consequences
Offline-first eraser now real on device. GPU/CPU latency and edge quality
are not benchmarked against BP-04's <5 s/preview bar; no quality claim is
made. General-object RMBG still needs a larger vendored model.
