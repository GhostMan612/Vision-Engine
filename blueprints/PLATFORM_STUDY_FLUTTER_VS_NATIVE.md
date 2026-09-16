# PLATFORM STUDY — Flutter vs pure native Kotlin (research only, no code)

> Status: STUDY for operator review. Decides NOTHING by itself. The decision
> it informs must precede 2D (Viewer UI) — building the Viewer in the wrong
> stack then rewriting it is the maximum-waste path. Date: 2026-09-15.

## 0. Verdict up front

- **If Vision Engine is Android-only forever: migrate to native Kotlin +
  Jetpack Compose — but only after the 2C device gate goes green.** The
  device-validated implementation is the baseline a port is verified
  against; migrating from unvalidated code means debugging the port and
  the product simultaneously.
- **If iOS is ever (even vaguely) on the horizon: stay on Flutter.**
  That single answer dominates every technical argument below.
- Either way, **do not authorize 2D until the platform is locked.**
- Honest qualifier the study must state plainly: the performance case for
  native is weaker than it looks (decoding already happens natively in
  both stacks; the channel costs microseconds per call). The REAL native
  advantages are complexity surface, APK/toolchain simplicity, and
  first-class SAF/lifecycle — not throughput. And native does not escape
  toolchain management (Compose compiler ↔ Kotlin coupling is strict).

## 1. Current inventory (exact, at 2C host-complete)

Pure-Dart domain (no Flutter imports):
`packages/rename_core` — prefix/strip, buildPlan + 3 collision classes,
manifest CSV codec, undo ordering; 22 host tests + 6 goldens;
`tools/parity_probe.py` proves Python↔Dart parity.
`packages/metadata_core` — Provenance, MetaField, PhotoMeta/VideoMeta
with corrected-dimension semantics, fixture codec, ExtractStatus; 19–20
host tests + 3 goldens.
`packages/media_library` — MediaSource/Record/Kind, ThumbInfo states,
folded ordering + dedupe, FNV cache keys, LRU cache, paging; 17 tests.

Android bridge (Kotlin, ~150 lines):
`MetadataBridge` (ExifInterface probe, retriever probe, video-frame grab)
+ `MainActivity` channel wiring (`vision_engine/metadata`: probePhoto,
probeVideo, getVideoFrame).

Dart adapters + pipeline (`app/lib`):
`metadata_adapter` (channel decode, MIME table, ISO-6709, status rules),
`local_rename_adapter`, `storage_probe`, `media_lister`, `thumb_store`
(Flutter-codec decode), `library_pipeline` (discover → page → probe).
44 host tests; 2 integration suites (5 + 1 tests) awaiting final device
rerun.

UI today: 3 thin tabs (Rename/Probe/Settings) + SettingsStore. No Viewer.
Roughly 25 Dart source files outside tests — the UI surface is SMALL;
the domain/test surface is where the value lives.

## 2. What ports cleanly (concept preservation, goldens reusable)

The golden JSON fixtures (`rename_core` 6 files, `metadata_core` 3 files)
are stack-agnostic: a Kotlin port replays the SAME vectors through JUnit.
Status rules, orientation/rotation tables, ISO-6709 vectors, ordering
vectors, cache-eviction vectors — all reusable as test data. This is the
dividend of the Atlas-style golden discipline: the CONTRACTS survive even
if Dart does not. Estimated re-proof, not re-derivation.

Likely Kotlin shape (mirrors ADR-001/ADR-004):
`vision-core` (pure JVM: Provenance, MetaField, PhotoMeta/VideoMeta,
MediaSource/Record/Kind, ordering, FNV keys, LRU policy, rename plan) →
`vision-android` (SAF tree access, ExifInterface, retriever, frame grab,
ImageDecoder/BitmapFactory thumbs, DataStore prefs) →
`vision-app` (Compose UI: viewer grid/detail, rename preview, probe,
settings).

