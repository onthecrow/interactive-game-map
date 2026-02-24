package com.onthecrow.interactivegamemap.network.di

import com.onthecrow.interactivegamemap.network.AndroidRemoteConfigProvider
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import org.koin.dsl.bind
import org.koin.dsl.module

actual val remoteConfigPlatformModule = module {
    single { AndroidRemoteConfigProvider() } bind RemoteConfigProvider::class
}
