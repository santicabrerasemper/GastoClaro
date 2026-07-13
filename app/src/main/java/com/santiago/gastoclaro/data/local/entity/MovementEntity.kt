package com.santiago.gastoclaro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("profileId"), Index("categoryId"), Index("paymentMethodId"), Index("occurredEpochDay")]
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val categoryId: Long,
    val paymentMethodId: Long? = null,
    val subcategoryName: String = "",
    val currency: String = "ARS",
    val currencyAmountCents: Long = amountCents,
    val exchangeRateCents: Long? = null,
    val type: MovementType,
    val amountCents: Long,
    val monthlyImpactCents: Long,
    val annualizedMonths: Int = 1,
    val installmentGroupId: String? = null,
    val installmentIndex: Int = 1,
    val installmentCount: Int = 1,
    val originalAmountCents: Long = amountCents,
    val note: String,
    val occurredEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long
)
