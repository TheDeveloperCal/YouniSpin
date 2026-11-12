package com.youni.younispin.data.repository

import android.content.Context
import com.youni.younispin.data.db.AppDatabase
import com.youni.younispin.data.db.RadioStationEntity
import com.youni.younispin.data.model.RadioStation
import com.youni.younispin.metadata.MatchConfidence
import com.youni.younispin.metadata.MusicBrainzClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.security.MessageDigest

class RadioRepository(context: Context, database: AppDatabase) {
    private val appContext = context.applicationContext
    private val radioDao = database.radioDao()
    private val radioBrowserClient = RadioBrowserClient()
    private val somaFmClient = SomaFmClient()

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStationsFlow().map { list ->
        list.map { it.toModel() }
    }

    val favoriteStations: Flow<List<RadioStation>> = radioDao.getFavoriteStationsFlow().map { list ->
        list.map { it.toModel() }
    }

    val recentStations: Flow<List<RadioStation>> = radioDao.getRecentStationsFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun discover(query: RadioDirectoryQuery): List<RadioStation> = withContext(Dispatchers.IO) {
        val savedStations = radioDao.getStationsBySource(RadioBrowserClient.SOURCE_RADIO_BROWSER)
            .associateBy { it.sourceId }
        radioBrowserClient.search(query).map { station ->
            val saved = station.sourceId?.let(savedStations::get)
            station.copy(
                isFavorite = saved?.isFavorite == true,
                lastPlayed = saved?.lastPlayed ?: 0L
            )
        }
    }

    suspend fun somaFmChannels(): List<RadioStation> = withContext(Dispatchers.IO) {
        somaFmClient.channels().map { channel ->
            val saved = channel.sourceId?.let {
                radioDao.getStationBySourceId(SomaFmClient.SOURCE_SOMAFM, it)
            }
            channel.copy(isFavorite = saved?.isFavorite == true, lastPlayed = saved?.lastPlayed ?: 0L)
        }
    }

    suspend fun resolveStream(station: RadioStation): RadioStation = withContext(Dispatchers.IO) {
        if (station.source == SomaFmClient.SOURCE_SOMAFM) {
            station.copy(url = somaFmClient.resolveStream(station))
        } else {
            station
        }
    }

    suspend fun nowPlaying(channelId: String): List<SomaFmTrack> = withContext(Dispatchers.IO) {
        somaFmClient.nowPlaying(channelId)
    }

    suspend fun resolveArtwork(artist: String, title: String, album: String?): String? =
        withContext(Dispatchers.IO) {
            if (artist.isBlank() || title.isBlank()) return@withContext null
            val key = sha256("${artist.trim().lowercase()}|${title.trim().lowercase()}|${album.orEmpty().trim().lowercase()}")
            val directory = File(appContext.cacheDir, "radio-artwork")
            val cached = File(directory, "$key.jpg")
            if (cached.isFile && cached.length() > 0) return@withContext cached.toURI().toString()

            val matches = MusicBrainzClient.searchTrack(title, artist, album)
            val exactMatch = matches.firstOrNull { match ->
                match.confidence == MatchConfidence.STRONG_MATCH &&
                    normalize(match.title) == normalize(title) &&
                    normalize(match.artist) == normalize(artist) &&
                    (album.isNullOrBlank() || normalize(match.album.orEmpty()) == normalize(album))
            } ?: return@withContext null
            val coverUrl = exactMatch.coverArtUrl ?: return@withContext null
            val bytes = MusicBrainzClient.downloadArtwork(coverUrl)
                ?.takeIf { it.isNotEmpty() && it.size <= MAX_ARTWORK_BYTES }
                ?: return@withContext null
            directory.mkdirs()
            cached.writeBytes(bytes)
            cached.toURI().toString()
        }

    suspend fun addStation(
        name: String,
        url: String,
        genre: String,
        logoUrl: String? = null,
        description: String? = null,
        homepage: String? = null
    ): Long = withContext(Dispatchers.IO) {
            validateHttpUrl(url)
            val entity = RadioStationEntity(
                name = name.trim(),
                url = url.trim(),
                genre = if (genre.isBlank()) "Various" else genre.trim(),
                logoUrl = logoUrl?.trim()?.takeIf(String::isNotBlank),
                description = description?.trim()?.takeIf(String::isNotBlank),
                homepage = homepage?.trim()?.takeIf(String::isNotBlank),
                source = "CUSTOM",
                isCustom = true
            )
            radioDao.insertStation(entity)
        }

