#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""Apply a SHA-guarded, locally backed-up Vision Engine remediation overlay.

Usage, from repository root:
  python tools/apply_vision_engine_overlay.py --check
  python tools/apply_vision_engine_overlay.py --apply
  python tools/apply_vision_engine_overlay.py --apply --include-frozen-flutter

No dependency installs, no network, no builds, no git writes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent
BACKUP = ROOT / ".vision_engine_overlay_backup"
BASE = {
    "rename_camera_prefixes.py": "5c3893ae3d4f2b190607cb16c308842574b08619",
    "tools/parity_probe.py": "b4fbb3acf3a1ca1a39e9bba18913121a51025174",
    ".github/workflows/python-app.yml": "1168bd9ad833dbe8e52ca13af9c2dcc9eeeebbe1",
    ".github/workflows/python-publish.yml": "82f8dbd960d2ee51c15733a17ca612a00645c862",
    "README.md": "6c5ab12bbfab2a23510be66a0f33faaee6b83f33",
    "native/README.md": "549a9d7739ed545bf2276fbf3d69882b5b763613",
    "blueprints/CURRENT_STATE.md": "5d307720dd4ee987d9335d02d8cd524e234c4a2c",
    "blueprints/decisions/ADR-005-native-android-platform.md": "9b9bde0facd1357dbf37c0c2e5afdd2aaee308e9",
    "SESSION_HANDOFF.md": "25346933c548c889d82aa0e84ebcd33d369a1260",
    ".gitignore": "f443bf1ba49ae0e0709fa8136e93b17618fcf7b3",
}
FROZEN = {
    "app/lib/adapters/local_rename_adapter.dart": "0baebd0a465bfea21d5f7739a369aa5e008cc598",
    "app/lib/features/rename/rename_screen.dart": "7e37e866637dca3fc4a515df0768b7a30605f37d",
    "packages/rename_core/lib/src/manifest.dart": "21fa0357732f901776ffc7b2d4752b9ceff0c528",
}


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\x00" + data).hexdigest()


def replace_once(s: str, old: str, new: str, path: str) -> str:
    count = s.count(old)
    if count != 1:
        raise ValueError(f"{path}: expected exactly one matching patch anchor; found {count}: {old[:85]!r}")
    return s.replace(old, new, 1)


