# Release checklist

## Source review

- [ ] Review the complete source diff before committing or publishing.
- [x] Confirm no signing keys, credentials, local paths, private device
  identifiers, or generated outputs are included.
- [ ] Confirm third-party software and user-supplied GIF asset notices are
  suitable for public distribution.
- [x] Confirm version name/code and release notes match the artifact.

## Build and artifact

- [x] Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon`.
- [x] Record the APK path, byte size, SHA-256, version name/code, and build date.
- [x] Identify the candidate as debug-signed and the release output as unsigned.
- [ ] For production, verify secure release signing and key custody separately.

## Device verification

- [x] Check target device connection before installation.
- [x] Install the exact APK without clearing application data.
- [x] Launch the app and check startup/crash logs.
- [x] Exercise root navigation, destination screens, MENU/SELECT, and playback.
- [x] Capture fresh screenshots and inspect them.
- [x] Clearly mark untested functionality and accessibility gaps.

## GitHub publication

- [ ] Complete source/license review, including public redistribution rights for
  supplied GIFs.
- [ ] Provision production signing and verify the signed release APK.
- [ ] Push the reviewed candidate commit and prerelease tag.
- [ ] Create the GitHub prerelease with evidence-based notes.
- [ ] Attach the debug APK and SHA-256 checksum file.
- [ ] Verify the release page, tag, downloadable assets, and Actions workflow.

The v1.0.2 source candidate may be pushed for review, but it is not a release
and no APK is published. A stable release requires production signing, signed
APK verification, compatible install/update verification, and completion of
all release review gates.
