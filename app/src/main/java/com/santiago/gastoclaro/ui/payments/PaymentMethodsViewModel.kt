package com.santiago.gastoclaro.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
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

data class CardStatementUi(
    val paymentMethodId: Long,
    val totalCents: Long,
    val movementCount: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate?
)

data class PaymentMethodsUiState(
    val profileId: Long? = null,
    val period: YearMonth = YearMonth.now(),
    val methods: List<PaymentMethodEntity> = emptyList(),
    val statements: List<CardStatementUi> = emptyList()
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val uiState: StateFlow<PaymentMethodsUiState> = combine(
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profileId, period -> profileId to period }
        .flatMapLatest { (profileId, period) ->
            if (profileId == null) {
                flowOf(PaymentMethodsUiState(period = period))
            } else {
                val start = period.minusMonths(1).atDay(1)
                val end = period.plusMonths(1).atEndOfMonth()
                combine(
                    financeRepository.observePaymentMethods(profileId),
                    financeRepository.observeMovementsBetween(profileId, start, end)
                ) { methods, movements ->
                    val statements = methods
                        .filter { it.kind == "CREDIT_CARD" }
                        .map { method ->
                            val periodEnd = period.atDay((method.closingDay ?: period.lengthOfMonth()).coerceIn(1, period.lengthOfMonth()))
                            val previous = period.minusMonths(1)
                            val previousClosing = previous.atDay((method.closingDay ?: previous.lengthOfMonth()).coerceIn(1, previous.lengthOfMonth()))
                            val periodStart = previousClosing.plusDays(1)
                            val rows = movements.filter { movement ->
                                movement.paymentMethodId == method.id &&
                                    movement.type == MovementType.EXPENSE &&
                                    LocalDate.ofEpochDay(movement.occurredEpochDay) in periodStart..periodEnd
                            }
                            CardStatementUi(
                                paymentMethodId = method.id,
                                totalCents = rows.sumOf { it.amountCents },
                                movementCount = rows.size,
                                periodStart = periodStart,
                                periodEnd = periodEnd,
                                dueDate = method.dueDay?.let { day ->
                                    val duePeriod = period.plusMonths(1)
                                    duePeriod.atDay(day.coerceIn(1, duePeriod.lengthOfMonth()))
                                }
                            )
                        }
                    PaymentMethodsUiState(profileId, period, methods, statements)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentMethodsUiState())

    fun save(id: Long?, name: String, kind: String, lastDigits: String, closingDay: Int?, dueDay: Int?) {
        val profileId = uiState.value.profileId ?: return
        viewModelScope.launch {
            runCatching {
                financeRepository.savePaymentMethod(
                    PaymentMethodEntity(
                        id = id ?: 0,
                        profileId = profileId,
                        name = name,
                        kind = kind,
                        lastDigits = lastDigits,
                        closingDay = closingDay,
                        dueDay = dueDay,
                        createdAt = 0,
                        updatedAt = 0
                    )
                )
            }.onSuccess {
                messages.emit(if (id == null) "Medio de pago guardado" else "Medio actualizado")
            }.onFailure {
                messages.emit(it.message ?: "No se pudo guardar")
            }
        }
    }

    fun archive(id: Long) {
        val profileId = uiState.value.profileId ?: return
        viewModelScope.launch {
            runCatching { financeRepository.archivePaymentMethod(profileId, id) }
                .onSuccess { messages.emit("Medio archivado") }
                .onFailure { messages.emit(it.message ?: "No se pudo archivar") }
        }
    }

    fun selectMonth(period: YearMonth) {
        viewModelScope.launch { activeProfileStore.setSelectedMonth(period) }
    }
}
