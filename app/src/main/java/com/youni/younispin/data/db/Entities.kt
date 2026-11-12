package com.youni.younispin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_overrides")
data class TrackOverrideEntity(
    @PrimaryKey
    val trackPath: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val composer: String? = null,
    val comment: String? = null,
    val customArtworkPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val trackPath: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackPath"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackPath: String,
    val orderIndex: Int
)

@Entity(tableName = "radio_stations")
data class RadioStationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val url: String,
    val genre: String,
    val logoUrl: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0L,
    val description: String? = null,
    val homepage: String? = null,
    val country: String? = null,
    val language: String? = null,
    val codec: String? = null,
    val bitrate: Int = 0,
    val source: String = "CUSTOM",
    val sourceId: String? = null,
    val isCustom: Boolean = true
)

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val trackPath: String,
    val title: String,
    val artist: String,
    val playedAt: Long = System.currentTimeMillis()
)