def patch_python(s: str) -> str:
    p = "rename_camera_prefixes.py"
    s = replace_once(s, "    dst: Path\n\n    @property", '    dst: Path\n    status: str = "planned"\n\n    @property', p)
    s = replace_once(s, "4. Never touches file contents, timestamps, or attributes — uses os.rename() only,\n   which on the same filesystem preserves all metadata.",
        "4. Never rewrites media bytes; Windows uses os.rename, POSIX uses atomic\n   no-clobber hard-link creation plus unlink (may temporarily show two links).", p)
    s = replace_once(s, "    skipped_no_prefix: int = 0\n", "    skipped_no_prefix: int = 0\n    skipped_unsupported: int = 0\n", p)
    s = replace_once(s, '(p for p in target_dir.rglob("*") if p.is_file() or p.is_dir()),', 'target_dir.rglob("*"),', p)
    s = replace_once(s, '''    # Pre-register existing names that will NOT be renamed, so we never
    # overwrite a file that is staying put.
    staying_names: set[str] = set()
''', '', p)
    s = replace_once(s, '''        if entry.is_dir():
            report.skipped_is_dir += 1
            continue
        if not entry.is_file():
            # Symlink / fifo / socket etc. — never touch.
            report.skipped_no_prefix += 1
            continue
''', '''        if entry.is_symlink():
            report.skipped_unsupported += 1
            continue
        if entry.is_dir():
            report.skipped_is_dir += 1
            continue
        if not entry.is_file():
            report.skipped_unsupported += 1
            continue
''', p)
    s = replace_once(s, '            staying_names.add(entry.name)\n', '', p)
    s = replace_once(s, '''    # Also register staying files' full paths for collision checks.
    staying_paths = {target_dir / n for n in staying_names}

''', '', p)
    s = replace_once(s, '''        if dst.exists() and dst not in [c[0] for c in candidates]:
''', '''        if os.path.lexists(dst) and dst not in candidate_sources:
''', p)
    # Destination dependency planning remains unchanged to preserve the frozen Dart golden plan.
    s = replace_once(s, '''        # Collision 2: destination equals a file that is staying put.
        if dst in staying_paths:
            report.skipped_collision.append(f"{src.name} -> {stripped_name} (target exists)")
            continue
''', '', p)
    s = replace_once(s, 'report.planned.sort(key=lambda op: op.old_name)', 'report.planned.sort(key=lambda op: (op.old_name, str(op.src)))', p)
    s = replace_once(s, '    for src, stripped_name in candidates:\n        dst = src.with_name(stripped_name)\n', '    candidate_sources = {item[0] for item in candidates}\n    for src, stripped_name in candidates:\n        dst = src.with_name(stripped_name)\n', p)
    start = s.index('def execute_plan(')
    stop = s.index('\n\n# ---------------------------------------------------------------------------\n# CLI', start)
    new = '''def _move_no_replace(src: Path, dst: Path) -> None:
    """Never overwrite: Windows rename is no-replace; POSIX link is atomic no-clobber.

    The POSIX link/unlink sequence may temporarily expose two hard links if
    interrupted, but never destroys an existing destination. Fails closed on
    filesystems that do not support hard links. Only regular files are accepted.
    """
    if src.is_symlink() or not src.is_file():
        raise OSError(f"source is not a regular, non-symlink file: {src}")
    if os.name == "nt":
        os.rename(src, dst)
        return
    os.link(src, dst, follow_symlinks=False)
    try:
        os.unlink(src)
    except OSError:
        # Keep the source and destination link as recoverable duplicates on
        # unlink failure; don't risk deleting a concurrently moved file.
        raise


def execute_plan(report: RenameReport, logger: logging.Logger) -> RenameReport:
    """Execute dependency-first so chain sources vacate occupied destinations."""
    pending = {op.src: op for op in report.planned}
    while pending:
        ready = sorted(
            (op for op in pending.values() if op.dst not in pending),
            key=lambda op: (op.old_name, str(op.src)),
        )
        if not ready:
            for op in pending.values():
                op.status = "skipped"
                report.skipped_collision.append(
                    f"{op.old_name} -> {op.new_name} (cyclic rename dependency)"
                )
            break
        for op in ready:
            try:
                if os.path.lexists(op.dst):
                    msg = f"SKIP (appeared during run): {op.old_name} -> {op.new_name} (target exists)"
                    logger.warning(msg)
                    op.status = "skipped"
                    report.skipped_collision.append(msg)
                else:
                    _move_no_replace(op.src, op.dst)
                    op.status = "renamed"
                    report.renamed += 1
                    logger.info(f"RENAMED: {op.old_name} -> {op.new_name}")
            except FileExistsError:
                msg = f"SKIP (atomic destination collision): {op.old_name} -> {op.new_name}"
                logger.warning(msg)
                op.status = "skipped"
                report.skipped_collision.append(msg)
            except OSError as exc:
                op.status = "error"
                msg = f"ERROR: {op.old_name} -> {op.new_name}: {exc}"
                logger.error(msg)
                report.errors.append(msg)
            finally:
                del pending[op.src]
    return report


def write_manifest(report: RenameReport, manifest_path: Path, *, overwrite: bool = False) -> None:
    """Write per-op audit statuses; never replace an unrelated manifest."""
    with manifest_path.open("w" if overwrite else "x", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=MANIFEST_FIELDNAMES)
        writer.writeheader()
        for op in report.planned:
            writer.writerow({
                "original_name": op.old_name,
                "new_name": op.new_name,
                "original_path": str(op.src),
                "new_path": str(op.dst),
                "status": op.status,
            })
        f.flush()
        os.fsync(f.fileno())


def undo_from_manifest(manifest_path: Path, logger: logging.Logger, dry_run: bool = True) -> Tuple[int, List[str]]:
    """Undo only rows verified marked renamed; never overwrite on recovery."""
    restored = 0
    errors: List[str] = []
    with manifest_path.open("r", newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    pending = {}
    virtual_vacated = set()
    for row in rows:
        if row.get("status") == "renamed":
            pending[row.get("new_path", "")] = row
        else:
            logger.info(f"SKIP manifest row (not renamed): {row.get('original_name', '?')}")
    while pending:
        ready = [r for r in reversed(list(pending.values()))
                 if r.get("original_path") not in pending]
        if not ready:
            errors.append("SKIP: cyclic undo dependencies; manual recovery required")
            break
        for row in ready:
            try:
                new_path = Path(row["new_path"])
                orig_path = Path(row["original_path"])
                if new_path.is_symlink() or not new_path.is_file():
                    errors.append(f"SKIP (not a regular source file): {row['new_name']}")
                    continue
                if os.path.lexists(orig_path) and str(orig_path) not in virtual_vacated:
                    errors.append(f"SKIP (original already exists): {row['original_name']}")
                    continue
                if dry_run:
                    logger.info(f"WOULD RESTORE: {new_path.name} -> {orig_path.name}")
                    virtual_vacated.add(str(new_path))
                    restored += 1
                else:
                    _move_no_replace(new_path, orig_path)
                    logger.info(f"RESTORED: {new_path.name} -> {orig_path.name}")
                    restored += 1
            except (OSError, KeyError, ValueError) as exc:
                errors.append(f"ERROR restoring {row.get('new_name', '?')}: {exc}")
            finally:
                pending.pop(row.get("new_path", ""), None)
    return restored, errors
'''
    s = s[:start] + new + s[stop:]
    s = replace_once(s, '''                f"already_clean={report.skipped_no_prefix} | dirs_skipped={report.skipped_is_dir}")''',
       '''                f"already_clean={report.skipped_no_prefix} | dirs_skipped={report.skipped_is_dir} "
                f"unsupported_skipped={report.skipped_unsupported}")''', p)
    s = replace_once(s, '''    if args.manifest:
        write_manifest(report, Path(args.manifest))
        logger.info(f"Manifest written: {args.manifest}")
''', '''    manifest_path = Path(args.manifest) if args.manifest else None
    if manifest_path:
        planned_paths = {os.path.normcase(os.path.abspath(path))
                         for op in report.planned for path in (op.src, op.dst)}
        if os.path.normcase(os.path.abspath(manifest_path)) in planned_paths:
            logger.error("ERROR: manifest path aliases a planned media source/destination")
            return 2
        try:
            write_manifest(report, manifest_path)
        except OSError as exc:
            logger.error(f"ERROR: cannot create a new manifest without overwrite: {exc}")
            return 2
        logger.info(f"Manifest written: {manifest_path}")
''', p)
    s = replace_once(s, '''    report = execute_plan(report, logger)
    logger.info(f"DONE: renamed={report.renamed}/{report.to_rename} "''', '''    report = execute_plan(report, logger)
    if manifest_path:
        try:
            write_manifest(report, manifest_path, overwrite=True)
        except OSError as exc:
            logger.error(f"ERROR: manifest refresh failed (review recovery manually): {exc}")
            report.errors.append(f"manifest refresh failed: {exc}")
    logger.info(f"DONE: renamed={report.renamed}/{report.to_rename} "''', p)
    s = replace_once(s,
        '    if not report.planned:\n        logger.info("Nothing to rename — all files already clean.")\n        return 0\n',
        '    if not report.planned:\n        logger.info("Nothing to rename — no executable operations remain.")\n        return 2 if report.skipped_collision or report.skipped_empty_result else 0\n', p)
    s = replace_once(s, '    return 0 if not report.errors else 2\n', '    return 0 if not report.errors and not report.skipped_collision else 2\n', p)
    return s


