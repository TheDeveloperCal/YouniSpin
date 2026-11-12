# YouniSpin functionality matrix

The table records the strongest available evidence, not an assumption that
every path was exercised in the latest release. Physical evidence from earlier
builds is identified as historical; release-candidate checks are in
`TEST_RESULTS.md`.

| Area | Status | Evidence / qualification |
| --- | --- | --- |
| Classic display above Orbit Dial; portrait layout | VERIFIED ON PHYSICAL DEVICE | Fresh 1.0.1 screenshot `device-evidence/release-v1.0.1/home.png`. |
| Root menu and Library navigation | VERIFIED ON PHYSICAL DEVICE | Opened on the 1.0.1 BLU install; see `release-v1.0.1/library.png`. |
| Center SELECT and menu navigation | VERIFIED ON PHYSICAL DEVICE | Center opened Library on 1.0.1. Up/down repeat and release behavior were verified on 1.0.0; see the joystick evidence named below. |
| Previous/next, hold rewind/fast-forward, MENU, play/pause | VERIFIED ON PHYSICAL DEVICE | Individually tested on 1.0.0. Not all controls were repeated during the 1.0.1 release check. |
| Local Media3 playback and Now Playing metadata | VERIFIED ON PHYSICAL DEVICE | 1.0.1 local track produced `PLAYING(3)` with advancing position; see `release-v1.0.1/now-playing.png`. |
| Metadata scan entry point and lookup execution | VERIFIED ON PHYSICAL DEVICE | The Now Playing cloud action opened the editor and started the lookup; the controlled test track returned “No matches found.” Evidence: `device-evidence/release-v1.0.1/metadata-scan.png`. |
| Metadata matching, ambiguity handling, and unit tests | IMPLEMENTED | Resolver unit tests are included in `testDebugUnitTest`; no claim of successful physical metadata enrichment. |
| Radio Browser directory and search | VERIFIED ON PHYSICAL DEVICE | Directory and station-name search exercised on the earlier 1.0.0 build. |
| SomaFM station catalogue, live metadata, and playback | VERIFIED ON PHYSICAL DEVICE | Official catalogue and live station metadata exercised on 1.0.0. Individual track-cover matching was not confirmed. |
| Background radio playback and notification pause/resume | VERIFIED ON PHYSICAL DEVICE | Android MediaSession remained active while browsing/locking; notification pause and resume changed playback state on 1.0.0. |
| Radio filter coverage, stream failure/reconnect, Bluetooth routing | IN PROGRESS | Implemented paths exist; these scenarios are not comprehensively device-tested. |
| Theme and visualizer/GIF options | IMPLEMENTED | Source/UI exists; GIF persistence/combined overlays and both themes were not reverified on 1.0.1. |
| TalkBack | NOT STARTED | No test evidence. |

## Physical-device evidence

Fresh release-candidate captures:

- `device-evidence/release-v1.0.1/home.png`
- `device-evidence/release-v1.0.1/library.png`
- `device-evidence/release-v1.0.1/songs.png`
- `device-evidence/release-v1.0.1/now-playing.png`

Historical Orbit Dial joystick screenshots and recording use the
`younispin-orbit-joystick-` filename prefix under `device-evidence/`. Historical
radio evidence is under `device-evidence/phase4/`. Those artifacts are not
presented as fresh captures from the v1.0.1 run.
