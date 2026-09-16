# Device validation — Work Package 2C pipeline

> Human-run. READ-ONLY. Dedicated temp dir (`VE_2C` under app-private
> cache) with bundled synthetic fixtures only. Never points at DCIM,
> never renames, never writes user media. Estimated 10–15 minutes
> including compile.

## Run (ONE command, from `C:\vision engine\app`)
```powershell
C:\android\flutter\bin\flutter.bat test integration_test
```
This runs BOTH device suites: the 2B extraction suite (5 tests) and the
2C pipeline suite (1 test): stage fixtures → discover (sorted, bounded,
classified, nested excluded) → load page (records with sort positions) →
probe via the real bridge → fetch photo/PNG/video/text thumbnails →
verify cache hit → verify source bytes+hashes unchanged → delete the
work dir and prove it is gone.

## Paste back into chat
1. The full terminal output (or at minimum the final `All tests passed!`
   / failure block).
2. If anything fails: the exact failing assertion + Expected/Actual lines.
3. Confirmation line: no folder picker appeared and no DCIM was touched
   (the suite is fully self-contained; none should).

## What success proves
End-to-end 2C on hardware: SAF-shape discovery (top-level, ordered),
real extraction on staged files, real Skia decode incl. orientation,
real retriever frame path (graceful `unavailable` on the trackless
fixture), bounded cache behavior, and byte-identical sources afterwards.

## If the build fails
Paste the Gradle error. Do NOT change compileSdk/AGP/Gradle versions —
toolchain changes need a dedicated session (ADR-002). First build needs
network (Gradle + cached deps).
