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
- [x] MP6 pipeline (authorized 2026-09-15): MediaPipeline + 7/7 JVM tests
  + instrumented device suite authored+compiled; device run DONE
  2026-11-17 — 13/13 PASS on BLU B160V, Android 14
  (ExtractorDeviceTest 6/6, ThumbsDeviceTest 6/6,
  PipelineDeviceTest 1/1), Gradle BUILD SUCCESSFUL in 1m 32s,
  instrumented XML reports independently checked. Hardware
  substitution: the S1 spec named the Moto G; the run used the BLU
  B160V instead — no Moto G coverage is claimed. ACCEPTED
  2026-11-17 for the tested BLU B160V hardware scope (run head
  commit `18c45c6`; observed second run BUILD SUCCESSFUL in 25s,
  watched in full by the operator, no folder picker appeared);
  see `blueprints/standalone-v1/S1-MP6-DEVICE-RERUN.md` §7 and
  §13. NO Viewer, NO MP7
- [x] MP4 extraction (2026-09-15): readers + extractor + 15/15 JVM tests;
  instrumented device suite 6/6 PASS on Moto G (agent-run); NO MP5, NO Viewer
- [x] MP5 thumbnails (2026-09-15): PhotoThumbs + VideoFrames + cache
  wiring; 12/12 JVM + device suite 6/6 PASS on Moto G (agent-run);
  missing-asset setup failure fixed same session; NO Viewer, NO pipeline
- [x] 2C device validation CLOSED 2026-09-15: Flutter pipeline suite 6/6
  PASS on Moto G (agent-run; sort-expectation fix to verified truth)
- [x] `packages/metadata_core` models + provenance + fixtures (Slice 1, 2026-09-15: 18/18 tests, analyze clean — STOPPED here per change control)
- [x] Extraction layer 2B (2026-09-15): Kotlin `MetadataBridge` + `vision_engine/metadata` channel + Dart `MetadataAdapter` (status/provenance decode, MIME table, ISO-6709); 18/18 adapter host tests; synthetic fixtures + device integration test authored (human run pending); exifinterface 1.4.1 pinned (ADR-003)
- [x] 2B triage (2026-09-15): 4 device failures classified (fixture SOF, orient-0, status rule, duration assert) — host green (26/26 app), device rerun pending
- [x] 2B device validation CLOSED 2026-09-15: Moto G 5/5 PASS (human-pasted), single-lane build clean
- [ ] 2C pipeline (authorized 2026-09-15): media_library + lister + thumbs + orchestrator + device suite (in progress)

## Standalone v1 (2026-11-15–17, main `18c45c6`)

- [x] **S1 — MP6 device re-run:** 13/13 PASS on BLU B160V
      2026-11-17 (BUILD SUCCESSFUL, 1m 32s, XML checked); Moto G →
      B160V hardware substitution recorded, no Moto G coverage
      claimed. ACCEPTED 2026-11-17 for the tested BLU B160V
      hardware scope (head commit `18c45c6`; observed second run
      BUILD SUCCESSFUL in 25s, watched in full, no picker).
- [x] **S2 — Native rename port (PR #4, merged 2026-11-16,
      `4ef05e7`):** `vision-core` rename package green on JVM (38
      rename tests incl. 6 golden replays), host gate green.
- [~] **S3 — SAF picker + grants (PR #5, merged 2026-11-16,
      `2223beb`):** picker + persisted grants + folder list UI
      merged; host gate green; grant + discovery proven on B160V;
      reboot + reinstall survival verified with the S4B/S4C runs.
      Revoke-then-re-pick NOT verified — open item.
- [x] **S4 — Renamer screen (PR #6, merged 2026-11-17,
      `5ea6c2c`, + PR #7 fix, merged 2026-11-17, `b0d2b36`):**
      preview → execute → honest manifest → undo on a real folder;
      host gate green; S4B synthetic + S4C real-media rename/Undo
      passed on B160V at PR #7 head `4c444db` (hashes identical,
      41.5 MB MP4 byte-identical, grants + manifests survived
      reinstall). Stage (c) live DCIM NOT run — separate Glenn
      go-ahead required.
- [x] **PR #8 — Python safety residuals (merged 2026-11-17,
      `18c45c6`):** exclusive manifest, alias guard,
      dependency-aware undo, 12 adapted safety tests; pytest 30
      passed (18 + 12), CI pass.
- [~] **S5 — MP7 Viewer (draft PR #10, branch
      `feat/mp7-native-viewer`, base main `9c3a2aa`,
      2026-11-17):** URI-gap fix + grid → detail + field tables
      + instrumented URI tests built; vision-core JUnitCore OK
      (99) on this VM, pytest 30 passed. Host Gradle gate +
      device evidence PENDING; G2 stays OPEN. See
      `blueprints/standalone-v1/S5-DEVICE-RUNBOOK.md`.
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
