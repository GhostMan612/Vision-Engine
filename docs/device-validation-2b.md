# Device validation — Work Package 2B extraction layer

> Human-run. READ-ONLY. Touches nothing but app-private temp + bundled
> synthetic fixtures. Never points at DCIM, never renames, never writes
> user media. Estimated 10–15 minutes including first compile.

## Prereqs
Moto G 2025 on USB (USB debugging accepted — same setup as the D2 run).

## Run (ONE command, from `C:\vision engine\app`)
```powershell
flutter test integration_test
```
This compiles a test binary, installs the debug app, stages three
synthetic fixtures (hand-built 305B EXIF JPEG, 454B EXIF-less PNG,
150B trak-less MP4 — see `tools/make_fixtures.py`) into app-private temp,
and exercises the real Kotlin bridge: full-EXIF assertions, unknown-field
assertions, missing-file, corrupt-file, and minimal-container shape.

## Paste back into chat
1. The full terminal output (or at minimum the final `All tests passed!`
   / failure block).
2. If anything fails: the exact failing test name + Expected/Actual lines.
3. Confirmation line: no DCIM/folder picker appeared (none should — the
   suite is fully self-contained).

## What success proves
The Kotlin bridge + channel + adapter decode real platform metadata
correctly on hardware (ExifInterface tag reads, GPS decimals, retriever
lifecycle). Exact video-field behavior on real camera files stays covered
by host payload tests now and G2 real-shot smoke later — the trak-less
fixture intentionally asserts graceful shape, not camera values.

## If the build fails
Paste the Gradle error. Do NOT change compileSdk/AGP/Gradle versions to
fix it — toolchain changes need a dedicated session (ADR-002). First
build needs network (Gradle + `exifinterface:1.4.1` download).
