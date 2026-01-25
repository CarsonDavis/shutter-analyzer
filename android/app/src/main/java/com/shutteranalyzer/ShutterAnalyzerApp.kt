package com.shutteranalyzer

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

/**
 * Application class for Shutter Analyzer.
 * Initializes OpenCV and Hilt dependency injection.
 */
@HiltAndroidApp
class ShutterAnalyzerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeOpenCV()
    }

    private fun initializeOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG, "OpenCV initialized successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed")
        }
    }

    companion object {
        private const val TAG = "ShutterAnalyzerApp"
    }
}
