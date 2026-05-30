# Task Management

- [ ] Fix Audio Editor Blank Screen
	- [ ] Update `AudioEditorViewModel` to resolve real path
	- [ ] Update `WaveformEditor` to use real path and handle OOM constraint
- [ ] Polish Audio Editor UI
	- [ ] Add `HybridCard` and `HybridSectionHeader` to `core:ui`
	- [ ] Add Fade In/Out controls to `AudioEditorScreen`
	- [ ] Refactor `AudioEditorScreen` with `HybridTheme` components
- [ ] Implement Video Editor
	- [ ] Create `VideoEditorModule`, `TrimVideoUseCase`, `VideoEditorViewModel`
	- [ ] Create `VideoEditorScreen` with preview and controls (Trim, Fade, Crop, Speed)
	- [ ] Update `AppNavHost` to use the real Video Editor
- [ ] Verification
	- [ ] Run `analyze_file` on all modified files
	- [ ] Use `render_compose_preview` for both editor screens
