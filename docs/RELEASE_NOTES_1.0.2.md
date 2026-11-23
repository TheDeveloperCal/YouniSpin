# YouniSpin 1.0.2 — Evaluation prerelease

This is a debug-signed release candidate for device evaluation. It is not a
stable production release and is not signed with a production key.

## Changes

- MENU now displays a temporary full-width navigation overlay. Choosing a
  destination closes it; MENU opens it again from destinations; Android Back
  closes the overlay or returns to the previous destination.
- Equalizer UI and playback now use the same audio-effects manager, so the
  sliders and presets target the active Media3 audio session.
- Radio Live has two built-in SomaFM fallbacks, Groove Salad and Drone Zone,
  when the catalogue cannot be reached. On the BLU over Wi-Fi, Beat Blender
  and Black Rock FM both entered LIVE playback and updated station metadata.
- Notification shade displays actual track artwork, title, artist, and
  previous/play-pause/next controls.
- Three supplied GIFs are selectable as bundled visualizer backgrounds.
  Imports are copied with a 12 MB cap using bounded streaming I/O.
- MusicBrainz search separates artist and recording fields, can derive them
  from a filename, and retries without album when a strict query returns no
  matches. The actual BLU track “Death Became Her” returned an app-parsed strong
  match (HTTP 200, one result). Its ambiguous release was not applied; existing
  title/artist/album data was already correct.

## Verification limits

- Installed over the existing app with `adb install -r`; app data was not
  cleared. Device evidence uses the BLU B1660V.
- The supplied GIFs appeared as bundled options. Simpsons Wave was selected
  and animated in the full-screen Visualizer while radio playback continued.
- The debug APK is signed with Android's development key. No production
  signing configuration is available, so this candidate must not be described
  as a stable production release.
- Not published to GitHub. Production signing is unavailable, so the build
  remains a local evaluation candidate rather than a stable release.
- See `TEST_RESULTS.md` and `IMPLEMENTATION_STATUS.md` for build, lint, device,
  and feature-by-feature results.
