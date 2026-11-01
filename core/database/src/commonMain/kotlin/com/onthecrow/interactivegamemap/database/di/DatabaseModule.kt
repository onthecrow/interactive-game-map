package com.onthecrow.nomadrates.database.di

import com.onthecrow.nomadrates.database.AppDatabase
import com.onthecrow.nomadrates.database.AppDatabaseConstructor
import com.onthecrow.nomadrates.database.RoomFactory
import com.onthecrow.nomadrates.database.create
import org.koin.dsl.module

private const val DATABASE_NAME = "map.db"

val databaseModule = module {
    includes(databasePlatformModule)
    single { get<RoomFactory>().create(DATABASE_NAME, AppDatabaseConstructor) }
    single { get<AppDatabase>().foundMarkerDao() }
}
