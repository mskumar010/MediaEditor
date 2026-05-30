# Android Slop Ban — Forbidden Patterns
# .agents/rules/android-slop-ban.md
# These are the patterns AI agents generate by default.
# Every single one of them will either crash, leak, or corrupt data.
# NONE of these patterns may appear in this codebase.

---

## MEMORY LEAKS

❌ BANNED:
```kotlin
companion object { var context: Context? = null }   // static context leak
object AppContext { val ctx = applicationContext }   // same
```
✅ USE: Inject Application context via Hilt @ApplicationContext only where truly needed.

---

❌ BANNED:
```kotlin
val player = ExoPlayer.Builder(context).build()
// inside Composable with no DisposableEffect
```
✅ MANDATORY:
```kotlin
DisposableEffect(Unit) {
    onDispose { player.stop(); player.release() }
}
```

---

❌ BANNED:
```kotlin
val retriever = MediaMetadataRetriever()
retriever.setDataSource(path)
val duration = retriever.extractMetadata(...)
// no release
```
✅ MANDATORY:
```kotlin
val retriever = MediaMetadataRetriever()
try {
    retriever.setDataSource(path)
    // use
} finally {
    retriever.release()
}
```

---

❌ BANNED:
```kotlin
cursor.moveToFirst()
val value = cursor.getString(0)
// no close
```
✅ MANDATORY: cursor.use {} or close() in finally.

---

## THREADING VIOLATIONS

❌ BANNED:
```kotlin
runOnUiThread { ... }
Handler(Looper.getMainLooper()).post { ... }
```
✅ USE: withContext(Dispatchers.Main) inside coroutines, or StateFlow emission.

---

❌ BANNED:
```kotlin
// In ViewModel
viewModelScope.launch {
    ffmpegEngine.execute(command) // runs on Main thread
}
```
✅ MANDATORY:
```kotlin
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        ffmpegEngine.execute(command)
    }
}
```

---

❌ BANNED:
```kotlin
// On IO thread:
transformer.start(input, output, listener) // Media3 Transformer
```
✅ MANDATORY: Transformer must start on Main thread. Wrap in withContext(Dispatchers.Main).

---

## NULL SAFETY VIOLATIONS

❌ BANNED:
```kotlin
val file = getFileFromUri(uri)!!
val path = file!!.absolutePath
```
✅ USE: Proper null handling with when/let/return@function. Document why !! is safe if truly unavoidable.

---

❌ BANNED:
```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    // empty
}
```
✅ MANDATORY: Log the error, emit to error state, or rethrow. Empty catch blocks hide crashes.

---

## STORAGE VIOLATIONS

❌ BANNED:
```kotlin
ffmpegEngine.execute("-i $contentUri -o $outputPath") // content URI to FFmpeg
```
✅ MANDATORY:
```kotlin
val realPath = storageManager.getRealPath(contentUri) // resolves to real path
ffmpegEngine.execute("-i $realPath -o $outputPath")
```

---

❌ BANNED:
```kotlin
File("/sdcard/output.mp4").writeBytes(data) // direct external write
File(Environment.getExternalStorageDirectory(), "output.mp4") // same
```
✅ MANDATORY: Use StorageManager.publishToMediaStore() for all final outputs on API 29+.

---

❌ BANNED:
```kotlin
// Writing output without IS_PENDING flag on API 29+
val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
outputStream = contentResolver.openOutputStream(uri)
// writing directly without IS_PENDING
```
✅ MANDATORY: Set IS_PENDING = 1 before write, IS_PENDING = 0 in finally after write.

---

## WORKMANAGER VIOLATIONS

❌ BANNED:
```kotlin
WorkManager.getInstance(context).enqueue(request) // survives rotation, duplicates jobs
```
✅ MANDATORY:
```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    "trim_${filename}_${timestamp}",
    ExistingWorkPolicy.KEEP,
    request
)
```

---

## COROUTINE VIOLATIONS

❌ BANNED:
```kotlin
GlobalScope.launch { ... }
CoroutineScope(Dispatchers.IO).launch { ... } // unscoped, never cancelled
```
✅ USE: viewModelScope, lifecycleScope, or Hilt-injected scoped CoroutineScope only.

---

❌ BANNED:
```kotlin
// FFmpeg session not cancelled on coroutine cancellation
viewModelScope.launch {
    ffmpegSession = FFmpegEngine.execute(command)
    // if job is cancelled here, ffmpeg keeps running in background
}
```
✅ MANDATORY:
```kotlin
viewModelScope.launch {
    try {
        ffmpegSession = FFmpegEngine.execute(command)
    } finally {
        if (!isActive) ffmpegSession?.cancel()
    }
}
```

---

## COMPOSE VIOLATIONS

❌ BANNED:
```kotlin
@Composable
fun AudioScreen() {
    val result = repository.loadFile() // IO in composable body
}
```
✅ USE: LaunchedEffect + ViewModel + StateFlow.

---

❌ BANNED:
```kotlin
@Composable
fun TrimHandle(onDrag: () -> Unit) {
    Box(Modifier.size(4.dp)) // 4dp touch target — impossible to tap
}
```
✅ MANDATORY: Minimum 48dp touch target. Use invisible larger touch area over visual handle.

---

## ARCHITECTURE VIOLATIONS

❌ BANNED:
```kotlin
// ViewModel calling engine directly
class AudioEditorViewModel : ViewModel() {
    fun trim() {
        ffmpegEngine.execute(...) // bypasses router, bypasses usecase
    }
}
```
✅ MANDATORY: ViewModel → UseCase → Repository → ProcessingRouter → Engine.

---

❌ BANNED:
```kotlin
// Business logic in Composable
@Composable
fun AudioScreen() {
    if (file.duration > 60_000) { // business rule in UI
        showWarning()
    }
}
```
✅ USE: Business logic in UseCases. Composables only render UiState.

---

## PERFORMANCE VIOLATIONS

❌ BANNED:
```kotlin
// Amplituda on full file, no downsample limit
amplituda.processAudio(uri) // OOM on files > 100MB
```
✅ MANDATORY:
```kotlin
amplituda.processAudio(uri, maxAmplitudes = 500)
```

---

❌ BANNED:
```kotlin
// MediaMetadataRetriever frame extraction in loop without throttling
for (timestamp in thumbnailTimestamps) {
    val bmp = retriever.getFrameAtTime(timestamp) // main thread, no cache
}
```
✅ USE: Dispatchers.IO, thumbnail cache, limit to visible frames only.
