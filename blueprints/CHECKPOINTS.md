# CHECKPOINTS.md — Vision Engine (gates; evidence before status flips)

## G0 — Foundation freeze (Phase 0)
- [x] `RULES.md`, `AGENTS.md`, `SESSION_HANDOFF.md`, all `blueprints/*.md`, `BP-01..BP-05`, `ADR-001/002`, `.opencode/` agents+commands, `docs/` matrix, `C:\venv-hub\vision-engine\` lane exist.
- [x] No writes outside `C:\vision engine` + new `C:\venv-hub\vision-engine\` (+ flutter build cache).
- [x] Operator decided D1/D5; D2 probe launched (execution = G5 work).
  Evidence: handoff 2026-09-14 scaffold entry.

## G1 — Rename parity (Phase 1)
- [x] Python↔Dart parity probe green on ALL golden cases + chain case + ignore-case.
- [x] `dart analyze` (rename_core) + `flutter analyze --no-pub` (app) → "No issues found!".
- [x] Tests green: 22/22 package + 5/5 app (round-trip, TOCTOU, probes, widget).
- [x] Pure-rename byte-proof: SHA-256 identical before/after (probe + app flow test).

## G2 — Viewer honesty (Phase 2)
- [ ] Synthetic EXIF + video fixtures render with correct orientation-corrected dims, `—` for missing, provenance chips accurate.
- [ ] No invented values (grep: no silent mtime-as-EXIF substitution).
- [ ] Analyzer zero + host tests green. Moto G smoke pasted by human (real shots, files never committed).

## G3 — Lossless edits (Phase 3)
- [ ] Each op (date-shift, GPS-strip, orient-fix, crop, resize/compress) shows plan→preview→execute→re-read with mismatch warnings.
- [ ] GPS-strip verified gone on re-read; date-shift preserves all other tags (dump-diff); crop/resize defaults to copy.
- [ ] Analyzer zero + host tests green + Moto G smoke.

## G4 — Advanced spec (Phase 4)
- [ ] BP-04 reviewed; each deferred slice has cost/size/perf/consent analysis. No code, no deps, no models committed. Promotion requires fresh ADR.

## G5 — Shell round-trip (Phase 5)
- [x] D2 device evidence 2026-09-15: Moto G shared-folder probe COMPLETE
  (RAW_RENAME WORKS, undo 2/2, cleaned; human-pasted reports).
- [ ] Full DCIM round-trip — deferred until explicitly tasked (never probed).

## 2026-11-17 note (main `9c3a2aa`) — no checkpoint flips

- S1's platform-truth leg is done: the MP6 device suite ran 13/13
  PASS on BLU B160V (hardware substitution for the Moto G named in
  the S1 spec; no Moto G coverage claimed). This feeds S5/MP7
  readiness, but it flips no checkpoint on its own.
- G2 stays open: the Viewer (S5) is built as draft PR #10
  (`feat/mp7-native-viewer`, base main `9c3a2aa`) but the host
  Gradle gate has not run yet and there is no instrumented Viewer
  device evidence or real-shot smoke yet. G2 flips only when the
  instrumented Viewer tests + the real-shot read-only smoke pass
  (runbook: `blueprints/standalone-v1/S5-DEVICE-RUNBOOK.md`).
- G5 stays open: the full DCIM round-trip is still deferred, and
  S4 stage (c) live DCIM was explicitly not run.
- S1 is now ACCEPTED for the tested BLU B160V hardware scope
  (2026-11-17): the operator confirmation was received — no
  folder picker appeared during the watched second run — and the
  run head commit is `18c45c6`. See
  `blueprints/standalone-v1/S1-MP6-DEVICE-RERUN.md` §7 and §13.
