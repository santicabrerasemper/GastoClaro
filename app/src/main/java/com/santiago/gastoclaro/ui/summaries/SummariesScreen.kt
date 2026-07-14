package com.santiago.gastoclaro.ui.summaries

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.core.util.formatDate
import com.santiago.gastoclaro.core.util.formatMoneyInput
import com.santiago.gastoclaro.core.util.formatPlainAmount
import com.santiago.gastoclaro.core.util.parseMoneyToCents
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.ui.components.DonutChart
import com.santiago.gastoclaro.ui.components.EmptyState
import com.santiago.gastoclaro.ui.components.MonthSelector
import com.santiago.gastoclaro.ui.components.MovementCard
import com.santiago.gastoclaro.ui.history.HistoryScreen
import com.santiago.gastoclaro.ui.payments.CardStatementUi
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummariesScreen(
    onOpenMonth: (YearMonth) -> Unit,
    viewModel: SummariesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingGoalUi?>(null) }
    var archiveGoal by remember { mutableStateOf<SavingGoalUi?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MonthSelector(
                period = state.period,
                onPrevious = { viewModel.selectMonth(state.period.minusMonths(1)) },
                onNext = { viewModel.selectMonth(state.period.plusMonths(1)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Mensual") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tarjetas") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Cierres") })
            }
            when (selectedTab) {
                0 -> MonthlySummaryTab(
                    state = state,
                    onCreateGoal = {
                        editingGoal = null
                        showGoalDialog = true
                    },
                    onEditGoal = { goal ->
                        editingGoal = goal
                        showGoalDialog = true
                    },
                    onArchiveGoal = { goal -> archiveGoal = goal }
                )
                1 -> CardsSummaryTab(state)
                else -> HistoryScreen(onOpenMonth = onOpenMonth)
            }
        }
    }

    if (showGoalDialog) {
        SavingGoalDialog(
            goal = editingGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { name, target ->
                viewModel.saveGoal(editingGoal?.id?.takeIf { it > 0 }, name, target)
                showGoalDialog = false
            }
        )
    }

    archiveGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { archiveGoal = null },
            title = { Text("Archivar meta") },
            text = { Text("La meta ${goal.name} deja de mostrarse, pero los aportes quedan guardados como movimientos de ahorro.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveGoal(goal.id)
                    archiveGoal = null
                }) { Text("Archivar") }
            },
            dismissButton = { TextButton(onClick = { archiveGoal = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MonthlySummaryTab(
    state: SummariesUiState,
    onCreateGoal: () -> Unit,
    onEditGoal: (SavingGoalUi) -> Unit,
    onArchiveGoal: (SavingGoalUi) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resumen mensual", style = MaterialTheme.typography.titleLarge)
                    SummaryRow("Presupuesto inicial", state.summary.initialCents.formatCurrency())
                    SummaryRow("Ingresos", state.summary.incomeCents.formatCurrency())
                    SummaryRow("Gastos", state.summary.expenseCents.formatCurrency())
                    SummaryRow("Ahorro", state.summary.savingCents.formatCurrency())
                    SummaryRow("Saldo", state.summary.balanceCents.formatCurrency())
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Metas de ahorro", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${state.savingGoals.size} metas · ${state.savings.size} aportes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = onCreateGoal) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text("Meta")
                        }
                    }
                    SummaryRow("Total ahorrado", state.summary.savingCents.formatCurrency())
                }
            }
        }
        if (state.savingGoals.isEmpty()) {
            item {
                Card {
                    EmptyState(
                        emoji = "🎯",
                        title = "Sin metas de ahorro",
                        message = "Crea una meta con objetivo, por ejemplo Auto, Viaje o Emergencia.",
                        actionLabel = "Crear meta",
                        onAction = onCreateGoal
                    )
                }
            }
        } else {
            items(state.savingGoals, key = { it.id }) { goal ->
                SavingGoalCard(
                    goal = goal,
                    onEdit = if (goal.id > 0) ({ onEditGoal(goal) }) else null,
                    onArchive = if (goal.id > 0) ({ onArchiveGoal(goal) }) else null
                )
            }
        }
        if (state.savings.isNotEmpty()) {
            item { Text("Aportes de ahorro", style = MaterialTheme.typography.titleMedium) }
            items(state.savings, key = { it.id }) { movement ->
                MovementCard(movement = movement, onClick = null)
            }
        }
        item {
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("En que gastaste", style = MaterialTheme.typography.titleLarge)
                    DonutChart(state.categoryTotals, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item { Text("Gastos del mes", style = MaterialTheme.typography.titleLarge) }
        if (state.expenses.isEmpty()) {
            item {
                Card {
                    EmptyState(
                        emoji = "🧾",
                        title = "Sin gastos del mes",
                        message = "Cuando cargues gastos, aca vas a ver el detalle y el grafico por categoria."
                    )
                }
            }
        } else {
            items(state.expenses, key = { it.id }) { movement ->
                MovementCard(movement = movement, onClick = null)
            }
        }
    }
}

