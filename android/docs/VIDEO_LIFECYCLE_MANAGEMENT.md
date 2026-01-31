# Video Lifecycle Management

**Status**: Implemented
**Created**: 2025-01-31
**Implemented**: 2026-01-31
**Problem**: Orphaned video files accumulate with no way to manage them

> **Note**: During implementation, we also fixed a bug where sessions always showed "No events recorded" in the test history list. The `getSessionsForCamera()` method was passing `emptyList()` for events. Fixed by adding `eventCount` property to `TestSession` and fetching counts via `ShutterEventDao.getEventCountForSession()`.

---

## Problem Statement

### Current Behavior
1. Videos are saved to `Movies/ShutterAnalyzer/` via MediaStore
2. Video URIs are stored in `TestSessionEntity.videoUri`
3. When cameras or sessions are deleted, only database records are removed
4. Video files remain on device storage indefinitely
5. Users have no way to manage these orphaned files from the app

### Evidence
Database query on connected device:
- 18 sessions with videos
- 36 orphaned sessions (null cameraId)
- 262 shutter events
- Only 1 camera visible in app

### Root Causes
1. **No video cleanup code exists** - deletion only removes DB records
2. **ImportViewModel allows null cameraId** - creates orphaned sessions
3. **Legacy migration** - converted cameraId=0 to NULL, orphaning old data

---

## Design Principle

**Videos are owned by TestSessions. Sessions are owned by Cameras. Delete cascades automatically.**

```
Camera (user deletes)
  → TestSession (cascade)
    → Video file (auto-cleanup via MediaStore)
```

Users manage **cameras** and **tests**, not media files.

---

## Implementation

### 1. New Component: VideoStorageManager

**File**: `data/storage/VideoStorageManager.kt`

```kotlin
@Singleton
class VideoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Delete a video from MediaStore by its content URI.
     * @return true if deleted or URI was null/empty, false on error
     */
    suspend fun deleteVideo(videoUri: String?): Boolean {
        if (videoUri.isNullOrBlank()) return true
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(videoUri)
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("VideoStorage", "Deleted video: $videoUri")
                    true
                } else {
                    Log.w("VideoStorage", "Video not found or already deleted: $videoUri")
                    true // Consider missing file as success
                }
            } catch (e: SecurityException) {
                Log.e("VideoStorage", "Permission denied deleting video: $videoUri", e)
                false
            } catch (e: Exception) {
                Log.e("VideoStorage", "Failed to delete video: $videoUri", e)
                false
            }
        }
    }
}
```

### 2. Modify TestSessionRepository

**File**: `data/repository/TestSessionRepository.kt`

Add video cleanup to delete operations:

```kotlin
interface TestSessionRepository {
    // ... existing methods ...
    suspend fun deleteSession(session: TestSession)
    suspend fun deleteAllSessionsForCamera(cameraId: Long)
}

class TestSessionRepositoryImpl @Inject constructor(
    private val testSessionDao: TestSessionDao,
    private val shutterEventDao: ShutterEventDao,
    private val videoStorageManager: VideoStorageManager  // NEW
) : TestSessionRepository {

    override suspend fun deleteSession(session: TestSession) {
        // Delete video file BEFORE removing DB record
        videoStorageManager.deleteVideo(session.videoUri)
        testSessionDao.delete(session.toEntity())
    }

    override suspend fun deleteAllSessionsForCamera(cameraId: Long) {
        // Get all sessions to access their videoUris
        val sessions = testSessionDao.getSessionsForCameraOnce(cameraId)
        // Delete each video file
        sessions.forEach { entity ->
            videoStorageManager.deleteVideo(entity.videoUri)
        }
        // Then delete DB records
        testSessionDao.deleteAllForCamera(cameraId)
    }
}
```

### 3. Modify CameraRepository

**File**: `data/repository/CameraRepository.kt`

Clean up videos before cascade delete removes session records:

```kotlin
class CameraRepositoryImpl @Inject constructor(
    private val cameraDao: CameraDao,
    private val testSessionRepository: TestSessionRepository  // NEW dependency
) : CameraRepository {

    override suspend fun deleteCamera(camera: Camera) {
        // Clean up videos for all sessions BEFORE cascade delete
        testSessionRepository.deleteAllSessionsForCamera(camera.id)
        // Now safe to delete camera (cascade will clean up any remaining session records)
        cameraDao.delete(camera.toEntity())
    }
}
```

### 4. Fix ImportViewModel - Always Create Camera

**File**: `ui/screens/videoimport/ImportViewModel.kt`

Never allow null cameraId:

