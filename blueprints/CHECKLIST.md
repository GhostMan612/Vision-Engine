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
- [ ] `packages/metadata_core` models + provenance + fixtures
- [ ] Photo adapter (`exif` + ExifInterface fallback) + video bridge (MediaMetadataRetriever)
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
