package com.onthecrow.nomadrates.database

import androidx.room.TypeConverter
import kotlin.time.Instant

object CommonTypeConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilliseconds()
    }

    @TypeConverter
    fun fromString(value: String): List<Double> {
        if (value.isEmpty()) return emptyList()
        return value.split(";").map { it.toDouble() }
    }

    @TypeConverter
    fun fromList(list: List<Double>): String {
        return list.joinToString(separator = ";")
    }
}
