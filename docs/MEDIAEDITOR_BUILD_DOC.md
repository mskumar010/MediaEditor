# FOSS Android Media Editor — Full Production Build Document
> From zero to MVP to final. For vibe-coding / AI-assisted development.
> GPL v3 · Kotlin · Jetpack Compose · Material 3 · Offline · F-Droid ready

**CRITICAL RULE:** Do NOT use any deprecated libraries, APIs, or components. Always check for deprecation status before using them in the codebase.

**CRITICAL RULE 2:** Ensure strict compatibility for all Gradle packages, libraries, and components downloaded or imported. They must be stable versions that do not cause build conflicts or future compatibility issues.

---

## TABLE OF CONTENTS

1. Project Identity
2. Tech Stack (locked)
3. Module Structure
4. Dependency Catalog (exact)
5. Permissions & Manifest
6. Data Models
7. Core Engines (Router, FFmpeg, Transformer, Waveform)
8. Phase 0 — Project Skeleton
9. Phase 1 — MVP (Audio Editor)
10. Phase 2 — MVP (Video Editor)
11. Phase 3 — Converter + Batch
12. Phase 4 — Polish + GPU Filters + Settings
13. Phase 5 — F-Droid Release
14. UI/UX Spec (screens, flows, components)
15. FFmpeg Command Reference
16. Testing Strategy
17. Known Pitfalls & Fixes

---

## 1. PROJECT IDENTITY

```
App name:       [YOU PICK — placeholder: "Media Editor"]
Package:        com.mediaeditor
Min SDK:        23 (Android 6.0)
Target SDK:     35 (Android 15)
Compile SDK:    35
Language:       Kotlin 2.x
UI:             Jetpack Compose + Material 3
License:        GPL v3
Repo:           github.com/yourname/mediaeditor
Distribution:   F-Droid + GitHub Releases
Internet:       NONE — zero network permission
```

---

## 2. TECH STACK (LOCKED)

### Processing Engines
| Engine | Purpose | Source |
|--------|---------|--------|
| Media3 Transformer | Primary video edit (trim, crop, rotate, speed, merge, mute, OpenGL effects) | `androidx.media3` |
| Media3 ExoPlayer | Playback preview (audio + video) | `androidx.media3` |
| FFmpegX-Android | All format conversion, audio filters, fade, MKV/AVI/FLAC/MP3/OGG output, frame extract, reverse | `github.com/mzgs/FFmpegX-Android` |
| Amplituda | PCM waveform data extraction from audio files | `lincollincol/Amplituda` |
| WaveformSeekBar | Compose waveform UI component | `massoudss/waveformSeekBar` |
| Mp4Composer | GPU shader video filters on export | `MasayukiSuda/Mp4Composer-android` |
| GPUVideo | GPU shader video filters on ExoPlayer preview | `MasayukiSuda/GPUVideo-android` |

### App Stack
| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 + Material You dynamic color |
| Architecture | Clean Architecture (data/domain/presentation) |
| DI | Hilt |
| Async | Coroutines + StateFlow + Flow |
| Navigation | Compose Navigation (typed routes) |
| Background jobs | WorkManager |
| Storage | Scoped Storage + SAF + MediaStore |
| Build | Kotlin DSL + Version Catalog (libs.versions.toml) |

### Engine Routing Rule
```
Operation needs format conversion?          → FFmpeg
Operation needs audio filter (fade/norm)?   → FFmpeg
Output format is MKV/AVI/FLV/MP3/FLAC/OGG? → FFmpeg
Everything else (trim/crop/rotate/speed)?   → Media3 Transformer
Lossless cut (stream copy)?                 → FFmpeg (-c copy)
Waveform data?                              → Amplituda
Preview playback?                           → ExoPlayer
```

---

## 3. MODULE STRUCTURE

```
root/
├── app/                          ← shell, navigation host, DI setup
├── core/
│   ├── ffmpeg/                   ← FFmpegX wrapper + command builder DSL
│   ├── transformer/              ← Media3 Transformer pipeline wrapper
│   ├── waveform/                 ← Amplituda + WaveformSeekBar integration
│   ├── router/                   ← ProcessingRouter: picks engine per operation
│   ├── storage/                  ← SAF, MediaStore, scoped storage abstraction
│   ├── queue/                    ← WorkManager job queue
│   └── ui/                       ← shared Compose components, theme, tokens
├── feature/
│   ├── audio-editor/             ← audio waveform editor screen
│   ├── video-editor/             ← video trimmer + editor screen
│   ├── converter/                ← format conversion screen
│   ├── batch/                    ← batch operations queue screen
│   └── settings/                 ← app settings screen
└── build-logic/                  ← convention plugins
```

Each feature module follows:
```
feature/audio-editor/
├── data/
│   ├── repository/AudioEditorRepositoryImpl.kt
│   └── datasource/AudioDataSource.kt
├── domain/
│   ├── model/AudioProject.kt
│   ├── usecase/TrimAudioUseCase.kt
│   ├── usecase/FadeAudioUseCase.kt
│   └── repository/AudioEditorRepository.kt (interface)
└── presentation/
    ├── AudioEditorScreen.kt
    ├── AudioEditorViewModel.kt
    └── components/
        ├── WaveformEditor.kt
        ├── TrimHandles.kt
        └── AudioControlBar.kt
```

---

## 4. DEPENDENCY CATALOG

