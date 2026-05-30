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

package com.mediaeditor.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.core.ui.components.HybridCard
import com.mediaeditor.core.ui.components.HybridSectionHeader
import com.mediaeditor.feature.settings.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var activeDialog by remember { mutableStateOf<PreferencesDialogType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(bottom = 24.dp)
        ) {
            // Section 1: Appearance
            HybridSectionHeader(title = "Appearance")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    PreferenceItem(
                        icon = Icons.Rounded.ColorLens,
                        title = "Theme Mode",
                        subtitle = uiState.preferences.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { activeDialog = PreferencesDialogType.THEME }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    PreferenceSwitchItem(
                        icon = Icons.Rounded.Palette,
                        title = "Dynamic Colors",
                        subtitle = "Apply wallpaper colors to the UI theme",
                        checked = uiState.preferences.dynamicColor,
                        onCheckedChange = { viewModel.updateDynamicColor(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Editor Defaults
            HybridSectionHeader(title = "Editor Defaults")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    PreferenceItem(
                        icon = Icons.Rounded.AudioFile,
                        title = "Default Audio Format",
                        subtitle = uiState.preferences.defaultAudioFormat.name,
                        onClick = { activeDialog = PreferencesDialogType.AUDIO_FORMAT }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    PreferenceItem(
                        icon = Icons.Rounded.VideoFile,
                        title = "Default Video Format",
                        subtitle = uiState.preferences.defaultVideoFormat.name,
                        onClick = { activeDialog = PreferencesDialogType.VIDEO_FORMAT }
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    PreferenceItem(
                        icon = Icons.Rounded.Code,
                        title = "Default Video Codec",
                        subtitle = uiState.preferences.defaultVideoCodec.name,
                        onClick = { activeDialog = PreferencesDialogType.VIDEO_CODEC }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Storage & Cache
            HybridSectionHeader(title = "Storage & Cache")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    PreferenceSwitchItem(
                        icon = Icons.Rounded.RestorePage,
                        title = "Keep Original Files",
                        subtitle = "Do not delete or modify input media after processing",
                        checked = uiState.preferences.keepOriginals,
                        onCheckedChange = { viewModel.updateKeepOriginals(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    PreferenceItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Temporary Files",
                        subtitle = "Cached input copies and metadata: ${formatBytes(uiState.cacheSize)}",
                        onClick = { viewModel.clearCache() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: About
            HybridSectionHeader(title = "About")
            HybridCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Media Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("FOSS offline Android media editor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Version 1.0.0 (GPL v3)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Render Dialogs
    when (activeDialog) {
        PreferencesDialogType.THEME -> {
            SelectionDialog(
                title = "Choose Theme Mode",
                options = ThemeMode.entries.map { it.name },
                currentSelected = uiState.preferences.themeMode.name,
                onSelect = {
                    viewModel.updateThemeMode(ThemeMode.valueOf(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        PreferencesDialogType.AUDIO_FORMAT -> {
            SelectionDialog(
                title = "Default Audio Format",
                options = AudioFormat.entries.map { it.name },
                currentSelected = uiState.preferences.defaultAudioFormat.name,
                onSelect = {
                    viewModel.updateDefaultAudioFormat(AudioFormat.valueOf(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        PreferencesDialogType.VIDEO_FORMAT -> {
            SelectionDialog(
                title = "Default Video Format",
                options = VideoFormat.entries.map { it.name },
                currentSelected = uiState.preferences.defaultVideoFormat.name,
                onSelect = {
                    viewModel.updateDefaultVideoFormat(VideoFormat.valueOf(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        PreferencesDialogType.VIDEO_CODEC -> {
            SelectionDialog(
                title = "Default Video Codec",
                options = VideoCodec.entries.map { it.name },
                currentSelected = uiState.preferences.defaultVideoCodec.name,
                onSelect = {
                    viewModel.updateDefaultVideoCodec(VideoCodec.valueOf(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        null -> {}
    }
}

enum class PreferencesDialogType {
    THEME, AUDIO_FORMAT, VIDEO_FORMAT, VIDEO_CODEC
}

@Composable
fun PreferenceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1.5f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PreferenceSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1.5f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    currentSelected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                options.forEach { option ->
                    val isSelected = option == currentSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null // Selected via Row click
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option.replace("_", " "),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return "%,.2f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
