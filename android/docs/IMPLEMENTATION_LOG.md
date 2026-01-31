# Android Implementation Log

This log tracks implementation progress for the Shutter Analyzer Android app.

---

## 2025-01-24 - Phase 0: Planning & Documentation

### Changes Made
- `android/docs/APP_SPEC.md`: Created app specification
  - Core design principles (guided experience, offline-first, camera-centric)
  - Full requirements gathering session with stakeholder responses
  - 10 screens defined with purpose, elements, and actions
  - User flows documented (new test, import, history, first-time user)
  - Technical considerations (frame rate accuracy, real-time detection)

- `android/docs/WIREFRAMES.md`: Created ASCII wireframes for all screens
  - Home screen (camera list + empty state)
  - Onboarding tutorial (5 screens)
  - Recording setup
  - Recording screen (4 states: calibrating, waiting, detected, progress)
  - Event review screen (frame toggle modal)
  - Processing screen
  - Results dashboard (+ expanded graph view)
  - Camera detail/history
  - Import flow (2 steps)
  - Settings
  - Color legend and frame rate warning modal

- `android/docs/TECH_STACK.md`: Created technology decisions document
  - Stack: Kotlin, Jetpack Compose, MVVM, CameraX, Room, Hilt
  - Full architecture diagram (UI → Domain → Data layers)
  - Code examples for each technology
  - Project folder structure
  - Complete `build.gradle.kts` dependencies
  - Database schema design

- `android/docs/IMPLEMENTATION_PLAN.md`: Created implementation plan
  - Algorithm summary from Python implementation
  - 8 implementation phases defined
  - Python → Kotlin porting guide with code examples
  - OpenCV Android integration (ImageProxy → Mat)
  - Real-time event detection strategy
  - Memory management notes
  - Testing strategy

- `android/docs/THEORY.md`: Moved from docs/ (shutter measurement theory)
- `android/docs/HOW_TO.md`: Moved from docs/ (usage guide)

- `android/`: Created root folder for Android project

### Documentation Structure
```
android/
└── docs/
    ├── APP_SPEC.md           # What to build
    ├── WIREFRAMES.md         # How it looks
    ├── TECH_STACK.md         # What technologies
    ├── IMPLEMENTATION_PLAN.md # How to build it
    ├── IMPLEMENTATION_LOG.md  # This file - progress tracking
    ├── THEORY.md             # Background theory
    └── HOW_TO.md             # User guide
```

### Key Decisions Made
| Decision | Choice | Rationale |
|----------|--------|-----------|
| Framework | Native Android | Full camera API access, best performance |
| Language | Kotlin | Modern, Google's preferred |
| UI | Jetpack Compose | Declarative, less boilerplate |
| Architecture | MVVM | Standard for Compose, good learning resources |
| Camera | CameraX | Simpler than Camera2, handles device quirks |
| Database | Room | Type-safe SQLite abstraction |
| Image Processing | OpenCV Android | Same algorithms as Python CLI |

### Next Steps
- Phase 1: Project Setup & Core Analysis ✅

---

## 2025-01-24 - Phase 1: Project Scaffold & Core Analysis Modules

### Changes Made

#### Project Scaffold
- `android/settings.gradle.kts`: Root Gradle settings
- `android/build.gradle.kts`: Root build file with plugin versions
- `android/gradle.properties`: Gradle and Kotlin configuration
- `android/gradle/wrapper/gradle-wrapper.properties`: Gradle 8.5 wrapper
- `android/app/build.gradle.kts`: App-level build with dependencies (OpenCV, Hilt, Compose, testing)
- `android/app/src/main/AndroidManifest.xml`: App manifest with camera and storage permissions
- `android/app/src/main/res/values/strings.xml`: App name resource
- `android/app/src/main/res/values/themes.xml`: Material theme
- `android/app/src/main/java/com/shutteranalyzer/ShutterAnalyzerApp.kt`: Application class with OpenCV initialization
- `android/app/src/main/java/com/shutteranalyzer/MainActivity.kt`: Placeholder activity with Compose

#### Data Models (ported from Python)
- `android/app/src/main/java/com/shutteranalyzer/analysis/model/BrightnessStats.kt`
  - Port of Python `FrameBrightnessStats` dataclass
  - Stores min/max/mean/median brightness, percentiles, baseline, threshold, peak

- `android/app/src/main/java/com/shutteranalyzer/analysis/model/ShutterEvent.kt`
  - Port of Python `ShutterEvent` class
  - Properties: `durationFrames`, `weightedDurationFrames`, `maxBrightness`, `avgBrightness`
  - Key algorithm: weighted duration using per-event median as plateau level
  - Extension functions: `median()`, `percentile()`, `stdDev()`

#### Analysis Modules (ported from Python)
- `android/app/src/main/java/com/shutteranalyzer/analysis/BrightnessAnalyzer.kt`
  - Port of `FrameAnalyzer.calculate_frame_brightness()`
  - Uses OpenCV to convert to grayscale and calculate mean
  - Memory-safe with Mat.release() in finally block

- `android/app/src/main/java/com/shutteranalyzer/analysis/ThresholdCalculator.kt`
  - Port of `FrameAnalyzer.analyze_brightness_distribution()`
  - Original method: `baseline + (median - baseline) * marginFactor`
  - Z-score method: iterates z=1.0-5.0 to find best match to expected events
  - DBSCAN deferred to later phase

- `android/app/src/main/java/com/shutteranalyzer/analysis/EventDetector.kt`
  - Port of `FrameAnalyzer.find_shutter_events()`
  - Port of `FrameAnalyzer.calculate_peak_brightness()` with plateau analysis
  - Handles video ending with shutter open

- `android/app/src/main/java/com/shutteranalyzer/analysis/ShutterSpeedCalculator.kt`
  - Port of `ShutterSpeedCalculator` class
  - `calculateShutterSpeed()`: converts weighted frames to 1/x speed
  - `compareWithExpected()`: percentage error calculation
  - `groupShutterEvents()`: matches events to expected speeds by duration
  - `formatShutterSpeed()`: human-readable formatting

#### Unit Tests
- `android/app/src/test/java/com/shutteranalyzer/analysis/TestVectors.kt`: Test data and expected values
- `android/app/src/test/java/com/shutteranalyzer/analysis/ExtensionFunctionsTest.kt`: Tests for median, percentile, stdDev
- `android/app/src/test/java/com/shutteranalyzer/analysis/ThresholdCalculatorTest.kt`: Threshold calculation tests
- `android/app/src/test/java/com/shutteranalyzer/analysis/EventDetectorTest.kt`: Event detection tests
- `android/app/src/test/java/com/shutteranalyzer/analysis/ShutterSpeedCalculatorTest.kt`: Speed calculation tests
- `android/app/src/test/java/com/shutteranalyzer/analysis/ShutterEventTest.kt`: ShutterEvent property tests

### Project Structure After Phase 1
```
android/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── docs/
│   └── (documentation files)
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/values/{strings,themes}.xml
        │   └── java/com/shutteranalyzer/
        │       ├── ShutterAnalyzerApp.kt
        │       ├── MainActivity.kt
        │       └── analysis/
        │           ├── model/
        │           │   ├── BrightnessStats.kt
        │           │   └── ShutterEvent.kt
        │           ├── BrightnessAnalyzer.kt
        │           ├── ThresholdCalculator.kt
        │           ├── EventDetector.kt
        │           └── ShutterSpeedCalculator.kt
        └── test/java/com/shutteranalyzer/analysis/
            ├── TestVectors.kt
            ├── ExtensionFunctionsTest.kt
            ├── ThresholdCalculatorTest.kt
            ├── EventDetectorTest.kt
            ├── ShutterSpeedCalculatorTest.kt
            └── ShutterEventTest.kt
```

### Key Algorithm Ports

#### Weighted Duration (ShutterEvent.kt)
```kotlin
val weightedDurationFrames: Double
    get() {
        val eventPeak = brightnessValues.median()  // Per-event median
        if (eventPeak <= baselineBrightness) return durationFrames.toDouble()

        val brightnessRange = eventPeak - baselineBrightness
        return brightnessValues.sumOf { brightness ->
            val weight = (brightness - baselineBrightness) / brightnessRange
            weight.coerceIn(0.0, 1.0)
        }
    }
```

