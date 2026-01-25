package com.shutteranalyzer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cameraDao(): CameraDao

    abstract fun testSessionDao(): TestSessionDao

    abstract fun shutterEventDao(): ShutterEventDao

    companion object {
        const val DATABASE_NAME = "shutter_analyzer.db"

        /**
         * Migration from version 1 to 2: Add expectedSpeedsJson and videoUri to test_sessions.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE test_sessions ADD COLUMN expectedSpeedsJson TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE test_sessions ADD COLUMN videoUri TEXT"
                )
            }
        }

        /**
         * Migration from version 2 to 3: Make cameraId nullable in test_sessions.
         * SQLite doesn't support changing nullability, so we recreate the table.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table with nullable cameraId
                db.execSQL("""
                    CREATE TABLE test_sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cameraId INTEGER,
                        recordingFps REAL NOT NULL,
                        testedAt INTEGER NOT NULL,
                        avgDeviationPercent REAL,
                        expectedSpeedsJson TEXT NOT NULL DEFAULT '',
                        videoUri TEXT,
                        FOREIGN KEY(cameraId) REFERENCES cameras(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy data (convert cameraId=0 to NULL)
                db.execSQL("""
                    INSERT INTO test_sessions_new (id, cameraId, recordingFps, testedAt, avgDeviationPercent, expectedSpeedsJson, videoUri)
                    SELECT id, CASE WHEN cameraId = 0 THEN NULL ELSE cameraId END, recordingFps, testedAt, avgDeviationPercent, expectedSpeedsJson, videoUri
                    FROM test_sessions
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE test_sessions")

                // Rename new table
                db.execSQL("ALTER TABLE test_sessions_new RENAME TO test_sessions")

                // Recreate index
                db.execSQL("CREATE INDEX index_test_sessions_cameraId ON test_sessions(cameraId)")
            }
        }
    }
}
