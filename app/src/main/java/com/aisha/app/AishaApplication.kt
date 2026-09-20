package com.aisha.app

import android.app.Application
import com.aisha.app.di.AppContainer

class AishaApplication : Application() {

    lateinit var container: AppContainer; private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, BuildConfig.GEMINI_API_KEY)
        // §20 — midnight Day Finalization chain; BootReceiver re-arms after restart (§21).
        com.aisha.app.services.ServiceScheduler.scheduleNextMidnight(this)
    }
}
