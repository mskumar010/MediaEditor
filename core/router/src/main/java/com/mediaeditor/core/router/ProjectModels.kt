package com.mediaeditor.core.router

import android.net.Uri

data class AudioProject(
    val sourceUri: Uri,
    val sourcePath: String? = null,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val volume: Float = 1.0f,
    val outputFormat: AudioFormat = AudioFormat.AAC,
    val playbackPositionMs: Long = 0
)

data class VideoProject(
    val sourceUri: Uri,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val cropRect: CropRect = CropRect.FULL,
    val rotation: Int = 0, // 0, 90, 180, 270
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val speed: Float = 1.0f,
    val muteAudio: Boolean = false,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val outputFormat: VideoFormat = VideoFormat.MP4,
    val resolution: Resolution? = null
)

enum class AudioFormat(val extension: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    AAC("m4a", "audio/mp4"),
    FLAC("flac", "audio/flac"),
    OGG("ogg", "audio/ogg"),
    OPUS("opus", "audio/opus"),
    WAV("wav", "audio/wav")
}

enum class VideoFormat(val extension: String) {
    MP4("mp4"), MKV("mkv"), WEBM("webm"),
    AVI("avi"), MOV("mov"), FLV("flv")
}

enum class VideoCodec { H264, H265, VP8, VP9 }

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    companion object { val FULL = CropRect() }
}

data class Resolution(val width: Int, val height: Int) {
    companion object {
        val P2160 = Resolution(3840, 2160)
        val P1080 = Resolution(1920, 1080)
        val P720  = Resolution(1280, 720)
        val P480  = Resolution(854, 480)
        val P360  = Resolution(640, 360)
    }
}

sealed class OperationResult {
    data class Success(val outputUri: Uri, val durationMs: Long) : OperationResult()
    data class Failure(val error: String, val cause: Throwable? = null) : OperationResult()
    data class Progress(val percent: Int, val message: String = "") : OperationResult()
}
