# NATIVE_ANDROID_MIGRATION_BLUEPRINT.md — Flutter → pure native Kotlin

> Status: BLUEPRINT for operator review. Authorizes NOTHING to build.
> GQ8 = NO (Android-only, forever). Flutter `main` is the frozen
> reference; Kotlin is the target; the Viewer will be native.
> No iOS work, no Flutter continuation beyond reference duty.
> Research date: 2026-09-15. Baseline: `4a94f25`, 135 tracked files.

## 0. Reading guide (fact discipline)

Labels used throughout: FACT (verified in repo/docs), REPO EVIDENCE
(file:line or gate result), RECOMMENDATION (judgment), INFERENCE
(strong but unverified), OPEN QUESTION (needs a decision or a device).
Assumptions are never presented as facts. The 2C device rerun is still
outstanding at the time of writing — nothing below claims 2C
device-validated; host gates are the cited baseline.

## 1. Forensic inventory (baseline `4a94f25`)

A. Pure/domain (zero Android/Flutter imports) — KEEP AS CONTRACT, PORT:
`packages/rename_core` (prefix/strip/buildPlan/collisions/manifest/undo;
22 tests + 6 goldens; `tools/parity_probe.py` proves Python parity),
`packages/metadata_core` (Provenance/MetaField/PhotoMeta/VideoMeta/
corrected dims/fixture codec/ExtractStatus; 19–20 tests + 3 goldens),
`packages/media_library` (MediaSource/Record/Kind, ThumbInfo, folded
ordering + dedupe, FNV keys, LRU cache, paging; 17 tests).
B. Flutter/Dart infrastructure — DELETE AS FLUTTER-ONLY at retirement:
`app/lib/main.dart`, 3 tab screens, `SettingsStore`, `flutter_test`
suites (44 host tests), `integration_test/` ×2 suites, golden-runner
harness shape (ported, not kept).
C. Android/Kotlin infrastructure — PORT (already native, keep files as
migration seed): `MetadataBridge.kt` (Exif probe, retriever probe,
video-frame grab), `MainActivity.kt` channel wiring (dissolved — no
channels in the target), `build.gradle.kts` pins (minSdk 26,
compileSdk 37, 64-bit ABIs, exifinterface 1.4.1).
D. Channel boundary — DELETE: `vision_engine/metadata`
(probePhoto/probeVideo/getVideoFrame). Direct calls replace it.
E. Tests — PORT (matrix §11): 44 host + 2 integration suites + golden
JSON (9 files, reused byte-identical).
F. Fixtures — KEEP AS CONTRACT: `tools/make_fixtures.py` (pure stdlib)
+ 4 binary fixtures (305B–454B) + golden JSON; generator stays Python,
outputs move with the port.
G. Docs/ADRs — KEEP: RULES/AGENTS/blueprints/BP-*/ADR-001..004/K-registry
(port the laws, not the files; K9–K19 re-encoded as native test/behavior
proofs).
H. Toolchain — KEEP PINNED, DO NOT UPGRADE FOR MIGRATION: Flutter 3.47
stays for reference duty; native side starts at AGP 9.1.0 + Gradle 9.3.1
+ JVM 17 + compileSdk 37 + minSdk 26 (proven D2/2B device build).
I. Permissions — KEEP AS CONTRACT: READ_MEDIA_IMAGES/VIDEO + legacy
capped at 32; no MANAGE_EXTERNAL_STORAGE; no broad access.
J. SAF behavior — PORT + EXTEND: folder pick exists (file_picker lane);
native adds persisted URI grants + direct DocumentsContract enumeration
(top-level only, same boundary).
K. Filesystem behavior — PORT: raw-path listing/stat with guarded
fallbacks; unstatable → nulls, never crash.
L. Metadata extraction — PORT EXACTLY (§8): 11 photo + 10 video fields,
provenance table, zero/unknown rules, exposure canonical form.
M. Thumbnail generation — PORT SEMANTICS, REPLACE MECHANISM (§9):
256px target, orientation-applied, small-source upscale, 64 MB cap,
≤512px JPEG-85 video frames, graceful unavailable.
N. Caching — PORT EXACTLY: FNV(source id, size, mtime, dim), LRU
100 entries / 32 MB, in-flight dedupe, failure never destroys records.
O. Paging/discovery — PORT: folded-name ordering, id dedupe first-wins,
page slicing, sequential probing default.
P. UI responsibilities — REPLACE: Rename preview/execute/undo, Probe
screens, Settings persistence, future Viewer grid/detail. All thin
today; the Viewer is unwritten in both stacks (why now is cheapest).

