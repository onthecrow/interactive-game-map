package com.onthecrow.nomadrates.database.di

import com.onthecrow.nomadrates.database.IosRoomFactory
import com.onthecrow.nomadrates.database.RoomFactory
import org.koin.dsl.module

internal actual val databasePlatformModule = module {
    single<RoomFactory> { IosRoomFactory() }
}
