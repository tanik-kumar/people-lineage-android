package com.tanik.peoplelineage.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_events",
    indices = [
        Index(value = ["eventType", "startedAt"]),
        Index(value = ["status", "completedAt"]),
    ],
)
data class StorageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val status: String,
    val storageMode: String,
    val locationUri: String = "",
    val checksum: String = "",
    val message: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = startedAt,
)