### libs.versions.toml
```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.0"
compose-bom = "2024.11.00"
media3 = "1.6.0"
hilt = "2.52"
hilt-compose = "1.2.0"
navigation-compose = "2.8.4"
workmanager = "2.9.1"
coroutines = "1.9.0"
amplituda = "2.2.2"
waveform-seekbar = "2.0.0"
mp4composer = "0.4.2"
gpuvideo = "1.1.0"

[libraries]
# Compose BOM
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }

# Media3
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
media3-transformer = { group = "androidx.media3", name = "media3-transformer", version.ref = "media3" }
media3-effect = { group = "androidx.media3", name = "media3-effect", version.ref = "media3" }
media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-compose" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# WorkManager
workmanager = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hilt-compose" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Waveform
amplituda = { group = "com.github.lincollincol", name = "amplituda", version.ref = "amplituda" }
waveform-seekbar = { group = "com.github.massoudss", name = "waveformSeekBar", version.ref = "waveform-seekbar" }

# GPU Video / Mp4Composer
mp4composer = { group = "com.github.MasayukiSuda", name = "Mp4Composer-android", version.ref = "mp4composer" }
gpuvideo = { group = "com.github.MasayukiSuda", name = "GPUVideo-android", version.ref = "gpuvideo" }

# FFmpeg — pulled from JitPack (mzgs/FFmpegX-Android)
# Add JitPack to settings.gradle.kts: maven { url = uri("https://jitpack.io") }
ffmpegx = { group = "com.github.mzgs", name = "FFmpegX-Android", version = "1.0.0" }

# Misc
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.7" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version = "1.1.1" }
documentfile = { group = "androidx.documentfile", name = "documentfile", version = "1.0.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### settings.gradle.kts repositories
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

---

## 5. PERMISSIONS & MANIFEST

```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Storage — Android 6-9 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <!-- Storage — Android 10-12 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- Storage — Android 13+ granular -->
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- WorkManager notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Foreground service for long exports -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <!-- NO INTERNET PERMISSION — intentional -->

    <application
        android:name=".App"
        android:allowBackup="false"
        android:enableOnBackInvokedCallback="true"
        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">

        <!-- MainActivity -->
        <activity
            android:name=".MainActivity"
            android:windowSoftInputMode="adjustResize"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Accept audio/video files from file managers -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <action android:name="android.intent.action.EDIT" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="audio/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <action android:name="android.intent.action.EDIT" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="video/*" />
            </intent-filter>
        </activity>

        <!-- Export foreground service -->
        <service
            android:name=".core.queue.ExportForegroundService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />

    </application>
</manifest>
```

---

## 6. DATA MODELS

### core domain models (shared across features)

```kotlin
// core/router/src/main/kotlin/com/mediaeditor/core/router/ProcessingEngine.kt
enum class ProcessingEngine { TRANSFORMER, FFMPEG }

// core/router/src/main/kotlin/com/mediaeditor/core/router/MediaOperation.kt
sealed class MediaOperation {
    data class TrimAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val startMs: Long,
        val endMs: Long,
        val lossless: Boolean = true,
        val fadeInMs: Long = 0,
        val fadeOutMs: Long = 0,
        val outputFormat: AudioFormat = AudioFormat.AAC
    ) : MediaOperation()

    data class TrimVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val startMs: Long,
        val endMs: Long,
        val lossless: Boolean = true,
        val fadeInMs: Long = 0,
        val fadeOutMs: Long = 0
    ) : MediaOperation()

    data class CropVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val left: Float, val top: Float,
        val right: Float, val bottom: Float // normalized 0f–1f
    ) : MediaOperation()

    data class ConvertAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: AudioFormat,
        val bitrate: Int = 128, // kbps
        val sampleRate: Int = 44100
    ) : MediaOperation()

    data class ConvertVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: VideoFormat,
        val codec: VideoCodec = VideoCodec.H264,
        val resolution: Resolution? = null,
        val bitrate: Int? = null
    ) : MediaOperation()

    data class ExtractAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: AudioFormat = AudioFormat.MP3
    ) : MediaOperation()

    data class MergeAudio(
        val inputUris: List<Uri>,
        val outputUri: Uri,
        val outputFormat: AudioFormat = AudioFormat.AAC
    ) : MediaOperation()

    data class MergeVideo(
        val inputUris: List<Uri>,
        val outputUri: Uri
    ) : MediaOperation()

    data class ChangeSpeed(
        val inputUri: Uri,
        val outputUri: Uri,
        val speed: Float // 0.25f to 4.0f
    ) : MediaOperation()

    data class ExtractFrame(
        val inputUri: Uri,
        val outputUri: Uri,
        val timestampMs: Long
    ) : MediaOperation()

    data class BatchOperation(
        val operations: List<MediaOperation>
    ) : MediaOperation()
}

enum class AudioFormat(val extension: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    AAC("m4a", "audio/mp4"),
    FLAC("flac", "audio/flac"),
    OGG("ogg", "audio/ogg"),
    OPUS("opus", "audio/opus"),
    WAV("wav", "audio/wav")
}

enum class VideoFormat(val extension: String) {
    MP4("mp4"), MKV("mkv"), WEBM("webm"),
    AVI("avi"), MOV("mov"), FLV("flv")
}

enum class VideoCodec { H264, H265, VP8, VP9 }

data class Resolution(val width: Int, val height: Int) {
    companion object {
        val P2160 = Resolution(3840, 2160)
        val P1080 = Resolution(1920, 1080)
        val P720  = Resolution(1280, 720)
        val P480  = Resolution(854, 480)
        val P360  = Resolution(640, 360)
    }
}

// Operation result
sealed class OperationResult {
    data class Success(val outputUri: Uri, val durationMs: Long) : OperationResult()
    data class Failure(val error: String, val cause: Throwable? = null) : OperationResult()
    data class Progress(val percent: Int, val message: String = "") : OperationResult()
}
```

### Audio project state
```kotlin
data class AudioProject(
    val sourceUri: Uri,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val volume: Float = 1.0f,
    val outputFormat: AudioFormat = AudioFormat.AAC,
    val playbackPositionMs: Long = 0
)

data class VideoProject(
    val sourceUri: Uri,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val cropRect: CropRect = CropRect.FULL,
    val rotation: Int = 0, // 0, 90, 180, 270
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val speed: Float = 1.0f,
    val muteAudio: Boolean = false,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val outputFormat: VideoFormat = VideoFormat.MP4,
    val resolution: Resolution? = null
)

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    companion object { val FULL = CropRect() }
}
```

---

## 7. CORE ENGINES

### 7.1 ProcessingRouter
```kotlin
// core/router/src/main/kotlin/.../ProcessingRouter.kt
@Singleton
class ProcessingRouter @Inject constructor(
    private val transformerEngine: TransformerEngine,
    private val ffmpegEngine: FFmpegEngine
) {
    suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit = {}
    ): OperationResult {
        val engine = route(operation)
        return when (engine) {
            ProcessingEngine.TRANSFORMER -> transformerEngine.execute(operation, onProgress)
            ProcessingEngine.FFMPEG -> ffmpegEngine.execute(operation, onProgress)
        }
    }

    private fun route(operation: MediaOperation): ProcessingEngine = when (operation) {
        // Always FFmpeg
        is MediaOperation.ConvertAudio   -> ProcessingEngine.FFMPEG
        is MediaOperation.ConvertVideo   -> ProcessingEngine.FFMPEG
        is MediaOperation.ExtractAudio   -> ProcessingEngine.FFMPEG
        is MediaOperation.MergeAudio     -> ProcessingEngine.FFMPEG
        is MediaOperation.ExtractFrame   -> ProcessingEngine.FFMPEG

        // FFmpeg if fade needed or non-AAC output, else Transformer
        is MediaOperation.TrimAudio -> when {
            operation.fadeInMs > 0 || operation.fadeOutMs > 0 -> ProcessingEngine.FFMPEG
            operation.outputFormat != AudioFormat.AAC         -> ProcessingEngine.FFMPEG
            else                                              -> ProcessingEngine.FFMPEG // audio always FFmpeg
        }

        // Transformer for speed-trim-crop-rotate, FFmpeg for exotic output formats
        is MediaOperation.TrimVideo -> when {
            operation.fadeInMs > 0 || operation.fadeOutMs > 0 -> ProcessingEngine.FFMPEG
            operation.lossless                                 -> ProcessingEngine.FFMPEG
            else                                               -> ProcessingEngine.TRANSFORMER
        }

        is MediaOperation.CropVideo    -> ProcessingEngine.TRANSFORMER
        is MediaOperation.ChangeSpeed  -> ProcessingEngine.TRANSFORMER
        is MediaOperation.MergeVideo   -> ProcessingEngine.TRANSFORMER

        is MediaOperation.BatchOperation -> ProcessingEngine.FFMPEG // batches go through FFmpeg queue
    }
}
```

### 7.2 FFmpegEngine
```kotlin
// core/ffmpeg/src/main/kotlin/.../FFmpegEngine.kt
@Singleton
class FFmpegEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Build FFmpeg command for each operation type
    suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = withContext(Dispatchers.IO) {
        val cmd = buildCommand(operation)
        runFFmpeg(cmd, onProgress)
    }

    private fun buildCommand(operation: MediaOperation): String = when (operation) {

        is MediaOperation.TrimAudio -> buildTrimAudioCmd(operation)
        is MediaOperation.ConvertAudio -> buildConvertAudioCmd(operation)
        is MediaOperation.ExtractAudio -> buildExtractAudioCmd(operation)
        is MediaOperation.MergeAudio -> buildMergeAudioCmd(operation)
        is MediaOperation.TrimVideo -> buildTrimVideoCmd(operation)
        is MediaOperation.ConvertVideo -> buildConvertVideoCmd(operation)
        is MediaOperation.ExtractFrame -> buildExtractFrameCmd(operation)
        else -> throw UnsupportedOperationException("Operation not handled by FFmpeg: $operation")
    }

    // See Section 15 for all FFmpeg command strings
    private fun buildTrimAudioCmd(op: MediaOperation.TrimAudio): String {
        val startSec = op.startMs / 1000.0
        val durationSec = (op.endMs - op.startMs) / 1000.0
        val fadeIn = if (op.fadeInMs > 0) ",afade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val fadeOut = if (op.fadeOutMs > 0) ",afade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val hasFilters = fadeIn.isNotEmpty() || fadeOut.isNotEmpty()
        val audioFilter = if (hasFilters) "-af \"aresample=async=1$fadeIn$fadeOut\"" else ""
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -q:a 2"
            AudioFormat.AAC  -> "-c:a aac -b:a 192k"
            AudioFormat.FLAC -> "-c:a flac"
            AudioFormat.OGG  -> "-c:a libvorbis -q:a 5"
            AudioFormat.OPUS -> "-c:a libopus -b:a 128k"
            AudioFormat.WAV  -> "-c:a pcm_s16le"
        }
        return "-ss $startSec -i ${op.inputUri.toPath()} -t $durationSec $audioFilter $codec ${op.outputUri.toPath()}"
    }

    private fun buildTrimVideoCmd(op: MediaOperation.TrimVideo): String {
        val startSec = op.startMs / 1000.0
        val durationSec = (op.endMs - op.startMs) / 1000.0
        if (op.lossless) {
            return "-ss $startSec -i ${op.inputUri.toPath()} -t $durationSec -c copy -avoid_negative_ts make_zero ${op.outputUri.toPath()}"
        }
        val fadeIn = if (op.fadeInMs > 0) ",fade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val fadeOut = if (op.fadeOutMs > 0) ",fade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val vf = if (fadeIn.isNotEmpty() || fadeOut.isNotEmpty()) "-vf \"null$fadeIn$fadeOut\"" else ""
        val afadeIn = if (op.fadeInMs > 0) ",afade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val afadeOut = if (op.fadeOutMs > 0) ",afade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val af = if (afadeIn.isNotEmpty() || afadeOut.isNotEmpty()) "-af \"aresample=async=1$afadeIn$afadeOut\"" else ""
        return "-ss $startSec -i ${op.inputUri.toPath()} -t $durationSec $vf $af -c:v libx264 -preset fast -crf 22 -c:a aac ${op.outputUri.toPath()}"
    }

    private fun buildConvertAudioCmd(op: MediaOperation.ConvertAudio): String {
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -b:a ${op.bitrate}k"
            AudioFormat.AAC  -> "-c:a aac -b:a ${op.bitrate}k"
            AudioFormat.FLAC -> "-c:a flac"
            AudioFormat.OGG  -> "-c:a libvorbis -q:a 5"
            AudioFormat.OPUS -> "-c:a libopus -b:a ${op.bitrate}k"
            AudioFormat.WAV  -> "-c:a pcm_s16le"
        }
        return "-i ${op.inputUri.toPath()} -ar ${op.sampleRate} $codec ${op.outputUri.toPath()}"
    }

    private fun buildExtractAudioCmd(op: MediaOperation.ExtractAudio): String {
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -q:a 2"
            AudioFormat.AAC  -> "-c:a aac -b:a 192k"
            AudioFormat.FLAC -> "-c:a flac"
            else             -> "-c:a aac -b:a 192k"
        }
        return "-i ${op.inputUri.toPath()} -vn $codec ${op.outputUri.toPath()}"
    }

    private fun buildMergeAudioCmd(op: MediaOperation.MergeAudio): String {
        val inputs = op.inputUris.joinToString(" ") { "-i ${it.toPath()}" }
        val filter = (0 until op.inputUris.size).joinToString("") { "[$it:a]" }
        val codec = when (op.outputFormat) {
            AudioFormat.MP3 -> "-c:a libmp3lame -q:a 2"
            AudioFormat.FLAC -> "-c:a flac"
            else -> "-c:a aac -b:a 192k"
        }
        return "$inputs -filter_complex \"${filter}concat=n=${op.inputUris.size}:v=0:a=1[a]\" -map \"[a]\" $codec ${op.outputUri.toPath()}"
    }

    private fun buildConvertVideoCmd(op: MediaOperation.ConvertVideo): String {
        val vcodec = when (op.codec) {
            VideoCodec.H264 -> "-c:v libx264 -preset fast -crf 22"
            VideoCodec.H265 -> "-c:v libx265 -preset fast -crf 24"
            VideoCodec.VP8  -> "-c:v libvpx -b:v ${op.bitrate ?: 1500}k"
            VideoCodec.VP9  -> "-c:v libvpx-vp9 -b:v ${op.bitrate ?: 1500}k"
        }
        val scale = op.resolution?.let { "-vf scale=${it.width}:${it.height}" } ?: ""
        return "-i ${op.inputUri.toPath()} $scale $vcodec -c:a aac -b:a 128k ${op.outputUri.toPath()}"
    }

    private fun buildExtractFrameCmd(op: MediaOperation.ExtractFrame): String {
        val sec = op.timestampMs / 1000.0
        return "-ss $sec -i ${op.inputUri.toPath()} -frames:v 1 -q:v 2 ${op.outputUri.toPath()}"
    }

    private suspend fun runFFmpeg(
        command: String,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult {
        // Using mzgs/FFmpegX-Android Kotlin API
        // Actual API call depends on library version — adapt as needed
        return try {
            // FFmpegX.execute(command) with progress callback
            // This is a placeholder — replace with actual FFmpegX-Android API call
            OperationResult.Success(Uri.EMPTY, 0L)
        } catch (e: Exception) {
            OperationResult.Failure(e.message ?: "FFmpeg failed", e)
        }
    }

    private fun Uri.toPath(): String = this.path ?: toString()
}
```

### 7.3 TransformerEngine
```kotlin
// core/transformer/src/main/kotlin/.../TransformerEngine.kt
@Singleton
class TransformerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = withContext(Dispatchers.Main) { // Transformer runs on main thread
        when (operation) {
            is MediaOperation.TrimVideo   -> trimVideo(operation, onProgress)
            is MediaOperation.CropVideo   -> cropVideo(operation, onProgress)
            is MediaOperation.ChangeSpeed -> changeSpeed(operation, onProgress)
            is MediaOperation.MergeVideo  -> mergeVideo(operation, onProgress)
            else -> OperationResult.Failure("Operation not supported by Transformer")
        }
    }

    private suspend fun trimVideo(
        op: MediaOperation.TrimVideo,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = suspendCancellableCoroutine { cont ->
        val mediaItem = MediaItem.Builder()
            .setUri(op.inputUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(op.startMs)
                    .setEndPositionMs(op.endMs)
                    .build()
            )
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    cont.resume(OperationResult.Success(op.outputUri, exportResult.durationMs))
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    cont.resume(OperationResult.Failure(exportException.message ?: "Transformer error", exportException))
                }
            })
            .build()

        transformer.start(
            Composition.Builder(EditedMediaItemSequence(EditedMediaItem.Builder(mediaItem).build())).build(),
            op.outputUri.path!!
        )

        cont.invokeOnCancellation { transformer.cancel() }
    }

    private suspend fun cropVideo(
        op: MediaOperation.CropVideo,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = suspendCancellableCoroutine { cont ->
        val cropEffect = Crop(op.left, op.right, op.bottom, op.top) // Media3 Crop effect
        val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(op.inputUri))
            .setEffects(Effects(emptyList(), listOf(cropEffect)))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    cont.resume(OperationResult.Success(op.outputUri, exportResult.durationMs))
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    cont.resume(OperationResult.Failure(exportException.message ?: "Crop failed", exportException))
                }
            })
            .build()

        transformer.start(
            Composition.Builder(EditedMediaItemSequence(editedItem)).build(),
            op.outputUri.path!!
        )
        cont.invokeOnCancellation { transformer.cancel() }
    }

    private suspend fun changeSpeed(op: MediaOperation.ChangeSpeed, onProgress: (OperationResult.Progress) -> Unit): OperationResult {
        // Media3 Transformer speed via SpeedProvider
        // Implementation uses SonicAudioProcessor for audio + video speed
        return OperationResult.Failure("Speed via Transformer — implement with SonicAudioProcessor")
    }

    private suspend fun mergeVideo(op: MediaOperation.MergeVideo, onProgress: (OperationResult.Progress) -> Unit): OperationResult {
        // Build Composition with multiple EditedMediaItem in sequence
        return OperationResult.Failure("Merge — implement with Composition API")
    }
}
```

### 7.4 StorageManager
```kotlin
// core/storage/src/main/kotlin/.../StorageManager.kt
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Create output file in app-specific directory or user-chosen SAF directory
    fun createOutputFile(
        name: String,
        extension: String,
        parentUri: Uri? = null // null = app cache dir
    ): Uri {
        return if (parentUri != null) {
            // SAF — user picked a folder
            val docTree = DocumentFile.fromTreeUri(context, parentUri)!!
            val file = docTree.createFile("*/*", "$name.$extension")!!
            file.uri
        } else {
            // App cache — always writable
            val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val file = File(dir, "$name.$extension")
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    // Copy finished export to MediaStore so it appears in gallery
    fun publishToMediaStore(sourceUri: Uri, name: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    if (mimeType.startsWith("audio")) Environment.DIRECTORY_MUSIC
                    else Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (mimeType.startsWith("audio"))
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val outputUri = context.contentResolver.insert(collection, values) ?: return null

        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(outputUri, values, null, null)
        }
        return outputUri
    }

    // Get real file path from URI for FFmpeg (FFmpeg needs actual path)
    fun getRealPath(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        if (uri.scheme == "content") {
            // Try MediaStore first
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    val path = cursor.getString(idx)
                    if (!path.isNullOrBlank()) return path
                }
            }
            // Fallback: copy to cache and return that path
            return copyToCacheAndGetPath(uri)
        }
        return null
    }

    private fun copyToCacheAndGetPath(uri: Uri): String? {
        val fileName = getFileName(uri) ?: "temp_${System.currentTimeMillis()}"
        val cacheFile = File(context.cacheDir, "input/$fileName").also {
            it.parentFile?.mkdirs()
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { out -> input.copyTo(out) }
            }
            cacheFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }
}
```

---

## 8. PHASE 0 — PROJECT SKELETON

**Goal:** Compilable, runnable shell. Navigation working. Hilt working. Theme working. No actual editing yet.

**Duration estimate:** 1 day

### 0.1 Create project
- New Android project, Kotlin DSL, min SDK 23, Compose enabled
- Set up `libs.versions.toml` (copy from Section 4)
- Add all modules listed in Section 3 as empty Android library modules
- Set up Hilt: `@HiltAndroidApp` on `App`, `@AndroidEntryPoint` on `MainActivity`

### 0.2 Theme
```kotlin
// core/ui/src/main/kotlin/.../theme/Theme.kt
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```

### 0.3 Navigation
```kotlin
// Destinations
sealed class Screen(val route: String) {
    object Home          : Screen("home")
    object AudioEditor   : Screen("audio_editor?uri={uri}") {
        fun createRoute(uri: String) = "audio_editor?uri=$uri"
    }
    object VideoEditor   : Screen("video_editor?uri={uri}") {
        fun createRoute(uri: String) = "video_editor?uri=$uri"
    }
    object Converter     : Screen("converter")
    object Batch         : Screen("batch")
    object Settings      : Screen("settings")
}