```kotlin
fun createSession() {
    viewModelScope.launch {
        try {
            // ALWAYS create a camera - generate name if blank
            val cameraName = _cameraName.value.trim().ifBlank {
                "Import ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d HH:mm"))}"
            }
            val camera = Camera(
                name = cameraName,
                createdAt = Instant.now()
            )
            val cameraId = cameraRepository.saveCamera(camera)

            val session = TestSession(
                cameraId = cameraId,  // NEVER NULL
                recordingFps = fps,
                testedAt = Instant.now(),
                // ... rest unchanged
            )
            // ...
        }
    }
}
```

### 5. Add DAO Method for Batch Fetch

**File**: `data/local/database/dao/TestSessionDao.kt`

```kotlin
@Dao
interface TestSessionDao {
    // ... existing methods ...

    @Query("SELECT * FROM test_sessions WHERE cameraId = :cameraId")
    suspend fun getSessionsForCameraOnce(cameraId: Long): List<TestSessionEntity>
}
```

### 6. One-Time Orphan Cleanup Migration

**File**: `data/local/database/AppDatabase.kt`

Add migration to clean orphaned sessions and their videos:

```kotlin
// Run as part of app initialization, not a schema migration
// (Schema migrations can't do async video deletion)

class OrphanCleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val testSessionDao: TestSessionDao,
    private val videoStorageManager: VideoStorageManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val orphanedSessions = testSessionDao.getSessionsWithNullCamera()
        orphanedSessions.forEach { session ->
            videoStorageManager.deleteVideo(session.videoUri)
            testSessionDao.delete(session)
        }
        return Result.success()
    }
}
```

Add DAO method:
```kotlin
@Query("SELECT * FROM test_sessions WHERE cameraId IS NULL")
suspend fun getSessionsWithNullCamera(): List<TestSessionEntity>
```

### 7. Dependency Injection

No separate module needed. `VideoStorageManager` uses `@Singleton` + `@Inject constructor`, so Hilt provides it automatically. Repositories receive it via constructor injection.

---

## Data Flow

### Delete Camera Flow
```
User taps "Delete Camera" in CameraDetailScreen
  → CameraDetailViewModel.deleteCamera()
    → CameraRepository.deleteCamera(camera)
      → TestSessionRepository.deleteAllSessionsForCamera(camera.id)
        → Fetch all sessions for camera
        → For each: VideoStorageManager.deleteVideo(videoUri)
        → testSessionDao.deleteAllForCamera(cameraId)
      → cameraDao.delete(camera)
        → FK CASCADE cleans any remaining session/event records
```

### Delete Individual Test Flow
```
User taps "Delete Test" in ResultsScreen (future feature)
  → ResultsViewModel.deleteSession()
    → TestSessionRepository.deleteSession(session)
      → VideoStorageManager.deleteVideo(session.videoUri)
      → testSessionDao.delete(session)
        → FK CASCADE cleans event records
```

---

## Files Changed Summary

| File | Action | Purpose |
|------|--------|---------|
| `data/storage/VideoStorageManager.kt` | CREATE | Video deletion utility |
| `data/repository/TestSessionRepository.kt` | MODIFY | Add video cleanup to deletes, add `deleteAllSessionsForCamera()`, fix event count loading |
| `data/repository/CameraRepository.kt` | MODIFY | Call session cleanup before camera delete |
| `data/local/database/dao/TestSessionDao.kt` | MODIFY | Add `getSessionsForCameraOnce()`, `getSessionsWithNullCamera()` |
| `data/local/database/dao/ShutterEventDao.kt` | MODIFY | Add `getEventCountForSession()` |
| `domain/model/TestSession.kt` | MODIFY | Add `eventCount` property |
| `ui/screens/camera/CameraDetailScreen.kt` | MODIFY | Use `eventCount` instead of `events.size` |
| `ui/screens/videoimport/ImportViewModel.kt` | MODIFY | Always create camera (auto-generate name if blank) |
| `ShutterAnalyzerApp.kt` | MODIFY | One-time orphan cleanup on first run |

Note: `VideoStorageManager` uses `@Singleton` + `@Inject constructor`, so Hilt provides it automatically without a separate module.

---

## Testing

### Manual Testing
1. Create a new test via recording flow → verify video saved
2. Delete the camera → verify video removed from `Movies/ShutterAnalyzer/`
3. Import a video without camera name → verify camera auto-created
4. Check database for orphaned sessions after cleanup runs

### Automated Testing
- Unit test `VideoStorageManager.deleteVideo()` with mock ContentResolver
- Unit test `TestSessionRepository.deleteSession()` verifies video cleanup called
- Integration test delete cascade from Camera → Session → Video

---

## Future Considerations

1. **Storage usage display**: Show total storage used by app in Settings
2. **Bulk cleanup**: "Delete all tests older than X" feature
3. **Export before delete**: Option to export video before deletion
4. **Schema constraint**: Make cameraId NOT NULL after orphan cleanup complete
