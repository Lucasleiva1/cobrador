package com.cajasimple.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.usecase.SaleEngine

@Composable
fun MoneyField(
    label: String,
    amount: Long,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    testTag: String = "money-field",
) {
    val digits = if (amount == 0L) "" else amount.toString()
    val keyboard = LocalConfiguration.current.keyboard
    val hasPhysicalKeyboard = keyboard != Configuration.KEYBOARD_NOKEYS &&
        keyboard != Configuration.KEYBOARD_UNDEFINED
    OutlinedTextField(
        value = digits,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(15)) },
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .semantics { contentDescription = "$label, ${SaleEngine.formatMoney(amount)}" }
            .testTag(testTag),
        enabled = enabled,
        label = { Text(label) },
        prefix = { Text("$ ") },
        supportingText = if (digits.isNotEmpty()) ({ Text(SaleEngine.formatMoney(amount)) }) else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
            showKeyboardOnFocus = !hasPhysicalKeyboard,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
    )
}

@Composable
fun ItemEditor(
    item: DraftItem,
    number: Int,
    onDescription: (String) -> Unit,
    onPrice: (String) -> Unit,
    onQuantity: (Int) -> Unit,
    onRemove: (() -> Unit)?,
    focusRequester: FocusRequester? = null,
) {
    var showDetail by remember(item.id) { mutableStateOf(item.description.isNotBlank()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Producto $number", style = MaterialTheme.typography.titleLarge)
            if (onRemove != null) IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).semantics { contentDescription = "Quitar producto $number" }) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        }
        MoneyField("Precio", item.unitPrice, onPrice, focusRequester = focusRequester, imeAction = ImeAction.Next, testTag = "price-$number")
        if (!showDetail) TextButton(onClick = { showDetail = true }) { Text("Agregar detalle opcional") }
        else OutlinedTextField(
            value = item.description,
            onValueChange = { onDescription(it.take(80)) },
            label = { Text("Descripción (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Cantidad", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onQuantity((item.quantity - 1).coerceAtLeast(1)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Remove, "Restar cantidad") }
            Text(item.quantity.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(38.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { onQuantity(item.quantity + 1) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Add, "Sumar cantidad") }
        }
        if (item.valid) Text("Subtotal ${SaleEngine.formatMoney(item.subtotal)}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PrimaryAction(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(58.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