// NavHost in MainActivity
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(
            Screen.AudioEditor.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val uri = backStack.arguments?.getString("uri")?.let { Uri.parse(it) }
            AudioEditorScreen(uri = uri, navController = navController)
        }
        composable(
            Screen.VideoEditor.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val uri = backStack.arguments?.getString("uri")?.let { Uri.parse(it) }
            VideoEditorScreen(uri = uri, navController = navController)
        }
        composable(Screen.Converter.route) { ConverterScreen(navController) }
        composable(Screen.Batch.route) { BatchScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
```

### 0.4 Home Screen
```kotlin
// Three main cards: Audio Editor, Video Editor, Converter + Batch
// Each card opens SAF file picker then navigates to correct screen
@Composable
fun HomeScreen(navController: NavHostController) {
    // Three cards: Audio, Video, Convert
    // SAF picker launcher for each
    // Bottom nav or top nav bar
    // Recent files list (from DataStore)
}
```

### 0.5 Permission handling
```kotlin
// core/storage — PermissionManager.kt
// Handles READ_MEDIA_AUDIO, READ_MEDIA_VIDEO (API 33+)
// vs READ_EXTERNAL_STORAGE (API < 33)
// POST_NOTIFICATIONS (API 33+)
// Show rationale if denied
```

**Phase 0 done when:** App launches, shows home, can pick a file via SAF, navigates to a stub screen. No crash.

---

## 9. PHASE 1 — MVP: AUDIO EDITOR

**Goal:** Full working audio editor. Waveform, trim, fade, format export.

**Duration estimate:** 5–7 days

### Features in this phase
- Pick audio file
- Display scrollable waveform with zoom (3 levels to start)
- Drag handles for start/end trim points
- Playback with position indicator on waveform
- Fade in / fade out duration slider
- Volume slider
- Output format picker (MP3, AAC, FLAC, OGG, WAV)
- Lossless cut toggle
- Export button → progress → done → share / open
- Set as ringtone/notification/alarm

### 9.1 Waveform Component
```kotlin
// core/waveform/src/main/kotlin/.../WaveformEditor.kt
@Composable
fun WaveformEditor(
    uri: Uri,
    trimStart: Long,         // ms
    trimEnd: Long,           // ms
    totalDuration: Long,     // ms
    playbackPosition: Long,  // ms
    onTrimStartChange: (Long) -> Unit,
    onTrimEndChange: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Use Amplituda to extract amplitude data
    // 2. Feed into WaveformSeekBar
    // 3. Overlay trim handles as draggable boxes
    // 4. Overlay playback position as vertical line
    // 5. Zoom: pinch gesture or zoom buttons

    val context = LocalContext.current
    var amplitudes by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            // Amplituda extraction
            Amplituda(context)
                .processAudio(uri)
                .get({ data ->
                    amplitudes = data.amplitudesAsList()
                }, { exception ->
                    // handle error
                })
        }
    }

    // Render WaveformSeekBar + custom trim handle overlay
    Box(modifier = modifier) {
        if (amplitudes.isNotEmpty()) {
            // WaveformSeekBar composable
            // Custom Canvas overlay for trim handles + playhead
        }
    }
}
```

### 9.2 Audio Editor ViewModel
```kotlin
@HiltViewModel
class AudioEditorViewModel @Inject constructor(
    private val router: ProcessingRouter,
    private val storageManager: StorageManager,
    private val player: ExoPlayer
) : ViewModel() {

    private val _project = MutableStateFlow<AudioProject?>(null)
    val project: StateFlow<AudioProject?> = _project.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    val playbackPosition: StateFlow<Long> = // from ExoPlayer position polling

    fun loadFile(uri: Uri) { /* load duration, create AudioProject */ }
    fun updateTrimStart(ms: Long) { /* update project */ }
    fun updateTrimEnd(ms: Long) { /* update project */ }
    fun updateFadeIn(ms: Long) { /* update project */ }
    fun updateFadeOut(ms: Long) { /* update project */ }
    fun updateVolume(v: Float) { /* update project */ }
    fun updateOutputFormat(format: AudioFormat) { /* update project */ }
    fun toggleLossless() { /* update project */ }

    fun play() { player.play() }
    fun pause() { player.pause() }
    fun seekTo(ms: Long) { player.seekTo(ms) }

    fun export() {
        viewModelScope.launch {
            _exportState.value = ExportState.Processing(0)
            val project = _project.value ?: return@launch
            val outputUri = storageManager.createOutputFile(
                "audio_export_${System.currentTimeMillis()}",
                project.outputFormat.extension
            )
            val operation = MediaOperation.TrimAudio(
                inputUri = project.sourceUri,
                outputUri = outputUri,
                startMs = project.trimStartMs,
                endMs = project.trimEndMs,
                lossless = false, // audio always goes through FFmpeg
                fadeInMs = project.fadeInMs,
                fadeOutMs = project.fadeOutMs,
                outputFormat = project.outputFormat
            )
            val result = router.execute(operation) { progress ->
                _exportState.value = ExportState.Processing(progress.percent)
            }
            _exportState.value = when (result) {
                is OperationResult.Success -> ExportState.Done(result.outputUri)
                is OperationResult.Failure -> ExportState.Error(result.error)
                else -> ExportState.Idle
            }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

sealed class ExportState {
    object Idle : ExportState()
    data class Processing(val percent: Int) : ExportState()
    data class Done(val outputUri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}
```

### 9.3 Audio Editor Screen layout
```
┌─────────────────────────────────────┐
│  Top bar: file name + duration      │
├─────────────────────────────────────┤
│                                     │
│  WAVEFORM (scrollable, zoomable)    │
│  ◄───[trimStart]═══[trimEnd]───►   │
│             │ playhead              │
│                                     │
├─────────────────────────────────────┤
│  ⏮  ⏪  ⏯  ⏩  ⏭               │
│  Position: 00:12 / 03:45           │
├─────────────────────────────────────┤
│  Trim:  [00:05] ──────── [02:30]   │
│  Fade in:  ════ 0.5s               │
│  Fade out: ════ 0.5s               │
│  Volume:   ══════════ 100%         │
├─────────────────────────────────────┤
│  Format: [MP3] [AAC] [FLAC] [OGG]  │
│  Lossless cut: [ toggle ]           │
├─────────────────────────────────────┤
│  [  EXPORT  ]    [Set as Ringtone] │
└─────────────────────────────────────┘
```

**Phase 1 done when:** Can trim any audio file, add fade, pick format, export, and file plays correctly.

---

## 10. PHASE 2 — MVP: VIDEO EDITOR

**Goal:** Full working video editor. Trim with thumbnail strip, crop, rotate, fade, export.

**Duration estimate:** 7–10 days

### Features in this phase
- Pick video file
- Thumbnail strip timeline for trim
- ExoPlayer preview with frame-accurate seek
- Trim with drag handles on thumbnail strip
- Crop with interactive overlay (drag corners)
- Rotate / flip buttons
- Speed picker (0.25x, 0.5x, 1x, 1.5x, 2x, 4x)
- Mute audio toggle
- Replace audio (pick audio file)
- Fade in / fade out sliders (video + audio)
- Lossless cut toggle
- Output format + resolution picker
- Export with progress
- Extract frame at current position

### 10.1 Thumbnail Strip
```kotlin
// feature/video-editor/presentation/components/ThumbnailStrip.kt
@Composable
fun ThumbnailStrip(
    videoUri: Uri,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimStartChange: (Long) -> Unit,
    onTrimEndChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // MediaMetadataRetriever to extract frames
    // LazyRow of Bitmap thumbnails
    // Drag handles overlay on top
    // Yellow border showing selected region

    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    val retriever = remember(videoUri) { MediaMetadataRetriever().apply { setDataSource(...) } }

    // Extract ~10 frames spread across duration
    LaunchedEffect(videoUri) {
        withContext(Dispatchers.IO) {
            val frameCount = 10
            repeat(frameCount) { i ->
                val timeUs = (durationMs * 1000L / frameCount) * i
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                bmp?.let { thumbnails.add(it) }
            }
        }
    }

    Box(modifier = modifier) {
        LazyRow { items(thumbnails) { bmp -> Image(bmp.asImageBitmap(), null) } }
        // Trim handles drawn as Canvas overlay
        // Left handle at trimStartMs position, right at trimEndMs
    }
}
```

### 10.2 Crop Overlay
```kotlin
// feature/video-editor/presentation/components/CropOverlay.kt
@Composable
fun CropOverlay(
    aspectRatio: AspectRatio,
    cropRect: CropRect,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark overlay with transparent crop window
    // 8 drag handles (corners + edge midpoints)
    // Aspect ratio lock when preset selected
    // Grid lines (rule of thirds) on drag
}

enum class AspectRatio {
    FREE, RATIO_16_9, RATIO_9_16, RATIO_1_1, RATIO_4_3, RATIO_3_4
}
```

### 10.3 Video Editor Screen layout
```
┌─────────────────────────────────────┐
│  Top bar: file name                 │
├─────────────────────────────────────┤
│                                     │
│    VIDEO PREVIEW (ExoPlayer)        │
│    16:9 aspect ratio box            │
│                                     │
├─────────────────────────────────────┤
│  THUMBNAIL STRIP (scrollable)       │
│  ▐██████████████████▌              │
│  ◄[start]        [end]►            │
├─────────────────────────────────────┤
│  ⏮  ⏯  ⏭   00:12 / 03:45        │
├─────────────────────────────────────┤
│  [Trim] [Crop] [Rotate] [Speed]    │
│  [Fade] [Audio] [Filter] [More]    │
├─────────────────────────────────────┤
│  (panel shows selected tool)        │
├─────────────────────────────────────┤
│  Format: [MP4] [MKV]  Res: [1080p] │
│  Lossless: [ toggle ]               │
│  [  EXPORT  ]   [Extract Frame]    │
└─────────────────────────────────────┘
```

### 10.4 Video Editor ViewModel
```kotlin
@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    private val router: ProcessingRouter,
    private val storageManager: StorageManager,
    private val player: ExoPlayer
) : ViewModel() {
    private val _project = MutableStateFlow<VideoProject?>(null)
    val project: StateFlow<VideoProject?> = _project.asStateFlow()
    val exportState: StateFlow<ExportState> // same as audio

    fun loadFile(uri: Uri) { /* probe duration, resolution via MediaMetadataRetriever */ }
    fun updateTrimStart(ms: Long) { _project.update { it?.copy(trimStartMs = ms) } }
    fun updateTrimEnd(ms: Long) { _project.update { it?.copy(trimEndMs = ms) } }
    fun updateCrop(rect: CropRect) { _project.update { it?.copy(cropRect = rect) } }
    fun updateRotation(degrees: Int) { _project.update { it?.copy(rotation = degrees) } }
    fun updateSpeed(speed: Float) { _project.update { it?.copy(speed = speed) } }
    fun toggleMute() { _project.update { it?.copy(muteAudio = !it.muteAudio) } }
    fun updateFadeIn(ms: Long) { _project.update { it?.copy(fadeInMs = ms) } }
    fun updateFadeOut(ms: Long) { _project.update { it?.copy(fadeOutMs = ms) } }

    fun export() {
        viewModelScope.launch {
            val project = _project.value ?: return@launch
            _exportState.value = ExportState.Processing(0)
            // Route through ProcessingRouter
            // Build appropriate MediaOperation based on project state
        }
    }

    fun extractFrame(timestampMs: Long) {
        viewModelScope.launch {
            val project = _project.value ?: return@launch
            val outputUri = storageManager.createOutputFile("frame_$timestampMs", "jpg")
            router.execute(MediaOperation.ExtractFrame(project.sourceUri, outputUri, timestampMs))
        }
    }
}
```

**Phase 2 done when:** Can trim, crop, rotate, and export video. Preview matches output. Lossless trim works on MP4.

---

## 11. PHASE 3 — CONVERTER + BATCH

**Goal:** Format converter screen + batch operations queue.

**Duration estimate:** 4–5 days

### Features in this phase
- Converter screen: pick file → pick output format → pick settings → convert
- Advanced settings panel (bitrate, resolution, codec, sample rate)
- Remember last used settings (DataStore)
- Batch screen: queue of operations, each with status
- Add operations from Converter or editors
- Run all / pause / cancel individual
- Background export via WorkManager

### 11.1 Converter Screen
```
┌─────────────────────────────────────┐
│  CONVERTER                          │
├─────────────────────────────────────┤
│  Input:  [Pick File]                │
│  video.mp4  1920×1080  H.264  45MB │
├─────────────────────────────────────┤
│  OUTPUT FORMAT                      │
│  Video: [MP4] [MKV] [WEBM] [AVI]   │
│  Audio: [MP3] [FLAC] [OGG] [WAV]   │
├─────────────────────────────────────┤
│  SETTINGS (collapsible)             │
│  Codec:      [H.264] [H.265]        │
│  Resolution: [Same] [1080p] [720p]  │
│  Bitrate:    [Auto] [Custom: ___]   │
├─────────────────────────────────────┤
│  Output: /Movies/video_export.mp4   │
│  [Change folder]                    │
├─────────────────────────────────────┤
│  [Add to Batch]   [Convert Now]     │
└─────────────────────────────────────┘
```

### 11.2 Batch Queue Screen
```
┌─────────────────────────────────────┐
│  BATCH QUEUE          [Run All]     │
├─────────────────────────────────────┤
│  ✅ video.mp4 → MKV    Done        │
│  ⏳ audio.flac → MP3   45%         │
│  ⏸  song.ogg → AAC    Queued      │
│  ❌ corrupt.avi        Failed       │
├─────────────────────────────────────┤
│  [+ Add More]    [Clear Completed]  │
└─────────────────────────────────────┘
```

### 11.3 WorkManager Job
```kotlin
// core/queue/src/main/kotlin/.../ExportWorker.kt
@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val router: ProcessingRouter,
    private val storageManager: StorageManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val operationJson = inputData.getString(KEY_OPERATION) ?: return Result.failure()
        val operation = Json.decodeFromString<MediaOperation>(operationJson)

        setForeground(createForegroundInfo("Processing..."))

        return when (val result = router.execute(operation) { progress ->
            setProgress(workDataOf("progress" to progress.percent))
        }) {
            is OperationResult.Success -> {
                storageManager.publishToMediaStore(
                    result.outputUri,
                    "export_${System.currentTimeMillis()}",
                    "video/mp4"
                )
                Result.success()
            }
            is OperationResult.Failure -> Result.failure(
                workDataOf("error" to result.error)
            )
            else -> Result.failure()
        }
    }

    private fun createForegroundInfo(title: String) = ForegroundInfo(
        NOTIFICATION_ID,
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_export)
            .setOngoing(true)
            .build()
    )

    companion object {
        const val KEY_OPERATION = "operation"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "export_channel"
    }
}
```

**Phase 3 done when:** Can queue 5 different conversions, run them in background, see progress in notification, all output files are correct.

---

## 12. PHASE 4 — POLISH + GPU FILTERS + SETTINGS

**Goal:** GPU video filters, audio waveform zoom/polish, settings screen, split audio export.

**Duration estimate:** 5–7 days

### Features in this phase
- GPU video filters on preview (GPUVideo) and export (Mp4Composer)
- Audio: silence detection + auto-trim button
- Audio: split at playhead button
- Video: more resolution presets, custom bitrate
- Waveform: 5 zoom levels with smooth pinch gesture
- Waveform: played/unplayed color split
- Settings: default output folder, default format, theme, keep originals toggle
- About screen with licenses
- Handle incoming intent (open audio/video from file manager directly)
- Proper error messages for unsupported files

### 12.1 GPU Filters
```kotlin
// Available GPU filter presets (via GPUVideo + Mp4Composer)
enum class VideoFilter(val label: String) {
    NONE("None"),
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    SEPIA("Sepia"),
    GRAYSCALE("Grayscale"),
    BLUR("Blur"),
    SHARPEN("Sharpen"),
    VIGNETTE("Vignette"),
    COOL("Cool Tone"),
    WARM("Warm Tone"),
    FADE("Fade")
}

