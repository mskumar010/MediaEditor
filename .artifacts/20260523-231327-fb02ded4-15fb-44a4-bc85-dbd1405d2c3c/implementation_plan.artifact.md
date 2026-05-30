# Implementation Plan - Audio Editor Polish & Fix

This plan addresses the blank audio preview issue and polishes the Audio Editor UI as requested. Video Editor implementation is postponed.

## User Review Required

- **Amplituda Constraint**: I will implement the >100MB check in `WaveformEditor` to prevent OOM as per `GEMINI.md`.
- **Real Path Requirement**: `Amplituda` and `FFmpeg` require real file paths. I will ensure these are resolved via `StorageManager`.

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
- Pass real path to UI via a new state or updated `AudioProject`.
- Add state and update methods for `fadeInMs` and `fadeOutMs`.
- Auto-seek to `trimStartMs` when it changes for immediate preview.

#### [AudioEditorScreen.kt](file:///C:/Users/pc/AndroidStudioProjects/MediaEditor/feature/audio-editor/src/main/java/com/mediaeditor/feature/audioeditor/presentation/AudioEditorScreen.kt)
- Use `HybridCard` and `HybridSectionHeader` for better layout.
- Add "Fade In" and "Fade Out" duration controls (Sliders or Input).
- Polish export section with major format selection.

## Verification Plan

### Automated Tests
- Run `analyze_file` on modified files.
- Use `render_compose_preview` for `AudioEditorScreen`.

### Manual Verification
- Check `logcat` for FFmpeg commands to verify fade filters (`afade`) are correctly applied.
- Verify `Amplituda` initialization via code analysis.
