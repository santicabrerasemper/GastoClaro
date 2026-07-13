package com.santiago.gastoclaro.ui.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.core.util.formatMoneyInput
import com.santiago.gastoclaro.core.util.formatPlainAmount
import com.santiago.gastoclaro.core.util.parseMoneyToCents
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.model.MovementDraft
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MovementFormUiState(
    val movementId: Long? = null,
    val profileId: Long = 0,
    val type: MovementType = MovementType.EXPENSE,
    val amountText: String = "",
    val currency: String = "ARS",
    val exchangeRateText: String = "",
    val convertedAmountCents: Long? = null,
    val note: String = "",
    val occurredOn: LocalDate = LocalDate.now(),
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryName: String = "",
    val selectedPaymentMethodId: Long? = null,
    val useNoPaymentMethod: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<String> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val annualizedMonths: Int = 1,
    val installmentIndex: Int = 1,
    val installmentCount: Int = 1,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface MovementFormEvent {
    data object Saved : MovementFormEvent
    data class Error(val message: String) : MovementFormEvent
}

@HiltViewModel
class MovementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    private val profileId: Long = checkNotNull(savedStateHandle["profileId"])
    private val movementIdArg: Long = savedStateHandle["movementId"] ?: -1L
    private val form = MutableStateFlow(
        MovementFormUiState(
            movementId = movementIdArg.takeIf { it > 0 },
            profileId = profileId,
            isLoading = movementIdArg > 0
        )
    )
    val events = MutableSharedFlow<MovementFormEvent>(extraBufferCapacity = 1)

    private val categories = form
        .map { it.type }
        .distinctUntilChanged()
        .flatMapLatest { type -> financeRepository.observeCategories(profileId, type) }

    private val paymentMethods = financeRepository.observePaymentMethods(profileId)

    val uiState: StateFlow<MovementFormUiState> = combine(form, categories, paymentMethods) { latest, availableCategories, availablePaymentMethods ->
        val selected = latest.selectedCategoryId?.takeIf { id -> availableCategories.any { it.id == id } }
            ?: availableCategories.firstOrNull()?.id
        val selectedCategory = availableCategories.firstOrNull { it.id == selected }
        val availableSubcategories = selectedCategory.subcategoriesForCategory()
        val selectedSubcategory = latest.selectedSubcategoryName.takeIf { it in availableSubcategories } ?: ""
        val convertedAmount = latest.convertedAmount()
        val selectedPayment = if (latest.useNoPaymentMethod) {
            null
        } else {
            latest.selectedPaymentMethodId?.takeIf { id -> availablePaymentMethods.any { it.id == id } }
        }
        latest.copy(
            categories = availableCategories,
            selectedCategoryId = selected,
            selectedSubcategoryName = selectedSubcategory,
            subcategories = availableSubcategories,
            convertedAmountCents = convertedAmount,
            paymentMethods = availablePaymentMethods,
            selectedPaymentMethodId = selectedPayment
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        if (movementIdArg > 0) {
            viewModelScope.launch {
                val movement = financeRepository.getMovement(profileId, movementIdArg)
                if (movement == null) {
                    events.emit(MovementFormEvent.Error("El movimiento ya no existe"))
                    form.value = form.value.copy(isLoading = false)
                } else {
                    form.value = form.value.copy(
                        movementId = movement.id,
                        type = movement.type,
                        amountText = formatMoneyInput(movement.currencyAmountCents.formatPlainAmount()),
                        currency = movement.currency,
                        exchangeRateText = movement.exchangeRateCents?.let { formatMoneyInput(it.formatPlainAmount()) } ?: "",
                        note = movement.note,
                        occurredOn = LocalDate.ofEpochDay(movement.occurredEpochDay),
                        selectedCategoryId = movement.categoryId,
                        selectedSubcategoryName = movement.subcategoryName,
                        selectedPaymentMethodId = movement.paymentMethodId,
                        useNoPaymentMethod = movement.paymentMethodId == null,
                        annualizedMonths = movement.annualizedMonths,
                        installmentIndex = movement.installmentIndex,
                        installmentCount = movement.installmentCount,
                        isLoading = false
                    )
                }
            }
        } else {
            viewModelScope.launch {
                val selectedPeriod = activeProfileStore.selectedMonth.first()
                val today = LocalDate.now()
                val currentPeriod = YearMonth.from(today)
                val suggestedDate = when {
                    selectedPeriod == currentPeriod -> today
                    selectedPeriod.isAfter(currentPeriod) -> today
                    else -> selectedPeriod.atDay(minOf(today.dayOfMonth, selectedPeriod.lengthOfMonth()))
                }
                form.value = form.value.copy(occurredOn = suggestedDate)
            }
        }
    }

    fun setType(type: MovementType) { form.value = form.value.copy(type = type, selectedCategoryId = null, selectedSubcategoryName = "") }
    fun setAmount(value: String) { form.value = form.value.copy(amountText = formatMoneyInput(value)) }
    fun setCurrency(value: String) {
        val currency = value.takeIf { it == "ARS" || it == "USD" } ?: "ARS"
        form.value = form.value.copy(
            currency = currency,
            exchangeRateText = if (currency == "USD") form.value.exchangeRateText else ""
        )
    }
    fun setExchangeRate(value: String) { form.value = form.value.copy(exchangeRateText = formatMoneyInput(value)) }
    fun setNote(value: String) { form.value = form.value.copy(note = value.take(120)) }
    fun setDate(value: LocalDate) { form.value = form.value.copy(occurredOn = value) }
    fun setCategory(id: Long) { form.value = form.value.copy(selectedCategoryId = id, selectedSubcategoryName = "") }
    fun setSubcategory(value: String) { form.value = form.value.copy(selectedSubcategoryName = value.take(60)) }
    fun setPaymentMethod(id: Long?) {
        val selected = form.value.paymentMethods.firstOrNull { it.id == id }
        form.value = form.value.copy(
            selectedPaymentMethodId = id,
            useNoPaymentMethod = id == null,
            installmentCount = if (selected?.kind == "CREDIT_CARD") form.value.installmentCount else 1
        )
    }
    fun setAnnualizedMonths(value: Int) {
        form.value = form.value.copy(annualizedMonths = value.coerceIn(1, 60), installmentCount = 1)
    }
    fun setInstallmentCount(value: Int) {
        form.value = form.value.copy(installmentCount = value.coerceIn(1, 60), annualizedMonths = 1)
    }

    fun save() {
        val state = uiState.value
        val originalAmount = parseMoneyToCents(state.amountText)
        val exchangeRate = if (state.currency == "USD") parseMoneyToCents(state.exchangeRateText) else null
        val amount = state.convertedAmountCents
        val categoryId = state.selectedCategoryId
        if (originalAmount == null || originalAmount <= 0 || amount == null || amount <= 0) {
            events.tryEmit(MovementFormEvent.Error("Ingresá un monto válido"))
            return
        }
        if (state.currency == "USD" && (exchangeRate == null || exchangeRate <= 0)) {
            events.tryEmit(MovementFormEvent.Error("Ingresá la cotización del dólar"))
            return
        }
        if (categoryId == null) {
            events.tryEmit(MovementFormEvent.Error("Elegí una categoría"))
            return
        }
        val selectedPaymentMethod = state.paymentMethods.firstOrNull { it.id == state.selectedPaymentMethodId }
        if (state.type == MovementType.EXPENSE && state.installmentCount > 1 && selectedPaymentMethod?.kind != "CREDIT_CARD") {
            events.tryEmit(MovementFormEvent.Error("Las cuotas requieren una tarjeta de crédito"))
            return
        }
        viewModelScope.launch {
            form.value = form.value.copy(isSaving = true)
            runCatching {
                financeRepository.saveMovement(
                    MovementDraft(
                        id = state.movementId,
                        profileId = state.profileId,
                        categoryId = categoryId,
                        subcategoryName = state.selectedSubcategoryName,
                        paymentMethodId = state.selectedPaymentMethodId,
                        currency = state.currency,
                        currencyAmountCents = originalAmount,
                        exchangeRateCents = exchangeRate,
                        type = state.type,
                        amountCents = amount,
                        monthlyImpactCents = monthlyImpactFor(amount, state.type, state.annualizedMonths),
                        annualizedMonths = if (state.type == MovementType.EXPENSE && state.installmentCount == 1) state.annualizedMonths else 1,
                        installmentCount = if (state.type == MovementType.EXPENSE && state.movementId == null) state.installmentCount else 1,
                        note = state.note,
                        occurredOn = state.occurredOn
                    )
                )
            }.onSuccess {
                events.emit(MovementFormEvent.Saved)
            }.onFailure {
                events.emit(MovementFormEvent.Error(it.message ?: "No se pudo guardar"))
            }
            form.value = form.value.copy(isSaving = false)
        }
    }

    private fun monthlyImpactFor(amount: Long, type: MovementType, months: Int): Long {
        if (type != MovementType.EXPENSE || months <= 1) return amount
        return ((amount + months - 1) / months).coerceAtLeast(1)
    }

    private fun MovementFormUiState.convertedAmount(): Long? {
        val originalAmount = parseMoneyToCents(amountText) ?: return null
        if (currency == "ARS") return originalAmount
        val rate = parseMoneyToCents(exchangeRateText) ?: return null
        if (originalAmount <= 0 || rate <= 0) return null
        return ((originalAmount * rate) + 50) / 100
    }

    private fun CategoryEntity?.subcategoriesForCategory(): List<String> = when (this?.name) {
        "Comida" -> listOf("Pedidos Ya", "Supermercado", "Salidas a comer")
        "Transporte" -> listOf("Nafta", "Seguro del auto")
        else -> emptyList()
    }
}
