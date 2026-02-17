package com.onthecrow.rdr2map

import android.app.Application
import com.onthecrow.interactivegamemap.AndroidPlatform
import com.onthecrow.interactivegamemap.AppInitializer

class RDR2MapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.initialize(AndroidPlatform(this))
    }
}