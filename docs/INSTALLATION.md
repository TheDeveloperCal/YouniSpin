# Installation

## Published APK availability

There is currently no published GitHub release or downloadable APK. The
v1.0.2 APK is a local debug-signed evaluation build, not a production release.
Do not install an APK signed with a different key over an existing build if
you need to preserve its app data.

## v1.0.2 evaluation build

The v1.0.2 candidate is debug-signed. Its local music, notification, metadata
lookup, radio playback, navigation, and GIF visualizer paths were verified on
the BLU; see [`TEST_RESULTS.md`](TEST_RESULTS.md). Build it from source with
the commands in [`BUILDING.md`](BUILDING.md). To install the generated debug
APK over an installation signed with the matching debug key:

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
The release variant currently assembles unsigned. A maintainer must provision
and securely back up a private release key, configure protected signing, and
verify update/install behavior before publishing a production build.
