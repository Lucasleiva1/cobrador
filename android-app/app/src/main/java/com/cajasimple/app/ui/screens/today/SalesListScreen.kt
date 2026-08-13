package com.cajasimple.app.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.usecase.SaleEngine
import com.cajasimple.app.util.DailyPdf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SpanishArgentina = Locale.forLanguageTag("es-AR")

@Composable
fun SalesListScreen(viewModel: SalesListViewModel, businessName: String, history: Boolean) {
    val sales by viewModel.sales.collectAsState()
    val date by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(businessName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(if (history) "Historial" else "Hoy", style = MaterialTheme.typography.headlineLarge)
        if (history) DateSelector(date, viewModel::previousDay, viewModel::nextDay)
        Text(date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SpanishArgentina)), style = MaterialTheme.typography.titleLarge)
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp)) {
                Column { Text("Ventas", style = MaterialTheme.typography.bodyLarge); Text(sales.size.toString(), style = MaterialTheme.typography.headlineLarge) }
                Spacer(Modifier.weight(1f))
                Column { Text("Total vendido", style = MaterialTheme.typography.bodyLarge); Text(SaleEngine.formatMoney(sales.sumOf { it.totalAmount }), style = MaterialTheme.typography.headlineLarge) }
            }
        }
        Button(
            onClick = { DailyPdf.share(context, DailyPdf.create(context, businessName, date, sales)) },
            enabled = sales.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Icon(Icons.Outlined.PictureAsPdf, null); Text("  Exportar PDF") }
        if (sales.isEmpty()) Text("Todavía no hay ventas en esta fecha.", style = MaterialTheme.typography.bodyLarge)
        sales.forEachIndexed { index, sale ->
            SaleRow(index + 1, sale)
            HorizontalDivider()
        }
    }
}

@Composable
private fun DateSelector(date: LocalDate, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(previous, Modifier.padding(2.dp)) { Icon(Icons.Outlined.ChevronLeft, "Día anterior") }
        Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), style = MaterialTheme.typography.titleLarge)
        IconButton(next, enabled = date < LocalDate.now(), modifier = Modifier.padding(2.dp)) { Icon(Icons.Outlined.ChevronRight, "Día siguiente") }
    }
}

@Composable
private fun SaleRow(number: Int, sale: ConfirmedSale) {
    var expanded by remember(sale.id) { mutableStateOf(false) }
    val time = Instant.ofEpochMilli(sale.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Venta $number · $time", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(SaleEngine.formatMoney(sale.totalAmount), style = MaterialTheme.typography.titleLarge)
        }
        if (expanded) {
            sale.items.forEach { Text("${it.description.ifBlank { "Sin detalle" }} · ${it.quantity} × ${SaleEngine.formatMoney(it.unitPrice)}") }
            Text("Recibido ${SaleEngine.formatMoney(sale.receivedAmount)} · Vuelto ${SaleEngine.formatMoney(sale.changeAmount)}")
        } else Text("Tocá para ver el detalle", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
