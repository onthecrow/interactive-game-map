package com.onthecrow.mapslinger

import android.app.Application
import com.onthecrow.interactivegamemap.AndroidPlatform
import com.onthecrow.interactivegamemap.AppInitializer
import com.onthecrow.interactivegamemap.sync.Synchronizer
import org.koin.android.ext.android.inject

class RDR2MapApplication : Application() {
    private val synchronizer: Synchronizer by inject()

    override fun onCreate() {
        super.onCreate()
        AppInitializer.initialize(AndroidPlatform(this))
        // As early as possible: the data starts loading while the activity is being created
        synchronizer.start()
    }
}
