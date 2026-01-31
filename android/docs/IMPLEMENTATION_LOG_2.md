# Android Implementation Log (Part 2)

This log continues from [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md).

---

## 2025-01-25 - Phase 6: Visualization & Charts

### Overview
Added rich visualizations to the Results screen to help users understand their shutter speed measurements, matching the visual analysis provided by the Python CLI.

### New Components Created

#### Chart Utilities
- `ui/components/ChartUtils.kt` (NEW)
  - `drawHorizontalGridLines()` / `drawVerticalGridLines()` - Background grid drawing
  - `drawDashedHorizontalLine()` / `drawDashedLine()` - Threshold and reference lines
  - `deviationToColor()` / `deviationToGradientColor()` - Accuracy color scheme
  - `speedToLogPosition()` / `logPositionToSpeed()` - Log scale conversion for shutter speeds
  - `drawAxisLabel()` - Native canvas text rendering
  - `formatSpeed()` - Human-readable speed formatting

#### Deviation Bar Chart
- `ui/components/DeviationBarChart.kt` (NEW)
  - Horizontal bars showing deviation for each measurement
  - Center line at 0% (perfect accuracy)
  - Bars extend left (slow) or right (fast)
  - Color gradient: green (0%) → yellow (10%) → orange (15%) → red (20%+)
  - Speed labels on Y-axis, deviation values on right

#### Speed Comparison Chart
- `ui/components/SpeedComparisonChart.kt` (NEW)
  - Scatter plot comparing expected vs measured speeds
  - Log-scale axes (1/1000s to 1s)
  - Diagonal dashed line = perfect accuracy
  - Points colored by deviation magnitude
  - Useful for identifying systematic bias

#### Brightness Timeline Chart
- `ui/components/BrightnessTimelineChart.kt` (NEW)
  - Line plot of brightness values over time
  - Y-axis: 0-255 brightness, X-axis: frame index
  - Horizontal dashed line at detection threshold
  - Green shaded regions for detected events
  - Event labels above each region
  - Horizontal scrolling for long recordings
  - Legend showing brightness line, threshold, and events

### ViewModel Updates

- `ui/screens/results/ResultsViewModel.kt`
  - Added `TimelineData` data class with brightnessValues, events, threshold, baseline
  - Added `timelineData: StateFlow<TimelineData?>`
  - Added `buildTimelineData()` to reconstruct timeline from stored event brightness values
  - Calculates threshold and baseline from aggregated data

### Screen Updates

- `ui/screens/results/ResultsScreen.kt`
  - Added `PrimaryTabRow` with 4 tabs: Summary, Deviation, Accuracy, Timeline
  - Tab state management with `remember { mutableIntStateOf() }`
  - **Summary tab**: Original results table (unchanged)
  - **Deviation tab**: `DeviationBarChart` with explanatory text
  - **Accuracy tab**: `SpeedComparisonChart` with explanatory text
  - **Timeline tab**: `BrightnessTimelineChart` with event highlighting
  - Header and average deviation card always visible above tabs
  - Bottom buttons (Save/Test Again) always visible below tabs

### Gradle/Dependency Updates

- `build.gradle.kts` (root)
  - AGP: 8.2.0 → 8.7.3
  - Kotlin: 1.9.21 → 2.1.0
  - Added `org.jetbrains.kotlin.plugin.compose` 2.1.0
  - Added `com.google.devtools.ksp` 2.1.0-1.0.29
  - Hilt: 2.50 → 2.54

- `app/build.gradle.kts`
  - Migrated from `kapt` to `ksp` for Hilt and Room
  - Removed `composeOptions.kotlinCompilerExtensionVersion` (now handled by compose plugin)
  - compileSdk/targetSdk: 34 → 35
  - Updated all dependencies to latest stable versions
  - CameraX: 1.3.1 → 1.4.1
  - Compose BOM: 2024.02.00 → 2024.12.01
  - Navigation: 2.7.7 → 2.8.5
  - DataStore: 1.0.0 → 1.1.1
  - Coroutines: 1.7.3 → 1.9.0

- `gradle-wrapper.properties`
  - Gradle: 8.5 → 8.10.2

### Project Structure After Phase 6

