# ADR-002 — Dependency ceiling + justification gate

> Status: ACCEPTED 2026-09-14. v1 set resolved: rename_core (path),
> path_provider, shared_preferences, permission_handler, file_picker 12.3.0.

## Context
Media deps (`ffmpeg_kit`, `video_player`, `permission_handler`, `file_picker`, `tflite_flutter`) carry size, GPL, and win32-ceiling risks proven in tagger sessions.

## Decision
- v1 allowed without fresh ADR: `path`, `path_provider`, `shared_preferences`, `permission_handler`, `file_picker`, `intl`, `exif`, `image`, `mime`, `video_player` (preview only), `share_plus`, `flutter_lints`, `flutter_test`.
- Requires fresh ADR + operator sign-off: `ffmpeg_kit_flutter` (BP-04 only), `tflite_flutter` (BP-04 only), any Chaquopy/Python-in-APK, any cloud SDK, any `--major-versions` bump.
- `flutter pub upgrade --major-versions` is banned without a dedicated session.

## Consequences
Rename/viewer/simple-edit stay lean and shippable while advanced lane can't bloat the APK by accident.
