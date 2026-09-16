# SESSION_HANDOFF.md — Vision Engine (live state)

> Update every session per RULES.md §4.2. Cold-start entry point after RULES.md.

## DOCUMENT MAP — cold-start hooks (read top-to-bottom)

| # | File | Holds | When to read |
|---|------|-------|--------------|
| 0 | `AGENTS.md` (root) | Compact ramp: structure, commands, env, architecture notes | automatic |
| 1 | `RULES.md` | **CANONICAL** operating law: read-only paths, hub-immutable, build boundary, §3 technical laws incl. mojibake ban | EVERY session, before any edit |
| 2 | THIS FILE | Latest deltas, next actions, open decisions, toolchain notes | EVERY session |
| 3 | `blueprints/CURRENT_STATE.md` | Verified per-file map, known-issue registry, toolchain freeze | before writing code |
| 4 | `blueprints/ROADMAP.md` | Phase tracker (0–5) with gates | when planning/phases |
| 5 | `blueprints/VISION_ENGINE_MASTER_BLUEPRINT.md` | Frozen product spec (v1.0 target) | before novel features |
| 6 | `blueprints/ARCHITECTURE.md` | Engine/adapters split, storage flow, metadata flow | structural changes |
| 7 | `blueprints/blueprint-sections/BP-*.md` | Executable task slices per phase | task work |
| — | `rename_camera_prefixes.py`, `rename_min.py` | **Executable truth** for rename semantics — prose never overrides | any rename-semantics question |

Conflict law: RULES.md > other docs; executable files (`*.py`, `pubspec.yaml`, `build.gradle`) > prose.

## Where we are (2026-09-15, MP1 native foundation COMPLETE — STOPPED)

- **MP1 done, all gates green here:** `:vision-core:test` 2/2 (boundary
  contract incl. executable forbidden-import rule), `:vision-android`
  compiles + test task executes, `:vision-app:assembleDebug` APK 8.79 MB,
  `:vision-app:testDebugUnitTest` 2/2 (plain ViewModel + Robolectric
  Compose launch). Manifest: zero permissions. Deps audited (no network/
  analytics; app→android→core edges only).
- **Two evidence-pinned deviations (versions unchanged):** AGP 9.1
  built-in Kotlin (explicit kotlin.android plugin declaration fails —
  dropped); Robolectric-on-JDK25 needs test JVM flags (native-access +
  two add-opens). Both recorded in `native/README.md`.
- **Flutter reference untouched** (`flutter-final` still points at
  `aae04f9`; no Flutter file modified). NO MP2, NO Viewer, NO 2D.
- **Next:** MP2 domain port authorization or redirect.

## Where we were (2026-09-15, MP0 reference freeze — awaiting MP1 kickoff)

- **Reference locked:** tag `flutter-final` (this commit) + baseline doc
  + 137-file SHA-256 manifest (self-verified green) + checker.
  Gates re-executed for the freeze: 22/22 + 20/20 + 17/17 + 44/44 host
  tests, all analyzers clean. 2C device rerun still outstanding (not
  claimed). NO native code started; NO 2D.
- **Next:** MP1 kickoff authorization or redirect.

## Where we were (2026-09-15, migration blueprint delivered — NO code, NO 2D)

- **GQ8 answered NO → native Kotlin + Compose is the migration target.**
  `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` (inventory A–P,
  responsibility matrix, Compose-vs-Views verdict, DocumentsContract-
  direct SAF call, threading/test-migration plans, MP0–MP9 sequence,
  freeze + retirement strategy, R-01–R-08) + ADR-005 written. Stated
  plainly inside: native wins on complexity surface + SAF-first +
  toolchain singularity, NOT throughput; 4–6 week solo estimate stands.
- **Honesty guardrails kept:** 2C device rerun still outstanding (NOT
  claimed as validated); no implementation started; 2D stays locked
  until a migration kickoff (MP0) is explicitly authorized.
- **Next:** human 2C device rerun → then MP0 kickoff decision or
  redirect.

## Where we were (2026-09-15, platform study delivered — NO code, NO 2D)

## Where we were (2026-09-15, 2C authorized — implementation in progress, NO Viewer UI)

- **2C scope (authorized, bounded):** SAF source → discovery → records →
  extraction (existing 2B bridge) → thumbnails → viewer-ready data model.
  `media_library` pure package green (17/17); `ExtractStatus` moved to
  core with zero breakage; lister + `ThumbStore` + pipeline written.