def patch_parity(s: str) -> str:
    p = "tools/parity_probe.py"
    # Extend execution proof: bytes/locations after an intentional chain.
    s = replace_once(s, '''    print(f"parity: {'GREEN' if failures == 0 else 'RED'} "''', '''    with tempfile.TemporaryDirectory(prefix="ve-chain-") as tmp:
        target = Path(tmp)
        (target / "IMG_VID_a.jpg").write_bytes(b"first")
        (target / "VID_a.jpg").write_bytes(b"second")
        chain = truth.build_plan(target)
        truth.execute_plan(chain, logging.getLogger("parity-chain"))
        chain_ok = (
            (target / "VID_a.jpg").read_bytes() == b"first"
            and (target / "a.jpg").read_bytes() == b"second"
            and chain.renamed == 2
        )
        print(f"[chain_execution] {'PASS' if chain_ok else 'FAIL'}")
        failures += 0 if chain_ok else 1

    print(f"parity: {'GREEN' if failures == 0 else 'RED'} "''', p)
    s = replace_once(s, 'fallback = Path(r"C:\\src\\flutter\\bin\\dart.bat")',
        'fallback = Path(r"C:\\android\\flutter\\bin\\dart.bat")', p)
    return s


def patch_dart_adapter(s: str) -> str:
    p = "app/lib/adapters/local_rename_adapter.dart"
    s = replace_once(s, '    required this.errors,\n  });\n\n  final int renamed;', '    required this.errors,\n    required this.statusesBySource,\n  });\n\n  final Map<String, String> statusesBySource;\n  final int renamed;', p)
    begin = s.index("  ExecuteResult execute(RenameReport report) {")
    finish = s.index("  UndoResult undoRows(", begin)
    execute = "  ExecuteResult execute(RenameReport report) {\n    int renamed = 0;\n    int skipped = 0;\n    final List<String> errors = <String>[];\n    final Map<String, String> statuses = <String, String>{};\n    final Map<String, RenameOp> pending = <String, RenameOp>{\n      for (final RenameOp op in report.planned) op.srcPath: op,\n    };\n    while (pending.isNotEmpty) {\n      final List<RenameOp> ready = pending.values\n          .where((RenameOp op) => !pending.containsKey(op.dstPath))\n          .toList();\n      if (ready.isEmpty) {\n        for (final RenameOp op in pending.values) {\n          statuses[op.srcPath] = 'skipped';\n          report.skippedCollision.add('${op.oldName} -> ${op.newName} (cyclic dependency)');\n          skipped += 1;\n        }\n        break;\n      }\n      ready.sort((RenameOp a, RenameOp b) => a.oldName.compareTo(b.oldName));\n      for (final RenameOp op in ready) {\n        try {\n          if (FileSystemEntity.typeSync(op.srcPath, followLinks: false) != FileSystemEntityType.file) {\n            statuses[op.srcPath] = 'error';\n            errors.add('ERROR: source is not a regular file: ${op.oldName}');\n          } else if (FileSystemEntity.typeSync(op.dstPath, followLinks: false) != FileSystemEntityType.notFound) {\n            statuses[op.srcPath] = 'skipped';\n            report.skippedCollision.add('SKIP (target exists): ${op.oldName} -> ${op.newName}');\n            skipped += 1;\n          } else {\n            File(op.srcPath).renameSync(op.dstPath);\n            statuses[op.srcPath] = 'renamed';\n            renamed += 1;\n          }\n        } on FileSystemException catch (e) {\n          statuses[op.srcPath] = 'error';\n          errors.add('ERROR: ${op.oldName} -> ${op.newName}: ${e.message}');\n        } finally {\n          pending.remove(op.srcPath);\n        }\n      }\n    }\n    return ExecuteResult(renamed: renamed, skipped: skipped,\n        errors: errors, statusesBySource: statuses);\n  }\n\n"
    s = s[:begin] + execute + s[finish:]
    begin = s.index("  UndoResult undoRows(")
    finish = s.index("\n  }\n}", begin) + len("\n  }")
    undo = "  UndoResult undoRows(List<ManifestRow> rows, {bool dryRun = false}) {\n    int restored = 0;\n    int skipped = 0;\n    final List<String> errors = <String>[];\n    final Map<String, ManifestRow> pending = <String, ManifestRow>{\n      for (final ManifestRow row in rows)\n        if (row.status == 'renamed') row.newPath: row,\n    };\n    skipped += rows.length - pending.length;\n    final Set<String> vacated = <String>{};\n    while (pending.isNotEmpty) {\n      final List<ManifestRow> ready = pending.values\n          .where((ManifestRow row) => !pending.containsKey(row.originalPath))\n          .toList()\n          .reversed\n          .toList();\n      if (ready.isEmpty) {\n        errors.add('ERROR: cyclic undo dependencies, manual recovery required');\n        skipped += pending.length;\n        break;\n      }\n      for (final ManifestRow row in ready) {\n        try {\n          final File src = File(row.newPath);\n          final File dst = File(row.originalPath);\n          if (FileSystemEntity.typeSync(src.path, followLinks: false) != FileSystemEntityType.file) {\n            errors.add('SKIP (not found or symlink): ${row.newName}');\n            skipped += 1;\n          } else if (FileSystemEntity.typeSync(dst.path, followLinks: false) != FileSystemEntityType.notFound && !vacated.contains(dst.path)) {\n            errors.add('SKIP (original already exists): ${row.originalName}');\n            skipped += 1;\n          } else if (dryRun) {\n            restored += 1;\n            vacated.add(src.path);\n          } else {\n            src.renameSync(dst.path);\n            restored += 1;\n          }\n        } on FileSystemException catch (e) {\n          errors.add('ERROR restoring ${row.newName}: ${e.message}');\n          skipped += 1;\n        } finally {\n          pending.remove(row.newPath);\n        }\n      }\n    }\n    return UndoResult(restored: restored, skipped: skipped, errors: errors);\n  }"
    s = s[:begin] + undo + s[finish:]
    return s


