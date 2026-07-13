package com.santiago.gastoclaro

import com.santiago.gastoclaro.domain.model.MonthlySummary
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlySummaryTest {
    @Test
    fun balanceIncludesInitialIncomeAndExpenses() {
        val summary = MonthlySummary(
            initialCents = 1_000_000,
            incomeCents = 250_000,
            expenseCents = 400_000
        )
        assertEquals(850_000, summary.balanceCents)
    }
}
