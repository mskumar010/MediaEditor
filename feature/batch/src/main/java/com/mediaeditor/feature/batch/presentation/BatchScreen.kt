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

package com.mediaeditor.feature.batch.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import com.mediaeditor.core.queue.domain.BatchJobInfo
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.ui.components.HybridCard
import com.mediaeditor.core.ui.components.HybridPrimaryButton
import com.mediaeditor.core.ui.components.HybridSectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val jobs by viewModel.batchJobs.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val videoFormat by viewModel.videoFormat.collectAsState()

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        viewModel.setSelectedUris(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Queue", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedUris.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.enqueueSelected() },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Enqueue") },
                    text = { Text("Enqueue ${selectedUris.size} file(s)") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── New Batch Task ──────────────────────────────────────────────
            item {
                HybridSectionHeader(title = "New Batch Task")
            }

            item {
                HybridCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // File picker button
                        HybridPrimaryButton(
                            text = if (selectedUris.isEmpty()) "Select Files" else "${selectedUris.size} file(s) selected",
                            onClick = { multiFilePicker.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (selectedUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedUris.joinToString("\n") { it.lastPathSegment ?: it.toString() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5
                            )
                        }
                    }
                }
            }

            item {
                HybridCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Operation Mode", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = mode is BatchMode.VideoConversion,
                                onClick = { viewModel.setMode(BatchMode.VideoConversion) },
                                label = { Text("Video Convert") }
                            )
                            FilterChip(
                                selected = mode is BatchMode.AudioConversion,
                                onClick = { viewModel.setMode(BatchMode.AudioConversion) },
                                label = { Text("Audio Convert") }
                            )
                            FilterChip(
                                selected = mode is BatchMode.ExtractAudio,
                                onClick = { viewModel.setMode(BatchMode.ExtractAudio) },
                                label = { Text("Extract Audio") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (mode) {
                            is BatchMode.VideoConversion -> {
                                Text("Target Video Format", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoFormat.entries.forEach { format ->
                                        FilterChip(
                                            selected = videoFormat == format,
                                            onClick = { viewModel.setVideoFormat(format) },
                                            label = { Text(format.name) }
                                        )
                                    }
                                }
                            }
                            is BatchMode.AudioConversion, is BatchMode.ExtractAudio -> {
                                Text("Target Audio Format", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AudioFormat.entries.forEach { format ->
                                        FilterChip(
                                            selected = audioFormat == format,
                                            onClick = { viewModel.setAudioFormat(format) },
                                            label = { Text(format.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Queue Status ────────────────────────────────────────────────
            item {
                HybridSectionHeader(title = "Queue Status (${jobs.size})")
            }

            if (jobs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No jobs in queue.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(jobs, key = { it.id }) { job ->
                    JobItem(job = job, onCancel = { viewModel.cancelJob(job.id) })
                }
            }
        }
    }
}

@Composable
fun JobItem(job: BatchJobInfo, onCancel: () -> Unit) {
    HybridCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job ${job.id.toString().take(8)}…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (job.state == WorkInfo.State.RUNNING || job.state == WorkInfo.State.ENQUEUED) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val statusColor = when (job.state) {
                WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.primary
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text("Status: ${job.state.name}", style = MaterialTheme.typography.bodyMedium, color = statusColor)

            if (job.state == WorkInfo.State.RUNNING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { job.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${job.progress}%", style = MaterialTheme.typography.labelSmall)
            }

            if (job.state == WorkInfo.State.ENQUEUED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (job.state == WorkInfo.State.SUCCEEDED && job.outputUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saved: ${job.outputUri?.substringAfterLast('/')}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (job.state == WorkInfo.State.FAILED && job.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: ${job.error}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
