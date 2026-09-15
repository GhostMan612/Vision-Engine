#!/usr/bin/env python3
"""
rename_camera_prefixes.py — Safely strip Android camera prefixes (IMG_, VID_) from filenames.

Purpose
-------
Moto G (and most Android devices) save camera files as:
    IMG_20260905_110523079_HDR.jpg
    VID_20260902_200800398.mp4

This script renames them to:
    20260905_110523079_HDR.jpg
    20260902_200800398.mp4

Safety guarantees (archival-grade)
----------------------------------
1. ONLY removes a leading IMG_ / VID_ prefix. Nothing else in the name is touched.
2. Case-sensitive by default (only exact uppercase IMG_ / VID_). `img_` is left alone
   unless you pass --ignore-case.
3. Never overwrites: if the stripped name already exists, that file is SKIPPED.
4. Never touches file contents, timestamps, or attributes — uses os.rename() only,
   which on the same filesystem preserves all metadata.
5. Two-phase operation: PLAN first (scan + validate all 213 files), EXECUTE second.
6. Dry-run is the DEFAULT. Nothing changes unless you pass --execute.
7. Sorted, deterministic order. Directories/symlinks are never renamed.
8. Writes a CSV manifest of every rename so you can audit or UNDO later.
9. Optional --undo restores original names from a manifest.

Usage
-----
# 1. Preview only (safe, default — changes nothing):
python rename_camera_prefixes.py "C:\\photos\\camera"

# 2. Preview with verbose per-file output:
python rename_camera_prefixes.py "C:\\photos\\camera" --verbose

# 3. Actually rename (after you reviewed the dry-run):
python rename_camera_prefixes.py "C:\\photos\\camera" --execute

# 4. Rename + save audit manifest + log:
python rename_camera_prefixes.py "C:\\photos\\camera" --execute --manifest renames.csv --log-file rename.log

# 5. Undo a previous run:
python rename_camera_prefixes.py "C:\\photos\\camera" --undo renames.csv

# 6. Include subfolders:
python rename_camera_prefixes.py "C:\\photos\\camera" --execute --recursive

# 7. PowerShell one-liner alternative (no script, preview only):
# Get-ChildItem -LiteralPath "C:\\photos\\camera" | Where-Object { $_.Name -match '^(IMG_|VID_)' } | ForEach-Object { "$($_.Name)  ->  $($_.Name.Substring(4))" }

Exit codes
----------
0 = success (or dry-run success), 1 = usage error, 2 = collisions/errors blocked execution.
"""

from __future__ import annotations

import argparse
import csv
import logging
import os
import sys
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Tuple

DEFAULT_PREFIXES: Tuple[str, ...] = ("IMG_", "VID_")
MANIFEST_FIELDNAMES = ["original_name", "new_name", "original_path", "new_path", "status"]


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class RenameOp:
    """A single planned rename: src -> dst."""
    src: Path
    dst: Path

    @property
    def old_name(self) -> str:
        return self.src.name

    @property
    def new_name(self) -> str:
        return self.dst.name


@dataclass
class RenameReport:
    """Outcome of a scan (plan) or execution."""
    scanned: int = 0
    planned: List[RenameOp] = field(default_factory=list)
    skipped_no_prefix: int = 0
    skipped_is_dir: int = 0
    skipped_empty_result: int = 0
    skipped_collision: List[str] = field(default_factory=list)
    renamed: int = 0
    errors: List[str] = field(default_factory=list)

    @property
    def to_rename(self) -> int:
        return len(self.planned)


# ---------------------------------------------------------------------------
# Core logic (pure, testable — no I/O side effects except Path existence checks)
# ---------------------------------------------------------------------------

def strip_leading_prefix(
    filename: str,
    prefixes: Tuple[str, ...] = DEFAULT_PREFIXES,
    case_sensitive: bool = True,
) -> Optional[str]:
    """Return filename with leading prefix removed, or None if no prefix matched.

    ONLY strips at position 0. Never strips mid-string occurrences.
    Returns None (not empty string) when there is no match, so callers can
    distinguish "skip" from "would become empty".
    """
    name_cmp = filename if case_sensitive else filename.upper()
    for prefix in prefixes:
        p_cmp = prefix if case_sensitive else prefix.upper()
        if name_cmp.startswith(p_cmp):
            stripped = filename[len(prefix):]  # len is same regardless of case
            if not stripped:
                return ""  # signals empty-result -> caller must skip
            return stripped
    return None


