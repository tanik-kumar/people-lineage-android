package com.tanik.peoplelineage.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tanik.peoplelineage.model.ParentRelationType

@Entity(
    tableName = "person_relationships",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["childId"]),
        Index(value = ["parentId", "childId"], unique = true),
    ],
)
data class PersonRelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long,
    val childId: Long,
    val relationType: String = ParentRelationType.UNKNOWN.name,
    val createdAt: Long = System.currentTimeMillis(),
)
