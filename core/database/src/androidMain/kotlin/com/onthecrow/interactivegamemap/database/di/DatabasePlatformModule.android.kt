package com.onthecrow.nomadrates.database.di

import com.onthecrow.nomadrates.database.AndroidRoomFactory
import com.onthecrow.nomadrates.database.RoomFactory
import org.koin.dsl.module

internal actual val databasePlatformModule = module {
    single<RoomFactory> { AndroidRoomFactory(get()) }
}
