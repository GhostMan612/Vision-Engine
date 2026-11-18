# S5 — MP7 Viewer: Compose grid → detail (IMPLEMENTED — draft PR #10)

> Status: IMPLEMENTED as draft PR #10 (branch
> `feat/mp7-native-viewer`, base main `9c3a2aa`), 2026-11-17.
> Host Gradle gate + device evidence PENDING — the PR is a draft,
> not merged, and G2 stays OPEN until the instrumented Viewer
> tests + the real-shot read-only smoke pass (see §8 and
> `S5-DEVICE-RUNBOOK.md`). The spec text below is preserved;
> deviations and decided questions are recorded in the
> implementation record (§11).
>
> Fact discipline (per
> `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §0): FACT =
> verified in repo/docs; REPO EVIDENCE = file:line or gate result;
> RECOMMENDATION = judgment; INFERENCE = strong but unverified;
> OPEN QUESTION = needs a decision or a device. Assumptions are never
> presented as facts.

## 1. Goal

Build the first native Viewer in `vision-app`: a read-only Compose
grid → detail screen over the folders a user has already picked, showing
honest, provenance-tagged metadata for device-camera photos and videos.
This slice closes the G2 gate (with the real-shot smoke) and is the first
screen a human actually looks at — everything before it (MP0–MP6) is
plumbing with no UI.

- FACT: the Viewer is unwritten in both stacks. The migration blueprint
  states "the Viewer is unwritten in both stacks (why now is cheapest)"
  and lists it as MP7, "Native Viewer (== 2D-native): Compose grid/detail
  per G2 contract."
- REPO EVIDENCE: `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:1`
  (§1 P) and §11 (MP7); `blueprints/ROADMAP.md` Phase 2 ("Viewer UI still
  out of scope", G2 gate text); `blueprints/blueprint-sections/BP-02-metadata-viewer.md`
  (G2 contract).
- FACT: `vision-app` today is a placeholder shell — two `Text` lines,
  zero permissions, no screens.
- REPO EVIDENCE: `native/vision-app/src/main/kotlin/com/visionengine/app/AppRoot.kt`
  (40 lines; `AppRoot` renders `state.appName` and "Native foundation
  ready" / "Starting").

## 2. Non-goals

- Any edit of any kind: no date-shift, no GPS-strip, no orientation-fix,
  no crop/resize/compress. Those are Phase 3 (BP-03) and G3, not this slice.
- No rename from the Viewer. Renaming is S4's screen; the Viewer is
  read-only and must not gain a rename affordance as a side effect.
- No slideshow, no share, no video playback beyond a still frame.
  (BP-02: "No edits, no share, no slideshow.")
- No MediaStore enumeration. SAF-first stands; the Viewer reads only
  folders the user picked through S3.
- No network, no geocoder, no map. GPS is displayed as coordinates only,
  and only if the file carries them.

## 3. Inputs (exact files — read before building)

### 3.1 Contracts this slice must not re-decide

| File | What it pins |
|---|---|
| `blueprints/blueprint-sections/BP-02-metadata-viewer.md` | G2 contract: grid → detail, orientation-corrected dims with raw-tag badge, missing = `—`, provenance chip per row, GPS row → "Strip…" routing to BP-03 (never edits inline). |
| `blueprints/CHECKPOINTS.md` §G2 | Gate text: fixtures render with correct orientation-corrected dims, `—` for missing, provenance chips accurate; no invented values (grep: no silent mtime-as-EXIF substitution); analyzer zero + host tests green; Moto G smoke pasted by human. |
| `blueprints/ROADMAP.md` Phase 2 | Viewer UI still out of scope as of the last ROADMAP update; G2 gate text (synthetic fixtures + Moto G smoke on real device shots — user-supplied, never committed). |
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` §4, §7–§9 | Compose `LazyVerticalGrid` recommendation; 11 photo + 10 video fields; 256px thumbs, orientation-applied; sequential probing default; bounded cache. |
| `blueprints/CURRENT_STATE.md` K4, K5 | K4: EXIF orientation lies about W×H — correct before display. K5: HEIF/DNG EXIF coverage differs Dart-`exif` vs native ExifInterface — fallback required (open). |

### 3.2 Native layers this slice builds on (already landed, MP4–MP6)

| File | What it provides |
|---|---|
| `native/vision-core/src/main/kotlin/com/visionengine/core/PhotoMeta.kt` (42 lines) | 11 fields as `MetaField<T>`; corrected `width`/`height` swap raw dims when `orientation in 5..8` (`orientationSwapsAxes`, lines 7–8, 23–40). |
| `native/vision-core/src/main/kotlin/com/visionengine/core/VideoMeta.kt` (41 lines) | 10 fields; corrected dims swap on rotation 90/270. |
| `native/vision-core/src/main/kotlin/com/visionengine/core/Provenance.kt` | `enum class Provenance { exif, container, filesystem, unknown }` (lines 7–12). |
| `native/vision-core/src/main/kotlin/com/visionengine/core/Cache.kt` | LRU bounds: `maxEntries = 100`, `maxBytes = 32 MB` (lines 39–40). |
| `native/vision-android/src/main/kotlin/com/visionengine/android/meta/ExifReader.kt` (146 lines) | FD-backed `ExifInterface` reads: orientation, raw W×H, datetime original, make/model, exposure, GPS. |
| `native/vision-android/src/main/kotlin/com/visionengine/android/meta/VideoReader.kt` (131 lines) | Retriever reads: duration, dims, rotation, creation time, location, codec. |
| `native/vision-android/src/main/kotlin/com/visionengine/android/thumbs/PhotoThumbs.kt` (157 lines) | Sampled decode, target width 256 (line 68), 64 MB source cap (line 69), JPEG-85 encode (line 116), explicit rotation. |
| `native/vision-android/src/main/kotlin/com/visionengine/android/thumbs/VideoFrames.kt` (144 lines) | First frame, `maxSide = 512` (lines 42, 64), JPEG-85 (line 115). |
| `native/vision-android/src/main/kotlin/com/visionengine/android/pipeline/MediaPipeline.kt` (233 lines) | `loadPage` (line 57) — sequential `for` loop over the page (line 64); thumbnail + extraction over MP4/MP5 layers. |

### 3.3 Reference baseline (do not modify)

- Tag `flutter-final` → commit `aae04f9`; `blueprints/FLUTTER_REFERENCE_BASELINE.md`
  + `blueprints/FLUTTER_REFERENCE_MANIFEST.json` + `tools/verify_reference.py`
  (52 lines; exit 0 = intact). The Flutter Viewer never existed, so there
  is no Flutter Viewer behavior to match — the contract is BP-02 + G2,
  not a port of an existing screen.

## 4. Step-by-step work

### Step 1 — ViewModel + state (thin, over `MediaPipeline`)

1. Add a `ViewerViewModel` (or extend the existing `MainViewModel` only
   if the diff stays smaller — RECOMMENDATION: new file, the placeholder
   `MainViewModel` is foundation scaffolding, not Viewer state) that
   exposes a `StateFlow` of: folder (from S3's persisted grant),
   current page of `MediaRecord`s, per-record probe state
   (loading / probed / failed), and per-record thumb state.
2. Paging reuses `MediaPipeline.loadPage` unchanged. No new paging
   mechanism in this slice (Paging 3 is an open question, §9 Q2).
3. Leaving the screen cancels in-flight thumbs/probes via the existing
   coroutine scope; completed records persist in the cache
   (migration blueprint §9 — already the pipeline's behavior).

### Step 2 — Grid screen

1. `LazyVerticalGrid` (migration blueprint §4 RECOMMENDATION: Compose
   `LazyVerticalGrid` + per-item async thumb loading matches the record
   model directly; Views would need RecyclerView + ViewHolder + adapter
   diffing for the same screen).
2. Each cell: 256px thumb (orientation-applied — the thumb layer already
   applies rotation; the grid must NOT re-rotate), file name, kind badge
   (photo/video), and a failure placeholder when the thumb state is
   `failed`/`unavailable` (never a blank cell that looks like a missing
   file, never a crash).
3. Grid shows records in the pipeline's folded-name order; the sort must
   not be re-implemented in the UI.
4. Empty states (each distinct, each tested):
   - No folder picked yet → prompt to pick (routes to S3), no fake content.
   - Folder picked, zero supported media → "No photos or videos here"
     with the folder name, not an error.
   - Folder picked, all files unsupported → count of skipped files, with
     the reason the pipeline already recorded (unsupported-as-record).
   - Permission lost (`SecurityException`) → re-request flow, state
     preserved (migration blueprint §6).
   - Probe failure on a record → that cell shows the failure state; the
     grid keeps rendering the rest (per-document try/catch is the
     pipeline's contract, not a UI invention).

### Step 3 — Detail screen

1. Tap a cell → detail screen for that record: full-width thumb (reuse
   the cached 256px thumb; do NOT decode a second full-resolution
   bitmap in this slice — see §9 Q4), file name, and the full field
   table.
2. Photo field table (11 fields, migration blueprint §7): datetime
   original, GPS latitude, GPS longitude, orientation (raw tag value
   shown alongside the corrected dims — the "raw-tag badge" from BP-02),
   make, model, exposure time, width (corrected), height (corrected),
   file size, MIME type.
3. Video field table (10 fields): duration (ms, displayed in a
   human-readable form — display formatting only, the stored value stays
   ms), width (corrected), height (corrected), rotation, creation time,
   location latitude, location longitude, codec, file size, MIME type.
4. Display rules (G2 contract, BP-02):
   - Orientation-corrected dims always; raw dims shown only as the badge.
     FACT: `PhotoMeta.width`/`height` already swap on orientation 5–8
     (`PhotoMeta.kt:23–40`); the UI must display those getters, never
     `widthRaw`/`heightRaw`, as the headline dims.
   - Unknown/missing = `—` for every field. No invented values: no
     mtime-as-EXIF substitution, no filename-derived date, no "0" shown
     as a real value. (G2 grep gate: "no silent mtime-as-EXIF
     substitution.")
   - One provenance chip per row, from `Provenance` (`exif` / `container`
     / `filesystem` / `unknown`) — the chip text is the enum name, nothing
     fancier.
   - GPS row carries a "Strip…" button that routes to Phase 3 (BP-03)
     with NO silent edit: in this slice the button is either disabled
     with a "Phase 3" label, or opens a placeholder that states GPS
     stripping is not implemented yet. RECOMMENDATION: disabled button
     with label — a routing target that does not exist yet must not look
     tappable. (See §9 Q1.)
5. Video detail shows the first frame as the still image (from
   `VideoFrames`); no playback control in this slice. A visibly
   trackless/unavailable frame shows the `unavailable` state, not a
   black rectangle that looks like a broken video.

### Step 4 — Cache behavior (bounded, already ported — wire, don't rebuild)

1. Grid thumb loading goes through the existing FNV-keyed LRU
   (100 entries / 32 MB, `Cache.kt:39–40`). The Viewer adds NO second
   cache and NO disk cache in this slice.
2. In-flight dedupe: scrolling fast over the same record must not spawn
   duplicate decodes (the pipeline's dedupe semantics — migration
   blueprint §1 N — already exist; the Viewer must call the pipeline,
   not decode directly).
3. Cache eviction while scrolling is expected and must not blank
   already-rendered cells in a way that looks like data loss; evicted
   thumbs re-decode on demand.
4. RECOMMENDATION: cap concurrent in-flight thumb decodes to the
   pipeline's existing sequential default. Bounded concurrency via
   `limitedParallelism` is explicitly YAGNI in migration blueprint §9
   and stays that way until a measured need exists.

### Step 5 — Sequential probing default

1. FACT: `MediaPipeline.loadPage` probes sequentially (plain `for` loop,
   `MediaPipeline.kt:64`). The Viewer keeps that default.
2. A page must render progressively (records appear as probed) rather
   than blocking on the whole page — the grid shows loading states per
   cell, not a full-screen spinner past first paint.
3. INFERENCE: sequential probing on a large DCIM folder will feel slow on
   first open. That is the accepted contract for v1 (migration blueprint
   §9); do not "fix" it inside this slice with concurrency. Record the
   measured first-page time on the Moto G smoke as evidence for a future
   decision, don't decide one here.

## 5. Acceptance criteria

- [x] Grid renders from a picked folder via `MediaPipeline.loadPage`; no
      direct SAF queries or decodes from UI code.
- [x] Detail shows all 11 photo fields / all 10 video fields from
      `PhotoMeta` / `VideoMeta`, with corrected dims as headline and raw
      values only as the badge.
- [x] Every missing field renders `—`; no field invents a value (grep
      gate passes: no mtime-as-EXIF substitution anywhere in the Viewer).
- [x] Every row carries a provenance chip matching the record's
      `Provenance` enum value.
- [x] GPS "Strip…" button never edits: disabled with a Phase 3 label
      (or placeholder stating not implemented) — no write path exists in
      this slice.
- [x] Empty / no-folder / all-unsupported / permission-lost / per-record
      failure states each render distinctly and are covered by tests.
- [x] Thumbs are 256px, orientation-applied, JPEG-85 cached under the
      existing FNV LRU (100 / 32 MB); no second cache, no disk cache.
- [x] Sequential probing default preserved; leaving the screen cancels
      in-flight work; completed records persist in cache.
- [ ] `flutter analyze`-equivalent native gates green (see §6); no new
      dependencies (see §7). Host Gradle gate PENDING at draft time.
- [ ] G2 checklist items in `blueprints/CHECKPOINTS.md` §G2 check off
      except the human Moto G smoke, which is recorded as device evidence
      (§8), not claimed from host runs. G2 stays OPEN until the
      instrumented Viewer tests + real-shot smoke pass.

## 6. Gates (must all pass before the slice is called done)

1. Host: `gradle :vision-app:testDebugUnitTest` (or the repo's pinned
   equivalent) green, including new Compose UI tests (see §7).
2. Analyzer/lint: the project's lint gate clean on all new files
   (ktlint per migration blueprint §12; "No issues found" discipline
   carried from the Flutter lane).
3. `tools/verify_reference.py` exit 0 — the reference tree is untouched
   (the Viewer adds files under `native/`; it must not touch `app/`,
   `packages/`, or the manifest).
4. No-new-dependency gate: `git diff` on the Gradle files shows no
   added artifact; Coil is NOT adopted in this slice (migration
   blueprint §8: revisit ONLY for crossfade/AsyncImage ergonomics at
   Viewer time — that revisit is §9 Q3, not a decision made here).
5. Manifest grep gate: no `INTERNET` permission added (migration blueprint
   §13); no new permission of any kind in this slice.
6. G2 gate in `blueprints/CHECKPOINTS.md` — all items except the device
   smoke, which needs §8 evidence.

## 7. Test plan

### Host (JVM / Robolectric / Compose UI tests)

- Compose UI tests from record fixtures (migration blueprint §10:
  "Compose UI tests assert grid/detail states from the same record
  fixtures"): build `PhotoMeta`/`VideoMeta` records from the committed
  synthetic fixtures (`tools/make_fixtures.py` outputs — `ve_exif.jpg`
  324B with orientation 6, `ve_plain.png`, `ve_minimal.mp4`, `ve_thumb.jpg`;
  FLUTTER_REFERENCE_BASELINE.md §9) and assert:
  - grid shows the record's name + kind + thumb state;
  - detail shows corrected dims (orientation-6 fixture: the
    committed `ve_thumb.jpg` is 24×16 raw and displays as 16×24
    corrected — the spec's 3000×4000 example was illustrative, not
    the committed fixture; tests pin the real values) with the
    raw-tag badge;
  - unknown fields render `—` (e.g. `ve_plain.png` has no EXIF);
  - provenance chips match the fixture's provenance;
  - GPS "Strip…" button is disabled / non-editing;
  - each empty/error state renders its distinct copy.
- ViewModel tests: page loading, cancellation on leaving the screen,
  permission-loss state, per-record failure isolation (one bad record
  does not fail the page).
- Cache tests: scrolling the same record twice hits the cache (no second
  decode); eviction re-decodes on demand.
- FACT: fixture coverage for the orientation rule already exists in
  `vision-core` (`DimensionsTest.kt`); the Viewer tests assert the UI
  *displays* the corrected values, they do not re-prove the swap rule.

### Device (Moto G — human/agent run, see §8)

- Instrumented Compose UI tests on the Moto G against staged synthetic
  fixtures (same bytes as host), plus the smoke below.
- No new device-only logic may appear without a host test proving the
  same branch first (migration blueprint §14 R-05 split).

## 8. Device evidence required (Moto G 2025)

The following must be pasted/recorded before G2 can close. Nothing here
may be claimed from a host run or an emulator.

1. MP6 device re-run (S1) green first — the Viewer builds on the
   pipeline; a red MP6 invalidates this slice's device evidence.
2. Viewer instrumented suite green on the Moto G (grid + detail +
   empty states over staged synthetic fixtures).
3. Real-shot smoke (G2 gate text): the operator opens the Viewer over
   real device-camera shots and pastes the observed result — corrected
   dims sane, `—` where expected, provenance chips accurate. FACT: the
   shots are user-supplied and never committed (ROADMAP.md Phase 2 gate;
   BP-02 verification; AGENTS.md rule 4: synthetic data only in
   code/tests/fixtures).
4. First-page load time on a real folder (recorded as a number, for the
   future concurrency decision — not a pass/fail in this slice).
5. Airplane-mode run: Viewer fully functional offline (BP-02 reference
   lineage: Recovery offline-first; migration blueprint §13 dynamic gate).

## 9. Risks

| ID | Risk | Likelihood / Impact | Mitigation in this slice |
|---|---|---|---|
| R-V1 | Thumb OOM on huge/odd files | MEDIUM / HIGH (migration blueprint §14 R-03) | 64 MB source cap + bounds-first decode already in `PhotoThumbs`; Viewer adds no full-res decode. Validate with hostile fixtures on device. |
| R-V2 | Orientation displayed wrong (K4 regression at the UI layer) | MEDIUM / HIGH | UI displays `PhotoMeta.width`/`height` getters only; UI tests pin the orientation-6 fixture's corrected dims. |
| R-V3 | Sequential probing feels slow on large folders | HIGH / LOW-MEDIUM | Accepted contract for v1; measure and record first-page time (§8 item 4) instead of adding concurrency here. |
| R-V4 | HEIF/DNG files show mostly `—` (K5 open) | MEDIUM / MEDIUM | Honest `—` is the correct v1 behavior; do NOT invent a fallback inside the Viewer. K5 fallback is a separate decision (§10 Q2). |
| R-V5 | Compose/Kotlin/AGP coupling breaks the build at slice time | MEDIUM / MEDIUM (migration blueprint §14 R-04) | Pinned BOM lane; no upgrades mid-slice. Fallback is Views per §4 (kept warm, not built). |
| R-V6 | Scope creep: "while you're here, add slideshow/share" | LOW / HIGH (migration blueprint §14 R-07) | Non-goals in §2 are explicit; any such request is a new slice with its own ADR, not a Viewer change. |

## 10. Open questions (for Glenn — do NOT decide in this PR)

- **Q1 — GPS "Strip…" button form:** disabled-with-label vs. a
  placeholder screen stating Phase 3 is not implemented? (RECOMMENDATION
  in §4 Step 3 is disabled-with-label; Glenn's call.)
  **Decided 2026-11-17 (Glenn's brief, implemented in PR #10):**
  disabled button with the label "Strip GPS — Phase 3"; no routing
  target, no write path.
- **Q2 — K5 HEIF/DNG fallback:** when a file's EXIF coverage differs
  between the Dart `exif` path and native `ExifInterface`, what is the
  v1 fallback — honest `—`, a second native probe, or a documented
  per-format quarantine note? (K5 is open in CURRENT_STATE.md; the Viewer
  must not invent an answer.)
  **Decided 2026-11-17 (Glenn's brief, implemented in PR #10):**
  honest `—`, no invented fallback — the spec's own R-V4 position.
  A second probe or quarantine note is a later decision, not this slice.
- **Q3 — Coil revisit trigger:** migration blueprint §8 says revisit Coil
  ONLY for crossfade/AsyncImage ergonomics at Viewer time. Does Glenn
  want that revisit now (with its key/contract trade-offs: default keys
  omit mtime, disk cache overlaps ours), or does the hand-rolled loader
  stay for v1?
  **Decided 2026-11-17 (Glenn's brief, implemented in PR #10):**
  Coil NOT adopted; the hand-rolled loader stays for v1. The revisit
  stays open as a later decision.
- **Q4 — Detail full-resolution image:** should the detail screen decode
  a larger image than the cached 256px thumb, or is the 256px thumb the
  detail image for v1? (A larger decode changes memory bounds — R-V1 —
  and must not be decided inside this slice's implementation.)
  **Decided 2026-11-17 (Glenn's brief, implemented in PR #10):**
  the cached 256px thumb is the detail image for v1; no larger decode.
- **Q5 — Page size for the grid:** the pipeline pages records; what page
  size does the Viewer request? (Manual paging is the default per ADR-005
  non-decisions; Paging 3 is a separate question for MP1 kickoff, not
  this slice.)
  **Decided 2026-11-17 (implemented in PR #10):** page size 50
  (`ViewerViewModel.PAGE_SIZE`), recorded in the PR body. No
  pre-existing page-size constant existed anywhere in the pipeline;
  device tests use 10. Paging 3 stays a separate question.
- **Q6 — Video detail beyond a still frame:** is a mute preview / scrub
  in scope for a later Viewer slice, or is the still frame the permanent
  v1 video detail? (Out of this slice either way; the answer shapes the
  detail screen's layout, not its v1 content.)
  **Still OPEN** — the still frame is the v1 video detail; no preview
  or scrub in this slice.

## 11. Implementation record (PR #10, draft 2026-11-17)

Branch `feat/mp7-native-viewer`, base main `9c3a2aa`. Draft PR,
not merged. Host Gradle gate + device evidence PENDING.

### The URI gap, closed

FACT: `SafDiscovery` produces `MediaSource` instances with
`documentUri` only, and `MediaPipeline.recordFor` /
`thumbnailFor` handled only `filePath` — a URI-only source came
back unreadable / "no local path". The URI read paths already
existed below the pipeline (`AndroidMetadataExtractor.probePhotoUri`
/ `probeVideoUri`, `VideoFrames.grabFrameDescriptor`), so the fix
is wiring only:

- `MediaPipeline` stores the constructor `ContentResolver`
  (`private val resolver`) and implements the new `ViewerPipeline`
  interface (`loadPage` + `thumbnailFor`), so the ViewModel can
  inject a fake pipeline in tests.
- `recordFor`: `filePath` present → existing `probePhotoFile` /
  `probeVideoFile`, unchanged; `filePath` absent and `documentUri`
  present → `probePhotoUri` / `probeVideoUri`; both absent →
  unreadable with the pre-existing "no local path" warning.
- `thumbnailFor`: file path → existing `fetchPhotoThumb` /
  `fetchVideoThumb`, unchanged; URI photo → `fetchPhotoThumbUri`
  (bounded FD byte read, then the shared `decodePhotoThumb`
  helper); URI video → `fetchVideoThumbUri`
  (`grabFrameDescriptor` on the opened FD).
- Descriptor lifecycle: every descriptor the pipeline opens is
  closed in a `finally` on all paths — success, decode failure,
  over-cap, and exception. The extractor's own `probePhotoUri`
  (`use {}`) and `probeVideoUri` (`finally`) already closed theirs.
- Bounds preserved: photo source cap 64 MB (`maxSourceBytes`,
  enforced by a bounded chunked read that aborts past the cap
  instead of reading the whole file first), video `maxSide` 512,
  cache 100 entries / 32 MB, detail reuses the cached 256px thumb.
  No second cache, no disk cache, no full-resolution decode.
- Never copies user libraries into app-private storage. Never
  constructs raw filesystem paths from SAF document IDs.

### What was built

- `native/vision-core/.../DetailFields.kt` — pure detail-field
  table: `DetailField(label, value, provenance)`,
  `photoDetailFields` (11, spec order) / `videoDetailFields`
  (10, spec order), `formatDurationMs` (<1000 ms → "N ms", else
  seconds one decimal trimmed), `formatFileSize` (B / KB / MB).
  Corrected dims from `PhotoMeta.width`/`height` /
  `VideoMeta.width`/`height` getters only; unknown → "—"
  (U+2014). No mtime-as-EXIF anywhere (grep-gated).
- `native/vision-app/.../viewer/ViewerViewModel.kt` — mirrors
  `RenamerViewModel`: injectable `pipelineFactory` /
  `discoveryFactory`, `StateFlow`s for grant / grantLost /
  loading / records / counts / hasMore / error / selected
  record; `openFolder` (discovery on IO, counts, first page),
  `loadMore` (page size 50), `selectRecord` / `clearSelection`,
  `onFoldersChanged` (lost grant keeps loaded records visible,
  state preserved; missing grant resets). Cancellation: the
  owning coroutine scope cancels in-flight work; a generation
  counter makes the latest open win and guarantees `_loading`
  returns to false via `finally` even when a load is cancelled
  mid-loop. No `viewModelScope` — `lifecycle-viewmodel-ktx` is
  not a dependency (same constraint as S3); the ViewModel is
  driven by suspend functions from the UI scope.
- `native/vision-app/.../viewer/ViewerScreen.kt` — `ViewerScreen`
  (folder list with Select, counts line, `LazyVerticalGrid`
  3-column in pipeline folded-name order, per-cell loading /
  failure placeholders, "Load more", error + Dismiss),
  `ViewerCell` (cached thumb or placeholder, name, kind badge,
  "Could not read this file" for unreadable records),
  `ViewerDetail` (cached thumb full-width, name, field table
  with one provenance chip per row, "Strip GPS — Phase 3"
  button DISABLED).
- `AppRoot.kt` — third tab "Viewer" (`tab == 2`), optional
  `viewerViewModel` injection param with default factory (same
  seam as `renamerViewModel`). This is the S5 extension the S4
  spec anticipated ("S5 extends it"); no S3/S4 logic changed.

### Tests (41 new; 1 renamed)

- `DetailFieldsTest.kt` (vision-core, JVM) — 11 tests: 11/10
  fields in spec order, corrected dims (24×16 raw → 16×24),
  rotation-corrected video dims, unknown + provenance,
  duration/size format vectors, raw-tag badge vectors (photo
  "Raw 24×16 · orientation tag 6", video
  "Raw 1920×1080 · rotation tag 90", null when the tag is
  absent).
- `UriPipelineTest.kt` (vision-android, Robolectric) — 10 tests
  with a fake ContentProvider serving staged fixtures:
  URI photo probe (orientation 6, 16×24), URI photo thumb
  decode + cache hit, URI video probe, URI video thumb
  unavailable on the trackless fixture, descriptor closure on
  decode failure, inaccessible source stays unreadable,
  over-cap thumb stays unavailable, file-path regression,
  `loadPage` invokes the per-record callback in source order,
  a second `loadPage` on the same pipeline instance hits the
  first page's thumbnail cache (a fresh instance does not).
  Descriptors are asserted closed (`fileDescriptor.valid ==
  false`) on every success and failure path.
- `ViewerViewModelTest.kt` (vision-app, Robolectric) — 20 tests
  with a fake pipeline + scripted discovery: healthy open with
  counts, lost grant blocks load, empty folder, discovery
  failure, discovery `SecurityException` → grant lost,
  cancellation stops without thumbs, second open supersedes
  first, select/clear, folders-changed lost (records kept) and
  missing (reset), first record published before the page
  finishes (gated pipeline), one pipeline instance across
  pages (factory called once for a 55-record two-page load),
  bitmap retention bound (15 records → 12 retained, oldest
  evicted, evicted record restores on demand),
  tab-hide cancels in-flight work (records stay, thumbs
  pending), folder changes leave the calling scope active
  (lost and missing cases) and cancel an in-flight load with
  records preserved (3 ownership regressions), plus 3
  cancellation-propagation regressions (latch-gated
  cancellation during discovery, page loading, and thumbnail
  fetching: no error set, loading false, calling scope
  survives, records pending where the load never completed
  them).
- `ViewerScreenTest.kt` (vision-app, Robolectric + Compose) — 6
  tests with the real pipeline + fake provider: empty state,
  lost-grant state, grid + photo detail (corrected dims, five
  "—", provenance counts 4 exif / 2 filesystem, Make "THUMB",
  Strip button displayed + disabled, Back returns to grid),
  video detail opens (rotation-tag badge asserted), empty folder,
  all-unsupported cells + counts.
- `UriPipelineDeviceTest.kt` + `TestDocsProvider` (instrumented)
  — 4 tests: URI photo probe + thumb on device, URI video probe
  + thumb unavailable on device, corrupt photo partial on
  device (probe `partial`, thumb `failed`, bytes null), sources
  untouched. The test provider is declared in a
  new `src/androidTest/AndroidManifest.xml` (`exported="true"`,
  test APK only, never ships).
- `AppRootTest.kt` — 1 new test: the Viewer tab renders the
  viewer empty state.
- `PipelineTest.kt` — 1 test RENAMED
  (`uriOnlySourceStaysUnreadableWithoutLocalPath` →
  `uriSourceWithNoProviderStaysUnreadable`); same assertions —
  with no provider registered the URI source still stays
  unreadable. That is the inaccessible-file case, not the old
  gap behavior.
- New test resource: `ve_thumb.jpg` copied byte-identical into
  `native/vision-app/src/test/resources/fixtures/` so the
  vision-app unit tests can load it (vision-android test
  resources are not on the vision-app test classpath).

### Verification on this VM (host gate is the arbiter)

- vision-core JUnitCore: OK (104 tests) — 93 pre-existing + 11
  new (6 original + 5 raw-tag badge).
- pytest: 30 passed. flake8 hard-select clean (no `.py` touched
  by this amendment).
- Standalone kotlinc checks over all five Android source sets:
  the first check claimed no real errors and MISSED a real
  one — `ViewerViewModel.kt` called `Job.ensureActive()` without
  `import kotlinx.coroutines.ensureActive`. The independent audit
  caught it (P0); the amendment adds the import and proves it
  here two ways: a minimal file importing
  `kotlinx.coroutines.ensureActive` and calling it on a `Job`
  compiles clean, and a full kotlinc pass over all changed files
  reports zero `kotlinx` / `ensureActive` errors. The remaining
  errors in that pass are missing-Android-SDK cascade noise
  (unresolved `android.*` / `androidx.*` / Robolectric symbols
  and their knock-ons), at the same level as the pre-existing
  files; the one candidate that looked real
  (`ViewerScreen.kt:161`, suspend call inside `LaunchedEffect`)
  is cascade from the unresolved Compose symbol, proven by a
  minimal pattern file that compiles clean. The host Gradle gate
  remains the arbiter for the Android sources.
- No new dependencies: `git diff` on all three
  `build.gradle.kts` files is empty. No `INTERNET` permission;
  both main manifests `git diff` empty. The only new manifest
  is the androidTest one (test provider).
- Genesis headers: exactly 4 `//` lines on all 10 new `.kt`
  files. No TODOs. No mojibake. No mtime-as-EXIF.
  `tools/verify_reference.py` still fails identically (28
  files, same list) on this branch and on clean main `9c3a2aa`
  — pre-existing, not this PR's scope.

### Host-gate-only (not run here)

- All Robolectric + Compose tests above (UriPipelineTest 10,
  ViewerViewModelTest 20, ViewerScreenTest 6, AppRootTest 1
  new + 1 existing, PipelineTest 7 incl. the renamed test),
  the vision-android unit suite, and `lintDebug`.
- The instrumented `UriPipelineDeviceTest` (4 tests) — runs on
  the B160V via `S5-DEVICE-RUNBOOK.md`.

### Audit fixes (amendment, 2026-11-17 — independent audit, CHANGES REQUESTED)

- **P0 compilation:** `ViewerViewModel.kt` called
  `currentCoroutineContext()[Job]?.ensureActive()` at four sites
  without `import kotlinx.coroutines.ensureActive`
  (`ensureActive` is a kotlinx-coroutines extension on `Job`).
  Import added; no other coroutine import was missing (audit
  of the file: `Dispatchers`, `Job`,
  `currentCoroutineContext`, `withContext`, `StateFlow` /
  `MutableStateFlow` all present). Proven on this VM as
  described in "Verification on this VM" above.
- **P1.1 progressive publishing (spec §4 Step 5.2):**
  `ViewerPipeline.loadPage` / `MediaPipeline.loadPage` gained
  an optional per-record callback
  (`onRecord: ((MediaRecord) -> Unit)? = null`), invoked inside
  the existing sequential loop right after each probe. The
  ViewModel appends each record to `_records` as it arrives, so
  the first record is visible without waiting for the other 49.
  Existing 3-argument call sites compile unchanged (default
  parameter); every `ViewerPipeline` implementation must invoke
  the callback for each record it returns. A local `published`
  counter (not a shared increment) keeps `loadedCount` exact
  under mid-page cancellation; the callback skips appends when
  its generation is stale (`loadGeneration` is `@Volatile` so
  the check sees a superseding open from the IO thread).
- **P1.2 shared pipeline:** the ViewModel retains ONE
  `ViewerPipeline` per folder open (`pipeline =
  pipelineFactory(resolver)` in `openFolder`, reused by every
  page, cleared in `resetRunState`), so `MediaPipeline`'s
  `ThumbnailCache` survives paging. The injectable factory /
  `ViewerPipeline` seam is unchanged.
- **P1.3 bound retained Bitmaps:** decoded `Bitmap`s retained
  in `_records` are capped at `MAX_RETAINED_BITMAPS = 12`
  (3-column grid, roughly one screenful; a 256×384 thumb is
  ~393 KB, so 12 is ~4.7 MB worst case). On attach,
  `enforceBitmapCap` evicts the oldest retained Bitmaps other
  than the just-attached record and the selected record; an
  evicted record keeps `ThumbStatus.ready` with a null Bitmap
  (its encoded bytes stay in the FNV cache) and re-decodes on
  demand via `restoreThumb` (cache hit, guarded by an
  in-flight set), triggered by the grid cell when it composes
  a ready-but-bitmapless record after loading finishes. An
  evicted record never blanks: the cell shows its loading
  placeholder until the restore lands.
- **P1.4 tab-leave cancellation:** `ViewerViewModel.onViewerHidden()`
  cancels the in-flight load job, bumps the generation, and
  resets `_loading`; completed records persist. `AppRoot` calls
  it from a `selectTab` helper when the tab switches away from
  Viewer. This is the ONLY AppRoot change beyond the tab switch
  itself and the reviewed S3/S4 integration correction for this
  amendment. No S3/S4 logic changed.
- **P1.5 regression tests:** progressive first-record
  visibility (gated pipeline), shared cache (factory called
  once across a 55-record two-page load; second `loadPage` on
  one pipeline instance hits the first page's cache), bitmap
  retention bound (15 records → 12 retained, oldest evicted,
  evicted record restores), tab-hide cancellation (in-flight
  work stops, records stay, thumbs pending). All host-gate-only
  except the core badge tests.
- **P2 raw-tag badge (BP-02 / S5):** `DetailFields.kt` gains
  `photoOrientationBadge` / `videoRotationBadge` ("Raw 24×16 ·
  orientation tag 6", "Raw 1920×1080 · rotation tag 90",
  "Raw — · … tag N" when raw dims are missing, null when the
  tag itself is absent). `ViewerDetail` renders the badge under
  the field table. Headline dims stay the corrected getters
  only.
- **Instrumented test scrutiny:** `UriPipelineDeviceTest`
  stages fixtures via the instrumentation (test) context's
  private dir (`InstrumentationRegistry.getInstrumentation()
  .context.getDir(...)`) instead of the target context's
  app-private dir, so the provider's file read no longer
  depends on the test process sharing the app UID.
  `TestDocsProvider` has no `android:process`, so it runs in
  the test APK's default process — the SAME process as the
  instrumented test code — and the static
  `TestDocsProvider.root` sharing is valid by construction, not
  cross-process. The cross-process hop is the
  `ParcelFileDescriptor` over Binder to the app process, the
  standard ContentProvider mechanism. The provider still fails
  loud (`FileNotFoundException` "no root") if the root is
  unset. The 4 instrumented cases validate URI processing on
  device; they do NOT replace the real picked-folder smoke
  (runbook keeps both).

### Deviations from this spec

1. §7's orientation-6 example ("3000×4000 raw displays
   4000×3000 corrected") does not match the committed fixture.
   Tests pin the real values: `ve_thumb.jpg` 24×16 raw → 16×24
   corrected. §7 corrected in place.
2. §6 gate 3 (`tools/verify_reference.py` exit 0) cannot pass
   on this branch: the script fails identically (28 files, same
   list) on clean main `9c3a2aa` — the files PRs #1–#9 changed
   (Python + blueprints + root files) differ from the frozen
   reference manifest. Pre-existing on main, not caused by this
   PR; the cross-slice gate in `standalone-v1/CHECKLIST.md`
   stays unchecked. No Flutter file was touched by this PR.
3. The video URI metadata assertions in `UriPipelineTest`
   mirror the file-path test's unscripted-retriever values
   (status ok, rotation 0) — whether
   `ShadowMediaMetadataRetriever` returns the same defaults
   over a `FileDescriptor` source is host-gate-verified, not
   proven here.
4. Discovery swallows query exceptions: `SafDiscovery.discover`
   catches them into a warnings-only outcome ("query failed"),
   so a revoked grant during discovery surfaces as a
   "Discovery failed" error, not as grant-lost. Grant-lost is
   driven by S3's persisted-flags state (`FolderState.LOST`),
   which the Viewer honors. The ViewModel's `SecurityException`
   catch covers factories that throw directly. Recorded as a
   limitation, not fixed here (changing `SafDiscovery`'s
   contract is S3's, not S5's).
5. The all-unsupported empty message in the spec (§4 Step 2)
   is not a separate screen state: unsupported files produce
   records, so they render as grid cells with "Unsupported"
   badges + the counts line ("Unsupported: N") instead of a
   dedicated message. The "No photos or videos here" text
   covers only the truly empty folder.
6. Page size 50 is the Viewer's own choice
   (`ViewerViewModel.PAGE_SIZE`); no pre-existing page-size
   constant existed anywhere (device tests use 10). Recorded
   here and in the PR body (S5 Q5).
7. `ViewerPipeline` is a new testability seam: `MediaPipeline`
   is a final class and cannot be subclassed, so the interface
   exists so ViewModel tests can inject a fake slow pipeline
   for deterministic cancellation tests.
8. The ViewModel decodes thumb JPEG bytes to `Bitmap` on IO
   (`BitmapFactory.decodeByteArray`) and holds them as
   transient per-page presentation state, now bounded at
   `MAX_RETAINED_BITMAPS = 12` with oldest-first eviction
   (selected + just-attached records exempt) and restore on
   demand via `restoreThumb` — see "Audit fixes" above. The
   FNV-keyed `ThumbnailCache` remains the only cache.
9. The instrumented test provider is `exported="true"` in the
   androidTest manifest: the provider runs in the test APK's
   default process, the same process as the instrumented test
   code, so the static `TestDocsProvider.root` sharing is valid
   by construction; the `ParcelFileDescriptor` hop over Binder
   to the app process is the standard ContentProvider
   mechanism. Test APK only, never ships.
10. Descriptor-opener seam (host-gate attempt 3, P0 preflight,
    2026-11-17): `MediaPipeline` and
    `AndroidMetadataExtractor` take a defaulted constructor
    parameter `openDescriptor: (Uri) ->
    ParcelFileDescriptor? = { uri ->
    resolver.openFileDescriptor(uri, "r") }`, and all four
    descriptor-open sites call it instead of the resolver
    directly. Justification: reliable observation of
    caller-owned descriptor closure is not possible test-only.
    A `ContentResolver` subclass cannot intercept the opens —
    `openFileDescriptor(Uri, String)` is `public final` in
    AOSP `ContentResolver.java` (L3133; the
    `CancellationSignal` overload is final at L3226), and
    `ContentResolver` has no public `getContext()` — so the
    first attempt at a tracking resolver subclass would not
    compile. The resolver also returns a different
    `ParcelFileDescriptor` wrapper than the provider's
    instance (canary-proven), so provider-side validity is not
    a closure signal under Robolectric's Parcel transport, and
    Robolectric exposes no public API to track the descriptors
    a pipeline receives. With the default, production behavior
    is unchanged (same resolver call, same mode, same sites);
    tests inject a tracking lambda that records every
    descriptor the pipeline actually receives and delegates to
    the real resolver, and `allDescriptorsClosed()` checks
    those caller-owned descriptors.
11. Stream-based URI photo probe (host-gate attempt 5,
    2026-11-17): `probePhotoUri` constructs `ExifInterface`
    from an `InputStream` over the descriptor
    (`FileInputStream(descriptor.fileDescriptor).use {
    ExifInterface(it) }`) instead of from the
    `FileDescriptor` directly — the four-path diagnostic
    proved `ExifInterface(FileDescriptor)` throws
    `NullPointerException` under Robolectric on both ordinary
    and ContentProvider descriptors, while the path and
    input-stream paths read orientation 6 / 24×16. The seam
    and descriptor lifecycle are unchanged; the file-path
    probe is untouched. Corrupt-but-present files probe as
    `partial`, not `unreadable`, through either constructor:
    androidx ExifInterface 1.4.1 `loadAttributes` swallows
    format errors and `addDefaultValuesForCompatibility`
    injects 0-valued width/length/orientation tags, which
    `ExifReader` maps to unknown (orientation 0
    illegitimate, dims ≤ 0 illegitimate), so `readPhoto`
    yields `partial`; the `unreadable` expectation was true
    only under Robolectric's broken FD constructor.
    Inaccessible sources (no descriptor, provider throws)
    still probe `unreadable`, and a corrupt file's thumbnail
    still fails. The same correction is applied to the
    never-run device test (`uriCorruptPhotoPartialOnDevice`).
    Corrupt-image rejection was first attempted under native
    graphics (`CorruptImageNativeGraphicsTest`,
    class-level `@GraphicsMode(NATIVE)` with a good-fixture
    control); host-gate attempt 6 (2026-11-17) showed native
    graphics still manufactures a 256×256 bitmap from the 6
    garbage bytes, so that approach is superseded by item 12
    below — the legacy shadow decodes garbage to 256×256 only
    while invalid-image acceptance stays at its default.
12. Strict legacy BitmapFactory decoding (host-gate attempt 6,
    2026-11-17): corrupt-image rejection is verified under
    `@GraphicsMode(LEGACY)` with
    `ShadowBitmapFactory.setAllowInvalidImageData(false)`.
    Verified against Robolectric 4.17: `ShadowBitmapFactory`
    is the legacy shadow only — its picker selects it for
    LEGACY and `ShadowNativeBitmapFactory` for NATIVE, and
    the switch exists only on the legacy shadow, so it cannot
    affect native mode. With the switch off, `decodeByteArray`
    returns null and sets `outWidth`/`outHeight` to -1 for
    undecodable data, which `PhotoThumbs` already maps to a
    null thumbnail. The switch is set in `@Before` and
    restored to `true` in `@After` in both
    `CorruptImageStrictLegacyGraphicsTest` (renamed from the
    native-graphics class) and `UriPipelineTest`
    (class-level LEGACY; `uriDescriptorClosesOnDecodeFailure`
    moves from NATIVE to LEGACY and keeps its
    `ThumbStatus.failed` assertion); Robolectric's `@Resetter`
    also restores the default after each test. The good
    fixture still decodes to 256×384 in both classes, so the
    strict mode is a control, not a weakened bar. The device
    test `uriCorruptPhotoPartialOnDevice` now also asserts the
    corrupt thumbnail bytes are null, not only the failed
    status and reason. Test-only; no production decoding
    change and no file-signature restriction.
13. App-layer fixes (host-gate attempt 7, 2026-11-17):
    (a) Fixture: `ve_minimal.mp4` was missing from
    `vision-app/src/test/resources/fixtures` (only
    `ve_thumb.jpg` was there), so `ViewerScreenTest` setUp
    threw `IllegalStateException("missing fixture")` and all
    six screen tests failed before running. The 150-byte
    fixture is copied byte-identical from the vision-android
    test resources (md5
    `19b91b1099a564c87d30fc868a2009f7`); the screen test's fake
    provider gets the same `ProviderInfo` / `attachInfo`
    initialization as the corrected `UriPipelineTest`, so
    `openFileDescriptor` reaches its `openFile`.
    (b) Load-Job ownership: `openFolder` / `loadMore` stored
    the caller's `Job` as `loadJob`, so `onFoldersChanged`
    cancelling it cancelled the caller's scope (in tests, the
    `runBlocking` parent). `loadJob` now holds the `Job` of a
    `coroutineScope` owned by the load invocation, a child of
    the caller; cancelling it stops only the Viewer load. The
    load body moves to `openFolderLocked` unchanged apart
    from that split; `onFoldersChanged`, `onViewerHidden`
    and `onCleared` are unchanged and now cancel only the
    owned `Job`. Three regression tests pin it: folder
    changes after a completed load leave the calling scope
    active, and a folder change during a slow load stops the
    load (records published, thumbs pending) with records
    preserved. (c) Screen-test corrections found while
    verifying the tests can pass past setup: strict legacy
    decoding in `ViewerScreenTest` (attempt-6 mechanism) so
    the corrupt thumbnail fails; a fifth grid row `gone.jpg`
    with no provider file, whose probe is genuinely
    `unreadable`, so the 'Could not read this file' marker
    renders (bad.jpg probes `partial`, so no four-row record
    could show it); `onNodeWithText("Photo")` requires
    exactly one matching node per the Compose source, so the
    kind-badge assertion becomes
    `onAllNodesWithText("Photo").assertCountEquals(3)`.
    Known adjacent wart, not fixed here: the generic
    `catch (e: Exception)` blocks in the discovery/load
    paths still swallow `CancellationException` (the work
    still stops via the generation guards plus
    `ensureActive` checkpoints, but a load cancelled during
    discovery can surface as an error instead of a clean
    cancellation). Pre-device acceptance requirement
    (attempt 8, 2026-11-17): the handlers must explicitly
    rethrow `CancellationException` before the generic
    catch, with regression coverage proving a load
    cancelled during discovery stops cleanly (no error
    set, loading false) while the calling scope survives.
    Required before B160V device acceptance; not part of
    the scrolling fix.
14. Detail-screen scrolling (host-gate attempt 8,
    2026-11-17): `ViewerDetail` is a vertically scrollable
    `Column` with the thumbnail before the metadata fields
    and the raw-tag badge, so the field and badge nodes sit
    below the viewport. The screen tests scroll each detail
    visibility assertion into view with `performScrollTo()`
    before `assertIsDisplayed()` (including the disabled
    Strip GPS control), and scroll Back into view before
    clicking it or asserting it is displayed. Test-only;
    production layout unchanged. The count assertions
    (`onAllNodesWithText`, `assertCountEquals`) need no
    scrolling: they count nodes in the tree, not displayed
    nodes.
15. Discovery-row file size (host-gate attempt 9,
    2026-11-17): the shared `safDocRow()` test helper sets
    `COLUMN_SIZE` to null, and `folderRows()` used it for
    `IMG_b.jpg` without an override, so the detail rendered
    the unknown marker for File size instead of "194 B".
    The `IMG_b.jpg` row only overrides `COLUMN_SIZE` with
    the staged fixture's actual length (194 bytes). No
    production change; the shared helper is untouched. With
    the size known, the existing assertions hold as
    written: "194 B", six unknown values, three exif and
    two filesystem provenance nodes.
16. Fixture Make tag (host-gate attempt 10, 2026-11-18):
    `tools/make_fixtures.py` `build_thumb_jpeg()` writes
    Make = `b"THUMB\x00"` into EXIF tag 0x010F, so the
    thumbnail fixture has four known EXIF fields (Make,
    Orientation, Width, Height), not three. The screen
    test counts five unknown values, four exif and two
    filesystem provenance nodes, and asserts Make /
    "THUMB" positively. AndroidX ExifInterface strips the
    trailing NUL (proven by the existing ExifReaderTest,
    which asserts the ve_exif.jpg Make `b"SYNTHETIC\x00"`
    reads exactly as "SYNTHETIC"). Correct production
    behavior; test-only correction, no fixture, generator,
    or JPEG change.
17. Cancellation propagation (attempt 11, pre-device
    hardening, 2026-11-18): the three generic
    `catch (e: Exception)` handlers in `ViewerViewModel`
    (`openFolderLocked` discovery, `loadNextPageLocked`
    page fetch, `loadNextPageLocked` thumbnail fetch) now
    explicitly rethrow `CancellationException` before the
    generic catch. The discovery handler clears `_loading`
    when the generation still matches, so a load cancelled
    during discovery stops with loading false and no false
    read-error; the page and thumbnail handlers rely on
    `loadNextPage`'s generation-guarded `finally` for the
    loading flag, and a thumbnail cancelled mid-fetch is
    never marked failed. The `SecurityException` handling
    and ordinary failure behavior are unchanged. Three
    deterministic regression tests (latch-gated discovery,
    page, and thumbnail cancellation) pin it: no error set,
    loading false, calling scope survives, records pending
    where the load never completed them. `ViewerViewModelTest`
    grows 17 → 20 tests; total new tests 50 → 53.

### Decided questions (see §10)

- Q1: disabled "Strip GPS — Phase 3" button (2026-11-17).
- Q2: K5 HEIF/DNG → honest "—", no fallback (2026-11-17).
- Q3: Coil NOT adopted (2026-11-17).
- Q4: detail reuses the cached 256px thumb, no larger decode
  (2026-11-17).
- Q5: page size 50 (2026-11-17).
- Q6: still OPEN — video beyond a still frame is a later slice.
