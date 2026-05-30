package com.mediaeditor.core.ffmpeg

import com.mediaeditor.core.router.FFmpegEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FFmpegModule {

    @Binds
    abstract fun bindFFmpegEngine(
        impl: FFmpegEngineImpl
    ): FFmpegEngine
}
