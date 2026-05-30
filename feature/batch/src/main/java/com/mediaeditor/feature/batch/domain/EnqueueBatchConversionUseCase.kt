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

package com.mediaeditor.feature.batch.domain

import android.net.Uri
import com.mediaeditor.core.queue.domain.BatchJobScheduler
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.MediaOperation
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.storage.StorageManager
import java.util.UUID
import javax.inject.Inject

/**
 * Encapsulates the logic for enqueueing multiple media operations at once.
 * Each file in [inputUris] is scheduled as an independent WorkManager job via
 * [BatchJobScheduler], so failures are isolated per file and progress is tracked individually.
 */
class EnqueueBatchConversionUseCase @Inject constructor(
    private val scheduler: BatchJobScheduler,
    private val storageManager: StorageManager
) {
    sealed class Mode {
        data class AudioConversion(val format: AudioFormat, val bitrate: Int = 128) : Mode()
        data class VideoConversion(val format: VideoFormat) : Mode()
        data class ExtractAudio(val format: AudioFormat) : Mode()
    }

    /**
     * Enqueues one WorkManager job per URI, returning the list of job UUIDs.
     * The [fileName] is derived from the URI so unique job names are deterministic.
     */
    operator fun invoke(inputUris: List<Uri>, mode: Mode): List<UUID> {
        return inputUris.map { uri ->
            val baseName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "file_${System.currentTimeMillis()}"

            val (outputUri, operation) = when (mode) {
                is Mode.AudioConversion -> {
                    val ext = mode.format.extension
                    val out = storageManager.createOutputFile(baseName, ext)
                    out to MediaOperation.ConvertAudio(
                        inputUri = uri,
                        outputUri = out,
                        outputFormat = mode.format,
                        bitrate = mode.bitrate
                    )
                }
                is Mode.VideoConversion -> {
                    val ext = mode.format.extension
                    val out = storageManager.createOutputFile(baseName, ext)
                    out to MediaOperation.ConvertVideo(
                        inputUri = uri,
                        outputUri = out,
                        outputFormat = mode.format
                    )
                }
                is Mode.ExtractAudio -> {
                    val ext = mode.format.extension
                    val out = storageManager.createOutputFile(baseName, ext)
                    out to MediaOperation.ExtractAudio(
                        inputUri = uri,
                        outputUri = out,
                        outputFormat = mode.format
                    )
                }
            }

            scheduler.enqueueJob(operation, baseName)
        }
    }
}