## 3. What must be rewritten (no salvage)

- All three packages' implementations (models are trivial; plan/collision
  logic, codec edge cases, and cache semantics must be re-expressed).
- `MetadataAdapter` decode/normalization (MIME table, ISO-6709, zero and
  orientation rules, status counting) — the K9–K19 lessons re-encoded.
- `ThumbStore`/`LibraryPipeline` orchestration → coroutines/Flow.
- The entire UI (small today: 3 tabs; the Viewer is the real cost and is
  unwritten in EITHER stack — that is precisely why this decision is cheap
  now and expensive later).
- Test harness: JUnit (+ Robolectric where JVM fidelity matters) for unit
  goldens; instrumented tests replace `integration_test/`; the Python
  parity probe stays valid (it tests rename semantics, stack-independent).
- `tools/make_fixtures.py` stays (pure Python, emits fixture bytes).

## 4. Honest effort estimate (solo developer, calendar time)

- `vision-core` port + JUnit golden re-proof: 1–2 weeks.
- `vision-android` (SAF + extraction + thumbs, logic already proven): ~1 week.
- Compose app (rename/probe/settings + viewer): 1–2 weeks.
- Parity/device re-validation to current gates: ~1 week.
- **Total: 4–6 weeks to current parity + Viewer**, during which the
  Android app stagnates. Staying the course: 2D Viewer in Flutter is
  ~1–2 weeks on the existing pipeline. The migration premium is real and
  must be weighed, not hand-waved.

## 5. What Flutter provides that would be lost

- The 44 host tests + suites as-runnable (ported, not kept).
- Hot-reload UI iteration speed (Compose Preview narrows but does not
  close this gap).
- One test runner for domain + UI + (via integration) device.
- The iOS path, if it ever matters (see §7 — this alone can veto).
- Zero Kotlin/Gradle/Compose-compiler coupling management (traded for
  Flutter/Gradle/AGP coupling — a trade, not an escape).

## 6. What native gains (verified claims only)

- SAF/DocumentFile/ContentResolver/persisted grants first-class: no
  channel, no dual-side path juggling, direct-: the validated raw path
  stays, URI-backed sources become natural instead of designed-around.
- `ImageDecoder` (sampled + mutable + post-processor), `BitmapFactory`
  bounds-first flows, `ParcelFileDescriptor` streaming — no full-file
  reads to decode a thumbnail (today's `readAsBytes` + 64 MB cap is a
  host-side workaround for a limitation native does not have).
- Lifecycle-aware cancellation (ViewModel + coroutines) for thumbnail
  prefetch/discovery — today's sequential-pages design is correct but
  conservative; native makes bounded concurrency idiomatic.
- Debugging surface shrinks from six layers to three for platform bugs.
- APK without the Flutter engine (~20 MB+ saved; matters more if/when
  an `ffmpeg` lane ever lands, since that dominates anyway).
- No dual-SDK failure class (K14): one toolchain, one Dart-less story.

## 7. The decisive question (GQ8 — operator must answer)

**Is iOS ever a requirement — even a vague, far-future one — for Vision
Engine?**
- YES / MAYBE → stay on Flutter. Revisit native only if the channel
  surface exceeds ~10 methods or background/sync/widget needs appear.
- NO, Android-only forever → migrate to native Kotlin + Compose after
  the 2C device gate goes green, and design 2D as the native Viewer.

## 8. Recommended sequence (no code until decided)

1. Human reruns `flutter test integration_test` (already authorized,
   still pending) → 2C device green locks the porting baseline.
2. Operator answers GQ8 and issues the platform verdict.
3. If native: freeze Flutter `main` as the reference implementation,
   scaffold `vision-*` modules, port golden-first (fixtures replay),
   Compose Viewer as 2D-native. If Flutter: authorize 2D on the current
   stack immediately.
4. Either way, 2D is designed ONCE, in the chosen stack.
