package com.onthecrow.nomadrates.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoundMarkerDao {
    @Query("SELECT markerId FROM found_markers")
    fun getMarkerIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(marker: FoundMarkerEntity)

    @Query("DELETE FROM found_markers WHERE markerId = :markerId")
    suspend fun delete(markerId: Int)
}
