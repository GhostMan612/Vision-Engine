# Vision Engine — SHA-guarded safety overlay (2026-10-13)

This ZIP contains a **ready-to-apply remediation**, with source edit recipes and regression tests. It **does not** automatically overwrite existing application files when extracted. That safeguard prevents replacing newer work in your local repository. Applying the changes requires one command after extraction.

## Apply on the local Vision Engine repository

1. Back up your local repository and confirm a clean/understood `git status --short`.
2. Extract **the contents** of the ZIP into the root of `C:\vision engine\` so `tools\apply_vision_engine_overlay.py` is present.
3. In PowerShell:

```powershell
cd 'C:\vision engine'
python tools\apply_vision_engine_overlay.py --check
python tools\apply_vision_engine_overlay.py --apply
python -m pytest -q tests\test_rename_safety.py
python tools\parity_probe.py
```

Use the Python interpreter already permitted in `RULES.md`; do **not** install packages into the central shared virtual environment without approval. The parity probe requires Dart in PATH or at `C:\android\flutter\bin\dart.bat`.

Optional, deliberately **not the default**: to modify the historical frozen Flutter reference, add `--include-frozen-flutter` to the **first** `--apply` command. This changes files pinned by the `flutter-final` baseline and requires explicit revalidation. It is **not** a way to turn `dart:io` rename into an atomic no-overwrite operation. See the plan for the remaining platform race risk.

## What gets changed by the applicator

- `rename_camera_prefixes.py` — rename/undo safety, status tracking, no-clobber POSIX behavior, fail-closed manifest creation.
- `tools/parity_probe.py` — chain execution assertion and corrected Dart fallback.
- `.github/workflows/python-app.yml` — real Python safety CI job.
- `.github/workflows/python-publish.yml` — **deleted** (template PyPI publisher).
- `README.md`, `native/README.md`, `blueprints/CURRENT_STATE.md`, ADR-005, `SESSION_HANDOFF.md`, `.gitignore` — updated/compacted.
- `blueprints/_archive/SESSION_HANDOFF_PRE_REMEDIATION.md` — verbatim prior handoff preserved.
- `LICENSE` and `docs/LICENSE_DECISION.md` — conservative rights notice, pending owner's license selection.
- Optional frozen Flutter file corrections (only via explicit flag).

Extraction also places `tests/test_rename_safety.py` and `blueprints/VISION_ENGINE_SAFETY_REMEDIATION_PLAN.md` in the repo before you run the applicator.

## Safeguards and recovery

Every edited file must match its exact Git blob digest as inspected from the GitHub `main` branch on 2026-10-13 (Git-for-Windows CRLF checkouts are normalized for comparison and their original line-ending style is retained). If even one differs, `--check` or `--apply` aborts before writing. This intentionally **does not** automatically reconcile newer local work.

Before edits, the applicator saves originals in `.vision_engine_overlay_backup/<UTC-timestamp>/` and records file operations in `manifest.json`; that backup directory is added to `.gitignore`. It writes each file using a temporary sibling and an atomic replacement, but the entire multi-file batch is **not** transactional. Restore the saved originals if any step fails. The deleted PyPI workflow is in that backup.

You should inspect `git diff --check`, `git diff --stat`, `git status --short`, regression test output, and the Dart parity report before committing by **explicit path**. Do not claim a device pass or MP7 work from these files. No remote repository, full builds, local system settings, or real media were modified in producing this ZIP.

## Validation status at generation

- Overlay applicator: Python syntax-compiled, plus **one synthetic end-to-end application/backup/deletion smoke test passed** in an isolated scratch repository.
- Python regression cases: **13/13 passed in a reconstructed isolated logic harness**. They were **not** executed against an actual full checkout of the GitHub repository (this artifact environment cannot clone GitHub; Dart is unavailable).
- GitHub baseline hash verification: hashes captured for targeted paths; **local checkout check pending**.
- Actual on-device behavior: **not tested**.

Stop and inspect on any failure; do not use real media files as the first test.
