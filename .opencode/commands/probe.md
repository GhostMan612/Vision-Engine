---
description: Probe a file or tree without ingesting it (line counts + head)
---

Preview the target without a full read (token conservation — mantle law):

1. For a file: report line count + first 40 lines + last 10 lines via `C:\venv-hub\venv\Scripts\python.exe -c "import io; p=r'<path>'; L=io.open(p,encoding='utf-8').read().splitlines(); print(len(L)); print('\n'.join(L[:40])); print('...'); print('\n'.join(L[-10:]))"`.
2. For a media tree: report counts by extension + total bytes (no filenames with personal data — hash or truncate).
3. Never `cat` large dumps, EXIF blobs, or CSV manifests into context.
