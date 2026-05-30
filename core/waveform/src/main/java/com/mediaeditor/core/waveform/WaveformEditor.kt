package com.mediaeditor.core.waveform

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress

import java.io.File

@Composable
fun WaveformEditor(
    sourcePath: String?,
    trimStartMs: Long,
    trimEndMs: Long,
    totalDurationMs: Long,
    playbackPositionMs: Long,
    fadeInMs: Long = 0,
    fadeOutMs: Long = 0,
    onTrimStartChange: (Long) -> Unit,
    onTrimEndChange: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var amplitudes by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(sourcePath) {
        if (sourcePath == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val file = File(sourcePath)
            if (!file.exists()) return@withContext
            
            val amplituda = Amplituda(context)
            val isLargeFile = file.length() > 100 * 1024 * 1024
            
            val request = if (isLargeFile) {
                amplituda.processAudio(sourcePath, Compress.withParams(Compress.AVERAGE, 500))
            } else {
                amplituda.processAudio(sourcePath, Compress.withParams(Compress.AVERAGE, 100))
            }

            request.get({ data ->
                    amplitudes = data.amplitudesAsList()
                }, {
                    // Ignore errors for now
                })
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val playheadColor = MaterialTheme.colorScheme.error

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(totalDurationMs) {
            detectDragGestures { change, _ ->
                val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                val newMs = (progress * totalDurationMs).toLong()
                onSeek(newMs)
            }
        }) {
            if (amplitudes.isEmpty() || totalDurationMs == 0L) return@Canvas

            val barWidth = 4.dp.toPx()
            val space = 2.dp.toPx()
            
            val totalBars = (width / (barWidth + space)).toInt()
            if (totalBars <= 0) return@Canvas

            val step = kotlin.math.max(1, amplitudes.size / totalBars)
            
            for (i in 0 until totalBars) {
                val dataIndex = i * step
                if (dataIndex >= amplitudes.size) break
                
                // Average the chunk
                var sum = 0
                var count = 0
                for (j in 0 until step) {
                    if (dataIndex + j < amplitudes.size) {
                        sum += amplitudes[dataIndex + j]
                        count++
                    }
                }
                val avgAmp = if (count > 0) sum / count else 0
                
                val normalizedAmp = (avgAmp / 100f).coerceIn(0.1f, 1f)
                val barHeight = height * normalizedAmp
                
                val x = i * (barWidth + space)
                val timeAtBar = ((x / width) * totalDurationMs).toLong()
                
                val color = if (timeAtBar in trimStartMs..trimEndMs) {
                    primaryColor
                } else {
                    trackColor
                }

                // Fade Overlay
                val alpha = when {
                    timeAtBar in trimStartMs..(trimStartMs + fadeInMs) && fadeInMs > 0 -> {
                        (timeAtBar - trimStartMs).toFloat() / fadeInMs
                    }
                    timeAtBar in (trimEndMs - fadeOutMs)..trimEndMs && fadeOutMs > 0 -> {
                        (trimEndMs - timeAtBar).toFloat() / fadeOutMs
                    }
                    else -> 1f
                }

                drawLine(
                    color = color.copy(alpha = if (timeAtBar in trimStartMs..trimEndMs) alpha else 1f),
                    start = Offset(x, height / 2 - barHeight / 2),
                    end = Offset(x, height / 2 + barHeight / 2),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }

            // Draw Playhead
            val playheadX = (playbackPositionMs.toFloat() / totalDurationMs.coerceAtLeast(1)) * width
            drawLine(
                color = playheadColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Trim Handles with 48dp touch targets
        val startX = (trimStartMs.toFloat() / totalDurationMs.coerceAtLeast(1)) * width
        val endX = (trimEndMs.toFloat() / totalDurationMs.coerceAtLeast(1)) * width

        // Start Handle
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 120.dp)
                .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (startX - 24.dp.toPx()).toDp() })
                .pointerInput(totalDurationMs) {
                    detectDragGestures { change, _ ->
                        val newProgress = (change.position.x + startX - 24.dp.toPx()) / width
                        val newMs = (newProgress * totalDurationMs).toLong().coerceIn(0L, trimEndMs)
                        onTrimStartChange(newMs)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = primaryColor,
                    start = Offset(24.dp.toPx(), 0f),
                    end = Offset(24.dp.toPx(), height),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        // End Handle
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 120.dp)
                .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (endX - 24.dp.toPx()).toDp() })
                .pointerInput(totalDurationMs) {
                    detectDragGestures { change, _ ->
                        val newProgress = (change.position.x + endX - 24.dp.toPx()) / width
                        val newMs = (newProgress * totalDurationMs).toLong().coerceIn(trimStartMs, totalDurationMs)
                        onTrimEndChange(newMs)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = primaryColor,
                    start = Offset(24.dp.toPx(), 0f),
                    end = Offset(24.dp.toPx(), height),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }
    }
}
