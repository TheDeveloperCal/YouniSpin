# Installation

## Published APK availability

The production-signed v1.0.3 release is available on
[GitHub](https://github.com/TheDeveloperCal/YouniSpin/releases/tag/v1.0.3).
Download [`app-release.apk`](https://github.com/TheDeveloperCal/YouniSpin/releases/download/v1.0.3/app-release.apk).
Its SHA-256 is
`0216271139e673206b23bab4afe3770b9c3983d90e7a14b6549434deca7ac66d`.
The release also includes a [checksum file](https://github.com/TheDeveloperCal/YouniSpin/releases/download/v1.0.3/YouniSpin-v1.0.3-app-release.apk.sha256).

## v1.0.3 production release

The v1.0.3 APK is signed with the YouniSpin production key. Its package is
`com.youni.younispin`, version `1.0.3` (versionCode `4`). Install it only on a
compatible Android 8.0 (API 26) or later device.

The earlier v1.0.2 debug candidate's local music, notification, metadata
lookup, radio playback, navigation, and GIF visualizer paths were verified on
the BLU; see [`TEST_RESULTS.md`](TEST_RESULTS.md).

To install, download the APK on the Android device, open it, and follow
Android's installation prompt. The v1.0.3 isolated install and launch check
could not be completed because there was no configured emulator or cached
system image. The BLU B1660V was not used for this signing-change test; see
[`TEST_RESULTS.md`](TEST_RESULTS.md).

Android will reject an in-place update from a debug-signed evaluation build
because its certificate differs. If you need to preserve data from such an
installation, do not uninstall it; back up its app data before planning a
migration. The BLU installation and its data were left untouched.

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