#### Original Threshold (ThresholdCalculator.kt)
```kotlin
val brightnessRange = medianBrightness - baseline
var threshold = baseline + (brightnessRange * marginFactor)
if (threshold == baseline) {
    threshold = baseline + (maxBrightness - baseline) * 0.1
}
```

### Next Steps
- Phase 2: Database & Repository Layer ✅

---

## 2025-01-24 - Phase 2: Database & Repository Layer

### Changes Made

#### Room Entities
- `android/app/src/main/java/com/shutteranalyzer/data/local/database/entity/CameraEntity.kt`
  - Represents a camera profile (id, name, createdAt)

- `android/app/src/main/java/com/shutteranalyzer/data/local/database/entity/TestSessionEntity.kt`
  - Represents a test session with foreign key to Camera
  - Fields: cameraId, recordingFps, testedAt, avgDeviationPercent

- `android/app/src/main/java/com/shutteranalyzer/data/local/database/entity/ShutterEventEntity.kt`
  - Represents a single shutter event measurement with foreign key to TestSession
  - Fields: startFrame, endFrame, expectedSpeed, measuredSpeed, deviationPercent, brightnessValuesJson

#### Type Converters
- `android/app/src/main/java/com/shutteranalyzer/data/local/database/converter/Converters.kt`
  - Converts List<Double> to/from comma-separated string for Room storage

#### DAOs
- `android/app/src/main/java/com/shutteranalyzer/data/local/database/dao/CameraDao.kt`
  - getAllCameras(), getCameraById(), getTestCountForCamera(), insert(), update(), delete()

- `android/app/src/main/java/com/shutteranalyzer/data/local/database/dao/TestSessionDao.kt`
  - getSessionsForCamera(), getSessionById(), insert(), updateAvgDeviation(), delete()

- `android/app/src/main/java/com/shutteranalyzer/data/local/database/dao/ShutterEventDao.kt`
  - getEventsForSession(), getEventsForSessionOnce(), insertAll(), deleteForSession()

#### Database
- `android/app/src/main/java/com/shutteranalyzer/data/local/database/AppDatabase.kt`
  - Room database with all 3 entities and type converters
  - Database name: "shutter_analyzer.db"

#### Domain Models
- `android/app/src/main/java/com/shutteranalyzer/domain/model/Camera.kt`
  - Clean domain model with Instant for dates, testCount property

- `android/app/src/main/java/com/shutteranalyzer/domain/model/TestSession.kt`
  - Domain model with events list, uses analysis ShutterEvent

#### Repositories
- `android/app/src/main/java/com/shutteranalyzer/data/repository/CameraRepository.kt`
  - Interface + CameraRepositoryImpl
  - Maps entities to domain models
  - getAllCameras(), getCameraById(), saveCamera(), deleteCamera()

- `android/app/src/main/java/com/shutteranalyzer/data/repository/TestSessionRepository.kt`
  - Interface + TestSessionRepositoryImpl
  - Handles session and event persistence
  - getSessionsForCamera(), getSessionById(), saveSession(), saveEvents(), updateAvgDeviation()

#### Hilt DI Modules
- `android/app/src/main/java/com/shutteranalyzer/di/DatabaseModule.kt`
  - Provides AppDatabase singleton and all DAOs

- `android/app/src/main/java/com/shutteranalyzer/di/RepositoryModule.kt`
  - Binds repository implementations to interfaces

### Database Schema
```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│    cameras      │     │   test_sessions      │     │   shutter_events    │
├─────────────────┤     ├──────────────────────┤     ├─────────────────────┤
│ id (PK)         │──┐  │ id (PK)              │──┐  │ id (PK)             │
│ name            │  │  │ cameraId (FK) ───────│──┘  │ sessionId (FK) ─────│
│ createdAt       │  │  │ recordingFps         │     │ startFrame          │
└─────────────────┘  │  │ testedAt             │     │ endFrame            │
                     │  │ avgDeviationPercent  │     │ expectedSpeed       │
                     └──└──────────────────────┘     │ measuredSpeed       │
                                                     │ deviationPercent    │
                                                     │ brightnessValuesJson│
                                                     └─────────────────────┘
```

### Project Structure After Phase 2
```
android/app/src/main/java/com/shutteranalyzer/
├── ShutterAnalyzerApp.kt
├── MainActivity.kt
├── analysis/                    # Phase 1
│   ├── model/
│   ├── BrightnessAnalyzer.kt
│   ├── ThresholdCalculator.kt
│   ├── EventDetector.kt
│   └── ShutterSpeedCalculator.kt
├── data/                        # Phase 2 (NEW)
│   ├── local/database/
│   │   ├── entity/
│   │   │   ├── CameraEntity.kt
│   │   │   ├── TestSessionEntity.kt
│   │   │   └── ShutterEventEntity.kt
│   │   ├── dao/
│   │   │   ├── CameraDao.kt
│   │   │   ├── TestSessionDao.kt
│   │   │   └── ShutterEventDao.kt
│   │   ├── converter/
│   │   │   └── Converters.kt
│   │   └── AppDatabase.kt
│   └── repository/
│       ├── CameraRepository.kt
│       └── TestSessionRepository.kt
├── domain/                      # Phase 2 (NEW)
│   └── model/
│       ├── Camera.kt
│       └── TestSession.kt
└── di/                          # Phase 2 (NEW)
    ├── DatabaseModule.kt
    └── RepositoryModule.kt
```

### Next Steps
- Phase 3: Camera Integration ✅

---

## 2025-01-24 - Phase 3: Camera Integration

### Changes Made

#### Dependencies Added (`app/build.gradle.kts`)
- CameraX (camera-core, camera2, lifecycle, video, view) v1.3.1
- Room (runtime, ktx, compiler) v2.6.1 - was missing from Phase 2
- lifecycle-viewmodel-compose v2.7.0
- hilt-navigation-compose v1.1.0

#### Camera Layer Files
- `android/app/src/main/java/com/shutteranalyzer/data/camera/SlowMotionCapability.kt`
  - `SlowMotionCapability` data class for device capabilities
  - `SlowMotionCapabilityChecker` - detects max FPS, supported ranges, high-speed sizes
  - `getBestFpsForAccuracy()` - recommends FPS for target shutter speed
  - `getAccuracyDescription()` - human-readable accuracy info

- `android/app/src/main/java/com/shutteranalyzer/data/camera/ImageProxyExtensions.kt`
  - `ImageProxy.calculateBrightness()` - efficient brightness from Y plane
  - Samples every 4th pixel for performance (1/16 of total pixels)
  - `calculateCenterBrightness()` - center region only (ignores edges)

- `android/app/src/main/java/com/shutteranalyzer/data/camera/LiveEventDetector.kt`
  - State machine: Calibrating → WaitingForEvent → EventInProgress
  - `DetectorState` sealed class for state representation
  - `EventResult` sealed class: CalibrationComplete, EventDetected
  - Calibration: collects frames, calculates baseline (25th percentile) and threshold
  - Threshold: `baseline + (median - baseline) * 1.5`
  - Tracks brightness values during events for later analysis

- `android/app/src/main/java/com/shutteranalyzer/data/camera/FrameAnalyzer.kt`
  - Implements `ImageAnalysis.Analyzer` for CameraX
  - Calculates brightness per frame, feeds to LiveEventDetector
  - Callbacks: onEventDetected, onCalibrationComplete, onBrightnessUpdate, onCalibrationProgress
  - Always closes ImageProxy to prevent memory leaks

- `android/app/src/main/java/com/shutteranalyzer/data/camera/ShutterCameraManager.kt`
  - Main camera controller using CameraX
  - `initialize()` - gets ProcessCameraProvider
  - `bindPreview()` - binds Preview, ImageAnalysis, VideoCapture to lifecycle
  - `startRecording()` / `stopRecording()` - video recording to MediaStore
  - StateFlows: cameraState, isRecording, currentBrightness, detectedEvents, calibrationProgress
  - `EventMarker` data class for detected events with timestamps

#### Hilt Module
- `android/app/src/main/java/com/shutteranalyzer/di/CameraModule.kt`
  - Provides FrameAnalyzer, SlowMotionCapabilityChecker, ShutterCameraManager

