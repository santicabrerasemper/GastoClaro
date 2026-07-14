package com.santiago.gastoclaro.ui.summaries

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
import com.santiago.gastoclaro.ui.payments.CardStatementUi
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

data class SummariesUiState(
    val profileId: Long? = null,
    val period: YearMonth = YearMonth.now(),
    val summary: MonthlySummary = MonthlySummary(),
    val categoryTotals: List<CategoryTotalRow> = emptyList(),
    val expenses: List<MovementRow> = emptyList(),
    val savings: List<MovementRow> = emptyList(),
    val savingGoals: List<SavingGoalUi> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val cardStatements: List<CardStatementUi> = emptyList()
)

data class SavingGoalUi(
    val id: Long,
    val name: String,
    val targetCents: Long,
    val amountCents: Long,
    val movementCount: Int
) {
    val progress: Float get() = if (targetCents <= 0) 0f else (amountCents.toFloat() / targetCents.toFloat()).coerceIn(0f, 1f)
    val remainingCents: Long get() = (targetCents - amountCents).coerceAtLeast(0)
}

private data class MonthlySummarySource(
    val budget: com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity?,
    val monthMovements: List<MovementRow>,
    val budgetMovements: List<MovementRow>
)

private data class CardSummarySource(
    val methods: List<PaymentMethodEntity>,
    val statementMovements: List<MovementRow>
)

private data class GoalSummarySource(
    val goals: List<SavingGoalEntity>,
    val progressMovements: List<MovementRow>
)

@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

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
                val monthlySource = combine(
                    financeRepository.observeBudget(profileId, period),
                    financeRepository.observeMovements(profileId, period),
                    financeRepository.observeMovementsBetween(profileId, period.minusMonths(59).atDay(1), period.atEndOfMonth())
                ) { budget, monthMovements, budgetMovements ->
                    MonthlySummarySource(budget, monthMovements, budgetMovements)
                }
                val cardSource = combine(
                    financeRepository.observePaymentMethods(profileId),
                    financeRepository.observeMovementsBetween(profileId, statementStart, statementEnd)
                ) { methods, statementMovements ->
                    CardSummarySource(methods, statementMovements)
                }
                val goalSource = combine(
                    financeRepository.observeSavingGoals(profileId),
                    financeRepository.observeMovementsBetween(profileId, LocalDate.of(2000, 1, 1), period.atEndOfMonth())
                ) { goals, progressMovements ->
                    GoalSummarySource(goals, progressMovements)
                }
                combine(
                    monthlySource,
                    cardSource,
                    goalSource
                ) { monthly, cards, goalsSource ->
                    val budget = monthly.budget
                    val monthMovements = monthly.monthMovements
                    val budgetMovements = monthly.budgetMovements
                    val methods = cards.methods
                    val statementMovements = cards.statementMovements
                    val goals = goalsSource.goals
                    val progressMovements = goalsSource.progressMovements
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
                        savingGoals = savings
                            .toGoalProgress(goals, progressMovements),
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

    fun saveGoal(id: Long?, name: String, targetCents: Long) {
        val profileId = uiState.value.profileId ?: return
        viewModelScope.launch {
            runCatching {
                financeRepository.saveSavingGoal(
                    SavingGoalEntity(
                        id = id ?: 0,
                        profileId = profileId,
                        name = name,
                        targetCents = targetCents,
                        createdAt = 0,
                        updatedAt = 0
                    )
                )
            }.onSuccess {
                messages.emit("Meta guardada")
            }.onFailure {
                messages.emit(it.message ?: "No se pudo guardar la meta")
            }
        }
    }

    fun archiveGoal(id: Long) {
        val profileId = uiState.value.profileId ?: return
        viewModelScope.launch {
            runCatching { financeRepository.archiveSavingGoal(profileId, id) }
                .onSuccess { messages.emit("Meta archivada") }
                .onFailure { messages.emit(it.message ?: "No se pudo archivar") }
        }
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

    private fun List<MovementRow>.toGoalProgress(
        goals: List<SavingGoalEntity>,
        progressMovements: List<MovementRow>
    ): List<SavingGoalUi> {
        val savingProgress = progressMovements
            .filter { it.type == MovementType.SAVING }
            .groupBy { it.note.trim().lowercase() }
        val realGoals = goals.map { goal ->
            val rows = savingProgress[goal.name.trim().lowercase()].orEmpty()
            SavingGoalUi(
                id = goal.id,
                name = goal.name,
                targetCents = goal.targetCents,
                amountCents = rows.sumOf { it.amountCents },
                movementCount = rows.size
            )
        }
        val names = goals.map { it.name.trim().lowercase() }.toSet()
        val adHocGoals = this
            .filter { it.note.isNotBlank() && it.note.trim().lowercase() !in names }
            .groupBy { it.note.trim() }
            .map { (name, rows) ->
                SavingGoalUi(
                    id = -name.hashCode().toLong(),
                    name = name,
                    targetCents = rows.sumOf { it.amountCents },
                    amountCents = rows.sumOf { it.amountCents },
                    movementCount = rows.size
                )
            }
        return (realGoals + adHocGoals).sortedByDescending { it.amountCents }
    }
}
