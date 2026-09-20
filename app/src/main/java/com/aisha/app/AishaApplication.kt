package com.aisha.app

import android.app.Application
import com.aisha.app.di.AppContainer

class AishaApplication : Application() {

    lateinit var container: AppContainer; private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, BuildConfig.GEMINI_API_KEY)
        // TODO phase 2: schedule DayFinalizationService (midnight, spec §20),
        //      ConnectivityMonitor + sync worker drain (spec §16/§21).
    }
}