## 2. Responsibility matrix (abridged — full table lives in ADR-005 §A)

| Component | Now | Destination | Contract preserved | Test port |
|---|---|---|---|---|
| rename plan/collisions/manifest | rename_core (Dart) | `vision-core` rename package (JVM) | 6 goldens replayed byte-identical + parity_probe untouched | JUnit golden runner |
| PhotoMeta/VideoMeta/corrected dims | metadata_core | `vision-core` models | 3 goldens + orientation/rotation vectors | JUnit |
| Provenance/ExtractStatus/warnings | metadata_core + adapter | `vision-core` (same names) | unknown-rules, size-excluded-from-ok | JUnit |
| EXIF/video extraction | MetadataBridge | `vision-android` extractor (same APIs + FD-backed inputs) | 11+10 fields, ISO-6709, exposure form | Robolectric-scripted retriever + real-fixture Exif JVM tests + device |
| Channel (3 methods) | MainActivity | DELETED (direct calls) | payload shape dies with it; models do not | — |
| MIME table/kind mapping | MetadataAdapter | `vision-android` table (same entries) | case-insensitive, unknown→unsupported, no channel call | JUnit |
| Discovery/sort/dedupe/page | media_library + lister | `vision-core` ordering + `vision-android` enumerator | folded key, id dedupe, top-level only | JUnit + device |
| Thumb decode/cache/policy | ThumbStore + media_library | ImageDecoder/BitmapFactory + ported LRU/FNV | 256px, orientation, upscale-small, 64 MB cap, 100/32 MB | JVM policy + device vectors |
| Video frames | getVideoFrame | retriever port (same contract) | first frame ≤512px JPEG-85, unavailable grace | device |
| Rename/Probe/Settings UI | Flutter tabs | Compose screens | behavior, not pixels | Compose UI tests |
| Viewer | absent | Compose grid/detail (2D-native) | G2 contract | device + real shots |

## 3. Native architecture (proposed)

```
vision-app (Compose UI: Viewer grid/detail, Rename, Probe, Settings)
   ↓ observes StateFlow, dispatches intents
vision-android (SAF tree access, ExifInterface, retriever, frame grab,
   ImageDecoder/BitmapFactory thumbs, DataStore prefs, bounded LRU)
   ↓ pure calls, zero Android types upward
vision-core (pure JVM: metadata/media models, provenance, ordering,
   FNV keys, cache policy, rename plan — NO android.* imports)
```

## 4. Compose vs Views — engineering comparison for THIS app

Grid UX: Compose `LazyVerticalGrid` + per-item async thumb loading
matches the record model directly; Views needs RecyclerView +
ViewHolder + adapter diffing boilerplate for the same screen. Memory:
both decode natively; Compose's lazy layout releases off-screen items
comparably to RecyclerView recycling. Lifecycle: Compose collects
`StateFlow` with lifecycle awareness in one line; Views needs manual
observer management. Testing: Compose UI tests assert grid/detail
states from the same record fixtures; Views needs Espresso + idling
machinery. Accessibility: both fine via semantics/content-description.
Toolchain risk: Compose BOM + Kotlin coupling is strict but pinned and
well-trodden; Views avoids it at the cost of 2–3× UI code. Native API
integration (SAF intents, permission launchers): both fine, Compose
mildly cleaner (`rememberLauncherForActivityResult`).
RECOMMENDATION: Jetpack Compose (BOM-pinned at migration time, not now).
Views retained only as fallback if the pinned Compose/Kotlin pair ever
conflicts with the frozen AGP 9.1.0 lane — recurse to §16 risk R-06.

