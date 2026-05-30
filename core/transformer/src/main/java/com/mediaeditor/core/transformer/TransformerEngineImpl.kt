package com.mediaeditor.core.transformer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.*
import com.mediaeditor.core.router.*
import com.mediaeditor.core.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class TransformerEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager
) : TransformerEngine {

    override suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = withContext(Dispatchers.Main) {
        val inputUri = when (operation) {
            is MediaOperation.TrimVideo -> operation.inputUri
            is MediaOperation.CropVideo -> operation.inputUri
            is MediaOperation.ChangeSpeed -> operation.inputUri
            else -> null
        } ?: return@withContext OperationResult.Failure("Unsupported operation")

        val outputUri = when (operation) {
            is MediaOperation.TrimVideo -> operation.outputUri
            is MediaOperation.CropVideo -> operation.outputUri
            is MediaOperation.ChangeSpeed -> operation.outputUri
            else -> null
        } ?: return@withContext OperationResult.Failure("Unsupported operation")

        suspendCancellableCoroutine<OperationResult> { continuation ->
            try {
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) {
                                continuation.resume(OperationResult.Success(outputUri, 0L))
                            }
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            if (continuation.isActive) {
                                continuation.resume(OperationResult.Failure(exportException.message ?: "Unknown Transformer error"))
                            }
                        }
                    })
                    .build()

                val mediaItemBuilder = MediaItem.Builder().setUri(inputUri)
                val effects = mutableListOf<Effect>()
                val audioProcessors = mutableListOf<AudioProcessor>()

                if (operation is MediaOperation.TrimVideo) {
                    mediaItemBuilder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(operation.startMs)
                            .setEndPositionMs(operation.endMs)
                            .build()
                    )
                    
                    if (operation.speed != 1.0f) {
                        val sonic = SonicAudioProcessor()
                        sonic.setSpeed(operation.speed)
                        audioProcessors.add(sonic)
                    }

                    if (operation.rotation != 0) {
                        effects.add(ScaleAndRotateTransformation.Builder()
                            .setRotationDegrees(operation.rotation.toFloat())
                            .build())
                    }

                    operation.cropRect?.let {
                        effects.add(Crop(it.left, it.top, it.right, it.bottom))
                    }
                }

                val mediaItem = mediaItemBuilder.build()
                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setEffects(androidx.media3.transformer.Effects(audioProcessors, effects))
                    .setRemoveAudio(operation is MediaOperation.TrimVideo && operation.muteAudio)
                    .build()

                val outputPath = storageManager.getRealPath(outputUri) ?: throw Exception("Could not resolve output path")
                
                transformer.start(editedMediaItem, outputPath)

                // Progress tracking
                val handler = Handler(Looper.getMainLooper())
                val progressRunnable = object : Runnable {
                    override fun run() {
                        if (continuation.isActive) {
                            val progressHolder = ProgressHolder()
                            val state = transformer.getProgress(progressHolder)
                            if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                onProgress(OperationResult.Progress(progressHolder.progress))
                            }
                            handler.postDelayed(this, 500)
                        }
                    }
                }
                handler.post(progressRunnable)

                continuation.invokeOnCancellation {
                    transformer.cancel()
                    handler.removeCallbacks(progressRunnable)
                }

            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(OperationResult.Failure(e.message ?: "Initialization error"))
                }
            }
        }
    }
}
