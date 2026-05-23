plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mediaeditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediaeditor"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)

    // Feature Modules
    implementation(project(":feature:audio-editor"))
    implementation(project(":feature:video-editor"))
    implementation(project(":feature:converter"))
    implementation(project(":feature:batch"))
    implementation(project(":feature:settings"))

    // Core Modules
    implementation(project(":core:ffmpeg"))
    implementation(project(":core:transformer"))
    implementation(project(":core:waveform"))
    implementation(project(":core:router"))
    implementation(project(":core:storage"))
    implementation(project(":core:queue"))
    implementation(project(":core:ui"))
}