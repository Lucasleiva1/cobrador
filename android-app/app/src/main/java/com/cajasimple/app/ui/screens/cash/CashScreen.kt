package com.cajasimple.app.ui.screens.cash

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import com.cajasimple.app.domain.model.CashMode
import com.cajasimple.app.domain.model.SaleDraft
import com.cajasimple.app.domain.usecase.SaleEngine
import com.cajasimple.app.ui.components.ItemEditor
import com.cajasimple.app.ui.components.MoneyField
import com.cajasimple.app.ui.components.PrimaryAction
import kotlinx.coroutines.delay

@Composable
fun CashScreen(
    viewModel: CashViewModel,
    mode: CashMode,
    businessName: String,
    productQuickPrices: List<Long>,
    paymentQuickAmounts: List<Long>,
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) {
        state.notice?.let { snackbar.showSnackbar(it); viewModel.clearNotice() }
    }
    Box(Modifier.fillMaxSize()) {
        when {
            state.lastConfirmed != null && state.draft.confirmed -> ConfirmedResult(state, viewModel::newSale)
            mode == CashMode.GUIDED -> GuidedCash(state, viewModel, businessName, productQuickPrices, paymentQuickAmounts)
            else -> QuickCash(state, viewModel, businessName, paymentQuickAmounts)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun ScreenColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
private fun GuidedCash(
    state: CashUiState,
    vm: CashViewModel,
    businessName: String,
    productQuickPrices: List<Long>,
    paymentQuickAmounts: List<Long>,
) {
    val draft = state.draft
    if (draft.paymentStep) BackHandler { vm.editSale() }
    if (!draft.paymentStep && state.guidedStep != GuidedStep.PRICE) BackHandler { vm.previousGuidedStep() }
    val currentItem = draft.items.last()
    val priceFocus = remember(currentItem.id) { FocusRequester() }
    val paymentFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(currentItem.id, state.guidedStep, draft.paymentStep) {
        if (!draft.paymentStep && state.guidedStep == GuidedStep.PRICE) {
            delay(180)
            priceFocus.requestFocus()
        }
    }
    LaunchedEffect(draft.paymentStep) { if (draft.paymentStep) { delay(250); paymentFocus.requestFocus() } }
    ScreenColumn {
        if (!draft.paymentStep) {
            val completedItems = if (state.guidedStep == GuidedStep.DECISION) {
                draft.validItems
            } else {
                draft.items.dropLast(1).filter { it.valid }
            }
            completedItems.forEachIndexed { index, item -> GuidedItemSummary(index + 1, item) }
            when (state.guidedStep) {
                GuidedStep.PRICE -> {
                    val finishPrice = {
                        focusManager.clearFocus()
                        vm.confirmGuidedPrice()
                    }
                    MoneyField(
                        "Precio del producto",
                        currentItem.unitPrice,
                        { vm.updateItem(currentItem.id, priceRaw = it) },
                        focusRequester = priceFocus,
                        onDone = finishPrice,
                        testTag = "guided-price",
                    )
                    QuickAmountButtons(productQuickPrices) { price ->
                        vm.updateItem(currentItem.id, priceRaw = price.toString())
                    }
                    PrimaryAction("Listo", currentItem.unitPrice > 0, finishPrice)
                }
                GuidedStep.QUANTITY -> {
                    Text("Cantidad", style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(
                            onClick = { vm.updateItem(currentItem.id, quantity = currentItem.quantity - 1) },
                            enabled = currentItem.quantity > 1,
                            modifier = Modifier.size(56.dp),
                        ) { Icon(Icons.Outlined.Remove, "Restar cantidad") }
                        Text(
                            currentItem.quantity.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(88.dp),
                        )
                        IconButton(
                            onClick = { vm.updateItem(currentItem.id, quantity = currentItem.quantity + 1) },
                            modifier = Modifier.size(56.dp),
                        ) { Icon(Icons.Outlined.Add, "Sumar cantidad") }
                    }
                    PrimaryAction("Listo", true, vm::confirmGuidedQuantity)
                }
                GuidedStep.DECISION -> {
                    Total(draft.totalAmount)
                    OutlinedButton(
                        onClick = vm::addGuidedItem,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) { Text("Agregar otro producto", style = MaterialTheme.typography.labelLarge) }
                    PrimaryAction("Ir a cobrar", draft.totalAmount > 0, vm::goToPayment)
                }
            }
        } else {
            Header(businessName, "Cobro")
            draft.validItems.forEachIndexed { index, item -> GuidedItemSummary(index + 1, item) }
            Text("Total a cobrar", style = MaterialTheme.typography.bodyLarge)
            Text(SaleEngine.formatMoney(draft.totalAmount), style = MaterialTheme.typography.displayMedium)
            MoneyField("¿Con cuánto te pagan?", draft.receivedAmount, vm::setPayment, focusRequester = paymentFocus, testTag = "received")
            QuickAmountButtons(paymentQuickAmounts) { vm.setPayment(it.toString()) }
            PaymentResult(draft)
            TextButton(onClick = vm::editSale, modifier = Modifier.align(Alignment.End).height(48.dp)) { Text("Editar venta") }
            PrimaryAction("Cobrar", draft.canConfirm && !state.busy, { vm.confirm() })
        }
    }
}

@Composable
private fun QuickAmountButtons(amounts: List<Long>, onSelect: (Long) -> Unit) {
    if (amounts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        amounts.chunked(2).forEach { rowAmounts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowAmounts.forEach { amount ->
                    OutlinedButton(
                        onClick = { onSelect(amount) },
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Text(SaleEngine.formatMoney(amount), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (rowAmounts.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GuidedItemSummary(number: Int, item: com.cajasimple.app.domain.model.DraftItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Producto $number", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${SaleEngine.formatMoney(item.unitPrice)} × ${item.quantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(SaleEngine.formatMoney(item.subtotal), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun QuickCash(state: CashUiState, vm: CashViewModel, businessName: String, paymentQuickAmounts: List<Long>) {
    val draft = state.draft
    ScreenColumn {
        Header(businessName, "Caja rápida")
        draft.items.forEachIndexed { index, item ->
            ItemEditor(
                item, index + 1,
                onDescription = { vm.updateItem(item.id, description = it) },
                onPrice = { vm.updateItem(item.id, priceRaw = it) },
                onQuantity = { vm.updateItem(item.id, quantity = it) },
                onRemove = if (draft.items.size > 1) ({ vm.removeItem(item.id) }) else null,
            )
            HorizontalDivider()
        }
        TextButton(onClick = vm::addItem, modifier = Modifier.align(Alignment.End).height(52.dp)) { Text("+ Agregar producto") }
        Total(draft.totalAmount)
        MoneyField("Dinero recibido", draft.receivedAmount, vm::setPayment, testTag = "received")
        QuickAmountButtons(paymentQuickAmounts) { vm.setPayment(it.toString()) }
        PaymentResult(draft)
        PrimaryAction("Cobrar", draft.canConfirm && !state.busy, { vm.confirm() })
        PrimaryAction("Nueva venta", draft.canConfirm || draft.confirmed, vm::newSale)
    }
}

@Composable
private fun Header(business: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(business, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun Total(total: Long) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text("Total", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        Text(SaleEngine.formatMoney(total), style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun PaymentResult(draft: SaleDraft) {
    val valid = draft.paymentEntered && draft.receivedAmount >= draft.totalAmount && draft.totalAmount > 0
    val text = when {
        !draft.paymentEntered -> "Ingresá el dinero recibido"
        valid -> "Vuelto ${SaleEngine.formatMoney(draft.changeAmount)}"
        else -> "El dinero no alcanza · faltan ${SaleEngine.formatMoney(draft.missingAmount)}"
    }
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = if (draft.paymentEntered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ConfirmedResult(state: CashUiState, onNewSale: () -> Unit) {
    val sale = requireNotNull(state.lastConfirmed)
    val view = LocalView.current
    var showConfetti by remember(sale.id) { mutableStateOf(true) }
    LaunchedEffect(sale.id) { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
    LaunchedEffect(sale.id) { delay(900); showConfetti = false }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(showConfetti, exit = fadeOut(tween(350))) { Confetti(Modifier.fillMaxSize()) }
        ScreenColumn {
            Spacer(Modifier.height(34.dp))
            Text("Venta registrada", style = MaterialTheme.typography.headlineLarge)
            Text("Total cobrado", style = MaterialTheme.typography.bodyLarge)
            Text(SaleEngine.formatMoney(sale.totalAmount), style = MaterialTheme.typography.displayMedium)
            AnimatedVisibility(visible = true, enter = scaleIn(tween(430), initialScale = 0.65f)) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
                ) {
                    Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vuelto a entregar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Text(SaleEngine.formatMoney(sale.changeAmount), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryAction("Nueva venta", true, onNewSale)
        }
    }
}

@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val colors = listOf(Color(0xFFEAB308), Color(0xFF2563EB), Color(0xFFDC2626), Color(0xFF16A34A))
        repeat(28) { index ->
            val x = ((index * 83) % 100) / 100f * size.width
            val y = ((index * 47) % 48) / 100f * size.height
            rotate((index * 31).toFloat(), pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                drawRect(colors[index % colors.size], topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(8f, 18f))
            }
        }
    }
}
