allprojects {
    repositories {
        google()
        mavenCentral()
        // ffmpeg_kit_flutter 6.0.3 pins ffmpeg-kit-https:6.0-2. The index
        // lists it under Maven Central, but repo.maven.apache.org currently
        // serves 404 for that path; Google's Maven Central mirror serves the
        // exact pinned POM/AAR (verified 2026-11-27). Keep the plugin's
        // original version instead of forcing a different build.
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        }
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}

subprojects {
    // Flutter plugins (tflite_flutter) pin Java 11 in their own
    // android.compileOptions while AGP 9's built-in Kotlin compiles at 17.
    // Override BOTH at afterEvaluate so our values win after the plugin's
    // build.gradle has configured the tasks (2026-11-27, ADR-011).
    afterEvaluate {
        extensions.findByType<com.android.build.gradle.LibraryExtension>()
            ?.compileOptions
            ?.apply {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
            .configureEach {
                compilerOptions.jvmTarget.set(
                    org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17,
                )
            }
        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }
    }
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