```
ui/components/
├── AccuracyIndicator.kt      # Phase 4
├── BrightnessIndicator.kt    # Phase 4
├── CameraCard.kt             # Phase 4
├── SpeedChip.kt              # Phase 4
├── ChartUtils.kt             # Phase 6 (NEW)
├── DeviationBarChart.kt      # Phase 6 (NEW)
├── SpeedComparisonChart.kt   # Phase 6 (NEW)
└── BrightnessTimelineChart.kt # Phase 6 (NEW)
```

### Design Decisions

#### Native Compose Canvas vs External Library
- Used native Compose `Canvas` API for all charts
- No external charting dependencies (MPAndroidChart, etc.)
- Smaller APK size, full control over rendering
- Consistent with Material Design colors

#### Tab Layout vs Expandable Sections
- Chose tabs over expandable cards for cleaner UX
- All charts accessible with single tap
- Summary tab shows familiar table view by default
- Charts don't overwhelm users who just want numbers

#### Timeline Data Reconstruction
- Events store brightness values in `brightnessValuesJson`
- Timeline reconstructed by adding simulated baseline gaps between events
- Shows detection in context, not just isolated events

### Next Steps
- ~~Phase 7: Polish & Testing~~ ✅ Complete (see below)
- Phase 8: Publishing

---

## 2025-01-25 - Phase 7: Polish & Testing

### Overview
Made the app production-ready with proper error handling, accessibility improvements, and UX polish.

### Changes Made

#### Permission Handling (CRITICAL)
- `ui/screens/setup/RecordingSetupScreen.kt`
  - Added permission denied dialog with explanation
  - Added permission rationale dialog (shown before first request if rationale applies)
  - Added open settings dialog for permanently denied permissions
  - Uses `ActivityCompat.shouldShowRequestPermissionRationale()` to detect denial type
  - Provides "Open Settings" button for users to manually enable camera access

#### Accessibility Improvements
- Added `contentDescription` to all actionable icons across screens:
  - HomeScreen: Settings, Add, Import icons
  - RecordingSetupScreen: Back, Info, Expand icons
  - RecordingScreen: Redo, Skip, Done, Check icons
  - ResultsScreen: Back, Save, Refresh icons
  - CameraDetailScreen: Back, Edit, Delete, History icons
  - ImportScreen: Back, VideoLibrary, VideoFile, Error icons
- Added `Modifier.semantics { heading() }` to all section titles for screen readers
- Section headings updated: "MY CAMERAS", "SHUTTER SPEEDS TO TEST", "ACCURACY TABLE", "DEVIATION BY SPEED", etc.

#### Error Handling Improvements
- `data/camera/ShutterCameraManager.kt`
  - Added `getCameraErrorMessage()` helper for user-friendly error messages
  - Distinguishes camera errors: "in use", "unavailable", "permission denied", "disconnected", "max cameras"
  - Applied to both `initialize()` and `bindPreview()` error paths

- `ui/screens/recording/RecordingViewModel.kt`
  - Added zero events handling in `stopRecording()`
  - Recording completes even with zero events (UI handles empty state)

#### Loading States
- `ui/screens/results/ResultsViewModel.kt`
  - Added `isLoading: StateFlow<Boolean>` for loading state tracking
  - Set loading true at start of `loadSessionAndCalculate()`
  - Set loading false after all data loaded

- `ui/screens/results/ResultsScreen.kt`
  - Added loading state with `CircularProgressIndicator` and "Loading results..." text
  - Shows centered loading spinner while session data loads

#### Empty States
- `ui/screens/results/ResultsScreen.kt`
  - Added `NoEventsDetectedState` composable for zero events scenario
  - Shows informative message with possible causes:
    - Lighting too dim
    - Phone not positioned correctly
    - Shutter not fired during recording
  - Suggests trying again with brighter lighting

#### Package Name Fix
- Renamed `ui/screens/import/` to `ui/screens/videoimport/`
- "import" is a Kotlin reserved keyword that caused Hilt/KSP code generation issues
- Updated all references in `NavGraph.kt`

#### Build Dependencies
- Added `androidx.compose.material:material-icons-extended` for extended Material icons
- Created app launcher icons (adaptive icons for Android 8.0+):
  - `res/drawable/ic_launcher_background.xml` - blue background
  - `res/drawable/ic_launcher_foreground.xml` - camera lens icon
  - `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`

