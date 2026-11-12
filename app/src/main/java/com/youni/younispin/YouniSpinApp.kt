package com.youni.younispin

import android.app.Application
import com.youni.younispin.data.db.AppDatabase
import com.youni.younispin.data.repository.MusicRepository
import com.youni.younispin.data.repository.RadioRepository

class YouniSpinApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var musicRepository: MusicRepository
        private set
    lateinit var radioRepository: RadioRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        musicRepository = MusicRepository(this, database)
        radioRepository = RadioRepository(this, database)
    }

    companion object {
        lateinit var instance: YouniSpinApp
            private set
    }
}
