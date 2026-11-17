# Standalone v1 — master checklist (spec)

> Gate-level checklist for the whole v1. Each slice file holds its own
> item-level acceptance checklist; this file tracks slice exits and
> cross-slice gates only. A slice is done when every item in its own
> file is checked AND its exit line below is checked.
>
> Status tokens: `[x]` done · `[~]` in progress · `[ ]` todo.
> Spec drafted 2026-11-15 against main `d103ef6`.

## Slice exits

- [x] **S1 — MP6 device re-run** (`S1-MP6-DEVICE-RERUN.md` §7).
      13/13 instrumented tests green on BLU B160V 2026-11-17 (Moto G
      → B160V substitution; BUILD SUCCESSFUL, 1m 32s, XML checked),
      `blueprints/CURRENT_STATE.md` MP6 entry updated. ACCEPTED
      2026-11-17 for the tested BLU B160V hardware scope: run head
      commit `18c45c6` confirmed; observed second run BUILD
      SUCCESSFUL in 25s, watched in full by the operator, no folder
      picker appeared; DCIM isolation supported by source
      inspection (qualified, not a filesystem trace). Terminal
      transcripts + `adb devices` output were supplied in the
      operator conversation but are not yet archived in GitHub.
- [x] **S2 — Native rename port** (`S2-NATIVE-RENAME-PORT.md` §7).
      `vision-core` rename package green on JVM (38 rename tests,
      6 goldens replayed byte-identical), `parity_probe.py` still
      exit 0 unmodified at S2 exit (PR #8 later added a chain
      assertion + Dart-path fix), `ModuleBoundaryTest` green, zero
      new deps, no caller in `vision-android`/`vision-app` at exit.
      PR #4, merged 2026-11-16 (`4ef05e7`).
- [~] **S3 — SAF picker + persisted grants**
      (`S3-SAF-PICKER-GRANTS.md` §6). Grant survives reboot + debug
      reinstall on BLU B160V (S4B/S4C runs, 17 Nov 2026); manifest
      permission list unchanged; DataStore the only new dep (+
      `kotlinx-coroutines-core`, Glenn-approved, see S3 §11).
      NOT fully closed: lost-grant state + re-pick NOT device-proven
      (revoke run never done). PR #5, merged 2026-11-16 (`2223beb`).
- [~] **S4 — Renamer screen** (`S4-RENAMER-SCREEN.md` §6). Preview
      dry-run proven (zero disk changes); staged smoke (a) app files
      green with a pasted report; manifest honest + app-private;
      undo restores exactly what renamed. Stage (b) as specified
      (copied/quarantined DCIM) NOT run — S4B zero-byte synthetics
      and S4C real JPEG + screen-recording MP4 in
      `Pictures/VE_TEST_S4C` passed Rename → Undo with unchanged
      hashes, but no copied DCIM directory was tested, so the
      original stage-(b) requirement remains UNVERIFIED unless the
      operator authorizes a revised acceptance criterion. Live DCIM
      is NOT part of this exit — separate Glenn-approved event.
      PR #6, merged 2026-11-17 (`5ea6c2c`), + PR #7 fix, merged
      2026-11-17 (`b0d2b36`); S4B/S4C passed on B160V at PR #7 head
      `4c444db`.
- [~] **S5 — MP7 Viewer** (`S5-MP7-VIEWER.md` §5, §11). Grid +
      detail render from a picked folder; corrected dims, `—` for
      unknown, provenance chips; URI-gap fix in `MediaPipeline`
      (descriptors closed in `finally`, bounds preserved);
      `ViewerPipeline` seam, `DetailFields.kt`, `ViewerViewModel`
      (page size 50, generation-guarded cancellation),
      `ViewerScreen` (third tab, Strip GPS disabled as Phase 3).
      29 new tests (6 core JVM + 8 URI pipeline + 10 ViewModel +
      6 screen) + instrumented `UriPipelineDeviceTest` (4 tests);
      vision-core JUnitCore OK (99) on this VM. Host Gradle gate
      + device evidence PENDING — draft PR #10, not merged; G2
      stays OPEN until the instrumented Viewer tests + real-shot
      read-only smoke pass (runbook:
      `S5-DEVICE-RUNBOOK.md`). Q1/Q2/Q3/Q4/Q5 decided
      2026-11-17 (see S5 §10); Q6 still open.
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
      MP9 by design; see S6 §5 Step 4 item 3. NOTE 2026-11-17:
      the script fails on clean main `9c3a2aa` (28 files, same
      list as on the S5 branch) — pre-existing, caused by PRs
      #1–#9 moving pinned files, not by any S5 change. No
      Flutter file was touched by PR #10.
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
the full table (D1–D12). Decided items are marked with the PR
reference (2026-11-17).

- [x] D1 rename mechanism + grant flags (S3 Q1/Q2, S4 Q1) —
      decided 2026-11-15: `renameDocument` + READ+WRITE (PR #5/#6)
- [x] D2 verify-after-write (S4 Q2) — decided (PR #6): re-query,
      UNVERIFIED never counted renamed
- [x] D3 manifest placement / export (S4 Q3) — decided (PR #6):
      app-private + `CreateDocument` export
- [~] D4 confirm 6-column manifest form (S2 Q1, S4 Q4) —
      implemented as recommended (PR #4/#6); formal confirmation
      still OPEN
- [x] D5 recursive rename (S4 Q5) — decided (PR #6): top-level only
- [x] D6 symlinks through SAF (S4 Q6) — decided (PR #6):
      undetectable, recorded limitation
- [x] D7 execute confirmation form (S4 Q7) — decided (PR #6): no
      second dialog
- [~] D8 MP6 run timing / runbook path / flake policy (S1 Q1–Q5) —
      run done 2026-11-17 on B160V; flake policy + stale runbook
      path still OPEN
- [~] D9 native rename port details (S2 Q2–Q6) — layout, astral
      pin, chain home decided (PR #4); `parity_probe.py` three-way
      still OPEN
- [x] D10 saved folders / picker / health-check / expiry (S3 Q3–Q6)
      — decided (PR #5)
- [~] D11 viewer details (S5 Q1–Q6) — Q1/Q2/Q3/Q4/Q5 decided
      2026-11-17 (PR #10): Strip button disabled "Phase 3", K5
      honest `—`, Coil NOT adopted, detail reuses the cached
      256px thumb, page size 50. Q6 (video beyond a still frame)
      still OPEN.
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