- **Hard-won root cause this session:** every 2C host-test hang traced to
  `whenComplete(() => map.remove(key))` — the cleanup closure returns the
  future itself, which `whenComplete` then awaits forever (silent
  deadlock, proven by m15/m18 isolation). One-line block-body fix. The
  long zone/binding/Skia misdiagnosis is banked as K17/K18 so it never
  repeats. Skia decode verified separately (256x384 orientation-applied;
  small sources upscale to target — pinned as verified truth).
- **Still to do in this package:** human device-suite rerun
  (`flutter test integration_test`); all green → 2C can close.
  ADR-004 + boundary/runbook/agent/tracking docs are committed.
- **Fix-forward 2026-09-15:** device lane reported missing `ThumbFetch`
  import in `pipeline_device_test.dart` (5 errors; `flutter test` never
  compiles `integration_test/`, and my analyze ran before the file
  existed — banked as K19). One-line import fix, analyzer clean, suite
  green (EXIT:0); awaiting human rerun.

## Where we were (2026-09-15, 2B FORMALLY CLOSED / PASS — awaiting 2C authorization)

- **2B device validation GREEN:** human-pasted Moto G result
  `01:06 +5: All tests passed!` Single lane `C:\android\flutter` /
  Dart 3.13.3; APK build/install clean — the Invalid SDK hash episode is
  conclusively resolved (clean + rebuild under one SDK).
- **Validated chain (device-proven):** Android file → MetadataBridge →
  ExifInterface/MediaMetadataRetriever → `vision_engine/metadata` channel
  → MetadataAdapter → metadata_core → provenanced PhotoMeta/VideoMeta.
- **Precision:** 2B closure ≠ G2 closure. G2 still needs the Viewer UI +
  real-shot smoke. No code changes follow from this closure.
- **2C candidate (NOT authorized):** SAF source → media discovery →
  extraction → thumbnail pipeline → deterministic viewer data model, with
  established exclusions intact. Awaiting explicit authorization; nothing
  started.

## Where we were (2026-09-15, exposure fix applied host-side — device rerun PENDING)

- **Last device failure resolved (test-expectation artifact, not a bug):**
  ExifInterface renders RATIONAL tags as decimal strings, so the fixture's
  `1/120` correctly arrives as `'0.008333333333333333'` (IEEE-deterministic).
  Frozen contract (`MetaField<String>`, verbatim codec) requires no rational
  form — assertion corrected to the exact canonical string + provenance
  check. No native/core/fixture change, no approximation, no 2C. (K16.)
- **Host gates green:** metadata_core 19/19, rename_core 22/22, app 26/26,
  both analyzers clean.
- **Next:** human reruns `flutter test integration_test` on Moto G. Five
  green → 2B can close.

## Where we were (2026-09-15, 2B triage fix applied host-side — device rerun PENDING)

- **Four device failures forensically classified, four minimal fixes
  (no weakening, no 2C):**
  (1) JPEG dims `"0"` → FIXTURE at fault (no SOF marker; real camera files
  always carry one) — SOF0 3000×4000 added to the generator, device
  assertions unchanged; (2) PNG orientation `0` → ADAPTER gap
  (ExifInterface absent-tag default; no camera writes `0`) — literal `0`
  now maps to unknown + warning, other values verbatim per core golden;
  (3) corrupt-file status `ok` → status-rule bug (`fileSizeBytes` counted
  toward `ok` made `partial` unreachable) — `ok` now counts EXIF/container
  fields only; corrupt test passes unmodified; (4) duration `8340` → TEST
  assumption artifact (retriever ignores mvhd without tracks; device truth
  is platform-reported `0`/container) — assertion + boundary doc corrected
  to verified truth. `metadata_core` untouched.
- **Host gates green under the surviving single SDK**
  (`C:\android\flutter`, Dart 3.13.3 — `C:\src\flutter` is gone from this
  environment): metadata_core 19/19, rename_core 22/22, app 26/26 (23 + 3
  new status-semantics tests), both analyzers clean.
- **Next:** human reruns `flutter test integration_test` on Moto G and
  pastes output. All five green → 2B can close. K15 banked.

## Where we were (2026-09-15, 2B build-blocker diagnosis — device run STILL PENDING)

- **Root cause PROVEN (not a code bug):** the 2B device run failed with
  `Can't load Kernel binary: Invalid SDK hash` in `objective_c`'s hook
  because this machine has TWO Flutter SDKs sharing the project —
  `C:\src\flutter` (Dart 3.13.0, agent lane) vs `C:\android\flutter`
  (Dart 3.13.3, human build lane) — and the 3.13.0-built `hook.dill`
  (byte-identical across all three cache dirs, incl. the failed 9/15 run's
  own stderr) was reused under 3.13.3. `objective_c` 9.6.0 is a legitimate
  transitive dep (`path_provider_foundation`); no repo change required.
