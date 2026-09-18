# CHECKLIST.md — Vision Engine (tick per session)

## Phase 0 — Foundation
- [x] Read Python scripts end-to-end
- [x] Survey 6 reference dirs (read-only) + venv-hub (settings untouched) + toolchain
- [x] Write `RULES.md` / `AGENTS.md` / `SESSION_HANDOFF.md`
- [x] Write `VISION_ENGINE_MASTER_BLUEPRINT.md` / `ARCHITECTURE.md` / `ROADMAP.md` / `CURRENT_STATE.md` / `CHECKLIST.md` / `CHECKPOINTS.md`
- [x] Write `blueprint-sections/BP-01..BP-05` + `decisions/ADR-001,ADR-002`
- [x] Create `.opencode/agents/` + `.opencode/commands/` + `docs/` matrix
- [x] Create `C:\venv-hub\vision-engine\` helper lane (README + requirements, no settings change)
- [x] Freeze + review with operator (D1 decided, D5 deferred, D2 probe launched)

## Phase 1 — Core renamer
- [x] `packages/rename_core` scaffold + Genesis headers
- [x] Golden fixtures (leading-only, case gate, empty skip, 3 collisions, sort, recursive)
- [x] Parity probe Python↔Dart green (6/6 + SHA-256 byte proof)
- [x] App preview screen on app-private dir (Rename tab: seed/preview/execute/manifest/undo)
- [x] `flutter analyze` zero + `flutter test` green (22/22 pkg + 5/5 app)

## Phase 2 — Metadata viewer
- [x] Platform migration blueprint (2026-09-15): GQ8=NO recorded,
  `NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` + ADR-005 written, research
  only — no implementation, no 2D
- [x] MP0 reference freeze (2026-09-15): `flutter-final` tag + baseline
  doc + 137-file digest manifest + verifier green; reference frozen
- [x] MP1 native foundation (2026-09-15): core 2/2, android lib compiles,
  app APK assembles, app unit 2/2 (Robolectric Compose launch); zero
  permissions; no Viewer, no MP2
- [x] MP2 domain port (2026-09-15): vision-core 48/48 incl. golden replay
  (9/9 fixtures) + FNV byte-vectors + edge suite; no new deps; boundary
  intact; NO MP3, NO Viewer
- [x] MP3 SAF discovery (2026-09-15): SafDiscovery + MimeTypes + 17/17
  Robolectric tests; test-scoped junit/robolectric reuse pinned versions;
  NO MP4, NO Viewer, NO permissions
- [x] MP4 extraction (2026-09-15): readers + extractor + 15/15 JVM tests;
  instrumented device suite authored (human run pending); NO MP5, NO Viewer
- [x] `packages/metadata_core` models + provenance + fixtures (Slice 1, 2026-09-15: 18/18 tests, analyze clean — STOPPED here per change control)
- [x] Extraction layer 2B (2026-09-15): Kotlin `MetadataBridge` + `vision_engine/metadata` channel + Dart `MetadataAdapter` (status/provenance decode, MIME table, ISO-6709); 18/18 adapter host tests; synthetic fixtures + device integration test authored (human run pending); exifinterface 1.4.1 pinned (ADR-003)
- [x] 2B triage (2026-09-15): 4 device failures classified (fixture SOF, orient-0, status rule, duration assert) — host green (26/26 app), device rerun pending
- [x] 2B device validation CLOSED 2026-09-15: Moto G 5/5 PASS (human-pasted), single-lane build clean
- [ ] 2C pipeline (authorized 2026-09-15): media_library + lister + thumbs + orchestrator + device suite (in progress)
- [ ] Viewer UI (grid → detail, orientation-correct, unknown-as-—, provenance chips)
- [ ] Moto G smoke (operator-supplied shots, never committed)

## Phase 3 — Simple manipulation
- [ ] Date-shift / GPS-strip / orientation-fix (write→read-back each)
- [ ] Crop / resize / compress (copy-default, replace opt-in)
- [ ] Rename templates v1 on rename_core pipeline

## Phase 4 — Advanced (spec only)
- [ ] BP-04 reviewed; no code until G1–G3 pass + promotion ADR

## Phase 5 — Shell hardening
- [x] App shell: `com.visionengine`, minSdk 26, compileSdk 37 (pinned, device-proven), 64-bit ABIs, media permissions, label
- [x] Settings persist (prefixes/case/recursive) + probe screen ships
- [x] D2 evidence (Moto G 2026-09-15): RAW_RENAME WORKS, undo 2/2, cleaned — MediaStore NOT justified, raw path stands, rename_core frozen
- [ ] Moto G DCIM round-trip incl. undo (explicitly deferred until tasked; probes never touch real DCIM)
