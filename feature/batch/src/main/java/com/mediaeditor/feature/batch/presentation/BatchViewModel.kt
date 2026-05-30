/*
 * Media Editor — FOSS offline Android media editor
 * Copyright (C) 2025 The Media Editor Open Source Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mediaeditor.feature.batch.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.mediaeditor.core.queue.domain.BatchJobInfo
import com.mediaeditor.core.queue.domain.BatchJobScheduler
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.feature.batch.domain.EnqueueBatchConversionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class BatchMode {
    object AudioConversion : BatchMode()
    object VideoConversion : BatchMode()
    object ExtractAudio : BatchMode()
}

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val batchJobScheduler: BatchJobScheduler,
    private val enqueueBatchConversionUseCase: EnqueueBatchConversionUseCase
) : ViewModel() {

    val batchJobs: StateFlow<List<BatchJobInfo>> = batchJobScheduler.observeJobs()
        .asFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedUris: StateFlow<List<Uri>> = _selectedUris.asStateFlow()

    private val _mode = MutableStateFlow<BatchMode>(BatchMode.VideoConversion)
    val mode: StateFlow<BatchMode> = _mode.asStateFlow()

    private val _audioFormat = MutableStateFlow(AudioFormat.MP3)
    val audioFormat: StateFlow<AudioFormat> = _audioFormat.asStateFlow()

    private val _videoFormat = MutableStateFlow(VideoFormat.MP4)
    val videoFormat: StateFlow<VideoFormat> = _videoFormat.asStateFlow()

    fun setSelectedUris(uris: List<Uri>) {
        _selectedUris.value = uris
    }

    fun setMode(mode: BatchMode) {
        _mode.value = mode
    }

    fun setAudioFormat(format: AudioFormat) {
        _audioFormat.value = format
    }

    fun setVideoFormat(format: VideoFormat) {
        _videoFormat.value = format
    }

    fun enqueueSelected() {
        val uris = _selectedUris.value
        if (uris.isEmpty()) return

        val useCaseMode = when (_mode.value) {
            is BatchMode.AudioConversion -> EnqueueBatchConversionUseCase.Mode.AudioConversion(_audioFormat.value)
            is BatchMode.VideoConversion -> EnqueueBatchConversionUseCase.Mode.VideoConversion(_videoFormat.value)
            is BatchMode.ExtractAudio -> EnqueueBatchConversionUseCase.Mode.ExtractAudio(_audioFormat.value)
        }

        viewModelScope.launch {
            enqueueBatchConversionUseCase(uris, useCaseMode)
            // Clear selection after enqueue so the UI returns to idle
            _selectedUris.update { emptyList() }
        }
    }

    fun cancelJob(id: UUID) {
        batchJobScheduler.cancelJob(id)
    }
}
