# BP-05 — Android shell & permissions (adapter track)

> Phase 5 (runs alongside 1–3 as adapter work). Moto G is truth; emulator is a hint.

## Objective
App shell that can actually rename shared DCIM files on modern Android without tripping scoped-storage.

## Scope
- Scaffold: `app/` (`com.visionengine` proposal — D1), `minSdk 26 / compileSdk 36`, 64-bit ABIs `arm64-v8a,x86_64` (tagger `build.gradle` pattern), launcher icon/label (Recovery `flutter_launcher_icons` pattern), `analysis_options.yaml` excludes.
- Permissions (`permission_handler`): `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` (33+), `READ_EXTERNAL_STORAGE` (≤32); request-before-batch; degraded viewer state when denied. No `MANAGE_EXTERNAL_STORAGE` unless D2 evidence demands it.
- Storage adapter: SAF folder picker (persisted URI) as folder source; MediaStore query for grid; rename commit via `DISPLAY_NAME` update; `createWriteRequest` consent on 11+ (tagger `storage`-channel pattern: small Kotlin `StorageBridge`, Dart wrapper); app-private cache uses plain `File.rename`; temp/pre-op backups under `getTemporaryDirectory` with purge-on-both-paths.
- Shell UX: bottom-nav (Rename | Viewer | Edit | Settings), rename preview→confirm→execute→undo wired to `rename_core`, settings (prefix list, case toggle, recursive toggle, backup/restore of SETTINGS only via XOR pattern — never media).
- Signing: debug fallback (tagger `build.gradle:62` pattern); release keystore never committed (RULES.md §1.3).

## Non-scope
No `ffmpeg_kit`, no `tflite`, no Chaquopy/Python in APK (unlike tagger — Vision Engine has no yt-dlp need; keep APK lean).

## Verification (G5)
Moto G DCIM round-trip: grant → preview → consent → rename → manifest → undo; denied-permission path; analyzer zero + host tests green. No host `flutter build` (human builds in Android Studio).

## Reference lineage
Tagger `build.gradle` + `StorageBridge` + permission runtime + signing fallback; Recovery `google-services`-absent-guarded-init + icon pipeline; Atlas `integration_test`-only-on-ask + device-claims-only-from-device.