#### Unit Tests
- `android/app/src/test/java/com/shutteranalyzer/data/camera/LiveEventDetectorTest.kt`
  - Tests state transitions, calibration, event detection, reset functionality

### Key Design Decisions

#### CameraX vs Camera2 for High-Speed Recording
CameraX's VideoCapture doesn't natively support >60fps recording. Decision:
- **Phase 3**: Use standard CameraX (up to 60fps)
- **Future**: Add Camera2 interop for true 120/240fps if needed
- **Workaround**: Users can import slow-mo videos from device camera app

#### Efficient Brightness Calculation
Instead of converting full ImageProxy to OpenCV Mat:
- Use Y plane directly (luminance channel in YUV format)
- Sample every 4th pixel (16x faster than full scan)
- Still accurate enough for threshold detection

### Project Structure After Phase 3
```
android/app/src/main/java/com/shutteranalyzer/
├── ShutterAnalyzerApp.kt
├── MainActivity.kt
├── analysis/                    # Phase 1
├── data/
│   ├── local/database/          # Phase 2
│   ├── repository/              # Phase 2
│   └── camera/                  # Phase 3 (NEW)
│       ├── SlowMotionCapability.kt
│       ├── ImageProxyExtensions.kt
│       ├── LiveEventDetector.kt
│       ├── FrameAnalyzer.kt
│       └── ShutterCameraManager.kt
├── domain/model/                # Phase 2
└── di/
    ├── DatabaseModule.kt        # Phase 2
    ├── RepositoryModule.kt      # Phase 2
    └── CameraModule.kt          # Phase 3 (NEW)
```

### Next Steps
- Phase 4: UI - Core Screens ✅

---

## 2025-01-25 - Phase 4: UI Layer

### Changes Made

#### Dependencies Added (`app/build.gradle.kts`)
- `androidx.lifecycle:lifecycle-runtime-compose:2.7.0` - for `collectAsStateWithLifecycle()`
- `androidx.navigation:navigation-compose:2.7.7` - Jetpack Navigation for Compose

#### Theme Files (`ui/theme/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/theme/Color.kt`
  - Accuracy indicator colors: Green (0-5%), Yellow (5-10%), Orange (10-15%), Red (>15%)
  - Context frame color: Blue
  - Light/Dark theme colors
  - Helper functions: `getAccuracyColor()`, `getAccuracyLabel()`

- `android/app/src/main/java/com/shutteranalyzer/ui/theme/Type.kt`
  - Standard Material3 typography scales

- `android/app/src/main/java/com/shutteranalyzer/ui/theme/Theme.kt`
  - `ShutterAnalyzerTheme` composable
  - Supports dynamic color (Android 12+) and light/dark modes

#### Navigation (`ui/navigation/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/navigation/NavGraph.kt`
  - `Screen` sealed class with routes: Home, RecordingSetup, Recording, EventReview, Results
  - `NavGraph` composable with NavHost
  - Handles session ID passing between screens

#### Reusable Components (`ui/components/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/components/AccuracyIndicator.kt`
  - `AccuracyIndicator` - colored badge showing deviation percentage
  - `AccuracyDot` - simple colored dot indicator
  - `getAccuracyRowColor()` - for table row backgrounds

- `android/app/src/main/java/com/shutteranalyzer/ui/components/CameraCard.kt`
  - Card displaying camera name, last test date, average deviation
  - Used in HomeScreen camera list

- `android/app/src/main/java/com/shutteranalyzer/ui/components/SpeedChip.kt`
  - `SpeedChip` - selectable chip with states: unselected, selected, detected
  - `SpeedDisplayChip` - non-interactive display during recording

- `android/app/src/main/java/com/shutteranalyzer/ui/components/BrightnessIndicator.kt`
  - `BrightnessIndicator` - vertical bar with threshold marker
  - `HorizontalBrightnessBar` - alternative horizontal display

#### Home Screen (`ui/screens/home/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/screens/home/HomeViewModel.kt`
  - `cameras` StateFlow from repository
  - `isEmpty` StateFlow for empty state detection

- `android/app/src/main/java/com/shutteranalyzer/ui/screens/home/HomeScreen.kt`
  - Large top app bar with "SHUTTER ANALYZER" title
  - Camera list (LazyColumn of CameraCards)
  - Empty state with icon and instructions
  - Bottom buttons: "+ NEW TEST" and "IMPORT"

#### Recording Setup Screen (`ui/screens/setup/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/screens/setup/RecordingSetupViewModel.kt`
  - Camera name input handling
  - Speed set selection (Standard vs Custom)
  - Device FPS capability detection
  - Session creation (creates Camera if name provided)
  - `STANDARD_SPEEDS` constant for default speed list

- `android/app/src/main/java/com/shutteranalyzer/ui/screens/setup/RecordingSetupScreen.kt`
  - Camera name text field
  - Radio selection for Standard/Custom speed sets
  - Speed picker bottom sheet for custom selection
  - Collapsible setup reminder card
  - Camera permission request on "START RECORDING"

#### Recording Screen (`ui/screens/recording/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/screens/recording/RecordingViewModel.kt`
  - `RecordingState` sealed class: Initializing, Calibrating, WaitingForShutter, EventDetected, Complete, Error
  - Camera initialization and recording control
  - Event observation with auto-advance
  - Skip/Redo/Done actions

- `android/app/src/main/java/com/shutteranalyzer/ui/screens/recording/RecordingScreen.kt`
  - CameraX PreviewView (full screen)
  - State-based overlays:
    - Calibrating: progress bar, "Hold camera steady"
    - Waiting: speed prompt with Redo/Skip/Done buttons
    - Detected: green checkmark animation with speed
    - Error: retry/cancel dialog
  - Header with FPS and progress indicator

#### Event Review Screen (`ui/screens/review/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/screens/review/EventReviewViewModel.kt`
  - `FrameInfo` and `ReviewEvent` data classes
  - Frame inclusion toggle support
  - Event navigation (next/previous)

- `android/app/src/main/java/com/shutteranalyzer/ui/screens/review/EventReviewScreen.kt`
  - Event header with speed label
  - Frame thumbnail grid (FlowRow)
  - Color-coded frames: green (full), orange (partial), blue (context)
  - Tap to toggle frame inclusion
  - Navigation controls with Prev/Next
  - "CONFIRM & CALCULATE" button

#### Results Screen (`ui/screens/results/`)
- `android/app/src/main/java/com/shutteranalyzer/ui/screens/results/ResultsViewModel.kt`
  - `ShutterResult` data class for display
  - Results calculation from events
  - Average deviation calculation
  - Formatting helpers for ms and deviation

- `android/app/src/main/java/com/shutteranalyzer/ui/screens/results/ResultsScreen.kt`
  - Camera name and date header
  - Average deviation card with AccuracyIndicator
  - Results table with color-coded rows
  - Columns: Speed, Expected, Actual, Error
  - SAVE and TEST AGAIN buttons

#### MainActivity Update
- `android/app/src/main/java/com/shutteranalyzer/MainActivity.kt`
  - Updated to use `ShutterAnalyzerTheme`
  - Navigation setup with `rememberNavController()`
  - `NavGraph` as root composable

### Project Structure After Phase 4
```
android/app/src/main/java/com/shutteranalyzer/
├── ShutterAnalyzerApp.kt
├── MainActivity.kt                  # Updated with navigation
├── analysis/                        # Phase 1
├── data/
│   ├── local/database/              # Phase 2
│   ├── repository/                  # Phase 2
│   └── camera/                      # Phase 3
├── domain/model/                    # Phase 2
├── di/                              # Phase 2-3
└── ui/                              # Phase 4 (NEW)
    ├── navigation/
    │   └── NavGraph.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    ├── components/
    │   ├── AccuracyIndicator.kt
    │   ├── CameraCard.kt
    │   ├── SpeedChip.kt
    │   └── BrightnessIndicator.kt
    └── screens/
        ├── home/
        │   ├── HomeScreen.kt
        │   └── HomeViewModel.kt
        ├── setup/
        │   ├── RecordingSetupScreen.kt
        │   └── RecordingSetupViewModel.kt
        ├── recording/
        │   ├── RecordingScreen.kt
        │   └── RecordingViewModel.kt
        ├── review/
        │   ├── EventReviewScreen.kt
        │   └── EventReviewViewModel.kt
        └── results/
            ├── ResultsScreen.kt
            └── ResultsViewModel.kt
```

