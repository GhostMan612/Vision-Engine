# S6 — MP8 parity + MP9 Flutter retirement (spec only)

> Status: SPEC — FULL ALLOW revision 2026-11-24 (denies lifted; canonical law: RULES.md, AGENTS.md — both FULL ALLOW revision 2026-11-24).
> against main `d103ef6` for the Vision-Engine standalone v1 spec PR.
> Sibling slices: S1 MP6 device re-run, S2 native rename port, S3 SAF
> picker + persisted grants, S4 Renamer screen, S5 MP7 Viewer.
>
> Fact discipline (per
> `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §0): FACT =
> verified in repo/docs; REPO EVIDENCE = file:line or gate result;
> RECOMMENDATION = judgment; INFERENCE = strong but unverified;
> OPEN QUESTION = needs a decision or a device. Assumptions are never
> presented as facts.

## 1. Goal

Prove, on hardware, that the native Kotlin stack behaves identically to
the frozen Flutter reference (MP8), and then — only if every retirement
criterion passes and Glenn says so at the gate — remove the Flutter tree
from `main` in one atomic commit (MP9). MP8 is a verification slice: it
writes no product code except what the parity runs prove is missing. MP9
is a deletion slice with a rollback plan, not a celebration.

- FACT: the migration blueprint defines MP8 as "side-by-side
  reference-vs-native device runs + real-shot smoke (G2 gate lives here)"
  and MP9 as "criteria checklist → archive tag `flutter-final`, remove
  Flutter tree from `main` in ONE atomic commit."
- REPO EVIDENCE: `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §11
  (MP8, MP9); §15 (retirement criteria — ALL required); ADR-005
  (MP8 exit: "G2 + golden-match sign-off"; MP9 exit: "criteria checklist
  signed. Rollback: revert commit").

## 2. Non-goals

- MP8 does not fix discrepancies by editing the reference. The reference
  is frozen; a proven reference bug is documented as a discrepancy and
  handled through the migration decision process, never silently patched
  (FLUTTER_REFERENCE_BASELINE.md §13). If a discrepancy is found, MP8
  records it and stops that leg — it does not "fix" the frozen side.
- MP8 does not add features to either stack. New behavior discovered
  during parity runs goes to a new slice with its own spec; it is not
  folded into the parity run.
- MP9 does not delete history, does not force-push, does not rewrite the
  tag, and does not remove Flutter from any other branch. History stays
  in-repo forever (migration blueprint §15).
- MP9 is not automatic when tests go green. Retirement is a Glenn
  decision at the gate, full stop (see §7).

## 3. Inputs (exact files — read before building)

| File | What it pins |
|---|---|
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §11 | MP8/MP9 work packages (mission/inputs/outputs/tests/gates/device/exit/rollback shape). |
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §15 | Retirement criteria — ALL required. |
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §10 | Test migration matrix: golden JSON reused byte-identical; `make_fixtures.py` stays; `parity_probe.py` stays valid. |
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §5 | FNV-1a-64 key function: SAME canonical string so keys are comparable across stacks during parity runs. |
| `blueprints/FLUTTER_REFERENCE_BASELINE.md` | Frozen reference definition: tag, toolchain, contracts, test baseline (103/103 host green), device baseline, fixture inventory, freeze rules (§13). |
| `blueprints/FLUTTER_REFERENCE_MANIFEST.json` + `tools/verify_reference.py` (52 lines) | SHA-256 per reference file; exit 0 = intact, 1 = mismatch/missing. |
| `blueprints/ROADMAP.md` Phase 2 | G2 gate text; MP0–MP6 status lines. |
| `blueprints/CHECKPOINTS.md` §G2 | G2 checklist (fixture renders, no invented values, analyzer zero, Moto G smoke). |
| `blueprints/decisions/ADR-005-native-android-platform.md` | MP8/MP9 skeleton + "Non-decisions" (Q5: keep Flutter `app/` building during migration — recommend yes). |
| `tools/parity_probe.py` | Python↔Dart parity probe (6/6 + byte proof at freeze); rename semantics are stack-independent, so it stays valid for the Kotlin port's rename package (migration blueprint §10). |
| `docs/device-validation-2b.md`, `docs/device-validation-2c.md`, `docs/device-validation-mp4.md`, `docs/device-validation-mp5.md`, `docs/device-validation-mp6.md` | Device runbooks and the evidence format MP8 reuses (paste-back terminal output + "no DCIM touched" confirmation). |

### 3.1 Verified state of the reference (FACT)

- Tag `flutter-final` → commit `aae04f9` (verified live in this clone:
  `git tag -l flutter-final`, `git rev-parse --short flutter-final^{commit}`).
- FACT: the tag already exists and already points at the MP0 freeze
  commit. See §8 "Tag-name discrepancy" — the MP9 archive-tag wording
  collides with this, and it must be resolved before MP9 is attempted.
- Host baseline at freeze: 103/103 green (rename_core 22/22,
  metadata_core 20/20, media_library 17/17, app 44/44)
  — FLUTTER_REFERENCE_BASELINE.md §7.
- Device baseline at freeze: 2B extraction suite 5/5 PASS on Moto G
  (human-pasted); 2C pipeline device suite rerun outstanding at freeze —
  explicitly NOT claimed (FLUTTER_REFERENCE_BASELINE.md §8). Since then:
  2C device 6/6 PASS, MP4 device 6/6 PASS, MP5 device 6/6 PASS (all
  agent-run on Moto G); MP6 device run blocked (Moto G not on adb —
  S1 re-runs it). REPO EVIDENCE: `blueprints/CURRENT_STATE.md`
  (MP4/MP5/MP6 entries) and `docs/device-validation-mp6.md`.

## 4. Step-by-step work — MP8 parity validation

### Step 1 — Freeze the parity inputs

1. Run `tools/verify_reference.py` and record exit 0 before any parity
   run. If it exits 1, stop: the reference is not intact and no parity
   claim can be made.
2. Record the exact native head commit and the exact reference tag
   (`flutter-final` / `aae04f9`) in the parity report. Both SHAs are part
   of the evidence, not ambient context.
3. Assemble the parity fixture set: the 4 committed binary fixtures from
   `tools/make_fixtures.py` (`ve_exif.jpg` 324B, `ve_plain.png` 454B,
   `ve_minimal.mp4` 150B, `ve_thumb.jpg` 194B) + the 9 golden JSON files
   (rename_core 6, metadata_core 3). FACT: the generator is pure stdlib
   and emits the same bytes both stacks consume (migration blueprint §1 F,
   §10). Regenerate and diff against the committed fixtures first — if
   the generator's output moved, the parity run is measuring generator
   drift, not stack parity.

### Step 2 — Golden replay (host, both stacks)

1. Replay all 9 golden JSON files through the native Kotlin port and
   through the frozen Flutter reference; outputs must be byte-identical
   to the goldens and to each other.
2. Rename parity: run `tools/parity_probe.py` against the native rename
   package's outputs the same way it was run against Dart (6/6 + byte
   proof). FACT: the canonical rename semantics are now the post-PR #1
   Python: chain renames resolve in dependency order, manifests carry
   honest `planned`/`renamed`/`skipped` statuses, symlinks are skipped
   and counted (`skipped_symlink`), exit code 2 on any collision/skip.
   REPO EVIDENCE: `rename_camera_prefixes.py:100–103, 137, 171, 258, 274,
   412–413, 433` (main `d103ef6`, merged PR #1 `5f78388`). The native
   rename port (S2) must match these semantics; MP8 verifies it, and any
   mismatch is a defect in the port, not a golden to be "updated."
3. FNV thumb keys: compute the key for the same fixture file on both
   stacks from the SAME canonical string (`FNV(id, size, mtime, dim)`)
   and compare. Keys must be comparable across stacks (migration
   blueprint §5); a key mismatch is a parity failure even if the thumbs
   look the same.

### Step 3 — Side-by-side device runs (Moto G)

1. Run the reference Flutter device suites and the native instrumented
   suites on the SAME device, in the same session where possible:
   extraction (MP4: 6 tests), thumbs (MP5: 6 tests), pipeline (MP6:
   `PipelineDeviceTest` — stage → discover → records → thumbs → hashes
   unchanged → cleanup), plus the Flutter 2C `pipeline_device_test`.
2. For each suite, record: suite name, test count, pass/fail per test,
   device model/Android version, and the "no folder picker appeared, no
   DCIM touched" confirmation (the MP6 runbook's paste-back format,
   `docs/device-validation-mp6.md`).
3. Compare like-for-like outputs, not just green/red: extraction field
   values (all 11 photo + 10 video fields) for the same staged fixtures,
   thumb dimensions and JPEG bytes (256px photo target; ≤512px video
   frames, JPEG-85), cache behavior (hit after second load), and the
   untouched-bytes proof (SHA-256 identical before/after).
4. Any difference goes into a discrepancy table with: fixture, field,
   reference value, native value, and the decision (port defect to fix in
   a follow-up slice, reference bug to document per §13, or contract
   ambiguity to raise as an open question). No discrepancy is resolved
   silently inside MP8.

### Step 4 — Real-shot smoke (G2 lives here)

1. Same real device-camera shots, viewed in both stacks where the
   reference can show them (INFERENCE: the Flutter stack has no Viewer
   UI — only the Probe tab touches real folders — so the "both stacks"
   comparison for the Viewer is native-Viewer vs. the reference's Probe
   output for the same files, not two Viewers. State the actual
   comparison performed; do not claim a Flutter Viewer comparison that
   cannot exist).
2. Operator pastes observed results for both: corrected dims, `—`
   handling, provenance chips, thumb correctness. Shots are
   user-supplied, never committed.
3. Airplane-mode run on the native stack (migration blueprint §13 dynamic
   gate): full Viewer + rename-preview flow works offline.

### Step 5 — Parity report + sign-off evidence

1. Write the parity report (a doc in the repo, location per §9 Q3 — the
   report is MP8's output): inputs (SHAs + tag), golden replay table,
   FNV key table, device run tables, discrepancy table (empty or not),
   G2 checklist status, and the sign-off line.
2. MP8 exits only when: all goldens replay byte-identical, all device
   suites green on the same device, the discrepancy table is empty or
   every entry has a disposition, and G2 is closable. (ADR-005 MP8 exit:
   "G2 + golden-match sign-off.")

## 5. Step-by-step work — MP9 Flutter retirement

### Step 1 — Criteria checklist (ALL required, no partial retirement)

Verify each item with its evidence pointer. One unchecked item blocks
MP9 entirely; there is no "retire most of it now" variant.

- [ ] Native domain + SAF + metadata + thumbnail + pipeline tests green
      (host suites + device suites on the Moto G).
- [ ] Viewer parity established (S5 done, MP8 §4 Step 4 green).
- [ ] Device validation passes (MP8 device runs, §4).
- [ ] Security/privacy gates pass: manifest contains no INTERNET (grep
      gate, migration blueprint §13); dependency audit rejects
      network/analytics/crash SDKs; permission list frozen with a diff
      gate; airplane-mode run green; GPS never leaves the device
      (import-scope grep gate).
- [ ] Performance/memory gates pass (thumb bounds, cache bounds, no OOM
      on hostile fixtures — migration blueprint §14 R-03 validation).
- [ ] Golden behavior matches reference byte-for-byte (MP8 §4 Step 2).
- [ ] Glenn decision at the gate: explicit approval naming MP9
      (see §7). The checklist alone never authorizes retirement.

### Step 2 — Archive tag procedure

1. Resolve the tag-name discrepancy FIRST (§8): the name `flutter-final`
   is already taken by the MP0 freeze tag (`aae04f9`). MP9 must NOT move
   or overwrite it.
2. RECOMMENDATION: create a distinct archive tag at the merge commit
   immediately before removal (e.g. `flutter-final-archive` — exact name
   is §9 Q1 for Glenn) pointing at the last commit where the Flutter
   tree still exists, with an annotated message stating it is the final
   pre-removal state. Verify with `git rev-parse <tag>^{commit}` and
   record the SHA in the MP9 PR body.
3. Verify `tools/verify_reference.py` still exits 0 at that commit (the
   reference must be intact at the moment of archiving).

### Step 3 — Atomic removal (ONE commit)

1. Delete in a single commit: `app/` + `packages/` (rename_core,
   metadata_core, media_library) + Flutter-only tooling (Flutter
   workflows, `.opencode` Flutter lanes if Flutter-only — INFERENCE:
   the exact Flutter-only tooling list must be enumerated from the tree
   at removal time; do not guess it in this spec).
2. The commit must NOT touch any native file, any golden, any fixture,
   or any doc except a short note recording the removal and the archive
   tag. If a doc references a removed path, the fix is a separate
   follow-up commit, not scope creep inside the removal.
3. Commit message names the archive tag and the criteria checklist
   (e.g. "retire Flutter tree (MP9); reference archived at
   <tag>; criteria per migration blueprint §15").
4. Push and open the PR as usual (GitHub connector flow). The PR body
   carries the completed §5 Step 1 checklist with evidence links.
5. FACT: a removal commit is trivially revertible — that is the rollback
   (§6). No separate backup is needed because history is never rewritten.

### Step 4 — Post-removal verification

1. `git log --oneline` shows the removal as one commit; `git show
   --stat` lists only deletions under the Flutter tree (plus the doc note).
2. Native host suites green on the post-removal head; native device
   suites unaffected (no native file was touched).
3. `tools/verify_reference.py` is expected to FAIL after removal — it
   verifies the frozen reference tree, which no longer exists on `main`.
   FACT: this is the intended consequence, not a regression. The archive
   tag preserves the verifiable state; the procedure must state this
   explicitly in the MP9 PR so nobody "fixes" it by resurrecting the
   tool. (See §9 Q2 for where the post-removal verification story lives.)
4. Branch hygiene: the MP9 branch is pruned after merge (same as PR #1 /
   PR #2 — Glenn prunes, there is no delete-branch tool in the GitHub
   connection).

## 6. Rollback plan

| Situation | Rollback |
|---|---|
| MP8 finds a parity discrepancy | No rollback needed: nothing was changed. Record the discrepancy, fix in a follow-up slice, re-run MP8. |
| MP8 device run fails on one stack only | Re-run that stack's suite in isolation; if the failure is environmental (device unplugged, adb lost), record it as environmental and re-run. Do not "fix" by editing the other stack. |
| MP9 merged, native regression found later | `git revert <removal-commit>` on a new branch, PR as usual. The Flutter tree returns exactly as archived; the archive tag is untouched. |
| MP9 merged, Glenn changes his mind | Same revert path. The tag + full history remain; nothing is lost by the removal itself. |
| Tag created incorrectly (wrong commit) | Annotated tags can be deleted and recreated ONLY before anyone depends on them, and only by Glenn's explicit say-so; the MP0 `flutter-final` tag is never a candidate for this. Prefer creating a corrected second tag over moving one. |

## 7. Acceptance criteria

### MP8

- [ ] `tools/verify_reference.py` exit 0 recorded before the runs.
- [ ] All 9 golden JSON files replay byte-identical on the native stack.
- [ ] `parity_probe.py` green against the native rename package (6/6 +
      byte proof), under post-PR #1 canonical semantics.
- [ ] FNV thumb keys identical across stacks for the same fixtures
      (same canonical string).
- [ ] Reference + native device suites green on the same Moto G in the
      same session (MP4 6/6, MP5 6/6, MP6 pipeline, Flutter 2C pipeline).
- [ ] Field-by-field extraction comparison recorded (11 photo + 10
      video fields per staged fixture, including the untouched-bytes
      proof).
- [ ] Real-shot smoke recorded for the native Viewer (and the reference
      Probe output for the same files, where comparable) — user-supplied
      shots, never committed.
- [ ] Airplane-mode run green.
- [ ] Discrepancy table written; empty, or every entry dispositioned.
- [ ] Parity report written with both SHAs, the tag, and the sign-off
      line; G2 checklist closable.

### MP9

- [ ] Every item in §5 Step 1 checked, with its evidence pointer.
- [ ] Tag-name discrepancy (§8) resolved by Glenn's decision (§9 Q1).
- [ ] Archive tag created at the last pre-removal commit; SHA recorded.
- [ ] Removal is exactly ONE commit, deleting only the Flutter tree
      (+ doc note); `git show --stat` confirms deletions only.
- [ ] Native host suites green on the post-removal head.
- [ ] Post-removal `verify_reference.py` behavior stated in the PR
      (expected failure, by design — §5 Step 4 item 3).
- [ ] Glenn's explicit MP9 approval recorded (not inferred from green
      tests — §7 note below).
- [ ] MP9 branch pruned after merge.

> **Retirement is a Glenn decision at the gate, not automatic when
> tests go green.** The criteria checklist is necessary and not
> sufficient. MP9 does not start until Glenn names it, and the merge
> itself goes through the usual approval-card flow like every other PR.

## 8. Gates

1. MP8 host gates: golden replay + parity probe + FNV comparison, all
   byte-identical. Any mismatch fails MP8.
2. MP8 device gates: all listed suites green on the same Moto G; the
   "no DCIM touched" confirmation present for every device run.
3. G2 gate (`blueprints/CHECKPOINTS.md` §G2) closes on MP8 evidence.
4. MP9 gate: §5 Step 1 checklist complete AND Glenn's explicit approval.
5. No-new-dependency gate: MP8/MP9 add no dependencies to either stack.
6. Reference-integrity gate: `verify_reference.py` exit 0 at every MP8
   step and at the MP9 archive point; its expected post-removal failure
   is documented, not hidden.

### Tag-name discrepancy (FACT — must be resolved before MP9)

- FACT: migration blueprint §11 (MP9) and §15 say the archive tag is
  `flutter-final`.
- FACT: tag `flutter-final` already exists, pointing at the MP0 freeze
  commit `aae04f9` (verified live in this clone).
- FACT: ADR-005 §MP0 says the freeze tag is `flutter-reference-final`,
  which does NOT exist in this clone — the baseline doc and the actual
  tag agree on `flutter-final`, ADR-005's skeleton disagrees.
- RECOMMENDATION: treat `flutter-final` as the MP0 freeze tag (it is the
  one that exists and the one the baseline doc names); MP9 needs a
  different archive-tag name. The exact name is §9 Q1 — do not pick it
  in an implementation PR.

## 9. Risks

| ID | Risk | Likelihood / Impact | Mitigation |
|---|---|---|---|
| R-P1 | Parity fails on a real behavioral difference (e.g. EXIF edge, K15/K16 decimal form, K5 HEIF/DNG coverage) | MEDIUM / HIGH | Discrepancy table + disposition; reference bugs documented, never patched; port defects fixed in follow-up slices. |
| R-P2 | Device unavailable at parity time (as MP6 was — Moto G not on adb) | MEDIUM / MEDIUM (migration blueprint §14 R-06 adjacent) | MP8 is explicitly device-gated; host goldens can proceed, device legs wait. No emulator substitution (emulator ≠ device). |
| R-P3 | SAF provider variance makes "same device, same session" runs differ (migration blueprint §14 R-01) | MEDIUM / HIGH | Staged synthetic fixtures in app-private dirs for the comparable legs; real-folder legs run on the same picked folder on the same device. |
| R-P4 | MP9 removes a file something still needs (doc, script, or test referencing `app/` or `packages/`) | LOW / HIGH | Pre-removal grep for references to the Flutter tree; doc fixes go in a separate follow-up commit, not inside the removal. |
| R-P5 | Someone "fixes" post-removal `verify_reference.py` failure by resurrecting the reference | LOW / MEDIUM | §5 Step 4 item 3 states the expected failure in the MP9 PR body; the archive tag is the verifiable state. |
| R-P6 | Momentum loss across a 4–6 week port, MP8/MP9 slipping (migration blueprint §14 R-06) | MEDIUM / MEDIUM | Golden-first slices; reference stays runnable until MP9 (ADR-005 Q5: keep Flutter building during migration, recommend yes). |

## 10. Open questions (for Glenn — do NOT decide in this PR)

- **Q1 — MP9 archive-tag name:** `flutter-final` is taken by the MP0
  freeze tag. What should the MP9 archive tag be called (e.g.
  `flutter-final-archive`, `flutter-retired`, something else)? And
  should ADR-005's `flutter-reference-final` wording be corrected to
  `flutter-final` in a docs follow-up?
- **Q2 — Post-removal verification story:** after MP9, `verify_reference.py`
  cannot pass on `main` (the tree it verifies is gone). Should the tool
  gain an `--archive-tag` mode that verifies a checkout of the archive
  tag instead, or is the tag + history sufficient and the tool retired
  with the tree? (RECOMMENDATION: an `--archive-tag` mode is cheap and
  keeps the audit story intact — but it is a decision, not a default.)
- **Q3 — Parity report location:** where does the MP8 parity report live
  — `docs/` (alongside the device-validation runbooks), `blueprints/`,
  or the PR body only? (RECOMMENDATION: `docs/parity-mp8.md`, so the
  evidence survives the PR; Glenn's call.)
- **Q4 — Second OEM device:** migration blueprint §16 Q4 asks whether a
  second OEM device is wanted for R-01/R-02 breadth (Moto G remains the
  gate). MP8's "same device" rule answers the parity question, but
  breadth across OEMs is still an open question for the suite as a whole.
- **Q5 — Keep Flutter `app/` building during migration:** ADR-005
  non-decisions recommend yes (cheap, de-risks MP9). Confirm, or accept
  a broken Flutter lane mid-migration? (If the lane breaks, MP8's
  reference side cannot run — this must be decided before MP8, not
  discovered during it.)
- **Q6 — Viewer comparison method:** the Flutter stack has no Viewer UI.
  Is "native Viewer vs. reference Probe output for the same files" the
  accepted form of the MP8 real-shot comparison (§4 Step 4), or does
  Glenn want a different comparison (e.g. field-table dump comparison
  only, no UI comparison at all)?
