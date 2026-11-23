# Release checklist

## Source review

- [x] Review the complete source diff before committing or publishing.
- [x] Confirm no signing keys, credentials, local paths, private device
  identifiers, or generated outputs are included.
- [ ] Confirm third-party software and user-supplied GIF asset notices are
  suitable for public distribution.
- [x] Confirm version name/code and release notes match the artifact.

## Build and artifact

- [x] Run `./gradlew testDebugUnitTest lintDebug lintRelease assembleDebug assembleRelease --no-daemon`.
- [x] Record the APK path, byte size, SHA-256, certificate, version name/code, and build date.
- [x] Verify the production certificate and release APK signature.
- [x] Keep production signing material outside Git with owner-only permissions.

## Device verification

- [x] Check target devices; only the protected BLU was connected.
- [ ] Install and launch the v1.0.3 APK on an isolated emulator or test device.
- [ ] Check startup/crash logs for the v1.0.3 APK.
- [x] Preserve the existing BLU installation and its data.
- [x] Clearly document the unavailable isolated test environment.

## GitHub publication

- [x] Obtain explicit authorization to publish with the isolated-test limitation.
- [x] Reauthenticate the local GitHub CLI as `TheDeveloperCal` and verify push access.
- [x] Push commit `3b5ee9a` to `main` without force-pushing.
- [x] Create and publish the GitHub release `v1.0.3` with evidence-based notes.
- [x] Attach the verified production APK and SHA-256 checksum file.
- [x] Verify the release page, tag, both assets, and public APK download checksum.
- [ ] Verify the GitHub Actions workflow for this release.

The v1.0.3 production-signed release is published. Isolated installation and
launch remain unverified and are disclosed in the release notes. The existing
BLU installation and data were left untouched.
