# RULES.md — Vision Engine Operating Law
> **CANONICAL RULESET.** Every session MUST read this file before doing any work.
> If any other document contradicts this file, THIS FILE WINS.
> Reference patterns distilled read-only from: `C:\pathfinder_god`, `C:\Recovery for All`, `C:\Sovereign Nodes`, `C:\sovereign_mantle`, `C:\sovereign_tagger`, `C:\Sovereign-Atlas-Engine`. Nothing in those trees was modified.

---

## 1. ABSOLUTE RULES (never violated, no exceptions)

### 1.1 External directories are READ-ONLY
Never create, modify, move, or delete ANYTHING under:
```
C:\pathfinder_god
C:\Recovery for All
C:\Sovereign Nodes
C:\sovereign_mantle
C:\sovereign_tagger
C:\Sovereign-Atlas-Engine
```
- Folders matching `sovereign_tagger*` are READ-ONLY regardless of name/state (per Atlas/Recovery law).
- Reading and copying FROM them into `C:\vision engine` is allowed. When in doubt: copy out, edit inside.
- The ONLY writable work area is `C:\vision engine` (plus approved tool homes: `%USERPROFILE%\.gradle`, pub-cache, `C:\android` SDK).
- Source: `pathfinder_god/RULES.md §1.1`, `Recovery/RULES.md §1.1`, `Atlas/RULES.md §1.2`, `tagger/.blueprints/RULES.md §1.1`.

### 1.2 Central venv hub — use AS-IS, settings immutable
- Approved Python: `C:\venv-hub\venv\Scripts\python.exe` (3.14.6). Use as-is.
- NEVER modify `C:\venv-hub\server.py`, `WakeHub.bat`, `venv\pyvenv.cfg`, or any existing venv settings.
- You MAY create a new isolated folder `C:\venv-hub\vision-engine\` (requirements, probe scripts, throwaway venvs) to aid this project. You MAY install packages INTO a new venv you create there. You MAY NOT `pip install` into the shared `C:\venv-hub\venv` without explicit operator approval (TF-wheel / win32-ceiling risk — see §3).
- Source: `pathfinder_god/RULES.md §1.1`, `Sovereign Nodes/RULES.md §1.1`.

### 1.3 Secrets and personal media never enter the repo
- Never commit: `.env`, API keys, keystores (`*.keystore`, `debug.keystore`), `local.properties`, `google-services.json`, signing passwords.
- Never commit real camera files, real EXIF GPS, real family names, or personal photos/videos. Fixtures use synthetic names (`SAMPLE_20260905_110523079_HDR.jpg`) and synthetic EXIF (fixed lat/lng `44.9778,-93.2650` Minneapolis fallback only as an obviously-fake constant, stripped before commit where possible).
- Source: all six reference `RULES.md §1.2/§1.3`.

### 1.4 Git discipline
- Stage by explicit path only. `git add -A` / `git add .` are FORBIDDEN.
- Never force-push, rebase public history, or delete branches unless explicitly asked.
- `blueprints\` is working documentation (committed for this project — unlike pathfinder/nodes where it is gitignored). `_archive\`, `dcim\` sample media, and `*.csv` manifests are gitignored — do not "fix" this.
- Commit messages report gates status (analyze/test) only. NEVER claim build success — the human builds in Android Studio.
- Source: `pathfinder_god/RULES.md §1.3`, `Atlas/RULES.md §1.4`.

### 1.5 BUILD BOUNDARY — HARD RULE
- NEVER run full builds. No `flutter build apk|appbundle`, no `flutter run`, no emulator installs. The human builds in Android Studio on the Moto G.
- Your lane ends at source correctness: `flutter pub get`, `flutter analyze` (must be "No issues found!"), `flutter test` (host-side only).
- `integration_test/` files (device rename + MediaStore smoke) exist for the human's manual runs — do NOT execute them here unless explicitly asked (they compile a test binary).
- Debug device failures from the human's pasted output — never by rebuilding locally.
- Source: `Recovery/RULES.md §1.5`, `Atlas/RULES.md §1.6`, `tagger/AGENTS.md`.

### 1.6 Nothing outside the project without approval
Do not install software, modify system settings, or write outside `C:\vision engine` / approved tool homes / `C:\venv-hub\vision-engine\` without asking first.

---

## 1A. CONTEXT & OUTPUT DISCIPLINE (from CLAUDE.md / mantle / atlas)

- Filter all terminal output; pipe for failures only (`| Select-String "error|fail"`), never ingest passing noise.
- No massive file reads — probe large media/logs/JSON with short Python scripts (`C:\venv-hub\venv\Scripts\python.exe`), not full `Read`.
- Targeted verification during development; full suites reserved for staged-commit verification.
- Spawn subagents for deep exploration when available; return summaries, not raw dumps.
- Proactively compact context after each verified phase.
- Source: `sovereign_mantle/AGENTS.md §§1-3`, `Atlas/RULES.md §1A`, `pathfinder_god/RULES.md §1A`.

---

## 2. PROJECT CONVENTIONS (The Sovereign Directives, adapted)

1. **Full code only** — no partial snippets, no TODO stubs, no "insert here".
2. **No comments in code** — except the Genesis header every new `.dart`/`.kt`/`.py` file must carry:
   ```
   // ============================================================
   // As Above, So Below. As Within, So Without.
   // The Future Dictates the Past and the Past is Always Present.
   // ============================================================
   ```
   (Python uses `#` equivalents.) Source: all six repos §2.
