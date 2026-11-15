import argparse
import os
import sys
from pathlib import Path
PREFIXES = ("IMG_", "VID_")
def strip(n):
    for p in PREFIXES:
        if n.startswith(p):
            return n[len(p):]
    return None
def _execution_order(ops):
    pending = list(ops)
    ordered = []
    while pending:
        sources = {s for s, _ in pending}
        ready = sorted((op for op in pending if op[1] not in sources), key=lambda op: op[0].name)
        if not ready:
            break
        ordered.extend(ready)
        for op in ready:
            pending.remove(op)
    return ordered, pending
def main(argv=None):
    a = argparse.ArgumentParser()
    a.add_argument("directory")
    a.add_argument("--execute", action="store_true")
    r = a.parse_args(argv)
    d = Path(r.directory)
    if not d.is_dir():
        print(f"ERROR: not a directory: {d}", file=sys.stderr)
        return 1
    ops = []
    skipped = 0
    seen = set()
    candidates = []
    for e in sorted(d.iterdir(), key=lambda x: x.name):
        if e.is_symlink():
            print(f"SKIP {e.name} (symlink, never renamed)")
            skipped += 1
            continue
        if not e.is_file():
            continue
        s = strip(e.name)
        if not s:
            continue
        candidates.append((e, e.with_name(s)))
    candidate_srcs = {s for s, _ in candidates}
    for e, t in candidates:
        if t.exists() and t not in candidate_srcs:
            print(f"SKIP {e.name} -> {t.name} (exists)")
            skipped += 1
            continue
        if t in seen:
            print(f"SKIP {e.name} -> {t.name} (duplicate target in batch)")
            skipped += 1
            continue
        seen.add(t)
        ops.append((e, t))
    if not r.execute:
        for s, t in ops:
            print(f"{s.name} -> {t.name}")
        print(f"{len(ops)} to rename (dry-run)")
        return 2 if skipped else 0
    ordered, cyclic = _execution_order(ops)
    for s, t in cyclic:
        print(f"ERROR: rename cycle, skipped: {s.name} -> {t.name}")
        skipped += 1
    n = 0
    for s, t in ordered:
        if t.exists():
            print(f"SKIP {s.name} (exists)")
            skipped += 1
            continue
        os.rename(s, t)
        print(f"{s.name} -> {t.name}")
        n += 1
    print(f"{n}/{len(ops)} renamed")
    return 2 if skipped or n != len(ops) else 0
if __name__ == "__main__":
    raise SystemExit(main())
