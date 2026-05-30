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

import com.mediaeditor.core.router.AudioFormat
import com.mediaeditor.core.router.VideoCodec
import com.mediaeditor.core.router.VideoFormat
import com.mediaeditor.feature.settings.domain.UserPreferencesRepository
import com.mediaeditor.feature.settings.domain.model.ThemeMode
import com.mediaeditor.feature.settings.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataSource: UserPreferencesDataSource
) : UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun updateThemeMode(themeMode: ThemeMode) = dataSource.updateThemeMode(themeMode)
    override suspend fun updateDynamicColor(enabled: Boolean) = dataSource.updateDynamicColor(enabled)
    override suspend fun updateDefaultAudioFormat(format: AudioFormat) = dataSource.updateDefaultAudioFormat(format)
    override suspend fun updateDefaultVideoFormat(format: VideoFormat) = dataSource.updateDefaultVideoFormat(format)
    override suspend fun updateDefaultVideoCodec(codec: VideoCodec) = dataSource.updateDefaultVideoCodec(codec)
    override suspend fun updateKeepOriginals(keep: Boolean) = dataSource.updateKeepOriginals(keep)
}