3. **Lossless guarantee (from tagger Forge/Workbench law):** rename and metadata-view never transcode, never re-encode, never strip bytes silently. EXIF edits are in-place via ExifInterface; export preserves source extension; any byte-changing op requires explicit operator opt-in + write→read-back verification.
4. **Archival safety (from `rename_camera_prefixes.py`):** dry-run default, two-phase plan→execute, leading-prefix-only strip, case-sensitive default, never overwrite (collision = skip), sorted deterministic order, CSV manifest + `--undo`. The Android port MUST preserve all six guarantees.
5. **Offline-first (from Recovery/Atlas):** rename + viewer + simple edits work with zero network. Failure modes designed before any network/AI feature is called complete.
6. **Provenance travels (from Atlas §2.3):** license, source, confidence, and EXIF source-tag travel with every file record. Unknown > invented. Never fabricate timestamps/GPS.

---

## 3. TECHNICAL LAWS (learned the hard way — see reference gotchas)

| Law | Rule | Source |
|-----|------|--------|
| PowerShell mojibake BAN | NEVER modify source via `Get-Content/-replace/Set-Content`, `Out-File`, `Add-Content`. PS 5.1 mis-reads UTF-8 as ANSI → mojibake (`—`→`â€"`) + silent letter swaps (`setState`→`setYtate`). Editor tools only; Python with `encoding='utf-8'` if scripted; then analyze + `'â€\|Ã\|Â'` signature grep. | tagger `RULES.md §3.1`, Recovery `AGENTS.md` |
| PowerShell binary pulls | NEVER pipe `adb pull` / binary output through PS pipes — write straight to file, no `Out-String`. | Atlas `RULES.md §3` |
| FFmpeg | ALWAYS `FFmpegKit.executeAsync()` via a serialized executor queue. Sync `execute()` freezes the Android UI thread. Explicit `-map` on merges; verify via `probeStreams`. | tagger `AGENTS.md`, `RULES.md §3` |
| Tag/EXIF writes | Native in-place channel only (ExifInterface / jaudiotagger-style), NEVER FFmpeg remux for tag-only changes. Preserve extension. Write→read-back round-trip on export. | tagger Forge agent |
| Async UI | Every `setState` after an `await` needs `if (!mounted) return;` (covers FilePicker `PlatformException`). | tagger §3 |
| Storage | MediaStore / SAF only. Temp artifacts under cache; clean up on failure paths too. `READ_MEDIA_IMAGES/VIDEO` runtime via `permission_handler`; `createWriteRequest` on Android 11+ for renames. Never assume raw `File.rename` works on scoped storage. | tagger Storage law + Recovery Firebase-guarded-init pattern |
| TF/Python ceiling | System Python is 3.14 → very few wheels (no TF). Any on-device-ML training/proto uses an isolated env (e.g. `C:\venv-hub\vision-engine\.venv-tf` with Python 3.12 via `uv`), mirroring Recovery `.venv-tf`. Never downgrade the shared venv. | Recovery `AGENTS.md`, tagger toolchain quirks |
| Dep ceiling | New deps need written justification (ADR-style). No `flutter pub upgrade --major-versions` without a dedicated session (win32 `5.9.0↔6.0.1` break documented in tagger). `ffmpeg_kit`, `video_player`, `permission_handler` pins are load-bearing until a major session. | tagger ceiling, Atlas dependency ceiling |
| Analyzer scope | `flutter analyze` must stay at zero issues. `analysis_options.yaml` excludes platform dirs + archives; don't loosen excludes. | Recovery §3 |
| Engine/adapters boundary | Pure rename/metadata logic lives in dependency-free Dart packages (`packages/`); Flutter/Android/MediaStore/FFmpeg types stay in adapters. `apps/` depends on `packages/` — never reverse. New packages/deps require an ADR in `blueprints/decisions/`. | Atlas `RULES.md §2` |

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
4. Commit code by explicit path with a descriptive message (never commit manifests, `dcim/` samples, or secrets)

### 4.3 Verification law
No checklist item is done until its checkpoint gate passes (see `blueprints/CHECKPOINTS.md`). Evidence before status flips. New work → define its gate first. Host gates always: analyze + unit/widget + engine fixtures. Device claims ONLY from device runs (human's pasted evidence).

### 4.4 Scope law
Big dreams (bg-eraser, upscale, slideshow/video-maker) live in blueprint phases first. Ship vertical slices; never let polish precede a passing rename-safety gate. No engine rewrites to serve app convenience.

### 4.5 Reference law
Sovereign-family directories are REFERENCE ONLY. Every line for Vision Engine is authored in this repo, tailored to this project's needs. Credit the source pattern in chat/docs where directly inspired (e.g. "collision-guard from `rename_camera_prefixes.py` + tagger Forge verify").
