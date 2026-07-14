package com.santiago.gastoclaro.ui.summaries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.model.MonthlySummary
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import com.santiago.gastoclaro.ui.payments.CardStatementUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SummariesUiState(
    val profileId: Long? = null,
    val period: YearMonth = YearMonth.now(),
    val summary: MonthlySummary = MonthlySummary(),
    val categoryTotals: List<CategoryTotalRow> = emptyList(),
    val expenses: List<MovementRow> = emptyList(),
    val savings: List<MovementRow> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val cardStatements: List<CardStatementUi> = emptyList()
)

@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SummariesUiState> = combine(
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profileId, period -> profileId to period }
        .flatMapLatest { (profileId, period) ->
            if (profileId == null) {
                flowOf(SummariesUiState(period = period))
            } else {
                val statementStart = period.minusMonths(1).atDay(1)
                val statementEnd = period.plusMonths(1).atEndOfMonth()
                combine(
                    financeRepository.observeBudget(profileId, period),
                    financeRepository.observeMovements(profileId, period),
                    financeRepository.observeMovementsBetween(profileId, period.minusMonths(59).atDay(1), period.atEndOfMonth()),
                    financeRepository.observePaymentMethods(profileId),
                    financeRepository.observeMovementsBetween(profileId, statementStart, statementEnd)
                ) { budget, monthMovements, budgetMovements, methods, statementMovements ->
                    val income = monthMovements
                        .filter { it.type == MovementType.INCOME }
                        .sumOf { it.amountCents }
                    val savings = monthMovements
                        .filter { it.type == MovementType.SAVING }
                        .sortedByDescending { it.occurredEpochDay }
                    val expenseRows = budgetMovements
                        .filter { it.type == MovementType.EXPENSE && it.impacts(period) }
                    val expense = expenseRows.sumOf { it.monthlyImpactCents }
                    val totals = expenseRows
                        .groupBy { it.categoryId }
                        .map { (id, rows) ->
                            val first = rows.first()
                            CategoryTotalRow(
                                categoryId = id,
                                categoryName = first.categoryName,
                                categoryEmoji = first.categoryEmoji,
                                colorArgb = first.categoryColorArgb,
                                amountCents = rows.sumOf { it.monthlyImpactCents }
                            )
                        }
                        .sortedByDescending { it.amountCents }
                    SummariesUiState(
                        profileId = profileId,
                        period = period,
                        summary = MonthlySummary(
                            initialCents = budget?.initialAmountCents ?: 0,
                            incomeCents = income,
                            expenseCents = expense,
                            savingCents = savings.sumOf { it.amountCents }
                        ),
                        categoryTotals = totals,
                        expenses = monthMovements
                            .filter { it.type == MovementType.EXPENSE }
                            .sortedByDescending { it.occurredEpochDay },
                        savings = savings,
                        paymentMethods = methods,
                        cardStatements = methods
                            .filter { it.kind == "CREDIT_CARD" }
                            .map { method -> method.toStatement(period, statementMovements) }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummariesUiState())

    fun selectMonth(period: YearMonth) {
        viewModelScope.launch { activeProfileStore.setSelectedMonth(period) }
    }

    private fun PaymentMethodEntity.toStatement(
        period: YearMonth,
        movements: List<MovementRow>
    ): CardStatementUi {
        val periodEnd = period.atDay((closingDay ?: period.lengthOfMonth()).coerceIn(1, period.lengthOfMonth()))
        val previous = period.minusMonths(1)
        val previousClosing = previous.atDay((closingDay ?: previous.lengthOfMonth()).coerceIn(1, previous.lengthOfMonth()))
        val periodStart = previousClosing.plusDays(1)
        val rows = movements.filter { movement ->
            movement.paymentMethodId == id &&
                movement.type == MovementType.EXPENSE &&
                LocalDate.ofEpochDay(movement.occurredEpochDay) in periodStart..periodEnd
        }
        return CardStatementUi(
            paymentMethodId = id,
            totalCents = rows.sumOf { it.amountCents },
            movementCount = rows.size,
            periodStart = periodStart,
            periodEnd = periodEnd,
            dueDate = dueDay?.let { day ->
                val duePeriod = period.plusMonths(1)
                duePeriod.atDay(day.coerceIn(1, duePeriod.lengthOfMonth()))
            },
            movements = rows.sortedByDescending { it.occurredEpochDay }
        )
    }

    private fun MovementRow.impacts(period: YearMonth): Boolean {
        val occurredPeriod = LocalDate.ofEpochDay(occurredEpochDay).let { YearMonth.from(it) }
        return if (annualizedMonths > 1) {
            !period.isBefore(occurredPeriod) && period.isBefore(occurredPeriod.plusMonths(annualizedMonths.toLong()))
        } else {
            occurredPeriod == period
        }
    }
}