## 5. Domain contract (Kotlin shape, behavior-equivalent)

`vision-core` is pure JVM (no `android.*`): `Provenance` enum (same 4),
`MetaField<T>(value, provenance)` + `isKnown`, `PhotoMeta`/`VideoMeta`
(same 11/10 fields), corrected-dimension getters (swap rules verbatim:
orientation 5–8, rotation 90/270), `ExtractStatus` (same 4),
`MediaSource` (id + filePath/documentUri identity rule),
`MediaRecord` factories, `ThumbInfo` states, `orderingKey`
(folded/name/id), FNV-1a-64 key function (SAME canonical string so keys
are comparable across stacks during parity runs), LRU policy constants
(100 / 32 MB / 256px / 64 MB). Representation changes allowed ONLY with
a line in the port notes proving contract-equivalence (e.g., Dart
`int?` ↔ Kotlin `Long?` for sizes/durations — widen, never narrow).

## 6. Storage/SAF model (native)

ACTION_OPEN_DOCUMENT_TREE → `takePersistableUriPermission` (READ only;
no write grant — the app is read-only) → `getTreeDocumentId` →
`buildChildDocumentsUriUsingTree` + ONE projection query
(DOCUMENT_ID, DISPLAY_NAME, MIME_TYPE, SIZE, LAST_MODIFIED) → client-side
folded sort. Top-level children only (same boundary as today). MIME→kind
via the ported table; unknown→unsupported records. Virtual documents
(FLAG_VIRTUAL_DOCUMENT) classify deterministically (typed-stream open
attempt or unsupported — decided at implementation with a device proof,
not here). Inaccessible/malformed rows skip with a count, never fail
discovery. Permission loss (SecurityException) → re-request flow, state
preserved. Cancellation via coroutine scope + CancellationSignal.
RECOMMENDATION (verified): DocumentsContract-direct, NOT DocumentFile —
DocumentFile costs ~20× per-file overhead (per-call provider queries;
AOSP source itself recommends DocumentsContract for performance).
MediaStore stays deferred (SAF-first decision stands).

## 7. Metadata mapping (2B, exact)