#### Static Method Fixes
- `analysis/ShutterSpeedCalculator.kt`
  - Added `formatShutterSpeed()` to companion object for static access

- `data/video/VideoAnalyzer.kt`
  - Injected `ThresholdCalculator` and `EventDetector` via constructor
  - Changed from static calls to instance method calls
  - Uses `analyzeBrightnessDistribution()` and `findShutterEvents()` + `createShutterEvents()`

#### Documentation
- Created `docs/BUILD.md` - comprehensive guide for building locally
  - Prerequisites (Android Studio, SDK)
  - Android Studio build instructions
  - Command-line build instructions with JAVA_HOME setup
  - Troubleshooting section
- Updated `INDEX.md` to link to BUILD.md

### Files Modified Summary

| File | Change Type |
|------|-------------|
| `RecordingSetupScreen.kt` | Permission denied UI, accessibility |
| `HomeScreen.kt` | Icon accessibility, semantic headings |
| `RecordingScreen.kt` | Icon accessibility |
| `ResultsScreen.kt` | Loading state, empty state, icon accessibility, headings |
| `ResultsViewModel.kt` | Loading state flow |
| `CameraDetailScreen.kt` | Icon accessibility, semantic headings |
| `ImportScreen.kt` | Icon accessibility, semantic headings, package rename |
| `ImportViewModel.kt` | Package rename |
| `NavGraph.kt` | Import path update |
| `ShutterCameraManager.kt` | Better error messages |
| `RecordingViewModel.kt` | Zero events handling |
| `ShutterSpeedCalculator.kt` | Static formatShutterSpeed |
| `VideoAnalyzer.kt` | Injected dependencies, correct method calls |
| `app/build.gradle.kts` | Added material-icons-extended |
| `res/drawable/` | Launcher icon resources |
| `res/mipmap-anydpi-v26/` | Adaptive icon definitions |
| `docs/BUILD.md` | NEW - Build guide |

### Build Status
- Build: ✅ Successful
- Warnings: 4 deprecation warnings (LocalLifecycleOwner, menuAnchor, statusBarColor) - non-blocking

### Next Steps
- Phase 7 Part 2: Comprehensive Bug Fixes ✅

---

## 2025-01-25 - Phase 7.5: Comprehensive Bug Fixes

### Overview
Fixed all bugs identified in a comprehensive code audit, addressing CRITICAL, HIGH, and MEDIUM priority issues to make the app production-ready.

### CRITICAL Fixes

#### 1. MainActivity - runBlocking on UI Thread
- **Problem**: `runBlocking { settingsRepository.setHasSeenOnboarding(true) }` blocked the UI thread
- **Fix**: Changed to `lifecycleScope.launch { }` for non-blocking execution
- **File**: `MainActivity.kt`

#### 2. ShutterCameraManager - Thread-Unsafe Mutable State
- **Problem**: `videoCapture`, `recording`, `imageAnalysis` accessed from different threads without synchronization
- **Fix**: Added `@Volatile` annotation to ensure visibility across threads
- **File**: `data/camera/ShutterCameraManager.kt`

#### 3. ImportViewModel - createSession Returns Null
- **Problem**: `createSession()` launched coroutine but returned before completion, returning null
- **Fix**: Changed function to return `Unit`, added proper error handling with try-catch
- **File**: `ui/screens/videoimport/ImportViewModel.kt`

### HIGH Priority Fixes

#### 4. VideoAnalyzer - Better FPS Estimation
- **Problem**: `estimateFrameRate()` always returned hardcoded 30 FPS
- **Fix**: Added frame count extraction on API 28+ using `METADATA_KEY_VIDEO_FRAME_COUNT`
- **File**: `data/video/VideoAnalyzer.kt`

#### 5. RecordingViewModel - advanceToNextSpeed Race Condition
- **Problem**: `observeCalibration()` called `advanceToNextSpeed()` which incremented index from 0 to 1, skipping the first speed
- **Fix**:
  - Changed `observeCalibration()` to call `startWaitingForFirstShutter()` instead
  - Added `lastProcessedEventCount` to track processed events
  - Added state check in `observeEvents()` to only process when in `WaitingForShutter` state
  - Reset counters in `startRecording()`
- **File**: `ui/screens/recording/RecordingViewModel.kt`

### MEDIUM Priority Fixes

