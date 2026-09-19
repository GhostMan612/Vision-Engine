# Device validation — Work Package MP6 pipeline (+ MP5 re-run)

> Human- or agent-run. READ-ONLY. App-private temp + bundled synthetic
> fixtures. Never points at DCIM, never renames, never writes user media.

## Status at authoring time
The MP6 device suite listed below was authored and COMPILED but could
not be executed here: `adb devices` reports an empty list (the Moto G
is not currently visible to adb — unplugged, USB mode changed,
debugging revoked, or powered off). An `adb kill-server` recovery
could not be attempted (executor process-spawn instability at the
time). Re-run as soon as the device reappears; no code changes needed.

## Run (ONE command, from `C:\vision engine\native`, Moto G attached)
```powershell
$env:JAVA_HOME = "C:\android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\android\sdk"
& "C:\Users\612co\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" :vision-android:connectedDebugAndroidTest --console=plain
```
This runs ALL instrumented suites: `ExtractorDeviceTest` (6 tests),
`ThumbsDeviceTest` (6 tests), and `PipelineDeviceTest` (1 test):
stage fixtures → planDiscovery ordering on-device → load page (real
Exif/retriever reads) → photo/PNG/video/text thumbnails → cache hit →
untouched-bytes proof → work-dir deletion proof.

## Paste back into chat
1. The full terminal output (or at minimum `BUILD SUCCESSFUL` /
   the failure block with Expected/Actual lines).
2. Confirmation line: no folder picker appeared and no DCIM was touched
   (all suites are fully self-contained; none should).

## What success proves
End-to-end MP6 on hardware: discovery ordering, real extraction on
staged files, real Skia decode incl. orientation, real retriever frame
path (graceful `unavailable` on trackless media), bounded cache
behavior, and byte-identical sources afterwards — the last
platform-truth leg before MP7 builds the Viewer on it.
