package com.youni.younispin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackOverrideEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        RadioStationEntity::class,
        ListeningHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackOverrideDao(): TrackOverrideDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun radioDao(): RadioDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "younispin.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN homepage TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN language TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN codec TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN bitrate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN source TEXT NOT NULL DEFAULT 'CUSTOM'")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN sourceId TEXT")
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """UPDATE radio_stations
                       SET source = 'SOMAFM', isCustom = 0,
                           sourceId = CASE name
                             WHEN 'Groove Salad (SomaFM)' THEN 'groovesalad'
                             WHEN 'Drone Zone (SomaFM)' THEN 'dronezone'
                             WHEN 'Secret Agent (SomaFM)' THEN 'secretagent'
                             WHEN 'SomaFM Indie Pop Rocks!' THEN 'indiepop'
                           END,
                           homepage = CASE name
                             WHEN 'Groove Salad (SomaFM)' THEN 'https://somafm.com/groovesalad/'
                             WHEN 'Drone Zone (SomaFM)' THEN 'https://somafm.com/dronezone/'
                             WHEN 'Secret Agent (SomaFM)' THEN 'https://somafm.com/secretagent/'
                             WHEN 'SomaFM Indie Pop Rocks!' THEN 'https://somafm.com/indiepop/'
                           END
                       WHERE name IN (
                         'Groove Salad (SomaFM)', 'Drone Zone (SomaFM)',
                         'Secret Agent (SomaFM)', 'SomaFM Indie Pop Rocks!'
                       )"""
                )
                db.execSQL(
                    """DELETE FROM radio_stations WHERE name IN (
                        'Lofi Hip Hop Radio', 'Classic Rock Radio',
                        'Synthwave / Retro 80s', 'KUSC Classical California'
                    )"""
                )
            }
        }
    }
}
