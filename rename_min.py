import argparse
import os
from pathlib import Path
PREFIXES = ("IMG_", "VID_")
def strip(n):
    for p in PREFIXES:
        if n.startswith(p):
            return n[len(p):]
    return None
def main():
    a = argparse.ArgumentParser()
    a.add_argument("directory")
    a.add_argument("--execute", action="store_true")
    r = a.parse_args()
    d = Path(r.directory)
    ops = []
    for e in sorted(d.iterdir(), key=lambda x: x.name):
        if not e.is_file():
            continue
        s = strip(e.name)
        if not s:
            continue
        t = e.with_name(s)
        if t.exists():
            print(f"SKIP {e.name} -> {s} (exists)")
            continue
        ops.append((e, t))
    if not r.execute:
        for s, t in ops:
            print(f"{s.name} -> {t.name}")
        print(f"{len(ops)} to rename (dry-run)")
        return
    n = 0
    for s, t in ops:
        if t.exists():
            print(f"SKIP {s.name} (exists)")
            continue
        os.rename(s, t)
        print(f"{s.name} -> {t.name}")
        n += 1
    print(f"{n}/{len(ops)} renamed")
if __name__ == "__main__":
    main()