- **My lane re-verified green:** `flutter test` 23/23 under Dart 3.13.0
  after the failure (further proof the tree is sound).
- **Remediation = environmental, human-executed:** `flutter clean` + `pub
  get` + rebuild under `C:\android\flutter`, then the existing
  `docs/device-validation-2b.md` run. Exact commands handed over; I did
  NOT touch `.dart_tool` (would symmetrically break my lane) and did NOT
  run any host build (RULES §1.5 wins over the work-package §11/§24 build
  leg — same supremacy call as ADR-003).
- **SDK XML v4 warning:** unrelated benign skew (cmdline-tools vs Studio
  metadata); platform install completed, failure came later at Dart kernel
  load. Left alone.
- Standing recommendation (operator decision, NOT executed): standardize
  on ONE Flutter SDK for both lanes, or always `flutter clean` when
  switching SDKs. Recorded as K14.

## Where we were (2026-09-15, Phase 2 Work Package 2B CLOSED host-side — STOPPED)

- **2B DONE, change control holds.** Kotlin `MetadataBridge` + channel +
  `MetadataAdapter` + 18/18 host tests + synthetic fixtures +
  device integration test authored (human Moto G run pending) +
  ADR-003 + boundary/runbook docs. Gates green (below). NO Viewer/UI/
  enumeration/thumbnails/MediaStore/G5/DCIM/writes started.
- **Gates 2B:** metadata_core analyze clean + 19/19; rename_core clean +
  22/22; app analyze clean + 23/23. New dep: exifinterface 1.4.1
  (native, justified ADR-003) + `integration_test` (SDK dev) +
  `metadata_core` path dep. rename_core behavior untouched.

## Where we were (2026-09-15, Phase 2 Slice 1 CLOSED — STOPPED)

- **Slice 1 DONE, change control holds.** `packages/metadata_core` v0.1.0
  (models + provenance + fixture codec + 3 goldens, 18/18 tests, analyze
  clean). GQ3 doc contradiction corrected (ARCHITECTURE ×2, blueprint
  table, matrix K1 — docs only). No Viewer/UI/platform/extraction work
  started. No Slice 2 — awaiting authorization.
- **Gates this slice:** metadata_core analyze clean + 18/18; rename_core
  analyze clean + 22/22; app analyze clean + 5/5. rename_core, app code,
  probes, D2 behavior untouched.

## Where we were (2026-09-15, Phase 2 research session)

- **Phase 2 = Metadata viewer (read-only first) — MAP FROZEN FOR REVIEW,
  NOT IMPLEMENTED.** `blueprints/PHASE_2_RESEARCH_MAP.md` holds the
  A/B/C report + 24-point execution map + 5 open questions (GQ1–GQ5).
  Awaiting operator review → lock → authorize. No production code touched.
- **Git:** already initialized last turn; verified in-sync (`main` ==
  `origin/main` == `01fa466`, clean tree). Nothing to re-init; this
  session commits only the map doc + this handoff.
- **Archaeology verdict:** Phase 2's meaning is unanimous across ROADMAP,
  BP-02, CHECKPOINTS G2, CHECKLIST, AGENTS, handoff (read-only metadata
  viewer). One real tension found: ARCHITECTURE §rename prescribes
  MediaStore for shared collections, but D2 + operator directive froze
  SAF-pick + raw rename — reword queued for next authorized task (D1 in
  the map doc), NOT edited here. Minor: the Python script's internal
  "Phase 1/Phase 2" (plan/execute) is unrelated to project phases.
- **D2 scope honesty (new precision):** D2 proved rename of APP-CREATED
  files in shared storage. Third-party (camera-created) file rename is
  NOT proven — G5/DCIM territory, still deferred, never probed.

- **Phase 0 CLOSED, Phase 1 CLOSED (host side).** `packages/rename_core`
  (pure Dart port) + `tools/parity_probe.py` + `app/` shell
  (`com.visionengine`, minSdk 26, compileSdk 37 pinned) scaffolded, gates
  green. Git initialized + pushed (D5 done). No writes outside
  `C:\vision engine` (+ build cache); hub settings untouched.
- **D1 DECIDED:** `com.visionengine` (namespace + applicationId; MainActivity
  moved to `com.visionengine`, stale `vision_engine` subpackage deleted).
