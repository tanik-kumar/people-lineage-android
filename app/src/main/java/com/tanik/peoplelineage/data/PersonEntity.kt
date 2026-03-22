package com.tanik.peoplelineage.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "people",
    indices = [
        Index(value = ["fullName", "village"]),
        Index(value = ["village"]),
        Index(value = ["district"]),
        Index(value = ["normalizedName"]),
        Index(value = ["normalizedPhone"]),
        Index(value = ["normalizedVillage"]),
        Index(value = ["normalizedDistrict"]),
        Index(value = ["normalizedState"]),
        Index(value = ["isFavorite", "lastViewedAt"]),
    ],
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val normalizedName: String = "",
    val gender: String = "",
    val age: String = "",
    val phoneNumber: String = "",
    val normalizedPhone: String = "",
    val village: String,
    val normalizedVillage: String = "",
    val policeStation: String,
    val postOffice: String,
    val district: String,
    val normalizedDistrict: String = "",
    val state: String,
    val normalizedState: String = "",
    val country: String,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val lastViewedAt: Long = 0L,
    val remoteId: String? = null,
    val syncState: String = PersonSyncState.LOCAL_ONLY.name,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
