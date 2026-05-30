package com.mediaeditor.core.router

interface FFmpegEngine {
    suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult
}

interface TransformerEngine {
    suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult
}
