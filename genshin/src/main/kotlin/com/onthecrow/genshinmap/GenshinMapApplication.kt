package com.onthecrow.genshinmap

import android.app.Application
import com.onthecrow.interactivegamemap.AndroidPlatform
import com.onthecrow.interactivegamemap.AppInitializer

class GenshinMapApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.initialize(AndroidPlatform(this))
    }
}