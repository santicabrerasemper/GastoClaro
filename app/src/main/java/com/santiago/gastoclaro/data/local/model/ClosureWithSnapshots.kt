package com.santiago.gastoclaro.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.santiago.gastoclaro.data.local.entity.MonthlyCategorySnapshotEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity

data class ClosureWithSnapshots(
    @Embedded val closure: MonthlyClosureEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "closureId"
    )
    val categories: List<MonthlyCategorySnapshotEntity>
)