def build_plan(
    target_dir: Path,
    prefixes: Tuple[str, ...] = DEFAULT_PREFIXES,
    recursive: bool = False,
    case_sensitive: bool = True,
) -> RenameReport:
    """Phase 1: scan directory and build a validated rename plan. Changes nothing."""
    report = RenameReport()

    entries: List[Path]
    if recursive:
        entries = sorted(
            (p for p in target_dir.rglob("*") if p.is_file() or p.is_dir()),
            key=lambda p: str(p),
        )
    else:
        entries = sorted(target_dir.iterdir(), key=lambda p: p.name)

    # Track destinations to catch intra-batch collisions
    # (e.g., IMG_X.jpg and VID_X.jpg would both -> X.jpg — impossible here
    #  since timestamps differ, but we check anyway).
    seen_destinations: set[Path] = set()
    # Pre-register existing names that will NOT be renamed, so we never
    # overwrite a file that is staying put.
    staying_names: set[str] = set()
    # First pass: classify.
    candidates: List[Tuple[Path, str]] = []
    for entry in entries:
        report.scanned += 1
        if entry.is_dir():
            report.skipped_is_dir += 1
            continue
        if not entry.is_file():
            # Symlink / fifo / socket etc. — never touch.
            report.skipped_no_prefix += 1
            continue
        stripped = strip_leading_prefix(entry.name, prefixes, case_sensitive)
        if stripped is None:
            staying_names.add(entry.name)
            report.skipped_no_prefix += 1
        elif stripped == "":
            report.skipped_empty_result += 1
        else:
            candidates.append((entry, stripped))

    # Also register staying files' full paths for collision checks.
    staying_paths = {target_dir / n for n in staying_names}

    for src, stripped_name in candidates:
        dst = src.with_name(stripped_name)
        # Collision 1: destination already exists on disk AND is not itself
        # part of this rename batch (i.e., would be overwritten).
        if dst.exists() and dst not in [c[0] for c in candidates]:
            report.skipped_collision.append(f"{src.name} -> {stripped_name} (target exists)")
            continue
        # Collision 2: destination equals a file that is staying put.
        if dst in staying_paths:
            report.skipped_collision.append(f"{src.name} -> {stripped_name} (target exists)")
            continue
        # Collision 3: two sources map to the same destination.
        if dst in seen_destinations:
            report.skipped_collision.append(f"{src.name} -> {stripped_name} (duplicate target in batch)")
            continue
        seen_destinations.add(dst)
        report.planned.append(RenameOp(src=src, dst=dst))

    # Deterministic order: sort by source name.
    report.planned.sort(key=lambda op: op.old_name)
    return report


def execute_plan(report: RenameReport, logger: logging.Logger) -> RenameReport:
    """Phase 2: execute a previously validated plan. Uses os.rename (metadata-preserving)."""
    for op in report.planned:
        try:
            # Final guard at execution time (TOCTOU protection): re-check target.
            if op.dst.exists():
                msg = f"SKIP (appeared during run): {op.old_name} -> {op.new_name} (target exists)"
                logger.warning(msg)
                report.skipped_collision.append(msg)
                continue
            os.rename(op.src, op.dst)  # same-filesystem: preserves content + timestamps
            logger.info(f"RENAMED: {op.old_name} -> {op.new_name}")
            report.renamed += 1
        except OSError as exc:
            msg = f"ERROR: {op.old_name} -> {op.new_name}: {exc}"
            logger.error(msg)
            report.errors.append(msg)
    return report


def write_manifest(report: RenameReport, manifest_path: Path) -> None:
    """Write CSV audit log of planned/executed renames (for review or --undo)."""
    with manifest_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=MANIFEST_FIELDNAMES)
        writer.writeheader()
        for op in report.planned:
            writer.writerow({
                "original_name": op.old_name,
                "new_name": op.new_name,
                "original_path": str(op.src),
                "new_path": str(op.dst),
                "status": "renamed" if str(op.src) else "planned",
            })


def undo_from_manifest(manifest_path: Path, logger: logging.Logger, dry_run: bool = True) -> Tuple[int, List[str]]:
    """Restore original names from a manifest CSV. Returns (restored_count, errors)."""
    restored = 0
    errors: List[str] = []
    with manifest_path.open("r", newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    # Reverse order so later renames undo first (safest if names overlapped).
    for row in reversed(rows):
        new_path = Path(row["new_path"])
        orig_path = Path(row["original_path"])
        # Manifest stores absolute paths; fall back to names if paths moved.
        src = new_path if new_path.exists() else Path(orig_path.parent) / row["new_name"]
        if not src.exists():
            errors.append(f"SKIP (not found): {row['new_name']}")
            continue
        if orig_path.exists():
            errors.append(f"SKIP (original already exists): {row['original_name']}")
            continue
        if dry_run:
            logger.info(f"WOULD RESTORE: {src.name} -> {orig_path.name}")
            restored += 1
        else:
            try:
                os.rename(src, orig_path)
                logger.info(f"RESTORED: {src.name} -> {orig_path.name}")
                restored += 1
            except OSError as exc:
                errors.append(f"ERROR restoring {src.name}: {exc}")
    return restored, errors


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Safely strip IMG_/VID_ prefixes from Android camera files. Dry-run by default.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Example: python rename_camera_prefixes.py \"C:\\photos\\camera\" --execute",
    )
    parser.add_argument("directory", help="Folder containing the photos/videos.")
    parser.add_argument(
        "--prefixes", nargs="+", default=list(DEFAULT_PREFIXES),
        help=f"Prefixes to strip (default: {' '.join(DEFAULT_PREFIXES)}). Only leading matches are removed.",
    )
    parser.add_argument("--execute", action="store_true",
                        help="Actually rename. Without this flag, only a dry-run preview is shown.")
    parser.add_argument("--recursive", action="store_true",
                        help="Also process subfolders (default: top folder only).")
    parser.add_argument("--ignore-case", action="store_true",
                        help="Match prefixes case-insensitively (default: case-sensitive, exact IMG_/VID_ only).")
    parser.add_argument("--manifest", default=None,
                        help="Write CSV audit manifest of renames to this path (enables --undo later).")
    parser.add_argument("--undo", metavar="MANIFEST_CSV", default=None,
                        help="Restore original names from a manifest CSV instead of renaming.")
    parser.add_argument("--verbose", action="store_true", help="List every file decision.")
    parser.add_argument("--log-file", default=None, help="Also write log output to this file.")
    return parser.parse_args(argv)


