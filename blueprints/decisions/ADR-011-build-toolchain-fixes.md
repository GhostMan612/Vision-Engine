# ADR-011 — Android build fixes for new native deps (2026-11-27)

## Context
Adding on-device TFLite and MP4 export exposed three build blockers on the
frozen Flutter 3.47 / AGP 9.1 / Gradle 9.3.1 toolchain:

1. `ffmpeg_kit_flutter 6.0.3` pins `com.arthenica:ffmpeg-kit-https:6.0-2`.
   The Maven Central index lists it, but `repo.maven.apache.org` returns 404
   for the pinned path (verified 2026-11-27); Google's Maven Central mirror
   serves the exact POM/AAR.
2. `ffmpeg_kit_flutter 6.0.3` still uses Flutter's removed v1 embedding
   (`PluginRegistry.Registrar`) and fails to compile on Flutter 3.47.
3. `tflite_flutter 0.12.1` compiles Java at 11 while AGP 9's built-in Kotlin
   compiles at 21 -> "Inconsistent JVM-target compatibility".

## Decision
- Add Google's Maven Central mirror
  (`https://maven-central.storage-download.googleapis.com/maven2/`) to
  `app/android/build.gradle.kts` for the pinned artifact; do NOT force a
  different ffmpeg build.
- Replace `ffmpeg_kit_flutter` with the maintained
  `ffmpeg_kit_flutter_new 5.0.0` fork (v2 embedding, same `FFmpegKit.execute`
  API). Import path changes only in `slideshow_service.dart`.
- Align every Android library subproject to Java 17 + Kotlin `JVM_17` in
  `app/android/build.gradle.kts`; the app already pinned 17. Do NOT use
  `options.release` for JavaCompile — AGP rejects it (bootclasspath), use
  `sourceCompatibility`/`targetCompatibility` instead.
- License note: the fork bundles `ffmpeg-kit-full-gpl`, so the APK is
  GPL-3.0 affected (already flagged in ADR-009; no change to that conclusion).

## Consequences
`flutter assembleDebug` resolves and compiles all plugins. Host tests and
device integration suites use the same dependency graph as production.
Revisit if upstream publishes a compatible artifact on Maven Central proper.
