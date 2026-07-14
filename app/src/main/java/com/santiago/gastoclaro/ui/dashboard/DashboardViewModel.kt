package com.santiago.gastoclaro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.entity.SavingGoalEntity
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.model.MonthlySummary
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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

data class DashboardUiState(
    val profileId: Long? = null,
    val period: YearMonth = YearMonth.now(),
    val summary: MonthlySummary = MonthlySummary(),
    val categoryTotals: List<CategoryTotalRow> = emptyList(),
    val recentMovements: List<MovementRow> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val savingGoals: List<SavingGoalEntity> = emptyList(),
    val nextCardEvents: List<CardEventUi> = emptyList(),
    val isClosed: Boolean = false,
    val isReopened: Boolean = false
)

data class CardEventUi(
    val title: String,
    val date: LocalDate,
    val kind: String
)

private data class DashboardSetupSource(
    val paymentMethods: List<PaymentMethodEntity>,
    val savingGoals: List<SavingGoalEntity>
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profileId, period -> profileId to period }
        .flatMapLatest { (profileId, period) ->
            if (profileId == null) return@flatMapLatest flowOf(DashboardUiState(period = period))
            val setupSource = combine(
                financeRepository.observePaymentMethods(profileId),
                financeRepository.observeSavingGoals(profileId)
            ) { paymentMethods, savingGoals ->
                DashboardSetupSource(paymentMethods, savingGoals)
            }
            combine(
                financeRepository.observeBudget(profileId, period),
                financeRepository.observeMovements(profileId, period),
                financeRepository.observeMovementsBetween(profileId, period.minusMonths(59).atDay(1), period.atEndOfMonth()),
                setupSource,
                financeRepository.observeClosure(profileId, period)
            ) { budget, movements, budgetMovements, setup, closure ->
                val income = movements.filter { it.type == MovementType.INCOME }.sumOf { it.amountCents }
                val saving = movements.filter { it.type == MovementType.SAVING }.sumOf { it.amountCents }
                val expenseRows = budgetMovements.filter { it.type == MovementType.EXPENSE && it.impacts(period) }
                val expense = expenseRows.sumOf { it.monthlyImpactCents }
                val totals = expenseRows
                    .groupBy { it.categoryId }
                    .map { (id, rows) ->
                        val first = rows.first()
                        CategoryTotalRow(id, first.categoryName, first.categoryEmoji, first.categoryColorArgb, rows.sumOf { it.monthlyImpactCents })
                    }
                    .sortedByDescending { it.amountCents }
                DashboardUiState(
                    profileId = profileId,
                    period = period,
                    summary = MonthlySummary(budget?.initialAmountCents ?: 0, income, expense, saving),
                    categoryTotals = totals,
                    recentMovements = movements.take(5),
                    paymentMethods = setup.paymentMethods,
                    savingGoals = setup.savingGoals,
                    nextCardEvents = setup.paymentMethods.nextEvents(period),
                    isClosed = closure != null,
                    isReopened = budget?.isReopened == true
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun closeReopenedMonth() {
        val state = uiState.value
        val profileId = state.profileId ?: return
        viewModelScope.launch {
            runCatching { financeRepository.closeMonth(profileId, state.period, origin = "RECLOSE") }
                .onSuccess { messages.emit("Mes cerrado nuevamente") }
                .onFailure { messages.emit(it.message ?: "No se pudo cerrar el mes") }
        }
    }

    fun updateBudget(amountCents: Long) {
        val state = uiState.value
        val profileId = state.profileId ?: return
        viewModelScope.launch {
            runCatching { financeRepository.updateBudget(profileId, state.period, amountCents) }
                .onSuccess { messages.emit("Presupuesto actualizado") }
                .onFailure { messages.emit(it.message ?: "No se pudo actualizar") }
        }
    }

    private fun MovementRow.impacts(period: YearMonth): Boolean {
        val occurredPeriod = LocalDate.ofEpochDay(occurredEpochDay).let { YearMonth.from(it) }
        return if (annualizedMonths > 1) {
            !period.isBefore(occurredPeriod) && period.isBefore(occurredPeriod.plusMonths(annualizedMonths.toLong()))
        } else {
            occurredPeriod == period
        }
    }

    private fun List<PaymentMethodEntity>.nextEvents(period: YearMonth): List<CardEventUi> =
        filter { it.kind == "CREDIT_CARD" }
            .flatMap { method ->
                val name = method.displayName()
                listOfNotNull(
                    method.closingDay?.let { day ->
                        CardEventUi(
                            title = "$name cierra",
                            date = period.atDay(day.coerceIn(1, period.lengthOfMonth())),
                            kind = "Cierre"
                        )
                    },
                    method.dueDay?.let { day ->
                        val duePeriod = period.plusMonths(1)
                        CardEventUi(
                            title = "$name vence",
                            date = duePeriod.atDay(day.coerceIn(1, duePeriod.lengthOfMonth())),
                            kind = "Vence"
                        )
                    }
                )
            }
            .sortedBy { it.date }
            .take(3)

    private fun PaymentMethodEntity.displayName(): String =
        if (lastDigits.isBlank()) name else "$name ****$lastDigits"
}
