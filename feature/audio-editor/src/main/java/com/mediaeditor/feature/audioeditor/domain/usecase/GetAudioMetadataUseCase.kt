package com.mediaeditor.feature.audioeditor.domain.usecase

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mediaeditor.core.router.AudioFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAudioMetadataUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AudioMetadata(
        val durationMs: Long,
        val format: AudioFormat
    )

    suspend operator fun invoke(uri: Uri): AudioMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            
            val format = when {
                mimeType?.contains("mpeg") == true -> AudioFormat.MP3
                mimeType?.contains("flac") == true -> AudioFormat.FLAC
                mimeType?.contains("ogg") == true -> AudioFormat.OGG
                mimeType?.contains("opus") == true -> AudioFormat.OPUS
                mimeType?.contains("wav") == true -> AudioFormat.WAV
                else -> AudioFormat.AAC // Default to AAC/M4A for others
            }
            
            AudioMetadata(duration, format)
        } catch (e: Exception) {
            AudioMetadata(0L, AudioFormat.AAC)
        } finally {
            retriever.release()
        }
    }
}
