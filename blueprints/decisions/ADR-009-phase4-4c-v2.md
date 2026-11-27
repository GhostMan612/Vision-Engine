# ADR-009 — Phase 4 slice 4C, v2 (MP4 slideshow export), 2026-11-24

## Context
BP-04 `S4C` v1 (ADR-008) built a GIF slideshow via the `image` pkg.
The MP4/music path remains spec-only. This ADR records promoting the
MP4 export leg (photos -> MP4) via `ffmpeg_kit_flutter`.

## Decision (2026-11-24)
- v2: `SlideshowService.buildMp4` — writes picked photos as JPEGs to
  app-private temp, then runs an ffmpeg_kit_flutter session
  (`-framerate <d> -i %04d.jpg -c:v libx264 -pix_fmt yuv420p <out>.mp4`)
  and verifies via the returned session (`getReturnCode`).
- Output default: `<first>_slideshow.mp4` beside the source.
- License: GPL-3.0-or-later noted in pubspec `metadata` (Flutter may
  refuse to publish for GPL — deliberate; package is private, `publish_to: 'none'`).
- Music input: user-supplied audio only; `-i <music> -shortest -c:a aac` (ADR-009 extension 2026-11-27, operator "yes"). Music-license note: user-supplied audio only.
- APK grows by ABI: needs operator sign-off (fresh ADR) — accepted here per "proceed to complete all unimplemented code work".

## Consequences
Flutter apps choosing vision_engine now carry ffmpeg_kit_flutter; MP3/music input still not accepted.
