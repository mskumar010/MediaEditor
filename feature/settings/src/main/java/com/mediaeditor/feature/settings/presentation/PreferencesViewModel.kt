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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.feature.settings.domain.UserPreferencesRepository
import com.mediaeditor.feature.settings.domain.model.ThemeMode
import com.mediaeditor.feature.settings.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cacheSize = MutableStateFlow(0L)

    val uiState: StateFlow<PreferencesUiState> = combine(
        repository.userPreferences,
        _cacheSize
    ) { prefs, size ->
        PreferencesUiState(preferences = prefs, cacheSize = size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PreferencesUiState()
    )

    init {
        updateCacheSize()
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDynamicColor(enabled)
        }
    }

    fun updateDefaultAudioFormat(format: AudioFormat) {
        viewModelScope.launch {
            repository.updateDefaultAudioFormat(format)
        }
    }

    fun updateDefaultVideoFormat(format: VideoFormat) {
        viewModelScope.launch {
            repository.updateDefaultVideoFormat(format)
        }
    }

    fun updateDefaultVideoCodec(codec: VideoCodec) {
        viewModelScope.launch {
            repository.updateDefaultVideoCodec(codec)
        }
    }

    fun updateKeepOriginals(keep: Boolean) {
        viewModelScope.launch {
            repository.updateKeepOriginals(keep)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear app cache subdirectories recursively but keep the directories themselves if possible
                context.cacheDir.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: Exception) {
                // Ignore silent errors
            }
            updateCacheSize()
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = try {
                context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } catch (e: Exception) {
                0L
            }
            _cacheSize.value = size
        }
    }
}
