# Release checklist

## Source review

- [ ] Review the complete source and staged diff.
- [ ] Confirm no signing keys, credentials, local paths, private device
  identifiers, or generated outputs are included.
- [ ] Confirm third-party software and asset notices are accurate.
- [ ] Confirm version name/code and release notes match the artifact.

## Build and artifact

- [ ] Run `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon`.
- [ ] Record the APK path, byte size, SHA-256, version name/code, and build date.
- [ ] Identify the artifact as debug/testing or production-signed.
- [ ] For production, verify secure release signing and key custody separately.

## Device verification

- [ ] Check target device connection before installation.
- [ ] Install the exact APK without clearing application data.
- [ ] Launch the app and check startup/crash logs.
- [ ] Exercise root and library navigation, Orbit Dial controls, and playback.
- [ ] Capture fresh screenshots and inspect them.
- [ ] Clearly mark untested functionality and accessibility gaps.

## GitHub publication

- [ ] Review the repository contents, license, documentation, and APK.
- [ ] Push the reviewed commit and version tag.
- [ ] Create a GitHub Release with concise, evidence-based notes.
- [ ] Attach the APK and SHA-256 checksum file.
- [ ] Verify release page, tag, and downloadable assets.
- [ ] Check the Actions workflow and report its status.
