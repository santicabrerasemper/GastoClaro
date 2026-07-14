package com.santiago.gastoclaro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saving_goals",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "name"], unique = true)
    ]
)
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val targetCents: Long,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
