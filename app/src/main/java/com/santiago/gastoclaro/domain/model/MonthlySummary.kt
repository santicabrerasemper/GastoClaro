package com.santiago.gastoclaro.domain.model

data class MonthlySummary(
    val initialCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0
) {
    val balanceCents: Long get() = initialCents + incomeCents - expenseCents
}
