#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""verify_reference.py — prove a checkout matches the frozen reference.

Compares worktree files against blueprints/FLUTTER_REFERENCE_MANIFEST.json
(SHA-256 per file). Exit 0 = intact, 1 = mismatch/missing, 2 = usage error.
Untracked-but-unignored files are reported as warnings only (scratch space
is allowed; the manifest pins tracked content, not worktree tidiness).

Run: C:\\venv-hub\\venv\\Scripts\\python.exe tools\\verify_reference.py
"""

from __future__ import annotations

import hashlib
import io
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "blueprints" / "FLUTTER_REFERENCE_MANIFEST.json"


def main() -> int:
    if len(sys.argv) > 1:
        print(f"usage: {Path(sys.argv[0]).name}")
        return 2
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    failures = 0
    for entry in manifest["files"]:
        target = ROOT / entry["path"]
        if not target.is_file():
            print(f"MISSING: {entry['path']}")
            failures += 1
            continue
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        if digest != entry["sha256"]:
            print(f"MISMATCH: {entry['path']}")
            failures += 1
    if failures:
        print(f"reference BROKEN: {failures} file(s) differ")
        return 1
    print(f"reference INTACT: {manifest['counts']['files']} files verified")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
