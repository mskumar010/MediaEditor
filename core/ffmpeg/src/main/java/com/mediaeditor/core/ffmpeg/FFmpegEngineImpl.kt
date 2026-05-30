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

package com.mediaeditor.core.ffmpeg

import android.content.Context
import android.net.Uri
import com.mediaeditor.core.router.*
import com.mediaeditor.core.storage.StorageManager
import com.mzgs.ffmpegx.FFmpeg
import com.mzgs.ffmpegx.FFmpegHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FFmpegEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager
) : FFmpegEngine {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun execute(
        operation: MediaOperation,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = withContext(Dispatchers.IO) {
        val outputUri = when (operation) {
            is MediaOperation.TrimAudio -> operation.outputUri
            is MediaOperation.ConvertAudio -> operation.outputUri
            is MediaOperation.ExtractAudio -> operation.outputUri
            is MediaOperation.MergeAudio -> operation.outputUri
            is MediaOperation.TrimVideo -> operation.outputUri
            is MediaOperation.ConvertVideo -> operation.outputUri
            is MediaOperation.ExtractFrame -> operation.outputUri
            else -> throw UnsupportedOperationException("Operation not handled by FFmpeg: $operation")
        }
        val cmd = buildCommand(operation)
        runFFmpeg(cmd, outputUri, onProgress)
    }

    private fun buildCommand(operation: MediaOperation): String = when (operation) {
        is MediaOperation.TrimAudio -> buildTrimAudioCmd(operation)
        is MediaOperation.ConvertAudio -> buildConvertAudioCmd(operation)
        is MediaOperation.ExtractAudio -> buildExtractAudioCmd(operation)
        is MediaOperation.MergeAudio -> buildMergeAudioCmd(operation)
        is MediaOperation.TrimVideo -> buildTrimVideoCmd(operation)
        is MediaOperation.ConvertVideo -> buildConvertVideoCmd(operation)
        is MediaOperation.ExtractFrame -> buildExtractFrameCmd(operation)
        else -> throw UnsupportedOperationException("Operation not handled by FFmpeg: $operation")
    }

    private fun buildTrimAudioCmd(op: MediaOperation.TrimAudio): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val startSec = op.startMs / 1000.0
        val durationSec = (op.endMs - op.startMs) / 1000.0
        val fadeIn = if (op.fadeInMs > 0) ",afade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val fadeOut = if (op.fadeOutMs > 0) ",afade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val hasFilters = fadeIn.isNotEmpty() || fadeOut.isNotEmpty()
        val audioFilter = if (hasFilters) "-af \"aresample=async=1$fadeIn$fadeOut\"" else ""
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -q:a 2"
            AudioFormat.AAC  -> "-c:a aac -b:a 192k"
            AudioFormat.FLAC -> "-c:a flac"
            AudioFormat.OGG  -> "-c:a libvorbis -q:a 5"
            AudioFormat.OPUS -> "-c:a libopus -b:a 128k"
            AudioFormat.WAV  -> "-c:a pcm_s16le"
        }
        return "-y -ss $startSec -i \"$inputPath\" -t $durationSec $audioFilter $codec \"$outputPath\""
    }

    private fun buildTrimVideoCmd(op: MediaOperation.TrimVideo): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val startSec = op.startMs / 1000.0
        val durationSec = (op.endMs - op.startMs) / 1000.0
        if (op.lossless) {
            return "-y -ss $startSec -i \"$inputPath\" -t $durationSec -c copy -avoid_negative_ts make_zero \"$outputPath\""
        }
        val fadeIn = if (op.fadeInMs > 0) ",fade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val fadeOut = if (op.fadeOutMs > 0) ",fade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val vf = if (fadeIn.isNotEmpty() || fadeOut.isNotEmpty()) "-vf \"null$fadeIn$fadeOut\"" else ""
        val afadeIn = if (op.fadeInMs > 0) ",afade=t=in:st=0:d=${op.fadeInMs/1000.0}" else ""
        val afadeOut = if (op.fadeOutMs > 0) ",afade=t=out:st=${(durationSec - op.fadeOutMs/1000.0)}:d=${op.fadeOutMs/1000.0}" else ""
        val af = if (afadeIn.isNotEmpty() || afadeOut.isNotEmpty()) "-af \"aresample=async=1$afadeIn$afadeOut\"" else ""
        return "-y -ss $startSec -i \"$inputPath\" -t $durationSec $vf $af -c:v libx264 -preset fast -crf 22 -c:a aac \"$outputPath\""
    }

    private fun buildConvertAudioCmd(op: MediaOperation.ConvertAudio): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -b:a ${op.bitrate}k"
            AudioFormat.AAC  -> "-c:a aac -b:a ${op.bitrate}k"
            AudioFormat.FLAC -> "-c:a flac"
            AudioFormat.OGG  -> "-c:a libvorbis -q:a 5"
            AudioFormat.OPUS -> "-c:a libopus -b:a ${op.bitrate}k"
            AudioFormat.WAV  -> "-c:a pcm_s16le"
        }
        return "-y -i \"$inputPath\" -ar ${op.sampleRate} $codec \"$outputPath\""
    }

    private fun buildExtractAudioCmd(op: MediaOperation.ExtractAudio): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val codec = when (op.outputFormat) {
            AudioFormat.MP3  -> "-c:a libmp3lame -q:a 2"
            AudioFormat.AAC  -> "-c:a aac -b:a 192k"
            AudioFormat.FLAC -> "-c:a flac"
            else             -> "-c:a aac -b:a 192k"
        }
        return "-y -i \"$inputPath\" -vn $codec \"$outputPath\""
    }

    private fun buildMergeAudioCmd(op: MediaOperation.MergeAudio): String {
        val outputScan = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        val inputs = op.inputUris.joinToString(" ") { 
            val path = storageManager.getRealPath(it) ?: it.toString()
            "-i \"$path\"" 
        }
        val filter = (0 until op.inputUris.size).joinToString("") { "[$it:a]" }
        val codec = when (op.outputFormat) {
            AudioFormat.MP3 -> "-c:a libmp3lame -q:a 2"
            AudioFormat.FLAC -> "-c:a flac"
            else -> "-c:a aac -b:a 192k"
        }
        return "$inputs -y -filter_complex \"${filter}concat=n=${op.inputUris.size}:v=0:a=1[a]\" -map \"[a]\" $codec \"$outputScan\""
    }

    private fun buildConvertVideoCmd(op: MediaOperation.ConvertVideo): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val vcodec = when (op.codec) {
            VideoCodec.H264 -> "-c:v libx264 -preset fast -crf 22"
            VideoCodec.H265 -> "-c:v libx265 -preset fast -crf 24"
            VideoCodec.VP8  -> "-c:v libvpx -b:v ${op.bitrate ?: 1500}k"
            VideoCodec.VP9  -> "-c:v libvpx-vp9 -b:v ${op.bitrate ?: 1500}k"
        }
        val scale = op.resolution?.let { "-vf scale=${it.width}:${it.height}" } ?: ""
        return "-y -i \"$inputPath\" $scale $vcodec -c:a aac -b:a 128k \"$outputPath\""
    }

    private fun buildExtractFrameCmd(op: MediaOperation.ExtractFrame): String {
        val inputPath = storageManager.getRealPath(op.inputUri) ?: op.inputUri.toString()
        val outputPath = storageManager.getRealPath(op.outputUri) ?: op.outputUri.toString()
        
        val sec = op.timestampMs / 1000.0
        return "-y -ss $sec -i \"$inputPath\" -frames:v 1 -q:v 2 \"$outputPath\""
    }

    private suspend fun runFFmpeg(
        command: String,
        outputUri: Uri,
        onProgress: (OperationResult.Progress) -> Unit
    ): OperationResult = suspendCancellableCoroutine { continuation ->
        
        val ffmpeg = FFmpeg.initialize(context)
        
        val job = scope.launch {
            ffmpeg.execute(command, object : FFmpegHelper.FFmpegCallback {
                override fun onStart() {
                    // Not needed
                }

                override fun onProgress(progress: Float, time: Long) {
                    onProgress(OperationResult.Progress(progress.toInt()))
                }

                override fun onOutput(line: String) {
                    // Log or handle raw output if needed
                }

                override fun onSuccess(output: String?) {
                    if (continuation.isActive) {
                        continuation.resume(OperationResult.Success(outputUri, 0L))
                    }
                }

                override fun onFailure(error: String) {
                    if (continuation.isActive) {
                        val userReadableError = error.split('\n')
                            .lastOrNull { it.contains("Error", ignoreCase = true) || it.contains("Invalid", ignoreCase = true) }
                            ?.trim() ?: "FFmpeg conversion failed"
                        continuation.resume(OperationResult.Failure(userReadableError))
                    }
                }

                override fun onFinish() {
                    // Not needed
                }
            })
        }

        continuation.invokeOnCancellation {
            job.cancel()
            ffmpeg.cancelAll() // Fallback to cancelAll since we don't have sessionId here yet
        }
    }
}
