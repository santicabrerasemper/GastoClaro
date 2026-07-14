package com.santiago.gastoclaro.ui.payments

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.core.util.formatDate
import com.santiago.gastoclaro.core.util.formatUsdCurrency
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.ui.components.EmptyState
import com.santiago.gastoclaro.ui.components.MonthSelector
import java.time.LocalDate

@Composable
fun PaymentMethodsScreen(
    viewModel: PaymentMethodsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var archiveTarget by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var expandedStatementId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Agregar") }
            )
        }
    ) { innerPadding ->
        if (state.methods.isEmpty()) {
            EmptyState(
                emoji = "💳",
                title = "Sin medios de pago",
                message = "Agrega tarjetas, cuentas o efectivo para ordenar tus gastos.",
                actionLabel = "Agregar medio",
                onAction = { showDialog = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    MonthSelector(
                        period = state.period,
                        onPrevious = { viewModel.selectMonth(state.period.minusMonths(1)) },
                        onNext = { viewModel.selectMonth(state.period.plusMonths(1)) }
                    )
                }
                items(state.methods, key = { it.id }) { method ->
                    PaymentMethodCard(
                        method = method,
                        statement = state.statements.firstOrNull { it.paymentMethodId == method.id },
                        expanded = expandedStatementId == method.id,
                        onToggleStatement = {
                            expandedStatementId = if (expandedStatementId == method.id) null else method.id
                        },
                        onEdit = {
                            editing = method
                            showDialog = true
                        },
                        onArchive = { archiveTarget = method }
                    )
                }
            }
        }
    }

    if (showDialog) {
        PaymentMethodDialog(
            method = editing,
            onDismiss = { showDialog = false },
            onSave = { name, kind, digits, closing, due ->
                viewModel.save(editing?.id, name, kind, digits, closing, due)
                showDialog = false
            }
        )
    }

    archiveTarget?.let { method ->
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            title = { Text("Archivar medio") },
            text = { Text("Se va a ocultar ${method.displayName()} para nuevos movimientos. Los movimientos ya cargados no se pierden.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archive(method.id)
                    archiveTarget = null
                }) { Text("Archivar") }
            },
            dismissButton = { TextButton(onClick = { archiveTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodEntity,
    statement: CardStatementUi?,
    expanded: Boolean,
    onToggleStatement: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(method.icon(), contentDescription = null)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(method.displayName(), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = {}, label = { Text(method.kindLabel()) })
                        if (method.closingDay != null) AssistChip(onClick = {}, label = { Text("Cierra ${method.closingDay}") })
                        if (method.dueDay != null) AssistChip(onClick = {}, label = { Text("Vence ${method.dueDay}") })
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onArchive) {
                    Icon(Icons.Rounded.Archive, contentDescription = "Archivar")
                }
            }
            if (statement != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Resumen actual: ${statement.totalCents.formatCurrency()}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${statement.movementCount} consumos · ${statement.periodStart.formatDate()} a ${statement.periodEnd.formatDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                statement.dueDate?.let {
                    Text(
                        "Vence: ${it.formatDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onToggleStatement) {
                    Text(if (expanded) "Ocultar resumen" else "Ver resumen")
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
                if (expanded) {
                    CardStatementMovements(statement)
                }
            }
        }
    }
}

@Composable
private fun CardStatementMovements(statement: CardStatementUi) {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    if (statement.movements.isEmpty()) {
        Text(
            "Sin consumos en este resumen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            statement.movements.forEach { movement ->
                StatementMovementRow(movement)
            }
        }
    }
}

@Composable
private fun StatementMovementRow(movement: MovementRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = Color(movement.categoryColorArgb).copy(alpha = 0.18f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(movement.categoryEmoji)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movement.note.ifBlank { movement.categoryName },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    movement.categoryName,
                    movement.subcategoryName.takeIf { it.isNotBlank() },
                    if (movement.currency == "USD") movement.currencyAmountCents.formatUsdCurrency() else null,
                    if (movement.installmentCount > 1) "Cuota ${movement.installmentIndex}/${movement.installmentCount}" else null,
                    if (movement.isRecurringMonthly) "Mensual" else null,
                    LocalDate.ofEpochDay(movement.occurredEpochDay).formatDate()
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            (if (movement.type == MovementType.EXPENSE) "-" else "+") + movement.amountCents.formatCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaymentMethodDialog(
    method: PaymentMethodEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int?, Int?) -> Unit
) {
    var name by rememberSaveable(method?.id) { mutableStateOf(method?.name.orEmpty()) }
    var kind by rememberSaveable(method?.id) { mutableStateOf(method?.kind ?: "CREDIT_CARD") }
    var digits by rememberSaveable(method?.id) { mutableStateOf(method?.lastDigits.orEmpty()) }
    var closingDay by rememberSaveable(method?.id) { mutableStateOf(method?.closingDay?.toString().orEmpty()) }
    var dueDay by rememberSaveable(method?.id) { mutableStateOf(method?.dueDay?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (method == null) "Nuevo medio de pago" else "Editar medio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "CREDIT_CARD", onClick = { kind = "CREDIT_CARD" }, label = { Text("Credito") })
                    FilterChip(
                        selected = kind == "DEBIT_CARD",
                        onClick = {
                            kind = "DEBIT_CARD"
                            closingDay = ""
                            dueDay = ""
                        },
                        label = { Text("Debito") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == "BANK",
                        onClick = {
                            kind = "BANK"
                            closingDay = ""
                            dueDay = ""
                        },
                        label = { Text("Cuenta") }
                    )
                    FilterChip(
                        selected = kind == "CASH",
                        onClick = {
                            kind = "CASH"
                            digits = ""
                            closingDay = ""
                            dueDay = ""
                        },
                        label = { Text("Efectivo") }
                    )
                }
                if (kind == "CASH") {
                    Text(
                        "Se va a guardar como Efectivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Nombre") }, singleLine = true)
                }
                if (kind != "CASH") {
                    OutlinedTextField(
                        value = digits,
                        onValueChange = { digits = it.filter(Char::isDigit).take(4) },
                        label = { Text("Ultimos digitos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                if (kind == "CREDIT_CARD") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = closingDay,
                            onValueChange = { closingDay = it.filter(Char::isDigit).take(2) },
                            label = { Text("Cierre") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueDay,
                            onValueChange = { dueDay = it.filter(Char::isDigit).take(2) },
                            label = { Text("Vence") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name,
                    kind,
                    if (kind == "CASH") "" else digits,
                    if (kind == "CREDIT_CARD") closingDay.toIntOrNull() else null,
                    if (kind == "CREDIT_CARD") dueDay.toIntOrNull() else null
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun PaymentMethodEntity.displayName(): String =
    if (kind == "CASH" || lastDigits.isBlank()) name else "$name ****$lastDigits"

private fun PaymentMethodEntity.icon() = when (kind) {
    "CREDIT_CARD" -> Icons.Rounded.CreditCard
    "DEBIT_CARD" -> Icons.Rounded.CreditCard
    "BANK" -> Icons.Rounded.AccountBalance
    "CASH" -> Icons.Rounded.Payments
    else -> Icons.Rounded.CreditCard
}

private fun PaymentMethodEntity.kindLabel(): String = when (kind) {
    "CREDIT_CARD" -> "Credito"
    "DEBIT_CARD" -> "Debito"
    "BANK" -> "Cuenta"
    "CASH" -> "Efectivo"
    else -> "Otro"
}
