package com.mediaeditor.feature.batch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.mediaeditor.core.queue.domain.BatchJobInfo
import com.mediaeditor.core.queue.domain.BatchJobScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val batchJobScheduler: BatchJobScheduler
) : ViewModel() {

    val batchJobs: StateFlow<List<BatchJobInfo>> = batchJobScheduler.observeJobs()
        .asFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelJob(id: UUID) {
        batchJobScheduler.cancelJob(id)
    }
}
