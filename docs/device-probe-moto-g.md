# Device probe — Moto G (D2 evidence run)

> Goal: decide the shared-storage rename path (D2). The app already renames
> app-private files with pure `File.rename`. What we do NOT know without a
> device: does raw `File.rename` work in a user-picked shared folder on YOUR
> Moto G's Android version, or is it BLOCKED (scoped storage, Android 10+)?
> BLOCKED confirms the BP-05 MediaStore `DISPLAY_NAME` adapter. Either answer
> closes D2 — there are no wrong results, only evidence.

## Safety rules for this probe

- The probe ONLY ever writes files named `VE_PROBE_*` / `IMG_probe_*` /
  `VID_probe_*` inside a folder it creates itself, and deletes that folder
  afterwards (the report ends with `cleaned=true` — check it).
- NEVER pick real DCIM/Camera for this probe. Create the throwaway folder in
  step 6. Your photos are never listed, read, or touched.
- Debug Run from Android Studio is enough. No release build, no signing.

## Steps (human operator)

1. **Phone:** Settings → About phone → tap Build number 7 times →
   Developer options → enable USB debugging. Connect USB, accept the RSA key.
2. **PC:** confirm the device is visible (PowerShell):
   `C:\android\sdk\platform-tools\adb.exe devices`
3. **Android Studio:** Open `C:\vision engine\app`. Wait for Gradle sync to
   finish (first sync downloads Gradle + deps — needs network, takes minutes).
   Select your Moto G in the device dropdown, press Run.
4. **Rename tab:** Seed demo files → Preview (expect 2 ops) → Execute →
   Undo. All inside app-private `vision_demo`. Zero risk.
5. **Probe tab:** Run app-private probe → expect `APP_PRIVATE_PROBE: OK`.
   Copy report.
6. **Throwaway folder:** on the phone (Files app), create
   `Pictures/VE_TEST` (empty — the probe brings its own files).
7. **Probe tab:** Check permission status (note the line) → Request media
   permission → Allow → Pick folder + run shared probe → pick `VE_TEST`.
   Copy report.
8. **Send back** (paste into chat):
   - Android version (Settings → About phone → Android version)
   - The full app-private report
   - The full shared-folder report (especially the `RAW_RENAME:` line)
   - Which consent dialogs appeared, and when

## How to read the result

- `RAW_RENAME: WORKS here` → raw rename is permitted on this device/version.
  SAF picker stays primary for consistency, MediaStore stays fallback.
- `RAW_RENAME: BLOCKED here` → scoped-storage semantics confirmed; the
  Phase-5 adapter MUST go through MediaStore `DISPLAY_NAME` update +
  `createWriteRequest` consent (BP-05 as specced). No `MANAGE_EXTERNAL_STORAGE`.
- `SHARED_PROBE: INCONCLUSIVE` or `cleaned=false` → re-run and report; do
  not proceed to real folders until a COMPLETE run exists.
