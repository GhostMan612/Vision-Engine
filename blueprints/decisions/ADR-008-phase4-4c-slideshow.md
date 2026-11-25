# ADR-008 — Phase 4 slice 4C, v1 (photos + music -> GIF slideshow), 2026-11-24

## Context
BP-04 `S4C`: slideshow / video-maker from photos + music, candidate
`ffmpeg_kit_flutter` (needs fresh ADR: +~40MB/ABI, GPL, Music-vault +
MediaStore export).

## Decision (2026-11-24, v1 bounded)
- v1: build a **GIF slideshow** from picked photos via the `image` pkg
  (`SlideshowService`), written as `_edit` copy. No music in v1 (deferred).
- v2/MP4 via ffmpeg_kit_flutter — needs a NEW fresh ADR (size/GPL) +
  operator sign-off; not started.
- APK unchanged (image pkg already allowed, ADR-002).

## Consequences
Slideshow semantics now cover photos; MP4/music path remains spec-only.
