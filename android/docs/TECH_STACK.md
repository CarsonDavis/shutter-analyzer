# Shutter Analyzer - Android Tech Stack

**Status**: Decided
**Last Updated**: 2025-01-31

---

## Summary

| Layer | Technology | Rationale |
|-------|------------|-----------|
| Language | Kotlin | Modern, concise, Google's preferred |
| UI Framework | Jetpack Compose | Declarative, less boilerplate, future direction |
| Architecture | MVVM | Standard Compose pattern, well-documented |
| Camera | CameraX | Simplified API, handles device differences |
| Image Processing | OpenCV Android | Same algorithms as CLI tool |
| Database | Room | Type-safe SQLite, standard choice |
| Dependency Injection | Hilt | Google's recommended DI for Android |
| Navigation | Compose Navigation | Native Compose navigation |
| Async | Kotlin Coroutines + Flow | Standard for Kotlin async |

---

## Decisions Log

| Date | Question | Decision | Rationale |
|------|----------|----------|-----------|
| 2025-01-24 | Framework | Native Android | Camera is core - need full API access, no cross-platform overhead |
| 2025-01-24 | Language | Kotlin | Modern, Google's preferred, good tooling |
| 2025-01-24 | UI | Jetpack Compose | Declarative, less boilerplate, matches MVVM well |
| 2025-01-24 | Camera API | CameraX | Simpler than Camera2, handles device quirks |
| 2025-01-24 | Architecture | MVVM | Standard for Compose, good learning resources |
| 2025-01-24 | Database | Room | Type-safe SQLite abstraction |

---

## Stack Details

### 1. Language: Kotlin

```kotlin
// Example: Data class for a camera profile
data class CameraProfile(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant = Instant.now()
)
```

**Why Kotlin over Java:**
- Null safety built into type system
- Data classes, coroutines, extension functions
- 100% interoperable with Java libraries
- Google's official preferred language since 2019

---

### 2. UI: Jetpack Compose

```kotlin
// Example: Results screen composable
@Composable
fun ResultsDashboard(
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column {
        Text("Average Deviation: ${state.avgDeviation}%")
        AccuracyTable(results = state.results)
        BrightnessTimeline(events = state.events)
    }
}
```

**Key Compose concepts to learn:**
- `@Composable` functions
- State hoisting with `remember` and `mutableStateOf`
- `LaunchedEffect` for side effects
- Navigation with `NavHost`

---

### 3. Architecture: MVVM

```
┌────────────────────────────────────────────────────────────┐
│                         UI Layer                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    Compose Screens                    │  │
│  │   HomeScreen, RecordingScreen, ResultsScreen, etc.   │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │                               │
│                            │ observes StateFlow            │
│                            ▼                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                     ViewModels                        │  │
│  │   HomeViewModel, RecordingViewModel, ResultsViewModel │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
                             │
                             │ calls
                             ▼
┌────────────────────────────────────────────────────────────┐
│                       Domain Layer                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                     Use Cases                         │  │
│  │   AnalyzeVideoUseCase, SaveCameraUseCase, etc.       │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
                             │
                             │ uses
                             ▼
┌────────────────────────────────────────────────────────────┐
│                        Data Layer                          │
│  ┌─────────────────────┐    ┌───────────────────────────┐  │
│  │    Repositories     │    │       Data Sources        │  │
│  │  CameraRepository   │◄───│  Room DB, CameraX, OpenCV │  │
│  │  AnalysisRepository │    │                           │  │
│  └─────────────────────┘    └───────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

**ViewModel example:**
```kotlin
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val analyzeFrameUseCase: AnalyzeFrameUseCase,
    private val saveEventUseCase: SaveEventUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun onShutterDetected(event: ShutterEvent) {
        viewModelScope.launch {
            saveEventUseCase(event)
            _state.update { it.copy(
                currentSpeedIndex = it.currentSpeedIndex + 1,
                detectedEvents = it.detectedEvents + event
            )}
        }
    }
}
```

---

### 4. Camera: CameraX

```kotlin
// Example: Setting up slow-motion recording
val recorder = Recorder.Builder()
    .setQualitySelector(QualitySelector.from(Quality.FHD))
    .build()

val videoCapture = VideoCapture.withOutput(recorder)

// CameraX handles device-specific slow-mo capabilities
cameraProvider.bindToLifecycle(
    lifecycleOwner,
    CameraSelector.DEFAULT_BACK_CAMERA,
    preview,
    videoCapture,
    imageAnalysis  // For real-time brightness detection
)
```

**CameraX advantages:**
- Consistent API across devices
- Handles camera lifecycle automatically
- `ImageAnalysis` use case for real-time frame processing
- Easier than Camera2 for common tasks

**Limitation:** CameraX slow-motion support varies by device. May need to fall back to Camera2 for specific high-speed modes on some devices.

---

### 5. Image Processing: OpenCV Android

```kotlin
// Example: Calculate frame brightness (same logic as Python CLI)
fun calculateBrightness(frame: Mat): Double {
    val gray = Mat()
    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
    return Core.mean(gray).`val`[0]
}

// Real-time analysis during recording
class BrightnessAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val mat = image.toMat()  // Convert to OpenCV Mat
        val brightness = calculateBrightness(mat)

        if (brightness > threshold) {
            onShutterDetected(brightness)
        }

        image.close()
    }
}
```

**Integration:**
- Add OpenCV Android SDK to project
- Use `ImageAnalysis.Analyzer` for real-time processing
- Same brightness calculation as Python CLI

---

### 6. Database: Room

```kotlin
// Entity
@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "test_sessions")
data class TestSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cameraId: Long,
    val testedAt: Long,
    val avgDeviation: Double
)

