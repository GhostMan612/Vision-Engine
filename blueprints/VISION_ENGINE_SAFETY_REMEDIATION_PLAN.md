# Vision Engine — Archival Safety, Parity, CI and Governance Remediation

**Plan version:** 2026-10-13 · **Target:** `GhostMan612/Vision-Engine`, `main` · **Mode:** local-only, reviewable, SHA-guarded overlay · **GitHub remote:** no changes made.

## 1. Executive decision and scope

Preserve Vision Engine's architecture: a Python canonical renamer, frozen Flutter/Dart behavioral reference, and active Kotlin/Jetpack Compose migration (MP0–MP6 recorded, MP6 device run **blocked**, MP7 Viewer not started). Avoid rewriting the Viewer or introducing new dependencies. Perform a narrow archive-safety and evidence-hygiene strike.

The attached Kimi assessment is the starting point; source inspection identified further issues. **Correction:** Kimi's ordinary POSIX overwrite-chain scenario is overstated because `execute_plan` checks `dst.exists()` before calling `os.rename`. The actual risks are execution-order divergence, POSIX time-of-check/time-of-use (TOCTOU) collision races, faulty manifest state, symlink behavior, and unsafe recovery semantics. A dependency-aware chain executor and no-clobber move primitive address those without changing the Python/Dart *planning* contract.

**Non-scope:** no cloud writes, device installs, Android full builds, Gradle dependency upgrades, MediaStore permission expansion, production Viewer work, or Flutter-reference retirement. All synthetic tests must run in temporary directories, never over real photos.

## 2. Verified baseline and safety contract

- `rename_camera_prefixes.py` initially recorded `"renamed" if str(op.src) else "planned"`, which always resolves to `renamed` for ordinary paths; `undo_from_manifest` ignored the status.
- The Python planner allows a destination if it is itself a planned source; execution's existing-target guard skips the earlier operation, making plan counts differ from actual execution. A still-present POSIX overwrite race exists between check and `os.rename`.
- `Path.is_file()` follows symlinks; dangling symlinks can be hidden by `exists()`. They require explicit non-follow classification and `lexists` checks.
- Manifest paths were unguarded and could overwrite existing material. CSV manifests must be created exclusively and must not alias any planned media source or destination.
- The old undo loops in reverse manifest order, which is not sufficient to restore a chain after dependency-ordered execution.
- `.github/workflows/python-app.yml` is a placeholder test job; `.github/workflows/python-publish.yml` is a template release-to-PyPI workflow unrelated to this app.
- `CURRENT_STATE.md` / ADR-005 describe native migration as proposed or awaiting kickoff, while session evidence records MP0–MP6 implementation. Formal operator acceptance has **not** been independently verified; don't retroactively invent it.
- Frozen Flutter code has the same status/undo defect, but changing it invalidates `blueprints/FLUTTER_REFERENCE_MANIFEST.json`. Preserve the original `flutter-final` tag.

### Required invariants

1. No source-byte rewrite and no replacement of an existing destination, including symlinks.
2. Default mode remains dry-run; only an explicit execution option mutates media names.
3. Empty-result and case-sensitive leading-only prefix rules remain unchanged.
4. Python and Dart **planning** semantics continue to match established goldens.
5. Manifest status is per operation: `planned`, `renamed`, `skipped`, or `error`; undo processes **only** `renamed` rows.
6. A dependency chain executes destination-first; undo restores in dependency order rather than simply reversing planned-row order.
7. A failed move, interrupted update, permission denial, collision, or unsupported filesystem must fail closed and surface diagnostics. A crash between media changes and manifest refresh still requires manual reconciliation; no false transactional guarantee.
8. Real device success is never claimed from host tests or synthetic files.

## 3. Work packages and exact files

| Package | Files | Implementation | Acceptance criteria |
|---|---|---|---|
| **A — Python safety** | `rename_camera_prefixes.py` | Per-op status; symlink-safe classification; separate unsupported counter; O(n) candidate lookup; deterministic tie sort; existing-target guard incl. dangling symlinks; chain dependency ordering; POSIX atomic hard-link no-clobber + unlink, Windows no-replace rename; status-filtered, dependency-aware undo; exclusive manifest creation, path alias guard; blocked partial runs exit nonzero | New pytest cases pass; synthetic source/destination SHA values unchanged except names; no overwrites |
| **B — Parity and regressions** | `tools/parity_probe.py`, **new** `tests/test_rename_safety.py` | Correct removed Dart fallback; exercise actual Python chain execution; test dry-run, status accuracy, partial execution, undo, race, symlinks, source/manifest aliasing | Python regression suite passes locally. Dart plan parity still must be run on user's host |
| **C — CI** | `.github/workflows/python-app.yml`, delete `python-publish.yml` | Focused Python syntax + pytest gate; no publishing, full Android/Flutter builds, or implicit packaging | Workflow no longer attempts PyPI publication; tests are real, nonempty |
| **D — Documentation** | `README.md`, `native/README.md`, `blueprints/CURRENT_STATE.md`, ADR-005, `SESSION_HANDOFF.md`, **new archive**, `.gitignore`, **new** `LICENSE`, `docs/LICENSE_DECISION.md` | Clear public README; repair blueprint link; archive long handoff verbatim; reconcile historical vs current native status without inventing approval; ignore overlay backups; conservative owner-rights notice pending explicit license selection | Pointers resolve, verified-vs-unverified gates explicit, archive retained |
| **E — Frozen Flutter correction (explicit opt-in only)** | `app/lib/adapters/local_rename_adapter.dart`, `app/lib/features/rename/rename_screen.dart`, `packages/rename_core/lib/src/manifest.dart` | Carry individual execution status into CSV; undo filters `renamed` and protects symlink destinations; new manifest file created exclusively | Requires a deliberate exception to frozen-reference contract, Dart tests, and baseline review; **not enabled in default application** |

