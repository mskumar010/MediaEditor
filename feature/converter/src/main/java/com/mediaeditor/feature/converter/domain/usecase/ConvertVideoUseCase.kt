package com.mediaeditor.feature.converter.domain.usecase

import android.net.Uri
import com.mediaeditor.core.router.MediaOperation
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.router.ProcessingRouter
import com.mediaeditor.core.router.Resolution
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class ConvertVideoUseCase @Inject constructor(
    private val router: ProcessingRouter
) {
    operator fun invoke(
        inputUri: Uri,
        outputUri: Uri,
        outputFormat: VideoFormat,
        codec: VideoCodec = VideoCodec.H264,
        resolution: Resolution? = null,
        bitrate: Int? = null
    ): Flow<OperationResult> = callbackFlow {
        val operation = MediaOperation.ConvertVideo(
            inputUri = inputUri,
            outputUri = outputUri,
            outputFormat = outputFormat,
            codec = codec,
            resolution = resolution,
            bitrate = bitrate
        )
        
        val result = router.execute(operation) { progress ->
            trySend(progress)
        }
        trySend(result)
        close()
        
        awaitClose { /* Handled by engine cancellation */ }
    }
}