@Entity(tableName = "shutter_events")
data class ShutterEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val expectedSpeed: String,      // "1/500"
    val measuredDurationMs: Double,
    val deviationPercent: Double,
    val frameData: String?          // JSON blob of frame brightnesses
)

// DAO
@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY createdAt DESC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    @Insert
    suspend fun insert(camera: CameraEntity): Long

    @Query("SELECT * FROM test_sessions WHERE cameraId = :cameraId")
    fun getSessionsForCamera(cameraId: Long): Flow<List<TestSessionEntity>>
}

// Database
@Database(
    entities = [CameraEntity::class, TestSessionEntity::class, ShutterEventEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun sessionDao(): SessionDao
    abstract fun eventDao(): EventDao
}
```

---

### 7. Dependency Injection: Hilt

```kotlin
// Application
@HiltAndroidApp
class ShutterAnalyzerApp : Application()

// Module for database
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "shutter_analyzer.db"
        ).build()
    }

    @Provides
    fun provideCameraDao(db: AppDatabase): CameraDao = db.cameraDao()
}

// ViewModel injection
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel()
```

---

### 8. Navigation: Compose Navigation

```kotlin
// Navigation graph
@Composable
fun ShutterAnalyzerNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNewTest = { navController.navigate("setup") },
                onCameraClick = { id -> navController.navigate("camera/$id") }
            )
        }
        composable("setup") {
            RecordingSetupScreen(
                onStartRecording = { config ->
                    navController.navigate("recording/${config.toJson()}")
                }
            )
        }
        composable("recording/{config}") { backStackEntry ->
            RecordingScreen(
                onComplete = { events ->
                    navController.navigate("review/${events.toJson()}")
                }
            )
        }
        composable("review/{events}") {
            EventReviewScreen(
                onConfirm = { navController.navigate("results") }
            )
        }
        composable("results") { ResultsDashboard() }
        composable("camera/{id}") { CameraDetailScreen() }
        composable("settings") { SettingsScreen() }
    }
}
```

---

## Project Structure

```
app/
├── src/main/java/com/shutteranalyzer/
│   ├── ShutterAnalyzerApp.kt           # Application class
│   ├── MainActivity.kt                  # Single activity
│   │
│   ├── ui/                              # UI Layer
│   │   ├── navigation/
│   │   │   └── NavHost.kt
│   │   ├── screens/
│   │   │   ├── home/
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   └── HomeViewModel.kt
│   │   │   ├── recording/
│   │   │   │   ├── RecordingScreen.kt
│   │   │   │   └── RecordingViewModel.kt
│   │   │   ├── review/
│   │   │   │   ├── EventReviewScreen.kt
│   │   │   │   └── EventReviewViewModel.kt
│   │   │   ├── results/
│   │   │   │   ├── ResultsDashboard.kt
│   │   │   │   └── ResultsViewModel.kt
│   │   │   └── ...
│   │   ├── components/                  # Reusable composables
│   │   │   ├── AccuracyTable.kt
│   │   │   ├── BrightnessChart.kt
│   │   │   └── FrameThumbnail.kt
│   │   └── theme/
│   │       └── Theme.kt
│   │
│   ├── domain/                          # Domain Layer
│   │   ├── model/
│   │   │   ├── Camera.kt
│   │   │   ├── TestSession.kt
│   │   │   └── ShutterEvent.kt
│   │   └── usecase/
│   │       ├── AnalyzeFrameUseCase.kt
│   │       ├── CalculateShutterSpeedUseCase.kt
│   │       └── SaveTestSessionUseCase.kt
│   │
│   ├── data/                            # Data Layer
│   │   ├── local/
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── entities/
│   │   │   │   └── dao/
│   │   │   └── datastore/
│   │   │       └── SettingsDataStore.kt
│   │   ├── camera/
│   │   │   ├── CameraManager.kt
│   │   │   └── BrightnessAnalyzer.kt
│   │   └── repository/
│   │       ├── CameraRepository.kt
│   │       └── AnalysisRepository.kt
│   │
│   └── di/                              # Dependency Injection
│       ├── DatabaseModule.kt
│       ├── CameraModule.kt
│       └── RepositoryModule.kt
│
├── src/main/res/
│   └── ...
│
└── build.gradle.kts
```

---

## Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt (using KSP instead of kapt)
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-compiler:2.54")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (using KSP instead of kapt)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX
    val camerax_version = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-video:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // OpenCV
    implementation("org.opencv:opencv:4.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Charts: Custom Compose Canvas (no external library)
}
```

---

## Minimum Requirements

| Requirement | Value | Reason |
|-------------|-------|--------|
| Min SDK | 26 (Android 8.0) | Camera2 slow-motion APIs |
| Target SDK | 35 (Android 15) | Play Store requirement |
| Compile SDK | 35 | Latest stable |
| Kotlin | 2.1.0 | Latest stable with Compose plugin |
| Gradle | 8.10.2 | Required for AGP 8.7+ |

---

## References

- [APP_SPEC.md](APP_SPEC.md) - App requirements and flows
- [WIREFRAMES.md](WIREFRAMES.md) - Screen mockups
- [Jetpack Compose docs](https://developer.android.com/jetpack/compose)
- [CameraX docs](https://developer.android.com/training/camerax)
- [Room docs](https://developer.android.com/training/data-storage/room)
