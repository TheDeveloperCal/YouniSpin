package com.youni.younispin.metadata

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MetadataMatch(
    val id: String,
    val score: Int,
    val title: String,
    val artist: String,
    val album: String?,
    val releaseId: String?,
    val date: String?,
    val trackNumber: Int?,
    val coverArtUrl: String?,
    val confidence: MatchConfidence
)

enum class MatchConfidence {
    STRONG_MATCH,
    POSSIBLE_MATCH,
    MULTIPLE_MATCHES,
    LOW_MATCH
}

object MusicBrainzClient {
    private const val TAG = "MusicBrainzClient"
    private const val USER_AGENT = "YouniSpin/1.0.1 (https://github.com/TheDeveloperCal/YouniSpin)"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val rateLimiterMutex = Mutex()
    private var lastRequestTime = 0L

    private suspend fun enforceRateLimit() {
        rateLimiterMutex.withLock {
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastRequestTime
            if (timeSinceLast < 1100) {
                delay(1100 - timeSinceLast)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    suspend fun searchTrack(
        title: String,
        artist: String?,
        album: String? = null,
        filePath: String? = null
    ): List<MetadataMatch> = withContext(Dispatchers.IO) {
        val query = MetadataQueryBuilder.fromSongFields(title, artist, album, filePath)
        if (query.title.isBlank()) {
            Log.i(TAG, "Skipping MusicBrainz search: empty title")
            return@withContext emptyList()
        }

        val attempts = buildList {
            add(MetadataQueryBuilder.luceneQuery(query, quoted = true, includeAlbum = query.album != null))
            if (query.album != null) {
                add(MetadataQueryBuilder.luceneQuery(query, quoted = true, includeAlbum = false))
            }
            add(MetadataQueryBuilder.luceneQuery(query, quoted = false, includeAlbum = false))
        }.distinct()

        var lastError: Exception? = null
        for ((index, lucene) in attempts.withIndex()) {
            try {
                val matches = executeSearch(lucene, query.album)
                Log.i(
                    TAG,
                    "MusicBrainz attempt ${index + 1}/${attempts.size} source=${query.source} " +
                        "query=[$lucene] results=${matches.size}"
                )
                if (matches.isNotEmpty()) return@withContext matches
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "MusicBrainz attempt ${index + 1} failed for [$lucene]: ${e.message}")
            }
        }
        if (lastError != null) throw lastError
        emptyList()
    }

    private suspend fun executeSearch(lucene: String, albumHint: String?): List<MetadataMatch> {
        val results = mutableListOf<MetadataMatch>()
        enforceRateLimit()

        val encodedQuery = URLEncoder.encode(lucene, "UTF-8")
        val url = "https://musicbrainz.org/ws/2/recording?query=$encodedQuery&limit=10&fmt=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "MusicBrainz HTTP ${response.code} for $url body=${body.take(180)}")
                throw IllegalStateException("MusicBrainz returned HTTP ${response.code}")
            }

            val json = JSONObject(body)
            val recordings = json.optJSONArray("recordings")
            Log.i(
                TAG,
                "MusicBrainz HTTP ${response.code} count=${json.optInt("count", 0)} " +
                    "parsed=${recordings?.length() ?: 0}"
            )
            if (recordings == null) return emptyList()

            for (i in 0 until recordings.length()) {
                val rec = recordings.getJSONObject(i)
                val recId = rec.optString("id")
                val recTitle = rec.optString("title")
                val score = rec.optInt("score", 50)

                var recArtist = "Unknown Artist"
                val artistCredit = rec.optJSONArray("artist-credit")
                if (artistCredit != null && artistCredit.length() > 0) {
                    recArtist = artistCredit.getJSONObject(0).optString("name", "Unknown Artist")
                }

                var releaseTitle: String? = null
                var releaseId: String? = null
                var releaseDate: String? = null
                var trackNum: Int? = null

                val releases = rec.optJSONArray("releases")
                if (releases != null && releases.length() > 0) {
                    val releaseObjects = (0 until releases.length()).map { releases.getJSONObject(it) }
                    val selectedRelease = if (!albumHint.isNullOrBlank() && albumHint != "Unknown Album") {
                        val normalizedAlbum = normalizeForComparison(albumHint)
                        releaseObjects.filter {
                            normalizeForComparison(it.optString("title")) == normalizedAlbum
                        }.singleOrNull()
                    } else {
                        releaseObjects.singleOrNull()
                    }
                    releaseTitle = selectedRelease?.optString("title")
                    releaseId = selectedRelease?.optString("id")
                    releaseDate = selectedRelease?.optString("date")

                    val media = selectedRelease?.optJSONArray("media")
                    if (media != null && media.length() > 0) {
                        val trackObj = media.getJSONObject(0).optJSONArray("track")
                        if (trackObj != null && trackObj.length() > 0) {
                            trackNum = trackObj.getJSONObject(0).optInt("number", 1)
                        }
                    }
                }

                val coverUrl = if (!releaseId.isNullOrBlank()) {
                    "https://coverartarchive.org/release/$releaseId/front-500"
                } else null

                results.add(
                    MetadataMatch(
                        id = recId,
                        score = score,
                        title = recTitle,
                        artist = recArtist,
                        album = releaseTitle,
                        releaseId = releaseId,
                        date = releaseDate,
                        trackNumber = trackNum,
                        coverArtUrl = coverUrl,
                        confidence = MatchConfidence.LOW_MATCH
                    )
                )
            }
        }

        val topScore = results.maxOfOrNull { it.score }
        val similarlyScoredCount = results.count { topScore != null && it.score >= topScore - 5 }
        for (index in results.indices) {
            val match = results[index]
            val confidence = when {
                match.score >= 90 && similarlyScoredCount == 1 -> MatchConfidence.STRONG_MATCH
                match.score >= 75 && similarlyScoredCount == 1 -> MatchConfidence.POSSIBLE_MATCH
                match.score >= 75 && similarlyScoredCount > 1 -> MatchConfidence.MULTIPLE_MATCHES
                else -> MatchConfidence.LOW_MATCH
            }
            results[index] = match.copy(confidence = confidence)
        }
        return results
    }

    suspend fun downloadArtwork(coverUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(coverUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext response.body?.bytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading artwork from $coverUrl: ${e.message}")
        }
        return@withContext null
    }

    private fun normalizeForComparison(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