    suspend fun updateStation(station: RadioStation) = withContext(Dispatchers.IO) {
        validateHttpUrl(station.url)
        radioDao.updateStation(station.toEntity(isCustom = true))
    }

    suspend fun deleteStation(station: RadioStation) = withContext(Dispatchers.IO) {
        radioDao.deleteStation(station.toEntity(isCustom = station.isCustom))
    }

    suspend fun setFavorite(station: RadioStation, isFav: Boolean) = withContext(Dispatchers.IO) {
        val saved = station.sourceId?.let { radioDao.getStationBySourceId(station.source, it) }
            ?: radioDao.getStationById(station.id)
        if (saved != null) {
            radioDao.updateStation(saved.copy(isFavorite = isFav))
        } else if (isFav) {
            radioDao.insertStation(station.copy(isFavorite = true, id = 0L).toEntity(isCustom = station.isCustom))
        } else {
            return@withContext
        }
    }

    suspend fun recordStationPlayed(station: RadioStation) = withContext(Dispatchers.IO) {
        val saved = station.sourceId?.let { radioDao.getStationBySourceId(station.source, it) }
            ?: radioDao.getStationById(station.id)
        val played = station.copy(
            isFavorite = saved?.isFavorite ?: station.isFavorite,
            lastPlayed = System.currentTimeMillis(),
            id = saved?.id ?: 0L
        )
        radioDao.insertStation(played.toEntity(isCustom = station.isCustom))
    }

    suspend fun exportM3U(destinationFile: File) = withContext(Dispatchers.IO) {
        val stations = radioDao.getAllStationsFlow().first().filter { it.isCustom }.map { it.toModel() }
        destinationFile.bufferedWriter().use { writer ->
            writer.write("#EXTM3U\n")
            stations.forEach { station ->
                writer.write("#EXTINF:-1,${station.name.replace('\n', ' ')}\n")
                writer.write("${station.url}\n")
            }
        }
    }

    suspend fun importM3U(content: String): Int = withContext(Dispatchers.IO) {
        val entries = mutableListOf<Pair<String, String>>()
        var pendingName: String? = null
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) ->
                    pendingName = line.substringAfter(',').trim().takeIf(String::isNotBlank)
                line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) -> {
                    validateHttpUrl(line)
                    entries += (pendingName ?: line.substringAfterLast('/').ifBlank { "Imported station" }) to line
                    pendingName = null
                }
            }
        }
        entries.forEach { (name, url) ->
            radioDao.insertStation(
                RadioStationEntity(name = name, url = url, genre = "Imported", source = "CUSTOM", isCustom = true)
            )
        }
        entries.size
    }

    private fun RadioStationEntity.toModel(): RadioStation {
        return RadioStation(
            id = id,
            name = name,
            url = url,
            genre = genre,
            logoUrl = logoUrl,
            isFavorite = isFavorite,
            lastPlayed = lastPlayed,
            description = description,
            homepage = homepage,
            country = country,
            language = language,
            codec = codec,
            bitrate = bitrate,
            source = source,
            sourceId = sourceId,
            isCustom = isCustom
        )
    }

    private fun RadioStation.toEntity(isCustom: Boolean = this.isCustom) = RadioStationEntity(
        id = id,
        name = name.trim(),
        url = url.trim(),
        genre = genre.trim().ifBlank { "Various" },
        logoUrl = logoUrl,
        isFavorite = isFavorite,
        lastPlayed = lastPlayed,
        description = description,
        homepage = homepage,
        country = country,
        language = language,
        codec = codec,
        bitrate = bitrate,
        source = source,
        sourceId = sourceId,
        isCustom = isCustom
    )

    private fun validateHttpUrl(value: String) {
        val uri = runCatching { URI(value.trim()) }.getOrNull()
        require(uri != null && uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "Radio station URL must be a valid HTTP or HTTPS URL"
        }
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val MAX_ARTWORK_BYTES = 8 * 1024 * 1024
    }
}
