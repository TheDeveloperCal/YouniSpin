# YouniSpin test results

## Release candidate 1.0.1

Date: 2026-11-11
Package: `com.youni.younispin`
Target: BLU B1660V, Android 15 (API 35)

### Build and tests

| Check | Result | Detail |
| --- | --- | --- |
| `testDebugUnitTest` | PASS | Completed successfully with JDK 21. |
| `assembleDebug` | PASS | Created the APK below from the v1.0.1 source. |
| `lintDebug` | BLOCKED | Android lint crashed in `MissingClassDetector` with `NoClassDefFoundError: org/jetbrains/kotlin/fir/java/scopes/JavaClassUseSiteMemberScope`. This is a lint tool failure; lint is not reported as passing. |
| `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` | NOT CLEAN | The requested combined command was attempted; lint failure prevented a successful combined result. Tests and APK assembly passed when run separately. |

The lint failure was reproduced with JDK 21, one Gradle worker, and Gradle
instrumentation disabled. Disabling only the named detector in the module
configuration did not avoid the crash, so that workaround was removed. No
Gradle/Kotlin/AGP versions were changed to mask it.

### APK and installation

- APK path: `app/build/outputs/apk/debug/app-debug.apk`
- Version name/code: `1.0.1` / `2`
- Size: `25,756,458` bytes.
- SHA-256: `ad75dc780e3241b60e3ff11b5abba2faf1950fd3a4117e4de9b507aab0da579e`
- Signing: Android debug signing only; not production signed.
- Installation: `adb install -r` returned `Success`; app data was not cleared.
- Installed package reported version `1.0.1`, versionCode `2`; the app was
  force-stopped and relaunched.
- `dumpsys activity` reported `com.youni.younispin/.MainActivity` as the top
  resumed activity.
- Recent logcat contained no `FATAL EXCEPTION` or app-process fatal entry.

### Device exercise

- Opened the classic root screen and selected Library with the center control.
- Opened All Songs and observed the available local tracks.
- Started `18 Carat Affair - Death Became Her`; Android MediaSession reported
  `PLAYING(3)` with advancing position and buffered position.
- Tapped the Now Playing cloud-download action. It opened Edit Track Metadata
  and started the online query; the test file returned “No matches found.”
  No metadata was applied. This verifies the user path and empty-result
  handling, not successful artist/title/cover identification.
- Captured fresh screenshots from the installed 1.0.1 APK:
  - `docs/device-evidence/release-v1.0.1/home.png`
  - `docs/device-evidence/release-v1.0.1/library.png`
  - `docs/device-evidence/release-v1.0.1/songs.png`
  - `docs/device-evidence/release-v1.0.1/now-playing.png`
  - `docs/device-evidence/release-v1.0.1/metadata-scan.png`
- The selected track showed `Unknown Artist` and a branded missing-art fallback.
  Its file lacks usable metadata/art in this test, so this run does not verify
  that an online scan finds and applies matching metadata or cover art.
- MediaSession state is evidence that playback was started, not an independent
  measurement of acoustic output.
- Joystick repeat gestures, full playback-control regression, notification
  actions, radio, theme switching, and TalkBack were not retested in this
  release-candidate pass. See historical results and limitations in
  `FUNCTIONALITY_MATRIX.md`.

## Historical verification

Earlier 1.0.0 device sessions tested Orbit Dial navigation/playback controls
and selected Radio Live paths on the same handset. Their screenshots and
recordings are retained under `docs/device-evidence/`; they are historical
evidence, not a substitute for re-testing the full feature set on 1.0.1.
