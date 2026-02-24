package com.onthecrow.interactivegamemap.network.di

import com.onthecrow.interactivegamemap.network.IOSRemoteConfigProvider
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import org.koin.dsl.bind
import org.koin.dsl.module

actual val remoteConfigPlatformModule = module {
    single { IOSRemoteConfigProvider() } bind RemoteConfigProvider::class
}
