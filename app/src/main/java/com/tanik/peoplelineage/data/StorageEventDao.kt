package com.tanik.peoplelineage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StorageEventEntity): Long

    @Query("SELECT * FROM storage_events ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<StorageEventEntity>>

    @Query("SELECT * FROM storage_events ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<StorageEventEntity>
}
