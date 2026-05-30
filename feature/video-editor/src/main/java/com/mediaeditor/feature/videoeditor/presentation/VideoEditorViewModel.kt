package com.mediaeditor.feature.videoeditor.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mediaeditor.core.router.*
import com.mediaeditor.core.storage.StorageManager
import com.mediaeditor.feature.videoeditor.domain.usecase.GetVideoMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    val player: ExoPlayer,
    private val getVideoMetadataUseCase: GetVideoMetadataUseCase,
    private val processingRouter: ProcessingRouter,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _project = MutableStateFlow<VideoProject?>(null)
    val project: StateFlow<VideoProject?> = _project.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var progressJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    val p = _project.value ?: return
                    player.seekTo(p.trimStartMs)
                }
            }
        })
    }

    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            val metadata = getVideoMetadataUseCase(uri)
            _project.value = VideoProject(
                sourceUri = uri,
                durationMs = metadata.durationMs,
                trimStartMs = 0,
                trimEndMs = metadata.durationMs,
                outputFormat = metadata.format,
                resolution = metadata.resolution
            )
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            player.pause()
        } else {
            val p = _project.value ?: return
            if (player.currentPosition >= p.trimEndMs) {
                player.seekTo(p.trimStartMs)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun updateTrimPoints(startMs: Long, endMs: Long) {
        _project.update { it?.copy(trimStartMs = startMs, trimEndMs = endMs) }
        if (player.currentPosition < startMs || player.currentPosition > endMs) {
            seekTo(startMs)
        }
    }

    fun updateSpeed(speed: Float) {
        _project.update { it?.copy(speed = speed) }
        player.setPlaybackSpeed(speed)
    }

    fun updateRotation() {
        _project.update { 
            val currentRotation = it?.rotation ?: 0
            val newRotation = (currentRotation + 90) % 360
            it?.copy(rotation = newRotation)
        }
    }

    fun updateFormat(format: VideoFormat) {
        _project.update { it?.copy(outputFormat = format) }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val currentMs = player.currentPosition
                val p = _project.value ?: break
                if (currentMs >= p.trimEndMs) {
                    player.pause()
                    player.seekTo(p.trimStartMs)
                    break
                }
                delay(30)
            }
        }
    }

    fun exportVideo() {
        val p = _project.value ?: return
        if (_exportState.value is ExportState.Exporting) return

        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(0)
            
            val outputUri = storageManager.createOutputFile(
                name = "MediaEditor_Video_${System.currentTimeMillis()}",
                extension = p.outputFormat.extension
            )

            // Media3 Transformer MUST start on Main thread.
            // Explicitly using Dispatchers.Main here as per GEMINI.md constraints.
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                // Simplification: We'll wrap the project settings into a TrimVideo operation
                // In a full implementation, we'd handle Crop/Speed/Rotate as combined operations
                val op = MediaOperation.TrimVideo(
                    inputUri = p.sourceUri,
                    outputUri = outputUri,
                    startMs = p.trimStartMs,
                    endMs = p.trimEndMs,
                    fadeInMs = p.fadeInMs,
                    fadeOutMs = p.fadeOutMs,
                    speed = p.speed,
                    rotation = p.rotation,
                    cropRect = p.cropRect,
                    muteAudio = p.muteAudio
                )

                processingRouter.execute(op) { progress ->
                    _exportState.value = ExportState.Exporting(progress.percent)
                }
            }

            when (result) {
                is OperationResult.Success -> {
                    storageManager.publishToMediaStore(
                        sourceUri = outputUri,
                        name = "MediaEditor_Video_${System.currentTimeMillis()}.${p.outputFormat.extension}",
                        mimeType = "video/${p.outputFormat.extension}"
                    )
                    _exportState.value = ExportState.Success
                }
                is OperationResult.Failure -> {
                    _exportState.value = ExportState.Error(result.error)
                }
                else -> {}
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}

sealed class ExportState {
    data object Idle : ExportState()
    data class Exporting(val progress: Int) : ExportState()
    data object Success : ExportState()
    data class Error(val message: String) : ExportState()
}
