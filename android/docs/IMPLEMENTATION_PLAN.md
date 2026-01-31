# Shutter Analyzer - Android Implementation Plan

**Status**: Implementation Complete
**Last Updated**: 2025-01-31

> **Note**: This document was created as a planning guide before implementation. The actual implementation has been completed and the codebase may have evolved beyond this plan. See [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md) for what was actually built.

---

## Overview

This document outlines the implementation plan for the Android app, leveraging the existing Python algorithms from `shutter_analyzer/`.

### Key Principle: Port, Don't Rewrite

The core analysis algorithms are already implemented and tested in Python. We will **port these directly to Kotlin**, not reinvent them.

| Python Module | Kotlin Equivalent | Notes |
|---------------|-------------------|-------|
| `frame_analyzer.py` | `BrightnessAnalyzer.kt` | Same math, OpenCV Android |
| `shutter_calculator.py` | `ShutterSpeedCalculator.kt` | Direct port |
| `video_processor.py` | CameraX handles this | No need to port |

---

## Algorithm Summary (from Python)

### 1. Brightness Calculation
```python
# Python (frame_analyzer.py:45-64)
def calculate_frame_brightness(frame):
    gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    return float(np.mean(gray_frame))
```
```kotlin
// Kotlin equivalent
fun calculateBrightness(frame: Mat): Double {
    val gray = Mat()
    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
    return Core.mean(gray).`val`[0]
}
```

### 2. Threshold Detection (Original Method)
```python
# Python (frame_analyzer.py:119-126)
baseline = percentiles[25]  # 25th percentile
brightness_range = median_brightness - baseline
threshold = baseline + (brightness_range * 1.5)
```

### 3. Event Detection
```python
# Python (frame_analyzer.py:156-203)
for frame_index, brightness in enumerate(brightness_values):
    is_open = brightness > threshold
    if is_open and current_event is None:
        current_event = frame_index  # Event started
    elif not is_open and current_event is not None:
        events.append((current_event, frame_index - 1))  # Event ended
```

### 4. Weighted Duration (for partial-open frames)
```python
# Python (shutter_calculator.py:57-98)
event_peak = median(brightness_values)
for brightness in brightness_values:
    weight = (brightness - baseline) / (event_peak - baseline)
    weight = clamp(weight, 0.0, 1.0)
    total += weight
```

### 5. Shutter Speed Calculation
```python
# Python (shutter_calculator.py:167-200)
duration = frame_count / recording_fps
shutter_speed = 1.0 / duration  # e.g., 500 for 1/500s
```

---

## Project Structure

