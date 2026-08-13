package com.cajasimple.app.domain.model

import java.util.UUID

enum class SaleStage { EMPTY, EDITING_ITEMS, READY_FOR_PAYMENT, PAYMENT_VALID, CONFIRMED }
enum class SyncStatus { PENDING, SYNCED, FAILED_RETRYABLE }
enum class CashMode { GUIDED, QUICK }
enum class VisualTheme { MONO, BLUE, RED_BLACK, CREAM, NAVY_CREAM }
enum class ThemeMode { LIGHT, DARK }

data class DraftItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val unitPrice: Long = 0,
    val quantity: Int = 1,
) {
    val subtotal: Long get() = unitPrice * quantity.toLong()
    val valid: Boolean get() = unitPrice > 0 && quantity > 0
}

data class SaleDraft(
    val id: String = UUID.randomUUID().toString(),
    val items: List<DraftItem> = listOf(DraftItem()),
    val receivedAmount: Long = 0,
    val paymentEntered: Boolean = false,
    val paymentStep: Boolean = false,
    val confirmed: Boolean = false,
) {
    val validItems: List<DraftItem> get() = items.filter(DraftItem::valid)
    val totalAmount: Long get() = validItems.sumOf(DraftItem::subtotal)
    val changeAmount: Long get() = (receivedAmount - totalAmount).coerceAtLeast(0)
    val missingAmount: Long get() = (totalAmount - receivedAmount).coerceAtLeast(0)
    val canConfirm: Boolean get() = !confirmed && totalAmount > 0 && paymentEntered && receivedAmount >= totalAmount
    val stage: SaleStage get() = when {
        confirmed -> SaleStage.CONFIRMED
        items.none(DraftItem::valid) -> SaleStage.EMPTY
        canConfirm -> SaleStage.PAYMENT_VALID
        paymentStep -> SaleStage.READY_FOR_PAYMENT
        else -> SaleStage.EDITING_ITEMS
    }
}

data class ConfirmedSale(
    val id: String,
    val createdAt: Long,
    val totalAmount: Long,
    val receivedAmount: Long,
    val changeAmount: Long,
    val items: List<DraftItem>,
)

data class BusinessSettings(
    val businessName: String = "Mi emprendimiento",
    val mode: CashMode = CashMode.GUIDED,
    val theme: VisualTheme = VisualTheme.CREAM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val productQuickPrices: List<Long> = listOf(18_000L, 20_000L, 25_000L, 30_000L),
    val paymentQuickAmounts: List<Long> = emptyList(),
)
