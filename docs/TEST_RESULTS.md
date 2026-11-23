# YouniSpin test results

## v1.0.2 release candidate

Date: 2026-11-22
Package: `com.youni.younispin`
Device: BLU B1660V, Android 15/API 35, 720×1600 portrait
Device ID: omitted from this public report.

### Build and static checks

The final source build completed successfully with:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon
```

| Task | Result | Detail |
| --- | --- | --- |
| `testDebugUnitTest` | PASS | Includes focused filename-to-MusicBrainz-query test coverage. |
| `lintDebug` | PASS | An initial `SuspiciousIndentation` finding in the Orbit Dial drag code was corrected. The subsequent full run passed; only deprecation warnings remained. The earlier Kotlin FIR detector crash did not recur. |
| `assembleDebug` | PASS | Candidate debug APK built. |
| `assembleRelease` | PASS | Release variant assembled, but unsigned. |

### APK verification

- Version name/code: `1.0.2` / `3`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Debug APK size: `37,358,959` bytes.
- Debug APK SHA-256: `fb4008c5761fa5cedc0027ad692219df5dd37c5733a23867b4e8351015d5316c`.
- `apksigner verify` passed for the debug APK (APK Signature Scheme v2).
- Certificate: Android Debug; this is not a production signing identity.
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, `29,174,170` bytes; signature verification fails because it is unsigned.
- Release APK SHA-256: `1703eb6d3d2385229ba667fd55456b8994c083d18d68d7449896f636641d9d3f`.
- GitHub currently shows no published releases or tags. The v1.0.2 debug APK
  is retained locally for evaluation and must not be presented as production.

### Installation and device checks

The exact candidate debug APK was installed with `adb install -r` over the
existing app. Android reported `Success`; package inspection showed version
1.0.2/versionCode 3. No app data was cleared. The app launched and remained
usable. Candidate captures are saved in `docs/device-evidence/`.

- Opened MENU, selected Library, confirmed it closed and the Library used the
  available display width. Reopened MENU from destinations. Playlists, Radio
  Live, Visualizer, Equalizer, Settings, and Now Playing all opened through
  the same temporary menu behavior.
- Android Back closed MENU and returned from Now Playing to the previous
  destination. Local playback remained active. MENU did not reopen
  unexpectedly.
- Center SELECT started the actual downloaded “Death Became Her” track by
  18 Carat Affair. MediaSession reported `PLAYING(3)` with advancing position.
- Notification shade showed the actual track artwork, title, artist, and
  previous/play-pause/next actions.
- Equalizer displayed five bands across the usable screen width during active
  playback. Changing a band changed its displayed value; AudioEffect parameter
  reads/writes appeared in Android logs. Playback remained active. No
  instrumented acoustic response test was performed.
- Beat Blender and Black Rock FM each entered LIVE state from the BLU over
  Wi-Fi. MediaSession reported `PLAYING(3)` and station metadata updated for
  both. Screenshots: `younispin-candidate-radio.png` and
  `younispin-candidate-radio-second.png`.
- Visualizer Studio listed the supplied Simpsons Wave, Monkey Music, and
  Vaporwave Sunset GIFs. Simpsons Wave was selected and shown animating in the
  full-screen Visualizer while radio playback continued.
- MusicBrainz scan on the actual local file logged HTTP 200, `count=1 parsed=1`
  for `recording:"Death Became Her" AND artist:"18 Carat Affair" AND
  release:"Televised Tragedies"`. The UI showed a strong exact-title/artist
  candidate. The release was not resolved unambiguously, and the existing
  title, artist, and album were already correct, so the suggestion was
  dismissed without writing unnecessary metadata to the database.
- Orbit Dial long-press seek and every directional repeat edge were not
  exhaustively repeated in this candidate run. Historical recordings remain
  in `docs/device-evidence/`.
- TalkBack, Bluetooth routing, radio reconnect/failure, and GIF import error
  cases were not covered in this run.

### Release status

This artifact is **not a stable production release**. The debug APK uses the
Android development key and the release APK is unsigned. No production
keystore or protected signing configuration is available. Do not distribute
the unsigned release APK as an installable production artifact. The candidate
has not been published. Source publication status is tracked in the repository
history; no APK asset is attached to a GitHub release.
