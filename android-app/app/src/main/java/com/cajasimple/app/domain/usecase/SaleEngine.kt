package com.cajasimple.app.domain.usecase

import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.model.SaleDraft
import java.text.NumberFormat
import java.util.Locale

object SaleEngine {
    fun sanitizeMoney(raw: String): Long = raw.filter(Char::isDigit).take(15).toLongOrNull() ?: 0L

    fun formatMoney(amount: Long): String {
        val format = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-AR"))
        format.isGroupingUsed = true
        return "$${format.format(amount)}"
    }

    fun updateItem(draft: SaleDraft, id: String, transform: (DraftItem) -> DraftItem): SaleDraft =
        draft.copy(items = draft.items.map { if (it.id == id) transform(it) else it })

    fun removeItem(draft: SaleDraft, id: String): SaleDraft {
        val remaining = draft.items.filterNot { it.id == id }
        return draft.copy(items = remaining.ifEmpty { listOf(DraftItem()) })
    }

    fun addItem(draft: SaleDraft): SaleDraft = draft.copy(items = draft.items + DraftItem())

    fun enterPayment(draft: SaleDraft, raw: String): SaleDraft = draft.copy(
        receivedAmount = sanitizeMoney(raw),
        paymentEntered = raw.any(Char::isDigit),
    )
}
