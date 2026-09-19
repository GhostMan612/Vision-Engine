# ROADMAP.md — Vision Engine (phase tracker)

> Status tokens: `[x]` done · `[~]` in progress · `[ ]` todo · `[-]` deferred/dropped. Gate per phase in `CHECKPOINTS.md`.

## Phase 0 — Foundation & spec [x] (closed 2026-09-14)
- [x] Python truth analyzed (`rename_camera_prefixes.py` 387 L + `rename_min.py` 43 L)
- [x] Reference survey (6 Sovereign dirs, read-only) + toolchain verify (Flutter 3.47 / Dart 3.13 / hub Python 3.14.6)
- [x] Operating docs freeze (`RULES.md`, `AGENTS.md`, `SESSION_HANDOFF.md`, blueprints, agents/commands, docs matrix)
- [x] `C:\venv-hub\vision-engine\` helper lane (requirements + README, no settings touched)
- [x] Master blueprint v1.0 frozen + `decisions/ADR-001` + `ADR-002` (both ACCEPTED at scaffold)
- Gate: G0 CLOSED.

## Phase 1 — Core renamer (port + prove parity) [x] (host side closed 2026-09-14)
- [x] `packages/rename_core`: `stripLeadingPrefix`, `buildPlan` (3 collisions, sort), manifest CSV codec, undo-order codec
- [x] `test/golden/`: 6 fixtures (leading-only, case gate, empty skip, collision trio, sort, recursive)
- [x] `tools/parity_probe.py`: Python `build_plan` vs Dart `buildPlan` — GREEN (6/6 + byte proof)
- [x] `app/` rename preview screen (dry-run default) wired to `rename_core` on app-private dir first (no MediaStore yet)
- Gate: G1 CLOSED (host). Device round-trip tracked under G5/D2.

## Phase 2 — Metadata viewer (read-only first) [~] (2C authorized + in progress 2026-09-15; Viewer UI still out of scope)

> Platform track (GQ8=NO): native Kotlin + Compose is the migration
> target — see `NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` + ADR-005. No
> implementation started; 2D stays locked until kickoff. MP0 reference
> freeze done (`flutter-final` tag + baseline + manifest). MP1 native
> foundation done (core/android/app build green, APK assembles). MP2
> domain port done (48/48 golden replay, no new deps). MP3 SAF discovery
> done (core 55/55 + 17/17 Robolectric, no MP4). MP4 extraction done
> (32/32 JVM + device suite 6/6 PASS on Moto G, agent-run). MP5 thumbnails
> done (12/12 JVM + device 6/6 PASS on Moto G, agent-run; no Viewer, no
> pipeline). 2C Flutter device suite 6/6 PASS (agent-run). MP6 pipeline
> implemented + JVM-green; device run blocked (Moto G not on adb —
> rerun per `docs/device-validation-mp6.md`).
- [x] `packages/metadata_core`: `PhotoMeta`/`VideoMeta` + provenance enum + fixture JSON (19/19 tests, analyze clean; no app code, no new deps)
- [x] 2B extraction layer: Kotlin bridge + channel + `MetadataAdapter` (18/18 host tests); device integration test authored, human Moto G run pending; NO Viewer/UI started
- [x] 2B triage 2026-09-15 (SOF fixture, orient-0 normalization, status-rule fix, duration truth) — host green, device rerun pending; no 2C
- [x] 2B FORMALLY CLOSED 2026-09-15: Moto G device suite 5/5 PASS, single-lane build clean; G2 still open (Viewer + real-shot smoke)
- [ ] Adapter: `exif` + ExifInterface fallback (photo); MediaMetadataRetriever bridge (video)
- [ ] Viewer UI: grid → detail, orientation-corrected dims, unknown-as-`—`, provenance chips, GPS→strip shortcut (routes to Phase 3, no silent edit)
- Gate: G2 (viewer shows synthetic EXIF/video fixtures correctly; analyzer zero; Moto G smoke on real device shots — user-supplied, never committed)

## Phase 3 — Simple manipulation (explicit + verified) [ ]
- [ ] EXIF date-shift, GPS-strip, orientation-fix (in-place, write→read-back)
- [ ] Crop / resize / compress (copy-default `*_edit`; replace only on opt-in; quality picker)
- [ ] Rename templates v1 (timestamp `yyyyMMdd_HHmmss` built on rename_core pipeline)
- Gate: G3 (round-trip verified on Moto G; byte-proof for pure renames; no silent overwrites)

## Phase 4 — Advanced lane (SPEC ONLY until G1–G3 pass) [-]
- [ ] BP-04 spec: bg-erase (TFLite RMBG candidate, isolated env), upscale (honest resize until real model), slideshow/video-with-music (ffmpeg concat ADR + size/perf gate)
- [ ] No code, no deps, no models until operator explicitly promotes a slice
- Gate: G4 (spec reviewed; promotion requires fresh ADR + device perf budget)

## Phase 5 — Android shell hardening [~] (shell + D2 done; DCIM deferred until tasked)
- [x] BP-05 shell: `com.visionengine`, minSdk 26, compileSdk 37 (device-proven), 64-bit ABIs, permissions, settings persist, probe screen
- [x] D2 evidence CLOSED/PASS (Moto G 2026-09-15): raw rename via
  user-picked folder validated → SAF-pick + raw rename stays primary;
  MediaStore adapter NOT justified → deferred, rename_core/behavior frozen
- [ ] DCIM round-trip only when explicitly tasked