def patch_dart_manifest(s: str) -> str:
    p = "packages/rename_core/lib/src/manifest.dart"
    s = replace_once(s, '''  String status = 'planned',
})''', '''  String status = 'planned',
  Map<String, String> statusesBySource = const <String, String>{},
})''', p)
    s = replace_once(s, '      _escape(status),', "      _escape(statusesBySource[op.srcPath] ?? status),", p)
    return s


def patch_dart_screen(s: str) -> str:
    p = "app/lib/features/rename/rename_screen.dart"
    s = replace_once(s, '''    File(path).writeAsStringSync(
      manifestToCsv(plan.planned, status: 'renamed'),
    );''', '''    try {
      File(path).createSync(exclusive: true);
      File(path).writeAsStringSync(
        manifestToCsv(plan.planned, statusesBySource: result.statusesBySource),
      );
    } on FileSystemException catch (e) {
      if (!mounted) return;
      setState(() {
        _log = 'Renamed=${result.renamed}, but manifest write failed: ${e.message}. Manual recovery required.';
      });
      return;
    }''', p)
    return s


def patch_current_state(s: str) -> str:
    p = "blueprints/CURRENT_STATE.md"
    s = replace_once(s, '## Snapshot (2026-09-14, scaffold session)', '## Historical snapshot (2026-09-14 scaffold; subsequent dated entries below)', p)
    s = replace_once(s, '''  NO implementation started; 2D stays locked until kickoff.''', '''  HISTORICAL ONLY: at that moment no native implementation had started;
  subsequent MP0–MP6 implementation below supersedes this line.''', p)
    s = replace_once(s, '''- ADR-005 PROPOSED 2026-09-15 (GQ8=NO → native Kotlin + Compose target,
  Flutter frozen reference, MP0–MP9 sequence; awaits kickoff).''', '''- ADR-005 recorded as PROPOSED on 2026-09-15; MP0–MP6 subsequently
  progressed on the native track. Formal acceptance signature not evidenced.
  See decision record for the documented implementation-vs-approval gap.''', p)
    s += '''\n## Remediation work order (2026-10-13; proposed, not yet device-proven)\n\n- Python rename correctness/undo safety, chain ordering, CI, docs and tests:\n  `blueprints/VISION_ENGINE_SAFETY_REMEDIATION_PLAN.md`. Applying an overlay\n  does not constitute a host or device validation pass.\n- Frozen Flutter baseline intentionally not edited by default. Optional\n  legacy reference patch requires explicit `--include-frozen-flutter` and\n  invalidates the worktree's `verify_reference.py` check until reconciled.\n- Native MP6 device test remains **BLOCKED/UNVERIFIED**; MP7 Viewer not begun.\n'''
    return s