### Key Implementation Patterns

#### State Management
```kotlin
// ViewModel with StateFlow
val cameras: StateFlow<List<Camera>> = cameraRepository.getAllCameras()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// Composable collection
val cameras by viewModel.cameras.collectAsStateWithLifecycle()
```

#### Navigation with Arguments
```kotlin
composable(
    route = "recording/{sessionId}",
    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
) { backStackEntry ->
    val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
    RecordingScreen(sessionId = sessionId, ...)
}
```

#### Camera Permission
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) { /* proceed */ }
}
permissionLauncher.launch(Manifest.permission.CAMERA)
```

### Next Steps
- ~~Phase 5: Analysis Pipeline Integration~~ Integration gaps fixed (see below)

---

## 2025-01-25 - Phase 4.5: Analysis Pipeline Integration Fix

### Problem Identified
After Phase 4 UI implementation, the recording → analysis → results flow was not properly wired up:
- Expected speeds selected by user were not persisted
- EventMarker (timestamp-based) not converted to ShutterEvent (frame-based)
- Events not saved to database after recording stopped
- ResultsScreen was guessing expected speeds instead of using stored values

### Changes Made

#### Database Schema Update (v1 → v2)
- `data/local/database/entity/TestSessionEntity.kt`
  - Added `expectedSpeedsJson: String` - comma-separated expected speeds (e.g., "1/500,1/250,1/125")
  - Added `videoUri: String?` - URI of recorded video for later playback

- `data/local/database/AppDatabase.kt`
  - Incremented version from 1 to 2
  - Added `MIGRATION_1_2` to add new columns via ALTER TABLE

- `data/local/database/dao/TestSessionDao.kt`
  - Added `updateVideoUri()` method
  - Added `updateExpectedSpeeds()` method

- `di/DatabaseModule.kt`
  - Added `.addMigrations(AppDatabase.MIGRATION_1_2)` to Room builder

#### Domain Model Update
- `domain/model/TestSession.kt`
  - Added `expectedSpeeds: List<String>` field
  - Added `videoUri: String?` field

#### New EventConverter
- `data/camera/EventConverter.kt` (NEW FILE)
  - `toShutterEvent()` - converts single EventMarker to ShutterEvent
  - `toShutterEvents()` - batch conversion
  - `timestampToFrame()` - formula: `(timestamp - startTimestamp) / 1e9 * fps`

#### Camera Manager Updates
- `data/camera/ShutterCameraManager.kt`
  - Added `recordingStartTimestamp` private var
  - Set timestamp in `VideoRecordEvent.Start` handler
  - Added `getRecordingStartTimestamp()` method
  - Added `getBaselineBrightness()` method (from frameAnalyzer)

#### Repository Updates
- `data/repository/TestSessionRepository.kt`
  - Added interface methods: `updateVideoUri()`, `updateExpectedSpeeds()`, `saveEventsWithExpectedSpeeds()`
  - Added `saveEventsWithExpectedSpeeds()` implementation:
    - Saves events with expected speeds and calculated deviations
    - Calculates and updates average deviation on session
  - Added `calculateDeviation()` and `parseShutterSpeed()` helper methods
  - Updated `toDomainModel()` to parse expectedSpeedsJson
  - Updated `toEntity()` to serialize expectedSpeeds

#### ViewModel Updates
- `ui/screens/setup/RecordingSetupViewModel.kt`
  - Updated `createSession()` to include `expectedSpeeds` from `selectedSpeeds.value`

- `ui/screens/recording/RecordingViewModel.kt`
  - Added imports for EventConverter and ShutterSpeedCalculator
  - Added `loadSessionData()` to load expected speeds from DB on init
  - Added `saveDetectedEvents()` method:
    - Converts EventMarkers to ShutterEvents via EventConverter
    - Calculates measured speeds via ShutterSpeedCalculator
    - Saves events with expected speeds via repository
    - Saves video URI if available
  - Updated `stopRecording()` to call `saveDetectedEvents()`

- `ui/screens/results/ResultsViewModel.kt`
  - Updated `calculateResults()` to use `session.expectedSpeeds` from database
  - Renamed `estimateExpectedSpeed()` to `fallbackExpectedSpeed()` (only used for legacy sessions)

#### Analysis Module Update
- `analysis/ShutterSpeedCalculator.kt`
  - Added companion object with static `calculateShutterSpeed(durationFrames, fps)` helper

### Data Flow After Fix

```
Setup Screen
  └─ User selects speeds ["1/500", "1/250", "1/125"]
  └─ createSession() saves TestSession with expectedSpeeds
        │
        ▼ sessionId
Recording Screen
  └─ loadSessionData() retrieves expectedSpeeds from DB
  └─ LiveEventDetector produces EventMarkers (timestamps)
  └─ stopRecording() → EventConverter → ShutterEvents (frame indices)
  └─ saveEventsWithExpectedSpeeds() stores everything
        │
        ▼ sessionId
Results Screen
  └─ getSessionById() loads session with expectedSpeeds
  └─ calculateResults() uses stored speeds (not guesses!)
  └─ Accurate deviation displayed
```

### Migration Strategy
- Room migration v1→v2 adds columns with defaults
- Existing sessions (if any) get empty expectedSpeedsJson
- ResultsViewModel falls back to standard speeds for legacy sessions

### Files Modified Summary
| File | Change Type |
|------|-------------|
| `data/local/database/entity/TestSessionEntity.kt` | Add fields |
| `data/local/database/AppDatabase.kt` | Add migration |
| `data/local/database/dao/TestSessionDao.kt` | Add methods |
| `di/DatabaseModule.kt` | Add migration |
| `domain/model/TestSession.kt` | Add fields |
| `data/camera/EventConverter.kt` | **NEW FILE** |
| `data/camera/ShutterCameraManager.kt` | Add timestamp tracking |
| `data/repository/TestSessionRepository.kt` | Add methods, update mappings |
| `analysis/ShutterSpeedCalculator.kt` | Add static helper |
| `ui/screens/setup/RecordingSetupViewModel.kt` | Pass expected speeds |
| `ui/screens/recording/RecordingViewModel.kt` | Load speeds, save events |
| `ui/screens/results/ResultsViewModel.kt` | Use stored expected speeds |

### Next Steps
- ~~Phase 5: Secondary Screens~~ ✅ Complete (see below)
- Phase 6: Polish & Testing
- Phase 7: Publishing

---

## 2025-01-25 - Phase 5: Secondary Screens Implementation

### Overview
Implemented all 4 secondary screens: Settings, Camera Detail, Onboarding, and Import Video.

### Changes Made

#### DataStore Infrastructure
- `data/local/datastore/SettingsDataStore.kt` (NEW)
  - `SpeedSet` enum: STANDARD, FAST, SLOW, CUSTOM
  - `Sensitivity` enum: LOW, NORMAL, HIGH with margin factors
  - `AppSettings` data class for persisted settings
  - Preference keys: `default_speed_set`, `detection_sensitivity`, `has_seen_onboarding`

- `data/repository/SettingsRepository.kt` (NEW)
  - Interface + implementation for settings access
  - Methods: `setDefaultSpeedSet()`, `setDetectionSensitivity()`, `setHasSeenOnboarding()`

- `di/DataStoreModule.kt` (NEW)
  - Hilt module providing SettingsDataStore singleton
  - Binds SettingsRepositoryImpl to SettingsRepository

#### Settings Screen
- `ui/screens/settings/SettingsViewModel.kt` (NEW)
  - Settings state from repository
  - Actions: setDefaultSpeedSet, setDetectionSensitivity, resetOnboarding

- `ui/screens/settings/SettingsScreen.kt` (NEW)
  - Section: Recording - Default speed set radio buttons
  - Section: Detection - Sensitivity slider (Low/Normal/High)
  - Section: Help - View Tutorial, How It Works
  - Section: About - Version, Copyright

#### Camera Detail Screen
- `ui/screens/camera/CameraDetailViewModel.kt` (NEW)
  - Loads camera and sessions by ID
  - Edit name dialog state management
  - Delete confirmation with cascade

- `ui/screens/camera/CameraDetailScreen.kt` (NEW)
  - Header with editable camera name
  - Session history list with AccuracyIndicator badges
  - Tap session → Results screen
  - Test Again and Delete buttons

#### Onboarding Screen
- `ui/screens/onboarding/OnboardingScreen.kt` (NEW)
  - HorizontalPager with 5 pages:
    1. Welcome - app description
    2. Equipment Setup - tripod positioning
    3. Lighting - requirements
    4. Framing - how to position phone
    5. Ready to Test - start instructions
  - Skip button (always visible)
  - Page indicator dots
  - Next/Get Started button

#### Import Video Screen
- `data/video/VideoAnalyzer.kt` (NEW)
  - `getVideoInfo()` - extracts metadata via MediaMetadataRetriever
  - `analyzeVideo()` - extracts frames, calculates brightness, detects events
  - `calculateBitmapBrightness()` - samples pixels for luminance
  - Reuses existing EventDetector and ThresholdCalculator

- `ui/screens/import/ImportViewModel.kt` (NEW)
  - `ImportState` sealed class: SelectFile, Loading, VideoSelected, Analyzing, AssignSpeeds, Complete, Error
  - Video selection and metadata extraction
  - Analysis with progress callback
  - Speed assignment per event
  - Session creation with events

- `ui/screens/import/ImportScreen.kt` (NEW)
  - Step 1: File picker + video info display
  - Step 2: Event list with speed assignment dropdowns
  - Progress indicator during analysis
  - Error state with retry

#### Navigation Updates
- `ui/navigation/NavGraph.kt`
  - Added 4 new routes: Settings, Onboarding, CameraDetail, Import
  - Wired HomeScreen callbacks to new screens

- `ui/screens/home/HomeScreen.kt`
  - Added `onImportClick` callback parameter
  - Wired all navigation callbacks

- `MainActivity.kt`
  - Inject SettingsRepository
  - Check hasSeenOnboarding flag on launch
  - Conditionally start at Onboarding or Home
  - Mark onboarding complete when reaching Home

#### Dependencies Added
- `app/build.gradle.kts`
  - `androidx.datastore:datastore-preferences:1.0.0`

### New Files Summary

```
data/local/datastore/
└── SettingsDataStore.kt

