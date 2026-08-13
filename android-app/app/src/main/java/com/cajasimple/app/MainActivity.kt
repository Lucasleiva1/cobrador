package com.cajasimple.app

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cajasimple.app.ui.screens.cash.CashScreen
import com.cajasimple.app.ui.screens.cash.CashViewModel
import com.cajasimple.app.ui.screens.settings.SettingsScreen
import com.cajasimple.app.ui.screens.settings.SettingsViewModel
import com.cajasimple.app.ui.screens.today.SalesListScreen
import com.cajasimple.app.ui.screens.today.SalesListViewModel
import com.cajasimple.app.ui.theme.CajaTheme
import com.cajasimple.app.update.UpdateInstaller
import com.cajasimple.app.update.UpdateUiState
import com.cajasimple.app.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CajaSimpleApplication).container
        val factory = CajaViewModelFactory(container, applicationContext)
        val cash = ViewModelProvider(this, factory)[CashViewModel::class.java]
        val today = ViewModelProvider(this, factory)["today", SalesListViewModel::class.java]
        val history = ViewModelProvider(this, factory)["history", SalesListViewModel::class.java]
        val settings = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        val updates = ViewModelProvider(this)[UpdateViewModel::class.java]
        setContent {
            val preferences by settings.settings.collectAsState()
            CajaTheme(preferences.theme, preferences.themeMode) {
                CajaApp(cash, today, history, settings, updates)
            }
        }
    }
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("cash", "Caja", Icons.Outlined.PointOfSale),
    Destination("today", "Hoy", Icons.Outlined.Today),
    Destination("history", "Historial", Icons.Outlined.History),
    Destination("settings", "Ajustes", Icons.Outlined.Settings),
)

@Composable
private fun CajaApp(
    cash: CashViewModel,
    today: SalesListViewModel,
    history: SalesListViewModel,
    settingsVm: SettingsViewModel,
    updatesVm: UpdateViewModel,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val settings by settingsVm.settings.collectAsState()
    val updateState by updatesVm.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatesVm.retryInstall()
    }
    LaunchedEffect(Unit) { updatesVm.check(silent = true) }
    LaunchedEffect(updateState) {
        val ready = updateState as? UpdateUiState.ReadyToInstall ?: return@LaunchedEffect
        if (!UpdateInstaller.canInstall(context)) {
            updatesVm.permissionRequired(ready.update, ready.file)
        } else {
            runCatching { UpdateInstaller.launch(context, ready.file) }
                .onSuccess { updatesVm.installationLaunched() }
                .onFailure { updatesVm.installationError(it.message ?: "Android no pudo abrir el instalador.") }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            destinations.forEach { destination ->
                NavigationBarItem(
                    selected = entry?.destination?.route == destination.route,
                    onClick = { nav.navigate(destination.route) { launchSingleTop = true; popUpTo("cash") { saveState = true }; restoreState = true } },
                    icon = { Icon(destination.icon, null) },
                    label = { Text(destination.label) },
                )
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "cash", modifier = Modifier.padding(padding)) {
            composable("cash") {
                CashScreen(
                    cash,
                    settings.mode,
                    settings.businessName,
                    settings.productQuickPrices,
                    settings.paymentQuickAmounts,
                )
            }
            composable("today") { LaunchedEffect(Unit) { today.today() }; SalesListScreen(today, settings.businessName, history = false) }
            composable("history") { SalesListScreen(history, settings.businessName, history = true) }
            composable("settings") { SettingsScreen(settingsVm, updatesVm) }
        }
    }
    UpdateDialogs(
        state = updateState,
        onInstall = updatesVm::download,
        onLater = updatesVm::dismiss,
        onCancelDownload = updatesVm::cancelDownload,
        onOpenPermission = { permissionLauncher.launch(UpdateInstaller.permissionIntent(context)) },
    )
}

@Composable
private fun UpdateDialogs(
    state: UpdateUiState,
    onInstall: (com.cajasimple.app.update.AppUpdate) -> Unit,
    onLater: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenPermission: () -> Unit,
) {
    when (state) {
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("Nueva actualización ${state.update.versionName}") },
            text = {
                Text(
                    state.update.notes.ifBlank {
                        "Hay una nueva versión de Caja Simple disponible. Podés instalarla ahora o hacerlo más tarde desde Ajustes."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { onInstall(state.update) }) { Text("Actualizar ahora") } },
            dismissButton = { TextButton(onClick = onLater) { Text("Más tarde") } },
        )
        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Descargando actualización") },
            text = {
                state.progress?.let { LinearProgressIndicator(progress = { it }) }
                    ?: LinearProgressIndicator()
            },
            confirmButton = { TextButton(onClick = onCancelDownload) { Text("Cancelar") } },
        )
        is UpdateUiState.PermissionRequired -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("Permitir la instalación") },
            text = { Text("Android necesita que autorices a Caja Simple para abrir su actualización. Después volverás a la aplicación para continuar.") },
            confirmButton = { TextButton(onClick = onOpenPermission) { Text("Abrir configuración") } },
            dismissButton = { TextButton(onClick = onLater) { Text("Más tarde") } },
        )
        is UpdateUiState.Error -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("No se pudo actualizar") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onLater) { Text("Aceptar") } },
        )
        else -> Unit
    }
}

private class CajaViewModelFactory(private val container: AppContainer, private val context: android.content.Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CashViewModel::class.java) -> CashViewModel(container.salesRepository, context) as T
        modelClass.isAssignableFrom(SalesListViewModel::class.java) -> SalesListViewModel(container.salesRepository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container.settingsRepository) as T
        else -> error("ViewModel no registrado: ${modelClass.name}")
    }
}