// Apply filter on ExoPlayer preview
fun applyPreviewFilter(player: ExoPlayer, filter: VideoFilter) {
    // GPUVideo-android — GlFilterGroup applied to ExoPlayer
}

// Apply filter on export via Mp4Composer
fun exportWithFilter(inputPath: String, outputPath: String, filter: VideoFilter) {
    Mp4Composer(inputPath, outputPath)
        .filter(filter.toGlFilter())
        .listener(object : Mp4Composer.Listener {
            override fun onProgress(progress: Double) { }
            override fun onCompleted() { }
            override fun onCanceled() { }
            override fun onFailed(exception: Exception) { }
        })
        .start()
}
```

### 12.2 Settings Screen
```
┌─────────────────────────────────────┐
│  SETTINGS                           │
├─────────────────────────────────────┤
│  Theme          [System / Dark / Light] │
│  Dynamic color  [On/Off]            │
├─────────────────────────────────────┤
│  Default output folder  [/Movies]   │
│  Default audio format   [AAC]       │
│  Default video format   [MP4]       │
│  Default video codec    [H.264]     │
│  Keep original files    [On/Off]    │
├─────────────────────────────────────┤
│  Max concurrent jobs    [1 / 2 / 3] │
├─────────────────────────────────────┤
│  Clear cache                        │
│  About / Licenses                   │
│  Version: 1.0.0 (GPL v3)           │
└─────────────────────────────────────┘
```

### 12.3 Intent handling (file manager open-with)
```kotlin
// MainActivity.kt — handle incoming URIs
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { handleIntent(it) }
}

