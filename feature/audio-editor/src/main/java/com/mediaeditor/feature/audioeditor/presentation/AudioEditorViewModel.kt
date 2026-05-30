package com.mediaeditor.feature.audioeditor.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.AudioProject
import com.mediaeditor.core.router.MediaOperation
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.storage.StorageManager
import com.mediaeditor.feature.audioeditor.domain.usecase.GetAudioMetadataUseCase
import com.mediaeditor.feature.audioeditor.domain.usecase.TrimAudioUseCase
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
class AudioEditorViewModel @Inject constructor(
    val player: ExoPlayer,
    private val trimAudioUseCase: TrimAudioUseCase,
    private val getAudioMetadataUseCase: GetAudioMetadataUseCase,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _project = MutableStateFlow<AudioProject?>(null)
    val project: StateFlow<AudioProject?> = _project.asStateFlow()

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
                    _project.update { it?.copy(playbackPositionMs = p.trimStartMs) }
                }
            }
        })
    }

    fun loadAudio(uri: Uri) {
        viewModelScope.launch {
            val metadata = getAudioMetadataUseCase(uri)
            val realPath = storageManager.getRealPath(uri)
            _project.value = AudioProject(
                sourceUri = uri,
                sourcePath = realPath,
                durationMs = metadata.durationMs,
                trimStartMs = 0,
                trimEndMs = metadata.durationMs,
                outputFormat = metadata.format
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
        _project.update { it?.copy(playbackPositionMs = positionMs) }
    }

    fun updateTrimPoints(startMs: Long, endMs: Long) {
        val oldStart = _project.value?.trimStartMs ?: 0L
        _project.update { it?.copy(trimStartMs = startMs, trimEndMs = endMs) }
        if (startMs != oldStart) {
            seekTo(startMs)
        }
    }

    fun updateFadePoints(fadeInMs: Long, fadeOutMs: Long) {
        _project.update { it?.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs) }
    }

    fun updateFormat(format: AudioFormat) {
        _project.update { it?.copy(outputFormat = format) }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val currentMs = player.currentPosition
                val p = _project.value ?: break
                
                // Stop if we hit the trim end
                if (currentMs >= p.trimEndMs) {
                    player.pause()
                    player.seekTo(p.trimStartMs)
                    _project.update { it?.copy(playbackPositionMs = p.trimStartMs) }
                    break
                }
                
                _project.update { it?.copy(playbackPositionMs = currentMs) }
                delay(30) // ~30fps update rate
            }
        }
    }

    fun exportAudio() {
        val p = _project.value ?: return
        if (_exportState.value is ExportState.Exporting) return

        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(0)
            
            // Create a temp file in cache to write to
            val outputUri = storageManager.createOutputFile(
                name = "MediaEditor_Audio_${System.currentTimeMillis()}",
                extension = p.outputFormat.extension
            )

            val op = MediaOperation.TrimAudio(
                inputUri = p.sourceUri,
                outputUri = outputUri,
                startMs = p.trimStartMs,
                endMs = p.trimEndMs,
                fadeInMs = p.fadeInMs,
                fadeOutMs = p.fadeOutMs,
                outputFormat = p.outputFormat
            )

            val result = trimAudioUseCase(op) { progress ->
                _exportState.value = ExportState.Exporting(progress.percent)
            }

            when (result) {
                is OperationResult.Success -> {
                    // Move to MediaStore
                    val publishedUri = storageManager.publishToMediaStore(
                        sourceUri = outputUri,
                        name = "MediaEditor_Audio_${System.currentTimeMillis()}.${p.outputFormat.extension}",
                        mimeType = p.outputFormat.mimeType
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
    }
}

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val progress: Int) : ExportState()
    object Success : ExportState()
    data class Error(val message: String) : ExportState()
}
