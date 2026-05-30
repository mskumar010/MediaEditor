package com.mediaeditor.feature.converter.domain.usecase

import android.net.Uri
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.MediaOperation
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.router.ProcessingRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class ConvertAudioUseCase @Inject constructor(
    private val router: ProcessingRouter
) {
    operator fun invoke(
        inputUri: Uri,
        outputUri: Uri,
        outputFormat: AudioFormat,
        bitrate: Int = 128,
        sampleRate: Int = 44100
    ): Flow<OperationResult> = callbackFlow {
        val operation = MediaOperation.ConvertAudio(
            inputUri = inputUri,
            outputUri = outputUri,
            outputFormat = outputFormat,
            bitrate = bitrate,
            sampleRate = sampleRate
        )
        
        val result = router.execute(operation) { progress ->
            trySend(progress)
        }
        trySend(result)
        close()
        
        awaitClose { /* Handled by engine cancellation */ }
    }
}
