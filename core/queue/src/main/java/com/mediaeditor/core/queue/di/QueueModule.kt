package com.mediaeditor.core.queue.di

import com.mediaeditor.core.queue.domain.BatchJobScheduler
import com.mediaeditor.core.queue.domain.WorkManagerBatchJobScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class QueueModule {

    @Binds
    abstract fun bindBatchJobScheduler(
        impl: WorkManagerBatchJobScheduler
    ): BatchJobScheduler
}
