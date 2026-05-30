package com.mediaeditor.core.waveform.di

import com.mediaeditor.core.waveform.AmplitudaEngine
import com.mediaeditor.core.waveform.AmplitudaEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WaveformModule {

    @Binds
    abstract fun bindAmplitudaEngine(
        impl: AmplitudaEngineImpl
    ): AmplitudaEngine
}