data/repository/
└── SettingsRepository.kt

data/video/
└── VideoAnalyzer.kt

di/
└── DataStoreModule.kt

ui/screens/
├── settings/
│   ├── SettingsScreen.kt
│   └── SettingsViewModel.kt
├── camera/
│   ├── CameraDetailScreen.kt
│   └── CameraDetailViewModel.kt
├── onboarding/
│   └── OnboardingScreen.kt
└── import/
    ├── ImportScreen.kt
    └── ImportViewModel.kt
```

### Project Structure After Phase 5

```
android/app/src/main/java/com/shutteranalyzer/
├── ShutterAnalyzerApp.kt
├── MainActivity.kt                  # Updated - onboarding check
├── analysis/                        # Phase 1
├── data/
│   ├── local/
│   │   ├── database/                # Phase 2
│   │   └── datastore/               # Phase 5 (NEW)
│   │       └── SettingsDataStore.kt
│   ├── repository/                  # Phase 2, 5
│   │   ├── CameraRepository.kt
│   │   ├── TestSessionRepository.kt
│   │   └── SettingsRepository.kt    # NEW
│   ├── camera/                      # Phase 3, 4.5
│   └── video/                       # Phase 5 (NEW)
│       └── VideoAnalyzer.kt
├── domain/model/                    # Phase 2
├── di/
│   ├── DatabaseModule.kt
│   ├── DataStoreModule.kt           # Phase 5 (NEW)
│   ├── RepositoryModule.kt
│   └── CameraModule.kt
└── ui/
    ├── navigation/NavGraph.kt       # Updated
    ├── theme/
    ├── components/
    └── screens/
        ├── home/
        ├── setup/
        ├── recording/
        ├── review/
        ├── results/
        ├── settings/                # Phase 5 (NEW)
        ├── camera/                  # Phase 5 (NEW)
        ├── onboarding/              # Phase 5 (NEW)
        └── import/                  # Phase 5 (NEW)
```

### Next Steps
- Phase 6: Visualization & Charts ✅ Complete
- Phase 7: Polish & Testing ✅ Complete
- Phase 8: Publishing

---

**Continued in [IMPLEMENTATION_LOG_2.md](IMPLEMENTATION_LOG_2.md)**

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

## 2025-01-25 - Phase 7 Part 2: Comprehensive Bug Fixes

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

### Testing Checklist
- [ ] Camera initialization: Start recording → camera preview shows → calibration starts
- [ ] First speed shown: After calibration, first speed (e.g., 1/1000) displays, not second
- [ ] Import video: Import completes without crash, session created
- [ ] Results loading: Results screen shows loading state, then data
- [ ] Empty session: Session with 0 events shows helpful message
- [ ] Permission denied: Deny camera → shows dialog with Settings button

### Next Steps
- Phase 7 Part 3: Camera Control Enhancements ✅

---

## 2025-01-25 - Phase 7 Part 3: Camera Control Enhancements

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

### Files Modified

| File | Changes |
|------|---------|
| `ShutterCameraManager.kt` | Added zoom state (`zoomRatio`, `minZoomRatio`, `maxZoomRatio`), focus state (`isAutoFocus`, `focusDistance`), `setZoom()`, `enableAutoFocus()`, `setManualFocus()` |
| `RecordingViewModel.kt` | Added `RecordingState.SettingUp`, exposed zoom/focus controls, added `beginDetection()` |
| `RecordingScreen.kt` | Added vertical sliders in `SettingUpOverlay` and `WaitingForShutterOverlay`, imported `ZoomIn`, `IconButton`, `rotate` |

### Next Steps
- Event Review Thumbnails ✅

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

#### Updated: `FrameInfo` data class
- Added `thumbnail: Bitmap? = null` field

### UI Layout

```
┌──────────────────────────────────────────────────────┐
│ ← REVIEW EVENTS                          1 of 5     │
├──────────────────────────────────────────────────────┤
│                                                      │
│ EVENT 1: 1/500                                       │
│ Tap frames to adjust boundary                        │
│                                                      │
│ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│ │ [img]  │ │ [img]  │ │ [img]  │ │ [img]  │ ...     │
│ │        │ │        │ │        │ │        │         │
│ ├────────┤ ├────────┤ ├────────┤ ├────────┤         │
│ │#98 ctx │ │#99 ctx │ │#100 245│ │#101 252│         │
│ └────────┘ └────────┘ └────────┘ └────────┘         │
│   (blue)    (blue)    (green)    (green)            │
│                                                      │
│ Legend: [■] Full  [■] Partial  [■] Context          │
│                                                      │
├──────────────────────────────────────────────────────┤
│        [◀ Prev]    1 / 5    [Next ▶]                │
│        [ CONFIRM & CALCULATE ]                       │
└──────────────────────────────────────────────────────┘
```

### Files Modified Summary

| File | Change Type |
|------|-------------|
| `data/video/FrameExtractor.kt` | **NEW** - Frame extraction utility |
| `ui/screens/review/EventReviewViewModel.kt` | Add thumbnail loading |
| `ui/screens/review/EventReviewScreen.kt` | Display actual frame images |

### Next Steps
- Two-Phase Calibration ✅

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

### Files Modified

| File | Changes |
|------|---------|
| `data/camera/LiveEventDetector.kt` | New state machine: CalibratingBaseline → WaitingForCalibrationShutter → CapturingCalibrationEvent → WaitingForEvent → EventInProgress. New properties: preliminaryThreshold, calibrationPeak, maxSeenDuringBaseline, baselineStdDev. New EventResult.BaselineCalibrationComplete. |
| `data/camera/FrameAnalyzer.kt` | Added onBaselineCalibrationComplete callback, isWaitingForCalibrationShutter property, updated calibration progress handling |
| `data/camera/ShutterCameraManager.kt` | Added isWaitingForCalibrationShutter StateFlow, updated callbacks for two-phase flow |
| `ui/screens/recording/RecordingViewModel.kt` | Added RecordingState.CalibratingBaseline and WaitingForCalibrationShutter, updated observeCalibration() for two-phase transitions |
| `ui/screens/recording/RecordingScreen.kt` | Added WaitingForCalibrationShutterOverlay with "Fire Shutter Once" prompt, updated CalibratingOverlay to accept message parameter |
| `android/docs/THEORY.md` | Added "Two-Phase Calibration" section explaining the approach |
| `android/docs/APP_SPEC.md` | Updated Recording Screen to document calibration flow |

### New State Machine

```
OLD:  Calibrating → WaitingForEvent → EventInProgress → (loop)

