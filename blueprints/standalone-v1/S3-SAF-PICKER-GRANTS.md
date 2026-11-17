# S3 — SAF picker + persisted grants (native)

> Status: IMPLEMENTED — PR #5, merged 2026-11-16 (main `2223beb`),
> plus follow-up fixes (see §11). The spec text below is preserved;
> implemented items are ticked in §1 and §6, superseded claims are
> marked in §3, device evidence is recorded in §8, and Glenn's
> Q-decisions are recorded in §10. Research date: 2026-11-15.
> Baseline: main `d103ef6` (PR #2 merged: python-app CI tests real
> code, PyPI workflow removed).

## 0. Reading guide (fact discipline)

Labels used throughout: FACT (verified in repo/docs), REPO EVIDENCE
(file:line or gate result), RECOMMENDATION (judgment), INFERENCE
(strong but unverified), OPEN QUESTION (needs a decision — Glenn's, not
settled here). Assumptions are never presented as facts. Nothing below
claims device validation that has not happened; the Moto G is truth and
the emulator is a hint.

## 1. Goal

Give the native app a real folder source: the user picks a folder once
with the system picker, the app keeps that grant across reboot and
app update, re-asks cleanly when the grant is gone, and enumerates the
folder's top level through the code that already exists for it.

- [x] ACTION_OPEN_DOCUMENT_TREE picker launched from the native app,
      returning a tree URI the app can keep. **Implemented PR #5:**
      `StartActivityForResult` + `buildTreePickerIntent` (see §11
      deviation note — not the spec's recommended
      `OpenDocumentTree()` contract).
- [x] Persisted URI grant taken and stored; the grant survives reboot
      and app update (verified on device, not assumed from the API doc).
      **Verified on BLU B160V with the S4B/S4C runs (see §8):**
      reboot + same-signature reinstall survival confirmed.