**Licensing decision:** A public repository with no license is normally all-rights-reserved, not open source. The proposed `LICENSE` records that default rather than granting MIT/Apache/GPL permission without owner consent. This is an owner-review item, not a claim of OSI compliance.

## 4. ZIP overlay application on Windows

**This is a safe applicator overlay, not a blind overwrite.** Extracting it creates the new plan, tool and tests; existing source files are changed only by a single SHA-guarded command. That additional step is intentional: it prevents clobbering work that happened after this audit. Use Python already approved by your project rules. Do not run it on media folders.

```powershell
cd 'C:\vision engine'
git status --short
# Unzip Vision_Engine_Safety_Overlay.zip here, preserving subfolders.
python tools\apply_vision_engine_overlay.py --check
python tools\apply_vision_engine_overlay.py --apply
python -m pytest -q tests\test_rename_safety.py
python tools\parity_probe.py
# Only if an explicit decision is made to alter the frozen Flutter reference:
# python tools\apply_vision_engine_overlay.py --apply --include-frozen-flutter
```

The optional Flutter command **must be chosen at first application**. After normal application its Git-blob preconditions no longer match, so a second invocation isn't a partial-upgrade mechanism; restore from the snapshot and reapply with the option if needed. The tool records an automatically generated backup under `.vision_engine_overlay_backup/<UTC timestamp>/`, including the previous workflow and 380-line handoff. The backup location is ignored by Git. Use ordinary file restore / version control rollback if necessary; there is no automatic remote mutation.

**Preflight aborts without changes** if any targeted source differs from the GitHub baseline observed on 2026-10-13 (allowing safe CRLF normalization for Windows checkouts), is missing, or if a new file would overwrite an existing one. Read the abort carefully; never defeat the hash guard without re-auditing the changed file. `--check` is non-mutating. The `--apply` operation itself is not one cross-file atomic transaction: backup is completed first, then files are written via temporary siblings and atomic rename.

## 5. Verification matrix / gates

| Gate | Evidence to record | Status at package creation |
|---|---|---|
| A0: Baseline matches | SHA-guard `--check` | **Not run on user's local checkout** |
| A1: Python safety suite | `python -m pytest -q tests/test_rename_safety.py` | **13/13 passed in an isolated reconstructed logic harness; not executed against a complete checkout of the remote repository** |
| A2: Python↔Dart plan parity | `python tools/parity_probe.py` (Dart SDK required) | **Not run here: Dart executable unavailable** |
| A3: Frozen Flutter, if opted into | `dart analyze`, `dart test`, app static/unit tests; historical reference comparison; change-control acknowledgment | **Not verified** |
| A4: Native track | Existing `docs/device-validation-mp6.md` procedure on actual Moto G; capture device evidence | **BLOCKED/UNVERIFIED from prior state** |
| A5: Review/no unrelated mutations | `git diff --check`, `git diff --stat`, `git status --short`; ensure backup not staged | **Operator action** |

Do not mark G2/G5, MP6 device, MP7, or any production-safety gate green from this package alone. No real images were used in authoring.

### Additional cases for subsequent implementation

- Windows FAT/exFAT and Android SAF/provider-specific rename behavior is not fully established by Python POSIX host tests. Hard-link operations can fail on unsupported filesystems; fail closed, never fall back to a replacing rename.
- The frozen Dart reference's `File.renameSync()` has a theoretical destination race on POSIX/Android. The opt-in status patch improves auditability but **does not** make that API an atomic no-replace primitive. Do not promote reference demo execution into a production writer without a native no-clobber path and device test.
- Manifest rows can be forged or stale and contain paths. Only trust manifests generated by your own local tool, verify media content before legacy recovery, and retain backups.
- CSV refresh after execution is not a journaled transaction. Crash recovery from a stale `planned` row needs operator inspection.
- The `flutter-final` Git tag and hash manifest are historical evidence: if the optional Flutter patch is applied, the worktree's frozen-reference checker is expected to report mismatch. Do **not** silently rewrite hashes to make that checker green.
- Explicitly decide formal ADR-005 acceptance and future license before declaring repository governance complete.

## 6. Final handoff to coding agents

Treat `RULES.md` as authoritative. Read the plan and changes before applying. Never modify Sovereign-family reference repositories, global settings or media fixtures. Apply on the local Vision Engine repository only. Preserve the remote, history, build boundary and frozen tag. Stop on a hash mismatch or failed host test, report exact path/line and do not make an unverified status claim.

**Promotion criterion:** Owner reviews the diff and locally records passing regression/parity gates and any required device evidence. Only then is the correction ready for commit by explicit paths (not `git add -A`).
