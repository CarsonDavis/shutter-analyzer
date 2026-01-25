package com.shutteranalyzer.di

import android.content.Context
import androidx.room.Room
import com.shutteranalyzer.data.local.database.AppDatabase
import com.shutteranalyzer.data.local.database.dao.CameraDao
import com.shutteranalyzer.data.local.database.dao.ShutterEventDao
import com.shutteranalyzer.data.local.database.dao.TestSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideCameraDao(database: AppDatabase): CameraDao {
        return database.cameraDao()
    }

    @Provides
    fun provideTestSessionDao(database: AppDatabase): TestSessionDao {
        return database.testSessionDao()
    }

    @Provides
    fun provideShutterEventDao(database: AppDatabase): ShutterEventDao {
        return database.shutterEventDao()
    }
}
