package com.mediaeditor.core.queue

import android.net.Uri
import com.mediaeditor.core.router.*
import org.json.JSONArray
import org.json.JSONObject

object MediaOperationSerializer {

    fun serialize(operation: MediaOperation): String {
        val json = JSONObject()
        when (operation) {
            is MediaOperation.TrimAudio -> {
                json.put("type", "TrimAudio")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("startMs", operation.startMs)
                json.put("endMs", operation.endMs)
                json.put("lossless", operation.lossless)
                json.put("fadeInMs", operation.fadeInMs)
                json.put("fadeOutMs", operation.fadeOutMs)
                json.put("outputFormat", operation.outputFormat.name)
            }
            is MediaOperation.ConvertAudio -> {
                json.put("type", "ConvertAudio")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("outputFormat", operation.outputFormat.name)
                json.put("bitrate", operation.bitrate)
                json.put("sampleRate", operation.sampleRate)
            }
            is MediaOperation.ExtractAudio -> {
                json.put("type", "ExtractAudio")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("outputFormat", operation.outputFormat.name)
            }
            is MediaOperation.ConvertVideo -> {
                json.put("type", "ConvertVideo")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("outputFormat", operation.outputFormat.name)
                json.put("codec", operation.codec.name)
                operation.resolution?.let {
                    val res = JSONObject()
                    res.put("width", it.width)
                    res.put("height", it.height)
                    json.put("resolution", res)
                }
                operation.bitrate?.let { json.put("bitrate", it) }
            }
            is MediaOperation.TrimVideo -> {
                json.put("type", "TrimVideo")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("startMs", operation.startMs)
                json.put("endMs", operation.endMs)
                json.put("lossless", operation.lossless)
                json.put("fadeInMs", operation.fadeInMs)
                json.put("fadeOutMs", operation.fadeOutMs)
            }
            is MediaOperation.MergeAudio -> {
                json.put("type", "MergeAudio")
                json.put("outputUri", operation.outputUri.toString())
                json.put("outputFormat", operation.outputFormat.name)
                val array = JSONArray()
                operation.inputUris.forEach { array.put(it.toString()) }
                json.put("inputUris", array)
            }
            is MediaOperation.MergeVideo -> {
                json.put("type", "MergeVideo")
                json.put("outputUri", operation.outputUri.toString())
                val array = JSONArray()
                operation.inputUris.forEach { array.put(it.toString()) }
                json.put("inputUris", array)
            }
            is MediaOperation.CropVideo -> {
                json.put("type", "CropVideo")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("left", operation.left)
                json.put("top", operation.top)
                json.put("right", operation.right)
                json.put("bottom", operation.bottom)
            }
            is MediaOperation.ChangeSpeed -> {
                json.put("type", "ChangeSpeed")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("speed", operation.speed)
            }
            is MediaOperation.ExtractFrame -> {
                json.put("type", "ExtractFrame")
                json.put("inputUri", operation.inputUri.toString())
                json.put("outputUri", operation.outputUri.toString())
                json.put("timestampMs", operation.timestampMs)
            }
        }
        return json.toString()
    }

    fun deserialize(jsonString: String): MediaOperation? {
        try {
            val json = JSONObject(jsonString)
            return when (json.getString("type")) {
                "TrimAudio" -> MediaOperation.TrimAudio(
                    inputUri = Uri.parse(json.getString("inputUri")),
                    outputUri = Uri.parse(json.getString("outputUri")),
                    startMs = json.getLong("startMs"),
                    endMs = json.getLong("endMs"),
                    lossless = json.getBoolean("lossless"),
                    fadeInMs = json.getLong("fadeInMs"),
                    fadeOutMs = json.getLong("fadeOutMs"),
                    outputFormat = AudioFormat.valueOf(json.getString("outputFormat"))
                )
                "ConvertAudio" -> MediaOperation.ConvertAudio(
                    inputUri = Uri.parse(json.getString("inputUri")),
                    outputUri = Uri.parse(json.getString("outputUri")),
                    outputFormat = AudioFormat.valueOf(json.getString("outputFormat")),
                    bitrate = json.getInt("bitrate"),
                    sampleRate = json.getInt("sampleRate")
                )
                "ExtractAudio" -> MediaOperation.ExtractAudio(
                    inputUri = Uri.parse(json.getString("inputUri")),
                    outputUri = Uri.parse(json.getString("outputUri")),
                    outputFormat = AudioFormat.valueOf(json.getString("outputFormat"))
                )
                "ConvertVideo" -> MediaOperation.ConvertVideo(
                    inputUri = Uri.parse(json.getString("inputUri")),
                    outputUri = Uri.parse(json.getString("outputUri")),
                    outputFormat = VideoFormat.valueOf(json.getString("outputFormat")),
                    codec = VideoCodec.valueOf(json.getString("codec")),
                    resolution = if (json.has("resolution")) {
                        val res = json.getJSONObject("resolution")
                        Resolution(res.getInt("width"), res.getInt("height"))
                    } else null,
                    bitrate = if (json.has("bitrate")) json.getInt("bitrate") else null
                )
                "TrimVideo" -> MediaOperation.TrimVideo(
                    inputUri = Uri.parse(json.getString("inputUri")),
                    outputUri = Uri.parse(json.getString("outputUri")),
                    startMs = json.getLong("startMs"),
                    endMs = json.getLong("endMs"),
                    lossless = json.getBoolean("lossless"),
                    fadeInMs = json.getLong("fadeInMs"),
                    fadeOutMs = json.getLong("fadeOutMs")
                )
                "BatchOperation" -> null // Deprecated; batching handled by WorkManager
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
