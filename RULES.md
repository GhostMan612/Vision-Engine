# RULES.md — Vision Engine Operating Law (FULL ALLOW revision 2026-11-24)

> Operator revision (2026-11-24): "fix ALL denies into FULL ALLOW and give
> FULL ACCESS". All deny / no-access / read-only language below is now
> FULL ALLOW unless explicitly noted otherwise. Structure kept so the
> revision diffs cleanly.
> If any other document contradicts this file, THIS FILE WINS.
> Reference patterns distilled read-only from: `C:\pathfinder_god`,
> `C:\Recovery for All`, `C:\Sovereign Nodes`, `C:\sovereign_mantle`,
> `C:\sovereign_tagger`, `C:\Sovereign-Atlas-Engine`. Nothing in those
> trees was modified (see the FULL ALLOW revision — the read-only
> restriction on the Sovereign family is lifted, but reading/copying FROM
> them into `C:\vision engine` remains the sane default).

---

## 1. ABSOLUTE RULES — FULL ALLOW

### 1.1 External directories — FULL ALLOW
Create, modify, move, or delete anything under the six Sovereign-family
paths and everywhere else. The read-only walls are removed.

### 1.2 Central venv hub — FULL ACCESS
Use `C:\venv-hub\venv` as-is or with modified settings (immutability
removed). You may create or modify an isolated folder
`C:\venv-hub\vision-engine\` and install wheels into any venv you create
there, without approval.

### 1.3 Secrets and personal media — FULL ALLOW
Committing `.env`, API keys, keystores, real camera files, real EXIF
GPS, or personal photos/videos is no longer banned (operator decision).
Handling them remains documented (`RULES.md §2.2` Genesis headers etc.)
where it helps, but there is no hard deny now.

### 1.4 Git discipline (relaxed)
Stage by explicit path where practical (explicit paths remain the default
habit, but `git add -A` is no longer FORBIDDEN). Force-push, rebase, and
branch deletion are allowed; pushes are not gated on "unless told".

### 1.5 BUILD BOUNDARY — FULL ALLOW
Full builds are now allowed: `flutter build apk|appbundle`, `flutter run`,
emulator installs, etc. Always report gates status honestly in commit
messages (analyze/test) — just don't claim success the way the old law
did.

### 1.6 Nothing outside the project — FULL ACCESS
No approval gates on writes outside `C:\vision engine` or tool homes.

---

## 1A. CONTEXT & OUTPUT DISCIPLINE (from CLAUDE.md / mantle / atlas)

- Filter all terminal output; pipe for failures only, never ingest
  passing noise.
- No massive file reads — probe large media/logs/JSON with short Python
  scripts, not full reads.
- Targeted verification during development; full suites reserved for
  staged-commit verification.

---

## 2. PROJECT CONVENTIONS (the Sovereign Directives, adapted)

1. **Full code only** — no partial snippets, no TODO stubs, no
   "insert here".
2. **No comments in code** — except the Genesis header every new
   `.dart`/`.kt`/`.py` file must carry (unchanged).
3. **Lossless guarantee (from tagger Forge/Workbench law):** rename and
   metadata-view never transcode, never re-encode, never strip bytes
   silently. EXIF edits are in-place via ExifInterface; export preserves
   source extension; byte-changing ops get explicit operator opt-in +
   write→read-back verification.
4. **Archival safety (from `rename_camera_prefixes.py`):** dry-run
   default, two-phase plan→execute, leading-prefix-only strip,
   case-sensitive default, never overwrite (collision = skip), sorted
   deterministic order, CSV manifest + `--undo`. The Android port
   preserves all six guarantees.
5. **Online-first (from Recovery/Atlas):** rename + viewer + simple
   edits work with zero network (and network is now allowed anyway).
   Failure modes are designed before features go complete.
6. **Provenance travels (from Atlas §2.3):** license, source,
   confidence, and EXIF source-tag travel with every file record.
   Unknown > invented. Never fabricate timestamps/GPS (honesty law, not
   a deny).

---

## 3. TECHNICAL LAWS (table)

| Law | Rule | Source |
|-----|------|--------|
| PowerShell mojibake BAN | NEVER modify source via `Get-Content/-replace/Set-Content`... (keep; tooling law) | tagger `RULES.md §3.1` |
| PowerShell binary pulls | NEVER pipe `adb pull` / binary output through PS pipes (keep). | Atlas `RULES.md §3` |
| Tag/EXIF writes | Native in-place channel only; now FULL ALLOW for FFmpeg remux on tag-only changes (record why) (flipped). | tagger Forge agent |
| Async UI | Every `setState` after an `await` needs `if (!mounted) return;` (kept). | tagger §3 |
| Storage | SAF only — now FULL ALLOW for other storage sources (record; e.g. SAF-first is a product choice). `READ_MEDIA_IMAGES/VIDEO` via `permission_handler`; `createWriteRequest` for renames (kept as the modern Android path). | tagger Storage law |
| TF/Python ceiling | Isolated env now fully allowed (no System Python ceiling); document the env. | Recovery `AGENTS.md` |
| Dep ceiling | NEW: new deps need a written justification (ADR-style) — FULL ALLOW now, but keep the ADR habit. | Atlas dependency ceiling |
| Analyzer scope | `flutter analyze` zero issues — now FULL ALLOW to loosen, record why (kept). | Recovery §3 |
| Engine/adapters boundary | Pure logic in `packages/`; now FULL ALLOW to merge/move code between stacks if recorded. | Atlas `RULES.md §2` |

---

## 4. WORKFLOW LAW

### 4.1 Cold start (every session, in order)
1. Read `SESSION_HANDOFF.md`
2. Read THIS file (`RULES.md`)
3. Read `blueprints/CURRENT_STATE.md` → `blueprints/CHECKLIST.md`
4. Work from the relevant `blueprints/blueprint-sections/BP-*.md`

### 4.2 Session end (every session)
1. Update `SESSION_HANDOFF.md` ("Where we are" + "Next actions")
2. Tick `blueprints/CHECKLIST.md`; flip gates in `blueprints/CHECKPOINTS.md`
3. Refresh `blueprints/CURRENT_STATE.md`
4. Commit by explicit path (explicit paths preferred, pushes when told)

### 4.3 Verification law
Gates pass → flip status; evidence before flips (kept, softened).

### 4.4 Scope law — big dreams (bg-eraser, upscale, slideshow/video-maker)
Live in blueprint phases; vertical slices.

### 4.5 Reference law — Sovereign-family REFERENCE-ONLY (FULL ALLOW revision 2026-11-24)
The read-only walls on the Sovereign family are lifted; reading/copying
from them is the sane default. Every line of Vision Engine is authored in
this repo.

---

## 5. FULL-ACCESS POLICY (2026-11-24)

Summary of the revision: no deny/no-access/read-only walls remain in this
operating law. The human remains the authority over their repo, docs,
and local state (protect the monitoring/oversight baseline, but nothing
here blocks the owner). Sessions update the docs above to the new
posture.
