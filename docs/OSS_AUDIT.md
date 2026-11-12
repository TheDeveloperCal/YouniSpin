# YouniSpin — Open Source Research & Licensing Audit
**Document Date:** November 2026
**Project:** YouniSpin Android Music Player (`com.youni.younispin`)
**Project license decision:** The existing audit records an explicit preference
for permissive distribution. This repository's original YouniSpin code is
licensed under the MIT License (`LICENSE`). Third-party dependencies and
artwork remain under their own respective licenses; the project license does
not relicense those materials. Review dependency notices before each release.

---

## 1. Executive Summary & Licensing Strategy

In accordance with project guidelines, YouniSpin is developed as an original, clean-room native Android application. It does not copy, fork, or incorporate proprietary code or copyleft GPL code into its core codebase without license adherence.

To achieve maximum stability, modern architecture, and permissive commercial/non-commercial distribution flexibility, YouniSpin adopts:
- **Kotlin** and **Jetpack Compose (Material 3)**.
- **AndroidX Media3 (ExoPlayer)** under the **Apache License 2.0**.
- **AndroidX Room, DataStore, Lifecycle, Navigation** under **Apache License 2.0**.
- **Coil Image Loader** under **Apache License 2.0**.
- **OkHttp 3** under **Apache License 2.0**.
- Clean-room custom implementations for the **Orbit Dial**, UI components, Audio Visualizer, LRC parser, ID3/Media metadata extraction, and MusicBrainz/Cover Art Archive integration.

Below is the required comprehensive audit of the six referenced open-source projects.

---

## 2. Research Audit Matrix

### 2.1 Auxio
- **Repository:** https://github.com/OxygenCobalt/Auxio
- **Author:** Alexander Capehart (OxygenCobalt)
- **License:** **GNU General Public License v3.0 (GPLv3)**
- **What Was Studied:**
  - Media3 / ExoPlayer integration patterns for purely local audio.
  - Efficient MediaStore querying and grouping tracks into albums and multi-disc sets.
  - Gapless playback handling and audio focus change listener strategies.
  - State persistence for queue and playback position.
- **What Can Be Reused:**
  - Architectural insights: Utilizing `MediaSessionService` with ExoPlayer as the single source of truth for audio playback.
  - Query concepts for filtering non-music media (duration > 5s, `IS_MUSIC != 0`).
- **What Cannot Be Reused:**
  - No source code or XML/Compose layouts can be directly copied, because GPLv3 would legally compel YouniSpin to adopt GPLv3 for all linked source files.
- **Attribution & Compliance:**
  - GPLv3 forbids proprietary redistribution and requires full source release under GPLv3 if code is incorporated. Auxio served strictly as a conceptual and architectural reference.

---

### 2.2 Fossify Music Player
- **Repository:** https://github.com/FossifyOrg/Music-Player
- **Author:** Fossify Org (continuation of Simple Mobile Tools)
- **License:** **GNU General Public License v3.0 (GPLv3)**
- **What Was Studied:**
  - Scoped storage workflows on modern Android (Android 11–15).
  - Storage Access Framework (SAF) URI permission handling for file tag editing.
  - Local playlist export/import in M3U format.
  - Sleep timer implementation patterns.
- **What Can Be Reused:**
  - Workflow understanding: Safely persisting metadata overrides in a local database when direct file tag writing is prevented by Scoped Storage restrictions.
  - Intent structures for Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- **What Cannot Be Reused:**
  - Direct source code or assets. GPLv3 copyleft restriction.
- **Attribution & Compliance:**
  - Kept strictly as conceptual study. No Fossify code or assets are present in YouniSpin.

---

### 2.3 Retro Music Player
- **Repository:** https://github.com/RetroMusicPlayer/RetroMusicPlayer
- **Author:** Hemanth Savarala
- **License:** **GNU General Public License v3.0 (GPLv3)**
- **What Was Studied:**
  - Card-based album presentation, color palette generation from album art, and typography.
  - Tabbed library navigation (Songs, Albums, Artists, Playlists, Genres).
  - Purely local audio workflow.
- **What Can Be Reused:**
  - Visual inspiration for high-contrast dark surfaces and smooth transitions between browse and now-playing views.
- **What Cannot Be Reused:**
  - All source code, artwork, icons, and proprietary design layouts.
- **Attribution & Compliance:**
  - No code or graphics were copied. YouniSpin uses its own distinct design language with the industrial Orbit Dial tactile controller.

---

