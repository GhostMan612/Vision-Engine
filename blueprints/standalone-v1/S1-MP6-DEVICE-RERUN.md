# S1 — MP6 device re-run (spec)

> Status: RUN RECORDED 2026-11-17 — see §7. The run happened on the
> BLU B160V (hardware substitution for the Moto G named below); 13/13
> PASS, Gradle BUILD SUCCESSFUL. The slice is NOT fully closed: the
> self-containment confirmation (§5.4 item 3 / §7 / §8) is PENDING
> operator confirmation. No code changes in this slice. The original
> spec text is preserved below; run results are recorded in §7 and
> §13, and superseded claims are marked, not erased.

Labels used throughout: **FACT** (verified in repo/docs), **REPO
EVIDENCE** (file:line or gate result), **RECOMMENDATION** (judgment),
**INFERENCE** (strong but unverified), **OPEN QUESTION** (needs Glenn).
Assumptions are never presented as facts.

## 1. Goal

Re-run the already-authored, already-compiled MP6 instrumented device
suite on the Moto G and paste the result back, closing the last
platform-truth leg before MP7 (Viewer) builds on it.

- **FACT:** The MP6 pipeline (`MediaPipeline`: sequential `loadPage` +
  `thumbnailFor` over the MP4/MP5 layers, URI-less degradation, guarded
  reads) exists with 7/7 JVM tests green, and its instrumented
  `PipelineDeviceTest` is authored and compiled — but its device run is
  blocked.
