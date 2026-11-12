# Installation

## Debug/testing APK

The GitHub Release provides the v1.0.1 debug APK and a SHA-256 checksum file.
Download both from:

https://github.com/TheDeveloperCal/YouniSpin/releases/tag/v1.0.1

Verify the APK before installation:

```bash
sha256sum --check YouniSpin-v1.0.1-debug.apk.sha256
```

Allow installation from the browser or file manager when Android prompts. The
APK uses Android's standard debug signing key; it is intended for evaluation,
not production distribution.

Install with ADB:

```bash
adb install -r YouniSpin-v1.0.1-debug.apk
adb shell monkey -p com.youni.younispin 1
```

An APK signed with a different key cannot update an existing installation.
Uninstalling first can erase application data, so export or back up user data
before doing so.

## Permissions

Grant music/audio access to scan local tracks. Android may also request
notification permission so playback controls can appear in the notification
shade. Internet access is used for radio streams and metadata searches the user
starts; the app can still browse local music without those network services.

## Production signing

This repository does not contain a production keystore or signing credentials.
The debug APK is not a Play Store-ready artifact. A maintainer must provision a
private release key, store credentials in the release environment's secret
manager, and configure a protected release-signing job before publishing a
production build.