def patch_adr(s: str) -> str:
    p = "blueprints/decisions/ADR-005-native-android-platform.md"
    return replace_once(s, '''> Status: PROPOSED 2026-09-15 (records GQ8=NO and the authorized target;
> implementation awaits a migration kickoff authorization).''', '''> Status: RECORDED AS PROPOSED 2026-09-15. Subsequent repository evidence\n> documents MP0–MP6 implementation; this heading was stale.\n> Formal decision acceptance/signature is not independently established.\n> Native migration IN PROGRESS; MP6 device verification BLOCKED, MP7 not begun.''', p)


def patch_native_readme(s: str) -> str:
    p = "native/README.md"
    s = replace_once(s, '''`blueprints/NATIVE_ANDROID_MIGRATION_\n> BLUEPRINT.md`''', '''`blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`''', p)
    s = replace_once(s, '''# native/ — Vision Engine pure-native Android track (MP1 foundation)''', '''# native/ — Vision Engine native Kotlin / Compose migration (MP6 host-complete)''', p)
    return s


def patch_gitignore(s: str) -> str:
    return s + '\n# Local safety overlay rollback snapshots (never commit)\n.vision_engine_overlay_backup/\n'


CI = '''name: Vision Engine Python safety checks
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
permissions:
  contents: read
jobs:
  python-safety:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.13'
      - name: Install host test runner
        run: python -m pip install pytest
      - name: Check Python syntax
        run: python -m py_compile rename_camera_prefixes.py tools/parity_probe.py
      - name: Safety regression tests (no media and no Android builds)
        run: python -m pytest -q tests/test_rename_safety.py
'''
README = '''# Vision Engine

**Vision Engine** is an offline-first Android media inspection and safe-renaming
project. The active target is native Kotlin + Jetpack Compose (`native/`),
following a frozen Flutter reference implementation (`app/`, `packages/`).
The native media pipeline is host-tested through **MP6**, but its device test
has **not** passed; **MP7 native Viewer has not started**. The product is a
work in progress and is **not** ready for irreplaceable media archives.

## What's here

- `rename_camera_prefixes.py` — standalone Python leading-camera-prefix
  renamer (dry-run by default, CSV manifest and status-filtered undo).
- `tools/parity_probe.py` — Python ↔ pure-Dart plan parity; requires Dart.
- `packages/` + `app/` — historical frozen Flutter behavior/reference;
  see `flutter-final` tag and `tools/verify_reference.py`.
- `native/vision-core`, `native/vision-android`, `native/vision-app` —
  native migration track (Kotlin core, Android adapters, Compose shell).
- `RULES.md`, `AGENTS.md`, `blueprints/` — operating contract and phase plans.

## Python renamer (synthetic test folder first)

```shell
python rename_camera_prefixes.py /path/to/camera            # preview only
python rename_camera_prefixes.py /path/to/camera --execute --manifest /path/to/NEW_manifest.csv
python rename_camera_prefixes.py /path/to/camera --undo /path/to/NEW_manifest.csv # preview undo
python rename_camera_prefixes.py /path/to/camera --undo /path/to/NEW_manifest.csv --execute
python -m pytest -q tests/test_rename_safety.py
```

Only `IMG_` and `VID_` at the beginning are removed (case-sensitive by
default). Renames never rewrite media bytes. Existing destinations are not
replaced; occupied-name dependency chains are executed destination-first.
On POSIX, source and destination may temporarily coexist as hard links; the
operation fails closed if the filesystem disallows hard links. The manifest
is a **new path** only and records each planned operation's actual outcome.
Legacy manifests must be reviewed before undo: older versions could
mislabel planned operations as renamed.

**Important:** This tool does not provide transactional multi-file rollbacks
and does not solve every adversarial filesystem race. Always back up your
media and validate a test directory before changing your archive.

## Verification and migration

Start with `RULES.md`, then `SESSION_HANDOFF.md`,
`blueprints/CURRENT_STATE.md`, and
`blueprints/VISION_ENGINE_SAFETY_REMEDIATION_PLAN.md`. Do not infer device
success from unit tests. Kotlin builds/device runs remain operator-controlled.

## License

All rights reserved unless an explicit license is selected by the owner.
The `LICENSE` file records that conservative default; it is **not** an
open-source grant. See `docs/LICENSE_DECISION.md` before redistributing.
'''
LICENSE = '''Copyright (c) 2026 GhostMan612. All rights reserved.

No license to copy, modify, sublicense, distribute, or commercially exploit
this repository is granted by this notice. Rights may be granted in a
separate explicit written agreement or a future license selected by the owner.

This is a rights notice, not an OSI-approved open-source license.
'''
LICENSE_DECISION = '''# Licensing decision — owner action required

This remediation overlay deliberately **does not** grant an MIT, Apache-2.0,
GPL, or other permissive/copyleft license. The repository did not have a
LICENSE file; automatically granting rights would be a material legal change.

The overlay installs a conservative All Rights Reserved notice, reflecting
the default for an unlicensed repository. This does not make the repository
open source. The owner can replace it with a chosen license after reviewing
third-party code, asset provenance, warranty limitations and desired patent
terms. The `README.md` licensing section must be updated at the same time.
'''


