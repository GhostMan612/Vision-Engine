---
description: Flutter analyze (filtered) — errors/warnings only
---

Run the analyzer gate exactly as defined in AGENTS.md. Do not dump raw output.

1. Execute `flutter analyze --no-pub` in the relevant dir (`app/` or `packages/<pkg>/`)
2. Pipe through `Select-String -Pattern "error •|warning •"` and show the first 20 hits only
3. Then show the last 3 lines (expect "No issues found!" on clean)
4. If any `error •` exists, group by file and suggest the fix. If only `warning •`/`info`, note them but do not block.
5. Never ingest full unfiltered logs.