#### 6. ResultsViewModel - Missing Error Handling
- **Problem**: `loadSessionAndCalculate()` had no try-catch, database errors would crash the app
- **Fix**: Added try-catch with `errorMessage: StateFlow<String?>` for UI error display
- **File**: `ui/screens/results/ResultsViewModel.kt`

#### 7. ThresholdCalculator - Empty List Handling
- **Problem**: `require(brightnessValues.isNotEmpty())` would throw exception on empty input
- **Fix**: Return sensible defaults (all zeros) instead of throwing
- **File**: `analysis/ThresholdCalculator.kt`

### Already Addressed (Verified)
- Chart components already had empty state checks (`if (results.isEmpty()) return`)
- OnboardingScreen doesn't request permissions (handled in RecordingSetupScreen)
- BrightnessAnalyzer division by zero handled by OpenCV's `Core.mean()`
- Extension functions `median()`, `percentile()`, `stdDev()` already return 0.0 for empty lists

### Files Modified Summary

| File | Priority | Changes |
|------|----------|---------|
| `MainActivity.kt` | CRITICAL | Replace runBlocking with lifecycleScope.launch |
| `ShutterCameraManager.kt` | CRITICAL | Add @Volatile to videoCapture, recording, imageAnalysis |
| `ImportViewModel.kt` | CRITICAL | Make createSession return Unit, add error handling |
| `VideoAnalyzer.kt` | HIGH | Extract FPS from frame count on API 28+ |
| `RecordingViewModel.kt` | HIGH | Fix advanceToNextSpeed race condition with state checks |
| `ResultsViewModel.kt` | MEDIUM | Add try-catch and errorMessage state |
| `ThresholdCalculator.kt` | MEDIUM | Return defaults for empty input instead of throwing |

---

## 2025-01-25 - Camera Control Enhancements

### Overview
Added camera zoom and manual focus controls to the recording screen, giving users fine-grained control over framing and focus during shutter detection.

### Features Added

#### 1. Camera Zoom Control
- **Added**: Vertical zoom slider on the right side of the recording screen
- **Range**: 1x to maximum device zoom (typically 8x-10x)
- **Implementation**: Uses CameraX `CameraControl.setZoomRatio()`
- **Files**: `ShutterCameraManager.kt`, `RecordingViewModel.kt`, `RecordingScreen.kt`

#### 2. Manual Focus Control
- **Added**: Vertical focus slider with autofocus toggle icon
- **Range**: Near (0) to Infinity (1)
- **Autofocus toggle**: Click the focus icon to enable autofocus
- **Implementation**: Uses CameraX `CameraControl.cancelFocusAndMetering()` to disable continuous AF
- **Files**: `ShutterCameraManager.kt`, `RecordingViewModel.kt`, `RecordingScreen.kt`

#### 3. Setup Phase Before Detection
- **Added**: `RecordingState.SettingUp` state
- **Flow**: Recording starts → SettingUp phase → User adjusts zoom/focus → "Begin Detecting" → Calibration
- **Benefit**: Users can frame and focus before detection begins

### UI Layout
- Vertical sliders on the right edge of the screen
- Zoom slider on far right with ZoomIn icon
- Focus slider to the left of zoom with CenterFocusStrong icon (autofocus toggle)
- "Begin Detecting" button positioned to avoid overlapping sliders

---

## 2025-01-25 - Event Review: Frame Thumbnails

### Overview
Added actual video frame thumbnails to the Event Review screen, allowing users to visually verify shutter open/close events instead of only seeing colored boxes with brightness numbers.

### Changes Made

#### New File: `data/video/FrameExtractor.kt`
- Extracts frame thumbnails from video at specific frame numbers
- Uses `MediaMetadataRetriever` for frame extraction
- `extractFrame()` - single frame extraction
- `extractFrames()` - batch extraction with single retriever instance for efficiency
- Scales frames to configurable thumbnail width (default 160px)

#### Updated: `ui/screens/review/EventReviewViewModel.kt`
- Added `FrameExtractor` injection
- Added `isLoadingThumbnails: StateFlow<Boolean>` for loading state
- Added `extractThumbnails()` method to load all frame images in background
- Updated `createReviewEvent()` to accept and use thumbnail map
- Thumbnails loaded on IO dispatcher, UI updated on Main

