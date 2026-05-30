package com.mediaeditor.core.router

import javax.inject.Inject
import javax.inject.Singleton

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