NEW:  CalibratingBaseline → WaitingForCalibrationShutter → CapturingCalibrationEvent
      → WaitingForEvent → EventInProgress → (loop)
```

### UI Flow

1. User taps "Begin Detecting"
2. **CalibratingBaseline** state: "Establishing baseline..." with progress bar (0-50%)
3. **WaitingForCalibrationShutter** state: "Fire Shutter Once" prompt with camera icon and note "(This event will not be recorded)"
4. User fires calibration shutter → system captures peak
5. **WaitingForShutter** state: Normal speed prompts begin (e.g., "1/1000")

### Key Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| `CALIBRATION_FRAME_COUNT` | 60 | Frames for baseline |
| `MIN_BRIGHTNESS_INCREASE` | 50 | Minimum brightness above baseline |
| `NOISE_MULTIPLIER` | 2.0 | maxNoise × 2 = preliminary threshold floor |
| `STDDEV_MULTIPLIER` | 5.0 | baseline + 5σ = statistical threshold |
| `DEFAULT_THRESHOLD_FACTOR` | 0.8 | Final threshold = 80% of brightness range |

### Benefits
- Threshold based on actual measured brightness range
- Works reliably across different lighting conditions
- Robust preliminary threshold prevents false triggers from shadows/movement
- Calibration event discarded - doesn't pollute measurements

### Next Steps
- Frame Index Alignment Fix ✅

---

## 2025-01-25 - Frame Index Alignment Fix

### Overview
Fixed a bug causing frame index misalignment between live detection timestamps and video frame positions. The issue caused event review thumbnails to show wrong frames (offset by the duration of the SettingUp phase).

### Root Cause
When `beginDetection()` was called, it triggered `resetCalibration()` → `frameAnalyzer.reset()` which cleared `firstFrameTimestamp` to 0L. This caused the frame index reference point to shift from the recording start time (T0) to the calibration start time (T1).

**Call chain:**
```
beginDetection()
  → cameraManager.resetCalibration() [ShutterCameraManager.kt:425]
    → frameAnalyzer.reset() [ShutterCameraManager.kt:426]
      → firstFrameTimestamp = 0L  // BUG!
```

**Result:** Frame indices were offset by the duration of the SettingUp phase (zoom/focus adjustment time).

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

// resetEvents() - clear events, keep calibration and timestamp
fun resetEvents() {
    liveEventDetector.resetEvents()
    // DO NOT reset firstFrameTimestamp
}
```

#### 2. `ShutterCameraManager.kt`
- `startRecording()` now calls `frameAnalyzer.resetForNewRecording()` instead of `resetEvents()`
- `resetCalibration()` continues calling `frameAnalyzer.reset()` (no timestamp reset)

#### 3. `RecordingViewModel.kt`
- Removed video re-analysis step (`analyzeRecordedVideo()`)
- Events are now saved directly from live detection via `saveLiveDetectedEvents()`
- Removed unused `videoAnalyzer` dependency

### Files Modified

| File | Change |
|------|--------|
| `FrameAnalyzer.kt` | Don't reset timestamp in `reset()` and `resetEvents()`, add `resetForNewRecording()` |
| `ShutterCameraManager.kt` | Call `resetForNewRecording()` in `startRecording()` |
| `RecordingViewModel.kt` | Remove video re-analysis, use live detection directly, remove unused imports |

### Verification
1. Record a session with SettingUp phase (adjust zoom/focus)
2. Go to Review screen
3. Thumbnails should now show actual bright frames at the correct positions
4. No offset - frame indices correctly reference recording start time

### Next Steps
- Bug Fixes ✅

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

**Files:**
| File | Change |
|------|--------|
| `ui/screens/settings/TheoryScreen.kt` | NEW - Theory content screen |
| `ui/screens/settings/SettingsScreen.kt` | Add onViewTheory callback |
| `ui/navigation/NavGraph.kt` | Add Theory route and import |

### Bug 2: 0-Event Session Crash
**Problem:** Tapping on a session with 0 events crashed the app
**Fix:**
- Made session cards with 0 events non-clickable
- Added "No events recorded" label for empty sessions
- Added error message display in ResultsScreen

**Files:**
| File | Change |
|------|--------|
| `ui/screens/camera/CameraDetailScreen.kt` | Disable click for 0-event sessions, show "Empty" badge |
| `ui/screens/results/ResultsScreen.kt` | Add error message state display |

### Bug 3: Sliders Persisting After Setup
**Problem:** Zoom/focus sliders appeared during WaitingForShutter state
**Fix:** Removed slider UI from `WaitingForShutterOverlay`, kept only in `SettingUpOverlay`

**Files:**
| File | Change |
|------|--------|
| `ui/screens/recording/RecordingScreen.kt` | Remove sliders from WaitingForShutterOverlay |

### Bug 4: Frame Clickability with State Cycling
**Problem:** Only non-context frames were clickable; users wanted all frames toggleable
**Fix:**
- Added `FrameState` enum: FULL, PARTIAL, EXCLUDED
- Made all frames (including context) clickable
- Tap cycles: Full → Partial → Excluded → (back to auto/Full)
- Updated visual display to show manual state overrides

**Files:**
| File | Change |
|------|--------|
| `ui/screens/review/EventReviewViewModel.kt` | Add FrameState enum, replace toggleFrameInclusion with cycleFrameState |
| `ui/screens/review/EventReviewScreen.kt` | Make all frames clickable, update visuals for state cycling |

### Bug 5: "Confirm & Calculate" Crash
**Problem:** Button could be clicked with no events or no included frames
**Fix:**
- Added `hasIncludedFrames` check to BottomControls
- Button disabled when no events or no included frames
- Button text changes to explain why it's disabled

**Files:**
| File | Change |
|------|--------|
| `ui/screens/review/EventReviewScreen.kt` | Add hasIncludedFrames validation, disable button when appropriate |

### Summary
All 5 bugs fixed and tested. App is ready for further testing.

### Next Steps
- Expandable Frame Boundaries Feature ✅

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

### Files Modified

#### `ui/screens/review/EventReviewViewModel.kt`

**New Constants:**
```kotlin
private const val DEFAULT_CONTEXT_FRAMES = 1  // Was 2
private const val FRAMES_TO_ADD_ON_EXPAND = 3
```

**New State:**
```kotlin
private val extraFramesBefore = mutableMapOf<Int, Int>()  // eventIndex -> count
private val extraFramesAfter = mutableMapOf<Int, Int>()   // eventIndex -> count
```

**New Methods:**
| Method | Purpose |
|--------|---------|
| `getContextFramesBefore(eventIndex)` | Returns DEFAULT + extra frames before |
| `getContextFramesAfter(eventIndex)` | Returns DEFAULT + extra frames after |
| `addFramesBefore(eventIndex)` | Adds 3 frames before, reloads thumbnails |
| `addFramesAfter(eventIndex)` | Adds 3 frames after, reloads thumbnails |

**Updated Methods:**
- `createReviewEvent()` - Uses dynamic context counts
- `loadThumbnailsForEvent()` - Extracts frames based on dynamic counts
- `prefetchEvent()` - Uses dynamic context counts
- `cycleFrameState()` - Handles dynamic context frame indices

#### `ui/screens/review/EventReviewScreen.kt`

**New Composable:**
```kotlin
@Composable
private fun ExpandFramesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```
- Card with [+] icon and "Examine more" text
- Matches frame thumbnail sizing (80dp width, 9:16 aspect ratio)
- Uses surfaceVariant background with 50% alpha

**Modified EventReviewContent:**
- Added `onAddFramesBefore: () -> Unit` parameter
- Added `onAddFramesAfter: () -> Unit` parameter
- FlowRow now includes ExpandFramesButton on both sides

**Modified EventReviewScreen:**
- Wired callbacks to ViewModel methods

