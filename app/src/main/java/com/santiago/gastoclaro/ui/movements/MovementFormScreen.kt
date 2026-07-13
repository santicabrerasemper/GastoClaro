package com.santiago.gastoclaro.ui.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatDate
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementFormScreen(
    initialType: MovementType?,
    onBack: () -> Unit,
    viewModel: MovementFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var subcategoryExpanded by rememberSaveable { mutableStateOf(false) }
    var paymentExpanded by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialType, state.movementId) {
        if (state.movementId == null && initialType != null && state.type != initialType) viewModel.setType(initialType)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MovementFormEvent.Saved -> onBack()
                is MovementFormEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.movementId == null) "Nuevo movimiento" else "Editar movimiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Tipo", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = state.type == MovementType.EXPENSE,
                        onClick = { viewModel.setType(MovementType.EXPENSE) },
                        label = { Text("Gasto") }
                    )
                    FilterChip(
                        selected = state.type == MovementType.INCOME,
                        onClick = { viewModel.setType(MovementType.INCOME) },
                        label = { Text("Ingreso") }
                    )
                }
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = viewModel::setAmount,
                    label = { Text("Monto") },
                    prefix = { Text(if (state.currency == "USD") "USD" else "$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = state.currency == "ARS",
                        onClick = { viewModel.setCurrency("ARS") },
                        label = { Text("Pesos") }
                    )
                    FilterChip(
                        selected = state.currency == "USD",
                        onClick = { viewModel.setCurrency("USD") },
                        label = { Text("Dólares") }
                    )
                }
                if (state.currency == "USD") {
                    OutlinedTextField(
                        value = state.exchangeRateText,
                        onValueChange = viewModel::setExchangeRate,
                        label = { Text("Cotización") },
                        prefix = { Text("$") },
                        supportingText = {
                            Text(
                                state.convertedAmountCents?.let { "Impacto estimado: ${it.formatCurrency()}" }
                                    ?: "Valor del dólar que querés usar para convertir a pesos"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.canUseRecurringMonthly) {
                    FilterChip(
                        selected = state.isRecurringMonthly,
                        onClick = { viewModel.setRecurringMonthly(!state.isRecurringMonthly) },
                        label = { Text("Pago mensual") }
                    )
                    if (state.isRecurringMonthly) {
                        Text(
                            "Se crearán pagos mensuales automáticos durante 36 meses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            state.categories.firstOrNull { it.id == state.selectedCategoryId }
                                ?.let { "${it.emoji} ${it.name}" }
                                ?: "Elegir categoría",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.name}") },
                                onClick = { viewModel.setCategory(category.id); categoryExpanded = false }
                            )
                        }
                    }
                }
                if (state.subcategories.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { subcategoryExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(
                                state.selectedSubcategoryName.ifBlank { "Sin subcategoría" },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = subcategoryExpanded, onDismissRequest = { subcategoryExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Sin subcategoría") },
                                onClick = { viewModel.setSubcategory(""); subcategoryExpanded = false }
                            )
                            state.subcategories.forEach { subcategory ->
                                DropdownMenuItem(
                                    text = { Text(subcategory) },
                                    onClick = { viewModel.setSubcategory(subcategory); subcategoryExpanded = false }
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedPaymentMethod = state.paymentMethods.firstOrNull { it.id == state.selectedPaymentMethodId }
                    OutlinedButton(
                        onClick = { paymentExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(selectedPaymentMethod.icon(), contentDescription = null)
                        Text(
                            selectedPaymentMethod?.displayName() ?: "Sin medio de pago",
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Sin medio de pago") },
                            onClick = { viewModel.setPaymentMethod(null); paymentExpanded = false }
                        )
                        state.paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName()) },
                                onClick = { viewModel.setPaymentMethod(method.id); paymentExpanded = false }
                            )
                        }
                    }
                }
                if (state.type == MovementType.EXPENSE) {
                    val selectedPaymentMethod = state.paymentMethods.firstOrNull { it.id == state.selectedPaymentMethodId }
                    val canUseInstallments = selectedPaymentMethod?.kind == "CREDIT_CARD"
                    if (state.movementId == null && !state.isRecurringMonthly) {
                        Text("Cuotas", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 3, 6, 12).forEach { count ->
                                FilterChip(
                                    selected = state.installmentCount == count,
                                    onClick = { viewModel.setInstallmentCount(count) },
                                    enabled = count == 1 || canUseInstallments,
                                    label = { Text(if (count == 1) "Sin cuotas" else "${count}x") }
                                )
                            }
                        }
                        if (!canUseInstallments) {
                            Text(
                                "Elegí una tarjeta para cargar cuotas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (state.installmentCount > 1) {
                        Text(
                            "Cuota ${state.installmentIndex}/${state.installmentCount}: editás solo este movimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.installmentCount > 1 && state.movementId == null) {
                        Text(
                            "Se crearán ${state.installmentCount} cuotas mensuales desde la fecha elegida.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Fecha: ${state.occurredOn.formatDate()}", modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                }
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = { Text("Nota opcional") },
                    supportingText = { Text("${state.note.length}/120") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.height(22.dp))
                    else {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Text(if (state.movementId == null) "Guardar movimiento" else "Guardar cambios")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occurredOn.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    if (selected != null && !selected.isAfter(LocalDate.now())) viewModel.setDate(selected)
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
}

private fun PaymentMethodEntity.displayName(): String =
    if (kind == "CASH" || lastDigits.isBlank()) name else "$name ****$lastDigits"

private fun PaymentMethodEntity?.icon() = when (this?.kind) {
    "CREDIT_CARD" -> Icons.Rounded.CreditCard
    "DEBIT_CARD" -> Icons.Rounded.CreditCard
    "BANK" -> Icons.Rounded.AccountBalance
    "CASH" -> Icons.Rounded.Payments
    else -> Icons.Rounded.CreditCard
}
