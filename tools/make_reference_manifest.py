#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""Regenerate the tracked-file SHA-256 reference manifest.

Run from the repo root (or anywhere) with:
    C:\\venv-hub\\venv\\Scripts\\python.exe tools\\make_reference_manifest.py

Only git-tracked files are pinned, except the manifest itself. Stage all
intended changes first so the new files are part of `git ls-files`.
"""

from __future__ import annotations

import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "blueprints" / "FLUTTER_REFERENCE_MANIFEST.json"
EXCLUDED = {MANIFEST.relative_to(ROOT).as_posix()}


def main() -> int:
    tracked = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=ROOT,
        capture_output=True,
        check=True,
    ).stdout.decode("utf-8").split("\0")
    entries = []
    total = 0
    for rel in sorted(p for p in tracked if p and p not in EXCLUDED):
        target = ROOT / rel
        if not target.is_file():
            continue
        data = target.read_bytes()
        entries.append(
            {
                "path": rel,
                "bytes": len(data),
                "sha256": hashlib.sha256(data).hexdigest(),
            }
        )
        total += len(data)
    payload = {
        "tag": "flutter-final-v2",
        "scope": (
            "all git-tracked files except this manifest; refreshed 2026-11-27 "
            "after Phase 3-5 + deferred work (verify_reference.py GREEN)"
        ),
        "files": entries,
        "counts": {"files": len(entries), "bytes": total},
    }
    MANIFEST.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"manifest refreshed: {len(entries)} files, {total} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
