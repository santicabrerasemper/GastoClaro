package com.santiago.gastoclaro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_closures",
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
        Index(value = ["profileId", "year", "month"], unique = true)
    ]
)
data class MonthlyClosureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val year: Int,
    val month: Int,
    val initialAmountCents: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val savingCents: Long = 0,
    val balanceCents: Long,
    val movementCount: Int,
    val closedAt: Long,
    val closureOrigin: String
)