```
android/
├── app/
│   ├── src/main/java/com/shutteranalyzer/
│   │   ├── ShutterAnalyzerApp.kt
│   │   ├── MainActivity.kt
│   │   │
│   │   ├── ui/
│   │   │   ├── navigation/
│   │   │   │   └── NavGraph.kt
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Color.kt
│   │   │   │   └── Type.kt
│   │   │   ├── components/
│   │   │   │   ├── AccuracyTable.kt
│   │   │   │   ├── BrightnessChart.kt
│   │   │   │   ├── FrameThumbnail.kt
│   │   │   │   ├── SpeedPrompt.kt
│   │   │   │   └── ProgressIndicator.kt
│   │   │   └── screens/
│   │   │       ├── home/
│   │   │       │   ├── HomeScreen.kt
│   │   │       │   └── HomeViewModel.kt
│   │   │       ├── onboarding/
│   │   │       │   └── OnboardingScreen.kt
│   │   │       ├── setup/
│   │   │       │   ├── RecordingSetupScreen.kt
│   │   │       │   └── RecordingSetupViewModel.kt
│   │   │       ├── recording/
│   │   │       │   ├── RecordingScreen.kt
│   │   │       │   └── RecordingViewModel.kt
│   │   │       ├── review/
│   │   │       │   ├── EventReviewScreen.kt
│   │   │       │   └── EventReviewViewModel.kt
│   │   │       ├── results/
│   │   │       │   ├── ResultsDashboard.kt
│   │   │       │   └── ResultsViewModel.kt
│   │   │       ├── camera/
│   │   │       │   ├── CameraDetailScreen.kt
│   │   │       │   └── CameraDetailViewModel.kt
│   │   │       ├── import/
│   │   │       │   ├── ImportScreen.kt
│   │   │       │   └── ImportViewModel.kt
│   │   │       └── settings/
│   │   │           ├── SettingsScreen.kt
│   │   │           └── SettingsViewModel.kt
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Camera.kt
│   │   │   │   ├── TestSession.kt
│   │   │   │   ├── ShutterEvent.kt
│   │   │   │   ├── ShutterSpeed.kt
│   │   │   │   └── BrightnessStats.kt
│   │   │   └── usecase/
│   │   │       ├── AnalyzeFrameUseCase.kt
│   │   │       ├── DetectShutterEventUseCase.kt
│   │   │       ├── CalculateShutterSpeedUseCase.kt
│   │   │       ├── SaveTestSessionUseCase.kt
│   │   │       └── GetCameraHistoryUseCase.kt
│   │   │
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── CameraEntity.kt
│   │   │   │   │   │   ├── TestSessionEntity.kt
│   │   │   │   │   │   └── ShutterEventEntity.kt
│   │   │   │   │   └── dao/
│   │   │   │   │       ├── CameraDao.kt
│   │   │   │   │       ├── TestSessionDao.kt
│   │   │   │   │       └── ShutterEventDao.kt
│   │   │   │   └── datastore/
│   │   │   │       └── SettingsDataStore.kt
│   │   │   ├── repository/
│   │   │   │   ├── CameraRepository.kt
│   │   │   │   └── AnalysisRepository.kt
│   │   │   └── camera/
│   │   │       ├── CameraManager.kt
│   │   │       └── FrameAnalyzer.kt
│   │   │
│   │   ├── analysis/                          # PORTED FROM PYTHON
│   │   │   ├── BrightnessAnalyzer.kt          # from frame_analyzer.py
│   │   │   ├── ThresholdCalculator.kt         # threshold methods
│   │   │   ├── EventDetector.kt               # event finding logic
│   │   │   └── ShutterSpeedCalculator.kt      # from shutter_calculator.py
│   │   │
│   │   └── di/
│   │       ├── AppModule.kt
│   │       ├── DatabaseModule.kt
│   │       ├── CameraModule.kt
│   │       └── AnalysisModule.kt
│   │
│   ├── src/main/res/
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   │       └── (icons)
│   │
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Implementation Phases

### Phase 1: Project Setup & Core Analysis (Foundation)
**Goal**: Get the analysis algorithms working in Kotlin

| Task | Description | Files |
|------|-------------|-------|
| 1.1 | Create Android project with Compose | Project scaffold |
| 1.2 | Add dependencies (OpenCV, Room, Hilt, CameraX) | `build.gradle.kts` |
| 1.3 | Port `BrightnessAnalyzer` | `analysis/BrightnessAnalyzer.kt` |
| 1.4 | Port `ThresholdCalculator` | `analysis/ThresholdCalculator.kt` |
| 1.5 | Port `EventDetector` | `analysis/EventDetector.kt` |
| 1.6 | Port `ShutterSpeedCalculator` | `analysis/ShutterSpeedCalculator.kt` |
| 1.7 | Write unit tests for analysis | `test/analysis/*Test.kt` |

**Deliverable**: Analysis code passes unit tests with same results as Python

---

### Phase 2: Data Layer
**Goal**: Camera profiles and test sessions persist to database

| Task | Description | Files |
|------|-------------|-------|
| 2.1 | Define Room entities | `data/local/database/entity/*.kt` |
| 2.2 | Create DAOs | `data/local/database/dao/*.kt` |
| 2.3 | Create AppDatabase | `data/local/database/AppDatabase.kt` |
| 2.4 | Create repositories | `data/repository/*.kt` |
| 2.5 | Create domain models | `domain/model/*.kt` |
| 2.6 | Create use cases | `domain/usecase/*.kt` |
| 2.7 | Set up Hilt modules | `di/*.kt` |

**Deliverable**: Can save/load cameras and test sessions

---

### Phase 3: Camera Integration
**Goal**: Record slow-motion video and analyze frames in real-time

| Task | Description | Files |
|------|-------------|-------|
| 3.1 | Set up CameraX preview | `data/camera/CameraManager.kt` |
| 3.2 | Implement slow-mo detection | Device capability check |
| 3.3 | Create `ImageAnalysis.Analyzer` | `data/camera/FrameAnalyzer.kt` |
| 3.4 | Implement real-time brightness monitoring | Live threshold detection |
| 3.5 | Implement event clipping | Save video segments |
| 3.6 | Handle camera permissions | Runtime permissions |

**Deliverable**: Can record video and detect shutter events in real-time

---

### Phase 4: UI - Core Screens
**Goal**: Implement the main user flow

| Task | Description | Files |
|------|-------------|-------|
| 4.1 | Set up navigation | `ui/navigation/NavGraph.kt` |
| 4.2 | Create theme | `ui/theme/*.kt` |
| 4.3 | Home screen | `ui/screens/home/*` |
| 4.4 | Recording setup screen | `ui/screens/setup/*` |
| 4.5 | Recording screen (core) | `ui/screens/recording/*` |
| 4.6 | Event review screen | `ui/screens/review/*` |
| 4.7 | Results dashboard | `ui/screens/results/*` |

**Deliverable**: Complete recording flow works end-to-end

---

### Phase 5: UI - Secondary Screens
**Goal**: Complete all screens

| Task | Description | Files |
|------|-------------|-------|
| 5.1 | Onboarding tutorial | `ui/screens/onboarding/*` |
| 5.2 | Camera detail/history | `ui/screens/camera/*` |
| 5.3 | Import flow | `ui/screens/import/*` |
| 5.4 | Settings | `ui/screens/settings/*` |
| 5.5 | Reusable components | `ui/components/*.kt` |

**Deliverable**: All screens implemented

---

### Phase 6: Charts & Visualization
**Goal**: Implement dashboard visualizations

| Task | Description | Files |
|------|-------------|-------|
| 6.1 | Accuracy table component | `ui/components/AccuracyTable.kt` |
| 6.2 | Brightness timeline chart | `ui/components/BrightnessChart.kt` |
| 6.3 | Speed comparison graph | Part of results screen |
| 6.4 | Frame thumbnail grid | `ui/components/FrameThumbnail.kt` |

**Deliverable**: Rich dashboard with all visualizations

---

### Phase 7: Polish & Edge Cases
**Goal**: Handle all edge cases and polish UX

| Task | Description |
|------|-------------|
| 7.1 | Error handling (permissions, storage, camera) |
| 7.2 | Empty states |
| 7.3 | Loading states |
| 7.4 | Frame rate warning dialog |
| 7.5 | Haptic feedback |
| 7.6 | Accessibility |
| 7.7 | Dark theme support |

**Deliverable**: Production-ready app

---

### Phase 8: Testing & Release
**Goal**: Prepare for Play Store

| Task | Description |
|------|-------------|
| 8.1 | Integration tests |
| 8.2 | UI tests (Compose testing) |
| 8.3 | Test on multiple devices |
| 8.4 | Performance profiling |
| 8.5 | Create release build |
| 8.6 | Generate screenshots for store |
| 8.7 | Submit to Play Store |

**Deliverable**: App published

---

## Porting Guide: Python → Kotlin

### Data Classes

**Python (`shutter_calculator.py`)**:
```python
class ShutterEvent:
    def __init__(self, start_frame, end_frame, brightness_values, ...):
        self.start_frame = start_frame
        # ...

    @property
    def duration_frames(self):
        return self.end_frame - self.start_frame + 1
```

**Kotlin**:
```kotlin
data class ShutterEvent(
    val startFrame: Int,
    val endFrame: Int,
    val brightnessValues: List<Double>,
    val baselineBrightness: Double? = null,
    val peakBrightness: Double? = null
) {
    val durationFrames: Int
        get() = endFrame - startFrame + 1

    val weightedDurationFrames: Double
        get() {
            // Same algorithm as Python
        }
}
```

### Statistics

**Python**:
```python
import numpy as np
import statistics

mean = statistics.mean(values)
median = statistics.median(values)
percentile = np.percentile(values, 25)
std = np.std(values)
```

**Kotlin**:
```kotlin
// Option 1: Manual implementation
fun List<Double>.median(): Double {
    val sorted = this.sorted()
    return if (size % 2 == 0) {
        (sorted[size/2 - 1] + sorted[size/2]) / 2.0
    } else {
        sorted[size/2]
    }
}

fun List<Double>.percentile(p: Int): Double {
    val sorted = this.sorted()
    val index = (p / 100.0 * (size - 1)).toInt()
    return sorted[index]
}

// Option 2: Apache Commons Math (add dependency)
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
val stats = DescriptiveStatistics(values.toDoubleArray())
val mean = stats.mean
val median = stats.getPercentile(50.0)
```

### OpenCV

**Python**:
```python
import cv2
import numpy as np

gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
brightness = np.mean(gray)
```

**Kotlin (OpenCV Android)**:
```kotlin
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

fun calculateBrightness(frame: Mat): Double {
    val gray = Mat()
    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
    val mean = Core.mean(gray)
    gray.release()  // Important: release Mat to avoid memory leaks
    return mean.`val`[0]
}
```

### Converting CameraX ImageProxy to OpenCV Mat

```kotlin
// In FrameAnalyzer.kt
class FrameAnalyzer : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val mat = imageProxyToMat(imageProxy)
        val brightness = calculateBrightness(mat)
        mat.release()
        imageProxy.close()
    }

    private fun imageProxyToMat(imageProxy: ImageProxy): Mat {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuv = Mat(imageProxy.height + imageProxy.height / 2, imageProxy.width, CvType.CV_8UC1)
        yuv.put(0, 0, nv21)

        val rgb = Mat()
        Imgproc.cvtColor(yuv, rgb, Imgproc.COLOR_YUV2BGR_NV21)
        yuv.release()

        return rgb
    }
}
```

---

## Critical Implementation Notes

### 1. Real-Time Detection Strategy

During recording, we need to detect events WITHOUT processing the full video:

```kotlin
class LiveEventDetector(
    private val onEventDetected: (timestamp: Long) -> Unit
) {
    private var baseline: Double? = null
    private var threshold: Double? = null
    private var calibrationFrames = mutableListOf<Double>()
    private var isCalibrating = true
    private var eventInProgress = false
    private var eventStartTimestamp: Long? = null

    // Called for each frame during recording
    fun processFrame(brightness: Double, timestamp: Long) {
        if (isCalibrating) {
            calibrationFrames.add(brightness)
            if (calibrationFrames.size >= CALIBRATION_FRAME_COUNT) {
                finishCalibration()
            }
            return
        }

        val isOpen = brightness > threshold!!

        if (isOpen && !eventInProgress) {
            // Event started
            eventInProgress = true
            eventStartTimestamp = timestamp
        } else if (!isOpen && eventInProgress) {
            // Event ended
            eventInProgress = false
            onEventDetected(eventStartTimestamp!!)
            eventStartTimestamp = null
        }
    }

    private fun finishCalibration() {
        baseline = calibrationFrames.percentile(25)
        val median = calibrationFrames.median()
        threshold = baseline!! + (median - baseline!!) * 1.5
        isCalibrating = false
        calibrationFrames.clear()
    }

    companion object {
        const val CALIBRATION_FRAME_COUNT = 60  // ~0.5s at 120fps
    }
}
```

### 2. Video Clipping Strategy

We need to save clips for each detected event:

```kotlin
// Option A: Record full video, mark timestamps, extract clips later
data class EventMarker(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val expectedSpeed: String
)

// Option B: Use MediaMuxer to write clips on-the-fly (more complex)
```

**Recommendation**: Option A is simpler. Record full video, store markers, extract clips during processing phase.

### 3. Memory Management

OpenCV Mat objects must be released to avoid memory leaks:

```kotlin
fun processFrame(frame: Mat) {
    val gray = Mat()
    try {
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
        // ... use gray
    } finally {
        gray.release()
    }
}
```

### 4. Frame Rate Detection

```kotlin
suspend fun getMaxSlowMotionFps(context: Context): Int {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList.first { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        characteristics.get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_BACK
    }

    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val configs = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
    )

    val highSpeedSizes = configs?.highSpeedVideoSizes ?: emptyArray()
    val fpsRanges = configs?.highSpeedVideoFpsRanges ?: emptyArray()

    return fpsRanges.maxOfOrNull { it.upper } ?: 30
}
```

---

## Testing Strategy

### Unit Tests (Phase 1)
Test analysis algorithms with known inputs:

```kotlin
class BrightnessAnalyzerTest {
    @Test
    fun `calculate brightness returns correct value for black frame`() {
        val blackFrame = Mat.zeros(100, 100, CvType.CV_8UC3)
        val brightness = BrightnessAnalyzer.calculateBrightness(blackFrame)
        assertEquals(0.0, brightness, 0.01)
    }

    @Test
    fun `calculate brightness returns correct value for white frame`() {
        val whiteFrame = Mat(100, 100, CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        val brightness = BrightnessAnalyzer.calculateBrightness(whiteFrame)
        assertEquals(255.0, brightness, 0.01)
    }
}

class ShutterSpeedCalculatorTest {
    @Test
    fun `calculate speed for 1 frame at 240fps equals 1-240`() {
        val event = ShutterEvent(0, 0, listOf(200.0))
        val speed = ShutterSpeedCalculator.calculate(event, fps = 240.0)
        assertEquals(240.0, speed, 0.01)  // 1/240 second
    }
}
```

### Integration Tests (Phase 8)
Test with actual test videos from `videos/` folder.

---

## References

- [APP_SPEC.md](APP_SPEC.md) - Requirements and flows
- [WIREFRAMES.md](WIREFRAMES.md) - Screen mockups
- [TECH_STACK.md](TECH_STACK.md) - Technology decisions
- [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md) - Progress tracking
- [../../shutter_analyzer/](../../shutter_analyzer/) - Python implementation (source of truth for algorithms)
