# Standalone v1 — Vision Engine, finished as its own app (spec)

> Status: SPEC + IMPLEMENTATION RECORD. Drafted 2026-11-15 against
> main `d103ef6` (PR #1 Python fixes + PR #2 CI fixes merged).
> Slices S1–S4 are implemented on main `18c45c6` (PRs #4–#8, merged
> 2026-11-16–17); S5 is next, S6 after. The spec text below is
> preserved; implemented slices are recorded in §1–§2 and §5, and
> each slice file carries its own implementation record.
>
> Fact discipline (per
> `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §0): FACT =
> verified in repo/docs; REPO EVIDENCE = file:line or gate result;
> RECOMMENDATION = judgment; INFERENCE = strong but unverified;
> OPEN QUESTION = needs a decision or a device. Assumptions are never
> presented as facts.

## 0. The decision this spec carries out

- **FACT:** On 2026-11-14 Glenn decided **Vision standalone FIRST —
  finish the renamer + viewer** before lending the engine to
  Sovereign Tagger / IndiOSINT. The shared media engine direction is
  wanted, but the standalone app comes first.
- **FACT:** Vision Engine started as Glenn's own script to stop
  manually renaming hundreds of device-camera photos/videos in a file
  manager. He wrote the script, tested it on a copied/quarantined
  DCIM folder, it worked, then wrote the second script. Flutter was
  chosen for the app because of the file picker (and "something else"
  he no longer recalls). The shared media engine was his initial idea
  once it became an app — "hence the name 'Vision Engine'."
- **FACT:** The repo's stated purpose (master blueprint v1.0, frozen
  2026-09-14) is an Android-first, offline-first camera-file studio:
  (1) safe archival-grade prefix rename, (2) honest
  provenance-tagged metadata viewer, (3) simple lossless
  manipulation. Non-goals: no cloud/accounts/analytics/ads/AI-tagging/
  duplicate-finder/gallery replacement/iOS/web/desktop.
- **RECOMMENDATION:** "Finished" for v1 means the first two of those
  three, native, on a real folder: pick DCIM, preview, execute, undo,
  done in 30 seconds — plus a read-only Viewer over the same folders.
  Phase 3 edits and the advanced lane stay out of v1 (see §4).

## 1. Where things stand (verified 2026-11-14/15)

| Layer | State |
|---|---|
| Python canonical scripts | `rename_camera_prefixes.py` + `rename_min.py`; PR #1 (merged `5f78388`) fixed chain renames, honest manifests, symlink skip, exit codes; PR #8 (merged `18c45c6`) added exclusive manifest creation, manifest alias guard, dependency-aware undo; 30 tests green (18 + 12 safety). Python is stdlib-only, frozen as the canonical truth the Dart port is golden-tested against. There is NO Chaquopy / Python-in-Android in this repo. |
| Flutter `app/` + `packages/` | Frozen at tag `flutter-final` (`aae04f9`). Rename UI works only in its own app-private `vision_demo/` folder; the Probe tab is the only real-folder touch (D2-validated SAF-pick + raw rename on Moto G). Metadata/thumbnail/pipeline code is real and tested but has no UI. |
| Native Kotlin MP0–MP6 | Read-only metadata + thumbnail pipeline library (55 core + 51 android JVM tests; MP4/MP5 device 6/6 each on Moto G). MP6 device run DONE 2026-11-17 — 13/13 PASS on BLU B160V, Android 14 (hardware substitution for the Moto G; no Moto G coverage claimed). Native rename (PR #4), folder picker + grants (PR #5), and Renamer screen (PR #6 + PR #7 fix) are all merged on main `18c45c6`. No Viewer yet (S5 next). |
| CI | PR #2 (merged `d103ef6`): `python-app.yml` runs `py_compile` + `pytest -q tests/` on Python 3.14; `python-publish.yml` deleted (no package to publish). No native Gradle CI exists — the only workflow is `python-app.yml`; host Gradle gates run on Glenn's laptop. |

## 2. The slices, in order (each is its own file here)

| Slice | File | What it is | Hard prerequisites |
|---|---|---|---|
| S1 | `S1-MP6-DEVICE-RERUN.md` | Re-run the authored MP6 instrumented device suite (13 tests) on Glenn's Moto G + laptop. Glenn-side; no code. **Done 2026-11-17: 13/13 PASS on BLU B160V (Moto G → B160V substitution); self-containment confirmation still PENDING.** | None (needs laptop + phone together) |
| S2 | `S2-NATIVE-RENAME-PORT.md` | Port `stripLeadingPrefix` + `buildPlan` + manifest codec + reverse-order undo into pure-JVM `vision-core`, replaying the 6 Dart goldens byte-identical. Post-PR #1 Python semantics. **Done: PR #4, merged 2026-11-16 (`4ef05e7`).** | None (pure JVM, no device) |
| S3 | `S3-SAF-PICKER-GRANTS.md` | System folder picker + persisted URI grants (DataStore), re-request flow, top-level-only enumeration via existing `SafDiscovery`. **Done: PR #5, merged 2026-11-16 (`2223beb`). Revoke-then-re-pick NOT verified — open item.** | — (S2 not required, but S4 needs both) |
| S4 | `S4-RENAMER-SCREEN.md` | Compose Renamer screen on a real folder: preview → execute → honest manifest → undo, staged smoke (app files, then copied/quarantined DCIM; live DCIM never first). **Done: PR #6, merged 2026-11-17 (`5ea6c2c`), + PR #7 fix, merged 2026-11-17 (`b0d2b36`); S4B/S4C green on B160V. Stage (c) live DCIM NOT run.** | S1, S2, S3 all green |
| S5 | `S5-MP7-VIEWER.md` | MP7 Viewer: Compose grid → detail, corrected dims, `—` for unknown, provenance chips, GPS strip button disabled (Phase 3). Closes G2 with the real-shot smoke. **Next — not started.** | S1 green (pipeline device-proved) |
| S6 | `S6-MP8-MP9-PARITY-RETIREMENT.md` | MP8 side-by-side reference-vs-native parity on hardware + G2 sign-off; MP9 Flutter retirement criteria + atomic removal, Glenn's call at the gate. **After S5.** | S1–S5 done |

`RECOMMENDATIONS.md` consolidates the cross-slice recommendations
(REC-01–REC-11) and the risk register (R-01–R-11). `CHECKLIST.md`
is the master gate checklist for the whole v1; each slice file holds
its own item-level checklist.

Slice order is load-bearing: S4 must not start until S2's package is
on `main` green and S3's grants exist — no temporary engine in the
app module, no stubbing the rename to "get the screen going" (that is
the drift the canonical rule exists to prevent).

## 3. What is deliberately NOT in v1

- Rename templates / date-based naming (Phase 3).
- Any edits: EXIF date-shift, GPS-strip, orientation-fix, crop,
  resize/compress (Phase 3, BP-03).
- Advanced lane: bg-erase, upscale, slideshow/video-with-music
  (BP-04, spec-only until Phases 1–3 pass).
- MediaStore enumeration (deferred; SAF-first stands).
- `MANAGE_EXTERNAL_STORAGE` (banned; D2 did not demand it).
- Recursive rename (top-level only in v1; separate spec later).
- Coil (hand-rolled thumb loading for v1; revisit is S5 Q3).
- Any cloud, accounts, analytics, ads, network, or geocoder.
- iOS / web / desktop (Android-only forever, GQ8=NO).

## 4. How to use this spec

1. Read this README, then the slice file for the work at hand.
   Each slice states its inputs (exact files), step-by-step work,
   acceptance checklist, gates, device evidence required, risks,
   and open questions.
2. Implementation is one PR per slice (smaller slices may split
   further, never merge two slices into one PR). Every PR states
   its slice + open-question answers in the PR body; no
   decision-grade choice is settled inside a PR body without
   Glenn's nod.
3. Gates green → handoff + state docs updated (RULES.md §4.2) →
   commit by explicit path → Glenn merges. One track at a time,
   no parallel PR builds.
4. Device work is Glenn-side (Moto G + laptop). Host gates alone
   never close a device-gated item; emulator ≠ device.

## 5. Open decisions (consolidated — each lives in its slice file)

These are Glenn's calls. Decisions settled by the merged work are
marked decided with the PR reference; the rest stay open. The
original "none is settled" line from the 2026-11-15 draft is
superseded by the entries below (2026-11-17).

| # | Question | Where | Status (2026-11-17, main `18c45c6`) |
|---|---|---|---|
| D1 | Rename mechanism from a SAF tree URI: `DocumentsContract.renameDocument` vs raw `File.rename`? (Also decides S3 grant flags: READ-only vs READ+WRITE.) | S3 Q1/Q2, S4 Q1 | **Decided 2026-11-15 (Glenn): `renameDocument` + READ+WRITE grants.** PR #5 / PR #6. |
| D2 | Verify-after-write: how do we know a rename really happened? | S4 Q2 | **Decided (PR #6):** re-query the document after the op; null/mismatch → UNVERIFIED, never counted renamed. |
| D3 | Manifest placement: app-private only, or also offer export? | S4 Q3 | **Decided (PR #6):** app-private + `CreateDocument` export. |
| D4 | Manifest columns: confirm the 6-column Dart form (with `content_uri`) as the native standard? (Spec recommends yes; S2/S4 reconciled to one position before this PR.) | S2 Q1, S4 Q4 | **Implemented as recommended (PR #4 / PR #6):** six-column form with `content_uri`. Formal confirmation by Glenn still OPEN — do not read this as a confirmed decision. |
| D5 | Recursive rename: ever, or never in v1? (Spec recommends top-level only.) | S4 Q5 | **Decided (PR #6):** top-level only for v1. |
| D6 | Symlinks through SAF: visible at all? (Probe in the S4 smoke, or state the limitation.) | S4 Q6 | **Decided (PR #6):** undetectable through SAF — recorded limitation, `skippedSymlink` stays 0 on that path. |
| D7 | Execute confirmation: is "Execute N renames" enough, or a second dialog? | S4 Q7 | **Decided (PR #6):** no second dialog. |
| D8 | MP6 run timing + stale Gradle path in the runbook + flake policy + state-update vehicle. | S1 Q1–Q5 | **Partially decided:** the run happened 2026-11-17 on BLU B160V; the state update landed as its own docs PR. Flake policy and the stale runbook Gradle path still OPEN. |
| D9 | Native rename port details: sealed result type vs nullable+sentinel; `parity_probe.py` three-way; package layout; astral-plane sort pinning; chain-ordering home. | S2 Q2–Q6 | **Partially decided (PR #4):** package layout `vision-core/rename/`; astral-plane sort pinned by test; chain ordering in `ExecutionOrder.kt` (port of Python `_execution_order`); nullable + `""` sentinel. The `parity_probe.py` three-way (Dart regression) still OPEN — PR #8 added a chain-execution assertion but not the three-way comparison. |
| D10 | Saved folders: one slot or a growable list? Picker initial location? Launch health-check cost? Lost-grant expiry? | S3 Q3–Q6 | **Decided (PR #5):** growable list; picker opens near Pictures via `EXTRA_INITIAL_URI` (silent fallback); lookup-only launch health check, live re-check on folder-open; lost-grant entry kept until forgotten/re-picked. |
| D11 | Viewer: GPS "Strip…" button form; K5 HEIF/DNG fallback; Coil revisit; detail full-res decode; page size; video beyond a still frame. | S5 Q1–Q6 | **Open** — S5 not started. |
| D12 | MP9 archive-tag name (`flutter-final` is taken by the MP0 freeze tag); post-removal `verify_reference.py` story; parity report location; second OEM device; keep Flutter building during migration; Viewer comparison method. | S6 Q1–Q6 | **Open** — S6 not started. |

## 6. Known findings recorded by this spec (not fixes — fixes are later PRs)

- The Flutter Rename tab writes its manifest with hardcoded
  `status: 'renamed'` for every planned row
  (`app/lib/features/rename/rename_screen.dart:80`) — the exact lie
  PR #1 fixed in Python, never fixed in Flutter. S4 ports the honest
  statuses; the Flutter screen is frozen reference and is NOT
  patched.
- The migration blueprint says MP9's archive tag is `flutter-final`,
  but that tag already exists at the MP0 freeze commit `aae04f9`
  (verified live); ADR-005 instead names `flutter-reference-final`,
  which does not exist. S6 treats `flutter-final` as the MP0 freeze
  tag and leaves the MP9 archive-tag name to D12.
- The blueprint's §6 projection lists five columns; `SafDiscovery`
  has six (FLAGS added, needed for the virtual-document rule). The
  code is the authority; the blueprint line is stale in that detail
  (S3 §3.2).
- Python sorts by code point; Kotlin/Dart `String.compareTo` by
  UTF-16 code unit — they can differ for astral-plane (e.g. emoji)
  filenames. No fixture covers it; S2 §9.1 records it as a risk and
  D9 asks whether pinning it is worth a test.
  **Resolved by PR #4 (2026-11-16):** `PlanTest` pins the Kotlin
  UTF-16 sort with an astral-plane test.
- The GitHub API connection cannot create/update files under
  `.github/workflows/` (403, proven in PR #2); workflow changes go
  via the web editor. No workflow change is in this PR for that
  reason (REC-09).

## 7. Sources

- `blueprints/VISION_ENGINE_MASTER_BLUEPRINT.md` (v1.0, frozen
  2026-09-14), `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md`,
  `blueprints/ROADMAP.md`, `blueprints/CURRENT_STATE.md`,
  `blueprints/CHECKPOINTS.md`, `blueprints/CHECKLIST.md`,
  `blueprints/blueprint-sections/BP-01`–`BP-05`,
  `blueprints/decisions/ADR-001`–`ADR-005`,
  `blueprints/FLUTTER_REFERENCE_BASELINE.md`,
  `docs/device-validation-mp6.md`, `docs/device-probe-moto-g.md`,
  `docs/android-metadata-matrix.md`.
- Code as of main `d103ef6`: `rename_camera_prefixes.py` (467
  lines, post-PR #1), `packages/rename_core/`,
  `packages/metadata_core/`, `packages/media_library/`,
  `native/vision-core/`, `native/vision-android/`,
  `native/vision-app/`, `tools/parity_probe.py`,
  `tools/verify_reference.py`.