def compact_handoff(original: str) -> tuple[str, str]:
    head = '''# SESSION_HANDOFF.md — Vision Engine (active handoff)

> Operating law: `RULES.md`; detail: `blueprints/CURRENT_STATE.md`.
> Historical handoff preserved verbatim in `blueprints/_archive/SESSION_HANDOFF_PRE_REMEDIATION.md`.

## Status at 2026-10-13 overlay authoring (not a new device proof)

- Native MP0–MP6 host work is recorded; MP6 `PipelineDeviceTest` authored
  and compiled, but device execution BLOCKED (not failed, not passed).
- MP7 Viewer not implemented or authorized by applying this overlay.
- Python renamer safety remediation is packaged; its completion requires
  running the included host regression suite and reviewing changes.
- Flutter reference `flutter-final` stays frozen by default; use explicit
  `--include-frozen-flutter` only with baseline reconciliation.
- ADR-005's original PROPOSED status conflicted with the executed MP work;
  acceptance signature not evidenced. Decision record now explains gap.
- Legacy manifests (`status=renamed`) are unreliable until independently
  cross-checked against media and recorded operations.

## Next actions

1. Execute Python regression tests using an isolated environment.
2. Run the host Dart parity probe; where possible run the app/package tests
   only if explicitly permitting the frozen reference correction.
3. Verify that no real media, external directories, or GitHub remote changed.
4. Reconnect Moto G, execute the existing MP6 device test runbook; do not
   mark MP6 device green until device evidence is produced.
5. Decide owner license and formal ADR-005 status; MP7 remains a separate
   explicit authorization.

## Important paths

- `RULES.md` and `AGENTS.md`: governing instructions.
- `blueprints/CURRENT_STATE.md`: evidence and K-registry.
- `blueprints/VISION_ENGINE_SAFETY_REMEDIATION_PLAN.md`: correction work order.
- `blueprints/_archive/SESSION_HANDOFF_PRE_REMEDIATION.md`: prior 380-line
  handoff (verbatim historical reference).
'''
    return head, original


