package com.santiago.gastoclaro.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.displayName
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.ui.components.EmptyState
import java.time.YearMonth

@Composable
fun HistoryScreen(
    onOpenMonth: (YearMonth) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val closures by viewModel.closures.collectAsStateWithLifecycle()
    if (closures.isEmpty()) {
        EmptyState(
            emoji = "🗓️",
            title = "Todavía no hay meses cerrados",
            message = "Cuando termine un mes, la app guardará automáticamente su resumen en este historial.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Historial mensual", style = MaterialTheme.typography.headlineMedium) }
        items(closures, key = { it.id }) { closure ->
            val period = YearMonth.of(closure.year, closure.month)
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenMonth(period) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(period.displayName(), style = MaterialTheme.typography.titleLarge)
                        Text("${closure.movementCount} movimientos · Gastos ${closure.expenseCents.formatCurrency()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saldo final", style = MaterialTheme.typography.bodySmall)
                        Text(closure.balanceCents.formatCurrency(), fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}
