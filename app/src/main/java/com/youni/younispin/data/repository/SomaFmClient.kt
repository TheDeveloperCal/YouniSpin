package com.youni.younispin.data.repository

import com.youni.younispin.data.model.RadioStation
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SomaFmTrack(
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val playedAtEpochSeconds: Long?
)

class SomaFmClient(
    private val client: OkHttpClient = sharedClient
) {
    @Volatile
    private var channelCache: Pair<Long, List<RadioStation>>? = null

    fun channels(): List<RadioStation> {
        channelCache?.takeIf { it.first > System.currentTimeMillis() }?.let { return it.second }
        val request = Request.Builder()
            .url(CHANNELS_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("SomaFM catalogue returned HTTP ${response.code}")
            val json = JSONObject(response.body?.string() ?: throw IOException("Empty SomaFM catalogue"))
            val channels = json.optJSONArray("channels") ?: throw IOException("Invalid SomaFM catalogue")
            val stations = buildList {
                for (i in 0 until channels.length()) {
                    val channel = channels.optJSONObject(i) ?: continue
                    val id = channel.optString("id").trim()
                    val title = channel.optString("title").trim()
                    if (id.isBlank() || title.isBlank()) continue
                    val playlists = channel.optJSONArray("playlists")
                    val selectedPlaylist = playlists?.let { list ->
                        (0 until list.length())
                            .mapNotNull { list.optJSONObject(it) }
                            .firstOrNull { it.optString("format").equals("mp3", true) }
                            ?: (0 until list.length())
                                .mapNotNull { list.optJSONObject(it) }
                                .firstOrNull { it.optString("format").equals("aac", true) }
                    }
                    val playlistUrl = selectedPlaylist?.optString("url")?.takeIf(String::isNotBlank)
                        ?: continue
                    add(
                        RadioStation(
                            id = RadioBrowserClient.stableId("somafm:$id"),
                            name = title,
                            url = playlistUrl,
                            genre = channel.optString("genre").replace('|', '·').ifBlank { "Various" },
                            logoUrl = channel.optString("xlimage").takeIf(String::isNotBlank)
                                ?: channel.optString("largeimage").takeIf(String::isNotBlank)
                                ?: channel.optString("image").takeIf(String::isNotBlank),
                            description = channel.optString("description").takeIf(String::isNotBlank),
                            homepage = "https://somafm.com/$id/",
                            codec = selectedPlaylist.optString("format").uppercase(),
                            bitrate = bitrateFromPlaylist(playlistUrl),
                            source = SOURCE_SOMAFM,
                            sourceId = id,
                            isCustom = false
                        )
                    )
                }
            }
            channelCache = System.currentTimeMillis() + CHANNEL_CACHE_MS to stations
            return stations
        }
    }

    fun resolveStream(station: RadioStation): String {
        require(station.source == SOURCE_SOMAFM) { "Station is not a SomaFM channel" }
        val request = Request.Builder()
            .url(station.url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "audio/x-scpls, text/plain, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("SomaFM playlist returned HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val file = Regex("""(?im)^\s*File\d+\s*=\s*(https?://\S+)\s*$""")
                .find(body)?.groupValues?.get(1)
            if (file != null) return file
            if (body.trim().startsWith("http://") || body.trim().startsWith("https://")) return body.trim()
            throw IOException("SomaFM returned no playable stream URL")
        }
    }

    fun nowPlaying(channelId: String): List<SomaFmTrack> {
        require(channelId.matches(Regex("[a-z0-9]+"))) { "Invalid SomaFM channel id" }
        val request = Request.Builder()
            .url("https://somafm.com/songs/$channelId.json")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("SomaFM now-playing returned HTTP ${response.code}")
            val songs = JSONObject(response.body?.string() ?: "{}").optJSONArray("songs")
                ?: return emptyList()
            return buildList {
                for (i in 0 until songs.length()) {
                    val song = songs.optJSONObject(i) ?: continue
                    val title = song.optString("title").trim()
                    val artist = song.optString("artist").trim()
                    if (title.isBlank() || artist.isBlank()) continue
                    add(
                        SomaFmTrack(
                            title = title,
                            artist = artist,
                            album = song.optString("album").takeIf(String::isNotBlank),
                            artworkUrl = song.optString("albumArt").takeIf(String::isNotBlank),
                            playedAtEpochSeconds = song.optString("date").toLongOrNull()
                        )
                    )
                }
            }
        }
    }

    private fun bitrateFromPlaylist(url: String): Int =
        Regex("""-(\d{2,3})(?:-(?:mp3|aac))?(?:\.pls)?(?:\?.*)?$""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    companion object {
        const val SOURCE_SOMAFM = "SOMAFM"
        private const val CHANNELS_URL = "https://somafm.com/channels.json"
        private const val USER_AGENT = "YouniSpin/1.0.1 (https://github.com/TheDeveloperCal/YouniSpin)"
        private const val CHANNEL_CACHE_MS = 6 * 60 * 60 * 1000L
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
