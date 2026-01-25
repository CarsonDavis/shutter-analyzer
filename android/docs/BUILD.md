# Building Shutter Analyzer Android App

This document explains how to build and run the Shutter Analyzer Android app locally.

## Prerequisites

1. **Android Studio** (Arctic Fox 2020.3.1 or later)
   - The project uses Android Studio's bundled JDK, so no separate Java installation is required.

2. **Android SDK**
   - Minimum SDK: 26 (Android 8.0)
   - Target SDK: 35 (Android 15)
   - Build Tools: Latest stable

3. **Device or Emulator**
   - Physical device recommended for camera features
   - Android 8.0+ required

## Project Structure

```
android/
├── app/
│   ├── build.gradle.kts      # App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/shutteranalyzer/
│       └── res/
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
```

## Building from Android Studio

1. **Open Project**
   - Open Android Studio
   - Select "Open" and navigate to the `android/` directory
   - Wait for Gradle sync to complete

2. **Build APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or use the "Run" button to build and install directly

3. **Run on Device**
   - Connect a physical device with USB debugging enabled
   - Select the device from the dropdown
   - Click "Run" (green play button)

## Building from Command Line

### Using Android Studio's Bundled JDK

The project uses Android Studio's bundled JetBrains Runtime (JBR). Set JAVA_HOME before building:

```bash
# macOS
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Windows
set JAVA_HOME="C:\Program Files\Android Studio\jbr"

# Linux
export JAVA_HOME="/opt/android-studio/jbr"
```

### Build Commands

```bash
cd android/

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Run tests
./gradlew test

# Check for lint errors
./gradlew lint
```

### APK Location

After building, the APK is located at:
- Debug: `android/app/build/outputs/apk/debug/app-debug.apk`
- Release: `android/app/build/outputs/apk/release/app-release-unsigned.apk`

## Installing on Device

```bash
# Install debug build via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Install and replace existing
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### "Unable to locate Java Runtime"

Set JAVA_HOME to Android Studio's bundled JDK:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### Gradle Sync Failed

1. File → Invalidate Caches and Restart
2. Delete `.gradle/` and `build/` directories, then sync again

### Camera Not Working on Emulator

The camera features require a physical device. The emulator has limited camera support for high-speed recording.

### Build Takes Too Long

Enable Gradle build cache:

```properties
# In gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
```

## Dependencies

The project uses the following major dependencies:

| Dependency | Purpose |
|------------|---------|
| Jetpack Compose | UI framework |
| CameraX | Camera access and video recording |
| Room | SQLite database |
| Hilt | Dependency injection |
| OpenCV | Image processing |
| DataStore | Settings persistence |

See `app/build.gradle.kts` for the complete list with versions.

## Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "com.shutteranalyzer.analysis.*"
```
