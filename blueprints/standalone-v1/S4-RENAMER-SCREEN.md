# S4 — Renamer screen, real folders (native)

> Status: IMPLEMENTED — PR #6, merged 2026-11-17 (main `5ea6c2c`),
> plus PR #7 (S4-FIX-01), merged 2026-11-17 (main `b0d2b36`).
> The spec text below is preserved; implemented items are ticked in
> §1 and §6, superseded claims are marked in §3–§4, device evidence
> is recorded in §8, and Glenn's Q-decisions are recorded in §10.
> Research date: 2026-11-15. Baseline: main `d103ef6`
> (PR #2 merged: python-app CI tests real code, PyPI workflow removed).

## 0. Reading guide (fact discipline)

Labels used throughout: FACT (verified in repo/docs), REPO EVIDENCE
(file:line or gate result), RECOMMENDATION (judgment), INFERENCE
(strong but unverified), OPEN QUESTION (needs a decision — Glenn's, not
settled here). Assumptions are never presented as facts. Nothing below
claims device validation that has not happened; the Moto G is truth and
the emulator is a hint.

## 1. Goal

The thin native app does the job the repo was started for: pick a real
folder, preview the prefix rename, execute it, write an honest manifest,
undo it — on a real user folder, with the same safety the Python
canonical script has had since the beginning.

- [x] Compose Renamer screen in `vision-app`: pick folder (from S3's
      saved grants) → preview → execute → manifest → undo → post-run
      report. **Implemented PR #6:** two-tab shell (Folders |
      Renamer), `RenamerScreen` + `RenamerViewModel`.
- [x] Dry-run is the default. Nothing changes on disk until the user
      explicitly executes a previewed plan. **Implemented PR #6;
      proven on device (Stage A dry-run file preservation).**
- [x] Preview shows every planned op AND every skipped row with its
      reason (collision class, empty result, symlink, non-file) —
      nothing is silently not renamed. **Implemented PR #6
      (`skippedRowsFor` + preview list); three collision classes
      render distinctly (see §11).**
- [x] Manifest is honest: `planned` / `renamed` / `skipped` per PR #1
      semantics, written after the execute decision, never before.
      **Implemented PR #6 (six-column Dart form) + PR #7
      (post-rename `content_uri`). Note: the manifest contains
      planned ops only — planning-skipped rows appear in the
      preview and the post-run report, not as manifest rows.**
- [x] Undo is reverse-order and refuses non-`renamed` rows, per PR #1.
      **Implemented PR #6 (`undoRows` + `isRestorable`) with
      PR #7's candidate-URI fallback (manifest `content_uri`
      first, reconstructed path second).**
- [x] First real-folder run is a Glenn-supervised smoke on a
      copied/quarantined DCIM folder — the way the original Python
      script was first tested — never on live DCIM first.
      **Done: stages (a)/(b) green on B160V (see §8); stage (c)
      live DCIM never run.**

## 2. Non-goals

- Rename templates / date-based naming. The v1 rename is prefix
  strip only (`IMG_` / `VID_`, leading-only, case-sensitive default).
  Templates are a later decision, not a quiet addition here.
- Any edits (Phase 3: date-shift, GPS-strip, orientation-fix, crop,
  resize/compress). None of those exist in this slice.
- Batch presets / saved rename profiles.
- Recursive descent. `buildPlan` defaults to non-recursive; the
  Flutter settings screen had a recursive toggle — v1 renames the
  picked folder's top level only, matching `SafDiscovery`'s boundary
  (S3). Recursive rename is a later decision (see Q5).
- A MediaStore fallback rename path. If the chosen rename mechanism
  fails on a file, that file is reported as an error/skipped row —
  the app does not silently switch mechanisms mid-batch.
- Viewer (S5), parity runs (S6). This slice is the renamer only.

## 3. Dependencies (read before the work — these gate the start)

