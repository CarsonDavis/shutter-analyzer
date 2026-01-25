package com.shutteranalyzer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shutteranalyzer.data.local.database.converter.Converters
import com.shutteranalyzer.data.local.database.dao.CameraDao
import com.shutteranalyzer.data.local.database.dao.ShutterEventDao
import com.shutteranalyzer.data.local.database.dao.TestSessionDao
import com.shutteranalyzer.data.local.database.entity.CameraEntity
import com.shutteranalyzer.data.local.database.entity.ShutterEventEntity
import com.shutteranalyzer.data.local.database.entity.TestSessionEntity

/**
 * Room database for Shutter Analyzer.
 *
 * Contains tables for cameras, test sessions, and shutter events.
 */
@Database(
    entities = [
        CameraEntity::class,
        TestSessionEntity::class,
        ShutterEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cameraDao(): CameraDao

    abstract fun testSessionDao(): TestSessionDao

    abstract fun shutterEventDao(): ShutterEventDao

    companion object {
        const val DATABASE_NAME = "shutter_analyzer.db"
    }
}
