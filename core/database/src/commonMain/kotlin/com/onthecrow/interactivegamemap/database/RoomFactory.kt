package com.onthecrow.nomadrates.database

import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.reflect.KClass

abstract class RoomFactory {
    @PublishedApi
    internal abstract fun <T : RoomDatabase> createByClass(
        name: String,
        klass: KClass<T>,
        constructor: RoomDatabaseConstructor<T>,
        config: (RoomDatabase.Builder<T>) -> Unit
    ): T
}

inline fun <reified T : RoomDatabase> RoomFactory.create(
    name: String,
    constructor: RoomDatabaseConstructor<T>,
    noinline config: (RoomDatabase.Builder<T>) -> Unit = {}
): T {
    return createByClass(name, T::class, constructor, config)
}

internal fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyCommonConfig(): RoomDatabase.Builder<T> {
    return this
        .setDriver(BundledSQLiteDriver())
        // todo provide dispatcher via di
        .setQueryCoroutineContext(Dispatchers.IO)
}
