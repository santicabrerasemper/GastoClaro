package com.santiago.gastoclaro.ui.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.ui.components.EmptyState
import com.santiago.gastoclaro.ui.components.MonthSelector
import com.santiago.gastoclaro.ui.components.MovementCard
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    onSelectMonth: (YearMonth) -> Unit,
    onAddMovement: (MovementType) -> Unit,
    onEditMovement: (Long) -> Unit,
    viewModel: MovementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var categoryMenu by rememberSaveable { mutableStateOf(false) }
    var paymentMenu by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.isClosed) {
                FloatingActionButton(onClick = { onAddMovement(MovementType.EXPENSE) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Agregar gasto")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            MonthSelector(
                period = state.period,
                onPrevious = { onSelectMonth(state.period.minusMonths(1)) },
                onNext = { onSelectMonth(state.period.plusMonths(1)) }
            )
            if (state.isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Text("Mes cerrado: búsqueda disponible, edición bloqueada")
                }
            }
            SearchBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Buscar movimientos…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {}
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = state.typeFilter == null, onClick = { viewModel.setTypeFilter(null) }, label = { Text("Todos") })
                FilterChip(
                    selected = state.typeFilter == MovementType.EXPENSE,
                    onClick = { viewModel.setTypeFilter(MovementType.EXPENSE) },
                    label = { Text("Gastos") },
                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) }
                )
                FilterChip(
                    selected = state.typeFilter == MovementType.INCOME,
                    onClick = { viewModel.setTypeFilter(MovementType.INCOME) },
                    label = { Text("Ingresos") },
                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) }
                )
            }
            Box {
                FilterChip(
                    selected = state.categoryFilter != null,
                    onClick = { categoryMenu = true },
                    label = { Text(state.categories.firstOrNull { it.id == state.categoryFilter }?.name ?: "Categoría") },
                    leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                    trailingIcon = if (state.categoryFilter != null) ({
                        IconButton(onClick = { viewModel.setCategoryFilter(null) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar filtro")
                        }
                    }) else null
                )
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    DropdownMenuItem(text = { Text("Todas") }, onClick = {
                        viewModel.setCategoryFilter(null); categoryMenu = false
                    })
                    state.categories.forEach { category ->
                        DropdownMenuItem(text = { Text("${category.emoji} ${category.name}") }, onClick = {
                            viewModel.setCategoryFilter(category.id); categoryMenu = false
                        })
                    }
                }
            }
            Box(modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = state.paymentMethodFilter != null,
                    onClick = { paymentMenu = true },
                    label = {
                        Text(
                            state.paymentMethods.firstOrNull { it.id == state.paymentMethodFilter }
                                ?.let { if (it.lastDigits.isBlank()) it.name else "${it.name} ****${it.lastDigits}" }
                                ?: "Medio de pago"
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
                    trailingIcon = if (state.paymentMethodFilter != null) ({
                        IconButton(onClick = { viewModel.setPaymentMethodFilter(null) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar filtro")
                        }
                    }) else null
                )
                DropdownMenu(expanded = paymentMenu, onDismissRequest = { paymentMenu = false }) {
                    DropdownMenuItem(text = { Text("Todos") }, onClick = {
                        viewModel.setPaymentMethodFilter(null); paymentMenu = false
                    })
                    state.paymentMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(if (method.lastDigits.isBlank()) method.name else "${method.name} ****${method.lastDigits}") },
                            onClick = { viewModel.setPaymentMethodFilter(method.id); paymentMenu = false }
                        )
                    }
                }
            }
            Text(
                text = "Balance visible mensualizado: ${state.totalVisibleCents.formatCurrency()}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            if (state.movements.isEmpty()) {
                EmptyState(
                    emoji = if (state.query.isBlank()) "🧾" else "🔎",
                    title = if (state.query.isBlank()) "No hay movimientos" else "Sin resultados",
                    message = if (state.query.isBlank()) "Agregá un gasto o ingreso para este mes." else "Probá con otra descripción, categoría o monto.",
                    actionLabel = if (state.query.isBlank()) "Agregar gasto" else null,
                    onAction = if (state.query.isBlank()) ({ onAddMovement(MovementType.EXPENSE) }) else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(state.movements, key = { it.id }) { movement ->
                        MovementCard(
                            movement = movement,
                            onClick = if (state.isClosed) null else ({ onEditMovement(movement.id) }),
                            onDelete = if (state.isClosed) null else ({ deleteTarget = movement.id })
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar movimiento") },
            text = { Text("Esta acción actualizará el saldo y las estadísticas del mes.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMovement(id); deleteTarget = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}
