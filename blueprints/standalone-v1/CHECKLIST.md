# Standalone v1 — master checklist (spec)

> Gate-level checklist for the whole v1. Each slice file holds its own
> item-level acceptance checklist; this file tracks slice exits and
> cross-slice gates only. A slice is done when every item in its own
> file is checked AND its exit line below is checked.
>
> Status tokens: `[x]` done · `[~]` in progress · `[ ]` todo.
> Spec drafted 2026-11-15 against main `d103ef6`.

## Slice exits

- [ ] **S1 — MP6 device re-run** (`S1-MP6-DEVICE-RERUN.md` §7).
      13/13 instrumented tests green on Glenn's Moto G, paste-back
      delivered, `blueprints/CURRENT_STATE.md` MP6 entry updated.
- [ ] **S2 — Native rename port** (`S2-NATIVE-RENAME-PORT.md` §7).
      `vision-core` rename package green on JVM, 6 goldens replayed
      byte-identical, `parity_probe.py` still exit 0 unmodified,
      `ModuleBoundaryTest` green, zero new deps, no caller in
      `vision-android`/`vision-app`.
- [ ] **S3 — SAF picker + persisted grants**
      (`S3-SAF-PICKER-GRANTS.md` §6). Grant survives reboot + debug
      reinstall on Moto G; lost-grant state + re-pick proven; manifest
      permission list unchanged; DataStore the only new dep.
- [ ] **S4 — Renamer screen** (`S4-RENAMER-SCREEN.md` §6). Preview
      dry-run proven (zero disk changes); staged smoke (a) app files
      and (b) copied/quarantined DCIM green with pasted reports;
      manifest honest + app-private; undo restores exactly what
      renamed. Live DCIM is NOT part of this exit — separate
      Glenn-approved event.
- [ ] **S5 — MP7 Viewer** (`S5-MP7-VIEWER.md` §5). Grid + detail
      render from a picked folder; corrected dims, `—` for unknown,
      provenance chips; G2 closable except the human smoke, which is
      recorded as device evidence in the slice.
- [ ] **S6 — MP8 parity + MP9 retirement**
      (`S6-MP8-MP9-PARITY-RETIREMENT.md` §7). MP8: 9 goldens
      byte-identical, parity probe green against the native rename
      package, FNV keys identical, both stacks green on the same
      Moto G, discrepancy table empty or dispositioned, parity report
      written. MP9: every §5 Step 1 criterion checked with evidence,
      archive-tag name decided (D12), removal exactly one commit,
      native suites green post-removal, Glenn's explicit approval
      recorded.

## Cross-slice gates (checked at every implementation PR, not once)

- [ ] `tools/verify_reference.py` exit 0 — the frozen Flutter
      reference is untouched (REC-10, R-10). Expected to FAIL after
      MP9 by design; see S6 §5 Step 4 item 3.
- [ ] Manifest contains no INTERNET (grep gate); no new permission
      without a diff-gate review (REC-06).
- [ ] Dependency audit: no network/analytics/crash SDK; any new dep
      carries its ADR justification (REC-04).
- [ ] Airplane-mode device run green for every slice with a device
      leg (REC-06).
- [ ] Honesty: unknown renders `—` / `unknown` / `skipped`, never a
      guessed value; manifests written after the execute decision
      (REC-11).
- [ ] No slice deletes or edits any part of the Flutter tree before
      MP9 (REC-10).
- [ ] No workflow file (`.github/workflows/`) changed inside a spec
      or implementation PR via the API — web-editor route only
      (REC-09, R-09).

## Open decisions (gate on Glenn, not on code)

Each decision below lives in its slice file; it is listed here so the
v1 cannot be called done with one still open. See `README.md` §5 for
the full table (D1–D12).

- [ ] D1 rename mechanism + grant flags (S3 Q1/Q2, S4 Q1)
- [ ] D2 verify-after-write (S4 Q2)
- [ ] D3 manifest placement / export (S4 Q3)
- [ ] D4 confirm 6-column manifest form (S2 Q1, S4 Q4)
- [ ] D5 recursive rename (S4 Q5)
- [ ] D6 symlinks through SAF (S4 Q6)
- [ ] D7 execute confirmation form (S4 Q7)
- [ ] D8 MP6 run timing / runbook path / flake policy (S1 Q1–Q5)
- [ ] D9 native rename port details (S2 Q2–Q6)
- [ ] D10 saved folders / picker / health-check / expiry (S3 Q3–Q6)
- [ ] D11 viewer details (S5 Q1–Q6)
- [ ] D12 MP9 tag name / verification story / report location /
      second device / Flutter lane / comparison method (S6 Q1–Q6)

## v1 done means all of these are true

- [ ] S1–S5 slice exits checked.
- [ ] S6 MP8 exit checked (parity proven on hardware).
- [ ] S6 MP9 exit checked (Flutter retired by Glenn's explicit
      approval, archive tag recorded, branch pruned).
- [ ] The native app on Glenn's Moto G does the original job:
      pick a real folder, preview, execute, undo — plus the Viewer —
      with the manifest in hand and no invented data anywhere.
