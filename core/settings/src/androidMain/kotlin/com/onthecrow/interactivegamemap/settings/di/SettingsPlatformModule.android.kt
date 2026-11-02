package com.onthecrow.interactivegamemap.settings.di

import com.onthecrow.interactivegamemap.settings.AndroidSettingsStorage
import com.onthecrow.interactivegamemap.settings.SettingsStorage
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val settingsPlatformModule = module {
    single { AndroidSettingsStorage(get()) } bind SettingsStorage::class
}
