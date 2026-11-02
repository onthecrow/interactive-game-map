package com.onthecrow.interactivegamemap.settings.di

import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.settings.SettingsRepository
import com.onthecrow.interactivegamemap.settings.SettingsRepositoryImpl
import com.onthecrow.interactivegamemap.settings.systemLanguageTags
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val settingsPlatformModule: Module

val settingsModule = module {
    includes(settingsPlatformModule)
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single { AppLanguageProvider(get(), ::systemLanguageTags) }
}
