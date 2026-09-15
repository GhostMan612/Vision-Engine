# BP-04 — Advanced lane (SPEC ONLY — do not implement)

> Phase 4. Frozen as spec until G1–G3 pass AND operator promotes a slice with a fresh ADR. Any code here before promotion violates Scope law.

## Slices (spec only)

### 4A. AI background eraser
- Candidate: on-device TFLite RMBG/U²-Net (<20MB INT8, isolated `.venv-tf`-trained or vendored model, `assets/models/` committed only if tiny like Recovery coach model).
- Fallback: explicit-consent cloud API (keys in device prefs only, never repo). Offline-first rule: eraser must degrade to "unavailable offline" rather than silently upload.
- Needs: model-size budget, Moto G latency gate (<5s/preview), edge-quality bar, consent copy. No dep (`tflite_flutter`) until ADR.

### 4B. Resolution upscale
- v1 honesty: ship `image`-pkg Lanczos "resize" only, labeled as such. True super-resolution (Real-ESRGAN-class) needs GPU + large weights — deferred; spec the perf/size/thermal gates (Recovery aura-thermal-gating pattern) before any claim of "AI upscale".

### 4C. Slideshow / video-maker from photos + music
- Candidate: `ffmpeg_kit_flutter` concat (`-framerate`, `scale,format=yuv420p`, `acrossfade`/`amix` for audio) — Flutter owns muxing (tagger Muxing law: explicit `-map`, `probeStreams` verify).
- Needs ADR: APK size (+~40MB/ABI, 150MB Play warning), GPL flag, Moto G encode-time gate, music-license note (user-supplied audio only), Music-vault destination + MediaStore export.
- Shares tagger Pipeline batch-loop guards (`_isBatchRunning` reset on every throw) and Workbench queue pattern.

## Promotion bar (G4)
Per-slice ADR (why this engine, size/perf/consent evidence) + device budget + operator sign-off. Nothing here may add deps, models, or permissions to Phases 1–3 builds.
