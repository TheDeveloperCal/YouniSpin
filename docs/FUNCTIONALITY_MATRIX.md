# YouniSpin functionality matrix

This records observed behavior on the v1.0.2 candidate installed on the BLU
B1660V (Android 15/API 35), plus explicit verification limits. See
[`TEST_RESULTS.md`](TEST_RESULTS.md) for the run details.

| Area | Status | Evidence / qualification |
| --- | --- | --- |
| Root menu and full-width Library | VERIFIED ON PHYSICAL DEVICE | Temporary menu opens from MENU, selection closes it, Library uses the available display width; MENU reopens. |
| Playlists, Radio Live, Visualizer, Equalizer, Settings, Now Playing | VERIFIED ON PHYSICAL DEVICE | Opened as destinations from the menu. Selection dismissed the menu. Candidate evidence is in `device-evidence/younispin-candidate-playlists.png`, `...-radio.png`, `...-visualizer-gif.png`, `...-equalizer.png`, `...-settings.png`, and `...-now-playing.png`. |
| Android Back navigation | VERIFIED ON PHYSICAL DEVICE | Back closes an open menu; Back from Now Playing returned to the previous screen while playback remained active. |
| Orbit Dial MENU and center SELECT | VERIFIED ON PHYSICAL DEVICE | MENU opened navigation; SELECT started a local track. Directional repeat and long-press seek are covered by historical recordings, not all re-exercised on v1.0.2. |
| Local track playback across screens | VERIFIED ON PHYSICAL DEVICE | “Death Became Her” by 18 Carat Affair remained `PLAYING(3)` while navigating. |
| Notification shade artwork, metadata, transport | VERIFIED ON PHYSICAL DEVICE | Actual track artwork/title/artist and previous, pause, next controls captured in `younispin-candidate-notification.png`. |
| Equalizer UI and audio session binding | VERIFIED ON PHYSICAL DEVICE | Five bands fit the full content width. Adjusted value and AudioEffect `getParameter`/`setParameter` calls observed during active playback. Acoustic response not measured. |
| Radio playback | VERIFIED ON PHYSICAL DEVICE | Beat Blender and Black Rock FM each displayed LIVE and played on the BLU; MediaSession showed `PLAYING(3)`. Evidence: `younispin-candidate-radio.png` and `younispin-candidate-radio-second.png`. |
| MusicBrainz lookup request and response parsing | VERIFIED ON PHYSICAL DEVICE | Actual local track query returned HTTP 200, count=1, parsed=1. Strong exact recording/artist result shown in `younispin-candidate-metadata-search.png`. |
| MusicBrainz result application | NOT NEEDED FOR TEST TRACK | Result omitted an unambiguous album/release; the local track already had correct title, artist, and album. The suggestion was dismissed without changing database data. |
| Bundled GIF options and live rendering | VERIFIED ON PHYSICAL DEVICE | All three supplied GIFs were seeded as choices; Simpsons Wave selection animated in full-screen Visualizer while radio remained playing. See `younispin-candidate-gif-choices.png` and `younispin-candidate-visualizer-gif.png`. |
| GIF upload limits and storage | IMPLEMENTED | Bounded streaming import and validation are present; large/corrupt-file and low-storage failure cases were not physically tested. |
| Unit tests, lint, debug and release APK assembly | PASS | All four Gradle tasks passed in the final source build. Release output is unsigned. |
| Production signing and GitHub release | BLOCKED | v1.0.2 has no production signing setup and is not published. Public latest remains v1.0.1. |
| TalkBack, Bluetooth routing, station recovery edge cases | NOT VERIFIED IN THIS PASS | Requires separate device scenarios. |
