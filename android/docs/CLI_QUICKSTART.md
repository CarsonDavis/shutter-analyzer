# CLI Quickstart

## Setup
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
alias adb=~/Library/Android/sdk/platform-tools/adb
```

## Build
```bash
cd android
./gradlew assembleDebug
```

## Install
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Get Crash Logs
```bash
# Clear logs, reproduce crash, then:
adb logcat -c
# ... trigger crash ...
adb logcat -d | grep -B 5 -A 30 "FATAL EXCEPTION"
```

## Real-time Log Monitoring
```bash
adb logcat *:E | grep -E "(AndroidRuntime|shutteranalyzer)"
```
