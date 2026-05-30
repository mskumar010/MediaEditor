package com.mediaeditor.feature.audioeditor.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.ui.components.HybridCard
import com.mediaeditor.core.ui.components.HybridPrimaryButton
import com.mediaeditor.core.ui.components.HybridSectionHeader
import com.mediaeditor.core.ui.theme.HybridTheme
import com.mediaeditor.core.waveform.WaveformEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioEditorScreen(
    uri: Uri?,
    onNavigateBack: () -> Unit,
    viewModel: AudioEditorViewModel = hiltViewModel()
) {
    val project by viewModel.project.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    LaunchedEffect(uri) {
        if (uri != null) {
            viewModel.loadAudio(uri)
        }
    }

    DisposableEffect(viewModel.player) {
        onDispose {
            viewModel.player.stop()
            viewModel.player.release()
        }
    }

    if (exportState is ExportState.Exporting) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            HybridCard(modifier = Modifier.width(200.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Exporting... ${(exportState as ExportState.Exporting).progress}%")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            project?.let { proj ->
                // Waveform Section
                HybridSectionHeader(title = "Waveform & Trim")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    WaveformEditor(
                        amplitudes = proj.amplitudes,
                        trimStartMs = proj.trimStartMs,
                        trimEndMs = proj.trimEndMs,
                        totalDurationMs = proj.durationMs,
                        playbackPositionMs = proj.playbackPositionMs,
                        fadeInMs = proj.fadeInMs,
                        fadeOutMs = proj.fadeOutMs,
                        onTrimStartChange = { newStart -> viewModel.updateTrimPoints(newStart, proj.trimEndMs) },
                        onTrimEndChange = { newEnd -> viewModel.updateTrimPoints(proj.trimStartMs, newEnd) },
                        onSeek = { ms -> viewModel.seekTo(ms) },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(proj.playbackPositionMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        FilledIconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = formatTime(proj.durationMs),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Fade Effects Section
                HybridSectionHeader(title = "Fade Effects")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FadeControl(
                        label = "Fade In",
                        valueMs = proj.fadeInMs,
                        maxMs = (proj.trimEndMs - proj.trimStartMs) / 2,
                        onValueChange = { viewModel.updateFadePoints(it, proj.fadeOutMs) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FadeControl(
                        label = "Fade Out",
                        valueMs = proj.fadeOutMs,
                        maxMs = (proj.trimEndMs - proj.trimStartMs) / 2,
                        onValueChange = { viewModel.updateFadePoints(proj.fadeInMs, it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Export Section
                HybridSectionHeader(title = "Export")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Output Format", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioFormat.entries.forEach { format ->
                                FilterChip(
                                    selected = proj.outputFormat == format,
                                    onClick = { viewModel.updateFormat(format) },
                                    label = { Text(format.name) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        HybridPrimaryButton(
                            text = "Export Now",
                            onClick = { viewModel.exportAudio() },
                            enabled = exportState !is ExportState.Exporting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Loading audio...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    // Handle Export Success/Error
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            // Show snackbar or toast (omitted for brevity, assume viewModel reset)
            viewModel.resetExportState()
            onNavigateBack()
        }
    }
}

@Composable
fun FadeControl(
    label: String,
    valueMs: Long,
    maxMs: Long,
    onValueChange: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${valueMs / 1000f}s", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = valueMs.toFloat(),
            onValueChange = { onValueChange(it.toLong()) },
            valueRange = 0f..maxMs.toFloat().coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AudioEditorScreenPreview() {
    HybridTheme {
        // Mock UI state for preview
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Audio Editor", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        HybridPrimaryButton(
                            text = "Export",
                            onClick = {},
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 32.dp)
            ) {
                HybridSectionHeader(title = "Waveform & Trim")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("00:00")
                        Spacer(modifier = Modifier.width(16.dp))
                        FilledIconButton(onClick = {}, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("05:00")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HybridSectionHeader(title = "Fade Effects")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FadeControl(label = "Fade In", valueMs = 1000, maxMs = 5000, onValueChange = {})
                    Spacer(modifier = Modifier.height(16.dp))
                    FadeControl(label = "Fade Out", valueMs = 2000, maxMs = 5000, onValueChange = {})
                }
            }
        }
    }
}
