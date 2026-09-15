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
