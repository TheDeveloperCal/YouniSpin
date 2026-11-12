package com.youni.younispin.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackOverrideDao {
    @Query("SELECT * FROM track_overrides")
    fun getAllOverridesFlow(): Flow<List<TrackOverrideEntity>>

    @Query("SELECT * FROM track_overrides")
    suspend fun getAllOverrides(): List<TrackOverrideEntity>

    @Query("SELECT * FROM track_overrides WHERE trackPath = :path")
    suspend fun getOverrideForPath(path: String): TrackOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(override: TrackOverrideEntity)

    @Delete
    suspend fun deleteOverride(override: TrackOverrideEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT trackPath FROM favorites")
    fun getFavoritePathsFlow(): Flow<List<String>>

    @Query("SELECT trackPath FROM favorites")
    suspend fun getFavoritePaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackPath = :path)")
    fun isFavoriteFlow(path: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackPath = :path")
    suspend fun removeFavorite(path: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    @Query("SELECT trackPath FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getPlaylistTrackPathsFlow(playlistId: Long): Flow<List<String>>

    @Query("SELECT trackPath FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getPlaylistTrackPaths(playlistId: Long): List<String>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackPath = :path")
    suspend fun removeTrackFromPlaylist(playlistId: Long, path: String)
}

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations WHERE isCustom = 1 OR isFavorite = 1 OR lastPlayed > 0 ORDER BY isFavorite DESC, name ASC")
    fun getAllStationsFlow(): Flow<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteStationsFlow(): Flow<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 20")
    fun getRecentStationsFlow(): Flow<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations WHERE id = :id")
    suspend fun getStationById(id: Long): RadioStationEntity?

    @Query("SELECT * FROM radio_stations WHERE source = :source AND sourceId = :sourceId LIMIT 1")
    suspend fun getStationBySourceId(source: String, sourceId: String): RadioStationEntity?

    @Query("SELECT * FROM radio_stations WHERE source = :source AND sourceId IS NOT NULL")
    suspend fun getStationsBySource(source: String): List<RadioStationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: RadioStationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<RadioStationEntity>)

    @Update
    suspend fun updateStation(station: RadioStationEntity)

    @Delete
    suspend fun deleteStation(station: RadioStationEntity)

    @Query("UPDATE radio_stations SET lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("UPDATE radio_stations SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun addHistoryEntry(entry: ListeningHistoryEntity)

    @Query("SELECT * FROM listening_history ORDER BY playedAt DESC LIMIT 50")
    fun getRecentHistoryFlow(): Flow<List<ListeningHistoryEntity>>

    @Query("SELECT trackPath, COUNT(*) as playCount FROM listening_history GROUP BY trackPath ORDER BY playCount DESC LIMIT 50")
    suspend fun getMostPlayedPaths(): List<TrackPlayCount>

    @Query("DELETE FROM listening_history")
    suspend fun clearHistory()
}

data class TrackPlayCount(
    val trackPath: String,
    val playCount: Int
)
