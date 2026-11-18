# S5 — MP7 Viewer device test runbook + evidence checklist

> Device evidence for draft PR #10 (`feat/mp7-native-viewer`, base
> main `9c3a2aa`). G2 stays OPEN until the instrumented Viewer
> suite AND the real-shot read-only smoke both pass and the
> operator pastes the evidence below. Nothing here may be claimed
> from a host run or an emulator.

## Hardware

- Primary device: **BLU View 5 / B160V, Android 14**, ADB serial
  `7040016025040287`, always USB-connected to the operator's laptop
  via USB debugging. This is the same device that ran S1 (13/13),
  S4 Stage A, and the S4B/S4C runs. The S5 spec names the Moto G;
  the B160V is the recorded hardware substitution — do not claim
  Moto G coverage.
- Laptop repo: build from a detached worktree at the reviewed PR
  head (same pattern as the S4 device builds and the S1 run), never
  from the diverged original checkout (`C:\vision engine`, local
  main `59b21eec`). Per-terminal, before any Gradle command:
  `$env:JAVA_HOME = "C:\android\Android Studio\jbr"` and
  `$env:ANDROID_HOME = "C:\android\sdk"`, then run `.\gradlew.bat`
  from the worktree's `native/` directory.

## Rules for every Viewer device run

- **No rename, no modify, no delete during Viewer tests.** The
  Viewer is read-only; if any file changes, stop and report it —
  that is a defect, not a test step.
- **No live DCIM writes.** Real-shot smoke uses COPIES of real
  camera files in an isolated folder under `Pictures`
  (e.g. `Pictures/VE_TEST_S5`), never the live DCIM folder.
- **No real media committed, ever.** Synthetic fixtures only in
  the repo. Real shots are operator-supplied and stay on the
  device.
- The Viewer reads only folders the user picked through S3. If a
  folder picker appears during a run that should not need one,
  record exactly when — that is evidence, not noise.

## Step 1 — Build + install

1. In the detached worktree at the PR head:
   `git rev-parse HEAD` — paste the exact head SHA.
2. `.\gradlew.bat :vision-app:assembleDebug`
   (`BUILD SUCCESSFUL` required).
3. `adb -s 7040016025040287 install -r
   vision-app\build\outputs\apk\debug\vision-app-debug.apk`
   (explicit serial targeting; `-r` reinstall keeps the S3 grants).
4. Paste: head SHA, Gradle result, install result, Android version.

## Step 2 — Instrumented Viewer suite

Run, from the worktree's `native/` directory:

```
.\gradlew.bat :vision-android:connectedDebugAndroidTest
```

Expected: `UriPipelineDeviceTest` 4/4 PASS — URI photo probe +
thumb on device (orientation 6, 16×24 corrected, 256×384 thumb,
cache hit on second call), URI video probe + thumb unavailable
on the trackless fixture (duration 0), corrupt photo partial
(metadata `partial`, thumb `failed`, bytes null), sources
untouched (snapshot identical). The suite
stages bundled synthetic fixtures in the instrumentation
(test) context's private `ve_mp7` directory — NOT the target
app's private dir — so the test provider's file read does not
depend on the test process sharing the app UID.
`TestDocsProvider` has no `android:process`, so it runs in the
test APK's default process, the SAME process as the
instrumented test code, and the static
`TestDocsProvider.root` sharing is valid by construction, not
cross-process. The cross-process hop is the
`ParcelFileDescriptor` over Binder to the app process, the
standard ContentProvider mechanism. The provider fails loud
(`FileNotFoundException` "no root") if the root is unset.

The 4 instrumented cases validate URI processing on device.
They do NOT replace the real picked-folder smoke in Step 3 —
the smoke over a genuinely picked folder is still required
evidence, and G2 stays open until both pass.

Also re-run the S1 MP6 suite in the same session (regression):
`ExtractorDeviceTest` 6/6, `ThumbsDeviceTest` 6/6,
`PipelineDeviceTest` 1/1. Paste the per-class counts and the
Gradle result. If any S1 test goes red, stop — the Viewer device
evidence is invalid until S1 is green again.

## Step 3 — Real-shot read-only smoke

1. Copy a few real device-camera shots (at least one photo with
   EXIF orientation, one plain photo, one video) into an isolated
   folder `Pictures/VE_TEST_S5`. Record the file count and, for
   each file, its name + SHA-256 BEFORE opening the Viewer.
2. Open the app → Folders tab → pick `Pictures/VE_TEST_S5` (or
   confirm it is already listed) → Viewer tab → Select the folder.
3. Observe and paste:
   - Grid: every file renders a cell (name + kind badge); thumbs
     present for photos, first frame or "Thumb unavailable" for
     video; no blank cell, no crash.
   - Tap a photo with EXIF orientation: corrected dims sane
     (headline W×H matches the orientation-corrected values),
     raw orientation tag shown, missing fields show `—`,
     provenance chips accurate (exif / container / filesystem /
     unknown as appropriate).
   - Tap a video: duration human-readable, corrected dims,
     rotation, codec; first frame or "Thumb unavailable".
   - GPS "Strip…" button: visible, DISABLED, labeled Phase 3.
     Tapping it must do nothing.
   - Back returns to the grid.
4. After the smoke, re-list the folder and re-hash every file:
   names + SHA-256 must be IDENTICAL to the before-hashes.
   Any change is a defect — report it, do not proceed.
5. First-page load time: time from tapping Select to the first
   page fully rendered (records + thumbs), in seconds, on this
   folder. Record the number; it is evidence for a future
   concurrency decision, not a pass/fail in this slice.

## Step 4 — Airplane-mode run

1. Turn airplane mode ON.
2. Kill the app (swipe away) and reopen it; open the Viewer over
   `Pictures/VE_TEST_S5` again.
3. Expected: the grid + detail render exactly as in Step 3 (all
   data is local; no network is used anywhere in the Viewer).
   Paste the result. Turn airplane mode OFF afterward.

## Step 5 — Empty / lost-grant states (quick)

1. Pick an empty folder → Viewer tab → "No photos or videos
   here" renders (no crash, no fake content).
2. Revoke the grant in system Settings (App info → Permissions /
   storage access) → reopen the Viewer → the folder shows
   "Vision lost access…" with a Re-pick button; loaded records
   stay visible; no crash. Re-pick restores it in place.
   (This also covers the S3 revoke-then-re-pick item that has
   been open since PR #5.)

## Evidence checklist (paste back, all of it)

- [ ] PR head SHA built + installed (exact 40-char SHA).
- [ ] Gradle `BUILD SUCCESSFUL` for `:vision-app:assembleDebug`.
- [ ] `UriPipelineDeviceTest` 4/4 PASS (per-test names + counts).
- [ ] S1 MP6 suite re-run: 13/13 PASS (6/6 + 6/6 + 1/1).
- [ ] Real-shot smoke: file list + before/after SHA-256
      (identical), corrected dims observed, `—` where expected,
      provenance chips accurate, Strip button disabled.
- [ ] First-page load time (seconds) on the smoke folder.
- [ ] Airplane-mode run result.
- [ ] Empty-folder + revoke/re-pick results.
- [ ] Any consent dialog that appeared, and when.
- [ ] Any provider variance (e.g. `renameDocument` is not used
      by the Viewer; any SAF read refusal or unverifiable thumb
      is evidence, not a pass).

## G2 stays open until

- The instrumented Viewer suite (Step 2) is green on the B160V,
  AND
- The real-shot read-only smoke (Step 3) is pasted with
  identical before/after hashes.

Host runs prove the branches; only this evidence closes G2.
Do not flip G2 on host results alone.
