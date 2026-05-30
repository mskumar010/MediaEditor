package com.mediaeditor.feature.converter.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.ui.components.HybridCard
import com.mediaeditor.core.ui.components.HybridPrimaryButton
import com.mediaeditor.core.ui.components.HybridSectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val inputUri by viewModel.inputUri.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val videoFormat by viewModel.videoFormat.collectAsState()
    val videoCodec by viewModel.videoCodec.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setInputUri(uri)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    if (state is ConverterState.Processing) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            HybridCard(modifier = Modifier.width(200.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Converting... ${(state as ConverterState.Processing).progress}%")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Converter", fontWeight = FontWeight.Bold) },
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
            // Input Selection
            HybridSectionHeader(title = "Input File")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (inputUri != null) "File Selected: ${inputUri?.lastPathSegment ?: "Unknown"}" else "No file selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HybridPrimaryButton(
                        text = "Select File",
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Selection
            HybridSectionHeader(title = "Conversion Mode")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConversionMode.entries.forEach { m ->
                            FilterChip(
                                selected = mode == m,
                                onClick = { viewModel.setMode(m) },
                                label = { Text(m.name.replace("_", " ")) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options
            HybridSectionHeader(title = "Output Options")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (mode) {
                        ConversionMode.AUDIO_TO_AUDIO, ConversionMode.EXTRACT_AUDIO -> {
                            Text("Output Audio Format", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
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
                        ConversionMode.VIDEO_TO_VIDEO -> {
                            Text("Output Video Format", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
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

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Video Codec", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoCodec.entries.forEach { codec ->
                                    FilterChip(
                                        selected = videoCodec == codec,
                                        onClick = { viewModel.setVideoCodec(codec) },
                                        label = { Text(codec.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HybridPrimaryButton(
                        text = "Start Conversion",
                        onClick = { viewModel.convert() },
                        enabled = inputUri != null && state !is ConverterState.Processing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    LaunchedEffect(state) {
        when (state) {
            is ConverterState.Success -> {
                snackbarHostState.showSnackbar("Conversion successful!")
                viewModel.resetState()
            }
            is ConverterState.Error -> {
                snackbarHostState.showSnackbar("Error: ${(state as ConverterState.Error).message}")
                viewModel.resetState()
            }
            else -> {}
        }
    }
}
