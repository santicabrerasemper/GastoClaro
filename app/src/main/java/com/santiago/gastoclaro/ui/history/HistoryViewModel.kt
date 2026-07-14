package com.santiago.gastoclaro.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.model.ClosureWithSnapshots
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
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
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    activeProfileStore: ActiveProfileStore,
    private val financeRepository: FinanceRepository
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val closures: StateFlow<List<MonthlyClosureEntity>> = activeProfileStore.activeProfileId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else financeRepository.observeClosures(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class HistoryDetailUiState(
    val closure: ClosureWithSnapshots? = null,
    val expenses: List<MovementRow> = emptyList()
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {
    private val profileId: Long = checkNotNull(savedStateHandle["profileId"])
    private val year: Int = checkNotNull(savedStateHandle["year"])
    private val month: Int = checkNotNull(savedStateHandle["month"])
    val period: YearMonth = YearMonth.of(year, month)
    val uiState: StateFlow<HistoryDetailUiState> = combine(
        financeRepository.observeClosure(profileId, period),
        financeRepository.observeMovements(profileId, period)
    ) { closure, movements ->
        HistoryDetailUiState(
            closure = closure,
            expenses = movements
                .filter { it.type == MovementType.EXPENSE }
                .sortedByDescending { it.occurredEpochDay }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailUiState())
    val events = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun reopen() {
        viewModelScope.launch {
            runCatching { financeRepository.reopenMonth(profileId, period) }
                .onSuccess { events.emit("Mes reabierto") }
                .onFailure { events.emit(it.message ?: "No se pudo reabrir") }
        }
    }
}
