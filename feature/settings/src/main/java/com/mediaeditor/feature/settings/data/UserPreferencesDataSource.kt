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

package com.mediaeditor.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.feature.settings.domain.model.ThemeMode
import com.mediaeditor.feature.settings.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val DEFAULT_VIDEO_FORMAT = stringPreferencesKey("default_video_format")
        val DEFAULT_VIDEO_CODEC = stringPreferencesKey("default_video_codec")
        val KEEP_ORIGINALS = booleanPreferencesKey("keep_originals")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val themeMode = try {
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
        val defaultAudioFormat = try {
            AudioFormat.valueOf(preferences[PreferencesKeys.DEFAULT_AUDIO_FORMAT] ?: AudioFormat.AAC.name)
        } catch (e: Exception) {
            AudioFormat.AAC
        }
        val defaultVideoFormat = try {
            VideoFormat.valueOf(preferences[PreferencesKeys.DEFAULT_VIDEO_FORMAT] ?: VideoFormat.MP4.name)
        } catch (e: Exception) {
            VideoFormat.MP4
        }
        val defaultVideoCodec = try {
            VideoCodec.valueOf(preferences[PreferencesKeys.DEFAULT_VIDEO_CODEC] ?: VideoCodec.H264.name)
        } catch (e: Exception) {
            VideoCodec.H264
        }

        UserPreferences(
            themeMode = themeMode,
            dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true,
            defaultAudioFormat = defaultAudioFormat,
            defaultVideoFormat = defaultVideoFormat,
            defaultVideoCodec = defaultVideoCodec,
            keepOriginals = preferences[PreferencesKeys.KEEP_ORIGINALS] ?: true
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateDefaultAudioFormat(format: AudioFormat) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_FORMAT] = format.name
        }
    }

    suspend fun updateDefaultVideoFormat(format: VideoFormat) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIDEO_FORMAT] = format.name
        }
    }

    suspend fun updateDefaultVideoCodec(codec: VideoCodec) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIDEO_CODEC] = codec.name
        }
    }

    suspend fun updateKeepOriginals(keep: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_ORIGINALS] = keep
        }
    }
}
