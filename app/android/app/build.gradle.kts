plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.visionengine"
    // Pinned 37 (not flutter.compileSdkVersion=36): permission_handler_android
    // requires SDK 37+. Proven device build: AGP 9.1.0 + Gradle 9.3.1 + Moto G
    // debug APK, 2026-09-15 (D2 run). Revisit only in a dedicated toolchain
    // session per ADR-002.
    compileSdk = 37
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.visionengine"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        // Vision Engine floor: minSdk 26 (scoped-storage + SAF rename path, BP-05).
        minSdk = 26
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        ndk {
            // 64-bit only: drops ~30% APK bloat (tagger pattern, BP-05).
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

dependencies {
    // 2B extraction path (ADR-003): platform-canonical EXIF reader —
    // JPEG/PNG/WebP/HEIC/DNG reads. Pinned 1.4.1 (verified Jetpack release
    // 2025-04-23; needs compileSdk 34+, we pin 37; minSdk floor satisfied
    // by our minSdk 26). No Dart EXIF package by design (single path).
    implementation("androidx.exifinterface:exifinterface:1.4.1")
}
