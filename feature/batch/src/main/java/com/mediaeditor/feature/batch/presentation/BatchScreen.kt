package com.mediaeditor.feature.batch.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.mediaeditor.core.ui.components.HybridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val jobs by viewModel.batchJobs.collectAsState()

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
        }
    ) { paddingValues ->
        if (jobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No batch operations currently queued.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(jobs) { job ->
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
                    text = "Job ID: ${job.id.toString().take(8)}...",
                    style = MaterialTheme.typography.titleMedium
                )
                if (job.state == WorkInfo.State.RUNNING || job.state == WorkInfo.State.ENQUEUED) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Status: ${job.state.name}", style = MaterialTheme.typography.bodyMedium)

            if (job.state == WorkInfo.State.RUNNING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { job.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    gapSize = 0.dp, drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${job.progress}%", style = MaterialTheme.typography.labelSmall)
            }

            if (job.state == WorkInfo.State.SUCCEEDED && job.outputUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Output: ${job.outputUri}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (job.state == WorkInfo.State.FAILED && job.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Error: ${job.error}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
