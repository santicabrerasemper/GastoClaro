package com.santiago.gastoclaro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_category_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyClosureEntity::class,
            parentColumns = ["id"],
            childColumns = ["closureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("closureId")]
)
data class MonthlyCategorySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val closureId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val colorArgb: Int,
    val amountCents: Long
)
