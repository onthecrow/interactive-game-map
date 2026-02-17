package com.onthecrow.interactivegamemap

import android.app.Application
import android.content.Context
import android.os.Build
import org.koin.dsl.module

class AndroidPlatform(val context: Application) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val platformModule = module {
        single<Context> { context }
    }
}
