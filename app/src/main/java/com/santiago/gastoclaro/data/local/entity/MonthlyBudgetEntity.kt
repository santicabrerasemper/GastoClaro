package com.santiago.gastoclaro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_budgets",
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
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val year: Int,
    val month: Int,
    val initialAmountCents: Long,
    val isReopened: Boolean = false,
    val updatedAt: Long
)
