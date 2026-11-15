# RECOMMENDATIONS — Vision-Engine standalone v1

> Status: SPEC ONLY — recommendations, not decisions. Drafted 2026-11-15
> against main `d103ef6` for the Vision-Engine standalone v1 spec PR.
> Every item is drawn ONLY from the sources in §1; no new decisions are
> made here. Open questions are listed in §5, not settled.
>
> Fact discipline (per
> `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §0): FACT =
> verified in repo/docs; REPO EVIDENCE = file:line or gate result;
> RECOMMENDATION = judgment; INFERENCE = strong but unverified;
> OPEN QUESTION = needs a decision or a device. Assumptions are never
> presented as facts.

## 1. Sources

- `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` (§4 Compose,
  §6 DocumentsContract-direct, §8 no Coil now, §12 dependency policy,
  §13 security/privacy gates, §14 risks R-01–R-08).
- `blueprints/decisions/ADR-002-dependency-ceiling.md` (ACCEPTED
  2026-09-14) and `blueprints/decisions/ADR-005-native-android-platform.md`
  (PROPOSED 2026-09-15).
- Facts updated 2026-11-14/15: PR #1 (`5f78388`, main) — Python rename
  fixes; PR #2 (`d103ef6`, main) — CI workflow fixes, including the
  GitHub API 403 on `.github/workflows/` that forced the web-editor
  route; Flutter frozen reference until MP9.

## 2. Consolidated recommendations

### REC-01 — Jetpack Compose for the Viewer (and all native UI)

- **What:** build all native UI — Viewer grid/detail (S5), Renamer
  screen (S4) — in Jetpack Compose, BOM-pinned at MP1 time, not now.
- **Why:** `LazyVerticalGrid` + per-item async thumb loading matches the
  record model directly; Views needs RecyclerView + ViewHolder + adapter
  diffing for the same screen; Compose collects `StateFlow` with lifecycle
  awareness in one line; Compose UI tests assert grid/detail states from
  the same record fixtures.
- **Evidence:** migration blueprint §4 (full comparison table); ADR-005
  (target: pure native Kotlin + Jetpack Compose).
- **What changes if Glenn disagrees:** Views becomes the UI stack with
  2–3× the UI code, Espresso + idling machinery instead of Compose UI
  tests, and manual observer management. The contracts (G2, field tables,
  provenance chips) do not change; only the implementation cost and the
  test shape change. Views is kept warm as the §4 fallback, not built.

### REC-02 — DocumentsContract-direct for SAF, never DocumentFile

- **What:** enumerate picked folders with `DocumentsContract` directly
  (single projection query: DOCUMENT_ID, DISPLAY_NAME, MIME_TYPE, SIZE,
  LAST_MODIFIED), top-level children only, client-side folded sort.
  Do not use `DocumentFile`.
- **Why:** DocumentFile costs ~20× per-file overhead (per-call provider
  queries); AOSP source itself recommends DocumentsContract for
  performance.
- **Evidence:** migration blueprint §6 (incl. the "verified" note);
  `native/vision-android/src/main/kotlin/com/visionengine/android/saf/SafDiscovery.kt`
  already implements this (MP3, 17/17 Robolectric fake-provider tests).
- **What changes if Glenn disagrees:** the enumerator is rewritten on
  DocumentFile; discovery gets measurably slower on large folders and the
  MP3 device evidence must be re-proven. The top-level-only boundary and
  the MIME→kind table do not change.

### REC-03 — No Coil now (hand-rolled thumb loading for v1)

- **What:** do not adopt Coil for the Viewer in v1. Keep the ported
  thumb path: ImageDecoder/BitmapFactory + FNV-keyed LRU (100 entries /
  32 MB) + in-flight dedupe.
- **Why:** Coil's default keys omit mtime (against the FNV contract) and
  its disk cache overlaps the ported cache.
- **Evidence:** migration blueprint §8 ("Coil NOT adopted now ... revisit
  ONLY for crossfade/AsyncImage ergonomics at Viewer time (documented
  fallback)"); REC-02/S5 carry the same rule into the Viewer slice.
- **What changes if Glenn disagrees:** the Viewer adopts Coil (or Coil 3)
  and must then prove key-equivalence with the FNV canonical string and
  disable or reconcile Coil's disk cache; the MP5 thumb evidence
  (256px, JPEG-85, orientation-applied) must be re-proven under the new
  loader. This is S5 open question Q3, not a default.

### REC-04 — Dependency ceiling: frozen toolchain, minimal adds, ADR gate for anything new

- **What:** keep the MP1 pins (AGP 9.1.0, Gradle 9.3.1, JVM 17,
  compileSdk 37, minSdk 26, exifinterface 1.4.1); add only coroutines,
  lifecycle, activity-result, DataStore Preferences, Material3, and the
  minimal test set (JUnit4, Robolectric, Turbine, Compose UI test);
  no network libs, no database, no DI framework, no MediaStore dep
  beyond framework APIs. Anything else needs a fresh ADR + operator
  sign-off.
- **Why:** the migration's cost is bounded by the frozen lane; each new
  dep is a new coupling (the Compose/Kotlin/AGP coupling is already the
  R-04 risk) and a new privacy surface.
- **Evidence:** migration blueprint §12 (RETAIN/REPLACE/ELIMINATE/ADD
  lists); ADR-002 (v1 allowed set; fresh ADR required for
  `ffmpeg_kit_flutter`, `tflite_flutter`, any Chaquopy/Python-in-APK,
  any cloud SDK, any `--major-versions` bump; `flutter pub upgrade
  --major-versions` banned without a dedicated session).
- **What changes if Glenn disagrees:** a new dep is admitted by ADR with
  its justification, its version pin, and its privacy review (no
  network/analytics/crash SDKs, migration blueprint §13). The ceiling
  itself is not silently raised by a PR that happens to add a dep.

### REC-05 — MediaStore stays deferred (SAF-first stands)

- **What:** no MediaStore enumeration in v1. The SAF-picked folder is
  the only media source.
- **Why:** D2 proved SAF-pick + raw rename works on the Moto G (2/2
  rename, 2/2 undo, cleaned); a MediaStore adapter was explicitly NOT
  justified and deferred.
- **Evidence:** `blueprints/CURRENT_STATE.md` (D2 CLOSED/PASS
  2026-09-15); `blueprints/ROADMAP.md` Phase 5; migration blueprint §6
  ("MediaStore stays deferred (SAF-first decision stands)").
- **What changes if Glenn disagrees:** a MediaStore lane becomes a new
  slice with its own ADR (it changes permissions, enumeration semantics,
  and the privacy gates) — it is not folded into S3/S4/S5.

### REC-06 — Security/privacy gates run on every slice, not once at the end

- **What:** static grep gate (manifest MUST NOT contain INTERNET),
  dependency audit rejecting network/analytics/crash SDKs, frozen
  permission list with a diff gate, airplane-mode device run, GPS
  never leaves the device (no geocoder/network import — import-scope
  grep gate), byte-identical sources asserted (untouched-proof pattern).
- **Why:** the app's offline-first, no-accounts posture is the product
  promise (master blueprint non-goals: no cloud/accounts/analytics/ads);
  a gate run once at MP9 catches violations after they have shipped in
  intermediate PRs.
- **Evidence:** migration blueprint §13; FLUTTER_REFERENCE_BASELINE.md
  §10 (static posture verifiable by grep; airplane-mode-capable).
- **What changes if Glenn disagrees:** gates move to MP8/MP9 only, and
  intermediate slices accept the risk that a privacy regression lands
  on `main` and is caught late. The gates themselves do not change.

### REC-07 — Sequential probing stays the default; concurrency only on measured need

- **What:** keep `MediaPipeline.loadPage`'s sequential `for` loop as the
  v1 default. Bounded concurrency via `limitedParallelism` only on a
  measured need, with the measurement recorded first.
- **Why:** sequential is the proven contract (Dart pipeline + Kotlin
  port); concurrency adds cancellation, ordering, and dedupe hazards
  for an unmeasured gain.
- **Evidence:** migration blueprint §9 ("Sequential metadata probing
  stays the default ... bounded concurrency ONLY on measured need via
  `limitedParallelism` — explicit YAGNI"); `MediaPipeline.kt:64`.
- **What changes if Glenn disagrees:** a concurrency experiment becomes
  its own slice with before/after first-page timings on the Moto G and a
  re-proof of ordering, dedupe, and cancellation. It is not a Viewer
  implementation detail.

### REC-08 — Python rename scripts are the canonical semantics the native port must match

- **What:** the native rename port (S2) must reproduce
  `rename_camera_prefixes.py` as it stands on main `d103ef6` — including
  the PR #1 fixes — behavior-for-behavior. The Dart `buildPlan` goldens
  and `tools/parity_probe.py` stay the cross-check.
- **Why:** Python is the frozen canonical truth the Dart port was
  golden-tested against (AGENTS.md, repo law); PR #1 changed that truth
  deliberately and with tests, so the port target moved with it.
- **Evidence (FACT, 2026-11-14/15):** PR #1 merged `5f78388`
  ("fix(python): chain renames, honest manifests, symlink skip, exit
  codes + tests", 18 tests in `tests/test_rename.py`): chain renames
  resolve in dependency order (`_execution_order`), manifests are
  written after the dry-run/execute decision with honest
  `planned`/`renamed`/`skipped` statuses (`renamed_ops`), `--undo`
  refuses non-`renamed` rows, symlinks are skipped and counted
  (`skipped_symlink`, `rename_camera_prefixes.py:171`), exit code 2 on
  any collision/skip, 0 only when `renamed == to_rename`. Gates verified
  locally: pytest 18 passed, flake8 hard-select clean, py_compile clean.
- **What changes if Glenn disagrees:** if any PR #1 behavior is judged
  wrong, the fix lands in Python FIRST (with tests), and S2/MP8 re-target
  to the corrected Python. The port never silently diverges from Python
  to "fix" a mismatch.

### REC-09 — Workflow files change via the web editor, not the API

- **What:** any change under `.github/workflows/` is made through the
  GitHub web editor (or a direct git push from a machine with
  credentials), never through the Muse GitHub connection's file APIs.
- **Why:** the connection returns `403 Resource not accessible by
  integration` for creating/updating files under `.github/workflows/`
  (the workflow permission is not granted); deletions do go through.
  PR #2 proved both halves of this in one PR.
- **Evidence (FACT, 2026-11-15):** PR #2 merged `d103ef6`: the
  `python-publish.yml` deletion went through the API (`a4fe233`); the
  `python-app.yml` update failed via both `push_files` and
  `create_or_update_file` (SHA `1168bd9a…`) with 403 and landed via the
  web editor instead (`efad35b`, verified byte-identical on the remote).
  The new `python-app.yml` runs `py_compile` + `pytest -q tests/` on
  Python 3.14 / setup-python v5.
- **What changes if Glenn disagrees:** the alternative is getting the
  workflow permission added to the Muse GitHub App — a Glenn-side
  account/settings action, not something to retry from here. Do not keep
  retrying the API routes for workflow files; the 403 is a permission
  fact, not a transient error.

### REC-10 — Flutter stays the frozen reference until MP9 — no partial retirement

- **What:** the Flutter tree (`app/` + `packages/` + Flutter tooling)
  stays on `main`, building, untouched except by explicit authorization
  naming a discrepancy, until MP9's full criteria checklist passes AND
  Glenn approves retirement at the gate. No slice deletes part of it
  early.
- **Why:** the reference is the only behavioral baseline the parity runs
  measure against; deleting it early destroys the evidence MP8 needs.
- **Evidence:** FLUTTER_REFERENCE_BASELINE.md §13 (frozen; changes only
  by explicit authorization naming the discrepancy); migration blueprint
  §15 (ALL criteria required); ADR-005 (MP9 exit: criteria checklist
  signed); ADR-005 non-decision Q5 (keep Flutter building during
  migration — recommend yes).
- **What changes if Glenn disagrees:** retirement (or partial removal)
  becomes a Glenn decision with its own checklist evidence, recorded in
  the PR that does it. The MP0 tag `flutter-final` (`aae04f9`) and the
  manifest are never the things that get "cleaned up" as part of it —
  see S6 §8 for the tag-name discrepancy that must be resolved first.

### REC-11 — Honesty over completeness, at every layer (unknown > invented)

- **What:** unknown renders as `—` (Viewer), `unknown` provenance
  (models), and `skipped`/`unreadable` statuses (rename/manifest) —
  never a guessed value, never a silent substitution. Manifests are
  written after the dry-run/execute decision, never before.
- **Why:** the master blueprint's stated purpose is archival-grade safety
  and honest provenance; a wrong value is worse than a missing one for
  files the user cannot re-create.
- **Evidence:** BP-02 (missing = `—`; provenance chip per row);
  CHECKPOINTS.md §G2 ("No invented values"); PR #1's honest-manifest fix
  (manifest lied pre-PR #1: status always `"renamed"` even on dry-run);
  CURRENT_STATE.md K4 (orientation lies — correct before display).
- **What changes if Glenn disagrees:** any "helpful" inference (e.g.
  mtime-as-EXIF, filename-derived dates) needs its own ADR + a distinct
  provenance value so invented data is always distinguishable from
  measured data. It is never a quiet default.

## 3. Risk register (standalone v1)

Carried from migration blueprint §14 (R-01–R-08), updated with the
2026-11-14/15 facts. Likelihood/impact are the blueprint's ratings
unless noted.

### R-01 — SAF provider variance (OEM/cloud/slow providers break assumptions)

- **Likelihood / Impact:** HIGH / HIGH.
- **Mitigation:** per-document try/catch, timeouts, virtual-doc rule,
  fake-provider matrix tests (MP3's 17/17 Robolectric suite).
- **Validation:** Moto G + second OEM if available (§5 Q4).
- **Fallback:** raw-path lane retained where valid.

### R-02 — Retriever/EXIF OEM variance (HEIC/DNG/odd containers)

- **Likelihood / Impact:** MEDIUM / HIGH.
- **Mitigation:** contract vectors + unknown-grace (honest `—`, REC-11);
  K5 fallback decided separately, not invented in a slice (S5 Q2).
- **Validation:** device suite + real shots (MP8 real-shot smoke).
- **Fallback:** per-OEM quarantine notes (tagger pattern).

### R-03 — Thumbnail OOM on huge/odd files

- **Likelihood / Impact:** MEDIUM / HIGH.
- **Mitigation:** bounds-first decode, 64 MB source cap
  (`PhotoThumbs.kt:69`), bounded cache (100 / 32 MB), software-decode
  asserts; the Viewer adds no full-res decode (S5 Q4 decides that
  separately).
- **Validation:** hostile fixtures on device.
- **Fallback:** lower caps.

### R-04 — Compose/Kotlin/AGP coupling at MP1

- **Likelihood / Impact:** MEDIUM / MEDIUM.
- **Mitigation:** pin BOM to the AGP 9.1 lane; no upgrades mid-migration
  (REC-04).
- **Validation:** MP1 foundation build green; re-verify at each slice's
  host gate.
- **Fallback:** Views per REC-01 (kept warm, not built).

### R-05 — Test-fidelity gap (Robolectric ≠ device)

- **Likelihood / Impact:** MEDIUM / MEDIUM.
- **Mitigation:** Robolectric for logic, device for platform truth (the
  existing split: JVM suites + instrumented suites on Moto G).
- **Validation:** MP4/MP5 device 6/6 each; MP6 device re-run is S1 and
  gates S5's device evidence.
- **Fallback:** device-suite failure blocks the slice; host-green alone
  never closes a device-gated item.

### R-06 — Momentum loss across a 4–6 week port

- **Likelihood / Impact:** MEDIUM / MEDIUM.
- **Mitigation:** golden-first slices (every MP lands green); reference
  stays runnable (REC-10).
- **Validation:** per-slice gates; the spec PR itself sequences S1–S6 so
  no slice starts without its inputs.
- **Fallback:** pause at a slice boundary with `main` green — the
  one-track-at-a-time rule, not parallel PR builds.

### R-07 — Scope creep into MediaStore / Phase 3 edits / advanced lane

- **Likelihood / Impact:** LOW / HIGH.
- **Mitigation:** frozen contracts + diff audit per slice; explicit
  non-goals in every slice spec (S5 §2, S6 §2); REC-05, REC-11.
- **Validation:** PR-body checklist review against the slice's stated
  non-goals before marking ready.
- **Fallback:** the creeping item becomes a new slice with its own spec
  and ADR — it does not ride inside the slice where it appeared.

### R-08 — URI-permission loss across reboots/updates

- **Likelihood / Impact:** LOW / MEDIUM.
- **Mitigation:** persisted grants + re-request flow + state preservation
  tests (S3's slice; migration blueprint §6).
- **Validation:** S3 device test: revoke + re-grant flow on the Moto G.
- **Fallback:** re-request flow with preserved state (never a crash or a
  silent empty grid).

### R-09 — GitHub API cannot write workflow files (new, 2026-11-15)

- **Likelihood / Impact:** certain (proven) / LOW-MEDIUM. It blocks
  CI changes via the normal PR flow, not product work.
- **Mitigation:** REC-09 — web-editor route for `.github/workflows/`;
  keep workflow changes out of spec/implementation PRs where possible
  (a workflow change inside a spec PR would hit the same 403).
- **Validation:** PR #2's `efad35b` (web editor) verified byte-identical
  on the remote; `python-app.yml` now runs the real test step.
- **Fallback:** Glenn-side permission grant on the Muse GitHub App, or a
  direct git push from a credentialed machine. Do not retry the API.

### R-10 — Reference drift: the frozen Flutter tree is edited by accident

- **Likelihood / Impact:** LOW / HIGH. A quiet edit to `app/` or
  `packages/` invalidates every parity claim MP8 will make.
- **Mitigation:** `tools/verify_reference.py` exit 0 as a gate in every
  slice that touches the repo (S5 §6, S6 §8); manifest SHA-256 per file.
- **Validation:** run the checker before and after each slice's work.
- **Fallback:** the manifest + tag `flutter-final` (`aae04f9`) identify
  exactly what changed; restore from the tag, never patch forward.

### R-11 — Canonical rename semantics drift between Python and the native port

- **Likelihood / Impact:** MEDIUM / HIGH. PR #1 moved the canonical
  truth; a port built from stale memory (pre-PR #1 semantics: lying
  manifests, half-failed chains, renamed symlinks) would pass its own
  tests and fail parity.
- **Mitigation:** REC-08 — S2 ports the `d103ef6` Python, MP8 replays
  the goldens + `parity_probe.py` against the port.
- **Validation:** MP8 golden replay byte-identical; `parity_probe.py`
  6/6 + byte proof against native outputs.
- **Fallback:** fix in Python first (with tests), re-target the port.
  Never adjust a golden to match a diverged port.

## 4. What this document does NOT decide

- Any item in §5. Recommendations are the author's judgment from the
  cited sources; Glenn's call on each open question is recorded when he
  makes it, not assumed here.
- S1–S4 slice contents (MP6 device re-run, native rename port, SAF
  picker/grants, Renamer screen) — this file references them only as
  dependencies and as the source of the facts above.

## 5. Open questions (for Glenn — do NOT decide in this PR)

- **Q1 — Coil revisit (from S5 Q3):** hand-rolled thumb loading for v1,
  or revisit Coil now for crossfade/AsyncImage ergonomics?
- **Q2 — K5 HEIF/DNG fallback (from S5 Q2):** honest `—`, a second
  native probe, or a per-format quarantine note?
- **Q3 — Detail full-resolution image (from S5 Q4):** is the 256px thumb
  the detail image for v1, or does detail decode larger?
- **Q4 — Second OEM device:** is a second device wanted for R-01/R-02
  breadth, or does the Moto G remain the sole gate?
- **Q5 — MP9 archive-tag name (from S6 Q1):** `flutter-final` is taken
  by the MP0 tag; what is the MP9 archive tag called?
- **Q6 — Parity report location (from S6 Q3):** `docs/parity-mp8.md`,
  `blueprints/`, or PR body only?
- **Q7 — Paging 3 vs manual pages (ADR-005 non-decision):** manual paging
  is the default; revisit on measured need, or decide now?
- **Q8 — Keep Flutter building during migration (ADR-005 Q5):** confirm
  yes (recommendation), or accept a possibly-broken Flutter lane
  mid-migration?