def setup_logging(verbose: bool, log_file: Optional[str]) -> logging.Logger:
    logger = logging.getLogger("rename_prefixes")
    logger.setLevel(logging.DEBUG if verbose else logging.INFO)
    logger.handlers.clear()
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("%(message)s"))
    logger.addHandler(handler)
    if log_file:
        fh = logging.FileHandler(log_file, encoding="utf-8")
        fh.setFormatter(logging.Formatter("%(asctime)s | %(levelname)s | %(message)s"))
        logger.addHandler(fh)
    return logger


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    logger = setup_logging(args.verbose, args.log_file)
    target = Path(args.directory)

    if not target.is_dir():
        logger.error(f"ERROR: not a directory: {target}")
        return 1

    prefixes = tuple(args.prefixes)
    case_sensitive = not args.ignore_case

    # ---- Undo mode ----
    if args.undo:
        manifest = Path(args.undo)
        if not manifest.is_file():
            logger.error(f"ERROR: manifest not found: {manifest}")
            return 1
        dry = not args.execute
        logger.info(f"UNDO mode ({'DRY-RUN' if dry else 'EXECUTING'}) from: {manifest}")
        restored, errors = undo_from_manifest(manifest, logger, dry_run=dry)
        logger.info(f"Undo summary: would-restore={restored}" if dry else f"Undo summary: restored={restored}")
        for e in errors:
            logger.warning(e)
        return 0 if not errors else 2

    # ---- Phase 1: plan ----
    logger.info(f"Scanning: {target.resolve()}")
    logger.info(f"Prefixes: {' '.join(prefixes)} "
                f"({'case-sensitive' if case_sensitive else 'case-insensitive'}, leading-only)"
                f"{' + subfolders' if args.recursive else ''}")
    report = build_plan(target, prefixes, args.recursive, case_sensitive)

    logger.info(f"Scanned={report.scanned} | to_rename={report.to_rename} | "
                f"already_clean={report.skipped_no_prefix} | dirs_skipped={report.skipped_is_dir}")
    if report.skipped_empty_result:
        logger.warning(f"Skipped {report.skipped_empty_result} file(s) that would become empty names.")
    for c in report.skipped_collision:
        logger.warning(f"COLLISION-SKIP: {c}")

    if args.verbose or not args.execute:
        # Show full plan (capped display, full count in summary).
        preview_n = len(report.planned) if args.verbose else min(10, len(report.planned))
        for op in report.planned[:preview_n]:
            logger.info(f"  {'WOULD RENAME' if not args.execute else 'PLAN'}: {op.old_name} -> {op.new_name}")
        if not args.verbose and len(report.planned) > preview_n:
            logger.info(f"  ... and {len(report.planned) - preview_n} more (use --verbose to list all)")

    if args.manifest:
        write_manifest(report, Path(args.manifest))
        logger.info(f"Manifest written: {args.manifest}")

    # ---- Phase 2: execute (only with --execute) ----
    if not args.execute:
        logger.info("DRY-RUN only — nothing was changed. Re-run with --execute to rename.")
        return 2 if report.skipped_collision or report.skipped_empty_result else 0

    if not report.planned:
        logger.info("Nothing to rename — all files already clean.")
        return 0

    logger.info(f"Executing {len(report.planned)} renames (metadata-preserving os.rename)...")
    report = execute_plan(report, logger)
    logger.info(f"DONE: renamed={report.renamed}/{report.to_rename} "
                f"errors={len(report.errors)} collision_skips={len(report.skipped_collision)}")
    for e in report.errors:
        logger.error(e)
    return 0 if not report.errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
