package com.mediaeditor.feature.converter.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.OperationResult
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.storage.StorageManager
import com.mediaeditor.feature.converter.domain.usecase.ConvertAudioUseCase
import com.mediaeditor.feature.converter.domain.usecase.ConvertVideoUseCase
import com.mediaeditor.feature.converter.domain.usecase.ExtractAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConverterState {
    object Idle : ConverterState()
    data class Processing(val progress: Int) : ConverterState()
    data class Success(val outputUri: Uri) : ConverterState()
    data class Error(val message: String) : ConverterState()
}

enum class ConversionMode {
    AUDIO_TO_AUDIO,
    VIDEO_TO_VIDEO,
    EXTRACT_AUDIO
}

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val convertAudioUseCase: ConvertAudioUseCase,
    private val convertVideoUseCase: ConvertVideoUseCase,
    private val extractAudioUseCase: ExtractAudioUseCase,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _state = MutableStateFlow<ConverterState>(ConverterState.Idle)
    val state: StateFlow<ConverterState> = _state.asStateFlow()

    private val _inputUri = MutableStateFlow<Uri?>(null)
    val inputUri: StateFlow<Uri?> = _inputUri.asStateFlow()

    private val _mode = MutableStateFlow(ConversionMode.AUDIO_TO_AUDIO)
    val mode: StateFlow<ConversionMode> = _mode.asStateFlow()

    // Audio options
    private val _audioFormat = MutableStateFlow(AudioFormat.MP3)
    val audioFormat: StateFlow<AudioFormat> = _audioFormat.asStateFlow()

    private val _audioBitrate = MutableStateFlow(128)
    val audioBitrate: StateFlow<Int> = _audioBitrate.asStateFlow()

    // Video options
    private val _videoFormat = MutableStateFlow(VideoFormat.MP4)
    val videoFormat: StateFlow<VideoFormat> = _videoFormat.asStateFlow()

    private val _videoCodec = MutableStateFlow(VideoCodec.H264)
    val videoCodec: StateFlow<VideoCodec> = _videoCodec.asStateFlow()

    fun setInputUri(uri: Uri?) {
        _inputUri.value = uri
        // Auto-detect mode based on mime type if possible, or leave it to user.
    }

    fun setMode(mode: ConversionMode) {
        _mode.value = mode
    }

    fun setAudioFormat(format: AudioFormat) {
        _audioFormat.value = format
    }

    fun setAudioBitrate(bitrate: Int) {
        _audioBitrate.value = bitrate
    }

    fun setVideoFormat(format: VideoFormat) {
        _videoFormat.value = format
    }

    fun setVideoCodec(codec: VideoCodec) {
        _videoCodec.value = codec
    }

    fun convert() {
        val input = _inputUri.value ?: return
        if (_state.value is ConverterState.Processing) return

        _state.value = ConverterState.Processing(0)

        viewModelScope.launch {
            val extension = when (_mode.value) {
                ConversionMode.AUDIO_TO_AUDIO -> _audioFormat.value.extension
                ConversionMode.VIDEO_TO_VIDEO -> _videoFormat.value.extension
                ConversionMode.EXTRACT_AUDIO -> _audioFormat.value.extension
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "converted_$timestamp"
            val outputUri = storageManager.createOutputFile(fileName, extension)

            val flow = when (_mode.value) {
                ConversionMode.AUDIO_TO_AUDIO -> {
                    convertAudioUseCase(
                        inputUri = input,
                        outputUri = outputUri,
                        outputFormat = _audioFormat.value,
                        bitrate = _audioBitrate.value
                    )
                }
                ConversionMode.VIDEO_TO_VIDEO -> {
                    convertVideoUseCase(
                        inputUri = input,
                        outputUri = outputUri,
                        outputFormat = _videoFormat.value,
                        codec = _videoCodec.value
                    )
                }
                ConversionMode.EXTRACT_AUDIO -> {
                    extractAudioUseCase(
                        inputUri = input,
                        outputUri = outputUri,
                        outputFormat = _audioFormat.value
                    )
                }
            }

            flow.collect { result ->
                when (result) {
                    is OperationResult.Progress -> {
                        _state.value = ConverterState.Processing(result.percent)
                    }
                    is OperationResult.Success -> {
                        val mimeType = when (_mode.value) {
                            ConversionMode.VIDEO_TO_VIDEO -> "video/${_videoFormat.value.extension}"
                            else -> _audioFormat.value.mimeType
                        }
                        val publishedUri = storageManager.publishToMediaStore(outputUri, "$fileName.$extension", mimeType)
                        _state.value = ConverterState.Success(publishedUri ?: outputUri)
                    }
                    is OperationResult.Failure -> {
                        _state.value = ConverterState.Error(result.error)
                    }
                }
            }
        }
    }

    fun resetState() {
        _state.value = ConverterState.Idle
    }
}
