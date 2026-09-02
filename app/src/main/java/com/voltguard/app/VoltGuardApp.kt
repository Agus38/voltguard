package com.voltguard.app

import android.app.Application
import com.voltguard.app.data.PowerRepository
import com.voltguard.app.data.db.AppDatabase
import com.voltguard.app.data.prefs.SettingsStore

class VoltGuardApp : Application() {

    lateinit var repository: PowerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = PowerRepository(
            app = this,
            store = SettingsStore(this),
            db = AppDatabase.get(this),
        )
    }

    companion object {
        @Volatile
        lateinit var instance: VoltGuardApp
            private set
    }
}
