package com.mediaeditor.core.waveform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface AmplitudaEngine {
    suspend fun extractAmplitudes(sourcePath: String): Result<List<Int>>
}

@Singleton
class AmplitudaEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AmplitudaEngine {
    
    override suspend fun extractAmplitudes(sourcePath: String): Result<List<Int>> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val file = File(sourcePath)
                if (!file.exists()) {
                    continuation.resume(Result.failure(Exception("File not found")))
                    return@suspendCancellableCoroutine
                }
                
                val amplituda = Amplituda(context)
                val isLargeFile = file.length() > 100 * 1024 * 1024
                
                val request = if (isLargeFile) {
                    amplituda.processAudio(sourcePath, Compress.withParams(Compress.AVERAGE, 500))
                } else {
                    amplituda.processAudio(sourcePath, Compress.withParams(Compress.AVERAGE, 100))
                }
                
                request.get({ data ->
                    if (continuation.isActive) {
                        continuation.resume(Result.success(data.amplitudesAsList()))
                    }
                }, { error ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                })
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
}
