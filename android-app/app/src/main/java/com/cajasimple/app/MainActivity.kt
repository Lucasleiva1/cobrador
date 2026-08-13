package com.cajasimple.app

import android.os.Bundle
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        setContent {
            val preferences by settings.settings.collectAsState()
            CajaTheme(preferences.theme, preferences.themeMode) {
                CajaApp(cash, today, history, settings)
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
private fun CajaApp(cash: CashViewModel, today: SalesListViewModel, history: SalesListViewModel, settingsVm: SettingsViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val settings by settingsVm.settings.collectAsState()
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
            composable("settings") { SettingsScreen(settingsVm) }
        }
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