- [ ] Grant re-request flow on `SecurityException` (or on a grant that
      no longer appears in `persistedUriPermissions`): user sees what
      happened, the saved folder entry is marked, nothing is renamed
      or deleted, no crash. **Implemented in code (PR #5) but NOT
      device-verified:** the revoke-then-re-pick run was never done.
      Open item — see §8.
- [x] Saved grant list persisted in DataStore: display name, URI
      string, granted timestamp, last-seen-healthy timestamp.
      **Implemented PR #5:** DataStore `vision_grants`,
      `grant_uris` set + `grant:<uri>:<field>` keys
      (`GrantStore.kt`).
- [x] Top-level-only enumeration boundary kept — the S3 slice changes
      how the folder is obtained, not what is enumerated.
- [x] Unknown / virtual-document handling stays exactly as
      `SafDiscovery` already does it (see §2).

## 2. Non-goals

- MediaStore enumeration. FACT: it stays deferred. REPO EVIDENCE:
  `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:130-145` (§6,
  "MediaStore stays deferred (SAF-first decision stands)") and K1
  (D2 proved the SAF-pick + raw rename path, so no MediaStore
  fallback is needed to reach v1).
- `MANAGE_EXTERNAL_STORAGE`. FACT: banned for this app. REPO EVIDENCE:
  `blueprints/blueprint-sections/BP-05-android-shell-permissions.md:10`
  ("No `MANAGE_EXTERNAL_STORAGE` unless D2 evidence demands it") —
  D2 did not demand it (raw rename works), so the ban stands.
- Recursive descent. The enumerator is top-level only and S3 does not
  change that boundary.
- Any rename execution. That is S4. S3 delivers the folder + the
  grant; S4 consumes them.
- Any write mechanism decision. See OPEN QUESTION Q1 — the grant's
  read/write flags are Glenn's decision, not this spec's.

## 3. Facts verified in code (grounding)

### 3.1 The enumerator already exists and is tested — the picker does not

- FACT: `SafDiscovery` is DocumentsContract-direct, one projection
  query, top-level only. REPO EVIDENCE:
  `native/vision-android/src/main/kotlin/com/visionengine/android/saf/SafDiscovery.kt:25-36`
  (`discoveryProjection`, six columns: DOCUMENT_ID, DISPLAY_NAME,
  MIME_TYPE, SIZE, LAST_MODIFIED, FLAGS),
  `:43` (`getTreeDocumentId`), `:52-57`
  (`buildChildDocumentsUriUsingTree` + single `queryChildren` call),
  `:160` (`FLAG_VIRTUAL_DOCUMENT` → kind `unsupported` + warning).
  Per-row failures skip with a count, never fail discovery; invalid
  tree URI, null cursor, query exception, and missing projection
  columns each yield a warnings-only outcome (`:49`, `:101`).
- FACT: the fake-provider test suite is 17 tests, Robolectric, and
  asserts exactly one query per discovery. REPO EVIDENCE:
  `native/vision-android/src/test/kotlin/com/visionengine/android/saf/DiscoveryTest.kt`
  (17 `@Test` methods; `singleQueryPerDiscovery`, `authoritiesScopeIdentities`,
  `virtualDocumentBecomesUnsupportedWithWarning`, `foldedOrderingBeatsProviderOrder`,
  `nullCursorYieldsWarning`, `queryFailureYieldsWarning`,
  `missingColumnYieldsWarning`, `invalidTreeUriYieldsWarning`).
- FACT: `SafDiscovery` currently has no production caller. REPO
  EVIDENCE: grep for `SafDiscovery` / `withResolver` across
  `native/` finds only `SafDiscovery.kt` itself and `DiscoveryTest.kt`.
  S3 is what wires it to a real user action.
  **Superseded 2026-11-16 (PR #5):** `MainViewModel`
  (`handlePickedFolder` / `handleRepickedFolder`) and
  `RenamerViewModel` now call `SafDiscovery` on real user actions.
- FACT: no picker or grant-taking code exists yet in either stack.
  REPO EVIDENCE: grep for `takePersistableUriPermission`,
  `OpenDocumentTree`, `ACTION_OPEN_DOCUMENT_TREE`,
  `persistedUriPermissions` across `native/` and `app/` returns no
  matches. The Flutter Probe tab picks with
  `FilePicker.getDirectoryPath` (a path, not a persisted SAF grant) —
  REPO EVIDENCE: `app/lib/features/probe/probe_screen.dart:58-66`.
  S3 is new code, not a port.
  **Superseded 2026-11-16 (PR #5):** picker + grant-taking code
  exists in `AppRoot.kt` (launcher) and `MainViewModel.kt`
  (`takePersistableUriPermission` with READ+WRITE flags,
  `persistedGrantUris` lookup).
- FACT: no DataStore exists yet anywhere in `native/`. REPO EVIDENCE:
  grep for `DataStore` / `datastore` across `native/` returns no
  matches. The dependency is pre-justified, not invented here:
  REPO EVIDENCE: migration blueprint §12
  (`blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:229-245`)
  lists `datastore-preferences` under "ADD (narrow, justified at
  MP1)". S3 still records the add in its own diff audit (see §7).
  **Superseded 2026-11-16 (PR #5):** `GrantStore.kt` uses DataStore
  Preferences (`vision_grants`); dep declared in
  `native/vision-android/build.gradle.kts`.

### 3.2 The blueprint contract S3 must honor

- FACT: migration blueprint §6 prescribes the shape. REPO EVIDENCE:
  `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:130-145`:
  ACTION_OPEN_DOCUMENT_TREE → `takePersistableUriPermission` (READ
  only — "no write grant — the app is read-only") → `getTreeDocumentId`
  → `buildChildDocumentsUriUsingTree` + ONE projection query →
  client-side folded sort, top-level children only. Two notes on that
  text, both FACTS: (a) the §6 projection lists five columns while
  `SafDiscovery.kt:25-36` has six — the sixth is FLAGS, which the
  virtual-document rule needs; the code is the authority, §6's list is
  stale in that one detail; (b) §6's "READ only" was written when the
  app was read-only (the MP0–MP6 pipeline). S4 rename needs a write
  mechanism, so the flag choice is re-opened as OPEN QUESTION Q1
  rather than silently carried forward.
- FACT: the blueprint already names this slice's risk. REPO EVIDENCE:
  migration blueprint §14 R-08
  (`blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:278`):
  "URI-permission loss across reboots/updates: LOW/MEDIUM. Mitigate:
  persisted grants + re-request flow + state preservation tests."
  S3 is the R-08 mitigation, implemented.
- FACT: permission policy is frozen at media-read. REPO EVIDENCE:
  migration blueprint §1.I and §13
  (`blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md:22-24,247-255`):
  READ_MEDIA_IMAGES/VIDEO + legacy capped at 32; manifest diff gate
  on the permission list; no INTERNET (grep gate). S3 adds no
  manifest permission — the SAF grant is a runtime grant, not a
  manifest permission — and the diff gate must still pass (§7).

### 3.3 The D2 evidence S3 builds on

- FACT: D2 proved SAF-pick + raw rename works on Moto G 2025.
  REPO EVIDENCE: `blueprints/CURRENT_STATE.md:68,82` (K1 closed
  2026-09-15: "D2 PROVED it WORKS on Moto G 2025 in a user-picked
  folder (`Pictures/VE_TEST`). SAF-pick + raw rename is the validated
  primary; MediaStore deferred") and
  `app/lib/adapters/storage_probe.dart:143-203` (shared probe: creates
  `VE_PROBE_*` files in the picked folder, renames 2/2, undo 2/2,
  `RAW_RENAME: WORKS here`, `SHARED_PROBE: COMPLETE`, `cleaned=true`).
- FACT: D2's scope boundary still applies. REPO EVIDENCE:
  `docs/android-metadata-matrix.md:25-33`: "D2 does NOT prove rename
  of pre-existing third-party (camera-created) media. That is G5
  territory — never assume, only probe." S3's device test (§8) must
  not over-claim: picking a folder and enumerating it is not proof
  that renaming pre-existing camera files in it works. That proof
  belongs to S4's staged smoke (§8 there).

## 4. Inputs (exact files)

| File | Role in this slice |
|---|---|
| `native/vision-android/src/main/kotlin/com/visionengine/android/saf/SafDiscovery.kt` | Enumerator to wire up; unchanged unless a defect is found (any change needs its own justification line) |
| `native/vision-android/src/test/kotlin/com/visionengine/android/saf/DiscoveryTest.kt` | Fake-provider pattern to extend (grant-loss scripted tests reuse `ScriptedSource` style) |
| `native/vision-app/src/main/kotlin/com/visionengine/app/MainActivity.kt` | Activity hosting the picker launcher |
| `native/vision-app/src/main/kotlin/com/visionengine/app/AppRoot.kt` | Placeholder shell today (two `Text` lines); gains the folder entry point (see §5) |
| `native/vision-app/src/main/kotlin/com/visionengine/app/MainViewModel.kt` | `AppUiState(appName, ready)` today; gains grant-list state |
| `native/vision-app/build.gradle.kts` | Compose BOM 2024.09.00, activity-compose 1.10.1 already present; DataStore add goes here |
| `native/vision-android/build.gradle.kts` | `vision-core`, exifinterface 1.4.1; no new dep expected here |
| `blueprints/NATIVE_ANDROID_MIGRATION_BLUEPRINT.md` (§6, §12, §13, §14 R-08) | Contract, dependency justification, gates, risk |
| `blueprints/blueprint-sections/BP-05-android-shell-permissions.md` | SAF folder picker (persisted URI) as folder source; MANAGE_EXTERNAL_STORAGE ban |
| `docs/device-probe-moto-g.md` | Runbook style for the device evidence report (§8) |
| `app/lib/features/probe/probe_screen.dart`, `app/lib/adapters/storage_probe.dart` | Reference behavior only (what D2 proved); NOT ported — Flutter stays frozen reference |

## 5. Step-by-step work

1. **Grant store (DataStore Preferences).** New file(s) in
   `vision-android` (RECOMMENDATION: android module owns it — it is
   the storage/SAF layer per migration blueprint §3; the app module
   consumes it). Record per grant: tree URI string, display name
   (from `DocumentsContract.getTreeDocumentId` or the picked folder's
   display name — record which source is used), granted epoch ms,
   last-seen-healthy epoch ms. One entry per tree URI; re-picking the
   same folder updates the entry, never duplicates it. Keys must be
   stable across app updates (no versioned key names).
2. **Picker launcher.** `rememberLauncherForActivityResult` with
   `ActivityResultContracts.OpenDocumentTree()` in the Compose UI
   (RECOMMENDATION: this contract exists in activity-compose 1.10.1,
   already a dependency — no new dep). The intent carries
   `Intent.FLAG_GRANT_READ_URI_PERMISSION` (plus WRITE only if Q1 is
   decided that way — default to the flag set Glenn approves) and
   `Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION`.
   RECOMMENDATION: set `DocumentsContract.EXTRA_INITIAL_URI` to the
   Pictures tree when the device exposes it, so the picker opens near
   DCIM instead of at the provider root; fall back silently to no
   initial URI if the extra is rejected — a rejected extra must never
   crash the launch.
3. **Take the grant.** On a non-null result URI, call
   `contentResolver.takePersistableUriPermission(uri, flags)` inside
   try/catch. If it throws (`SecurityException` — provider refused
   persistability), show the failure in the UI and do NOT record the
   grant as persisted; offer "Try again". A grant the provider will
   not persist is not a saved folder.
4. **Verify before trusting.** After taking the grant, immediately run
   `SafDiscovery.withResolver(resolver).discover(treeUri)` once. If
   the outcome carries warnings (`invalid tree uri`, `query failed`,
   `empty result` on a folder the user knows is non-empty — careful:
   an empty folder is a legitimate empty outcome, see §6), surface
   the warning text. Only a warning-free (or empty-folder) first
   discovery marks the grant healthy and writes
   `last-seen-healthy`. INFERENCE (not yet device-proved): a tree URI
   that enumerates once will enumerate again while the grant lives;
   the device test (§8) is what proves or refutes this per provider.
5. **Grant health check on launch.** On app start, for each saved
   grant: check `contentResolver.persistedUriPermissions` for a
   matching URI with the needed flags. Missing → mark the entry
   "grant lost — re-pick to restore" (keep the entry, keep its display
   name, do NOT silently delete it — the user chose that folder and
   deserves to know it stopped working). Present → re-run a light
   discovery (or a `takePersistableUriPermission` no-op read — pick
   one, document it) and update `last-seen-healthy` on success.
6. **Re-request flow.** Any `SecurityException` from a SAF call
   surfaces as a distinct UI state: "Vision lost access to <name>.
   Re-pick the folder to restore it." The re-pick button re-runs step
   2 for that entry; success replaces the stored URI in place (same
   entry, new grant timestamp). State already on screen (the saved
   list, any in-progress S4 plan) is preserved across the flow —
   REPO EVIDENCE for the requirement: migration blueprint §6,
   "Permission loss (`SecurityException`) → re-request flow, state
   preserved."
7. **Revocation UX.** The user can revoke in system Settings at any
   time; the app must treat the next health check (step 5) as the
   discovery point, never poll in the background. Revoked grants are
   never auto-retried, never re-requested without the user tapping.
   A "Forget this folder" action removes the DataStore entry (and
   calls `releasePersistableUriPermission` best-effort); forgetting
   never touches the folder's contents.
8. **Enumeration boundary — unchanged.** S3 calls `SafDiscovery`
   exactly as tested: top-level children only, one projection query,
   folded sort client-side. Directories are skipped and counted
   (`skippedDirs`), virtual documents become `unsupported` with a
   warning, unknown MIME → `unsupported`. S3 adds no recursive
   descent, no second query, no DocumentFile.
9. **UI (thin, explicit).** A folder list: display name, grant state
   (healthy / lost / never-verified), item count from the last
   discovery, "Pick a folder" button, per-entry "Re-pick" and
   "Forget". No thumbnails, no metadata in this slice — those belong
   to the pipeline (MP6, done) and the Viewer (S5). The screen must
   work with zero grants (empty state, not an error) and with a
   grant whose folder is empty (empty state, not an error).
10. **No-INTERNET / permission-diff gates.** Run the §7 gates after
    every dependency or manifest touch. The manifest gains no
    permission in this slice; if any step seems to need one, stop and
    flag it — that is a spec change, not an implementation detail.

## 6. Acceptance criteria checklist

- [x] Picking a folder via the system picker returns a tree URI and
      the app takes a persistable grant for it (flags per Q1's answer).
      **Done PR #5, flags READ+WRITE per Glenn's Q1 decision
      (15 Nov 2026); proven on B160V (Stage A grant + discovery).**
- [x] The grant appears in `contentResolver.persistedUriPermissions`
      after the pick (asserted on device, §8). **Done: grant taken
      with READ+WRITE flags and listed by `persistedGrantUris`
      (`GrantHealth.kt`); reboot + reinstall survival (below)
      confirms persistence on B160V.**
- [x] After a reboot, the saved folder still enumerates with no
      re-pick (asserted on device, §8). **Verified on BLU B160V
      with the S4B/S4C runs, 17 Nov 2026.**
- [x] After an app update (debug reinstall over the existing install),
      the saved folder still enumerates with no re-pick (asserted on
      device, §8). INFERENCE, not FACT, until the device run proves it.
      **Verified on BLU B160V with the S4B/S4C runs, 17 Nov 2026
      (same-signature reinstall).**
- [ ] A forced grant loss (revoke in system Settings, or clear via
      `releasePersistableUriPermission` in a test build) shows the
      "grant lost — re-pick" state; no crash, no silent drop, no
      rename attempted. **NOT verified — the revoke run was never
      done. Open item; code exists (`deriveFolderStates` LOST state
      + "Re-pick" UI), device proof outstanding.**
- [ ] Re-picking the lost folder restores it in place (same list
      entry, updated timestamps); pre-loss screen state is preserved.
      **NOT verified — same open item as above.**
- [x] Picking the same folder twice creates one entry, not two.
      **Done PR #5: `upsertGrant` replaces same-URI entries in
      place (`GrantModel.kt`); `GrantModelTest` covers it.**
- [x] `SecurityException` thrown by `takePersistableUriPermission`
      (provider refuses persistability) is shown to the user; the
      folder is NOT recorded as a saved grant. **Done PR #5:
      `handlePickedFolder` catches it → pick-failure state, no
      save (`MainViewModel.kt`).**
- [x] Empty folder enumerates to an empty list with zero warnings
      treated as success (an empty folder is not an error).
      **Done PR #5: an empty folder is healthy (warnings empty →
      `lastSeenHealthyMs` set); verified in code.**
- [x] A folder containing a virtual document (cloud-backed) lists it
      as `unsupported` with a warning, exactly as `SafDiscovery`
      does today — S3 adds no new classification. **Unchanged
      `SafDiscovery` behavior; `DiscoveryTest` covers it.**
- [x] Subdirectories are skipped and counted (`skippedDirs`), never
      descended into. **Unchanged `SafDiscovery` behavior.**
- [x] Enumeration still issues exactly one query per discovery
      (existing `singleQueryPerDiscovery` test stays green; no second
      query added for grant checks — health checks reuse discovery or
      `persistedUriPermissions`, never a new listing query).
      **Done: `DiscoveryTest.singleQueryPerDiscovery` green;
      launch health check is a `persistedUriPermissions` lookup
      only (`refreshHealth`).**
- [x] "Forget this folder" removes the entry and releases the grant;
      folder contents are untouched. **Done PR #5: `forgetFolder`
      removes the DataStore entry + best-effort
      `releasePersistableUriPermission`; never touches contents.**
- [x] Zero grants → calm empty state; the screen never presents an
      error for having no folder yet. **Done PR #5: "No folders yet
      — pick a folder to begin" (`AppRoot.kt`).**
- [x] Manifest permission list unchanged (diff gate, §7); no INTERNET
      in the manifest (grep gate, §7). **Done PR #5: both manifests
      unchanged, `grep -ri internet` no match.**
- [x] DataStore is the only new dependency, and it is the one
      migration blueprint §12 already justifies — recorded in the PR
      body with that citation. **Done PR #5, with one deviation:
      `kotlinx-coroutines-core:1.7.3` was also added explicitly
      (both modules, `65a27a9`, Glenn-approved) because DataStore's
      POMs scope it runtime-only — see §11.**

## 7. Gates

- Host: `./gradlew :vision-android:testDebugUnitTest
  :vision-app:testDebugUnitTest` — all green, including the 17
  existing `DiscoveryTest` tests (no test deleted or weakened).
- Host: Android Lint on `:vision-app` (`./gradlew :vision-app:lintDebug`)
  — no new warnings introduced by this slice.
- Host: manifest permission diff — `git diff` on
  `native/vision-app/src/main/AndroidManifest.xml` (and the
  library manifest) shows no added permission; `grep -ri internet`
  on the manifest shows nothing.
- Host: dependency audit — the only new artifact is
  `androidx.datastore:datastore-preferences`; no network, analytics,
  or crash SDK; record the §12 citation in the PR body.
- Device: §8 evidence run complete with a pasted report (same
  runbook style as D2). No device evidence → slice is not done,
  however green the host gates are.
- Human gate: Glenn reviews the PR diff; the Q1 answer (if decided
  before implementation) is recorded in the PR body, not buried in
  code.

## 8. Device evidence required (Moto G — the gate)

**Evidence recorded (2026-11-17, BLU B160V — hardware substitution
for the Moto G named above; no Moto G coverage claimed):**

1. Android version: Android 14 (BLU B160V, ADB serial
   `7040016025040287`).
2. Test folder: `Pictures/VE_TEST_S4` (Stage A) / fresh
   `Pictures/VE_TEST_S4B` (S4B re-run), seeded files — never live
   DCIM.
3. Folder list after the pick: grant taken, discovery enumerated
   (Stage A: SAF grant + discovery PASS; preview + collision
   planning PASS).
4. **Reboot → folder still enumerates, no re-pick: VERIFIED**
   (S4B/S4C runs, 17 Nov 2026).
5. **Reinstall (same-signature debug reinstall) → folder still
   enumerates, no re-pick: VERIFIED** (S4B/S4C runs, 17 Nov 2026).
   App-private manifests also survived the reinstall.
6. **Revoke in system Settings → "grant lost — re-pick" state:
   NOT VERIFIED — open item.** The revoke run was never done; the
   code path exists (`deriveFolderStates` LOST state, "Re-pick"
   button in `AppRoot.kt`) but has no device proof.
7. Consent dialogs: none reported beyond the SAF picker itself.
8. Provider variance: ExternalStorageProvider behavior verified
   through the S4B/S4C runs (including the PR #7 preflight fix for
   its `FileNotFoundException` on a not-yet-existing destination).

Run on Glenn's Moto G (same setup as the D2 run:
`docs/device-probe-moto-g.md` — USB debugging, debug run from Android
Studio). Paste back — the original ask, preserved (items 4–5 above
answer parts of it on B160V; item 6 remains open):

1. Android version (Settings → About phone → Android version).
2. Pick a folder the operator creates for the test (a throwaway
   folder, e.g. `Pictures/VE_TEST_S3`, with a few seeded camera-style
   files copied in — never live DCIM for this slice).
3. The app's folder list after the pick: display name, grant state,
   item count. Confirm the grant appears in
   `persistedUriPermissions` (log line or a debug readout on the
   screen — implementation's choice, recorded in the PR).
4. Reboot the phone → open the app → folder still enumerates, no
   re-pick. Paste the before/after counts.
5. Reinstall the debug build over the existing install → folder
   still enumerates, no re-pick.
6. Revoke the grant in system Settings → open the app → "grant lost
   — re-pick" state appears, no crash. Re-pick → restored in place.
7. Which consent dialogs appeared, and when (picker, any provider
   prompts).
8. Any provider variance observed (e.g. the picker surfacing a cloud
   provider's tree, a virtual document in the listing).

RECOMMENDATION: record per-provider notes the way the Tagger fleet
keeps per-OEM quarantine notes (migration blueprint §14 R-02
fallback) — one line per provider that misbehaved, in the PR body or
a docs note, so the next slice inherits the knowledge.

## 9. Risks

- R-01 (from migration blueprint §14): SAF provider variance —
  OEM/cloud/slow providers break assumptions. Mitigation here:
  per-call try/catch already in `SafDiscovery`, warnings-only
  outcomes, fake-provider matrix tests, and the device run on more
  than one provider tree if Glenn has one handy. Highest-probability
  risk in this slice.
- R-08 (migration blueprint §14): URI-permission loss across
  reboots/updates — this slice IS the mitigation; if the device run
  shows the grant does not survive a debug reinstall, that is a
  finding to report, not a failure to hide. A release-signed build
  may behave differently from debug — INFERENCE, flag in the PR if
  observed.
- Grants taken with the wrong flags (Q1 undecided at implementation
  time) would force a second pick for every user later. Mitigation:
  do not implement the flag choice until Q1 is answered; if the slice
  must land with READ-only first, say so in the PR body and treat
  WRITE as a named follow-up, not a silent default.
- DataStore schema drift: keys chosen now are the contract for S4+
  (the Renamer screen reads the same grant list). Mitigation: keys
  named for what they are (`grants/<uri-hash>/displayName` style is
  one option — implementation's choice, but the choice is
  documented in the PR and frozen afterwards; renaming keys later
  needs a migration note).
- The picked folder's display name is provider-supplied and may be
  blank or odd on some providers. Mitigation: fall back to the tree
  document ID, never to an empty string; record which source was
  used per entry.

## 10. Open questions (for Glenn — NOT decided here)

- Q1 — **Grant flags: READ-only, or READ+WRITE?** Migration
  blueprint §6 says READ-only because the app was read-only then.
  S4 rename needs a write mechanism, and the two candidate
  mechanisms want different flags: `DocumentsContract.renameDocument`
  needs a WRITE persistable grant; raw `File.rename` on a resolved
  path needs no SAF write grant at all (that is what D2 proved — but
  D2 resolved a real filesystem path from `file_picker`, and a SAF
  tree URI does not reliably yield one). FACT: neither mechanism has
  been proven from a SAF tree URI on this device. The choice decides
  the flags S3 requests, so it should be answered before S3
  implements — or S3 lands READ-only first with WRITE as a named,
  separately-reviewed follow-up.
  **Decided 2026-11-15 (Glenn): READ+WRITE grants now** — so S4
  rename goes via `DocumentsContract.renameDocument`. Implemented
  PR #5 (`grantFlags()` READ|WRITE, `persistedGrantUris` requires
  both); provider support proven on B160V by the S4B/S4C runs.
- Q2 — **Rename mechanism: `DocumentsContract.renameDocument` vs raw
  `File.rename` from a SAF tree?** Same fork as Q1, stated from the
  S4 side. If `renameDocument` is chosen, what happens on providers
  that do not support it (returns what — null, exception, silent
  no-op)? Needs a device probe per provider; S4's smoke covers it,
  S3 must not bake in an assumption.
  **Decided by Glenn's Q1 answer (15 Nov 2026) + PR #6:**
  `renameDocument`; null return → "rename refused" (skipped),
  exception → ERROR, verified by re-query after the op. Proven on
  B160V (S4B/S4C); other providers unprobed.
- Q3 — **How many saved folders?** One (the current camera folder)
  or a list the user grows? RECOMMENDATION: a list (the DataStore
  shape already supports it, and a second folder costs nothing),
  but if Glenn wants exactly one slot, the UI and the store should
  say so up front rather than growing a list nobody asked for.
  **Decided 2026-11-15 (spec recommendation adopted in PR #5): a
  list** — `GrantStore` holds one entry per tree URI, UI lists
  them.
- Q4 — **Initial picker location:** is opening the picker near
  Pictures/DCIM via `EXTRA_INITIAL_URI` wanted, or should the picker
  always open at the provider root and let the user navigate? Minor,
  but it changes first-run UX.
  **Decided 2026-11-15 (spec recommendation adopted in PR #5):**
  picker opens near Pictures via `EXTRA_INITIAL_URI`
  (`picturesTreeUri()`, `primary:Pictures`) with silent fallback to
  a bare intent.
- Q5 — **Grant health check on launch: cheap
  `persistedUriPermissions` lookup only, or a live discovery
  re-run?** The lookup is free and never touches the provider; the
  re-run proves the provider still answers but costs a query and can
  surface provider slowness on every launch. RECOMMENDATION: lookup
  on launch, live re-check only when the user opens the folder —
  but this is a judgment call, flag it if it feels wrong.
  **Decided 2026-11-15 (spec recommendation adopted in PR #5):**
  lookup-only on launch (`refreshHealth`); live re-check when the
  user opens the folder (S4 folder-open discovery). Note:
  `GrantStore.markHealthy` exists but is never called — the
  last-seen-healthy timestamp is stamped only at pick time, never
  refreshed afterwards.
- Q6 — **Should a lost grant keep its entry forever, or expire?**
  RECOMMENDATION: keep it until the user forgets or re-picks it
  (an entry is a bookmark the user chose). Alternative: auto-drop
  after N days — needs a reason to exist, none seen yet.
  **Decided 2026-11-15 (spec recommendation adopted in PR #5):**
  keep the entry until forgotten or re-picked
  (`deriveFolderStates` LOST state; `forgetFolder` removes it).

## 11. Implementation record (PR #5, merged 2026-11-16, main `2223beb`)

- **Files:** `GrantModel.kt`, `GrantStore.kt`, `GrantHealth.kt`,
  `FolderDisplayName.kt`, `SafPicker.kt` (new, in
  `native/vision-android/.../saf/`); `GrantModelTest.kt` (14 tests);
  `MainViewModel.kt` + `AppRoot.kt` modified (folder list,
  Re-pick/Forget, pick-failure + "Try again"); `MainActivity.kt`
  unchanged.
- **Deviations from this spec (recorded in the PR #5 body):**
  (1) picker uses `StartActivityForResult` + a manual intent, NOT
  the recommended `OpenDocumentTree()` — its `createIntent` cannot
  carry the READ+WRITE + PERSISTABLE flags; (2) DataStore dep in
  `vision-android`, not `vision-app`; (3) live re-check deferred
  to S4 folder-open, and `markHealthy` remains uncalled — the
  last-seen-healthy timestamp is stamped only at pick time;
  (4) display name queried at pick time (`resolveFolderDisplayName`,
  falls back to the tree document ID, never empty); (5) DataStore
  `IOException`s caught and surfaced as pick-failure text.
- **Dependency audit:** `datastore-preferences:1.1.1` (justified by
  migration blueprint §12) + `kotlinx-coroutines-core:1.7.3`
  explicit in both modules (`65a27a9`, Glenn-approved 15 Nov
  2026) — DataStore's POMs scope coroutines runtime-only, so the
  compile classpath lacked it.
- **Host-gate fixes on the PR #5 branch:** `preferencesDataStore`
  import package (`67275cf`), explicit `vision-core` dep in
  `vision-app` (`86c47c3`), stale-test rewrites (`eb696b9`),
  manifest activity name (`bf0e480` — the `.MainActivity`
  `MissingClass` lint error was pre-existing). Host gate green on
  the laptop (unit tests + lint).
- **DataStore key scheme (frozen contract for S4+):**
  `grant_uris` string-set index + per-entry
  `grant:<uri>:displayName` / `:displayNameSource` / `:grantedMs` /
  `:lastSeenHealthyMs`. Renaming keys later needs a migration note.
- **Device evidence:** see §8. Reboot + reinstall verified on BLU
  B160V (S4B/S4C runs); revoke-then-re-pick NOT verified.