def transformations(include_frozen: bool) -> dict[str, object]:
    base = {
        "rename_camera_prefixes.py": patch_python,
        "tools/parity_probe.py": patch_parity,
        ".github/workflows/python-app.yml": lambda _: CI,
        ".github/workflows/python-publish.yml": lambda _: None,
        "README.md": lambda _: README,
        "native/README.md": patch_native_readme,
        "blueprints/CURRENT_STATE.md": patch_current_state,
        "blueprints/decisions/ADR-005-native-android-platform.md": patch_adr,
        ".gitignore": patch_gitignore,
        "SESSION_HANDOFF.md": lambda x: compact_handoff(x)[0],
    }
    if include_frozen:
        base.update({
            "app/lib/adapters/local_rename_adapter.dart": patch_dart_adapter,
            "app/lib/features/rename/rename_screen.dart": patch_dart_screen,
            "packages/rename_core/lib/src/manifest.dart": patch_dart_manifest,
        })
    return base


def prepare(include_frozen: bool) -> tuple[dict[str, bytes | None], list[str]]:
    change: dict[str, bytes | None] = {}
    problems = []
    for path, fn in transformations(include_frozen).items():
        target = ROOT / path
        if not target.is_file() or target.is_symlink():
            problems.append(f"MISSING/UNSAFE: {path}")
            continue
        raw = target.read_bytes()
        expected = (FROZEN if path in FROZEN else BASE)[path]
        canonical = raw.replace(b"\r\n", b"\n")
        if git_blob(canonical) != expected:
            problems.append(f"BASELINE CHANGED: {path} (expected {expected}, got {git_blob(canonical)})")
            continue
        try:
            transformed = fn(canonical.decode("utf-8"))
            if transformed is None:
                change[path] = None
            else:
                output = transformed.encode("utf-8")
                if b"\r\n" in raw:
                    output = output.replace(b"\n", b"\r\n")
                change[path] = output
        except (ValueError, UnicodeError) as exc:
            problems.append(str(exc))
    if not problems:
        archive = ROOT / "SESSION_HANDOFF.md"
        change["blueprints/_archive/SESSION_HANDOFF_PRE_REMEDIATION.md"] = archive.read_bytes()
        for path, value in {
            "LICENSE": LICENSE,
            "docs/LICENSE_DECISION.md": LICENSE_DECISION,
        }.items():
            target = ROOT / path
            if target.exists() or target.is_symlink():
                problems.append(f"NEW-FILE COLLISION: {path}")
            change[path] = value.encode("utf-8")
        if include_frozen:
            problems.append("FROZEN FLUTTER MODIFICATIONS: requires separate golden-reference reconciliation and verification. This is intentional, not silently passed.")
    return change, problems


