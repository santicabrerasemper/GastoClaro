package com.santiago.gastoclaro.data.local.model

import com.santiago.gastoclaro.data.local.entity.MovementType

data class MovementRow(
    val id: Long,
    val profileId: Long,
    val categoryId: Long,
    val paymentMethodId: Long?,
    val subcategoryName: String,
    val currency: String,
    val currencyAmountCents: Long,
    val exchangeRateCents: Long?,
    val type: MovementType,
    val amountCents: Long,
    val monthlyImpactCents: Long,
    val annualizedMonths: Int,
    val installmentGroupId: String?,
    val installmentIndex: Int,
    val installmentCount: Int,
    val originalAmountCents: Long,
    val note: String,
    val occurredEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryColorArgb: Int,
    val paymentMethodName: String?,
    val paymentMethodKind: String?,
    val paymentMethodLastDigits: String?
)
