package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SettingsRepository

class SmartTranslatorApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
    }

    companion object {
        lateinit var instance: SmartTranslatorApplication
            private set
    }
}
