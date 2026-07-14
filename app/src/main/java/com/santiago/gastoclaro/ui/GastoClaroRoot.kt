package com.santiago.gastoclaro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.ui.dashboard.DashboardScreen
import com.santiago.gastoclaro.ui.history.HistoryDetailScreen
import com.santiago.gastoclaro.ui.history.HistoryScreen
import com.santiago.gastoclaro.ui.movements.MovementFormScreen
import com.santiago.gastoclaro.ui.movements.MovementsScreen
import com.santiago.gastoclaro.ui.payments.PaymentMethodsScreen
import com.santiago.gastoclaro.ui.profiles.ProfilesScreen
import com.santiago.gastoclaro.ui.summaries.SummariesScreen
import java.time.YearMonth

private object Routes {
    const val Dashboard = "dashboard"
    const val Movements = "movements"
    const val Payments = "payments"
    const val Summaries = "summaries"
    const val History = "history"
    const val Profiles = "profiles"
    const val MovementForm = "movement_form/{profileId}/{movementId}?type={type}"
    const val HistoryDetail = "history_detail/{profileId}/{year}/{month}"

    fun movementForm(profileId: Long, movementId: Long = -1L, type: MovementType? = null): String =
        "movement_form/$profileId/$movementId" + (type?.let { "?type=${it.name}" } ?: "")

    fun historyDetail(profileId: Long, period: YearMonth): String =
        "history_detail/$profileId/${period.year}/${period.monthValue}"
}

private data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.Dashboard, "Ini.", Icons.Rounded.Home),
    BottomItem(Routes.Movements, "Mov.", Icons.Rounded.ReceiptLong),
    BottomItem(Routes.Payments, "Med.", Icons.Rounded.CreditCard),
    BottomItem(Routes.Summaries, "Res.", Icons.Rounded.PieChart),
    BottomItem(Routes.History, "Hist.", Icons.Rounded.History),
    BottomItem(Routes.Profiles, "Perf.", Icons.Rounded.People)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoClaroRoot(viewModel: AppViewModel = hiltViewModel()) {
    val appState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshClosures()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (appState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (appState.profiles.isEmpty()) {
        ProfilesScreen(
            activeProfileId = null,
            onboarding = true,
            onSelectProfile = viewModel::selectProfile
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val isMainDestination = bottomItems.any { it.route == route }

    Scaffold(
        topBar = {
            if (isMainDestination) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            when (route) {
                                Routes.Dashboard -> "GastoClaro"
                                Routes.Movements -> "Movimientos"
                                Routes.Payments -> "Medios"
                                Routes.Summaries -> "Resumenes"
                                Routes.History -> "Historial"
                                Routes.Profiles -> "Perfiles"
                                else -> "GastoClaro"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        ProfileSwitcher(
                            profiles = appState.profiles,
                            activeProfileId = appState.activeProfileId,
                            onSelect = viewModel::selectProfile
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (isMainDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Clip) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    onSelectMonth = viewModel::selectMonth,
                    onAddMovement = { type ->
                        appState.activeProfileId?.let { navController.navigate(Routes.movementForm(it, type = type)) }
                    },
                    onOpenMovement = { id ->
                        appState.activeProfileId?.let { navController.navigate(Routes.movementForm(it, id)) }
                    },
                    onOpenMovements = { navController.navigate(Routes.Movements) }
                )
            }
            composable(Routes.Movements) {
                MovementsScreen(
                    onSelectMonth = viewModel::selectMonth,
                    onAddMovement = { type ->
                        appState.activeProfileId?.let { navController.navigate(Routes.movementForm(it, type = type)) }
                    },
                    onEditMovement = { id ->
                        appState.activeProfileId?.let { navController.navigate(Routes.movementForm(it, id)) }
                    }
                )
            }
            composable(Routes.Payments) {
                PaymentMethodsScreen()
            }
            composable(Routes.Summaries) {
                SummariesScreen()
            }
            composable(Routes.History) {
                HistoryScreen(
                    onOpenMonth = { period ->
                        appState.activeProfileId?.let { navController.navigate(Routes.historyDetail(it, period)) }
                    }
                )
            }
            composable(Routes.Profiles) {
                ProfilesScreen(
                    activeProfileId = appState.activeProfileId,
                    onboarding = false,
                    onSelectProfile = viewModel::selectProfile
                )
            }
            composable(
                route = Routes.MovementForm,
                arguments = listOf(
                    navArgument("profileId") { type = NavType.LongType },
                    navArgument("movementId") { type = NavType.LongType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val initialType = entry.arguments?.getString("type")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { MovementType.valueOf(it) }.getOrNull() }
                MovementFormScreen(initialType = initialType, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.HistoryDetail,
                arguments = listOf(
                    navArgument("profileId") { type = NavType.LongType },
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { entry ->
                val period = YearMonth.of(
                    entry.arguments?.getInt("year") ?: YearMonth.now().year,
                    entry.arguments?.getInt("month") ?: YearMonth.now().monthValue
                )
                HistoryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReopened = {
                        viewModel.selectMonth(period)
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Dashboard) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileSwitcher(
    profiles: List<com.santiago.gastoclaro.data.local.entity.ProfileEntity>,
    activeProfileId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = profiles.firstOrNull { it.id == activeProfileId }
    Row {
        IconButton(onClick = { expanded = true }) {
            if (active == null) Icon(Icons.Rounded.AccountCircle, contentDescription = "Seleccionar perfil")
            else Text(active.emoji, style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cambiar perfil")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text("${profile.emoji} ${profile.name}") },
                    onClick = { onSelect(profile.id); expanded = false }
                )
            }
        }
    }
}