### 2.4 Vinyl Music Player
- **Repository:** https://github.com/VinylMusicPlayer/VinylMusicPlayer
- **Author:** Adriel Cafe
- **License:** **GNU General Public License v3.0 (GPLv3)**
- **What Was Studied:**
  - Traditional Android MediaStore content observer pattern.
  - Handling of compilation albums and multi-artist separator tags (`/`, `;`, `feat.`).
- **What Can Be Reused:**
  - Conceptual understanding of artist split parsing and sorting rules.
- **What Cannot Be Reused:**
  - Source code and layouts (GPLv3).
- **Attribution & Compliance:**
  - No code copied.

---

### 2.5 VLC for Android
- **Repository:** https://github.com/videolan/vlc-android
- **Author:** VideoLAN
- **License:** **GNU General Public License v3.0 / LGPLv2.1**
- **What Was Studied:**
  - LibVLC JNI bindings and network streaming capabilities (Icecast, Shoutcast, RTSP, SMB/SFTP).
  - Network buffer management and ICY metadata reading over network streams.
- **What Can Be Reused:**
  - Architectural understanding of modular playback engines where an abstract `PlaybackEngine` interface can delegate between standard ExoPlayer for local/HTTP streams and LibVLC when specialized network protocols (SMB/RTSP) are requested.
  - LibVLC JNI binaries (`libvlc-all`) are large (typically 40MB+ per ABI). For YouniSpin's fast, responsive, and rock-solid core on Android 15, AndroidX Media3 handles MP3, FLAC, AAC, WAV, OGG, Opus, HLS, DASH, and ICY streams natively with zero native crash risk. YouniSpin implements a clean `AudioEngine` abstraction that can host LibVLC as an optional modular provider.
- **Attribution & Compliance:**
  - LibVLC is LGPLv2.1; if bundled as an `.so`, dynamic linking requirements must be met. Media3 is selected as the default primary engine.

---

### 2.6 AndroidX Media3
- **Repository / Docs:** https://developer.android.com/media/media3
- **Author:** Google LLC
- **License:** **Apache License 2.0**
- **What Was Studied:**
  - Official Android playback standard (`androidx.media3:media3-exoplayer`, `androidx.media3:media3-session`).
  - Native integration with Android `MediaSession`, lock-screen media controls, Bluetooth AVRCP headset controls, and notification channels.
  - Built-in support for gapless playback, audio offload, audio focus management (`AUDIOFOCUS_GAIN`), and streaming (HLS, DASH, HTTP progressive audio, ICY stream metadata).
- **What Can Be Reused:**
  - Official Google binary dependencies under Apache 2.0. Completely permissive for commercial and open-source applications with attribution.
- **Attribution & Compliance:**
  - Included under Apache 2.0 license notice in app settings and documentation.

---

## 3. Selected Dependencies for YouniSpin

All runtime dependencies incorporated into YouniSpin are permissively licensed (Apache 2.0 or MIT):

| Library | Version | License | Role |
|---------|---------|---------|------|
| `androidx.media3:media3-exoplayer` | 1.5.1 | Apache 2.0 | Audio playback, gapless, decoders |
| `androidx.media3:media3-session` | 1.5.1 | Apache 2.0 | Android MediaSession & Foreground Service |
| `androidx.media3:media3-exoplayer-hls` | 1.5.1 | Apache 2.0 | HLS stream playback (`.m3u8`) |
| `androidx.media3:media3-exoplayer-dash` | 1.5.1 | Apache 2.0 | DASH audio streaming |
| `androidx.compose.*` | 2024.10+ | Apache 2.0 | Modern declarative UI and animations |
| `androidx.room:*` | 2.6.1 | Apache 2.0 | Local SQLite database for library, history, overrides |
| `io.coil-kt:coil-compose` | 2.7.0 | Apache 2.0 | Fast, cached image loading for album covers |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | Apache 2.0 | MusicBrainz & Cover Art Archive REST client |
| `androidx.core:core-ktx` | 1.15.0 | Apache 2.0 | Android KTX extensions |
| `androidx.activity:activity-compose`| 1.10.1 | Apache 2.0 | Activity and Photo Picker contracts |

---

## 4. Conclusion

YouniSpin maintains 100% clean-room IP independence. No copyrighted code, trade dress, or assets from Apple, Auxio, Fossify, Retro, or Vinyl are copied. YouniSpin's Orbit Dial, layout, typography, database schema, and branding are entirely original.
