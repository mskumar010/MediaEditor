package com.mediaeditor.feature.videoeditor.domain.usecase

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mediaeditor.core.router.Resolution
import com.mediaeditor.core.router.VideoFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetVideoMetadataUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class VideoMetadata(
        val durationMs: Long,
        val resolution: Resolution,
        val format: VideoFormat
    )

    suspend operator fun invoke(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            val format = when {
                mimeType?.contains("video/mp4") == true -> VideoFormat.MP4
                mimeType?.contains("video/x-matroska") == true -> VideoFormat.MKV
                mimeType?.contains("video/webm") == true -> VideoFormat.WEBM
                mimeType?.contains("video/x-msvideo") == true -> VideoFormat.AVI
                mimeType?.contains("video/quicktime") == true -> VideoFormat.MOV
                else -> VideoFormat.MP4
            }

            VideoMetadata(duration, Resolution(width, height), format)
        } catch (e: Exception) {
            VideoMetadata(0L, Resolution(0, 0), VideoFormat.MP4)
        } finally {
            retriever.release()
        }
    }
}
