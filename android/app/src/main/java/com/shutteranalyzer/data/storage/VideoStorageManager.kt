package com.shutteranalyzer.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages video file storage operations, primarily deletion.
 * Videos are stored via MediaStore and referenced by content URIs.
 */
@Singleton
class VideoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Delete a video from MediaStore by its content URI.
     *
     * @param videoUri The content URI of the video to delete (e.g., "content://media/external/video/media/123")
     * @return true if deleted or URI was null/empty/not found, false on permission error
     */
    suspend fun deleteVideo(videoUri: String?): Boolean {
        if (videoUri.isNullOrBlank()) return true
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(videoUri)
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d(TAG, "Deleted video: $videoUri")
                    true
                } else {
                    Log.w(TAG, "Video not found or already deleted: $videoUri")
                    true // Consider missing file as success
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied deleting video: $videoUri", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete video: $videoUri", e)
                false
            }
        }
    }

    companion object {
        private const val TAG = "VideoStorageManager"
    }
}