#### Updated: `ui/screens/review/EventReviewScreen.kt`
- Added loading indicator while thumbnails are being extracted
- Updated `FrameThumbnail` composable:
  - Shows actual frame image when available
  - Maintains color-coded border (green=full, orange=partial, blue=context)
  - Shows placeholder with "..." while loading
  - Displays frame number and brightness in info bar below image
  - Excluded frames show dark overlay with "✕"
  - Increased thumbnail width to 80dp for better visibility

---

## 2025-01-25 - Two-Phase Calibration with Peak Detection

### Overview
Improved the calibration system to use a two-phase approach that captures actual peak brightness from a calibration shutter event, resulting in more reliable threshold detection across different lighting conditions.

### Problem
The previous calibration calculated the threshold from dark frames only:
```
threshold = baseline + (median - baseline) * 1.5
```
This guessed what "bright" would look like without ever seeing an actual shutter event, leading to:
- Potential false triggers in bright ambient conditions
- Missed events if actual brightness was different than expected

### Solution
Two-phase calibration that captures the actual brightness range:

**Phase 1 - Baseline Establishment:**
- Collect 60 dark frames (shutter closed)
- Calculate baseline = 25th percentile
- Calculate preliminary threshold using robust noise detection:
  ```
  preliminaryThreshold = max(
      baseline + stdDev * 5,    // 5-sigma statistical outlier
      baseline + 50,            // Absolute minimum increase
      maxSeenDuringBaseline * 2 // 2x the brightest noise
  )
  ```

**Phase 2 - Calibration Shutter:**
- User fires shutter once to calibrate
- System captures peak brightness during this event
- Calculate final threshold:
  ```
  threshold = baseline + (peak - baseline) * 0.8
  ```
- **This event is discarded** - not counted toward measurements

### UI Flow

1. User taps "Begin Detecting"
2. **CalibratingBaseline** state: "Establishing baseline..." with progress bar (0-50%)
3. **WaitingForCalibrationShutter** state: "Fire Shutter Once" prompt with camera icon and note "(This event will not be recorded)"
4. User fires calibration shutter → system captures peak
5. **WaitingForShutter** state: Normal speed prompts begin (e.g., "1/1000")

---

## 2025-01-25 - Frame Index Alignment Fix

### Overview
Fixed a bug causing frame index misalignment between live detection timestamps and video frame positions. The issue caused event review thumbnails to show wrong frames (offset by the duration of the SettingUp phase).

### Root Cause
When `beginDetection()` was called, it triggered `resetCalibration()` → `frameAnalyzer.reset()` which cleared `firstFrameTimestamp` to 0L. This caused the frame index reference point to shift from the recording start time (T0) to the calibration start time (T1).

### Fix Applied

#### 1. `FrameAnalyzer.kt`
Separated timestamp management from calibration reset:

```kotlin
// reset() - for recalibration (does NOT reset timestamp)
fun reset() {
    liveEventDetector.reset()
    // DO NOT reset firstFrameTimestamp here
}

// resetForNewRecording() - for starting a new recording session
fun resetForNewRecording() {
    liveEventDetector.reset()
    firstFrameTimestamp = 0L
    hasFirstFrameTimestamp = false
}
```

#### 2. `ShutterCameraManager.kt`
- `startRecording()` now calls `frameAnalyzer.resetForNewRecording()` instead of `resetEvents()`
- `resetCalibration()` continues calling `frameAnalyzer.reset()` (no timestamp reset)

#### 3. `RecordingViewModel.kt`
- Removed video re-analysis step (`analyzeRecordedVideo()`)
- Events are now saved directly from live detection via `saveLiveDetectedEvents()`
- Removed unused `videoAnalyzer` dependency

---

## 2025-01-25 - Bug Fix Sprint

### Overview
Fixed 5 bugs reported in testing:
1. "How It Works" button in Settings not working
2. Camera with 0-event sessions crashing when opened
3. Zoom/focus sliders persisting after setup phase
4. Event frames not all clickable
5. "Confirm & Calculate" crashing

### Bug 1: "How It Works" Button
**Problem:** Settings screen had a TODO comment instead of navigation
**Fix:**
- Created `TheoryScreen.kt` with shutter measurement theory content
- Added `Screen.Theory` route to NavGraph
- Added `onViewTheory` callback to SettingsScreen
- Wired navigation from Settings → Theory