private fun handleIntent(intent: Intent) {
    val uri = intent.data ?: return
    val mimeType = intent.type ?: contentResolver.getType(uri) ?: return
    when {
        mimeType.startsWith("audio/") -> navController.navigate(Screen.AudioEditor.createRoute(uri.toString()))
        mimeType.startsWith("video/") -> navController.navigate(Screen.VideoEditor.createRoute(uri.toString()))
    }
}
```

**Phase 4 done when:** Filters work in preview and export. Settings persist. App opens correctly from file manager. Silence auto-trim works.

---

## 13. PHASE 5 — F-DROID RELEASE

**Goal:** Clean, signed, reproducible release. F-Droid metadata. GitHub Actions CI.

**Duration estimate:** 2–3 days

### 13.1 F-Droid metadata
```
# fastlane/metadata/android/en-US/short_description.txt
All-in-one audio and video editor. Trim, crop, convert, fade. No ads. No internet.

# fastlane/metadata/android/en-US/full_description.txt
Media Editor is a free, open-source media editor for Android.

Features:
• Audio editor with waveform visualization, trim, fade, normalize
• Video editor with thumbnail timeline, crop, rotate, speed change
• Format converter: MP4, MKV, AVI, MP3, FLAC, OGG, OPUS, WAV
• Batch processing queue
• GPU video filters
• Lossless cut mode (no quality loss)
• Fully offline — zero internet permission
• Material You design

