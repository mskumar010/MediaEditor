# Task Management

- [x] Fix Audio Editor Blank Screen
	- [x] Update `AudioEditorViewModel` to resolve real path
	- [x] Update `WaveformEditor` to use real path and handle OOM constraint
- [x] Polish Audio Editor UI
	- [x] Add `HybridCard` and `HybridSectionHeader` to `core:ui`
	- [x] Add Fade In/Out controls to `AudioEditorScreen`
	- [x] Refactor `AudioEditorScreen` with `HybridTheme` components
- [x] Verification
	- [x] Run `analyze_file` on all modified files
	- [x] Use `render_compose_preview` for `AudioEditorScreen`
