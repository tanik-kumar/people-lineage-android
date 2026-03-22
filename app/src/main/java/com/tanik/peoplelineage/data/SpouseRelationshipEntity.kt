package com.tanik.peoplelineage.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "spouse_relationships",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personAId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personBId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["personAId"]),
        Index(value = ["personBId"]),
        Index(value = ["personAId", "personBId"], unique = true),
    ],
)
data class SpouseRelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personAId: Long,
    val personBId: Long,
    val status: String = SpouseRelationshipStatus.ACTIVE.name,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
