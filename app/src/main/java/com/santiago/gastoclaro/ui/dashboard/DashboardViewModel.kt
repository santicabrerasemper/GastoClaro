package com.santiago.gastoclaro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.model.MonthlySummary
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isClosed: Boolean = false,
    val isReopened: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val uiState: StateFlow<DashboardUiState> = combine(
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profileId, period -> profileId to period }
        .flatMapLatest { (profileId, period) ->
            if (profileId == null) return@flatMapLatest flowOf(DashboardUiState(period = period))
            combine(
                financeRepository.observeBudget(profileId, period),
                financeRepository.observeMovements(profileId, period),
                financeRepository.observeMovementsBetween(profileId, period.minusMonths(59).atDay(1), period.atEndOfMonth()),
                financeRepository.observeClosure(profileId, period)
            ) { budget, movements, budgetMovements, closure ->
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
}
