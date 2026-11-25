# ADR-006 — Phase 4 promotion, 2026-11-24

## Context
BP-04 (`blueprints/blueprint-sections/BP-04-advanced-deferred.md`) is
SPEC ONLY by design; no code until G4 review + operator promotion with
this ADR. G4 review cost/size/perf/consent table below; CHECKPOINTS G4
flipped 2026-11-24. `image` pkg (Phase 3 pixel ops) is already allowed
without fresh ADR (ADR-002).

## Slice review

### 4A — AI background eraser (candidate: on-device TFLite RMBG/U2-Net)
- Size: model ≤20MB INT8, isolated env; vendor or commit tiny model only.
- Perf: Moto G preview gate <5s/preview.
- Consent: explicit opt-in UI copy required.
- Degradation: must degrade to "unavailable offline" — never silently upload.
- Status: NOT promoted this session; no `tflite_flutter` dep added (ADR-002).

### 4B — Resolution upscale
- v1 honesty: ships `image`-pkg Lanczos resize (Phase 3 `pixel_ops.resize`) — labeled "resize", never "AI upscale" (done in Phase 3).
- Super-resolution (Real-ESRGAN-class): GPU + large weights — deferred.
- Status: PROMOTED v1 (already shipped in Phase 3; see ADR-002 `image`).

### 4C — Slideshow / video-maker with music
- Candidate: `ffmpeg_kit_flutter` (+~40MB/ABI; 150MB Play warning), explicit `-map`/probeStreams verify.
- Needs ADR re: GPL flag, music-license (user-supplied audio only), MediaStore export destination.
- Status: NOT promoted this session.

## Decision (2026-11-24)
- Promoted: 4B v1 (honest resize — shipped in Phase 3; no new work).
- Deferred (pending dedicated ADRs): 4A (RMBG), 4C (video-maker).
- Nothing added to Phases 1–3 builds.

## Consequences
APK stays lean; 4A/4C require fresh ADRs + operator sign-off before any code.
