# BP-04 — Advanced lane (promoted slices recorded below)

> Phase 4. Original spec-only rule superseded per-slice by ADR-006 through
> ADR-011 and operator authorization (2026-11-24 / 2026-11-27). Unpromoted
> items stay spec-only.

## Slices (spec only)

### 4A. AI background eraser
- DONE 2026-11-27 (ADR-007 cloud + ADR-010 on-device TFLite segmenter,
  `app/assets/models/selfie_segmenter.tflite`, `MaskFn` seam). Device suite
  `app/integration_test/on_device_erase_device_test.dart` runs the real model.
- Original candidate note (history): on-device TFLite RMBG/U²-Net (<20MB
  INT8, isolated `.venv-tf`-trained or vendored model, `assets/models/`
  committed only if tiny like Recovery coach model). The bundled 244 KB
  model is a person segmenter, not RMBG — no general-object quality claim.

### 4B. Resolution upscale
- v1 honesty: ship `image`-pkg Lanczos "resize" only, labeled as such. True super-resolution (Real-ESRGAN-class) needs GPU + large weights — deferred; spec the perf/size/thermal gates (Recovery aura-thermal-gating pattern) before any claim of "AI upscale".

### 4C. Slideshow / video-maker from photos + music
- DONE 2026-11-27: GIF (ADR-008), MP4 via `ffmpeg_kit_flutter_new` (ADR-009/011),
  user-supplied music; device suite `app/integration_test/mp4_device_test.dart`.
- Candidate: `ffmpeg_kit_flutter` concat (`-framerate`, `scale,format=yuv420p`, `acrossfade`/`amix` for audio) — Flutter owns muxing (tagger Muxing law: explicit `-map`, `probeStreams` verify).
- Needs ADR: APK size (+~40MB/ABI, 150MB Play warning), GPL flag, Moto G encode-time gate, music-license note (user-supplied audio only), Music-vault destination + MediaStore export.
- Shares tagger Pipeline batch-loop guards (`_isBatchRunning` reset on every throw) and Workbench queue pattern.

## Promotion bar (G4)
Per-slice ADR (why this engine, size/perf/consent evidence) + device budget + operator sign-off. Nothing here may add deps, models, or permissions to Phases 1–3 builds.
