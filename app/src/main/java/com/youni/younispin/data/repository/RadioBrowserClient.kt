package com.youni.younispin.data.repository

import com.youni.younispin.data.model.RadioStation
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

data class RadioDirectoryQuery(
    val name: String = "",
    val genre: String = "",
    val country: String = "",
    val language: String = "",
    val codec: String = "",
    val minimumBitrate: Int? = null,
    val order: RadioSortOrder = RadioSortOrder.POPULARITY
)

enum class RadioSortOrder { POPULARITY, NAME }

class RadioBrowserClient(
    private val client: OkHttpClient = sharedClient
) {
    private data class CachedStations(val expiresAt: Long, val stations: List<RadioStation>)

    private val cache = LinkedHashMap<String, CachedStations>()

    @Synchronized
    private fun cached(key: String): List<RadioStation>? =
        cache[key]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.stations

    @Synchronized
    private fun remember(key: String, stations: List<RadioStation>) {
        cache[key] = CachedStations(System.currentTimeMillis() + CACHE_TTL_MS, stations)
        while (cache.size > MAX_CACHE_ENTRIES) cache.remove(cache.keys.first())
    }

    @Throws(IOException::class)
    fun search(query: RadioDirectoryQuery, limit: Int = 60): List<RadioStation> {
        val key = listOf(
            query.name.trim().lowercase(),
            query.genre.trim().lowercase(),
            query.country.trim().lowercase(),
            query.language.trim().lowercase(),
            query.codec.trim().lowercase(),
            query.minimumBitrate,
            query.order,
            limit
        ).joinToString("|")
        cached(key)?.let { return it }

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("all.api.radio-browser.info")
            .addPathSegments("json/stations/search")
            .apply {
                if (query.name.isNotBlank()) addQueryParameter("name", query.name.trim())
                if (query.genre.isNotBlank()) addQueryParameter("tag", query.genre.trim())
                if (query.country.isNotBlank()) addQueryParameter("country", query.country.trim())
                if (query.language.isNotBlank()) addQueryParameter("language", query.language.trim())
                if (query.codec.isNotBlank()) addQueryParameter("codec", query.codec.trim())
                query.minimumBitrate?.let { addQueryParameter("bitrateMin", it.coerceAtLeast(0).toString()) }
                addQueryParameter("hidebroken", "true")
                addQueryParameter("lastcheckok", "1")
                addQueryParameter(
                    "order",
                    if (query.order == RadioSortOrder.NAME) "name" else "clickcount"
                )
                addQueryParameter("reverse", if (query.order == RadioSortOrder.NAME) "false" else "true")
                addQueryParameter("limit", limit.coerceIn(1, 100).toString())
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Radio Browser returned HTTP ${response.code}")
            val array = JSONArray(response.body?.string() ?: "[]")
            val stations = buildList {
                for (i in 0 until array.length()) {
                    val json = array.optJSONObject(i) ?: continue
                    if (json.optInt("lastcheckok", 0) != 1) continue
                    val name = json.optString("name").trim()
                    val streamUrl = json.optString("url_resolved").ifBlank { json.optString("url") }.trim()
                    val uuid = json.optString("stationuuid").trim()
                    if (name.isBlank() || streamUrl.isBlank() || uuid.isBlank()) continue
                    add(
                        RadioStation(
                            id = stableId(uuid),
                            name = name,
                            url = streamUrl,
                            genre = json.optString("tags")
                                .split(',')
                                .map(String::trim)
                                .filter(String::isNotBlank)
                                .take(3)
                                .joinToString(" · ")
                                .ifBlank { "Various" },
                            logoUrl = json.optString("favicon").takeIf { it.startsWith("https://") || it.startsWith("http://") },
                            description = null,
                            homepage = json.optString("homepage").takeIf(String::isNotBlank),
                            country = json.optString("country").takeIf(String::isNotBlank),
                            language = json.optString("language").takeIf(String::isNotBlank),
                            codec = json.optString("codec").takeIf(String::isNotBlank),
                            bitrate = json.optInt("bitrate", 0),
                            source = SOURCE_RADIO_BROWSER,
                            sourceId = uuid,
                            isCustom = false
                        )
                    )
                }
            }
            remember(key, stations)
            return stations
        }
    }

    companion object {
        const val SOURCE_RADIO_BROWSER = "RADIO_BROWSER"
        private const val USER_AGENT = "YouniSpin/1.0.1 (https://github.com/TheDeveloperCal/YouniSpin)"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 24
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        internal fun stableId(value: String): Long = value.hashCode().toLong() and 0x7fffffffL
    }
}
