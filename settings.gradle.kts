pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MediaEditor"
include(":app")
include(":core:ffmpeg")
include(":core:transformer")
include(":core:waveform")
include(":core:router")
include(":core:storage")
include(":core:queue")
include(":core:ui")
include(":feature:audio-editor")
include(":feature:video-editor")
include(":feature:converter")
include(":feature:batch")
include(":feature:settings")