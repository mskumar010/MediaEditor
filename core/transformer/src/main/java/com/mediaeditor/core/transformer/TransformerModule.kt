package com.mediaeditor.core.transformer

import com.mediaeditor.core.router.TransformerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransformerModule {

    @Binds
    abstract fun bindTransformerEngine(
        impl: TransformerEngineImpl
    ): TransformerEngine
}
