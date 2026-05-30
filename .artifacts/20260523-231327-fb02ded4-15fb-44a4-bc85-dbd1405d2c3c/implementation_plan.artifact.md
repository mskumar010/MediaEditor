# Implementation Plan - Audio and Video Editor Polish & Fix

This plan addresses the blank audio preview issue, polishes the Audio Editor UI, and implements the Video Editor with similar "smart" features.

## User Review Required

- **Boundary Crossing**: This task involves `feature/audio-editor` (Agent A) and `feature/video-editor` (Agent B). I will handle both sequentially.
- **Data Model Change**: I'll add `sourcePath` to `AudioProject` and `VideoProject` (or handle it in ViewModels) to support `Amplituda` and ensure real path usage as required by `GEMINI.md`.

## Proposed Changes

### [Core UI]

#### [HybridCard.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/core/ui/src/main/java/com/mediaeditor/core/ui/components/HybridCard.kt) [NEW]
- Implement `HybridCard` following the design guide (1dp border + subtle elevation).

#### [HybridSectionHeader.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/core/ui/src/main/java/com/mediaeditor/core/ui/components/HybridSectionHeader.kt) [NEW]
- Implement `HybridSectionHeader` for section titles.

---

### [Core Waveform]

#### [WaveformEditor.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/core/waveform/src/main/java/com/mediaeditor/core/waveform/WaveformEditor.kt)
- Update to accept `sourcePath: String?`.
- Use `sourcePath` for `Amplituda` to fix blank screen (content URIs fail).
- Implement >100MB check: use `maxAmplitudes = 500` if file > 100MB to avoid OOM.
- Ensure handles are responsive and trigger seek on drag.

---

### [Feature Audio Editor]

#### [AudioEditorViewModel.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/audio-editor/src/main/java/com/mediaeditor/feature/audioeditor/presentation/AudioEditorViewModel.kt)
- Resolve real path using `StorageManager.getRealPath(uri)`.
- Pass real path to UI.
- Add state and update methods for `fadeInMs` and `fadeOutMs`.
- Auto-seek to `trimStartMs` when it changes.

#### [AudioEditorScreen.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/audio-editor/src/main/java/com/mediaeditor/feature/audioeditor/presentation/AudioEditorScreen.kt)
- Use `HybridCard` and `HybridSectionHeader` for better layout.
- Add "Fade In" and "Fade Out" duration controls.
- Polish export section with major format selection.

---

### [Feature Video Editor]

#### [VideoEditorModule.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/video-editor/src/main/java/com/mediaeditor/feature/videoeditor/di/VideoEditorModule.kt) [NEW]
- Provide `ExoPlayer` for video preview.

#### [TrimVideoUseCase.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/video-editor/src/main/java/com/mediaeditor/feature/videoeditor/domain/usecase/TrimVideoUseCase.kt) [NEW]
- Invoke `ProcessingRouter` for video trim operations.

#### [VideoEditorViewModel.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/video-editor/src/main/java/com/mediaeditor/feature/videoeditor/presentation/VideoEditorViewModel.kt) [NEW]
- Manage `VideoProject` state.
- Handle playback with `ExoPlayer`.
- Support Trim, Fade, Crop, and Speed options.

#### [VideoEditorScreen.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/video-editor/src/main/java/com/mediaeditor/feature/videoeditor/presentation/VideoEditorScreen.kt) [NEW]
- Full-featured video editor UI.
- Video preview area.
- Multi-purpose editor controls (Trim, Fade, Crop, Speed).
- Use `HybridTheme` components.

---

### [App Navigation]

#### [AppNavHost.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/app/src/main/java/com/mediaeditor/navigation/AppNavHost.kt)
- Point to real `VideoEditorScreen` in `feature/video-editor` package.

## Verification Plan

### Automated Tests
- Run existing unit tests if any.
- Since I cannot run on real device easily, I will use `analyze_file` and `render_compose_preview` to verify UI.

### Manual Verification
- Use `render_compose_preview` for both Audio and Video editors.
- Check `logcat` for FFmpeg commands to verify fade filters are correctly applied.
- Verify `Amplituda` initialization and error handling via code analysis.
