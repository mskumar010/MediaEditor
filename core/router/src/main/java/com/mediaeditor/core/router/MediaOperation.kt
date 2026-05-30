package com.mediaeditor.core.router

import android.net.Uri

sealed class MediaOperation {
    data class TrimAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val startMs: Long,
        val endMs: Long,
        val lossless: Boolean = true,
        val fadeInMs: Long = 0,
        val fadeOutMs: Long = 0,
        val outputFormat: AudioFormat = AudioFormat.AAC
    ) : MediaOperation()

    data class TrimVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val startMs: Long,
        val endMs: Long,
        val lossless: Boolean = true,
        val fadeInMs: Long = 0,
        val fadeOutMs: Long = 0,
        val speed: Float = 1.0f,
        val rotation: Int = 0,
        val cropRect: CropRect? = null,
        val muteAudio: Boolean = false
    ) : MediaOperation()

    data class CropVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val left: Float, val top: Float,
        val right: Float, val bottom: Float // normalized 0f–1f
    ) : MediaOperation()

    data class ConvertAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: AudioFormat,
        val bitrate: Int = 128, // kbps
        val sampleRate: Int = 44100
    ) : MediaOperation()

    data class ConvertVideo(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: VideoFormat,
        val codec: VideoCodec = VideoCodec.H264,
        val resolution: Resolution? = null,
        val bitrate: Int? = null
    ) : MediaOperation()

    data class ExtractAudio(
        val inputUri: Uri,
        val outputUri: Uri,
        val outputFormat: AudioFormat = AudioFormat.MP3
    ) : MediaOperation()

    data class MergeAudio(
        val inputUris: List<Uri>,
        val outputUri: Uri,
        val outputFormat: AudioFormat = AudioFormat.AAC
    ) : MediaOperation()

    data class MergeVideo(
        val inputUris: List<Uri>,
        val outputUri: Uri
    ) : MediaOperation()

    data class ChangeSpeed(
        val inputUri: Uri,
        val outputUri: Uri,
        val speed: Float // 0.25f to 4.0f
    ) : MediaOperation()

    data class ExtractFrame(
        val inputUri: Uri,
        val outputUri: Uri,
        val timestampMs: Long
    ) : MediaOperation()
}