- **D2 CLOSED/PASS (2026-09-15, Moto G 2025, human device run).**
  `RAW_RENAME: WORKS` on real shared storage
  (`/storage/emulated/0/Pictures/VE_TEST`): to_rename=2, renamed=2,
  errors=0, undo_restored=2, cleaned=true, `SHARED_PROBE: COMPLETE`
  (folder-picker path confirmed in a second identical run).
  Permissions observed: photos=granted, videos=granted, storage=denied
  (expected on API 33+, not a failure). No MediaStore fallback justified —
  raw rename via user-picked folder stays the validated path; rename_core
  and rename behavior UNCHANGED per operator directive. Real DCIM never
  touched.
- **Build fix (human-applied, recorded here):** first `assembleDebug`
  failed — `permission_handler_android` requires SDK 37 while the template
  pinned `flutter.compileSdkVersion` (36). Fixed by `compileSdk = 37`
  (SDK 37 already installed; AGP 9.1.0 + Gradle 9.3.1, debug APK launched).
  Pinned + justified in `build.gradle.kts` + ADR-002 scope. Incidental:
  SDK XML v4 vs cmdline-tools warning (Studio/cmdline skew, benign).
- **Python truth unchanged.** `rename_camera_prefixes.py` /
  `rename_min.py` / `dcim/` untouched.
- **Toolchain notes:** template minSdk default is 24 (pinned 26 per BP-05);
  template compileSdk is `flutter.compileSdkVersion` = 36 on Flutter 3.47
  (matches BP-05). Resolved: file_picker 12.3.0 (static
  `FilePicker.getDirectoryPath`, no `.platform`), permission_handler +
  path_provider + shared_preferences via `flutter pub add`.

## What shipped this session (delta)

- `packages/rename_core` v0.1.0: `stripLeadingPrefix` + `buildPlan`
  (staying-set, 3 collision classes, deterministic sort) + manifest CSV codec
  (superset: +`content_uri`) + undo-order codec. 22/22 host tests
  (strip/plan/manifest units + 6 golden fixtures). `dart analyze` clean.
- `tools/parity_probe.py`: 6/6 Python↔Dart plan cases PASS + SHA-256
  byte-proof PASS (`parity: GREEN`). Chain-rename case (`IMG_VID_a` →
  `VID_a` → `a`) proven identical on both sides.
- `app/` (vision_engine): Rename tab (seed → preview → execute → manifest →
  undo on app-private `vision_demo`), Probe tab (D2: app-private + shared
  probes, permission status/request, copyable report), Settings tab
  (prefixes/case/recursive persisted). `LocalRenameAdapter` (preview/execute/
  undo over dart:io) + `storage_probe` (self-creating/self-cleaning
  VE_PROBE dirs). 5/5 `flutter test`, `flutter analyze` clean. NEVER built.
- `docs/device-probe-moto-g.md`: exact human steps for the Moto G D2 run.
- Lessons banked: widget-test dart:io needs `tester.runAsync` (K9);
  `List.join(',')` ≠ `List.toString()` in comparisons (K10); file_picker 12
  API change (K11).

## Next actions

1. **Operator:** authorize 2C (candidate scope above) or redirect. No code
   until authorized — change control holds.
2. D3/D4 stay deferred (no ffmpeg/tflite until G1–G3 + promotion ADR).
3. Git discipline stands: explicit paths, gates-only messages, no force-push.

## Open decisions (deferred, do NOT implement yet)

- D1: DECIDED `com.visionengine` (shipped 2026-09-14).
- D2: CLOSED/PASS 2026-09-15 — raw rename via user-picked folder is the
  validated path; MediaStore adapter deferred (not justified by evidence).
- D3: `ffmpeg_kit_flutter` inclusion — ONLY for BP-04 slideshow/concat; never for tag writes (tagger law). Needs ADR + size-budget sign-off (~40MB/ABI).
- D4: On-device bg-erase engine (TFLite U²-Net vs وا cloud) — deferred to BP-04; no dep until Phases 1–3 pass.
- D5: DONE 2026-09-15 — repo initialized, 81 files committed by explicit
  path (`fc4317f`), GitHub README merged (`28d7205`), pushed to
  `https://github.com/GhostMan612/Vision-Engine` (`main` tracks
  `origin/main`). Discipline from here: explicit paths only, gates-only
  messages, no force-push.

## Toolchain notes (frozen)

- `C:\venv-hub\venv` Python 3.14.6 is the ONLY interpreter for harness scripts. No TF wheels on 3.14 — ML prototyping gets its own `.venv-tf` (Python 3.12) inside the project lane if ever needed (Recovery pattern).
- Flutter 3.47 / Dart 3.13 / `compileSdk 37` (pinned, device-proven) /
  `minSdk 26` / `targetSdk 36` (Flutter-pinned) / AGP 9.1.0 / Gradle 9.3.1.
- PS 5.1 mojibake ban in force for all source edits.