### Bug 2: 0-Event Session Crash
**Problem:** Tapping on a session with 0 events crashed the app
**Fix:**
- Made session cards with 0 events non-clickable
- Added "No events recorded" label for empty sessions
- Added error message display in ResultsScreen

### Bug 3: Sliders Persisting After Setup
**Problem:** Zoom/focus sliders appeared during WaitingForShutter state
**Fix:** Removed slider UI from `WaitingForShutterOverlay`, kept only in `SettingUpOverlay`

### Bug 4: Frame Clickability with State Cycling
**Problem:** Only non-context frames were clickable; users wanted all frames toggleable
**Fix:**
- Added `FrameState` enum: FULL, PARTIAL, EXCLUDED
- Made all frames (including context) clickable
- Tap cycles: Full → Partial → Excluded → (back to auto/Full)
- Updated visual display to show manual state overrides

### Bug 5: "Confirm & Calculate" Crash
**Problem:** Button could be clicked with no events or no included frames
**Fix:**
- Added `hasIncludedFrames` check to BottomControls
- Button disabled when no events or no included frames
- Button text changes to explain why it's disabled

---

## 2025-01-25 - Expandable Frame Boundaries Feature

### Overview
Added the ability for users to expand event boundaries in the Event Review screen by adding more frames before/after the detected event. This helps when automatic detection missed the start or end of a shutter event.

### Problem
Automatic event detection sometimes clips event boundaries too tightly:
- Edge frames with partial brightness may be excluded
- User needs to verify if adjacent frames should be included

### Solution
- Reduced default context frames from 2 to 1 on each side
- Added [+] "Examine more" buttons on left and right of frame grid
- Clicking [+] adds 3 frames from that direction (excluded by default)
- Users can tap new frames to cycle their state (Full/Partial/Excluded)
- Can click [+] multiple times to keep adding frames

### Visual Layout
```
┌───────────────────────────────────────────────────────────┐
│ [+]  │ [ctx] │ [EVT] [EVT] [EVT] [EVT] │ [ctx] │  [+]    │
│ more │  14   │  187   245   242   95   │  15   │  more   │
│      │ gray  │ green green green orange│ gray  │         │
└───────────────────────────────────────────────────────────┘
```

---

## 2025-01-25 - Video Analysis Performance Optimization

### Problem
Video import analysis was extremely slow due to `MediaMetadataRetriever.getFrameAtTime()`:
- **10-50ms per frame** (seeks to keyframe, decodes each time)
- 7-minute video (12,998 frames) took **~6.5 minutes** to analyze

### Solution
Implemented `SequentialFrameDecoder` using Android's MediaCodec API:
- **0.5-2ms per frame** (continuous hardware-accelerated decoding)
- Same video now analyzes in **~15-30 seconds** (10-50x faster)

### Key Discovery
OpenCV VideoCapture does NOT work for MP4/H.264 on Android. The Maven package `org.opencv:opencv:4.9.0` lacks FFmpeg/MediaNDK backends. Only reads MJPEG/AVI.

### Implementation

**New File: `data/video/SequentialFrameDecoder.kt`**
- MediaExtractor + MediaCodec wrapper
- Extracts Y-plane (luminance) only
- Reuses byte array buffer
- Supports content:// URIs natively

**Modified: `data/video/VideoAnalyzer.kt`**
- `analyzeWithMediaCodec()` - new fast path
- `analyzeWithMediaMetadataRetriever()` - existing code as fallback
- `analyzeVideo()` - tries MediaCodec first, falls back if needed

### Performance Comparison

| Metric | Before (MMR) | After (MediaCodec) |
|--------|--------------|-------------------|
| Per-frame | 10-50ms | 0.5-2ms |
| 12,998 frames | ~6.5 min | ~15-30 sec |
| Memory | New Bitmap/frame | Reused buffer |

---

## 2025-01-25 - Bug Fix Round 2

### Overview
Fixed 4 bugs identified during testing to improve detection stability, event review UX, and data persistence.

### Bug 1: Lock Camera Exposure During Detection
**Problem:** Auto-exposure (AE) changes during detection could cause inconsistent brightness readings, potentially causing missed or false events.

