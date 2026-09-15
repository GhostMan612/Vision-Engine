#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""parity_probe.py — prove Dart rename_core matches rename_camera_prefixes.py.

Builds synthetic trees in temp dirs, runs the Python build_plan directly,
runs the Dart core via tool/dump_plan.dart on the same tree, and diffs
planned ops + collisions + counts. Then proves pure renames preserve bytes
(SHA-256 of contents before/after execute).

Run: C:\\venv-hub\\venv\\Scripts\\python.exe tools\\parity_probe.py
(workdir: C:\\vision engine). Uses the hub interpreter as-is; touches nothing
outside temp dirs. Exit 0 = full parity, 1 = mismatch.
"""

from __future__ import annotations

import hashlib
import importlib.util
import json
import logging
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DART_PKG = ROOT / "packages" / "rename_core"

CASES = [
    {
        "name": "leading_only",
        "files": [
            "IMG_20260905_110523079_HDR.jpg",
            "VID_20260902_200800398.mp4",
            "MY_IMG_foo.jpg",
            "20200101_plain.jpg",
        ],
        "opts": {},
    },
    {
        "name": "case_gate_default",
        "files": ["img_lower.jpg", "vid_lower.mp4", "IMG_upper.jpg"],
        "opts": {},
    },
    {
        "name": "case_gate_ignore",
        "files": ["img_lower.jpg", "vid_lower.mp4", "IMG_upper.jpg"],
        "opts": {"ignore_case": True},
    },
    {
        "name": "empty_skip",
        "files": ["IMG_", "VID_", "IMG_ok.jpg"],
        "opts": {},
    },
    {
        "name": "collision_trio",
        "files": [
            "IMG_A.jpg",
            "A.jpg",
            "IMG_B.jpg",
            "VID_B.jpg",
            "IMG_C.jpg",
            "C.jpg",
            "IMG_D.jpg",
            "D.jpg",
        ],
        "opts": {},
    },
    {
        "name": "chain_sources",
        "files": ["IMG_VID_a.jpg", "VID_a.jpg"],
        "opts": {},
    },
]


def load_truth():
    spec = importlib.util.spec_from_file_location(
        "rename_truth", ROOT / "rename_camera_prefixes.py"
    )
    mod = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = mod
    spec.loader.exec_module(mod)
    return mod


def find_dart() -> str:
    found = shutil.which("dart")
    if found:
        return found
    fallback = Path(r"C:\src\flutter\bin\dart.bat")
    if fallback.is_file():
        return str(fallback)
    raise SystemExit("dart not found on PATH")


def snapshot_hashes(target: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for path in sorted(target.iterdir()):
        if path.is_file():
            out[path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
    return out


def main() -> int:
    logging.basicConfig(level=logging.WARNING)
    truth = load_truth()
    dart = find_dart()
    failures = 0

    for case in CASES:
        with tempfile.TemporaryDirectory(prefix="ve-parity-") as tmp:
            target = Path(tmp)
            for name in case["files"]:
                (target / name).write_bytes(b"SYNTHETIC:" + name.encode())
            prefixes = ("IMG_", "VID_")
            report = truth.build_plan(
                target,
                prefixes,
                False,
                not case["opts"].get("ignore_case", False),
            )
            py_planned = sorted(
                (str(op.src), str(op.dst)) for op in report.planned
            )
            py_collisions = sorted(report.skipped_collision)
            py_counts = {
                "scanned": report.scanned,
                "planned": len(report.planned),
                "skippedNoPrefix": report.skipped_no_prefix,
                "skippedIsDir": report.skipped_is_dir,
                "skippedEmptyResult": report.skipped_empty_result,
                "collisions": len(report.skipped_collision),
            }
            entries = [
                {
                    "path": str(target / name),
                    "name": name,
                }
                for name in sorted(
                    p.name for p in target.iterdir() if p.is_file()
                )
            ]
            existing = sorted(str(p) for p in target.rglob("*"))
            entries_path = target / "entries.json"
            existing_path = target / "existing.json"
            entries_path.write_text(json.dumps(entries), encoding="utf-8")
            existing_path.write_text(json.dumps(existing), encoding="utf-8")

            cmd = [
                dart,
                "run",
                "tool/dump_plan.dart",
                "--entries",
                str(entries_path),
                "--existing",
                str(existing_path),
            ]
            if case["opts"].get("ignore_case"):
                cmd.append("--ignore-case")
            proc = subprocess.run(
                cmd,
                cwd=str(DART_PKG),
                capture_output=True,
                text=True,
                timeout=120,
            )
            if proc.returncode != 0:
                print(f"[{case['name']}] DART TOOL FAILED: {proc.stderr[-500:]}")
                failures += 1
                continue
            dart_out = json.loads(proc.stdout)
            dart_planned = sorted(tuple(p) for p in dart_out["planned"])
            dart_collisions = sorted(dart_out["collisions"])
            ok = (
                dart_planned == py_planned
                and dart_collisions == py_collisions
                and dart_out["counts"] == py_counts
            )
            print(f"[{case['name']}] {'PASS' if ok else 'MISMATCH'} "
                  f"planned={len(py_planned)} collisions={len(py_collisions)}")
            if not ok:
                failures += 1
                print(f"  python planned: {py_planned}")
                print(f"  dart   planned: {dart_planned}")
                print(f"  python collisions: {py_collisions}")
                print(f"  dart   collisions: {dart_collisions}")
                print(f"  python counts: {py_counts}")
                print(f"  dart   counts: {dart_out['counts']}")

    with tempfile.TemporaryDirectory(prefix="ve-bytes-") as tmp:
        target = Path(tmp)
        payloads = {
            "IMG_alpha.jpg": b"\x00\x01alpha-payload",
            "VID_beta.mp4": b"\x02\x03beta-payload",
            "plain.jpg": b"\x04plain",
        }
        for name, data in payloads.items():
            (target / name).write_bytes(data)
        before = snapshot_hashes(target)
        report = truth.build_plan(target, ("IMG_", "VID_"), False, True)
        logger = logging.getLogger("parity-bytes")
        truth.execute_plan(report, logger)
        after = snapshot_hashes(target)
        names_ok = sorted(after) == ["alpha.jpg", "beta.mp4", "plain.jpg"]
        bytes_ok = sorted(before.values()) == sorted(after.values())
        print(f"[byte_proof] names_ok={names_ok} bytes_ok={bytes_ok} "
              f"renamed={report.renamed}")
        if not (names_ok and bytes_ok):
            failures += 1

    print(f"parity: {'GREEN' if failures == 0 else 'RED'} "
          f"({len(CASES)} plan cases + byte proof)")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
