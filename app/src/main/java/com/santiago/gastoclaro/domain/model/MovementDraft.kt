package com.santiago.gastoclaro.domain.model

import com.santiago.gastoclaro.data.local.entity.MovementType
import java.time.LocalDate

data class MovementDraft(
    val id: Long? = null,
    val profileId: Long,
    val categoryId: Long,
    val paymentMethodId: Long?,
    val type: MovementType,
    val amountCents: Long,
    val monthlyImpactCents: Long,
    val annualizedMonths: Int,
    val installmentCount: Int,
    val note: String,
    val occurredOn: LocalDate
)
