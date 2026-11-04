package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.IOSPreferencesStorage
import com.onthecrow.interactivegamemap.map.PreferencesStorage
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val mapPlatformModule = module {
    single { IOSPreferencesStorage() } bind PreferencesStorage::class
}
