# S2 — Native rename port to `vision-core` (spec)

> Status: SPEC for operator review. Authorizes NOTHING to build.
> Spec-only: this file describes the port; no `.kt` file is written by
> this slice. Implementation is a later PR, feature by feature.

Labels used throughout: **FACT** (verified in repo/docs), **REPO
EVIDENCE** (file:line or gate result), **RECOMMENDATION** (judgment),
**INFERENCE** (strong but unverified), **OPEN QUESTION** (needs Glenn).
Assumptions are never presented as facts.

## 1. Goal

Port the rename contract — `stripLeadingPrefix` + `buildPlan` +
manifest CSV codec + reverse-order undo — from Python/Dart into
`vision-core` as pure JVM Kotlin, behavior-identical to the current
Python semantics (post-PR #1), proven by replaying the same 6 Dart
golden fixtures byte-identical.

- **FACT:** `vision-core` today has no rename package at all.
- **REPO EVIDENCE:** `native/vision-core/src/main/kotlin/com/visionengine/core/`
  contains exactly: `Cache.kt`, `ExtractStatus.kt`, `Fields.kt`,
  `MediaKind.kt`, `MediaRecord.kt`, `MediaSource.kt`, `MetaField.kt`,
  `MimeTypes.kt`, `Ordering.kt`, `PhotoMeta.kt`, `Provenance.kt`,
  `StatusRules.kt`, `ThumbInfo.kt`, `ThumbKeys.kt`, `VideoMeta.kt`
  (plus a `.gitkeep`). No file mentions rename, prefix, plan, or
  manifest.
- **FACT:** The only "rename" string anywhere in `vision-core`
  tests is a display name, not rename logic.
- **REPO EVIDENCE:** `grep -rn -i rename native/vision-core/src/test/`
  returns one hit — `LibraryTest.kt:44`
  (`displayName = "a-renamed.jpg"`, a dedupe test fixture). It proves
  nothing about rename behavior.
- **FACT:** The rename contract lives in two places today: the Python
  canonical script and the Dart port, and the migration blueprint
  already assigns the Kotlin destination.
- **REPO EVIDENCE:** `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`
  §2 responsibility matrix: "rename plan/collisions/manifest |
  rename_core (Dart) | `vision-core` rename package (JVM) | 6 goldens
  replayed byte-identical + parity_probe untouched | JUnit golden
  runner".

## 2. Non-goals (hard boundary for this slice)

- **NO UI.** No Renamer screen, no Compose, no preview list. (That is
  S4.)
- **NO SAF picker, NO persisted grants.** No folder picking, no
  `DocumentsContract`, no URI handling. (That is S3.)
- **NO execution against real folders.** No `java.io.File.renameTo`,
  no filesystem writes, no MediaStore calls. The port implements
  plan/manifest/undo as pure functions over supplied entries; wiring
  them to a real directory is S4.
- **NO CLI / exit-code layer.** The Python script's exit codes (0/1/2)
  belong to its `main()`; a pure JVM library has no exit codes.
- **NO new dependencies.** Zero new Gradle dependencies (see §7).
- **NO changes to the Python script, the Dart package, or
  `tools/parity_probe.py`.** This slice adds Kotlin; it does not edit
  the sources it ports from. (One open question, §10.3, asks whether
  the probe should later also cover Kotlin — the answer is not
  assumed here.)

## 3. Grounding — the contract to port

### 3.1 Canonical source: Python (post-PR #1 — target these semantics)

- **FACT:** `rename_camera_prefixes.py` is CANONICAL — "port
  faithfully, don't drift."
- **REPO EVIDENCE:** `AGENTS.md` ("`rename_camera_prefixes.py`
  (387 L, archival-grade)"), `RULES.md` house law (canonical), and
  the PR #1 merge (`5f78388`, 2026-11-15) which changed the file.
  The file is now **467 lines** (verified by `wc -l`).
- **FACT:** PR #1 changed four behaviors. The Kotlin port must target
  the **current** (post-PR #1) semantics, not the pre-PR #1 ones:
  1. **Chain renames resolve** instead of half-failing with exit 0:
     `_execution_order()` orders renames so an op whose destination
     is another op's source runs after that op
     (`VID_a.jpg -> a.jpg` executes before `IMG_VID_a.jpg ->
     VID_a.jpg`). Stripped names are strictly shorter than sources,
     so no cycles can exist; leftovers are reported as cycles.
  2. **Honest manifest statuses:** `planned` on a dry run,
     `renamed` only for ops that really executed, `skipped` for
     planned ops that did not run. The manifest is written **after**
     the dry-run/execute decision, and `--undo` refuses non-`renamed`
     rows.
  3. **Symlink skip:** `is_symlink()` is checked first in
     `build_plan`; a symlink-to-file is never renamed and is counted
     as `skipped_symlink`.
  4. **Unified exit codes:** 2 on any collision/skip/error in dry-run
     AND execute alike; 0 only when `renamed == to_rename`.
- **REPO EVIDENCE:** `rename_camera_prefixes.py` — `_execution_order`
  (~line 219), `write_manifest` (~line 275), `undo_from_manifest`
  (~line 304), `build_plan` symlink check (~line 148), `main()` exit
  logic (~line 417); `tests/test_rename.py` (18 tests) pins each one:
  `test_plan_chain_lists_both`, `test_execute_chain_renames_both`,
  `test_plan_skips_symlink_to_file`, `test_dry_run_manifest_says_planned`,
  `test_execute_manifest_says_renamed_and_undo_restores`,
  `test_undo_of_dry_run_manifest_restores_nothing`,
  `test_dry_run_with_collision_returns_2`.
- **FACT:** `rename_min.py` (80 lines, post-PR #1) is the minimal
  twin with the same two-pass plan. It is reference only; the Kotlin
  port targets the full script, not the twin.

### 3.2 The Dart port (the other half of the contract)

- **REPO EVIDENCE:** `packages/rename_core/lib/src/prefix.dart`
  (24 lines, `stripLeadingPrefix`), `plan.dart` (100 lines,
  `buildPlan`), `model.dart` (64 lines, `RenameEntry`/`RenameOp`/
  `RenameReport`/`ManifestRow`), `manifest.dart` (115 lines,
  `manifestToCsv`/`parseManifestCsv`/`undoOrder`); exports in
  `packages/rename_core/lib/rename_core.dart`.
- **FACT:** Dart `buildPlan` must be (and is) behavior-identical to
  Python `build_plan`: leading-only, case-sensitive default, 3
  collision classes, deterministic sort.
- **REPO EVIDENCE:** `AGENTS.md` ("Dart `buildPlan` must be
  behavior-identical to Python `build_plan`... Golden fixtures prove
  it"); `tools/parity_probe.py` (222 lines) proves Python↔Dart
  parity over synthetic trees + SHA-256 byte proof, exit 0 = full
  parity.
- **FACT — a divergence to carry consciously, not accidentally:**
  the Dart manifest has **6 columns** (`original_name`, `new_name`,
  `original_path`, `new_path`, `content_uri`, `status`), while the
  Python manifest has **5** (same list minus `content_uri`).
- **REPO EVIDENCE:** `packages/rename_core/lib/src/manifest.dart`
  `manifestFieldnames` vs `rename_camera_prefixes.py`
  `MANIFEST_FIELDNAMES = ["original_name", "new_name",
  "original_path", "new_path", "status"]`.
  The extra Dart column exists for SAF content URIs (S3/S4
  territory). The Kotlin codec adopts the Dart 6-column form as its
  RECOMMENDATION (§6.4) — a SAF rename's identity is the document
  URI, and the Python 5-column form cannot name one (see S4 §4.1,
  §10 Q4 for the consequence: Python `--undo` of a Kotlin manifest
  must tolerate the extra column, header-driven parsing already
  does). Which form Kotlin writes was S2's open question §10.1;
  the assembler reconciled it with S4 before this PR rather than
  shipping two defaults in one spec.

### 3.3 The 6 golden fixtures (the acceptance oracle)

- **REPO EVIDENCE:** `packages/rename_core/test/golden/`:
  `leading_only.json`, `case_gate.json`, `empty_skip.json`,
  `collision_trio.json`, `sort_determinism.json`, `recursive.json`.
  Each fixture carries `prefixes`, `caseSensitive`, `recursive`,
  `entries`, `existing`, `expectedPlanned`, `expectedCollisions`,
  `expectedCounts`.
- **FACT:** The Dart golden runner (`packages/rename_core/test/golden_test.dart`)
  feeds each fixture through `buildPlan` and asserts planned ops,
  collision strings, and counts.
- **REPO EVIDENCE (worked example):** `collision_trio.json` — 7
  entries, `expectedPlanned` is exactly one op
  (`/t/IMG_B.jpg -> /t/B.jpg`), `expectedCollisions` is exactly the
  four strings `IMG_A.jpg -> A.jpg (target exists)`,
  `IMG_C.jpg -> C.jpg (target exists)`,
  `IMG_D.jpg -> D.jpg (target exists)`,
  `VID_B.jpg -> B.jpg (duplicate target in batch)`, and
  `expectedCounts` is `{scanned: 7, planned: 1, skippedNoPrefix: 2,
  skippedIsDir: 0, skippedEmptyResult: 0, collisions: 4}`.
  That single fixture pins **both** staying-put checks (K2) and the
  intra-batch check in one shot.

### 3.4 House rules this slice inherits

- **REPO EVIDENCE:** `blueprints/CURRENT_STATE.md` known-issue
  registry — K2 ("Intra-batch + staying-put collisions must BOTH be
  checked (Python does both; naive ports miss one). Pinned by BP-01
  golden") and K3 ("Case gate: `img_`/`vid_` lowercase must NOT match
  by default. Pinned by BP-01 golden").
- **REPO EVIDENCE:** `native/vision-core/src/test/kotlin/com/visionengine/core/ModuleBoundaryTest.kt`
  — `mainSourcesContainNoForbiddenImports` walks `src/main`, fails
  on any import starting `android.`, `androidx.`, `com.android.`,
  or `io.flutter.`. New rename files are automatically covered by
  this test; no separate boundary check needs writing.
- **REPO EVIDENCE:** `AGENTS.md` rule 5 — Genesis header on every
  new `.kt` file (the existing `// ====... As Above, So Below...`
  header). Every new file in this slice carries it.
- **FACT:** `vision-core` is a pure JVM module: Kotlin/JVM 2.4.0,
  JVM target 17, one test dependency (`junit:junit:4.13.2`), JUnit
  runner.
- **REPO EVIDENCE:** `native/vision-core/build.gradle.kts`.
- **FACT:** `vision-core` already has the golden-replay pattern this
  slice reuses: hand-rolled test-only JSON parser (zero new deps)
  and resource-loaded golden JSON.
- **REPO EVIDENCE:** `native/vision-core/src/test/kotlin/com/visionengine/core/GoldenReplayTest.kt`
  (`loadGolden` reads `/golden/$name.json` via
  `getResourceAsStream`; `parseJson` in `JsonParser.kt`) and
  `native/vision-core/src/test/resources/golden/` (3 existing
  goldens: `photo_exif.json`, `video_meta.json`,
  `orientation_vectors.json`).

## 4. Inputs (exact files)

| File | Role in this slice |
|---|---|
| `rename_camera_prefixes.py` (467 lines) | CANONICAL contract. Read, do not modify. |
| `tests/test_rename.py` (18 tests) | PR #1 semantics, pinned. Read, do not modify. |
| `packages/rename_core/lib/src/prefix.dart` | Dart `stripLeadingPrefix`. Read, do not modify. |
| `packages/rename_core/lib/src/plan.dart` | Dart `buildPlan`. Read, do not modify. |
| `packages/rename_core/lib/src/model.dart` | Dart models. Read, do not modify. |
| `packages/rename_core/lib/src/manifest.dart` | Dart manifest codec. Read, do not modify. |
| `packages/rename_core/test/golden/*.json` (6 files) | Golden oracle — copied byte-identical (see §6.5). |
| `packages/rename_core/test/golden_test.dart` | Dart golden-runner shape. Read, do not modify. |
| `tools/parity_probe.py` | Python↔Dart parity proof. Read, do not modify. |
| `native/vision-core/src/main/kotlin/com/visionengine/core/Ordering.kt` | Existing `planDiscovery`/`pageOf` — neighbour code; shows core naming + pure-function style. Read-only. |
| `native/vision-core/src/test/kotlin/com/visionengine/core/GoldenReplayTest.kt`, `JsonParser.kt` | Golden-runner + JSON-parser pattern to reuse. Read-only. |
| `native/vision-core/src/test/kotlin/com/visionengine/core/ModuleBoundaryTest.kt` | Boundary gate that auto-covers new files. Read-only. |
| `native/vision-core/build.gradle.kts` | Module config — must need NO change in this slice. |
| `blueprints/CURRENT_STATE.md` (K2, K3) | Pinned rename laws. |

## 5. Step-by-step work (implementation PR, not this spec)

1. **Scaffold the package.** Create
   `native/vision-core/src/main/kotlin/com/visionengine/core/rename/`
   (or flat files beside the existing ones — see §10.4) with the
   Genesis header on every new file.
2. **Port `stripLeadingPrefix`.** Pure function, signature mirroring
   Dart: filename + prefixes (default `["IMG_", "VID_"]`) +
   `caseSensitive` (default `true`). Semantics: leading position 0
   only; first matching prefix wins; return `""` when the strip
   would leave an empty name; return `null` when no prefix matched
   (§6.1).
3. **Port the models.** `RenameEntry` (path, name, isDir, isFile),
   `RenameOp` (srcPath, dstPath, `oldName`/`newName` derived),
   `RenameReport` (scanned, planned, skippedNoPrefix, skippedIsDir,
   skippedEmptyResult, skippedSymlink count field, skippedCollision
   strings, `toRename`), `ManifestRow` (see §10.1 for the column
   question). Mirror the Dart field names (camelCase) so the golden
   runner reads naturally.
4. **Port `buildPlan`.** Pure function over a supplied entry list +
   existing-paths set (Dart form, §6.2). No filesystem access inside
   `buildPlan` — that is what keeps it JVM-pure and testable.
5. **Port `_execution_order` as a pure function** (`executionOrder`
   or equivalent): takes the planned ops, returns ordered ops +
   leftover cyclic ops (§6.3). **RECOMMENDATION:** include it in
   this slice even though execution itself is S4 — it is pure logic,
   it is the PR #1 chain fix, and without it the port silently
   targets pre-PR #1 semantics. It is tested here, wired to real
   renames in S4.
6. **Port the manifest codec.** `manifestToCsv` (write), CSV parse,
   `undoOrder` (reverse) (§6.4). CSV quoting rules mirror the Dart
   codec (quote on `,`/`"`/newline; `""` escape).
7. **Copy the 6 golden fixtures byte-identical** into
   `native/vision-core/src/test/resources/golden/` (same filenames)
   and write the JUnit golden runner (§6.5).
8. **Write the unit tests** (§6.6): strip cases, collision cases,
   chain ordering, manifest round-trip + quoting, undo order,
   empty/edge cases.
9. **Run the gates** (§8). No new dependencies, no Gradle edits.
   If a gate needs a dependency, stop — that is a design change, not
   part of this slice.
10. **Do not wire anything to a UI, a picker, or a directory.**
    There must be no caller in `vision-android` or `vision-app` in
    this slice. If a caller appears, it is scope creep — remove it.

## 6. Port specification (the contract, function by function)

### 6.1 `stripLeadingPrefix`

| Aspect | Required behavior | Source |
|---|---|---|
| Match position | Position 0 only. `MY_IMG_foo.jpg` → no match. | Python `strip_leading_prefix` (`filename.startswith`); Dart `prefix.dart`; golden `leading_only` |
| Case | Case-sensitive by default: `img_foo.jpg`, `vid_foo.mp4` → no match. `caseSensitive = false` uppercases both sides before comparing and strips by prefix length. | K3; golden `case_gate`; Python `--ignore-case` |
| Prefix order | First matching prefix in the list wins. Default list `["IMG_", "VID_"]`. | Python `DEFAULT_PREFIXES`; Dart `defaultPrefixes` |
| Remainder preserved | Suffixes preserved verbatim (`_HDR`, `_BURST`, extensions). Never parse/reformat the remainder. | `AGENTS.md` gotchas |
| Empty result | Stripping `IMG_` from `IMG_` yields `""` (not `null`) — the caller must skip it as `skippedEmptyResult`. | Python (`return ""`); Dart (`return ''`); golden `empty_skip` |
| No match | Return `null`. | Python (`return None`); Dart (`return null`) |
| Return shape | `String?` with the `""` sentinel preserved. | **RECOMMENDATION** — keep the nullable+sentinel shape so the port is mechanical and the goldens transfer without translation. A sealed result type would be nicer Kotlin but is a redesign; see §10.2. |

### 6.2 `buildPlan`

Inputs (Dart form — pure, no filesystem):

- `entries: List<RenameEntry>` — caller-supplied; each entry carries
  path, name, isDir, isFile.
- `existingPaths: Set<String>` — full paths known to exist on disk,
  supplied by the caller (in Python this is a live `Path.exists()`
  check; in Dart/Kotlin it is a parameter, which is what makes the
  function pure).
- `prefixes`, `caseSensitive` (default `true`), `recursive`
  (default `false`).

Required behavior, in order:

1. **Sort entries first.** Non-recursive: by name; recursive: by
   full path. (Python: `sorted(..., key=p.name)` / `key=str(p)`;
   Dart: same. **INFERENCE:** Dart sorts by `String.compareTo`,
   Python by code-point order; for BMP filenames these agree — see
   risk §9.3.)
2. **Classify each entry, in order:** symlink → dir → non-file →
   strip. Count `scanned` for every entry.
   - `skippedSymlink`: **see §6.2 note below** — symlink detection
     is NOT inside `buildPlan` (see note).
   - Directories → `skippedIsDir`. Non-files (fifo/socket/etc. in
     Python; `isFile == false` in Dart) → `skippedNoPrefix`.
   - No prefix match → stays: record the path in `stayingPaths`,
     count `skippedNoPrefix`.
   - Empty strip result → `skippedEmptyResult`.
   - Otherwise → candidate.
3. **Collision checks, for each candidate, in this order:**
   - **Check 1 (target exists, not in batch):** destination is in
     `existingPaths` AND is not itself a candidate source →
     `"<name> -> <stripped> (target exists)"`, skip.
   - **Check 2 (staying-put):** destination is in `stayingPaths` →
     same `"(target exists)"` string, skip. (K2: this is the check
     naive ports miss.)
   - **Check 3 (intra-batch):** destination already seen from an
     earlier candidate → `"<name> -> <stripped> (duplicate target
     in batch)"`, skip.
   - Else: record destination, append `RenameOp`.
4. **Sort planned ops** by old name, tie-break by source path
   (Dart form; Python relies on a stable sort over pre-sorted
   candidates, which yields the same order — see risk §9.2).
5. **Report counts:** `scanned`, `toRename` (= planned size),
   `skippedNoPrefix`, `skippedIsDir`, `skippedEmptyResult`,
   `skippedCollision` (list of exact strings, in candidate order).

**Symlink note (important — do not get this wrong):** PR #1's
symlink fix lives in Python `build_plan` because Python enumerates
the directory itself (`entry.is_symlink()`). Dart `buildPlan` has no
symlink concept at all — it receives pre-built entries. The Kotlin
port follows the Dart shape: `buildPlan` takes entries and does NOT
detect symlinks. Symlink skipping belongs to the **enumerator**
(S3/S4), which must mark symlink entries so they never reach
`buildPlan` as files (or pass them with a flag the plan step
counts). The `skippedSymlink` counter exists on the report so the
enumerator layer can record it — but `buildPlan` itself must not
claim to handle symlinks. Any test that "proves" Kotlin `buildPlan`
skips symlinks without an enumerator is testing a fiction.

### 6.3 `executionOrder` (pure port of Python `_execution_order`)

- Input: the planned ops. Output: `(ordered, cyclic)`.
- Rule: an op whose destination is another op's source runs AFTER
  that op; within each ready wave, sort by old name.
- **FACT:** Stripped names are strictly shorter than sources, so no
  cycles can exist; anything left over is reported as `cyclic`
  rather than hung on.
- **REPO EVIDENCE:** `rename_camera_prefixes.py` `_execution_order`.
- **RECOMMENDATION:** Port it, test it (chain fixture: `IMG_VID_a.jpg`
  + `VID_a.jpg` → order is `VID_a.jpg -> a.jpg` first), but do NOT
  call it from any filesystem code in this slice. S4 owns execution.

### 6.4 Manifest codec + undo

- **Fieldnames (default):** `original_name`, `new_name`,
  `original_path`, `new_path`, `content_uri`, `status` — the Dart
  6-column form. (**RECOMMENDATION**, reconciled with S4 §4.1
  before this PR shipped — see §3.2. Python's 5-column form stays
  readable by the same codec because parsing is header-driven; the
  Python `--undo` of a Kotlin manifest must tolerate the extra
  column, see §10.1.)
- **Statuses (PR #1 honest form):** `planned` when the manifest is
  written for a dry run / unexecuted plan; `renamed` only for ops
  that really executed; `skipped` for planned ops that did not run.
  The writer must take an `executed` flag (or equivalent) and a set
  of actually-renamed ops — it must never stamp `renamed` on a
  dry-run row. (Pre-PR #1, every row said `renamed`: that was bug
  #2, and the Kotlin codec must not reintroduce it.)
- **CSV quoting:** quote a field containing `,`, `"`, `\n`, or `\r`;
  escape `"` as `""`. Mirror `manifest.dart` `_escape` /
  `_splitCsvLine` (round-trip: parse(write(x)) == x).
- **Parse:** header-driven (map by field name, not position), CRLF
  tolerant, missing fields default to `""`.
- **Undo order:** reverse the manifest rows (`undoOrder`).
- **Undo rule (PR #1):** a row is restorable ONLY if its status is
  `renamed`. Rows with `planned`/`skipped` (or any other status)
  are skipped with a recorded note, never executed. The undo
  executor itself is S4; this slice supplies the codec + the
  status gate as a pure predicate.
- **REPO EVIDENCE:** `rename_camera_prefixes.py` `write_manifest`
  ("Status is honest about what actually happened") and
  `undo_from_manifest` ("Reverse order so later renames undo first");
  `packages/rename_core/lib/src/manifest.dart`.

### 6.5 Golden replay (the acceptance oracle)

- **RECOMMENDATION:** Copy the 6 JSON fixtures from
  `packages/rename_core/test/golden/` into
  `native/vision-core/src/test/resources/golden/` **byte-identical**
  — same filenames, same bytes. Do not reformat, re-key, or "improve"
  them. (Precedent: `GoldenReplayTest` reuses Flutter fixture JSON
  byte-identical; `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`
  §1E: "golden JSON (9 files, reused byte-identical)".)
- Write a JUnit golden runner modeled on Dart `golden_test.dart` +
  Kotlin `GoldenReplayTest.kt`: load each fixture with the existing
  `loadGolden`/`parseJson` helpers, build entries + `existingPaths`,
  call `buildPlan`, assert `expectedPlanned` (src/dst pairs, in
  order), `expectedCollisions` (strings, in order), and
  `expectedCounts` (all six counters).
- **FACT:** The existing Kotlin `JsonParser` is hand-rolled and
  test-only; it already handles the JSON shapes the goldens use
  (objects, arrays, strings, booleans, numbers as `Long`).
  **INFERENCE:** No parser work should be needed, but if a golden
  uses a shape the parser rejects, extending the parser (with tests)
  is in scope for this slice — it is test-only code.
- The runner must cover all 6 fixtures: `leading_only`,
  `case_gate`, `empty_skip`, `collision_trio`, `sort_determinism`,
  `recursive`.

### 6.6 Test plan (JUnit, `vision-core` only)

| Test group | What it proves | Oracle |
|---|---|---|
| Strip unit tests | Leading-only, case gate (both defaults), empty sentinel, no-match `null`, ignore-case mode, suffix preservation (`_HDR`, `_BURST`) | Python `strip_leading_prefix` + `tests/test_rename.py::test_strip_*` |
| Plan unit tests | Dir/non-file skip, empty skip, staying-put collision, intra-batch collision, existing-target collision, deterministic sort | Goldens + `tests/test_rename.py::test_plan_*` |
| Golden runner | All 6 fixtures byte-identical replay | `packages/rename_core/test/golden/*.json` |
| Chain ordering tests | `_execution_order` port: 2-op chain, 3-op chain, ready-wave name order, no-cycle guarantee | `tests/test_rename.py::test_plan_chain_lists_both`, `test_execute_chain_renames_both` (order half only — execution itself is S4) |
| Manifest codec tests | Write/parse round-trip incl. commas/quotes/newlines in names, CRLF tolerance, header-driven parse, `planned`/`renamed`/`skipped` stamping, undo refusal of non-`renamed` rows, reverse undo order | `tests/test_rename.py::test_dry_run_manifest_says_planned`, `test_execute_manifest_says_renamed_and_undo_restores`, `test_undo_of_dry_run_manifest_restores_nothing` |
| Boundary | No `android.*`/`androidx.*` imports in new files | Existing `ModuleBoundaryTest.mainSourcesContainNoForbiddenImports` (auto-covers; no new test needed) |

- **RECOMMENDATION:** Every test uses synthetic names only
  (`IMG_...`, `VID_...`, `a.jpg` style). No real filenames, no real
  EXIF, no real media anywhere (house rule: synthetic data only).
- **FACT:** `vision-core` tests run on the JVM with JUnit 4 only;
  no Robolectric, no device, no Android SDK needed for this slice.

## 7. Acceptance criteria — checklist

- [ ] Package exists under `native/vision-core/src/main/kotlin/
      com/visionengine/core/` with the Genesis header on every new
      `.kt` file.
- [ ] `stripLeadingPrefix` matches §6.1 on all strip unit tests.
- [ ] `buildPlan` matches §6.2 on all plan unit tests, including
      BOTH collision checks (K2) and the case gate (K3).
- [ ] `executionOrder` matches §6.3 (chain fixture passes; cyclic
      leftovers reported, never hung).
- [ ] Manifest codec matches §6.4: 6-column Dart form (with
      `content_uri`), honest statuses,
      quoting round-trip, undo refuses non-`renamed` rows, reverse
      undo order.
- [ ] All 6 golden fixtures replayed byte-identical: planned ops,
      collision strings, and all six counters match the fixture
      JSON exactly.
- [ ] `tools/parity_probe.py` still passes unmodified (Python↔Dart
      parity untouched — run it as a regression check, do not edit
      it).
- [ ] `ModuleBoundaryTest` green — no forbidden imports in the new
      files.
- [ ] Zero new Gradle dependencies; `native/vision-core/build.gradle.kts`
      unchanged.
- [ ] No caller in `vision-android` or `vision-app`; no filesystem
      access, no `java.io.File` rename, no SAF/URI code in the new
      package (pure-JVM proof: the tests run on the JVM with no
      Android SDK).
- [ ] No file outside `native/vision-core/` modified (except the
      spec's own state-doc update, if any).

## 8. Gates

| Gate | Command / check | Pass condition |
|---|---|---|
| JVM tests | `:vision-core:test` (same Gradle invocation as the MP6 runbook, task `:vision-core:test`) | All tests green, including the 6-fixture golden runner |
| Boundary | `ModuleBoundaryTest` (runs inside the suite above) | No forbidden imports |
| Parity regression | `tools/parity_probe.py` (hub Python, as-is) | Exit 0 — Python↔Dart parity still green |
| Dart regression | `dart test` in `packages/rename_core` (unchanged code, unchanged goldens) | Green — proves the copied fixtures were not edited in place |
| Static | Kotlin compile via the Gradle task (no separate lint configured in this repo) | Clean compile, no warnings introduced by the new files |

- **FACT:** There is no device gate for this slice. Nothing here
  needs the Moto G, adb, or an emulator.
- **RECOMMENDATION:** Run the gates in the order listed — the JVM
  suite first (fast signal), parity + Dart regression last
  (proves the sources this port was copied from are untouched).

## 9. Risks

| # | Risk | Note |
|---|---|---|
| 9.1 | Sort-order drift (Unicode) | Python sorts by code point, Dart/Kotlin `String.compareTo` by UTF-16 code unit. For BMP filenames they agree; for names containing characters above U+FFFF (e.g. emoji), UTF-16 surrogate order can differ from code-point order, so a plan could order two such files differently across Python/Dart/Kotlin. **INFERENCE** — no fixture covers it and it has not been observed; it is a theoretical drift. Mitigation: add one unit test with an astral-plane filename to pin the Kotlin behavior, and record the finding (see §10.5). |
| 9.2 | Planned-sort tiebreak | Python relies on a stable sort over pre-sorted candidates; Dart adds an explicit srcPath tiebreak. They agree when names differ or paths differ consistently, but the two mechanisms must be checked against the `sort_determinism` golden, not assumed. The golden runner is the proof. |
| 9.3 | The 5-vs-6 column manifest | If Kotlin writes 6 columns but a later consumer expects Python's 5, the extra `content_uri` column must be tolerated. The codec is header-driven (§6.4), so a 6-column file stays readable by the 5-column reader; the reverse (a S3/S4 consumer needing `content_uri` from a 5-column file) is the failure mode the 6-column default avoids. Still needs Glenn's confirm (§10.1) before S3/S4 consume manifests — the recommendation is stated here, not silently assumed. |
| 9.4 | Symlink misunderstanding | The most likely port bug: implementing symlink detection inside `buildPlan` (impossible — it takes entries, not a directory) or, worse, claiming the port "handles symlinks" while the enumerator passes them through as files. §6.2's note exists to prevent this. The enumerator slice must own it. |
| 9.5 | Scope creep into execution | `executionOrder` is pure, but it invites "just call `File.renameTo` here". Any filesystem call in this slice violates the boundary (§2) and the pure-JVM gate (§7). S4 owns execution. |
| 9.6 | Golden drift | If anyone "fixes" a golden JSON to match a Kotlin behavior instead of fixing the Kotlin code, parity is silently destroyed. Mitigation: the Dart regression gate (§8) re-runs Dart against the same files; a changed fixture fails there first. |
| 9.7 | Python/Dart semantic drift since PR #1 | The port targets post-PR #1 Python. If the Python script changes again before this slice lands, the target moved. Mitigation: record the Python head (`git log --oneline -1 -- rename_camera_prefixes.py`) in the implementation PR body. |

## 10. Open questions (for Glenn — do NOT decide here)

1. **Manifest columns (confirm, not decide):** the spec
   RECOMMENDS the Dart 6-column form (§6.4, reconciled with S4
   §4.1: SAF identity is the document URI). Confirm the 6-column
   form as the native standard — or revert to Python's 5 columns.
   Consequence to note: the three stacks then have two manifest
   shapes (Python 5, Dart/Kotlin 6), and any future Python `--undo`
   of a Kotlin manifest must handle the extra column (or explicitly
   refuse it). Flag if that consequence is unacceptable.
2. **Return shape:** Keep the nullable-`String?` + `""`-sentinel
   shape for `stripLeadingPrefix` (mechanical port, §6.1), or use
   a sealed result type (`NoMatch` / `EmptyResult` / `Stripped`) that
   is more idiomatic Kotlin but diverges from both Python and Dart?
3. **`parity_probe.py` and Kotlin:** The probe proves Python↔Dart
   only. Should it be extended to also run the Kotlin golden runner
   (making parity three-way), or stay untouched with Kotlin parity
   proven transitively through the shared golden fixtures?
4. **Package layout:** Flat files beside the existing core files
   (`RenamePlan.kt`, … — matches current flat layout) or a
   `core/rename/` subpackage (cleaner, but the first subpackage in
   `vision-core`)? No existing precedent either way in this module.
5. **Astral-plane filenames (§9.1):** Is pinning UTF-16 vs
   code-point sort order for emoji/astral filenames worth a test
   and a documented rule, or is it over-engineering for camera
   filenames (which are ASCII `IMG_…`/`VID_…` in practice)?
6. **Chain-ordering home (§6.3):** The spec recommends porting
   `_execution_order` as pure logic in this slice. Alternative: leave
   it entirely to S4 (execution). Is the in-slice pure port the
   right call, or should this slice be strictly plan/manifest/undo
   with no ordering function at all?
