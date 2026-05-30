package com.mediaeditor.feature.converter.di

import com.mediaeditor.core.router.ProcessingRouter
import com.mediaeditor.feature.converter.domain.usecase.ConvertAudioUseCase
import com.mediaeditor.feature.converter.domain.usecase.ConvertVideoUseCase
import com.mediaeditor.feature.converter.domain.usecase.ExtractAudioUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ConverterModule {

    @Provides
    @ViewModelScoped
    fun provideConvertAudioUseCase(router: ProcessingRouter): ConvertAudioUseCase {
        return ConvertAudioUseCase(router)
    }

    @Provides
    @ViewModelScoped
    fun provideConvertVideoUseCase(router: ProcessingRouter): ConvertVideoUseCase {
        return ConvertVideoUseCase(router)
    }

    @Provides
    @ViewModelScoped
    fun provideExtractAudioUseCase(router: ProcessingRouter): ExtractAudioUseCase {
        return ExtractAudioUseCase(router)
    }
}