@Composable
private fun SavingGoalCard(
    goal: SavingGoalUi,
    onEdit: (() -> Unit)?,
    onArchive: (() -> Unit)?
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressDonut(progress = goal.progress)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Editar meta")
                        }
                    }
                    if (onArchive != null) {
                        IconButton(onClick = onArchive) {
                            Icon(Icons.Rounded.Archive, contentDescription = "Archivar meta")
                        }
                    }
                }
                SummaryRow("Ahorrado", goal.amountCents.formatCurrency())
                SummaryRow("Objetivo", goal.targetCents.formatCurrency())
                SummaryRow("Falta", goal.remainingCents.formatCurrency())
                Text(
                    "${(goal.progress * 100).toInt()}% completado · ${goal.movementCount} aportes",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProgressDonut(progress: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
        Canvas(modifier = Modifier.size(92.dp)) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = (progress * 360f).coerceAtLeast(if (progress > 0f) 3f else 0f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SavingGoalDialog(
    goal: SavingGoalUi?,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var name by rememberSaveable(goal?.id) { mutableStateOf(goal?.name.orEmpty()) }
    var target by rememberSaveable(goal?.id) {
        mutableStateOf(goal?.targetCents?.formatPlainAmount()?.let { formatMoneyInput(it) }.orEmpty())
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Nueva meta de ahorro" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nombre") },
                    placeholder = { Text("Auto, Viaje, Emergencia") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = formatMoneyInput(it) },
                    label = { Text("Objetivo") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val targetCents = parseMoneyToCents(target)
                when {
                    name.trim().length < 2 -> error = "Ingresa un nombre"
                    targetCents == null || targetCents <= 0 -> error = "Ingresa un objetivo valido"
                    else -> onSave(name, targetCents)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CardsSummaryTab(state: SummariesUiState) {
    var expandedCardId by rememberSaveable { mutableStateOf<Long?>(null) }
    val creditCards = state.paymentMethods.filter { it.kind == "CREDIT_CARD" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (creditCards.isEmpty()) {
            item {
                Card {
                    EmptyState(
                        emoji = "💳",
                        title = "Sin tarjetas de credito",
                        message = "Agrega una tarjeta en Medios para ver resumen, cierre, vencimiento y consumos."
                    )
                }
            }
        } else {
            items(creditCards, key = { it.id }) { method ->
                val statement = state.cardStatements.firstOrNull { it.paymentMethodId == method.id }
                CardStatementCard(
                    method = method,
                    statement = statement,
                    expanded = expandedCardId == method.id,
                    onToggle = {
                        expandedCardId = if (expandedCardId == method.id) null else method.id
                    }
                )
            }
        }
    }
}

@Composable
private fun CardStatementCard(
    method: PaymentMethodEntity,
    statement: CardStatementUi?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(method.displayName(), style = MaterialTheme.typography.titleLarge)
            if (statement == null) {
                Text("Sin resumen disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            SummaryRow("Resumen actual", statement.totalCents.formatCurrency())
            Text(
                "${statement.movementCount} consumos · ${statement.periodStart.formatDate()} a ${statement.periodEnd.formatDate()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            statement.dueDate?.let {
                Text("Vence: ${it.formatDate()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Ocultar consumos" else "Ver consumos")
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                if (statement.movements.isEmpty()) {
                    Text("Sin consumos en este resumen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        statement.movements.forEach { movement ->
                            MovementCard(movement = movement, onClick = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun PaymentMethodEntity.displayName(): String =
    if (lastDigits.isBlank()) name else "$name ****$lastDigits"
