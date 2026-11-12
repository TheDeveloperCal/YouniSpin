package com.youni.younispin.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.youni.younispin.data.db.*
import com.youni.younispin.data.model.*
import com.youni.younispin.metadata.ArtworkManager
import com.youni.younispin.metadata.MetadataMatchResolver
import com.youni.younispin.metadata.MusicBrainzClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val metadataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var metadataLookupJob: Job? = null
    private val attemptedMetadataLookups = ConcurrentHashMap.newKeySet<String>()
    private val trackOverrideDao = database.trackOverrideDao()
    private val favoriteDao = database.favoriteDao()
    private val playlistDao = database.playlistDao()
    private val historyDao = database.historyDao()

    private val _songsFlow = MutableStateFlow<List<Song>>(emptyList())
    val songsFlow: StateFlow<List<Song>> = _songsFlow.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        // Observe overrides and favorites changes to refresh memory cache
        combine(
            trackOverrideDao.getAllOverridesFlow(),
            favoriteDao.getFavoritePathsFlow()
        ) { overrides, favorites ->
            val favSet = favorites.toSet()
            val overrideMap = overrides.associateBy { it.trackPath }
            val current = _songsFlow.value
            if (current.isNotEmpty()) {
                _songsFlow.value = current.map { song ->
                    val ov = overrideMap[song.path]
                    val isFav = favSet.contains(song.path)
                    applyOverride(song, ov, isFav)
                }
            }
        }.launchIn(kotlinx.coroutines.CoroutineScope(Dispatchers.IO))
    }

    private fun applyOverride(song: Song, ov: TrackOverrideEntity?, isFav: Boolean): Song {
        if (ov == null) {
            return song.copy(isFavorite = isFav)
        }
        return song.copy(
            title = ov.title ?: song.title,
            artist = ov.artist ?: song.artist,
            album = ov.album ?: song.album,
            albumArtist = ov.albumArtist ?: song.albumArtist,
            genre = ov.genre ?: song.genre,
            year = ov.year ?: song.year,
            trackNumber = ov.trackNumber ?: song.trackNumber,
            discNumber = ov.discNumber ?: song.discNumber,
            composer = ov.composer ?: song.composer,
            comment = ov.comment ?: song.comment,
            customArtworkPath = ov.customArtworkPath ?: song.customArtworkPath,
            isFavorite = isFav
        )
    }

    suspend fun scanLocalMusic() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        val songs = mutableListOf<Song>()
        val overrides = trackOverrideDao.getAllOverrides().associateBy { it.trackPath }
        val favorites = favoriteDao.getFavoritePaths().toSet()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: ""
                    val rawArtist = cursor.getString(artistCol) ?: ""
                    val rawAlbum = cursor.getString(albumCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val artistId = cursor.getLong(artistIdCol)
                    val year = cursor.getInt(yearCol)
                    val rawTrack = cursor.getInt(trackCol)
                    val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    // Track number parsing (often contains disc number * 1000)
                    val trackNum = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack
                    val discNum = if (rawTrack >= 1000) rawTrack / 1000 else 1

                    val baseSong = Song(
                        id = id,
                        title = rawTitle,
                        artist = rawArtist,
                        album = rawAlbum,
                        duration = duration,
                        path = path,
                        uriString = uri.toString(),
                        albumId = albumId,
                        artistId = artistId,
                        year = year,
                        trackNumber = trackNum,
                        discNumber = discNum,
                        mimeType = mimeType,
                        sizeBytes = size,
                        dateAdded = dateAdded
                    )

                    val ov = overrides[path]
                    val isFav = favorites.contains(path)
                    songs.add(applyOverride(baseSong, ov, isFav))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore", e)
        }

        _songsFlow.value = songs
        _isScanning.value = false
        metadataLookupJob?.cancel()
        metadataLookupJob = metadataScope.launch {
            songs.asSequence()
                .filter(::isMetadataLookupCandidate)
                .filter { attemptedMetadataLookups.add(it.path) }
                .forEach { song ->
                    try {
                        if (autoResolveMetadata(song)) {
                            val override = trackOverrideDao.getOverrideForPath(song.path)
                            _songsFlow.update { current ->
                                current.map { entry ->
                                    if (entry.path == song.path) {
                                        applyOverride(entry, override, entry.isFavorite)
                                    } else {
                                        entry
                                    }
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("MusicRepository", "Metadata lookup failed for ${song.path}", error)
                    }
                }
        }
    }

    private fun isMetadataMissing(song: Song): Boolean {
        val missingTitle = song.title.isBlank() || song.title.equals("Unknown Track", ignoreCase = true)
        val missingArtist = song.artist.isBlank() || song.artist.equals("Unknown Artist", ignoreCase = true) || song.artist.equals("<unknown>", ignoreCase = true)
        val missingAlbum = song.album.isBlank() || song.album.equals("Unknown Album", ignoreCase = true) || song.album.equals("<unknown>", ignoreCase = true)
        return missingTitle || missingArtist || missingAlbum
    }

    private fun isMetadataLookupCandidate(song: Song): Boolean {
        val title = song.title.trim()
        val artist = song.artist.trim()
        val hasSearchableTitle = title.isNotEmpty() &&
            !title.equals("Unknown Track", ignoreCase = true) &&
            !title.equals("<unknown>", ignoreCase = true)
        val hasSearchableArtist = artist.isNotEmpty() &&
            !artist.equals("Unknown Artist", ignoreCase = true) &&
            !artist.equals("<unknown>", ignoreCase = true)
        return isMetadataMissing(song) && hasSearchableTitle && hasSearchableArtist && song.path.isNotBlank()
    }

    suspend fun autoResolveMetadata(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (song.path.isBlank()) return@withContext false
        if (!isMetadataMissing(song)) return@withContext false
        if (!isMetadataLookupCandidate(song)) return@withContext false

        val matches = MusicBrainzClient.searchTrack(song.title, song.artist, song.album)
        val candidate = MetadataMatchResolver.chooseBestMatch(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.duration,
            candidates = matches
        ) ?: return@withContext false

        if (!MetadataMatchResolver.canAutoApply(song.title, song.artist, song.album, candidate)) {
            return@withContext false
        }

        val existing = trackOverrideDao.getOverrideForPath(song.path)
        var artworkPath: String? = existing?.customArtworkPath

        if (!candidate.coverArtUrl.isNullOrBlank() &&
            song.albumId <= 0L &&
            isMissingValue(song.album) &&
            existing?.customArtworkPath.isNullOrBlank()
        ) {
            val artworkBytes = MusicBrainzClient.downloadArtwork(candidate.coverArtUrl)
            if (artworkBytes != null) {
                artworkPath = ArtworkManager.saveArtworkBytes(
                    context,
                    artworkBytes,
                    "mb_${song.path.hashCode().coerceAtLeast(0)}"
                ) ?: artworkPath
            }
        }

        val resolvedOverride = (existing ?: TrackOverrideEntity(trackPath = song.path)).copy(
            title = existing?.title ?: if (isMissingValue(song.title)) candidate.title else null,
            artist = existing?.artist ?: if (isMissingValue(song.artist)) candidate.artist else null,
            album = existing?.album ?: if (isMissingValue(song.album)) candidate.album else null,
            year = existing?.year ?: candidate.date?.take(4)?.toIntOrNull()
                .takeIf { song.year <= 0 },
            trackNumber = existing?.trackNumber ?: candidate.trackNumber
                .takeIf { song.trackNumber <= 0 },
            customArtworkPath = artworkPath,
            lastUpdated = System.currentTimeMillis()
        )

        trackOverrideDao.saveOverride(resolvedOverride)
        true
    }

    private fun isMissingValue(value: String): Boolean =
        value.isBlank() || value.equals("<unknown>", ignoreCase = true) ||
            value.equals("Unknown Track", ignoreCase = true) ||
            value.equals("Unknown Artist", ignoreCase = true) ||
            value.equals("Unknown Album", ignoreCase = true)

    suspend fun toggleFavorite(song: Song) = withContext(Dispatchers.IO) {
        if (song.isFavorite) {
            favoriteDao.removeFavorite(song.path)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(song.path))
        }
    }

    suspend fun saveTrackOverride(
        song: Song,
        title: String,
        artist: String,
        album: String,
        albumArtist: String?,
        genre: String?,
        year: Int?,
        trackNumber: Int?,
        discNumber: Int?,
        composer: String?,
        customArtworkPath: String?
    ) = withContext(Dispatchers.IO) {
        val override = TrackOverrideEntity(
            trackPath = song.path,
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            composer = composer,
            customArtworkPath = customArtworkPath ?: song.customArtworkPath
        )
        trackOverrideDao.saveOverride(override)
    }

    suspend fun recordPlayed(song: Song) = withContext(Dispatchers.IO) {
        if (!song.isStream) {
            historyDao.addHistoryEntry(
                ListeningHistoryEntity(
                    trackPath = song.path,
                    title = song.title,
                    artist = song.artist
                )
            )
        }
    }

    // Playlists
    fun getPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow().map { entities ->
            entities.map { entity ->
                val trackCount = playlistDao.getPlaylistTrackPaths(entity.id).size
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songCount = trackCount,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackPath: String) = withContext(Dispatchers.IO) {
        val currentTracks = playlistDao.getPlaylistTrackPaths(playlistId)
        if (!currentTracks.contains(trackPath)) {
            playlistDao.insertPlaylistTracks(
                listOf(PlaylistTrackEntity(playlistId, trackPath, currentTracks.size))
            )
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackPath: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackPath)
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val paths = playlistDao.getPlaylistTrackPaths(playlistId).toSet()
        val allSongs = _songsFlow.value.associateBy { it.path }
        paths.mapNotNull { allSongs[it] }
    }

    // Albums
    fun getAlbumsFlow(): Flow<List<Album>> {
        return songsFlow.map { songs ->
            songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = albumId,
                    title = first.displayAlbum,
                    artist = first.displayArtist,
                    songCount = albumSongs.size,
                    year = first.year,
                    customArtworkPath = first.customArtworkPath
                )
            }.sortedBy { it.title.lowercase() }
        }
    }

    // Artists
    fun getArtistsFlow(): Flow<List<Artist>> {
        return songsFlow.map { songs ->
            songs.groupBy { it.displayArtist.lowercase() }.map { (_, artistSongs) ->
                val first = artistSongs.first()
                val albumCount = artistSongs.map { it.albumId }.distinct().size
                Artist(
                    id = first.artistId,
                    name = first.displayArtist,
                    songCount = artistSongs.size,
                    albumCount = albumCount,
                    customArtworkPath = first.customArtworkPath
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    // Folders
    fun getFoldersFlow(): Flow<List<MusicFolder>> {
        return songsFlow.map { songs ->
            songs.groupBy { song ->
                File(song.path).parent ?: "/storage"
            }.map { (dirPath, dirSongs) ->
                MusicFolder(
                    path = dirPath,
                    name = dirPath.substringAfterLast('/'),
                    songCount = dirSongs.size
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    // Genres
    fun getGenresFlow(): Flow<Map<String, List<Song>>> {
        return songsFlow.map { songs ->
            songs.groupBy { it.genre ?: "Various" }
        }
    }
}
