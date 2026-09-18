# Device validation — Work Package MP5 thumbnails (+ MP4 suite)

> Human- or agent-run. READ-ONLY. App-private temp + bundled synthetic
> fixtures. Never points at DCIM, never renames, never writes user media.

## Run (ONE command, from `C:\vision engine\native`, Moto G attached)
```powershell
$env:JAVA_HOME = "C:\android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\android\sdk"
& "C:\Users\612co\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" :vision-android:connectedDebugAndroidTest --console=plain
```
This runs BOTH instrumented suites: `ExtractorDeviceTest` (6 tests —
exact EXIF incl. decimal exposure + GPS, PNG unknowns, missing/corrupt
grace, container shape, untouched sources) and `ThumbsDeviceTest`
(6 tests — orientation decode 256x384, PNG 256x256, scan-less/garbage/
empty graceful nulls, trackless-video unavailable, cache round-trip,
untouched sources + work-dir deletion proof).

## Paste back into chat
1. The full terminal output (or at minimum `BUILD SUCCESSFUL` /
   the failure block with Expected/Actual lines).
2. Confirmation line: no folder picker appeared and no DCIM was touched
   (both suites are fully self-contained; none should).

## What success proves
Real Skia decode incl. explicit orientation transforms, real retriever
frame path (graceful `unavailable` on trackless media), real EXIF reads,
bounded cache behavior, and byte-identical sources afterwards — the last
platform-truth leg before MP6 builds the pipeline on it.
