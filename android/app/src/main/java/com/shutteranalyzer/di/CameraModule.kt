package com.shutteranalyzer.di

import android.content.Context
import com.shutteranalyzer.data.camera.FrameAnalyzer
import com.shutteranalyzer.data.camera.ShutterCameraManager
import com.shutteranalyzer.data.camera.SlowMotionCapabilityChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing camera-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideFrameAnalyzer(): FrameAnalyzer {
        return FrameAnalyzer()
    }

    @Provides
    @Singleton
    fun provideSlowMotionCapabilityChecker(
        @ApplicationContext context: Context
    ): SlowMotionCapabilityChecker {
        return SlowMotionCapabilityChecker(context)
    }

    @Provides
    @Singleton
    fun provideShutterCameraManager(
        @ApplicationContext context: Context,
        frameAnalyzer: FrameAnalyzer
    ): ShutterCameraManager {
        return ShutterCameraManager(context, frameAnalyzer)
    }
}
