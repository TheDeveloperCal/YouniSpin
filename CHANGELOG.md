# Changelog

Notable user-facing changes are recorded here. Dates refer to the release
candidate preparation and do not imply that every listed subsystem has
completed independent physical-device verification; see the linked test
reports.

## 1.0.2 — 2026-11-22 (evaluation prerelease; not a stable release)

- Navigation now opens as a temporary, full-width menu overlay on destinations;
  selecting a destination dismisses it, and Android Back closes the menu or
  returns to the prior destination.
- Equalizer controls share the playback service's audio-effects manager so
  band and enhancement settings operate on the active playback session.
- Radio Live displays Groove Salad and Drone Zone from its SomaFM fallback when
  the catalogue is unreachable. Listening still requires an internet
  connection.
- Playback notifications show current artwork, title, artist, and transport
  controls.
- Added three bundled selectable GIF backgrounds. GIF imports now stream to a
  bounded temporary file instead of buffering the complete file in memory.
- Metadata lookups now split artist/title information from filenames and retry
  MusicBrainz searches without an album constraint when needed. Exact-match
  confidence rules remain in place.
- Candidate is debug-signed and is not a stable production release. See
  `docs/RELEASE_NOTES_1.0.2.md` and `docs/TEST_RESULTS.md` for verification
  limits.

## 1.0.1 — 2026-11

- Added a per-song **Scan Online for Metadata** action to start a user-requested
  MusicBrainz search from Now Playing and the song action menu.
- Updated the classic display layout with a small top inset, localized clock,
  weather symbol, and removal of the small lower bezel wordmark.
- Display the application version and build number from build configuration.
- Updated metadata and radio service User-Agent strings for the release
  version and project repository.
- Prepared a debug/testing APK; this is not a production-signed release.

## 1.0.0 — development baseline

- Established the native Android music player, classic framed display and
  Orbit Dial controls, local music library, Media3 playback, Radio Live,
  appearance customization, and metadata tooling.
- Earlier physical-device tests and known gaps are recorded in
  [docs/TEST_RESULTS.md](docs/TEST_RESULTS.md) and
  [docs/FUNCTIONALITY_MATRIX.md](docs/FUNCTIONALITY_MATRIX.md).
