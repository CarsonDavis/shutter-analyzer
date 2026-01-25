package com.shutteranalyzer.di

import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.data.repository.CameraRepositoryImpl
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.data.repository.TestSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        impl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindTestSessionRepository(
        impl: TestSessionRepositoryImpl
    ): TestSessionRepository
}
