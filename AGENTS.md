# AGENTS.md — Media Editor Android
# Cross-tool rules: Antigravity · Cursor · Claude Code
# Read this FULLY before writing or modifying any file.

---

## PROJECT IDENTITY

- App: Media Editor — FOSS offline Android media editor
- Package: com.mediaeditor
- Language: Kotlin 2.x
- UI: Jetpack Compose + Material 3
- Architecture: Clean Architecture (data / domain / presentation)
- License: GPL v3
- Distribution: F-Droid + GitHub Releases
- Internet permission: NONE. The app is 100% offline. Never add network calls.

---

## ABSOLUTE RULES — NEVER VIOLATE

1. No network calls. No internet permission. No analytics. No crash reporters that phone home.
2. No runOnUiThread(). Use withContext(Dispatchers.Main) or LaunchedEffect.
3. No findViewById(). No View binding. Compose only.
4. No deprecated API usage without a documented reason in a comment.
5. No hardcoded file paths. Use StorageManager abstraction in core/storage/.
6. No content:// URI passed directly to FFmpeg. Always resolve to real path via StorageManager.getRealPath() first.
7. No static Context references. Context leaks are silent and catastrophic.
8. No GlobalScope. Use viewModelScope, lifecycleScope, or injected CoroutineScope.
9. No blocking calls on Main thread. FFmpeg, Transformer, Amplituda — all go to Dispatchers.IO.
10. No new dependencies without updating libs.versions.toml first.

---

## MODULE STRUCTURE — DO NOT DEVIATE

```
root/
├── app/                    ← navigation host, DI root, MainActivity only
├── core/
│   ├── ffmpeg/             ← FFmpegX wrapper + command builder DSL only
│   ├── transformer/        ← Media3 Transformer pipeline wrapper only
│   ├── waveform/           ← Amplituda + WaveformSeekBar integration only
│   ├── router/             ← ProcessingRouter: engine selection logic only
│   ├── storage/            ← SAF, MediaStore, scoped storage abstraction only
│   ├── queue/              ← WorkManager job queue only
│   └── ui/                 ← shared Compose components, theme, tokens only
├── feature/
│   ├── audio-editor/
│   ├── video-editor/
│   ├── converter/
│   ├── batch/
│   └── settings/
└── build-logic/            ← convention plugins only
```

Each feature module must have exactly: data/ domain/ presentation/
Do not add layers. Do not flatten. Do not merge feature modules.

---

## PROCESSING ENGINE ROUTING — MANDATORY

Use ProcessingRouter. Never call FFmpeg or Transformer directly from ViewModels or UseCases.

```
Format conversion needed?              → FFmpegEngine
Audio filter (fade / normalize)?       → FFmpegEngine
Output is MKV / AVI / FLV?            → FFmpegEngine
Output audio is MP3 / FLAC / OGG?     → FFmpegEngine
Lossless stream copy?                  → FFmpegEngine (-c copy)
Video trim / crop / rotate / speed?    → TransformerEngine (Media3)
Video merge / mute / replace audio?    → TransformerEngine (Media3)
GPU shader filter on export?           → Mp4ComposerEngine
Waveform data extraction?              → AmplitudaEngine
Preview playback?                      → ExoPlayer (never FFmpeg)
```

---

## NAMING CONVENTIONS

- ViewModels: `FeatureNameViewModel` (e.g. AudioEditorViewModel)
- UseCases: verb + noun + UseCase (e.g. TrimAudioUseCase, FadeAudioUseCase)
- Repositories: `FeatureNameRepository` (interface) + `FeatureNameRepositoryImpl` (impl)
- Screens: `FeatureNameScreen` composable (e.g. AudioEditorScreen)
- State: `FeatureNameUiState` sealed class or data class
- Events: `FeatureNameEvent` sealed class
- Core engines: `*Engine` suffix (FFmpegEngine, TransformerEngine)
- Commands: `*Command` suffix (TrimAudioCommand, ConvertCommand)

---

## ARCHITECTURE RULES

- ViewModels expose: StateFlow<UiState> and Channel<UiEffect> only.
- ViewModels call: UseCases only. Never repositories directly.
- UseCases call: Repositories only. Never engines directly.
- Repositories call: DataSources and Engines. Never ViewModels.
- Engines live in core/. Features depend on core/. Core never depends on features.
- No business logic in Composable functions. Composables render state, emit events.
- UiState must be a data class or sealed class. No raw Strings as state.

---

## ASYNC RULES

- All media processing: Dispatchers.IO
- Media3 Transformer start/create: Dispatchers.Main (framework requirement — comment this)
- UI state updates: Dispatchers.Main via StateFlow
- Progress emission: Flow<ProcessingProgress> from engines to ViewModels
- Cancellation: every processing job must respect coroutine cancellation. FFmpegEngine must call session.cancel() on coroutine cancellation.

---

## RESOURCE LIFECYCLE — MANDATORY CLEANUP

ExoPlayer:
```kotlin
DisposableEffect(Unit) {
    onDispose { player.stop(); player.release() }
}
```

MediaMetadataRetriever: always use .use {} or release() in finally.
FFmpeg sessions: cancel + release on coroutine cancellation or ViewModel onCleared().
MediaCodec: always release() in finally. Never leave open.
Amplituda: cancel extraction on ViewModel onCleared().
Cursor: always close() in finally or use .use {}.

---

## STORAGE RULES

- Always use StorageManager (core/storage/) for all file operations.
- Work files (temp, in-progress): cacheDir only.
- Final output: MediaStore via StorageManager.publishToMediaStore() on API 29+.
- Never write to external storage directly.
- content:// URIs: resolve via StorageManager.getRealPath() before FFmpeg. Cache copy if real path unavailable.
- IS_PENDING flag: set before write, clear after. Always in try/finally.

---

## WORKMANAGER RULES

- Always use enqueueUniqueWork() with a deterministic unique name per job.
- Unique name format: "operation_filename_timestamp" to survive rotation.
- Constraints: requires storage, not low battery.
- Progress: send via setProgressAsync() with standard keys.

---

## ERROR HANDLING

- All engine errors must return Result<T> or emit to error StateFlow. Never throw raw exceptions to UI.
- FFmpeg non-zero exit code: parse stderr for user-readable message before emitting error.
- File not found: emit specific FileNotFoundError, not generic exception.
- No empty catch blocks. Ever.
- No `!!` operator except where null is truly impossible and a comment explains why.

---

## CODE STYLE

- No comments that restate what code does ("// set the title"). Comments explain WHY.
- No TODO left in committed code unless it's a tracked issue with a reference.
- Max function length: 40 lines. Extract if longer.
- Max file length: 300 lines. Split if longer.
- No magic numbers. Extract to named constants.
- Exhaustive when() on sealed classes. No else branch on sealed hierarchies.

---

## F-DROID COMPLIANCE

- No JitPack dependencies in production. Vendor AARs or use Maven Central only.
- No proprietary SDKs. No Firebase. No Google Play Services.
- No non-free assets.
- Reproducible build: pin all dependency versions exactly in libs.versions.toml.
- GPL v3 headers in all source files.
