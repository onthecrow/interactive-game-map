package com.onthecrow.interactivegamemap.network.di

import com.onthecrow.interactivegamemap.network.provideHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

val networkModule: Module = module {
    single<HttpClient> { provideHttpClient() }
}
