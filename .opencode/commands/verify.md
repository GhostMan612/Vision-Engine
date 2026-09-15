---
description: Run the verification gates (analyze + test)
---

Run the project verification gates in order (lane ends at source correctness — NEVER build):

1. `flutter pub get` (in `app/`) or `dart pub get` (in `packages/<pkg>/`)
2. `flutter analyze --no-pub` (app) / `dart analyze` (packages) — must report **No issues found!**
3. `flutter test` (app) / `dart test` (packages) — all tests must pass
4. If `rename_core` or the Python script changed: `C:\venv-hub\venv\Scripts\python.exe tools/parity_probe.py` (expect `parity: GREEN`)

Filter output to failures only. Report compact summary: analyze status, test N/N, parity status, failures with file:line. NEVER run `flutter build` anything — the human builds in Android Studio.
