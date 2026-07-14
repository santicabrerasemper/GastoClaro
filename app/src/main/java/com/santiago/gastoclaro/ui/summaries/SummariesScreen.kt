package com.santiago.gastoclaro.ui.summaries

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.core.util.formatDate
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.ui.components.DonutChart
import com.santiago.gastoclaro.ui.components.MonthSelector
import com.santiago.gastoclaro.ui.components.MovementCard
import com.santiago.gastoclaro.ui.payments.CardStatementUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummariesScreen(
    viewModel: SummariesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelector(
            period = state.period,
            onPrevious = { viewModel.selectMonth(state.period.minusMonths(1)) },
            onNext = { viewModel.selectMonth(state.period.plusMonths(1)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Mensual") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Tarjetas") }
            )
        }
        if (selectedTab == 0) {
            MonthlySummaryTab(state)
        } else {
            CardsSummaryTab(state)
        }
    }
}

@Composable
private fun MonthlySummaryTab(state: SummariesUiState) {
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
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ahorro del mes", style = MaterialTheme.typography.titleLarge)
                    SummaryRow("Total ahorrado", state.summary.savingCents.formatCurrency())
                    Text(
                        "${state.savings.size} movimientos de ahorro",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state.savings.isNotEmpty()) {
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
        item {
            Text("Gastos del mes", style = MaterialTheme.typography.titleLarge)
        }
        if (state.expenses.isEmpty()) {
            item {
                Card {
                    Text(
                        "Todavia no hay gastos registrados para este mes.",
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(
                        "No hay tarjetas de credito cargadas.",
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