**Fix:**
- Added `lockExposure()` method to `ShutterCameraManager.kt`
  - Uses `Camera2CameraControl.setCaptureRequestOptions()` with `CONTROL_AE_LOCK = true`
- Added `unlockExposure()` method for cleanup
- Called `lockExposure()` in `RecordingViewModel.beginDetection()`
- Called `unlockExposure()` in `RecordingViewModel.stopRecording()`

### Bug 2: Closed Frames Shouldn't Be Greyed Out
**Problem:** Excluded frames had `Color.Black.copy(alpha = 0.5f)` overlay obscuring the thumbnail image.

**Fix:**
- Removed dark background overlay from excluded frames
- Changed X mark color from white to `MaterialTheme.colorScheme.error` (red) for visibility
- Thumbnail now remains fully visible with only X mark overlay

### Bug 3: Simplify to Only 3 Options (Full, Partial, Closed)
**Problem:** Context frames appeared visually distinct (blue border) from closed/excluded frames when they should be treated the same.

**Fix:**
- Unified context frames visually with closed frames:
  - Red border for both context and excluded frames
  - Same placeholder color (gray)
  - Same info bar background color
  - State label shows "out" instead of "ctx"
  - Bold text for all closed frames
- Simplified Legend to only show: Full, Partial, Closed (removed Context)

### Bug 4: Events Not Being Saved After Detection
**Problem:** `saveLiveDetectedEvents()` had no error handling - exceptions would silently fail and events could be lost.

**Fix:**
- Added comprehensive try-catch around all save operations
- Added logging for success and failure cases
- Database operations continue even if one fails (e.g., video URI saved even if events fail)
- Errors logged with stack traces for debugging

### Bug 5: Calculate Crashes (ResultsViewModel NPE)
**Problem:** Navigating to Results screen crashed with `NullPointerException` at `ResultsViewModel.kt:116`

**Root Cause:** Property initialization order bug. `_errorMessage` was declared *after* the `init` block.

**Fix:** Moved `_errorMessage` declaration before the `init` block.

---

## 2025-01-25 - Import Flow & Results Display Improvements

### Overview
Enhanced import flow UX and fixed results display format.

### Import Flow → Event Review
**Problem:** Import flow went directly to Results, skipping frame review.

**Fix:** Changed navigation to go to EventReview screen after import so users can verify frames.

### Remove Fake Events in Import
**Problem:** Users couldn't remove false-positive shutter events detected during import.

**Fix:** Added delete button (X) on each event card in the speed assignment step.

### Results Display: Speed Format
**Problem:** Actual/measured speeds displayed in milliseconds (e.g., "8.0ms") instead of shutter speed format.

**Fix:** Added `formatAsSpeed()` method that converts ms to "1/x" format where x is rounded to nearest int.

---

## 2025-01-25 - Event Preview Feature for Import Flow

### Overview
Added the ability for users to preview event frames during the import flow before creating a session. Users can click on an event in the speed assignment list to see its frames, verify it's a real shutter event, and delete false positives.

### User Flow
```
Import → Analyze → Speed Assignment ↔ Event Preview → Create Session → EventReview → Results
                         ↑                  ↓
                         └──── Back/Delete ─┘
```

### Files Created

#### `ui/screens/videoimport/EventPreviewScreen.kt` (NEW)
Displays frames for a single event during import preview.

**Key Features:**
- Top bar with back button, "Event X of Y" title
- Event info card (frame count, duration, frame range, assigned speed)
- Frame grid showing thumbnails with brightness info
- Context frames (2 before/after event) with "out" labels
- Sampling for long events (max 12 frames displayed)
- "Delete Event" button (red, bottom of screen)
- Loading state while extracting thumbnails

---

## 2025-01-25 - Thumbnail Loading Optimization for Event Review

### Problem
Event review thumbnail loading was slow because all events were extracted upfront:
- **Previous behavior**: Extract thumbnails for ALL events before showing any
- **Example**: 10 events × 16 frames = 160 frames × 20ms = 3.2 seconds wait

### Solution
Implemented lazy per-event loading with prefetch strategy:
- Load thumbnails for current event only (fast initial load)
- Prefetch next event in background
- LRU cache keeps max 5 events' thumbnails in memory

### Performance Targets

