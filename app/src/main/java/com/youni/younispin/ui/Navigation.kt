package com.youni.younispin.ui

object Screen {
    const val HOME = "home"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album_detail/{albumId}"
    const val ARTISTS = "artists"
    const val ARTIST_DETAIL = "artist_detail/{artistId}"
    const val GENRES = "genres"
    const val FOLDERS = "folders"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
    const val FAVORITES = "favorites"
    const val RADIO = "radio"
    const val NOW_PLAYING = "now_playing"
    const val EQUALIZER = "equalizer"
    const val METADATA_EDITOR = "metadata_editor/{trackPath}?scan={scan}"
    const val SETTINGS = "settings"
    const val APPEARANCE_STUDIO = "appearance_studio"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
    fun artistDetail(artistId: Long) = "artist_detail/$artistId"
    fun playlistDetail(playlistId: Long) = "playlist_detail/$playlistId"
    fun metadataEditor(encodedPath: String, scanOnline: Boolean = false) =
        "metadata_editor/$encodedPath?scan=$scanOnline"
}
