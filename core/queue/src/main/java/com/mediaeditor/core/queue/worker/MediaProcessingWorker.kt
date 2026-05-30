package com.mediaeditor.core.queue.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mediaeditor.core.queue.MediaOperationSerializer
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.router.ProcessingRouter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class MediaProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processingRouter: ProcessingRouter
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val opJson = inputData.getString(KEY_OPERATION_JSON) ?: return@withContext Result.failure()
        val operation = MediaOperationSerializer.deserialize(opJson) ?: return@withContext Result.failure()

        try {
            val result = processingRouter.execute(operation) { progress ->
                setProgressAsync(workDataOf(KEY_PROGRESS to progress.percent))
            }

            when (result) {
                is OperationResult.Success -> {
                    val outputData = workDataOf(KEY_OUTPUT_URI to result.outputUri.toString())
                    Result.success(outputData)
                }
                is OperationResult.Failure -> {
                    Result.failure(workDataOf(KEY_ERROR to result.error))
                }
                else -> Result.failure()
            }
        } catch (e: CancellationException) {
            // Handled by WorkManager, we just rethrow so coroutine framework cleans up
            throw e
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val KEY_OPERATION_JSON = "operation_json"
        const val KEY_PROGRESS = "progress"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ERROR = "error"
    }
}