| Metric | Before | After |
|--------|--------|-------|
| Initial load (10 events) | 3-5 seconds | <500ms |
| Navigate to next event | instant | instant (prefetched) |
| Navigate to distant event | instant | ~300ms (on-demand) |
| Memory (10 events) | ~10MB all loaded | ~5MB (5 event LRU) |

---

## 2025-01-25 - UI/UX Improvements

### Overview
Implemented 10 UI/UX improvements based on user feedback to enhance usability and visual consistency.

### Changes Made

#### Task 1: Fix ExpandFramesButton Sizing
**Problem:** Button used `aspectRatio(9f/16f)` (portrait ~142dp tall) but frame thumbnails use `aspectRatio(16f/9f)` (landscape ~45dp tall). Button was 3x taller than frames.

**Fix:** Changed to `aspectRatio(16f/9f)` to match frame thumbnails.

#### Task 2: Change Expand Button Text
**Problem:** Both buttons showed generic "Examine\nmore" text.

**Fix:** Added `isLeft: Boolean` parameter:
- Left button: "Earlier\nframes"
- Right button: "Later\nframes"

#### Task 3: Update Accuracy Color Thresholds
**Problem:** Old thresholds (0-5%, 5-10%, 10-15%, 15%+) were too strict for typical film camera variance.

**Fix:** Updated to more realistic thresholds:
| Range | Color | Label |
|-------|-------|-------|
| 0-10% | Green | Good |
| 10-30% | Yellow | Fair |
| 30-60% | Orange | Poor |
| 60%+ | Red | Bad |

#### Task 4: Results Table Format - Fractions Only
**Fix:**
- Removed "Speed" column (redundant with Expected)
- Changed "Expected" to show fraction (e.g., "1/500")
- Changed "Actual" header to "Measured"
- Removed ms display entirely

#### Task 5: Default Camera Name When Empty
**Fix:** Always create a camera entry:
- If name provided: use provided name
- If blank: generate default "Test Jan 25 14:30" using current datetime

#### Task 6: Move "Begin Detecting" Button
**Fix:** Moved to `Alignment.BottomCenter` with `padding(bottom = 32.dp)` for thumb-friendly access.

#### Task 7: Reposition Zoom/Focus Sliders
**Fix:** Changed from `Row` to `Column` layout:
- Zoom slider on top
- Focus slider on bottom
- Both aligned to right edge

#### Task 9: Custom Speeds - 3 Options
**Fix:** Added `SpeedSelectionMode` enum with 3 options:
| Mode | Description |
|------|-------------|
| `STANDARD` | Uses STANDARD_SPEEDS list as-is (1/1000 to 1s) |
| `ADD_REMOVE` | Checkbox picker from ALL_SPEEDS (1/8000 to 8s) |
| `ENTER_CUSTOM` | Text input for comma-separated speeds |

#### Task 10: Help Icon on Home Screen
**Fix:** Added Help icon with dropdown menu in top app bar:
- Help icon (question mark) next to Settings icon
- Dropdown menu with "Tutorial" and "How It Works" options

---

## 2025-01-25 - Debug: Event Saving Investigation

### Problem
Events are detected (user sees "DETECTED!") but not persisted when test is saved.

### Analysis
Detection works, so the issue is in the save path:
```
Events detected → _detectedEvents accumulates → stopRecording() → onComplete callback
→ saveLiveDetectedEvents() → ??? events lost ???
```

### Changes Made
Added critical logging to trace event flow:

**RecordingViewModel.kt:**
- `saveLiveDetectedEvents()`: Log sessionId, marker count, and individual marker details
- `stopRecording()`: Log current speed index and detected events count
- `onComplete` callback: Log when callback fires

**ShutterCameraManager.kt:**
- `onEventDetected` callback: Log each event as it's detected with timestamps
- `stopRecording()`: Log event count and details before stopping
- `startRecording()`: Log when events are reset

### Files Modified
| File | Change |
|------|--------|
| `ui/screens/recording/RecordingViewModel.kt` | Added save flow logging |
| `data/camera/ShutterCameraManager.kt` | Added event lifecycle logging |

### Verification Steps
1. Build and install app
2. Run test, fire 2-3 shutters (see DETECTED!)
3. Click "Done"
4. Run: `adb logcat -d | grep -i "RecordingViewModel\|ShutterCamera"`
5. Look for:
   - Event count when detected
   - Event count at save time
   - sessionId value

---
