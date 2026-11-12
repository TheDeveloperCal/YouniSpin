# Building YouniSpin

## Requirements

- JDK 17 or newer.
- Android SDK with platform/build tools for API 35.
- Gradle wrapper included in the repository (Gradle 8.10.2).
- Android Gradle Plugin 8.8.2 and Kotlin 2.0.21 are declared by the project.

Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` to your Android SDK location. If
needed, accept the Android SDK licenses using the Android SDK tools.

## Build and validate

From the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.youni.younispin 1
```

The build is debug-signed. No production signing configuration or credentials
are committed.