Photo: ExifInterface over file descriptor (`ExifInterface(fd)` — URI-safe,
an improvement over today's path-string ctor) for the same 11 fields;
GPS via `getLatLong`; orientation raw int; datetimes/exposure/make/model
verbatim strings; dims from SOF/tags; size from stat. Video: retriever
`setDataSource(context, uri)`, same 10 keys, ms duration, ISO-6709
location parsed in core-adjacent pure code, codec passthrough, release
in finally. Status counting (EXIF/container only), orientation-0 rule,
dims ≤0 rule, exposure decimal form: all ported verbatim with the host
vectors replayed. No channel remains; errors surface as the same
`ExtractStatus` + warnings.

## 8. Thumbnail mapping (2C semantics, native mechanisms)

Photo: ImageDecoder primary (`setTargetSize(256)`, EXIF-aware) with
BitmapFactory+inSampleSize fallback for API 26/27 (minSdk stays 26 —
RECOMMENDATION over a minSdk bump, which needs its own justification).
Small sources upscale to target (pinned behavior); >64 MB sources skip;
failures → `failed`/`unavailable`, records intact. Video: port the
`getFrameAtTime` + ≤512px + JPEG-85 behavior verbatim (or
`getScaledFrameAtTime` API 27+ with identical contract — implementation
choice, same assertions). Cache: FNV keys byte-compatible with Dart
(same canonical string), LRU 100/32 MB ported line-for-line,
in-flight dedupe via coroutine Mutex/actor (same semantics as the fixed
`_dedupe`). Coil NOT adopted now: its default keys omit mtime (against
our contract) and its disk cache overlaps ours; revisit ONLY for
crossfade/AsyncImage ergonomics at Viewer time (documented fallback).

## 9. Threading model

Main: Compose state reads only. IO dispatcher: SAF queries, stat,
file/FD opens, EXIF parse, retriever calls, bitmap decode. Default
dispatcher: hashing, sorting, JSON codec. viewModelScope (or a
repository-owned SupervisorJob scope for prefetch) with structured
cancellation: leaving the screen cancels in-flight thumbs/probes;
completed records persist in cache. Sequential metadata probing stays
the default (current contract); bounded concurrency ONLY on measured
need via `limitedParallelism` — explicit YAGNI carried forward.

## 10. Test migration matrix

- rename_core 22 + goldens → JUnit golden runner, SAME 6 JSON files.
- metadata_core 19–20 + 3 goldens → JUnit, SAME JSON files.
- media_library 17 → JUnit (ordering/dedupe/FNV/LRU/paging/record codec).
- Adapter 18+21 → JVM: real-fixture Exif tests (ve_*.jpg/png committed
  binaries) + Robolectric-scripted retriever tests (addMetadata/addFrame
  per path — mirrors today's fake-channel style 1:1) + status rule tests.
- `flutter test` widget/integration suites → Compose UI tests (grid/
  detail states from record fixtures) + instrumented suites on Moto G
  (`pipeline_device_test` scenario ported: stage → discover → records →
  thumbs → hashes unchanged → cleanup).
- `make_fixtures.py` stays (emits the same bytes both stacks consume);
  `parity_probe.py` stays valid (rename semantics are stack-independent).
- Coverage bar: equal or stronger; no test deleted for language reasons.

## 11. Migration order (work packages, each with mission/inputs/outputs/
tests/gates/device/exit/rollback — full text in ADR-005 §B)

- MP0 Reference freeze: tag `flutter-reference-final`, golden digest
  doc, Flutter `main` stays green until MP9. ROLLBACK BASELINE.
- MP1 Native foundation: Gradle/AGP 9.1.0/JVM 17/compileSdk 37/minSdk 26
  (NO upgrades), `:core` + `:app` modules, JUnit4 + Robolectric +
  Turbine + Compose UI-test deps (minimal, justified).
- MP2 Domain port: metadata/media/rename core + golden replay green.
- MP3 SAF layer: grants + DocumentsContract enumerator + classification;
  fake-provider JVM tests + VE_2C-style device test.
- MP4 Metadata extraction port: FD-backed Exif + retriever + rules;
  Robolectric + real-fixture tests; device parity vs reference outputs.
- MP5 Thumbnails/cache: decoder + frame + FNV/LRU port; 256x384 + 256x256
  device vectors green.
- MP6 Pipeline: coroutines port, paging, sequential default; untouched-
  bytes proof.
- MP7 Native Viewer (== 2D-native): Compose grid/detail per G2 contract.
- MP8 Parity validation: side-by-side reference-vs-native device runs +
  real-shot smoke (G2 gate lives here).
- MP9 Flutter retirement: criteria checklist → archive tag
  `flutter-final`, remove Flutter tree from `main` in ONE atomic commit.
  Repo strategy: native modules scaffolded ALONGSIDE `app/` (single
  history, shared goldens/fixtures); `app/` deleted only at MP9.

## 12. Dependency policy (audit)

RETAIN (same artifacts): androidx.exifinterface 1.4.1,
permission-related AndroidX core/activity/result contracts.
REPLACE: path_provider→Context/DataStore paths; shared_preferences→
DataStore Preferences; permission_handler→ActivityResult
RequestPermission; file_picker→OpenDocumentTree contract;
flutter_lints→ktlint (minimal); flutter_test→JUnit4 + Robolectric +
Turbine + Compose UI test; integration_test→instrumented tests;
cupertino_icons/material→Compose Material3 (+ Coil ONLY if §8 fallback
triggers). ELIMINATE: flutter engine/sdk, all hosted Dart deps.
ADD (narrow, justified at MP1): coroutines-core, lifecycle-viewmodel +
runtime-compose, activity-compose/result, datastore-preferences,
documentfile? NO — DocumentsContract direct (no dep needed),
exifinterface (kept). No network libs, no database, no DI framework
(manual construction mirrors current DI style), no MediaStore dep
beyond framework APIs.

## 13. Security/privacy parity gates

Static: manifest MUST NOT contain INTERNET (grep gate in MP1+);
dependency audit rejects network/analytics/crash SDKs; permission list
frozen (media-read + legacy cap) with a diff gate on the manifest.
Dynamic: device suite runs airplane-mode green; no `MANAGE_EXTERNAL_
STORAGE`, no MediaStore enumeration, SAF-only reads, byte-identical
sources asserted (existing untouched-proof pattern). GPS never leaves
the device (no geocoder/network import — import-scope grep gate).

## 14. Risk register (ranked)

R-01 SAF provider variance (OEM/cloud/slow providers break
assumptions): HIGH prob / HIGH impact. Mitigate: per-document try/catch,
timeouts, virtual-doc rule, fake-provider matrix tests. Validate: Moto G
+ second OEM if available. Fallback: raw-path lane retained where valid.
R-02 Retriever/EXIF OEM variance (HEIC/DNG/odd containers): MEDIUM/HIGH.
Mitigate: contract vectors + unknown-grace. Validate: device suite +
real shots. Fallback: per-OEM quarantine notes (tagger pattern).
R-03 Thumbnail OOM on huge/odd files: MEDIUM/HIGH. Mitigate: bounds-
first decode, 64 MB cap, bounded cache, software-decode asserts.
Validate: hostile fixtures on device. Fallback: lower caps.
R-04 Compose/Kotlin/AGP coupling at MP1: MEDIUM/MEDIUM. Mitigate: pin
BOM to the AGP 9.1 lane, no upgrades mid-migration. Fallback: Views
per §4 (kept warm, not built).
R-05 Test-fidelity gap (Robolectric ≠ device): MEDIUM/MEDIUM. Mitigate:
Robolectric for logic, device for platform truth (existing split).
R-06 Momentum loss across a 4–6 week port: MEDIUM/MEDIUM. Mitigate:
golden-first slices (every MP lands green), reference stays runnable.
R-07 Scope creep into MediaStore/G5 writes: LOW/HIGH. Mitigate: frozen
contracts + diff audit per MP (existing discipline).
R-08 URI-permission loss across reboots/updates: LOW/MEDIUM. Mitigate:
persisted grants + re-request flow + state preservation tests.

## 15. Flutter retirement (criteria — ALL required)

Native domain + SAF + metadata + thumbnail + pipeline tests green;
Viewer parity established; device validation passes; security/privacy
gates pass; performance/memory gates pass; golden behavior matches
reference byte-for-byte; THEN archive tag `flutter-final` + atomic
removal. Default: Flutter history stays in-repo forever (tag + history,
never force-pushed away).

## 16. Open questions (for MP1 kickoff, not now)

Q1 Exact Compose BOM + Kotlin versions compatible with AGP 9.1.0 lane
(pin at MP1, not today). Q2 Paging 3 vs manual pages (recommend manual;
revisit on measured need). Q3 Coil fallback trigger threshold. Q4 Second
OEM device for R-01/R-02 breadth (Moto G remains the gate). Q5 Keep
Flutter `app/` building during migration (recommend yes — cheap,
de-risks MP9).

## 17. Sources consulted (research standard §20)

developer.android.com: SAF documents-files + document-provider +
create/manage docs; media-thumbnails guide (Coil/VideoFrameDecoder,
loadThumbnail, ImageDecoder resampler, retriever scaled frames);
ExifInterface release notes + API reference (read/write matrices);
MANAGE_EXTERNAL_STORAGE policy + manage-all-files guide; Android 13
granular permissions; Android 14 partial access. AOSP: DocumentFile
source (overhead note), TreeDocumentFile query behavior. Robolectric:
ShadowMediaMetadataRetriever (addMetadata/addFrame), shadows strategy,
Google's Robolectric guidance (last-resort note — our split honors it:
Robolectric for retriever scripting, real fixtures + device for truth).
Community evidence: DocumentFile ~20× overhead threads, SAF + Coil
content-URI caveats (extensionless URIs need explicit fetchers — our
kind mapping already satisfies this). Coil 3 upgrade notes (KMP,
no-network-default, cache-key mtime removal — the reasons for §8's call).
