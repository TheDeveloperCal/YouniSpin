# Installation

## Latest published APK

The latest published GitHub release is v1.0.1. Download the APK and checksum
from:

https://github.com/TheDeveloperCal/YouniSpin/releases/tag/v1.0.1

Verify the published APK before installation:

```bash
sha256sum --check YouniSpin-v1.0.1-debug.apk.sha256
```

Allow installation from the browser or file manager when Android prompts. The
APK uses Android's standard debug signing key. This is an evaluation
prerelease, not a production-signed stable release.

Install with ADB:

```bash
adb install -r YouniSpin-v1.0.1-debug.apk
adb shell monkey -p com.youni.younispin 1
```

## v1.0.2 evaluation build

The v1.0.2 candidate is not published because it is debug-signed. Its local
music, notification, metadata lookup, radio
playback, navigation, and GIF visualizer paths were verified on the BLU; see
[`TEST_RESULTS.md`](TEST_RESULTS.md). Build it from source with the commands in
[`BUILDING.md`](BUILDING.md). Install the generated debug APK with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.youni.younispin 1
```

The `-r` option updates the installed app while preserving its data when the
signing key matches. If Android rejects an update because the APK signing key
differs, do not uninstall if preserving app data is important; export or back
up playlists/settings first.

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
