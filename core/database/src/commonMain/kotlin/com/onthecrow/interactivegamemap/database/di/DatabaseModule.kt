package com.onthecrow.nomadrates.database.di

import org.koin.dsl.module

val databaseModule = module {
    includes(databasePlatformModule)
}