Licensed under GPL v3.
```

```yaml
# .github/workflows/build.yml
name: Build & Release
on:
  push:
    tags: ['v*']
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build release APK
        run: ./gradlew assembleRelease
      - name: Sign APK
        uses: r0adkll/sign-android-release@v1
        with:
          releaseDirectory: app/build/outputs/apk/release
          signingKeyBase64: ${{ secrets.SIGNING_KEY }}
          alias: ${{ secrets.KEY_ALIAS }}
          keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
          keyPassword: ${{ secrets.KEY_PASSWORD }}
      - name: Upload to GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/*.apk
```

### 13.2 F-Droid submission checklist
- [ ] All dependencies in `libs.versions.toml`, reproducible build
- [ ] No proprietary SDK, no analytics, no Firebase
- [ ] `fdroid/metadata/com.mediaeditor.yml` in repo
- [ ] Screenshots in `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- [ ] Build is reproducible (same inputs → same APK)
- [ ] ProGuard/R8 rules don't break FFmpeg JNI
- [ ] Submit PR to `fdroiddata` repository

### 13.3 ProGuard rules
```proguard
# app/proguard-rules.pro

# FFmpeg JNI — keep all native method bindings
-keep class com.mzgs.ffmpegx.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Media3
-keep class androidx.media3.** { *; }

# Amplituda
-keep class linc.com.amplituda.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Kotlin serialization (for WorkManager data)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
```

---

## 14. UI/UX SPEC

### Navigation structure
```
Home
├── Audio Editor
│   ├── File picker
│   ├── Waveform editor
│   └── Export
├── Video Editor
│   ├── File picker
│   ├── Thumbnail timeline
│   ├── Crop overlay
│   └── Export
├── Converter
│   └── Add to Batch or Convert Now
├── Batch Queue
└── Settings
```

### Design tokens
```kotlin
// core/ui/src/main/kotlin/.../theme/Tokens.kt
object AppTokens {
    val waveformColorUnplayed = Color(0xFF90CAF9) // Blue 200
    val waveformColorPlayed   = Color(0xFF1976D2) // Blue 700
    val trimHandleColor       = Color(0xFFFFEB3B) // Yellow
    val trimRegionColor       = Color(0x33FFEB3B) // Yellow 20% alpha
    val playheadColor         = Color(0xFFF44336) // Red
    val thumbnailStripHeight  = 64.dp
    val waveformHeight        = 120.dp
    val handleWidth           = 4.dp
    val handleTouchTarget     = 48.dp
}
```

### Component library (core/ui)
```
AppTopBar.kt          — consistent top bar with back, title, action menu
AppBottomSheet.kt     — standard bottom sheet for tool panels
ProgressDialog.kt     — export progress modal (can't be dismissed)
FormatChips.kt        — format selector chip group
ExportDoneDialog.kt   — success dialog with Share / Open / OK
ErrorSnackbar.kt      — error feedback
ToolButton.kt         — icon + label button for tool bar
TimeLabel.kt          — formatted MM:SS.mmm display
ZoomControls.kt       — + / - zoom buttons for waveform
```

### Key UX decisions
- Trim handles have 48dp touch target even if visual is 4dp wide
- Lossless mode shows a green "⚡ Lossless" badge on export button
- Progress can't be cancelled accidentally — long press to cancel
- After export: always show Share button first
- Error messages say what failed in plain language ("MP4 container doesn't support FLAC audio — switching to AAC")
- Thumbnails load lazily — show placeholder until ready
- Waveform shows "Loading..." progress while Amplituda processes large files
- Settings changes are instant (no save button)

---

## 15. FFMPEG COMMAND REFERENCE

Full command patterns for the FFmpegEngine. Use these verbatim as your template.

```bash
# ── AUDIO TRIM ──────────────────────────────────────────────────────────────

# Lossless trim (stream copy)
ffmpeg -ss {startSec} -i input.mp3 -t {durationSec} -c copy output.mp3

# Re-encode trim with fades
ffmpeg -ss {startSec} -i input.mp3 -t {durationSec} \
  -af "afade=t=in:st=0:d={fadeInSec},afade=t=out:st={outFadeStart}:d={fadeOutSec}" \
  -c:a libmp3lame -q:a 2 output.mp3

# ── AUDIO CONVERT ────────────────────────────────────────────────────────────

# → MP3
ffmpeg -i input.flac -c:a libmp3lame -b:a 320k output.mp3

# → AAC
ffmpeg -i input.mp3 -c:a aac -b:a 192k output.m4a

# → FLAC
ffmpeg -i input.mp3 -c:a flac output.flac

# → OGG Vorbis
ffmpeg -i input.mp3 -c:a libvorbis -q:a 5 output.ogg

# → OPUS
ffmpeg -i input.mp3 -c:a libopus -b:a 128k output.opus

# → WAV (uncompressed)
ffmpeg -i input.mp3 -c:a pcm_s16le output.wav

# ── AUDIO OPERATIONS ─────────────────────────────────────────────────────────

# Extract audio from video
ffmpeg -i input.mp4 -vn -c:a libmp3lame -q:a 2 output.mp3

# Merge/concat multiple audio files
ffmpeg -i 1.mp3 -i 2.mp3 -i 3.mp3 \
  -filter_complex "[0:a][1:a][2:a]concat=n=3:v=0:a=1[a]" \
  -map "[a]" -c:a aac output.m4a

# Normalize volume (loudnorm)
ffmpeg -i input.mp3 -af loudnorm=I=-16:TP=-1.5:LRA=11 -c:a aac output.m4a

# Detect silence (output timestamps to stdout)
ffmpeg -i input.mp3 -af silencedetect=noise=-30dB:d=0.5 -f null -

# ── VIDEO TRIM ───────────────────────────────────────────────────────────────

# Lossless (stream copy, fast, no quality loss)
ffmpeg -ss {startSec} -i input.mp4 -t {durationSec} \
  -c copy -avoid_negative_ts make_zero output.mp4

# Re-encode trim with video + audio fade
ffmpeg -ss {startSec} -i input.mp4 -t {durationSec} \
  -vf "fade=t=in:st=0:d={fadeInSec},fade=t=out:st={outFadeStart}:d={fadeOutSec}" \
  -af "afade=t=in:st=0:d={fadeInSec},afade=t=out:st={outFadeStart}:d={fadeOutSec}" \
  -c:v libx264 -preset fast -crf 22 -c:a aac output.mp4

# ── VIDEO CONVERT ────────────────────────────────────────────────────────────

# MP4 → MKV (stream copy, instant)
ffmpeg -i input.mp4 -c copy output.mkv

# MP4 → MKV with H.265
ffmpeg -i input.mp4 -c:v libx265 -preset fast -crf 24 -c:a aac output.mkv

# MP4 → WEBM (VP9)
ffmpeg -i input.mp4 -c:v libvpx-vp9 -b:v 1500k -c:a libvorbis output.webm

# MP4 → AVI
ffmpeg -i input.mp4 -c:v libx264 -c:a libmp3lame output.avi

# Resize + convert
ffmpeg -i input.mp4 -vf scale=1280:720 -c:v libx264 -preset fast -crf 22 -c:a aac output.mp4

# ── VIDEO OPERATIONS ─────────────────────────────────────────────────────────

# Rotate 90° clockwise
ffmpeg -i input.mp4 -vf "transpose=1" -c:v libx264 output.mp4
# transpose=0: 90°CCW+flip  1: 90°CW  2: 90°CCW  3: 90°CW+flip

# Flip horizontal
ffmpeg -i input.mp4 -vf hflip -c:v libx264 output.mp4

# Flip vertical
ffmpeg -i input.mp4 -vf vflip -c:v libx264 output.mp4

# Speed change (2x faster — video + audio)
ffmpeg -i input.mp4 \
  -vf "setpts=0.5*PTS" \
  -af "atempo=2.0" \
  -c:v libx264 -c:a aac output.mp4

# Speed change (0.5x slower)
ffmpeg -i input.mp4 \
  -vf "setpts=2.0*PTS" \
  -af "atempo=0.5" \
  -c:v libx264 -c:a aac output.mp4

# Mute video
ffmpeg -i input.mp4 -c:v copy -an output.mp4

# Replace audio
ffmpeg -i video.mp4 -i audio.mp3 \
  -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 \
  -shortest output.mp4

# Merge / concatenate videos (same codec/resolution)
# First create concat list file:
# file 'clip1.mp4'
# file 'clip2.mp4'
ffmpeg -f concat -safe 0 -i list.txt -c copy output.mp4

# Crop video (x, y, width, height in pixels)
ffmpeg -i input.mp4 -vf "crop={w}:{h}:{x}:{y}" -c:v libx264 output.mp4

# Extract frame as JPEG
ffmpeg -ss {timestampSec} -i input.mp4 -frames:v 1 -q:v 2 frame.jpg

# Reverse video
ffmpeg -i input.mp4 -vf reverse -af areverse output.mp4

# ── PROGRESS PARSING ─────────────────────────────────────────────────────────
# FFmpeg outputs "time=HH:MM:SS.ms" to stderr during processing
# Parse this against total duration to calculate percent progress
# Regex: time=(\d{2}):(\d{2}):(\d{2}\.\d{2})
```

---

## 16. TESTING STRATEGY

### Unit tests
```kotlin
// Test ProcessingRouter routing decisions
class ProcessingRouterTest {
    @Test fun `audio trim with fade routes to FFmpeg`() { ... }
    @Test fun `video trim without fade routes to Transformer`() { ... }
    @Test fun `convert to MKV routes to FFmpeg`() { ... }
    @Test fun `convert audio to MP3 routes to FFmpeg`() { ... }
}

// Test FFmpeg command building
class FFmpegCommandBuilderTest {
    @Test fun `trim audio lossless generates correct -c copy command`() { ... }
    @Test fun `fade in command has correct afade filter`() { ... }
    @Test fun `MP3 convert uses libmp3lame codec`() { ... }
}

// Test AudioProject model
class AudioProjectTest {
    @Test fun `trimEnd cannot exceed duration`() { ... }
    @Test fun `fadeOut cannot exceed trim duration`() { ... }
}
```

### Integration tests
```kotlin
// Test with real audio/video files in androidTest
class FFmpegEngineTest {
    @Test fun `trim audio produces correct output duration`() { ... }
    @Test fun `convert MP3 to FLAC produces valid FLAC file`() { ... }
    @Test fun `lossless video trim stream copy succeeds`() { ... }
}
```

### UI tests
```kotlin
// Compose UI tests
class AudioEditorScreenTest {
    @Test fun `waveform renders after file load`() { ... }
    @Test fun `export button disabled while loading`() { ... }
    @Test fun `progress shows during export`() { ... }
}
```

### Manual test matrix
| Test | Min SDK 23 | API 29 | API 33 | API 35 |
|------|-----------|--------|--------|--------|
| Audio trim lossless | | | | |
| Audio trim with fade | | | | |
| Video trim lossless | | | | |
| MP4→MKV convert | | | | |
| MP3→FLAC convert | | | | |
| Batch 5 jobs | | | | |
| Open from file manager | | | | |
| Set as ringtone | | | | |

---

## 17. KNOWN PITFALLS & FIXES

### FFmpeg path handling
**Problem:** FFmpeg needs real file paths. `content://` URIs don't work.
**Fix:** `StorageManager.getRealPath()` copies to cache if path unavailable. Always call this before building FFmpeg commands.

### Android 10+ W^X restriction
**Problem:** Executing native code loaded from writable paths is blocked on API 29+.
**Fix:** `mzgs/FFmpegX-Android` already handles this via JNI wrapper approach. Don't use older binaries.

### 16KB page size (Android 15+)
**Problem:** NDK libraries not aligned to 16KB pages crash on new devices.
**Fix:** Use `mzgs/FFmpegX-Android` — it's built with 16KB page alignment. Don't use original `arthenica` binaries.

### Media3 Transformer main thread
**Problem:** Transformer must be created and started on main thread.
**Fix:** `TransformerEngine` uses `withContext(Dispatchers.Main)`. Don't move off main.

### Scoped storage file writing
**Problem:** Can't write to arbitrary paths on API 29+. MediaStore `IS_PENDING` flag required.
**Fix:** Use `StorageManager.publishToMediaStore()` for final output. Work in cache dir during processing.

### Large file waveform
**Problem:** Amplituda can OOM on files > 100MB.
**Fix:** Process in chunks, downsample aggressively. Use `Amplituda.processAudio(uri, maxAmplitudes = 500)`.

### MediaMetadataRetriever leak
**Problem:** `MediaMetadataRetriever` not closed = file handle leak.
**Fix:** Always `retriever.release()` in finally block or use `use {}` pattern.

### Video fade lossless conflict
**Problem:** Fade filters require re-encoding. Can't do lossless + fade.
**Fix:** If fade > 0, disable lossless toggle. Show user message: "Fade requires re-encoding."

### Concat filter audio sync
**Problem:** Concat of audio files with different sample rates / channel counts fails.
**Fix:** Add `-ar 44100 -ac 2` to normalize output of concat filter.

### ExoPlayer in Compose lifecycle
**Problem:** ExoPlayer not released on screen disposal = memory leak.
**Fix:**
```kotlin
DisposableEffect(Unit) {
    onDispose {
        player.stop()
        player.release()
    }
}
```

### Trim handle touch targets too small
**Problem:** 4dp visual handle is impossible to drag on real devices.
**Fix:** Use 48dp touch target via `Modifier.pointerInput` on a larger invisible area centered on the handle visual.

### WorkManager duplicate jobs
**Problem:** Device rotation re-enqueues same job.
**Fix:** Use `WorkManager.enqueueUniqueWork()` with unique name per file + operation.

### FFmpeg progress on stderr
**Problem:** FFmpeg writes progress to stderr, not stdout.
**Fix:** Redirect stderr via JNI callback. Parse `time=HH:MM:SS.ms` pattern. Compare against total duration (from FFprobe pre-scan) to get percent.

### F-Droid reproducible build
**Problem:** JitPack can't be used for F-Droid submissions.
**Fix:** Either vendor the FFmpegX-Android AAR directly into the repo, or find it on Maven Central. F-Droid builds from source — all deps must be resolvable without JitPack. If JitPack is unavoidable, submit as an "Anti-Feature" or bundle the prebuilt AAR.

---

## BUILD ORDER SUMMARY

```
Phase 0 (1 day)   → Skeleton compiles, nav works, theme set, Hilt wired
Phase 1 (7 days)  → Audio editor works end to end
Phase 2 (10 days) → Video editor works end to end
Phase 3 (5 days)  → Converter + Batch queue working
Phase 4 (7 days)  → GPU filters, polish, settings, intents
Phase 5 (3 days)  → CI, signing, F-Droid submission
─────────────────────────────────────────────────────
Total estimate:    ~33 days solo. Faster with AI-assisted coding.
```

---

*GPL v3 · Build it. Ship it. Own it.*
