package com.santiago.gastoclaro.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.displayName
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow
import com.santiago.gastoclaro.ui.components.DonutChart
import com.santiago.gastoclaro.ui.components.MovementCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    onReopened: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            snackbar.showSnackbar(it)
            if (it == "Mes reabierto") onReopened()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.period.displayName()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val data = state.closure
        val closure = data?.closure
        if (closure == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
                Text("El mes ya no está cerrado.")
            }
        } else {
            val categories = data.categories.mapIndexed { index, item ->
                CategoryTotalRow(index.toLong(), item.categoryName, item.categoryEmoji, item.colorArgb, item.amountCents)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Resumen congelado", style = MaterialTheme.typography.titleLarge)
                            SummaryRow("Presupuesto inicial", closure.initialAmountCents.formatCurrency())
                            SummaryRow("Ingresos", closure.incomeCents.formatCurrency())
                            SummaryRow("Gastos", closure.expenseCents.formatCurrency())
                            SummaryRow("Ahorro", closure.savingCents.formatCurrency())
                            SummaryRow("Saldo final", closure.balanceCents.formatCurrency())
                            SummaryRow("Movimientos", closure.movementCount.toString())
                        }
                    }
                }
                item {
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("Distribución de gastos", style = MaterialTheme.typography.titleLarge)
                            DonutChart(categories, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                item {
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gastos del mes", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${state.expenses.size} gastos registrados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(state.expenses, key = { it.id }) { movement ->
                    MovementCard(movement = movement, onClick = null)
                }
                item {
                    Button(onClick = viewModel::reopen, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null)
                        Text("Reabrir mes para corregir")
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
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
