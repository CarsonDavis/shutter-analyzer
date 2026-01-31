package com.shutteranalyzer

import android.app.Application
import android.util.Log
import com.shutteranalyzer.data.local.database.dao.TestSessionDao
import com.shutteranalyzer.data.storage.VideoStorageManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

/**
 * Application class for Shutter Analyzer.
 * Initializes OpenCV and Hilt dependency injection.
 */
@HiltAndroidApp
class ShutterAnalyzerApp : Application() {

    @Inject
    lateinit var testSessionDao: TestSessionDao

    @Inject
    lateinit var videoStorageManager: VideoStorageManager

    override fun onCreate() {
        super.onCreate()
        initializeOpenCV()
        cleanupOrphanedSessions()
    }

    private fun initializeOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG, "OpenCV initialized successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed")
        }
    }

    /**
     * One-time cleanup of orphaned sessions (those with null cameraId).
     * These are legacy sessions from before we enforced camera creation.
     */
    private fun cleanupOrphanedSessions() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ORPHAN_CLEANUP_DONE, false)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val orphaned = testSessionDao.getSessionsWithNullCamera()
                orphaned.forEach { session ->
                    videoStorageManager.deleteVideo(session.videoUri)
                    testSessionDao.delete(session)
                }
                prefs.edit().putBoolean(KEY_ORPHAN_CLEANUP_DONE, true).apply()
                Log.d(TAG, "Cleaned up ${orphaned.size} orphaned sessions")
            } catch (e: Exception) {
                Log.e(TAG, "Orphan cleanup failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "ShutterAnalyzerApp"
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ORPHAN_CLEANUP_DONE = "orphan_cleanup_done"
    }
}
