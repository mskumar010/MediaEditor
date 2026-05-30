# Processing Engine Routing Rules
# .agents/rules/processing-engine.md
# Every operation in this app routes through ProcessingRouter.
# Agents must follow this table. No exceptions.

---

## ENGINE DECISION TABLE

| Operation | Engine | Reason |
|-----------|--------|--------|
| Audio trim, lossless | FFmpegEngine (-c copy) | Stream copy, no re-encode |
| Audio trim + fade | FFmpegEngine (afade filter) | Fade forces re-encode |
| Audio volume adjust | FFmpegEngine (volume filter) | |
| Audio normalize | FFmpegEngine (loudnorm filter) | |
| Audio merge/concat | FFmpegEngine (concat filter) | Sample rate normalization needed |
| Audio format convert | FFmpegEngine | All codec changes |
| Output: MP3 / FLAC / OGG / AAC | FFmpegEngine | MediaCodec can't write these |
| Video trim, lossless, no filters | FFmpegEngine (-c copy) | Stream copy |
| Video trim + re-encode | TransformerEngine | Faster, hardware-accelerated |
| Video crop | TransformerEngine | Hardware-accelerated |
| Video rotate / flip | TransformerEngine | Hardware-accelerated |
| Video speed change | TransformerEngine | |
| Video merge / concat | TransformerEngine | |
| Video mute | TransformerEngine (-an equivalent) | |
| Video replace audio | TransformerEngine | |
| Output: MKV / AVI / FLV / WEBM | FFmpegEngine | MediaCodec can't write these containers |
| Output: MP4 (standard) | TransformerEngine | Default fast path |
| GPU shader filter on export | Mp4ComposerEngine | OpenGL required |
| GPU filter preview (ExoPlayer) | GPUVideoEngine | Live preview only |
| Waveform extraction | AmplitudaEngine | Never FFmpeg for this |
| Thumbnail strip frames | MediaMetadataRetriever | Never FFmpeg for this |
| Preview playback | ExoPlayer | Never FFmpeg for this |

---

## LOSSLESS DECISION

Lossless (stream copy) is only possible if ALL of these are true:
- No fade filter
- No volume change
- No speed change
- No crop
- No rotate
- No format conversion
- Output container same as input container

If any condition fails → re-encode. Set lossless = false automatically in ProcessingRouter.

---

## INTERFACE CONTRACT

```kotlin
// core/router/ProcessingRouter.kt
interface ProcessingRouter {
    suspend fun process(operation: MediaOperation): Flow<ProcessingProgress>
}

// core/router/ProcessingRouterImpl.kt
// Routes to correct engine based on MediaOperation properties.
// ViewModels and UseCases only call ProcessingRouter, never engines directly.
```

---

## FFMPEG WRAPPER

```kotlin
// core/ffmpeg/FFmpegEngine.kt
// Wraps mzgs/FFmpegX-Android
// ALWAYS call StorageManager.getRealPath(uri) before building any command
// ALWAYS cancel session in coroutine cancellation handler
// ALWAYS parse stderr for user-readable error messages on failure
// ALWAYS emit Progress(percent) by parsing "time=HH:MM:SS.ms" from stderr
```

---

## TRANSFORMER WRAPPER

```kotlin
// core/transformer/TransformerEngine.kt
// Wraps Media3 Transformer
// ALWAYS create and start Transformer on Dispatchers.Main (framework requirement)
// ALWAYS release Transformer in finally block
// ALWAYS use TransformationRequest.Builder() for operation specification
```
