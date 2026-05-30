# GEMINI.md — Antigravity-Specific Rules
# Overrides AGENTS.md for Antigravity agents only.
# Priority: GEMINI.md > AGENTS.md > .agents/rules/

---

## AGENT BEHAVIOR

- Plan before coding. For any task touching more than 1 file: output the plan first, wait for approval.
- Do not refactor code outside the stated task scope. Scope creep = broken builds.
- Do not rename symbols without a full project-wide search first. Kotlin symbol renames break module boundaries silently.
- When running parallel agents: assign one agent per feature module maximum. Never two agents in the same module simultaneously.
- After every code-producing task: run lint mentally. Flag any issue before closing the task.

---

## PARALLEL AGENT BOUNDARIES

These modules can be worked on in parallel by separate agents. Never cross boundaries mid-task.

```
Agent A: feature/audio-editor/ + core/waveform/
Agent B: feature/video-editor/ + core/transformer/
Agent C: feature/converter/ + core/ffmpeg/
Agent D: feature/batch/ + core/queue/
Agent E: app/ + core/router/ + core/storage/ + core/ui/
```

If a task requires touching two agent boundaries: stop, report the conflict, and let the developer decide.

---

## WHAT AGENTS MAY DO WITHOUT ASKING

- Write new Composable functions inside existing screens
- Write new UseCases inside existing feature modules
- Add new FFmpeg commands to the command builder DSL
- Write unit tests for existing logic
- Fix compilation errors caused by their own previous output
- Update libs.versions.toml versions (with a note on what changed)

---

## WHAT AGENTS MUST ASK BEFORE DOING

- Adding a new dependency not in libs.versions.toml
- Changing module structure (new module, merge modules, rename modules)
- Changing the ProcessingRouter routing logic
- Modifying StorageManager (any change here breaks all features)
- Changing any data model in domain/model/ (breaks serialization, state, tests)
- Adding any permission to AndroidManifest.xml
- Changing the navigation graph structure

---

## TASK OUTPUT FORMAT

For every coding task, output in this order:
1. What files will be changed/created
2. The code
3. What to verify after applying

No filler. No "Great question!". No restating the task back. Just the three parts.

---

## ANDROID STUDIO INTEGRATION

- Use the android studio CLI command for: symbol resolution, Compose preview generation, lint checks.
- After generating Composable functions: request a Compose Preview check.
- After modifying build files: verify sync before proceeding.

---

## KNOWN PROJECT-SPECIFIC CONSTRAINTS

- Media3 Transformer: MUST start on Main thread. Always withContext(Dispatchers.Main). Do not move this.
- FFmpeg: MUST receive real file paths. content:// URIs will fail silently. Always call StorageManager.getRealPath() first.
- Amplituda on files > 100MB: always use maxAmplitudes = 500 parameter. OOM otherwise.
- ExoPlayer: MUST be released in DisposableEffect onDispose. This is not optional.
- WorkManager jobs: MUST use enqueueUniqueWork. Rotation duplicate jobs is a known issue.
- 16KB page size: only use mzgs/FFmpegX-Android binaries. Never arthenica binaries (pulled from Maven).

---

## STRICT BUILD STABILITY RULES (The "Zero-Failure" Protocol)

- **RULE 1: NO KAPT.** Kapt is deprecated and causes metadata version mismatches with Hilt. Use **KSP** exclusively for all annotation processing.
- **RULE 2: KOTLIN-KSP HANDSHAKE.** The KSP version must ALWAYS match the major Kotlin version (e.g., Kotlin `2.1.0` -> KSP `2.1.0-x.y.z`).
- **RULE 3: NO FORCED VERSIONS.** Never use `resolutionStrategy { force(...) }` in the root build file. Resolve conflicts in `libs.versions.toml`.
- **RULE 4: AGP 9.0 NATIVE KOTLIN.** Do not apply the `kotlin-android` plugin. AGP 9.0+ provides built-in Kotlin support.
- **RULE 5: CENTRALIZED VERSIONING.** No hardcoded version strings in module `build.gradle.kts` files. Use `libs.versions.toml` only.
- **RULE 6: DSL COMPLIANCE.** Use `compilerOptions` instead of `kotlinOptions` for JVM targets and other compiler flags.

---

## SKILLS DIRECTORY

See .agents/skills/ for detailed technical reference:
- processing-engine-routing.md — exact decision table for engine selection
- ffmpeg-command-patterns.md — copy-paste FFmpeg command templates
- storage-patterns.md — scoped storage, SAF, MediaStore patterns
- compose-lifecycle-patterns.md — DisposableEffect, LaunchedEffect, lifecycle rules
