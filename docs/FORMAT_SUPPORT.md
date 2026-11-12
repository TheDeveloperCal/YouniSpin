# YouniSpin — Audio Format Compatibility Matrix

**Device Tested:** BLU B1660V
**OS Version:** Android 15 (API Level 35)
**Primary Engine:** AndroidX Media3 / ExoPlayer 1.5.1
**Architecture:** arm64-v8a

---

## 1. Format & Container Matrix

| Format / Codec | Common Extensions | Container | Engine | Decoding Mode | Real Device Tested | Status / Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MP3** (MPEG-1/2 Audio Layer III) | `.mp3` | ID3v1/ID3v2 in MP3 frame stream | Media3 ExoPlayer | Hardware / MediaCodec | Yes | Fully supported; ID3v2.3/ID3v2.4 tags and embedded APIC artwork parsed |
| **FLAC** (Free Lossless Audio Codec) | `.flac` | Native FLAC container | Media3 ExoPlayer | MediaCodec (Native FLAC extractor) | Yes | Bit-perfect lossless 16-bit / 24-bit up to 192kHz; Vorbis comment metadata |
| **AAC** (Advanced Audio Coding) | `.aac`, `.m4a` | ADTS / MPEG-4 ISO container | Media3 ExoPlayer | Hardware / MediaCodec | Yes | LC-AAC and HE-AAC supported; iTunes atoms metadata parsed |
| **M4A / ALAC** (Apple Lossless) | `.m4a` | MPEG-4 container (`alac` atom) | Media3 ExoPlayer | MediaCodec / Software fallback | Yes | Plays native ALAC in MP4 container on Android 10+ |
| **WAV** (Linear PCM) | `.wav` | RIFF | Media3 ExoPlayer | Uncompressed PCM | Yes | Standard 16-bit / 24-bit 44.1kHz / 48kHz audio streams |
| **OGG Vorbis** | `.ogg` | Ogg container | Media3 ExoPlayer | MediaCodec | Yes | Vorbis comments parsed; gapless playback supported |
| **Opus** | `.opus`, `.ogg` | Ogg container | Media3 ExoPlayer | MediaCodec (Native Android 10+) | Yes | High efficiency modern voice & music codec |
| **AIFF / AIF** | `.aif`, `.aiff` | IFF container | Media3 ExoPlayer | PCM / Extractor | Yes | Uncompressed audio |
| **HLS Stream** | `.m3u8` | HTTP Live Streaming (TS / fMP4) | Media3 HLS module | Dynamic network chunks | Yes | Adaptive bitrate streaming |
| **DASH Stream** | `.mpd` | Dynamic Adaptive Streaming | Media3 DASH module | Dynamic network chunks | Yes | Supported via Media3 DASH extension |
| **Icecast / Shoutcast** | Direct stream URL | HTTP stream with ICY header | Media3 OkHttp / DefaultDataSource | Stream decoder | Yes | ICY station and track title parsed via `MetadataOutput` |

---

## 2. Playback Architecture & Engine Abstraction

YouniSpin utilizes a clean `AudioEngine` abstraction:
1. **`DefaultPlaybackEngine` (Media3/ExoPlayer):** Primary rock-solid, hardware-accelerated playback pipeline. Fully integrated with Android `MediaSessionService`, system notifications, lock-screen controls, Bluetooth AVRCP controls, and Android 15 edge-to-edge / audio focus handling.
2. **Stream Demuxer & ICY Handler:** Real-time extraction of live radio track info ("Artist - Title" metadata) from Icecast / Shoutcast servers.
3. **Hardware Equalizer:** Android `android.media.audiofx.Equalizer` bound directly to the active `audioSessionId`, ensuring zero CPU overhead DSP audio processing.
