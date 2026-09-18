# Device validation — Work Package MP4 extraction

> Human-run. READ-ONLY. App-private temp + bundled synthetic fixtures.
> Never points at DCIM, never renames, never writes user media.

## Run (ONE command, from `C:\vision engine\native`, Moto G attached)
```powershell
$env:JAVA_HOME = "C:\android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\android\sdk"
& "C:\Users\612co\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" :vision-android:connectedDebugAndroidTest --console=plain
```
(Or run the `ExtractorDeviceTest` configuration from Android Studio
with the Moto G selected.) This installs the test APK, stages the four
synthetic fixtures into app-private temp, and asserts exact EXIF values
(incl. decimal exposure + GPS), unknown-field behavior, missing/corrupt
grace, minimal-container shape, and byte-identical sources afterwards.

## Paste back into chat
1. The full terminal output (or at minimum `BUILD SUCCESSFUL` / the
   failure block with Expected/Actual lines).
2. Confirmation line: no folder picker appeared and no DCIM was touched
   (the suite is fully self-contained; none should).

## What success proves
Real ExifInterface + MediaMetadataRetriever behavior on hardware for the
exact mapping the JVM suites already pin: tag reads, GPS decimals,
orientation/dimension handling, retriever lifecycle, and graceful
degradation — the last platform-truth leg before MP5/MP6 build on it.