### Visual Layout
```
┌───────────────────────────────────────────────────────────┐
│ [+]  │ [ctx] │ [EVT] [EVT] [EVT] [EVT] │ [ctx] │  [+]    │
│ more │  14   │  187   245   242   95   │  15   │  more   │
│      │ gray  │ green green green orange│ gray  │         │
└───────────────────────────────────────────────────────────┘
```

### Edge Cases Handled
1. **Frame 0 boundary**: Checks `startFrame - totalContextBefore >= 0` before adding
2. **Video end**: Frame extraction fails gracefully for frames past video end
3. **State persistence**: Extra frames are session-only (not persisted to DB)
4. **Memory**: Uses existing LRU cache - triggers reload when expanding

### Files Summary

| File | Change Type |
|------|-------------|
| `EventReviewViewModel.kt` | MODIFY - Add state tracking, helper methods, update frame extraction |
| `EventReviewScreen.kt` | MODIFY - Add ExpandFramesButton composable, wire to ViewModel |

### Next Steps
- Phase 8: Publishing

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

### Documentation
- Created `VIDEO_DECODER_DESIGN.md` with full technical details

### Next Steps
- Bug Fix Round 2 ✅

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

**Files:**
| File | Change |
|------|--------|
| `data/camera/ShutterCameraManager.kt` | Add `lockExposure()` and `unlockExposure()` methods |
| `ui/screens/recording/RecordingViewModel.kt` | Call exposure lock/unlock |

### Bug 2: Closed Frames Shouldn't Be Greyed Out
**Problem:** Excluded frames had `Color.Black.copy(alpha = 0.5f)` overlay obscuring the thumbnail image.

**Fix:**
- Removed dark background overlay from excluded frames
- Changed X mark color from white to `MaterialTheme.colorScheme.error` (red) for visibility
- Thumbnail now remains fully visible with only X mark overlay

**Files:**
| File | Change |
|------|--------|
| `ui/screens/review/EventReviewScreen.kt` | Remove `.background()` from excluded overlay |

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
- Removed unused `ContextBlue` import

**Files:**
| File | Change |
|------|--------|
| `ui/screens/review/EventReviewScreen.kt` | Unify context/closed visuals, simplify Legend |

### Bug 4: Events Not Being Saved After Detection
**Problem:** `saveLiveDetectedEvents()` had no error handling - exceptions would silently fail and events could be lost.

**Fix:**
- Added comprehensive try-catch around all save operations
- Added logging for success and failure cases
- Database operations continue even if one fails (e.g., video URI saved even if events fail)
- Errors logged with stack traces for debugging

**Files:**
| File | Change |
|------|--------|
| `ui/screens/recording/RecordingViewModel.kt` | Add try-catch with logging to `saveLiveDetectedEvents()` |

### Bug 5: Calculate Crashes (ResultsViewModel NPE)
**Problem:** Navigating to Results screen crashed with `NullPointerException` at `ResultsViewModel.kt:116`

**Root Cause:** Property initialization order bug. `_errorMessage` was declared *after* the `init` block:
```kotlin
init {
    loadSessionAndCalculate()  // Calls _errorMessage.value = null
}
// _errorMessage not yet initialized here!
private val _errorMessage = MutableStateFlow<String?>(null)
```

In Kotlin, properties are initialized in declaration order. When `init` ran, `_errorMessage` was still null.

**Fix:** Moved `_errorMessage` declaration before the `init` block.

**Files:**
| File | Change |
|------|--------|
| `ui/screens/results/ResultsViewModel.kt` | Move `_errorMessage` before `init` block |

### Summary
- Exposure lock ensures stable brightness readings during detection
- Cleaner event review UI with visible thumbnails and simplified legend
- Robust event persistence with error handling
- Fixed Results screen crash caused by property initialization order

### Documentation Updated
- `THEORY.md` - Added "Auto-Exposure Lock" section explaining the AE lock feature

### Next Steps
- Import Flow & Results Display Improvements ✅

---

## 2025-01-25 - Import Flow & Results Display Improvements

### Overview
Enhanced import flow UX and fixed results display format.

### Import Flow → Event Review
**Problem:** Import flow went directly to Results, skipping frame review.

**Fix:** Changed navigation to go to EventReview screen after import so users can verify frames.

**Files:**
| File | Change |
|------|--------|
| `ui/navigation/NavGraph.kt` | Change `onComplete` to navigate to EventReview instead of Results |

### Remove Fake Events in Import
**Problem:** Users couldn't remove false-positive shutter events detected during import.

**Fix:** Added delete button (X) on each event card in the speed assignment step.

**Files:**
| File | Change |
|------|--------|
| `ui/screens/videoimport/ImportScreen.kt` | Add Close icon button to EventSpeedCard |
| `ui/screens/videoimport/ImportViewModel.kt` | Add `removeEvent()` method to delete events and reindex speeds |

### Results Display: Speed Format
**Problem:** Actual/measured speeds displayed in milliseconds (e.g., "8.0ms") instead of shutter speed format.

**Fix:** Added `formatAsSpeed()` method that converts ms to "1/x" format where x is rounded to nearest int.

**Files:**
| File | Change |
|------|--------|
| `ui/screens/results/ResultsViewModel.kt` | Add `formatAsSpeed()` method |
| `ui/screens/results/ResultsScreen.kt` | Use `formatAsSpeed()` for Actual column |

### Summary
- Import flow now allows frame review before showing results
- Users can delete false-positive events during import
- Results show speeds in familiar "1/125" format

### Next Steps
- Event Preview Feature ✅

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

**Data Access:**
- Uses shared ImportViewModel passed from NavGraph
- Gets event data, videoUri, fps from ImportViewModel
- Extracts thumbnails using FrameExtractor

### Files Modified

#### `ui/navigation/NavGraph.kt`
- Added `Screen.EventPreview` object with route `"import/preview/{eventIndex}"`
- Added composable for EventPreview screen
- Created `EventPreviewViewModelHelper` to inject FrameExtractor
- Updated Import screen composable to pass `onEventClick` callback

#### `ui/screens/videoimport/ImportScreen.kt`
- Added `onEventClick: (Int) -> Unit` parameter to `ImportScreen`
- Added `onEventClick` parameter to `SpeedAssignmentStep`
- Made `EventSpeedCard` clickable with `onClick` parameter

#### `ui/screens/videoimport/ImportViewModel.kt`
Added helper methods for preview screen:
- `getEvent(index: Int): ShutterEvent?`
- `getAssignedSpeed(index: Int): String`
- `getRecordingFps(): Double`
- `getVideoUri(): Uri?`
- `getEventCount(): Int`

### Implementation Details

#### Frame Sampling
Events with many frames are sampled to max 8 event frames (plus 4 context frames = 12 total):
- If ≤8 event frames: show all
- If >8: evenly sample across the event duration

#### Context Frames
- 2 frames before event start (baseline brightness)
- 2 frames after event end (baseline brightness)
- Displayed with gray "closed" styling and "out" label

#### Thumbnail Extraction
- Reuses existing `FrameExtractor` class
- Extracts all needed frames in parallel
- Shows loading spinner while extracting
- Gracefully handles missing thumbnails with colored placeholders

#### Delete Flow
1. User taps "DELETE EVENT" button
2. `importViewModel.removeEvent(eventIndex)` called
3. Event removed from list, speeds reindexed
4. Navigation pops back to speed assignment list
5. If all events deleted, shows error state

### Verification Checklist
- [ ] Import video → Analyze → Click event card → See frames for that event
- [ ] From preview → Back button → Return to speed assignment list
- [ ] From preview → Delete button → Event removed, return to list with updated count
- [ ] Delete all events → Show error "All events removed"
- [ ] Verify thumbnails load with progress indicator
- [ ] Verify 2 context frames shown before/after event

### Next Steps
- Thumbnail Loading Optimization ✅

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

### Implementation

#### New State Variables in `EventReviewViewModel.kt`
```kotlin
private val loadedEventIndices = mutableSetOf<Int>()  // Track loaded events
private val eventThumbnailCache = mutableMapOf<Int, Map<Int, Bitmap>>()  // LRU cache
private var prefetchJob: Job? = null  // Background prefetch job
private var cachedVideoUri: Uri? = null  // For lazy loading
private var cachedSession: TestSession? = null
```

