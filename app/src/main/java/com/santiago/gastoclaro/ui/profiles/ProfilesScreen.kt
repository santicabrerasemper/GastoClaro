package com.santiago.gastoclaro.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santiago.gastoclaro.core.util.formatMoneyInput
import com.santiago.gastoclaro.core.util.parseMoneyToCents
import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import kotlinx.coroutines.launch

private val profileColors = listOf(
    0xFF2E7D65.toInt(), 0xFF546E7A.toInt(), 0xFF5E6BC6.toInt(),
    0xFFB65B72.toInt(), 0xFFE18A3B.toInt(), 0xFF6E8B3D.toInt()
)
private val profileEmojis = listOf("👤", "😊", "🏠", "✈️", "💼", "🎯")

@Composable
fun ProfilesScreen(
    activeProfileId: Long?,
    onboarding: Boolean,
    onSelectProfile: (Long) -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProfileEntity?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!onboarding || profiles.isNotEmpty()) {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Crear perfil")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (onboarding) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 18.dp)) {
                        Text("Tu plata, más clara", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Creá un perfil para empezar. Cada perfil mantiene sus presupuestos, gastos e historial completamente separados.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (profiles.isEmpty()) {
                            Button(onClick = { editing = null; showForm = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Text("Crear mi primer perfil")
                            }
                        }
                    }
                }
            } else {
                item { Text("Perfiles", style = MaterialTheme.typography.headlineMedium) }
                item { Text("Usá perfiles distintos para separar finanzas personales, del hogar o de un viaje.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(profiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectProfile(profile.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (profile.id == activeProfileId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(profile.colorArgb)),
                            contentAlignment = Alignment.Center
                        ) { Text(profile.emoji) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (profile.id == activeProfileId) "Perfil activo" else "Tocar para usar", style = MaterialTheme.typography.bodySmall)
                        }
                        if (profile.id == activeProfileId) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { editing = profile; showForm = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { deleteTarget = profile }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        ProfileDialog(
            profile = editing,
            onDismiss = { showForm = false },
            onCreate = { name, emoji, color, initial ->
                viewModel.createProfile(name, emoji, color, initial)
                showForm = false
            },
            onUpdate = { updated ->
                viewModel.updateProfile(updated)
                showForm = false
            },
            onError = { message -> scope.launch { snackbar.showSnackbar(message) } }
        )
    }

    deleteTarget?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar ${profile.name}") },
            text = { Text("Se eliminarán sus presupuestos, movimientos e historial local. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProfile(profile); deleteTarget = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ProfileDialog(
    profile: ProfileEntity?,
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Long) -> Unit,
    onUpdate: (ProfileEntity) -> Unit,
    onError: (String) -> Unit
) {
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var emoji by rememberSaveable(profile?.id) { mutableStateOf(profile?.emoji ?: profileEmojis.first()) }
    var selectedColor by rememberSaveable(profile?.id) { mutableIntStateOf(profile?.colorArgb ?: profileColors.first()) }
    var initialAmount by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "Nuevo perfil" else "Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, label = { Text("Nombre") }, singleLine = true)
                Text("Icono", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profileEmojis.forEach { option ->
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(if (emoji == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center
                        ) { Text(option) }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profileColors.forEach { option ->
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(option))
                                .clickable { selectedColor = option },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == option) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (profile == null) {
                    OutlinedTextField(
                        value = initialAmount,
                        onValueChange = { initialAmount = formatMoneyInput(it) },
                        label = { Text("Monto inicial del mes") },
                        prefix = { Text("$") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().length < 2) {
                    onError("Ingresá un nombre de al menos 2 caracteres")
                    return@TextButton
                }
                if (profile == null) {
                    val initial = if (initialAmount.isBlank()) 0 else parseMoneyToCents(initialAmount)
                    if (initial == null || initial < 0) onError("Ingresá un monto inicial válido")
                    else onCreate(name, emoji, selectedColor, initial)
                } else onUpdate(profile.copy(name = name, emoji = emoji, colorArgb = selectedColor))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
