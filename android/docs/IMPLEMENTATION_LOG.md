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
- ~~Phase 6: Visualization & Charts~~ ✅ Complete (see below)
- Phase 7: Polish & Testing
- Phase 8: Publishing

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
- Phase 7: Polish & Testing
- Phase 8: Publishing

---