#### New Methods
| Method | Purpose |
|--------|---------|
| `loadThumbnailsForEvent(eventIndex)` | Extract frames for single event only |
| `prefetchAdjacentEvents(currentIndex)` | Background-load next/previous events |
| `prefetchEvent(eventIndex)` | Actual prefetch work (suspend function) |
| `evictOldEvents(currentIndex)` | LRU eviction - keeps 5 closest events |
| `updateEventWithThumbnails()` | Update single event in state |
| `onCleared()` | Clean up bitmaps when ViewModel destroyed |

#### Modified Flow
```
OLD: loadSession() → extractThumbnails(ALL events) → UI ready

NEW: loadSession() → events without thumbnails → UI ready (fast)
                   ↓
     loadThumbnailsForEvent(0) → event 0 ready (~300ms)
                   ↓
     prefetchAdjacentEvents(0) → event 1 prefetching in background
                   ↓
     User navigates → instant (prefetched) OR on-demand load
```

#### Navigation Changes
`nextEvent()` and `previousEvent()` now:
1. Check if target event is already loaded
2. If not loaded, trigger `loadThumbnailsForEvent()`
3. If loaded, just trigger prefetch for adjacent events

### Constants
| Constant | Value | Purpose |
|----------|-------|---------|
| `MAX_FRAMES_PER_EVENT` | 12 | Max thumbnails per event |
| `MAX_EVENTS_IN_MEMORY` | 5 | LRU cache size |

### Memory Management
- **Per-event memory**: 16 frames × 160px × 90px × 4 bytes ≈ 1MB
- **Max memory**: 5 events × 1MB ≈ 5MB
- **LRU eviction**: Events furthest from current index evicted first
- **Bitmap recycling**: Bitmaps recycled on eviction and ViewModel clear

### Performance Targets

| Metric | Before | After |
|--------|--------|-------|
| Initial load (10 events) | 3-5 seconds | <500ms |
| Navigate to next event | instant | instant (prefetched) |
| Navigate to distant event | instant | ~300ms (on-demand) |
| Memory (10 events) | ~10MB all loaded | ~5MB (5 event LRU) |

### Files Modified

| File | Changes |
|------|---------|
| `ui/screens/review/EventReviewViewModel.kt` | Added lazy loading, prefetch, LRU cache, memory management |

### Verification Plan
```bash
adb logcat | grep EventReviewViewModel
```
Expected log messages:
- "Loading thumbnails for event 0 only (lazy loading)"
- "Loading thumbnails for event 0"
- "Prefetching event 1"
- "Prefetch complete for event 1"
- "Event 1 already loaded, skipping extraction"
- "Evicting thumbnails for event X (too far from current Y)"

### Next Steps
- UI/UX Improvements ✅

---

## 2025-01-25 - UI/UX Improvements (todo_4)

### Overview
Implemented 10 UI/UX improvements based on user feedback to enhance usability and visual consistency.

### Changes Made

#### Task 1: Fix ExpandFramesButton Sizing
**Problem:** Button used `aspectRatio(9f/16f)` (portrait ~142dp tall) but frame thumbnails use `aspectRatio(16f/9f)` (landscape ~45dp tall). Button was 3x taller than frames.

**Fix:** Changed to `aspectRatio(16f/9f)` to match frame thumbnails. Adjusted padding from 8dp to 4dp and icon size from 24dp to 20dp for better fit.

**File:** `ui/screens/review/EventReviewScreen.kt`

#### Task 2: Change Expand Button Text
**Problem:** Both buttons showed generic "Examine\nmore" text.

**Fix:** Added `isLeft: Boolean` parameter to `ExpandFramesButton`:
- Left button: "Earlier\nframes"
- Right button: "Later\nframes"

**File:** `ui/screens/review/EventReviewScreen.kt`

#### Task 3: Update Accuracy Color Thresholds
**Problem:** Old thresholds (0-5%, 5-10%, 10-15%, 15%+) were too strict for typical film camera variance.

**Fix:** Updated to more realistic thresholds:
| Range | Color | Label |
|-------|-------|-------|
| 0-10% | Green | Good |
| 10-30% | Yellow | Fair |
| 30-60% | Orange | Poor |
| 60%+ | Red | Bad |

**File:** `ui/theme/Color.kt`

#### Task 4: Results Table Format - Fractions Only
**Problem:** Table showed "Expected (ms)" column with milliseconds. User wanted fractions only.

**Fix:**
- Removed "Speed" column (redundant with Expected)
- Changed "Expected" to show fraction (e.g., "1/500")
- Changed "Actual" header to "Measured"
- Measured column uses `formatAsSpeed()` for "1/x" format
- Removed ms display entirely

**File:** `ui/screens/results/ResultsScreen.kt`

#### Task 5: Default Camera Name When Empty
**Problem:** Empty camera name created session with `cameraId = null`, losing the test in history.

**Fix:** Always create a camera entry:
- If name provided: use provided name
- If blank: generate default "Test Jan 25 14:30" using current datetime

**File:** `ui/screens/setup/RecordingSetupViewModel.kt`

#### Task 6: Move "Begin Detecting" Button
**Problem:** Button was at top-right, awkward position during framing.

**Fix:** Moved to `Alignment.BottomCenter` with `padding(bottom = 32.dp)` for thumb-friendly access.

**File:** `ui/screens/recording/RecordingScreen.kt`

#### Task 7: Reposition Zoom/Focus Sliders
**Problem:** Sliders were side-by-side in a Row, taking too much horizontal space.

**Fix:** Changed from `Row` to `Column` layout:
- Zoom slider on top
- Focus slider on bottom
- Both aligned to right edge
- Reduced slider height from 200dp to 150dp each
- Spacing reduced from 12dp to 16dp vertical

**File:** `ui/screens/recording/RecordingScreen.kt`

#### Task 8: Exposure Lock Research
**Finding:** Current `CONTROL_AE_LOCK = true` implementation is correct. AE_LOCK freezes the auto-exposure algorithm's current ISO and shutter speed values. Manual control would require Camera2 interop for `SENSOR_SENSITIVITY` and `SENSOR_EXPOSURE_TIME`.

**Status:** No code changes needed - current implementation is sufficient.

#### Task 9: Custom Speeds - 3 Options
**Problem:** Only 2 options (Standard, Custom checkboxes).

**Fix:** Added `SpeedSelectionMode` enum with 3 options:

| Mode | Description |
|------|-------------|
| `STANDARD` | Uses STANDARD_SPEEDS list as-is (1/1000 to 1s) |
| `ADD_REMOVE` | Checkbox picker from ALL_SPEEDS (1/8000 to 8s) |
| `ENTER_CUSTOM` | Text input for comma-separated speeds |

Speed ranges:
- `STANDARD_SPEEDS`: 1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1s
- `ALL_SPEEDS`: 1/8000, 1/4000, 1/2000, 1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1s, 2s, 4s, 8s

Custom text input:
- Validates format: `1/xxx` or `xs` (e.g., "1/500, 1/250, 1s")
- Shows parse errors for invalid entries
- Shows count of valid speeds entered

**Files:**
- `ui/screens/setup/RecordingSetupViewModel.kt`
- `ui/screens/setup/RecordingSetupScreen.kt`

#### Task 10: Help Icon on Home Screen
**Problem:** Tutorial and Theory only accessible through Settings menu.

**Fix:** Added Help icon with dropdown menu in top app bar:
- Help icon (question mark) next to Settings icon
- Dropdown menu with "Tutorial" and "How It Works" options
- Tutorial → Onboarding screen
- How It Works → Theory screen

**Files:**
- `ui/screens/home/HomeScreen.kt`
- `ui/navigation/NavGraph.kt`

### Files Modified Summary

| File | Tasks |
|------|-------|
| `ui/screens/review/EventReviewScreen.kt` | 1, 2 |
| `ui/theme/Color.kt` | 3 |
| `ui/screens/results/ResultsScreen.kt` | 4 |
| `ui/screens/setup/RecordingSetupViewModel.kt` | 5, 9 |
| `ui/screens/setup/RecordingSetupScreen.kt` | 9 |
| `ui/screens/recording/RecordingScreen.kt` | 6, 7 |
| `ui/screens/home/HomeScreen.kt` | 10 |
| `ui/navigation/NavGraph.kt` | 10 |

### Next Steps
- Phase 8: Publishing

---
