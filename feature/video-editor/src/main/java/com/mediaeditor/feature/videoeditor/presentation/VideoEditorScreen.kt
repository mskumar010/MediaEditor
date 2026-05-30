package com.mediaeditor.feature.videoeditor.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.ui.components.HybridCard
import com.mediaeditor.core.ui.components.HybridPrimaryButton
import com.mediaeditor.core.ui.components.HybridSectionHeader
import com.mediaeditor.core.ui.theme.HybridTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoEditorScreen(
    uri: Uri?,
    onNavigateBack: () -> Unit,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val project by viewModel.project.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uri) {
        if (uri != null) {
            viewModel.loadVideo(uri)
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
                title = { Text("Video Editor", fontWeight = FontWeight.Bold) },
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
                // Player View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = viewModel.player
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trimming Section
                HybridSectionHeader(title = "Trim Range")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        RangeSlider(
                            value = proj.trimStartMs.toFloat()..proj.trimEndMs.toFloat(),
                            onValueChange = { range ->
                                viewModel.updateTrimPoints(range.start.toLong(), range.endInclusive.toLong())
                            },
                            valueRange = 0f..proj.durationMs.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(proj.trimStartMs), style = MaterialTheme.typography.labelMedium)
                            Text(formatTime(proj.trimEndMs), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tools Section
                HybridSectionHeader(title = "Transform")
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HybridCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.updateRotation() }) {
                                Icon(Icons.Default.RotateRight, contentDescription = null)
                            }
                            Text("Rotate", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HybridCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${proj.speed}x", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Slider(
                                value = proj.speed,
                                onValueChange = { viewModel.updateSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 3
                            )
                            Text("Speed", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Export Card
                HybridSectionHeader(title = "Export")
                HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Format", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoFormat.entries.forEach { format ->
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
                            onClick = { viewModel.exportVideo() },
                            enabled = exportState !is ExportState.Exporting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            viewModel.resetExportState()
            onNavigateBack()
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
