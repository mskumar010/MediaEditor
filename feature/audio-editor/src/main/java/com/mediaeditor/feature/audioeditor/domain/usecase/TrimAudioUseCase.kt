package com.mediaeditor.feature.audioeditor.domain.usecase

import com.mediaeditor.core.router.MediaOperation
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.router.ProcessingRouter
import javax.inject.Inject

class TrimAudioUseCase @Inject constructor(
    private val router: ProcessingRouter
) {
    suspend operator fun invoke(
        operation: MediaOperation.TrimAudio,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult {
        return router.execute(operation, onProgress)
    }
}
