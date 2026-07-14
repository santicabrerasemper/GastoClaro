package com.santiago.gastoclaro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.core.util.formatMoneyInput
import com.santiago.gastoclaro.core.util.formatPlainAmount
import com.santiago.gastoclaro.core.util.parseMoneyToCents
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.ui.components.DonutChart
import com.santiago.gastoclaro.ui.components.EmptyState
import com.santiago.gastoclaro.ui.components.MonthSelector
import com.santiago.gastoclaro.ui.components.MovementCard
import kotlinx.coroutines.launch
import java.time.YearMonth

@Composable
fun DashboardScreen(
    onSelectMonth: (YearMonth) -> Unit,
    onAddMovement: (MovementType) -> Unit,
    onOpenMovement: (Long) -> Unit,
    onOpenMovements: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelector(
                    period = state.period,
                    onPrevious = { onSelectMonth(state.period.minusMonths(1)) },
                    onNext = { onSelectMonth(state.period.plusMonths(1)) }
                )
            }
            if (state.isClosed) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                            Text("Este mes está cerrado y se muestra en modo lectura")
                        }
                    }
                }
            }
            if (state.isReopened && state.period.isBefore(YearMonth.now())) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Mes reabierto para correcciones")
                            Button(onClick = viewModel::closeReopenedMonth) { Text("Guardar cambios y cerrar nuevamente") }
                        }
                    }
                }
            }
            item {
                BalanceCard(
                    balance = state.summary.balanceCents,
                    initial = state.summary.initialCents,
                    income = state.summary.incomeCents,
                    expense = state.summary.expenseCents,
                    saving = state.summary.savingCents,
                    onEditBudget = { showBudgetDialog = true },
                    enabled = !state.isClosed
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onAddMovement(MovementType.EXPENSE) },
                        enabled = !state.isClosed,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = null)
                        Text("Gasto", maxLines = 1, fontSize = 13.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAddMovement(MovementType.INCOME) },
                        enabled = !state.isClosed,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Ingreso", maxLines = 1, fontSize = 13.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAddMovement(MovementType.SAVING) },
                        enabled = !state.isClosed,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Ahorro", maxLines = 1, fontSize = 13.sp)
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("¿En qué gastaste?", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(14.dp))
                        DonutChart(state.categoryTotals)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Últimos movimientos", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOpenMovements) { Text("Ver todos") }
                }
            }
            if (state.recentMovements.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🧾",
                        title = "Todavía no hay movimientos",
                        message = "Registrá tu primer gasto o ingreso para empezar.",
                        actionLabel = if (state.isClosed) null else "Agregar gasto",
                        onAction = if (state.isClosed) null else ({ onAddMovement(MovementType.EXPENSE) })
                    )
                }
            } else {
                items(state.recentMovements, key = { it.id }) { movement ->
                    MovementCard(
                        movement = movement,
                        onClick = if (state.isClosed) null else ({ onOpenMovement(movement.id) })
                    )
                }
            }
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentAmount = state.summary.initialCents,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { value ->
                viewModel.updateBudget(value)
                showBudgetDialog = false
            },
            onError = { scope.launch { snackbar.showSnackbar(it) } }
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Long,
    initial: Long,
    income: Long,
    expense: Long,
    saving: Long,
    onEditBudget: () -> Unit,
    enabled: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val gradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF1E6B55),
                Color(0xFF27566F),
                Color(0xFF4C3D67)
            )
        )
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    }
    val foreground = if (isDark) Color(0xFFF4FFF9) else Color.White
    val mutedForeground = foreground.copy(alpha = .78f)
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Saldo disponible", color = mutedForeground)
                    Text(balance.formatCurrency(), style = MaterialTheme.typography.headlineLarge, color = foreground)
                }
                OutlinedButton(onClick = onEditBudget, enabled = enabled) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Text("Base")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Inicial", initial.formatCurrency(), foreground, mutedForeground, Modifier.weight(1f))
                    MiniStat("Ingresos", income.formatCurrency(), foreground, mutedForeground, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Gastos", expense.formatCurrency(), foreground, mutedForeground, Modifier.weight(1f))
                    MiniStat("Ahorro", saving.formatCurrency(), foreground, mutedForeground, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, foreground: Color, mutedForeground: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = mutedForeground, style = MaterialTheme.typography.bodySmall)
        Text(value, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
    }
}

@Composable
private fun BudgetDialog(
    currentAmount: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onError: (String) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf(formatMoneyInput(currentAmount.formatPlainAmount())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Presupuesto inicial") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Modifica el monto base del mes. Los ingresos se registran por separado.")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatMoneyInput(it) },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = parseMoneyToCents(amount)
                if (parsed == null || parsed < 0) onError("Ingresá un monto válido") else onConfirm(parsed)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
