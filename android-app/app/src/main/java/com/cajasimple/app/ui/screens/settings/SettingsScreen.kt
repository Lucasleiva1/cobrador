package com.cajasimple.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cajasimple.app.domain.model.CashMode
import com.cajasimple.app.domain.model.ResetButtonStyle
import com.cajasimple.app.domain.model.ThemeMode
import com.cajasimple.app.domain.model.VisualTheme
import com.cajasimple.app.domain.usecase.SaleEngine
import com.cajasimple.app.ui.components.ThousandsSeparatorTransformation
import com.cajasimple.app.BuildConfig
import com.cajasimple.app.update.UpdateUiState
import com.cajasimple.app.update.UpdateViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, updatesViewModel: UpdateViewModel) {
    val settings by viewModel.settings.collectAsState()
    val updateState by updatesViewModel.state.collectAsState()
    var name by remember { mutableStateOf(settings.businessName) }
    LaunchedEffect(settings.businessName) { if (name != settings.businessName) name = settings.businessName }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
        SectionTitle("Identidad de la caja")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60); viewModel.setName(name) },
            label = { Text("Nombre de la marca") },
            placeholder = { Text("Ej.: Almacén San Martín") },
            supportingText = { Text("Se mostrará arriba de la calculadora y en los reportes.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
        )
        SectionTitle("Modo de caja")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = settings.mode == CashMode.GUIDED, onClick = { viewModel.setMode(CashMode.GUIDED) }, label = { Text("Guiado") })
            FilterChip(selected = settings.mode == CashMode.QUICK, onClick = { viewModel.setMode(CashMode.QUICK) }, label = { Text("Rápido") })
        }
        SectionTitle("Botón Reiniciar")
        Text(
            "Elegí cómo querés verlo arriba de la caja.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = settings.resetButtonStyle == ResetButtonStyle.ICON_ONLY,
                onClick = { viewModel.setResetButtonStyle(ResetButtonStyle.ICON_ONLY) },
                leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                label = { Text("Solo ícono") },
            )
            FilterChip(
                selected = settings.resetButtonStyle == ResetButtonStyle.ICON_WITH_TEXT,
                onClick = { viewModel.setResetButtonStyle(ResetButtonStyle.ICON_WITH_TEXT) },
                leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                label = { Text("Ícono y texto") },
            )
        }
        QuickAmountsEditor(
            title = "Precios rápidos de productos",
            description = "Aparecen debajo del precio del producto.",
            amounts = settings.productQuickPrices,
            onAdd = viewModel::addProductQuickPrice,
            onRemove = viewModel::removeProductQuickPrice,
        )
        QuickAmountsEditor(
            title = "Montos rápidos de cobro",
            description = "Aparecen debajo del dinero recibido al cobrar.",
            amounts = settings.paymentQuickAmounts,
            onAdd = viewModel::addPaymentQuickAmount,
            onRemove = viewModel::removePaymentQuickAmount,
        )
        SectionTitle("Modo de pantalla")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                leadingIcon = { Icon(Icons.Outlined.LightMode, contentDescription = null) },
                label = { Text("Claro") },
                modifier = Modifier.height(48.dp),
            )
            FilterChip(
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                leadingIcon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                label = { Text("Oscuro") },
                modifier = Modifier.height(48.dp),
            )
        }
        Text(
            "La aplicación recordará este modo al volver a abrirla.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionTitle("Tema de colores")
        VisualTheme.entries.forEach { theme ->
            FilterChip(
                selected = settings.theme == theme,
                onClick = { viewModel.setTheme(theme) },
                label = { Text(theme.label()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
        }
        UpdateSettings(updateState, updatesViewModel::check)
        AdvancedSettings()
    }
}

@Composable
private fun UpdateSettings(state: UpdateUiState, onCheck: (Boolean) -> Unit) {
    SectionTitle("Actualizaciones")
    Text(
        "Versión instalada: ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.bodyLarge,
    )
    when (state) {
        UpdateUiState.Checking -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Buscando una versión nueva…")
        }
        UpdateUiState.UpToDate -> Text("Caja Simple está actualizada.")
        is UpdateUiState.Available -> Text("Versión ${state.update.versionName} disponible.")
        is UpdateUiState.Downloading -> Text("Descargando la versión ${state.update.versionName}…")
        is UpdateUiState.PermissionRequired -> Text("La actualización está descargada y espera la autorización de Android.")
        is UpdateUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        else -> Text(
            "Podés comprobar manualmente si hay una versión nueva.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(
        onClick = { onCheck(false) },
        enabled = state !is UpdateUiState.Checking && state !is UpdateUiState.Downloading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(if (state is UpdateUiState.Available) "Ver actualización" else "Buscar actualización")
    }
}

@Composable
private fun AdvancedSettings() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Avanzado", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Cerrar Avanzado" else "Abrir Avanzado",
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuditSubsystemInfo()
        }
    }
}

private const val AUDIT_SUBSYSTEM_TITLE =
    "Subsistema de Auditoría Algorítmica de Integridad, Trazabilidad y Validación de Flujos Transaccionales"

@Composable
private fun AuditSubsystemInfo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            AUDIT_SUBSYSTEM_TITLE,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Cerrar $AUDIT_SUBSYSTEM_TITLE" else "Abrir $AUDIT_SUBSYSTEM_TITLE",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "Los datos operativos se procesan y conservan según la configuración activa del sistema.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Se mantiene una representación local de la información necesaria para la continuidad operativa.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Cuando las condiciones lo permiten, el sistema replica automáticamente la información mediante los servicios habilitados.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun QuickAmountsEditor(
    title: String,
    description: String,
    amounts: List<Long>,
    onAdd: (Long) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var raw by remember(title) { mutableStateOf("") }
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Cerrar $title" else "Abrir $title",
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            amounts.forEach { amount ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(SaleEngine.formatMoney(amount), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onRemove(amount) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Quitar ${SaleEngine.formatMoney(amount)}")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it.filter(Char::isDigit).take(15) },
                    label = { Text("Nuevo importe") },
                    prefix = { Text("$ ") },
                    visualTransformation = ThousandsSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        raw.toLongOrNull()?.takeIf { it > 0 }?.let(onAdd)
                        raw = ""
                    },
                    enabled = raw.toLongOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.height(56.dp),
                ) { Text("Agregar") }
            }
        }
    }
}

@Composable private fun SectionTitle(value: String) = Text(value, style = MaterialTheme.typography.titleLarge)

private fun VisualTheme.label() = when (this) {
    VisualTheme.MONO -> "Negro y blanco"
    VisualTheme.BLUE -> "Azul y blanco"
    VisualTheme.RED_BLACK -> "Rojo y negro"
    VisualTheme.CREAM -> "Crema y negro"
    VisualTheme.NAVY_CREAM -> "Azul oscuro y crema"
}