- **REPO EVIDENCE:** `blueprints/CURRENT_STATE.md` MP6 entry
  ("instrumented `PipelineDeviceTest` authored+compiled, DEVICE RUN
  BLOCKED (Moto G not visible to adb at session time — rerun per
  `docs/device-validation-mp6.md`)"); `docs/device-validation-mp6.md`
  ("The MP6 device suite listed below was authored and COMPILED but
  could not be executed here: `adb devices` reports an empty list").
- **RECOMMENDATION:** This slice is a *re-run*, not a build. If the run
  fails on a test assertion (rather than on device visibility), that is
  a new defect and belongs in a follow-up slice, not in this one.

## 2. Non-goals

- No code changes of any kind (no fixes, no test edits, no Gradle edits).
- No real photos, no DCIM, no user media touched in any way.
- No MP7 Viewer work, no MP8 parity work, no MP9 retirement work.
- No re-run of the Flutter suites (the Flutter 2C device story is
  already green; see §3).
- No attempt to run the suite from this VM — there is no device on
  this VM's adb, and that is out of scope for this slice.

## 3. Grounding — what already exists

### 3.1 The three suites (13 instrumented tests)

| Suite | File | Tests | App-private work dir | Fixtures staged |
|---|---|---|---|---|
| `ExtractorDeviceTest` | `native/vision-android/src/androidTest/kotlin/com/visionengine/android/meta/ExtractorDeviceTest.kt` | 6 | `ve_mp4` | `ve_exif.jpg`, `ve_plain.png`, `ve_minimal.mp4`, empty `empty.jpg` |
| `ThumbsDeviceTest` | `native/vision-android/src/androidTest/kotlin/com/visionengine/android/thumbs/ThumbsDeviceTest.kt` | 6 | `ve_mp5` | same fixture set |
| `PipelineDeviceTest` | `native/vision-android/src/androidTest/kotlin/com/visionengine/android/pipeline/PipelineDeviceTest.kt` | 1 | `ve_mp6` | `ve_thumb.jpg` → `IMG_b.jpg`, `ve_plain.png` → `shot.png`, `ve_minimal.mp4` → `VID_a.mp4`, `note.txt`, `sub/nested.jpg` |

- **REPO EVIDENCE (counts):** `grep -c '@Test'` on the three files
  returns 6, 6, and 1 respectively — 13 total, matching
  `docs/device-validation-mp6.md`.
- **FACT:** All three suites stage only bundled synthetic fixtures
  into app-private storage (`Context.getDir(...)`) and delete the work
  dir in `@After` (`work.deleteRecursively()`).
- **REPO EVIDENCE:** `PipelineDeviceTest.kt` `stage()` lines ~30–47
  and `cleanup()`; `ExtractorDeviceTest.kt` `stage()` stages into
  `ve_mp4`.
- **FACT:** The same run pattern already worked twice on this device:
  MP4 extraction device suite 6/6 PASS and MP5 thumbnails device
  suite 6/6 PASS, both agent-run over adb on the Moto G.
- **REPO EVIDENCE:** `blueprints/CURRENT_STATE.md` MP4 and MP5
  entries ("device suite 6/6 PASS on Moto G (agent-run)").
- **INFERENCE:** The MP6 block is therefore most likely environmental
  (device visibility), not a defect in the suite — but that is not
  proven until the run completes. The runbook agrees: "Re-run as soon
  as the device reappears; no code changes needed."
  **Resolved 2026-11-17:** the run completed 13/13 with no test
  failure — the block was environmental, not a suite defect.

### 3.2 The runbook (already written — this spec does not rewrite it)

- **REPO EVIDENCE:** `docs/device-validation-mp6.md` is the runbook.
  It is READ-ONLY, app-private temp + bundled synthetic fixtures,
  "Never points at DCIM, never renames, never writes user media."
- **FACT:** The one command, run from `C:\vision engine\native` with
  the Moto G attached (PowerShell, quoted verbatim from the runbook):

```powershell
$env:JAVA_HOME = "C:\android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\android\sdk"
& "C:\Users\612co\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" :vision-android:connectedDebugAndroidTest --console=plain
```

- **FACT:** That command runs ALL instrumented suites in
  `:vision-android` — the three MP6 suites above.
- **REPO EVIDENCE:** `docs/device-validation-mp6.md` ("This runs ALL
  instrumented suites: `ExtractorDeviceTest` (6 tests),
  `ThumbsDeviceTest` (6 tests), and `PipelineDeviceTest` (1 test)").
- **OPEN QUESTION:** The Gradle path in the runbook embeds a
  wrapper-distribution hash (`9ot9r568e8zfvvd4mn8rbu1j0`) and Gradle
  9.3.1. If Glenn's laptop has since upgraded Gradle or cleared the
  wrapper cache, that path will not exist. Confirm the path first; if
  it is stale, the fix is to point at the current `gradle.bat` (or the
  project's `gradlew`) — a runbook correction, not a code change.

### 3.3 What success proves

- **REPO EVIDENCE:** `docs/device-validation-mp6.md` ("What success
  proves"): end-to-end MP6 on hardware — discovery ordering, real
  extraction on staged files, real Skia decode incl. orientation,
  real retriever frame path (graceful `unavailable` on trackless
  media), bounded cache behavior, and byte-identical sources
  afterwards — "the last platform-truth leg before MP7 builds the
  Viewer on it."
- **FACT:** `PipelineDeviceTest` additionally proves the work-dir
  deletion (`@After` cleanup) and the untouched-bytes proof
  (snapshotted fixture bytes before/after).

## 4. Inputs (exact files)

| File | Role in this slice |
|---|---|
| `docs/device-validation-mp6.md` | Runbook — the procedure. Read, do not rewrite in this slice. |
| `native/vision-android/src/androidTest/kotlin/com/visionengine/android/meta/ExtractorDeviceTest.kt` | Suite under test (6 tests). Read-only. |
| `native/vision-android/src/androidTest/kotlin/com/visionengine/android/thumbs/ThumbsDeviceTest.kt` | Suite under test (6 tests). Read-only. |
| `native/vision-android/src/androidTest/kotlin/com/visionengine/android/pipeline/PipelineDeviceTest.kt` | Suite under test (1 test). Read-only. |
| `native/vision-android/src/androidTest/assets/fixtures/ve_exif.jpg`, `ve_minimal.mp4`, `ve_plain.png`, `ve_thumb.jpg` | Bundled synthetic fixtures the suites stage. Read-only. |
| `blueprints/CURRENT_STATE.md` (MP6 entry) | State record to update after the run (see §7). |
| `blueprints/CHECKLIST.md`, `blueprints/CHECKPOINTS.md` | State records to update after the run (see §7). |

No other files are in scope. In particular, do not touch
`native/vision-android/src/main/**`, `native/vision-core/**`, or any
Gradle file as part of this slice.

## 5. Step-by-step work (Glenn-side, laptop + Moto G)

### 5.1 Preconditions

- [ ] Moto G (2025) powered on, unlocked, USB debugging enabled
      (Developer options).
- [ ] Phone connected to the laptop by USB; on the phone, the USB
      mode / RSA fingerprint prompt is accepted ("Allow USB
      debugging" — accept, do not tick "always allow" unless Glenn
      already does).
- [ ] `C:\vision engine\` is on the latest `main` (post-PR #2,
      `d103ef6` or later — the MP6 suite is unchanged by PRs #1/#2,
      but run from a known head and record it).
- [ ] The Gradle path in the runbook (§3.2) still exists on the
      laptop; if not, note the corrected path before running.

### 5.2 Device visibility check (before the Gradle run)

```powershell
adb devices
```

- [ ] Output lists exactly one device with status `device`
      (not `unauthorized`, not `offline`, not empty).
- [ ] If the list is empty or the device shows `unauthorized`, work
      §6 (triage) first — do not start Gradle until `adb devices`
      is clean. A Gradle run against no device fails slowly and
      tells nothing.

### 5.3 The run

- [ ] From `C:\vision engine\native`, run the one command in §3.2
      exactly (with the §3.2 path caveat applied if needed).
- [ ] Let it run to completion. Do not cancel mid-run: a cancelled
      run leaves the test APK installed and the app-private work
      dir behind, and the paste-back is ambiguous.
- [ ] Capture the full terminal output to a file or copy it whole
      (the runbook asks for the full output, or at minimum the
      `BUILD SUCCESSFUL` line / the failure block with
      Expected/Actual lines).

### 5.4 Paste-back evidence (into chat, verbatim)

1. The full terminal output (or at minimum `BUILD SUCCESSFUL` /
   the failure block with Expected/Actual lines).
2. The head commit the run was made from (`git log --oneline -1`).
3. Confirmation line: no folder picker appeared and no DCIM was
   touched (all suites are fully self-contained; none should).
4. The `adb devices` output from §5.2 (proves which device ran).

**What was actually pasted back (2026-11-17):** a summary, not the
full §5.4 paste-back — per-suite counts (Extractor 6/6, Thumbs 6/6,
Pipeline 1/1), `BUILD SUCCESSFUL`, 1m 32s, device BLU B160V
Android 14 (ADB serial `7040016025040287`), instrumented XML
reports independently checked. **Gaps (do not invent):** the head
commit was not pasted, the `adb devices` output was not pasted,
and the §5.4 item 3 confirmation line ("no folder picker appeared
and no DCIM was touched") was NOT given — it is PENDING, and S1
stays open until it arrives.

### 5.5 After the run — state update (separate, small commit or handoff note)

- [ ] `blueprints/CURRENT_STATE.md` MP6 entry updated with the run
      result (date, device, 13/13 or the failure, head commit).
- [ ] `blueprints/CHECKLIST.md` / `blueprints/CHECKPOINTS.md`
      updated per RULES.md §4.2 session-end law, if that law is
      still in force when the run lands.
- **RECOMMENDATION:** Keep the state update in its own commit from
  any later code work. Do not bundle it into S2 or MP7.

## 6. Failure triage (ordered — stop at the first step that fixes it)

These steps are **RECOMMENDATION**, not repo evidence: the runbook
records only that `adb devices` was empty and that `adb kill-server`
recovery "could not be attempted (executor process-spawn instability
at the time)". Each step is cheap, reversible, and Glenn-side.

1. **Re-seat the cable.** Unplug/replug USB; try a different port.
   Re-run `adb devices`.
2. **Check the phone's USB mode.** Pull down the USB notification:
   it should be File Transfer / MTP (or "No data transfer" is fine
   for adb, but Charging-only mode on some builds hides adb).
   If a "Allow USB debugging?" RSA prompt appears, accept it; if it
   never appears, the laptop's adb key may be revoked on the phone.
3. **`adb kill-server` then `adb start-server`, then `adb devices`.**
   (The recovery that could not be attempted last time.)
4. **Revoke authorizations on the phone** (Developer options →
   "Revoke USB debugging authorizations"), reconnect, accept the
   fresh RSA prompt.
5. **Check for a competing adb server.** Android Studio runs its own
   adb; a second server on port 5037 can shadow the CLI one. Close
   Android Studio (or kill its adb), then `adb kill-server` +
   `adb start-server` again.
6. **Windows driver check.** Device Manager → is "Moto G" present
   under Portable Devices / Android Device without a warning icon?
   If it shows as unknown, the Google USB driver / Motorola driver
   needs reinstalling. (This is a laptop fix, not a repo fix.)
7. **Try another cable.** Charge-only cables carry no data and are
   the classic silent failure.
8. **If the device appears but the Gradle run fails on a test
   assertion:** paste the failure block; do not edit the test to
   make it pass. That becomes a new defect slice (see §8).

## 7. Acceptance criteria — checklist

- [ ] `adb devices` listed the Moto G as `device` before the run
      (§5.2). **Not met as written:** the run used the BLU B160V
      (hardware substitution, recorded in §13); the `adb devices`
      output was not pasted.
- [x] The §3.2 command ran to completion from
      `C:\vision engine\native` (not cancelled). **2026-11-17:** run
      completed in 1m 32s per the operator report (exact command
      line not pasted).
- [x] Terminal output shows `BUILD SUCCESSFUL` and all 13 tests ran:
      `ExtractorDeviceTest` 6, `ThumbsDeviceTest` 6,
      `PipelineDeviceTest` 1. **2026-11-17:** per the operator
      report; instrumented XML reports independently checked.
- [ ] Paste-back delivered per §5.4 (full output, head commit,
      no-picker/no-DCIM confirmation, `adb devices` output).
      **Partial 2026-11-17:** a summary was delivered (counts,
      BUILD SUCCESSFUL, duration, device); full output, head
      commit, and `adb devices` output were not pasted.
- [x] All 13 tests passed (0 failures, 0 skipped).
      **2026-11-17, BLU B160V.**
- [ ] No folder picker appeared during the run; no DCIM touched
      (confirmation line present). **PENDING operator confirmation
      — S1 stays open until this line is given.**
- [x] `blueprints/CURRENT_STATE.md` MP6 entry updated with the
      result (date, device, head, 13/13). **Done by the 2026-11-17
      documentation-reconciliation PR (head commit for the run
      itself was not pasted — recorded as a gap, not invented).**
- [x] No file outside the state docs (§5.5) was modified by this
      slice. **The run itself modified no repo file; the state
      update is this docs PR.**

## 8. Gates

| Gate | How it is checked | Pass condition |
|---|---|---|
| Device visibility | `adb devices` (§5.2) | One device, status `device` |
| Suite run | §3.2 Gradle command | `BUILD SUCCESSFUL` |
| Test count | Gradle / test report | 13 tests run, 13 passed |
| Self-containment | Operator confirmation (§5.4.3) | No picker, no DCIM touched |
| Cleanup | Suite `@After` + operator check | App-private `ve_mp4`/`ve_mp5`/`ve_mp6` run dirs gone (or empty) after the run |

- **FACT:** There is no host-side gate for this slice: the suites are
  instrumented tests and can only run on the device. `flutter test`
  / JVM `./gradlew :vision-core:test` do not cover them.
- **RECOMMENDATION:** Do not substitute a JVM run or an emulator run
  for the Moto G run. Emulator ≠ device (house law, K1); the point
  of MP6 is hardware truth.

## 9. Device evidence required

The paste-back in §5.4 **is** the device evidence for this slice.
Nothing else counts: a verbal "it passed" without the terminal
output does not close MP6, because the MP4/MP5 closes were also
recorded from pasted output ("human-pasted `All tests passed!`" for
2B; "agent-run" 6/6 for MP4/MP5 — **REPO EVIDENCE**,
`blueprints/CURRENT_STATE.md`).

Keep the pasted output with the state update (in the handoff or
appended to the MP6 entry), so a later reader can see the 13/13
without re-running.

## 10. What unblocks after

- **FACT (blueprint order):** MP6 is the last platform-truth leg
  before MP7.
- **REPO EVIDENCE:** `docs/device-validation-mp6.md` ("the last
  platform-truth leg before MP7 builds the Viewer on it");
  `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §11 sequence
  (MP6 → "MP7 Native Viewer (== 2D-native): Compose grid/detail per
  G2 contract" → MP8 parity → MP9 Flutter retirement).
- **RECOMMENDATION:** On a green MP6, the next slice in queue is S5
  (MP7 Viewer spec), not more pipeline work. If MP6 goes red on a
  test assertion, MP7 stays blocked and the failure gets its own
  triage slice first.

## 11. Risks

| Risk | Note |
|---|---|
| Device not visible again | The known blocker (empty `adb devices`, Sept 2026). Triage §6 covers the common causes; if none work, the slice stalls and needs Glenn's laptop-side diagnosis, not repo work. |
| Stale Gradle path | Runbook embeds a wrapper-dist hash (§3.2 open question). A stale path fails before any test runs; check first. |
| Flaky device run | Instrumented runs can flake on a busy device (screen lock, low storage). **RECOMMENDATION:** re-run once on a flake before opening a defect; a second identical failure is a defect, not a flake. |
| MP4/MP5 regressions hiding in the same command | The §3.2 command runs ALL `:vision-android` instrumented suites, so MP4/MP5 suites run too. That is a feature (free regression check), but a failure in an MP4/MP5 test would need triage even though this slice is "MP6". Report it, don't silently ignore it. |
| Work-dir leftovers | If a run is cancelled mid-flight, `@After` cleanup may not run. **RECOMMENDATION:** check `ve_mp4`/`ve_mp5`/`ve_mp6` app-private dirs are empty afterwards; leftovers are harmless (synthetic fixtures only) but pollute the next run's evidence. |

## 12. Open questions (for Glenn — do NOT decide here)

1. **When does the run happen?** It needs Glenn's laptop + Moto G
   together. Is there a day/time to schedule it, or does it happen
   opportunistically?
   **Answered in part 2026-11-17:** the run happened on the BLU
   B160V (not the Moto G). No Moto G run is scheduled; no Moto G
   coverage is claimed.
2. **Gradle path:** Is the runbook's `gradle-9.3.1` wrapper-dist path
   (§3.2) still valid on the laptop, or has Gradle been upgraded
   since September? If upgraded, should the runbook be corrected to
   the new path (or to the project's `gradlew`) as part of this
   slice's state update?
3. **Single-class vs all-suites:** The §3.2 command runs every
   instrumented suite. On a failure, is a focused re-run
   (`--tests "*PipelineDeviceTest"`, or the equivalent class
   filter) acceptable for diagnosis, or should every attempt be the
   full command?
4. **Flake policy:** If the run flakes once (e.g. device busy) and
   passes on retry, is one green re-run enough to close MP6, or
   should a flake be recorded and the suite re-run a second time
   for confidence?
5. **State-update vehicle:** Should the §5.5 state update land as
   its own commit on `main`, or ride along with the next code PR?
   (House session-end law says update the state docs every session;
   the PR #1 lane deliberately deferred that to the spec PR.)
   **Answered 2026-11-17:** it landed as its own documentation
   PR (docs-only, no code).

## 13. Run result — 2026-11-17 (BLU B160V)

- **Device:** BLU B160V, Android 14, ADB serial
  `7040016025040287`. **Hardware substitution:** the S1 spec named
  the Moto G; the run used the BLU B160V instead. No Moto G
  coverage is claimed anywhere from this run.
- **Result:** `ExtractorDeviceTest` 6/6 PASS, `ThumbsDeviceTest`
  6/6 PASS, `PipelineDeviceTest` 1/1 PASS — **13 passed, 0 failed,
  0 skipped**. Gradle `BUILD SUCCESSFUL`; test execution completed
  in 1m 32s. Instrumented XML reports independently checked.
- **Counts corroborated in-repo:** `grep -c '@Test'` on the three
  device test files returns 6, 6, and 1.
- **Gaps (do not invent):** the head commit the run was made from
  was not pasted; the `adb devices` output was not pasted; the
  §5.4 item 3 confirmation line ("no folder picker appeared and
  no DCIM was touched") was NOT given.
- **Status:** 13/13 passed on B160V. S1 is NOT fully closed until
  the self-containment confirmation arrives. Recorded in
  `blueprints/CURRENT_STATE.md` and `blueprints/CHECKLIST.md` by
  the 2026-11-17 documentation-reconciliation PR.
