package com.onthecrow.interactivegamemap.settings.di

import com.onthecrow.interactivegamemap.settings.IOSSettingsStorage
import com.onthecrow.interactivegamemap.settings.SettingsStorage
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val settingsPlatformModule = module {
    single { IOSSettingsStorage() } bind SettingsStorage::class
}
