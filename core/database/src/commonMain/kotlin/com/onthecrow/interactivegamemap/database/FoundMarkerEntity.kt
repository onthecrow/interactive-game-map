package com.onthecrow.nomadrates.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "found_markers")
data class FoundMarkerEntity(
    @PrimaryKey
    val markerId: Int,
)
