# PHASE_2_RESEARCH_MAP.md — Vision Engine (research + execution map, NO implementation)

> Status: RESEARCH → MAP. Frozen for operator REVIEW. Nothing here authorizes
> code. Promotion requires explicit operator sign-off per Scope law
> (`RULES.md` §4.4). Research date: 2026-09-15.

## 0. Authoritative definition (one line)

**Phase 2 = Metadata viewer (read-only first):** a read-only grid → detail
viewer for device-camera photos/videos with per-field provenance. Viewer
ships BEFORE any edit op. Honesty over completeness.

## A. SOURCE-DEFINED (explicit in project documents)

A1. Name + mission — `blueprints/ROADMAP.md:20` ("Phase 2 — Metadata viewer
(read-only first)"), `blueprints/blueprint-sections/BP-02-metadata-viewer.md:3`
("Viewer ships BEFORE any edit op. Honesty over completeness"),
`blueprints/VISION_ENGINE_MASTER_BLUEPRINT.md:43-48` (§4: photos via EXIF,
video via retriever, read-only grid → detail, unknown-as-`—`, provenance
chips, GPS→strip shortcut routing to BP-03).
A2. Work items — `ROADMAP.md:21-23`: `packages/metadata_core` models +
provenance + fixtures; adapter (`exif` + ExifInterface fallback, video
MediaMetadataRetriever bridge); viewer UI (grid → detail,
orientation-corrected, unknown-as-`—`, provenance chips).
A3. Gate G2 — `blueprints/CHECKPOINTS.md:15-18`: synthetic fixtures render
with corrected dims + `—` + accurate chips; no invented values (grep for
mtime-as-EXIF substitution); analyzer zero + host tests green; Moto G smoke
on operator-supplied shots, never committed.
A4. Non-goals — `BP-02:14-15`: no edits, no share, no slideshow, no
`ffmpeg_kit`. Master blueprint §6 + BP-04: advanced lane stays spec-only.
A5. Contracts — `blueprints/ARCHITECTURE.md:37-47` (metadata flow:
adapter probe → `metadata_core` model → viewer; never invent; orientation
corrected + raw-tag badged); `RULES.md` §§2–3 (Genesis header, lossless
guarantee, provenance travels, unknown > invented, PS mojibake ban,
ADR-gated deps); `ADR-001` (pure `packages/` vs `app/adapters/`);
`ADR-002` (dep ceiling; `exif`/`image`/`mime`/`video_player` pre-allowed,
`ffmpeg_kit`/`tflite` need fresh ADR).
A6. Field contract — `docs/android-metadata-matrix.md`: JPEG full EXIF;
HEIF/DNG via native fallback; PNG/WebP screenshots carry no EXIF
(filesystem dates labeled as such); video duration/dims/rotation/creation/
location via retriever; never guess codec from extension.
A7. Sequencing — viewer (Phase 2) before edits (Phase 3, BP-03 plan →
preview → confirm → execute → re-read); GPS-strip shortcut routes to
Phase 3, never edits inline.

## B. EVIDENCE-DEFINED (proven by implementation/test/device)

B1. G0/G1 closed: parity GREEN (6/6 plan cases + SHA-256 byte proof),
22/22 + 5/5 tests, analyzers clean (`SESSION_HANDOFF.md`, `CHECKPOINTS.md`).
B2. D2 CLOSED/PASS (Moto G 2025, human-pasted reports 2026-09-15):
RAW_RENAME=WORKS in SAF-picked `Pictures/VE_TEST` (2/2 renamed, 0 errors,
2/2 undo, cleaned, COMPLETE ×2); photos/videos granted, storage denied
(expected 33+). Scope boundary: probe files were APP-CREATED. Renaming
third-party (camera-created) files is NOT proven — that is G5/DCIM
territory, explicitly deferred, never probed.
B3. Toolchain device-proven: compileSdk 37 (permission_handler_android
requires it), AGP 9.1.0 + Gradle 9.3.1, debug APK launches; targetSdk 36
(Flutter-pinned), minSdk 26.
B4. App skeleton exists: Rename/Probe/Settings tabs, `LocalRenameAdapter`
(preview/execute/undo over dart:io), `SettingsStore`, file_picker 12
(static `getDirectoryPath`), permission_handler wired (K9/K11 closed).
B5. Platform (verified 2026-09-15 against developer.android.com +
support.google.com): SAF `ACTION_OPEN_DOCUMENT_TREE` (API 21+) with
persistable URI grants (`takePersistableUriPermission`); Android 11+
tree-request bans (storage root, Download, Android/data); Android 13+
granular `READ_MEDIA_IMAGES/VIDEO` (legacy capped at 32 — matches our
manifest); Android 14+ partial access (`READ_MEDIA_VISUAL_USER_SELECTED`
— future consideration, not Phase 2 scope); ExifInterface reads
JPEG/PNG/WebP/HEIC/DNG/RAW/AVIF(31+) but WRITES only JPEG/PNG/WebP
(HEIC/DNG save unsupported; DNG save support was removed for corruption —
grounds K5 and constrains Phase 3, noted here only as a dependency);
`MANAGE_EXTERNAL_STORAGE` is Play-restricted with media access explicitly
ineligible — operator ban aligns with Play policy.

## C. PROPOSED / INFERRED (NOT roadmap — offered for review)

C1. Thumbnail source: BP-02 says "MediaStore thumbs", but D2 kept the raw
path primary. Propose Flutter `Image.file` with `cacheWidth` + bounds
decode via the pre-allowed `image` package (no new dep, offline-first);
MediaStore thumbs only if a MediaStore path is ever adopted. DECISION
REQUIRED at authorization.
C2. Folder source for the viewer grid: reuse the existing SAF folder pick
(`file_picker`) + raw listing (same validated path as D2), NOT a
MediaStore query. DECISION REQUIRED.
C3. First-implementation slice: `packages/metadata_core` models + fixtures
+ tests only (no app code, no new deps) — smallest Atlas-style vertical
unit. PROPOSED, not yet authorized.
C4. Android 14+ partial-access handling (`READ_MEDIA_VISUAL_USER_SELECTED`)
is G5/future work, NOT Phase 2: Phase 2 reads files the user already
granted via folder pick or app-private demo. FLAGGED, not scoped.

## D. Contradictions / ambiguities found (not silently resolved)

D1. `ARCHITECTURE.md:31,35` + `VISION_ENGINE_MASTER_BLUEPRINT.md:37`
prescribe "shared collection → MediaStore `DISPLAY_NAME` update". D2
evidence validates SAF-pick + raw rename instead, and the operator froze
that path with MediaStore deferred. The architecture lines predate D2 and
now contradict evidence + directive. PROPOSED RESOLUTION: reword to
"user-picked shared folder → raw rename (D2-validated primary);
MediaStore path deferred unless future evidence demands it" — queued for
the next authorized task, NOT edited in this research pass.
D2. BP-02 "MediaStore thumbs" vs raw-path primary — see C1.
D3. Terminology collision (minor): `rename_camera_prefixes.py:207,368`
calls plan "Phase 1" and execute "Phase 2". Those are the SCRIPT's
two-step operation and are UNRELATED to project Phases 0–5. Noted to
prevent cross-reading; no doc change proposed.
D4. No contradiction on Phase 2's meaning: ROADMAP, BP-02, CHECKPOINTS G2,
CHECKLIST, AGENTS, SESSION_HANDOFF all agree (read-only metadata viewer).

## E. Full Phase 2 execution map (authorization pending)

1. Mission: honest read-only metadata viewer (A0).
2. Why: renaming blind is unsafe; operators verify what files ARE (date,
   GPS, orientation) before Phase 3 edits. Viewer-first is the Scope-law
   vertical slice.
3. Preconditions: G0/G1/D2 as recorded (B1–B4). Nothing else.
4. Inputs: `rename_core` patterns (golden fixtures, parity probe method),
   `LocalRenameAdapter` listing code, SAF pick + permission flows, K4/K5,
   matrix doc, ADR-001/002.
5. Non-goals (A4 +): no EXIF writes, no pixel ops, no share-sheet, no
   slideshow, no ffmpeg/tflite, no new permissions, no DCIM targeting,
   no `MANAGE_EXTERNAL_STORAGE`, no `READ_MEDIA_VISUAL_USER_SELECTED`
   handling.
6. Architecture affected: NEW `packages/metadata_core` (pure Dart);
   NEW `app/lib/adapters/metadata_adapter.dart`; NEW viewer feature
   folder; EXTEND bottom nav (Viewer tab). `rename_core`, probe, settings
   untouched.
7. Files/modules likely affected: `packages/metadata_core/{pubspec,
   lib, test/golden}`; `app/lib/adapters/metadata_adapter.dart`;
   `app/lib/features/viewer/*`; `app/lib/main.dart` (nav only);
   `app/pubspec.yaml` (pre-allowed deps only); `docs/` + `blueprints/`
   (gates as they pass).
8. Invariant contracts: A5 (pure-core boundary, provenance, unknown >
   invented, no invented datetimes, orientation corrected + badged,
   lossless guarantee untouched, analyzer-zero, explicit-path git).
9. Data-flow changes: ADD read-only flow (ARCHITECTURE §3 as specced).
   No change to rename plan→execute→undo flow. No writes anywhere.
10. UI changes: ADD Viewer tab (grid → detail sheet, provenance chips,
    `—` unknowns, GPS row with disabled-until-Phase-3 strip affordance
    that routes, never acts). No changes to Rename/Probe/Settings visuals.
11. Android implications: minSdk 26 stays (ExifInterface via AndroidX back
    to 26 for JPEG baseline; HEIC read needs newer OS — degrade to `—`
    with provenance, per honesty rule). No manifest changes. No Kotlin
    unless the retriever bridge needs it (video) — small channel mirroring
    the tagger `storage`-channel pattern; Java/Kotlin only if Dart-side
    probing proves insufficient.
12. Storage implications: NONE (read-only). Read via existing granted
    paths only. No persisted URIs beyond what the picker already holds;
    no new persistence unless C2 review demands it (then: URI string in
    SharedPreferences, same pattern as SettingsStore).
13. Permission implications: NONE new. Existing READ_MEDIA_* + legacy
    read cover other-app media reads; SAF grant covers tree reads.
    Partial-access (14+) explicitly out of scope (C4).
14. Test strategy (Atlas posture): `metadata_core` unit + golden JSON
    fixtures (EXIF dump → model; orientation-correction vectors; unknown
    fields); adapter tests on synthetic files in temp dirs (host);
    widget tests with injected adapter + temp media (runAsync law, K9);
    `integration_test/` authored but human-run only (RULES §1.5).
15. Device validation: Moto G smoke per G2 — operator opens viewer on own
    shots, pastes grid/detail observations; media NEVER committed.
    Minimum bar: JPEG + one HEIF + one MP4 read correctly; one EXIF-less
    PNG shows `—` with filesystem dates labeled.
16. Failure modes: unreadable/corrupt file → row shows `—` + `unknown`
    provenance, never crash; missing permission → degraded viewer state
    (existing pattern); huge image → bounds-decode only, no full
    in-memory load (OOM guard); HEIC on old OS → `—` + note.
17. Rollback: read-only → rollback = revert commit(s) by explicit path;
    no user data at risk (nothing written). Feature-flag NOT required;
    tab may ship hidden behind Settings toggle if operator prefers
    (decision at authorization).
18. Security/privacy: viewer SURFACES GPS — detail screen must not
    screenshot-leak by design beyond OS behavior; no network calls (add a
    grep gate: viewer imports must not include `http`); no analytics;
    synthetic fixtures only.
19. Performance: grid scroll 60fps via thumbnails ≤256px + cacheWidth;
    EXIF parse off UI thread for folders >100 files (isolate only if
    measured jank — no premature abstraction); no new native deps for
    perf until proven need (ADR-002).
20. Documentation required: G2 flips in CHECKPOINTS/CHECKLIST/ROADMAP/
    CURRENT_STATE + handoff entries per session law; `docs/` note only if
    a new K-lesson is banked; no blueprint rewrites (frozen v1.0).
21. Git strategy: explicit paths only; slice commits (core → adapter →
    UI → gates); messages = gates status; push per operator order; no
    force, no rebase of public history.
22. Definition of Done: G2 all green (source: CHECKPOINTS G2).
23. Exit criteria: fixtures render correctly + no-invention grep clean +
    analyzers zero + host tests green + human Moto G paste accepted +
    docs ticked + pushed. Then Phase 2 CLOSED; Phase 3 still requires
    separate authorization.
24. Dependencies on later phases: GPS-strip shortcut is DEAD-ENDED UI
    until Phase 3 authorized (must route, never act); orientation-fix and
    date-shift likewise. Slideshow (BP-04) must not reuse viewer grid
    selection state without its own ADR (scope hygiene).
25. Explicitly deferred: everything in A4 + C4 + MediaStore adapter +
    DCIM targeting + `MANAGE_EXTERNAL_STORAGE` + any cloud/ML + icon/
    theme polish.

## F. Exact first implementation task (for authorization, NOT started)

Scaffold `packages/metadata_core` ONLY: `pubspec.yaml` (sdk
`>=3.0.0 <4.0.0`, dev `test` + `lints`), `lib/{metadata_core.dart,
src/provenance.dart, src/photo_meta.dart, src/video_meta.dart,
src/fixtures.dart}`, `test/golden/{photo_exif.json,
video_meta.json, orientation_vectors.json}` + unit tests, Genesis headers,
`dart analyze` + `dart test` green. No app code. No new hosted deps.
Estimated: one session slice. Gate: package-level analyze/test only.

## G. Unresolved questions for operator review

GQ1. C1 thumbnail source (`image`-bounds vs MediaStore thumbs)?
GQ2. C2 viewer folder source (SAF-pick reuse vs MediaStore query)?
GQ3. Resolve D1 architecture-line reword in the same authorization (recommended)
    or a separate docs task?
GQ4. Viewer tab visible immediately at Phase 2 close, or behind a Settings
    toggle until Phase 3 lands?
GQ5. Confirm G2's "one HEIF + one MP4 + one EXIF-less PNG" device minimum,
    or name the exact device set.