- FACT: **S2 is a hard prerequisite — it does not exist yet.**
  REPO EVIDENCE: `native/vision-core/src/main/kotlin/com/visionengine/core/`
  contains 14 files (models, ordering, cache, thumbnails, metadata)
  and none of them is rename; grep for `rename` across `native/`
  finds only a mention in `LibraryTest.kt`. The `vision-core`
  rename package (strip + `buildPlan` + collisions + manifest +
  undo, golden-replayed against the Dart goldens and Python parity)
  is S2's deliverable. S4 implementation MUST NOT start until S2's
  package is on `main` with its gates green. S4 may be drafted (this
  file) while S2 is in flight; it may not be built on a rename
  package that does not exist yet.
  **Met 2026-11-16 (PR #4, `4ef05e7`):** `vision-core/rename/`
  (strip, plan, manifest, execution order; 38 tests incl. golden
  replay) on main, host gate green.
- FACT: **S3 is a hard prerequisite.** The folder comes from S3's
  saved grants; the Renamer screen has no folder picker of its own.
  If S3 has not landed, S4 has no folder source.
  **Met 2026-11-16 (PR #5, `2223beb`).**
- FACT: **S1 (MP6 device re-run) is a sequencing prerequisite.**
  The pipeline S4 executes against must have its device re-run
  complete first — the slice order is S1 → S2 → S3 → S4 for a reason,
  and S4's device smoke (§8) assumes the pipeline underneath it is
  already device-proved.
  **Met 2026-11-17:** the MP6 suite ran 13/13 PASS on BLU B160V
  (hardware substitution for the Moto G; see S1 doc §13), and S1
  was ACCEPTED the same day for the tested BLU B160V hardware
  scope.
- RECOMMENDATION: if S2 or S3 slips, S4 waits. Do not stub a rename
  engine inside the app module "temporarily" — a temporary engine
  becomes the drift the canonical rule exists to prevent (see §4).

## 4. Facts verified in code (grounding)

### 4.1 The canonical rename semantics (what S2 ports, S4 executes)

- FACT: `rename_camera_prefixes.py` is CANONICAL — port faithfully,
  don't drift. REPO EVIDENCE: `AGENTS.md` ("CANONICAL rename logic
  (port faithfully, don't drift)") and `RULES.md` (canonical law).
  Semantics, all VERIFIED in `rename_camera_prefixes.py` (467 lines,
  main `d103ef6` incl. PR #1):
  - Leading-only, case-sensitive `IMG_` / `VID_` strip (default);
    `MY_IMG_foo.jpg` and lowercase `img_foo.jpg` must NOT match.
  - Deterministic sort; three collision classes — target exists
    (pre-existing file), target exists (staying file), duplicate
    target within the batch — each recorded as a skip, never an
    overwrite.
  - Empty strip result (a file named exactly `IMG_`) is skipped.
  - Symlinks are skipped, never renamed (PR #1: `is_symlink()`
    checked first, counted as `skipped_symlink`).
  - Chain renames resolve (`IMG_VID_a.jpg` + `VID_a.jpg` in one
    batch) via dependency ordering (PR #1 `_execution_order()`).
  - Exit codes unified: 2 on any collision / skip / error, 0 only
    when `renamed == to_rename`.
- FACT: the manifest is honest since PR #1. REPO EVIDENCE:
  `rename_camera_prefixes.py:70` (5 columns: `original_name`,
  `new_name`, `original_path`, `new_path`, `status`),
  `:266-292` (`write_manifest`: `planned` on a dry run, `renamed`
  only for ops that really executed, `skipped` for planned ops that
  did not run; "written AFTER the dry-run/execute decision"),
  `:294-310` (`undo_from_manifest`: reverse order; rows whose status
  is not `renamed` are refused with a `SKIP (not a completed rename)`
  error per row — a dry-run manifest can never undo renames that
  never happened).
  **Updated by PR #8 (2026-11-17, `18c45c6`):** Python undo is no
  longer plain reverse order — it is dependency-aware
  (pending-map + vacated-set), so chain undos restore both rows.
  The line citations above still hold on main `18c45c6`.
- FACT: the Dart port's `buildPlan` is behavior-identical to
  `build_plan` and is golden-tested. REPO EVIDENCE:
  `packages/rename_core/lib/src/plan.dart` (sort by name — or by path
  if recursive; staying-paths + seen-destinations collision sets;
  planned sorted by name then path),
  `packages/rename_core/lib/src/manifest.dart` (manifest codec),
  `packages/rename_core/test/` (`plan_test.dart`,
  `manifest_test.dart`, `strip_test.dart`, `golden_test.dart` +
  `golden/` fixtures). The golden fixtures are the contract S2
  replays in Kotlin (migration blueprint §2, §10); S4's tests reuse
  the same fixtures, never invent new rename semantics.
- FACT: the Dart manifest carries one column the Python manifest
  does not. REPO EVIDENCE: `packages/rename_core/lib/src/manifest.dart`
  — six columns, adding `content_uri`. For a SAF rename, identity is
  the document URI, not a filesystem path. RECOMMENDATION: the
  native manifest uses the six-column Dart form (with `content_uri`),
  not the five-column Python form — and the PR body records that
  choice explicitly so the Python/Dart/Kotlin manifest forms stop
  drifting silently (see Q4).

### 4.2 The Flutter Rename tab (reference behavior — and one defect NOT to port)

- FACT: the Flutter Rename tab works only in the app-private
  `vision_demo/` folder with seeded fake files. REPO EVIDENCE:
  `app/lib/features/rename/rename_screen.dart:28-48` (`_seed` writes
  `IMG_demo_a.jpg`, `VID_demo_b.mp4`, `demo_keep.jpg` into
  `vision_demo`), `:50-60` (Preview), `:62-92` (Execute/Undo).
  The screen's flow — Seed → Preview → Execute → Undo, log line +
  planned list + orange SKIP lines — is the UX shape S4 re-implements
  against a real folder. "Seed demo files" does NOT come across:
  there is no seeding of a real user folder, ever.
- FACT: the Flutter screen writes the manifest with a hardcoded
  dishonest status. REPO EVIDENCE:
  `app/lib/features/rename/rename_screen.dart:76-81`: the manifest
  is written as `renames_<epochMillis>.csv` **inside the folder
  being renamed**, via `manifestToCsv(plan.planned, status: 'renamed')`
  — every planned row claims `renamed` whether or not its execute
  succeeded. That is the exact lie PR #1 fixed in Python (see §4.1)
  and it was never fixed in the Flutter screen. S4 ports PR #1's
  honest statuses, not the Flutter screen's call. Do not copy line
  80's `status: 'renamed'` into Kotlin.
- FACT: the Flutter screen writes the manifest into the folder being
  renamed. RECOMMENDATION: the native screen does not. A manifest
  dropped into the user's camera folder pollutes the folder it is
  auditing and shows up in the next enumeration. See §5 step 6 and
  Q3 for the placement decision.

### 4.3 The D2 evidence (what is proved, what is not)

- FACT: D2 proved raw rename works in a user-picked shared folder on
  Moto G 2025 — app-created probe files, 2/2 renamed, 2/2 undo,
  cleaned. REPO EVIDENCE: `blueprints/CURRENT_STATE.md:68,82` (K1
  closed 2026-09-15) and `app/lib/adapters/storage_probe.dart:143-203`
  (`RAW_RENAME: WORKS here`, `SHARED_PROBE: COMPLETE`).
- FACT: D2 does NOT prove renaming pre-existing third-party
  (camera-created) media. REPO EVIDENCE:
  `docs/android-metadata-matrix.md:25-33` ("D2 does NOT prove rename
  of pre-existing third-party (camera-created) media. That is G5
  territory — never assume, only probe.") This is why S4's smoke is
  staged (§5 step 9, §8): app-created files first, then COPIES of
  camera-created files, live DCIM never first and never without
  Glenn supervising.
- FACT: the origin story constrains the first real run. Glenn's own
  words, 2026-11-14: he wrote the script, "tested it on a
  copied/quarantined DCIM folder", it worked, then wrote the second
  script. S4's first real-folder run repeats that exact discipline
  with the native screen instead of the script.

### 4.4 The native shell today (what S4 builds on)

- FACT: the native app is a placeholder shell — two `Text` lines,
  zero permissions, no navigation, no rename, no viewer, no folder
  picker. REPO EVIDENCE:
  `native/vision-app/src/main/kotlin/com/visionengine/app/AppRoot.kt`
  (`AppRoot` renders `state.appName` + "Native foundation ready"),
  `MainViewModel.kt` (`AppUiState(appName, ready)`),
  `MainActivity.kt` (`setContent { AppRoot() }`).
  Compose is already wired: REPO EVIDENCE:
  `native/vision-app/build.gradle.kts` (Compose BOM 2024.09.00,
  activity-compose 1.10.1, lifecycle-viewmodel-compose 2.8.7,
  material3, ui-test-junit4). S4 adds screens to this shell; it does
  not re-scaffold it.
  **Superseded 2026-11-17 (PR #5 + PR #6):** the shell is now a
  two-tab shell (Folders | Renamer) with the folder list and the
  Renamer screen; the two-`Text` placeholder survives only as the
  header lines above the tabs.
- FACT: minSdk 26, compileSdk 37, targetSdk 36. REPO EVIDENCE:
  `native/vision-app/build.gradle.kts`. The Renamer screen must work
  at minSdk 26 — any API newer than 26 needs a version guard, not a
  minSdk bump (a bump needs its own justification).

## 5. Step-by-step work

1. **Screen + navigation.** Add a Renamer destination to `AppRoot`
   (RECOMMENDATION: bottom navigation or a simple two-tab shell —
   Renamer now, Viewer in S5; keep the navigation shape minimal and
   let S5 extend it rather than building a full tab bar for a
   one-screen app). The screen's state lives in a `RenamerViewModel`
   (or an extension of `MainViewModel` — implementation's choice,
   documented in the PR): selected grant, discovered entries, plan,
   execution result, manifest reference, undo state.
2. **Pick folder (S3).** The folder comes from S3's saved-grant list.
   The screen shows the grant's display name and health; if the
   grant is lost, the screen offers S3's re-pick flow and blocks
   Preview until a healthy grant is selected. The screen never
   launches its own picker — one picker, owned by S3.
3. **Discover.** Enumerate via `SafDiscovery` (top-level only, one
   query). Show scanned / found-photo / found-video / unsupported /
   skipped-dirs / warnings counts before Preview — the user sees
   what the app sees before any plan exists. Discovery failure
   (warnings-only outcome) blocks Preview with the warning text, not
   a silent empty list.
4. **Preview (dry-run, default and only mode at this step).** Build
   the plan through S2's `vision-core` rename package — the screen
   calls the package, it does not re-implement strip/collisions.
   The preview lists, per row: old name → new name for every planned
   op; and every skipped row with its reason in plain words
   ("target already exists", "duplicate target in this batch",
   "empty result", "symlink — skipped", "not a file").
   Counts line up in the header: scanned / to-rename / clean /
   collisions / skipped. Execute is disabled until a preview has
   run; re-preview after any folder change.
5. **Execute.** Explicit confirm: the Execute button reads
   "Execute N renames" (N = planned count) and is only enabled with
   a fresh preview. Execution runs on an IO dispatcher, one op at a
   time, in S2's dependency order (chains resolve — PR #1's
   `_execution_order` semantics, ported by S2, exercised by S4's
   chain fixture test in §6). Each op records renamed / skipped /
   error as it lands; a failure on one file never aborts the batch
   silently — it is counted, listed, and the run continues. Never
   overwrite: a destination that appears mid-batch is a skip, not a
   replace. No silent failures: any op that reports success but
   cannot be verified (see Q2) is reported as unverified, not as
   renamed.
6. **Manifest.** Written AFTER the execute decision, with honest
   statuses per PR #1 (`planned` / `renamed` / `skipped`), six
   columns (Dart form, `content_uri` included — §4.1). RECOMMENDATION:
   the manifest lives in app-private storage, NOT in the renamed
   folder (the Flutter screen's in-folder placement is a defect, see
   §4.2). Filename: `renames_<epochMillis>.csv`, keeping the Flutter
   screen's naming shape so manifests stay recognizable across
   stacks. The post-run report shows the manifest's location and
   offers it for viewing/sharing (see Q3 — whether the user can pick
   a custom location is Glenn's call). The manifest is also the undo
   source: no manifest, no undo button.
7. **Undo.** Reads the manifest, runs in reverse order, refuses any
   row whose status is not `renamed` (PR #1 rule, ported by S2's undo
   and enforced again at the screen layer — a dry-run manifest must
   never restore renames that never happened). Undo reports
   restored / skipped / errors, and its own result line. Undo after a
   partially-failed execute restores only what really renamed.
8. **Post-run report.** One screen state after execute: renamed X/Y,
   skipped (with reasons), errors (with file + message), manifest
   path, "Undo" available. The report is copyable text (same pattern
   as the D2 probe report's "Copy report" button) so Glenn can paste
   it back — device evidence for this slice is a pasted report (§8).
9. **First real-folder run — staged, supervised.** (a) App-created
   files in a throwaway folder (the S3 test folder, reused):
   create `IMG_smoke_a.jpg` / `VID_smoke_b.mp4` style files, preview,
   execute, undo, verify. (b) COPIES of real camera files in a
   copied/quarantined DCIM folder (Glenn's original method):
   same cycle, verifying mtime/byte-identity is untouched apart
   from the name. (c) Live DCIM only after (a) and (b) are green
   AND Glenn explicitly approves — never as part of this slice's
   own gates. Each stage is a separate device run with its own
   pasted report.
10. **No-INTERNET / permission-diff gates.** Same as S3 §7: manifest
    gains no permission in this slice; run the diff + grep gates.
    The screen adds no new dependency (Compose + S2 + S3 only).

## 6. Acceptance criteria checklist

- [x] Renamer screen exists in `vision-app` and is reachable from
      the app shell; the placeholder two-`Text` shell is gone from
      the user path (kept only if a screen is genuinely absent).
      **Done PR #6: two-tab shell (Folders | Renamer); the header
      lines remain above the tabs.**
- [x] Folder selection comes only from S3's saved grants; a lost
      grant blocks Preview and offers re-pick; no folder → calm
      empty state, not an error. **Done PR #6 in code
      (`RenamerViewModel` lost-grant flag, no discovery); the
      lost-grant path itself is NOT device-verified (S3 revoke
      run open — see S3 doc §8).**
- [x] Preview is dry-run: after Preview, zero files on disk have
      changed (asserted in UI + device tests — compare directory
      listing before/after). **Done PR #6; proven on device
      (Stage A dry-run file preservation — hashes unchanged).**
- [x] Preview lists every planned op (old → new) and every skipped
      row with its reason; counts reconcile: planned + skipped +
      clean + dirs == scanned. **Done PR #6 (`skippedRowsFor` +
      `countsReconcile`); Stage A preview counts reported.**
- [x] The three collision classes each render distinctly
      ("target already exists", "target is a file staying put",
      "duplicate target in this batch") with the file names attached.
      **Done PR #6 (collision fix `3cba024` + test
      `collisionClassesRenderDistinctly`).**
- [x] A chain fixture (`IMG_VID_a.jpg` + `VID_a.jpg` in one batch)
      previews and executes to the correct final names — no
      half-failed chain with a success report (the PR #1 bug, now a
      regression test). **Done: S2 `ExecutionOrderTest` + S4
      `RenameExecutionTest` chain cases; Python chain execution
      covered by PR #1/PR #8 tests.**
- [ ] A symlink fixture is skipped, never renamed (PR #1 rule).
      INFERENCE, not FACT: whether a symlink is even visible through
      SAF enumeration is unproven — the test asserts the behavior on
      the path the rename actually takes (see Q2), and the spec
      records the outcome either way.
      **Decided, not ticked (Q6, PR #6): symlinks are undetectable
      through SAF — `SafDiscovery` exposes no symlink flag, so
      `skippedSymlink` stays 0 on the SAF path. Recorded limitation,
      not a passing test.**
- [x] Execute is disabled until a preview exists; the Execute button
      reads "Execute N renames" with the live planned count.
      **Done PR #6 (`RenamerScreen.kt`: "Execute " + count +
      " renames", enabled only on a fresh preview).**
- [x] Execute never overwrites: a destination appearing mid-batch is
      skipped and reported, not replaced. **Done PR #6 + PR #7
      (per-op destination preflight before every op).**
- [x] A mid-batch failure on one file is counted and listed; the
      batch continues; the final report never claims a higher
      renamed count than files actually renamed. **Done PR #6
      (`executePlan` per-op results; `RenameExecutionTest`
      partial-batch case).**
- [x] Manifest is written after the execute decision with honest
      statuses; a dry-run preview produces either no manifest or a
      manifest whose rows are all `planned` — never `renamed`.
      **Done PR #6 + PR #7 (post-rename `content_uri`). Note:
      the manifest contains planned ops only.**
- [x] Manifest has the six Dart columns including `content_uri`,
      and lives in app-private storage, not in the renamed folder.
      **Done PR #6 (`filesDir/renames_<epochMillis>.csv`) + PR #7.**
- [x] Undo restores in reverse order; rows with status other than
      `renamed` are refused with a per-row SKIP note; undo of a
      dry-run manifest restores zero files. **Done PR #6 + PR #7
      (candidate-URI fallback); proven on device (S4B/S4C Undo
      passed, files restored with identical SHA-256).**
- [x] Undo after a partially-failed execute restores exactly the
      files that really renamed — no more, no fewer. **Done PR #6
      (`undoRows` + `isRestorable`); host-tested.**
- [x] Post-run report shows renamed/skipped/errors + manifest
      location + Undo availability, and is copyable as text.
      **Done PR #6 (`buildExecuteReportText` /
      `buildUndoReportText`, Copy report button).**
- [x] Discovery warnings (provider failure, virtual documents,
      unsupported rows) surface on screen; the screen never shows an
      empty preview for a folder that failed to enumerate.
      **Done PR #6 (discovery counts + warnings on screen;
      warnings-only outcome blocks Preview).**
- [ ] The screen works offline (airplane mode) — no network exists in
      this app and this slice adds none. **Not device-verified: no
      airplane-mode run was reported. The app has no network
      (grep gate, no INTERNET permission) — that part is verified;
      the airplane-mode run itself is open.**
- [x] No new dependency beyond S2 + S3; manifest permission list
      unchanged (diff gate, §7); no INTERNET (grep gate, §7).
      **Done PR #6: no new deps, both manifests unchanged, no
      INTERNET.**

## 7. Gates

- Host: `./gradlew :vision-app:testDebugUnitTest
  :vision-android:testDebugUnitTest :vision-core:test` — all green;
  S2's golden replay stays green (S4 does not weaken S2's tests).
- Host: Compose UI tests for the screen states (empty, grant-lost,
  preview with collisions, post-run report, undo) built from the
  golden fixtures — the fixtures are the same files the Dart
  `golden_test.dart` uses, reused byte-identical (migration
  blueprint §1.F, §10).
- Host: Android Lint on `:vision-app` — no new warnings.
- Host: manifest permission diff + no-INTERNET grep gate (as S3 §7).
- Device: §8 staged smoke run (a) complete with a pasted report.
  Run (b) as specified (copied/quarantined DCIM) is NOT complete:
  S4B (zero-byte synthetics) and S4C (real JPEG + screen-recording
  MP4 in `Pictures/VE_TEST_S4C`) passed Rename → Undo with
  unchanged hashes, but no copied DCIM directory was tested, so
  the original stage-(b) requirement remains UNVERIFIED unless
  the operator authorizes a revised acceptance criterion.
  Stage (c) — live DCIM — is explicitly NOT a gate for this slice;
  it is a separate Glenn-approved event afterwards.
- Human gate: Glenn reviews the PR diff and the pasted device
  reports; the Q1/Q2 answers are recorded in the PR body.

## 8. Device evidence required (Moto G — the gate)

**Evidence recorded (2026-11-17, BLU B160V, Android 14 — hardware
substitution for the Moto G named above; no Moto G coverage
claimed; PR #7 head `4c444db`, also recorded as a comment on
PR #7):**

**Stage (a) — throwaway folder, app-created files.** `Pictures/
VE_TEST_S4`, seeded `IMG_smoke_a.jpg`, `VID_smoke_b.mp4`,
`keep.jpg`, plus pre-existing `smoke_a.jpg` as a deliberate
collision. **Result:** grant + discovery PASS, preview + collision
planning PASS, dry-run file preservation PASS (hashes unchanged),
execution report accurate. **Execute BLOCKED on the first build
(`5ea6c2c`):** `destinationExists` queried the not-yet-existing
child URI and ExternalStorageProvider threw
`FileNotFoundException` → op SKIPPED "could not check destination",
0 renamed. Manifest honestly recorded the VID row `skipped`.
**Fixed by PR #7; re-run as S4B below.**

**Stage (b) — copied/quarantined DCIM — NOT TESTED as specified.**
No copied DCIM directory was run. What ran instead is recorded
here under its actual scope. **S4B:** zero-byte synthetic files in
a fresh folder; rename/Undo PASSED, all four original files
restored with identical SHA-256 hashes. **S4C:** a real nonempty
JPEG and a screen-recording MP4 copied into
`Pictures/VE_TEST_S4C`; rename/Undo PASSED with unchanged hashes;
the 41.5 MB MP4 preserved byte-for-byte. SAF grants survived
reboot + same-signature reinstall; app-private manifests survived
reinstall; post-rename SAF document URI verified; no live DCIM
touched. The original copied-DCIM requirement remains UNVERIFIED
unless the operator authorizes a revised acceptance criterion.

**Stage (c) — live DCIM.** **NOT RUN.** Requires (a) and (b) green
plus Glenn's explicit go-ahead in chat. When it happens, it follows
the same report shape, and the first live run is one folder,
watched, with the manifest in hand before Undo is even discussed.

Same runbook style as D2 (`docs/device-probe-moto-g.md`): USB
debugging, debug run from Android Studio, paste the report back.

**Stage (a) — throwaway folder, app-created files.** Reuse the S3
test folder or a fresh `Pictures/VE_TEST_S4`. Seed it with
app-created files (`IMG_smoke_a.jpg`, `VID_smoke_b.mp4`,
`keep.jpg`, plus one deliberate collision: a pre-existing
`smoke_a.jpg` so one op must skip). Paste: Android version, the
preview counts, the post-run report (renamed/skipped/errors),
the manifest contents (or its location), the undo report, and a
before/after listing proving Preview changed nothing.

**Stage (b) — copied/quarantined DCIM.** Glenn copies a real DCIM
folder to a quarantine location on the phone (or the operator does
it on the laptop and pushes it). Same cycle: preview → execute →
undo → verify. Paste: the same report, plus confirmation that
file bytes and mtimes are unchanged apart from names (the
untouched-bytes proof pattern from MP6), and the name of the
camera app / Android version the copied files came from.

**Stage (c) — live DCIM.** NOT part of this slice. Requires (a)
and (b) green plus Glenn's explicit go-ahead in chat. When it
happens, it follows the same report shape, and the first live run
is one folder, watched, with the manifest in hand before Undo is
even discussed.

Also paste for every stage: which consent dialogs appeared and when
(SAF grant, any rename/consent prompt the mechanism surfaces), and
any provider variance (a file that renamed differently than
previewed, a `renameDocument` refusal, an unverifiable op).

## 9. Risks

- R-01 (migration blueprint §14): SAF provider variance — the
  rename mechanism may behave differently per provider (refusals,
  silent no-ops, renames that report success but did not happen).
  Mitigation: verify-after-write where the mechanism allows it
  (see Q2), report unverified ops honestly, per-provider notes in
  the PR (Tagger quarantine-note pattern).
- G5 scope risk (from `docs/android-metadata-matrix.md`): renaming
  pre-existing third-party camera files is unproven territory. The
  staged smoke (§5 step 9) exists precisely because a green stage
  (a) says nothing about stage (b). If stage (b) finds a class of
  file that will not rename, that is a finding to report and a
  reason to stop — not something to work around quietly.
- The rename mechanism fork (Q1/Q2) is the biggest schedule risk in
  the slice: if the mechanism chosen from the SAF tree URI cannot be
  proven on Moto G, S4 waits for a new probe rather than shipping a
  rename that works only on the happy path.
- Manifest placement in app-private storage means a user who clears
  app data loses undo history. Mitigation: the post-run report says
  so plainly, and the manifest is viewable/sharable from the report
  screen (see Q3). Do not silently promise durable undo.
- Preview/execute skew: the folder's contents change between Preview
  and Execute (user or another app renames a file in between).
  Mitigation: execute re-checks each destination immediately before
  its op (the S2 plan is a plan, not a lock); a destination that
  appeared since preview is a skip, reported as such.

## 10. Open questions (for Glenn — NOT decided here)

- Q1 — **Rename mechanism: `DocumentsContract.renameDocument` or raw
  `File.rename` from a SAF tree?** D2 proved raw rename from a
  `file_picker` path. A SAF tree URI does not reliably yield a
  filesystem path, so raw rename may not be available at all from
  S3's grants. `renameDocument` is the SAF-native call but is
  provider-dependent (some providers refuse it, and its failure
  modes need device evidence). This is the decision S3's Q1/Q2 and
  S4's Q1 are the same fork, stated twice because both slices need
  the answer. FACT: neither mechanism is proven from a SAF tree URI
  on this device yet.
  **Decided 2026-11-15 (Glenn's READ+WRITE grant choice) + PR #6:**
  `renameDocument`. Proven on B160V (S4B/S4C); other providers
  unprobed.
- Q2 — **Verify-after-write: how do we know a rename really
  happened?** Options: re-query the document URI after the op and
  compare display names; re-enumerate the folder and diff; trust
  the call's return value. RECOMMENDATION: re-query the single
  document (cheap, precise) where the mechanism allows it, and mark
  any op that cannot be verified as "unverified" in the report
  rather than counting it renamed. If re-query is not possible on
  some provider, that provider gets a quarantine note, not a
  silent pass.
  **Decided (spec recommendation adopted in PR #6):** re-query the
  document after the op; null or name mismatch → UNVERIFIED, never
  counted renamed (`executePlan`, `undoRows`).
- Q3 — **Manifest placement: app-private only, or also offer export?**
  RECOMMENDATION: app-private by default (never pollute the user's
  folder), with a Share/Export action on the post-run report so the
  user can copy the manifest out. Alternative: ask the user at
  execute time where the manifest goes. The Flutter screen's
  in-folder default is NOT an option — it is recorded here as a
  defect, not a candidate.
  **Decided (spec recommendation adopted in PR #6):** app-private
  (`filesDir/renames_<epochMillis>.csv`) + `CreateDocument`
  export ("Export manifest" button). The report states plainly
  that clearing app data loses undo history.
- Q4 — **Manifest columns: six-column Dart form (with `content_uri`)
  as the native standard?** RECOMMENDATION: yes (§4.1) — SAF
  identity is the document URI, and the Python five-column form
  cannot name a SAF document. Consequence to note: the three stacks
  then have two manifest shapes (Python 5, Dart/Kotlin 6), and any
  future Python `--undo` of a Kotlin manifest must handle the extra
  column (or explicitly refuse it). Flag if that consequence is
  unacceptable.
  **Decided (spec recommendation adopted in PR #6, implemented
  as-recommended):** six-column Dart form, `content_uri` carried
  through. Formal confirmation by Glenn is still open (see
  README §5 D4) — the implementation follows the recommendation;
  the decision is recorded as implemented-as-recommended, not
  formally confirmed.
- Q5 — **Recursive rename: ever, or never?** The Flutter settings
  had a recursive toggle and `buildPlan` accepts `recursive: true`,
  but S3's enumerator and S4's v1 are top-level only. RECOMMENDATION:
  top-level only for v1; recursive rename is a separate spec with
  its own safety review (a recursive rename that hits a nested
  collision mid-tree is a worse failure mode than a flat one).
  **Decided (spec recommendation adopted in PR #6):** top-level
  only for v1.
- Q6 — **Symlinks through SAF: visible at all?** `SafDiscovery`
  has no symlink concept (DocumentsContract exposes no symlink
  flag), and the Python `is_symlink` skip cannot be expressed
  against a document URI directly. If a provider surfaces a symlink
  as a regular document, the rename may follow it. RECOMMENDATION:
  the S4 device smoke includes a symlink probe if the test folder
  can contain one (likely only on the raw-path lane, not SAF) and
  records the outcome; if it cannot be probed, the limitation is
  stated in the PR rather than assumed away.
  **Decided (limitation stated, PR #6):** symlinks are undetectable
  through SAF; `skippedSymlink` stays 0 on that path. Recorded
  limitation, not probed.
- Q7 — **Execute confirmation: is the "Execute N renames" button
  enough, or is a second confirm dialog wanted?** The Flutter
  screen executed on one tap after preview. RECOMMENDATION: the
  button label carrying the live count plus the preview already on
  screen is the confirmation — a second dialog adds friction to an
  already deliberate two-step flow. Flag if a second confirm is
  preferred.
  **Decided (spec recommendation adopted in PR #6):** no second
  dialog; the "Execute N renames" button (enabled only on a fresh
  preview) is the confirmation.

## 11. Implementation record (PR #6 + PR #7)

- **PR #6 (merged 2026-11-17, main `5ea6c2c`):**
  `RenameExecution.kt` (pure engine: `executePlan` / `undoRows` /
  counts / report text), `SafRenameBackend.kt`
  (`renameDocument`, re-query display name, destination-exists
  query), `RenamerViewModel.kt` (preview / execute / manifest /
  undo / export), `RenamerScreen.kt`, two-tab `AppRoot.kt`.
  Tests: `RenameExecutionTest.kt` (16 + collision test),
  `RenamerViewModelTest.kt`, `RenamerScreenTest.kt`,
  `FakeRenameBackend.kt`, `SafTestFixtures.kt`. Host gate BUILD
  SUCCESSFUL in 1m 40s on the laptop (four tasks green); two
  test-only fixes on the branch (`f8bfce1` void-return,
  `550432f` scroll-into-view).
- **PR #7 S4-FIX-01 (merged 2026-11-17, main `b0d2b36`):**
  destination preflight via parent enumeration
  (`buildChildDocumentsUriUsingTree`, three outcomes — present →
  skip, clean absence → proceed, indeterminate → throw);
  post-rename URI preserved into the manifest `content_uri`;
  undo tries the manifest URI first, reconstructed path second.
  `SafRenameBackendTest.kt` (6 Robolectric tests via a fake
  `ContentProvider` registered with
  `ShadowContentResolver.registerProviderInternal` — note:
  `registerProvider` does NOT resolve in Robolectric 4.17).
  Host gate green; S4B/S4C device evidence in §8.
- **Deviations from this spec (recorded in the PR #6 body):**
  (1) collision checks 1+2 originally rendered identically —
  **fixed on the branch** (`3cba024`: "target is a file staying
  put" / "target already exists" / "duplicate target in this
  batch"); (2) export is `CreateDocument` (no FileProvider, no
  manifest change); (3) undo URI reconstruction — **fixed by
  PR #7** (post-rename URI preserved); (4) `skippedRowsFor`
  parses S2's `"<name> -> <stripped> (<reason>)"` format — a
  display name containing `" -> "` mis-parses the name (row still
  shown, generic reason fallback).
- **Manifest note:** the manifest contains planned ops only
  (`RenamerViewModel.kt:271` calls `manifestToCsv` on
  `report.planned`). Planning-skipped rows (e.g. the IMG collision
  in Stage A) appear in the Preview and in the post-run report
  header (`collisions=1`), not as named manifest rows.
- **Device evidence:** see §8. Stage (c) live DCIM NOT run.
