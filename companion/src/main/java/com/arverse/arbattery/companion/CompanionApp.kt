package com.arverse.arbattery.companion

import android.app.Application

class CompanionApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Schedule periodic WorkManager sync
        CompanionSyncManager.schedulePeriodicSync(this)
    }

    companion object {
        lateinit var instance: CompanionApp
            private set
    }
}
