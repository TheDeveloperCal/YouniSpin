# Installation

## Published APK availability

There is currently no published GitHub release or downloadable APK. The
v1.0.3 candidate is locally production-signed and must complete isolated
emulator validation before publication. Do not install it over the existing
BLU app: that installation uses the Android debug key, and changing signing
identity would be rejected by Android.

## v1.0.3 production candidate

The v1.0.3 candidate uses the local production key. Its package is
`com.youni.younispin`, version `1.0.3` (versionCode `4`). Install it only on a
separate emulator or test device until its validation is complete. The BLU
installation and its app data are intentionally left untouched.

The earlier v1.0.2 debug candidate's local music, notification, metadata
lookup, radio playback, navigation, and GIF visualizer paths were verified on
the BLU; see [`TEST_RESULTS.md`](TEST_RESULTS.md).

The v1.0.3 isolated install and launch check has not been completed because
there is no configured emulator or cached system image. See
[`TEST_RESULTS.md`](TEST_RESULTS.md) for the exact status. Do not install this
candidate on the BLU B1660V.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.youni.younispin 1
```

Do not install the production-signed candidate over the debug-signed BLU app.
Do not uninstall that app or clear its data.

## Permissions

Grant music/audio access to scan local tracks. Android may also request
notification permission so playback controls can appear in the notification
shade. Internet access is used for radio streams and metadata searches the user
starts; the app can still browse local music without those network services.

## Production signing

The production keystore and signing properties are stored outside this
repository under `~/.config/younispin/`, with owner-only permissions.
They are excluded from Git and must never be uploaded. Back up the keystore
and signing properties together to encrypted storage accessible only to the
maintainer. Never regenerate this key for an update.