def write_atomically(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=".ve-overlay-", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
        os.replace(temp_name, path)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def apply(changes: dict[str, bytes | None]) -> Path:
    BACKUP.mkdir(exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    snapshot = BACKUP / timestamp
    snapshot.mkdir()
    record: list[dict[str, object]] = []
    # Backup every file *before* modifying anything.
    for relative, output in changes.items():
        source = ROOT / relative
        existed = source.is_file()
        if existed:
            dest = snapshot / relative
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, dest)
        record.append({"path": relative, "existed": existed,
                       "action": "delete" if output is None else "write"})
    (snapshot / "manifest.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
    for relative, output in changes.items():
        target = ROOT / relative
        if output is None:
            target.unlink()
        else:
            write_atomically(target, output)
    return snapshot


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--apply", action="store_true")
    parser.add_argument("--include-frozen-flutter", action="store_true",
                        help="Explicitly alter historical Flutter reference; invalidates pinned hashes")
    args = parser.parse_args(argv)
    if not (ROOT / "RULES.md").is_file():
        print("ABORT: extract overlay into the root of Vision Engine", file=sys.stderr)
        return 2
    changes, issues = prepare(args.include_frozen_flutter)
    warnings = [issue for issue in issues if issue.startswith("FROZEN FLUTTER MODIFICATIONS:")]
    fatal = [issue for issue in issues if issue not in warnings]
    for issue in fatal:
        print(f"ABORT: {issue}", file=sys.stderr)
    for warning in warnings:
        print(f"WARNING: {warning}")
    if fatal:
        print("ABORTED; no repository files modified.", file=sys.stderr)
        return 2
    print("Files to update/create/delete:")
    for path, data in changes.items():
        print("  " + ("DELETE " if data is None else "WRITE  ") + path)
    if args.check:
        print("CHECK PASSED: original Git blobs match; no files changed")
        return 0
    backup = apply(changes)
    print(f"APPLIED: original files backed up at {backup.relative_to(ROOT)}")
    print("NOW RUN: python -m pytest -q tests/test_rename_safety.py")
    print("THEN: python tools/parity_probe.py (requires Dart)")
    if args.include_frozen_flutter:
        print("WARNING: Flutter freeze manifest check is EXPECTED TO FAIL until formally reconciled")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
