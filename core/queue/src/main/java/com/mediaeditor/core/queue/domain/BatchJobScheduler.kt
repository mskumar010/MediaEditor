package com.mediaeditor.core.queue.domain

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import com.mediaeditor.core.queue.MediaOperationSerializer
import com.mediaeditor.core.queue.worker.MediaProcessingWorker
import com.mediaeditor.core.router.MediaOperation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface BatchJobScheduler {
    fun enqueueJob(operation: MediaOperation, fileName: String): UUID
    fun cancelJob(id: UUID)
    fun observeJobs(): LiveData<List<BatchJobInfo>>
}

data class BatchJobInfo(
    val id: UUID,
    val state: WorkInfo.State,
    val progress: Int,
    val outputUri: String?,
    val error: String?
)

@Singleton
class WorkManagerBatchJobScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : BatchJobScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun enqueueJob(operation: MediaOperation, fileName: String): UUID {
        val opJson = MediaOperationSerializer.serialize(operation)
        
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaProcessingWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(MediaProcessingWorker.KEY_OPERATION_JSON to opJson))
            .addTag("BATCH_JOB")
            .build()

        // Deterministic unique name to survive rotation
        val uniqueName = "operation_${fileName}_${System.currentTimeMillis()}"
        
        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )

        return request.id
    }

    override fun cancelJob(id: UUID) {
        workManager.cancelWorkById(id)
    }

    override fun observeJobs(): LiveData<List<BatchJobInfo>> {
        return workManager.getWorkInfosByTagLiveData("BATCH_JOB").map { workInfos ->
            workInfos.map { info ->
                BatchJobInfo(
                    id = info.id,
                    state = info.state,
                    progress = info.progress.getInt(MediaProcessingWorker.KEY_PROGRESS, 0),
                    outputUri = info.outputData.getString(MediaProcessingWorker.KEY_OUTPUT_URI),
                    error = info.outputData.getString(MediaProcessingWorker.KEY_ERROR)
                )
            }
        }
    }
}
