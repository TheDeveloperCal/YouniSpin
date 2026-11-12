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
        album: String? = null
    ): List<MetadataMatch> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MetadataMatch>()
        if (title.isBlank()) return@withContext results

        try {
            enforceRateLimit()

            val queryBuilder = StringBuilder()
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
            queryBuilder.append("recording:\"").append(cleanTitle).append("\"")

            if (!artist.isNullOrBlank() && artist != "Unknown Artist") {
                val cleanArtist = artist.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
                queryBuilder.append(" AND artist:\"").append(cleanArtist).append("\"")
            }

            if (!album.isNullOrBlank() && album != "Unknown Album") {
                val cleanAlbum = album.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
                queryBuilder.append(" AND release:\"").append(cleanAlbum).append("\"")
            }

            val encodedQuery = URLEncoder.encode(queryBuilder.toString(), "UTF-8")
            val url = "https://musicbrainz.org/ws/2/recording?query=$encodedQuery&limit=10&fmt=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "MusicBrainz API returned ${response.code}")
                return@withContext results
            }

            val body = response.body?.string() ?: return@withContext results
            val json = JSONObject(body)
            val recordings = json.optJSONArray("recordings") ?: return@withContext results

            for (i in 0 until recordings.length()) {
                val rec = recordings.getJSONObject(i)
                val recId = rec.optString("id")
                val recTitle = rec.optString("title")
                val score = rec.optInt("score", 50)

                // Artist credit
                var recArtist = "Unknown Artist"
                val artistCredit = rec.optJSONArray("artist-credit")
                if (artistCredit != null && artistCredit.length() > 0) {
                    recArtist = artistCredit.getJSONObject(0).optString("name", "Unknown Artist")
                }

                // A recording may appear on many releases; only expose a release when unambiguous.
                var releaseTitle: String? = null
                var releaseId: String? = null
                var releaseDate: String? = null
                var trackNum: Int? = null

                val releases = rec.optJSONArray("releases")
                if (releases != null && releases.length() > 0) {
                    val releaseObjects = (0 until releases.length()).map { releases.getJSONObject(it) }
                    val selectedRelease = if (!album.isNullOrBlank() && album != "Unknown Album") {
                        val normalizedAlbum = normalizeForComparison(album)
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
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}")
        }

        return@withContext results
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
