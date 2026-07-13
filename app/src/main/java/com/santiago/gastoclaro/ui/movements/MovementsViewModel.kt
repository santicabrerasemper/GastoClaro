package com.santiago.gastoclaro.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.core.util.parseMoneyToCents
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.YearMonth
import javax.inject.Inject

data class MovementsUiState(
    val profileId: Long? = null,
    val period: YearMonth = YearMonth.now(),
    val query: String = "",
    val typeFilter: MovementType? = null,
    val categoryFilter: Long? = null,
    val paymentMethodFilter: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val movements: List<MovementRow> = emptyList(),
    val totalVisibleCents: Long = 0,
    val isClosed: Boolean = false
)

private data class MovementSource(
    val profileId: Long?,
    val period: YearMonth,
    val categories: List<CategoryEntity>,
    val paymentMethods: List<PaymentMethodEntity>,
    val movements: List<MovementRow>,
    val isClosed: Boolean
)

@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    activeProfileStore: ActiveProfileStore
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<MovementType?>(null)
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val paymentMethodFilter = MutableStateFlow<Long?>(null)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val source = combine(
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profileId, period -> profileId to period }
        .flatMapLatest { (profileId, period) ->
            if (profileId == null) {
                flowOf(MovementSource(null, period, emptyList(), emptyList(), emptyList(), false))
            } else {
                combine(
                    financeRepository.observeCategories(profileId),
                    financeRepository.observePaymentMethods(profileId),
                    financeRepository.observeMovements(profileId, period),
                    financeRepository.observeClosure(profileId, period)
                ) { categories, paymentMethods, movements, closure ->
                    MovementSource(profileId, period, categories, paymentMethods, movements, closure != null)
                }
            }
        }

    val uiState: StateFlow<MovementsUiState> = combine(
        source,
        query,
        typeFilter,
        categoryFilter,
        paymentMethodFilter
    ) { sourceValue, currentQuery, currentType, currentCategory, currentPaymentMethod ->
        val normalizedQuery = currentQuery.normalized()
        val exactAmount = parseMoneyToCents(currentQuery)
        val filtered = sourceValue.movements.filter { movement ->
            val matchesType = currentType == null || movement.type == currentType
            val matchesCategory = currentCategory == null || movement.categoryId == currentCategory
            val matchesPaymentMethod = currentPaymentMethod == null || movement.paymentMethodId == currentPaymentMethod
            val matchesSearch = normalizedQuery.isBlank() ||
                movement.note.normalized().contains(normalizedQuery) ||
                movement.categoryName.normalized().contains(normalizedQuery) ||
                movement.paymentMethodName.orEmpty().normalized().contains(normalizedQuery) ||
                (exactAmount != null && movement.amountCents == exactAmount)
            matchesType && matchesCategory && matchesPaymentMethod && matchesSearch
        }
        MovementsUiState(
            profileId = sourceValue.profileId,
            period = sourceValue.period,
            query = currentQuery,
            typeFilter = currentType,
            categoryFilter = currentCategory,
            paymentMethodFilter = currentPaymentMethod,
            categories = sourceValue.categories,
            paymentMethods = sourceValue.paymentMethods,
            movements = filtered,
            totalVisibleCents = filtered.sumOf {
                if (it.type == MovementType.EXPENSE) -it.monthlyImpactCents else it.amountCents
            },
            isClosed = sourceValue.isClosed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovementsUiState())

    fun setQuery(value: String) { query.value = value }
    fun setTypeFilter(value: MovementType?) { typeFilter.value = value }
    fun setCategoryFilter(value: Long?) { categoryFilter.value = value }
    fun setPaymentMethodFilter(value: Long?) { paymentMethodFilter.value = value }

    fun deleteMovement(id: Long) {
        val state = uiState.value
        val profileId = state.profileId ?: return
        if (state.isClosed) {
            messages.tryEmit("El mes está cerrado")
            return
        }
        viewModelScope.launch {
            runCatching { financeRepository.deleteMovement(profileId, id) }
                .onSuccess { messages.emit("Movimiento eliminado") }
                .onFailure { messages.emit(it.message ?: "No se pudo eliminar") }
        }
    }

    private fun String.normalized(): String = Normalizer.normalize(lowercase().trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}
