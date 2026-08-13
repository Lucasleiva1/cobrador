package com.cajasimple.app.domain

import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.model.SaleDraft
import com.cajasimple.app.domain.model.SaleStage
import com.cajasimple.app.domain.usecase.SaleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleEngineTest {
    @Test fun `suma un producto`() {
        assertEquals(50_000, SaleDraft(items = listOf(DraftItem(unitPrice = 50_000))).totalAmount)
    }

    @Test fun `suma varios productos y cantidades`() {
        val draft = SaleDraft(items = listOf(DraftItem(unitPrice = 1_000, quantity = 3), DraftItem(unitPrice = 5_000, quantity = 2)))
        assertEquals(13_000, draft.totalAmount)
    }

    @Test fun `formatea pesos sin centavos`() {
        assertEquals("$50.000", SaleEngine.formatMoney(50_000))
    }

    @Test fun `limpia simbolos puntos espacios y letras pegadas`() {
        assertEquals(50_000, SaleEngine.sanitizeMoney("$ 50.000abc"))
    }

    @Test fun `recibido exacto permite cobrar sin vuelto`() {
        val draft = paidDraft("50000")
        assertTrue(draft.canConfirm)
        assertEquals(0, draft.changeAmount)
        assertEquals(SaleStage.PAYMENT_VALID, draft.stage)
    }

    @Test fun `recibido superior calcula vuelto`() {
        assertEquals(10_000, paidDraft("60000").changeAmount)
    }

    @Test fun `recibido inferior calcula faltante y bloquea`() {
        val draft = paidDraft("30000")
        assertEquals(20_000, draft.missingAmount)
        assertFalse(draft.canConfirm)
    }

    @Test fun `modificar producto recalcula dinamicamente`() {
        val original = paidDraft("60000")
        val modified = SaleEngine.updateItem(original, original.items.first().id) { it.copy(unitPrice = 55_000) }
        assertEquals(5_000, modified.changeAmount)
        assertTrue(modified.canConfirm)
    }

    @Test fun `agregar producto despues del pago recalcula faltante`() {
        val original = paidDraft("60000")
        val withItem = SaleEngine.addItem(original)
        val changed = SaleEngine.updateItem(withItem, withItem.items.last().id) { it.copy(unitPrice = 20_000) }
        assertEquals(10_000, changed.missingAmount)
        assertFalse(changed.canConfirm)
    }

    @Test fun `venta confirmada no puede confirmarse otra vez`() {
        assertFalse(paidDraft("50000").copy(confirmed = true).canConfirm)
    }

    private fun paidDraft(received: String): SaleDraft = SaleEngine.enterPayment(
        SaleDraft(items = listOf(DraftItem(unitPrice = 50_000)), paymentStep = true), received,
    )
}

