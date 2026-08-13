package com.cajasimple.app.data.repository

import com.cajasimple.app.data.local.DraftEntity
import com.cajasimple.app.data.local.DraftItemWithOwner
import com.cajasimple.app.data.local.SaleEntity
import com.cajasimple.app.data.local.SaleItemEntity
import com.cajasimple.app.data.local.SaleWithItems
import com.cajasimple.app.data.local.SalesDao
import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.model.SaleDraft
import com.cajasimple.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class SalesRepository(private val dao: SalesDao, private val deviceId: String = "caja-local") {
    suspend fun saveDraft(draft: SaleDraft) {
        if (draft.confirmed) return
        dao.replaceDraft(
            DraftEntity(saleId = draft.id, receivedAmount = draft.receivedAmount, paymentEntered = draft.paymentEntered, paymentStep = draft.paymentStep),
            draft.items.mapIndexed { index, item -> DraftItemWithOwner(item.id, description = item.description, unitPrice = item.unitPrice, quantity = item.quantity, position = index) },
        )
    }

    suspend fun loadDraft(): SaleDraft? = dao.loadDraft()?.let { stored ->
        SaleDraft(
            id = stored.draft.saleId,
            items = stored.items.sortedBy { it.position }.map { DraftItem(it.id, it.description, it.unitPrice, it.quantity) }.ifEmpty { listOf(DraftItem()) },
            receivedAmount = stored.draft.receivedAmount,
            paymentEntered = stored.draft.paymentEntered,
            paymentStep = stored.draft.paymentStep,
        )
    }

    suspend fun confirm(draft: SaleDraft, now: Long = System.currentTimeMillis()): ConfirmedSale? {
        if (!draft.canConfirm) return null
        val sale = SaleEntity(
            id = draft.id,
            deviceId = deviceId,
            createdAt = now,
            totalAmount = draft.totalAmount,
            receivedAmount = draft.receivedAmount,
            changeAmount = draft.changeAmount,
            syncStatus = SyncStatus.PENDING.name,
            confirmedAt = now,
        )
        val items = draft.validItems.mapIndexed { index, item ->
            SaleItemEntity(item.id, draft.id, item.description.trim().ifBlank { null }, item.unitPrice, item.quantity, item.subtotal, index)
        }
        if (!dao.insertConfirmedOnce(sale, items)) return null
        dao.clearDraft()
        return ConfirmedSale(sale.id, now, sale.totalAmount, sale.receivedAmount, sale.changeAmount, draft.validItems)
    }

    fun observeDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<ConfirmedSale>> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<ConfirmedSale> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.getBetween(start, end).map { it.toDomain() }
    }

    suspend fun pendingSales(limit: Int = 25): List<ConfirmedSale> =
        dao.pendingSales(limit).map { it.toDomain() }

    suspend fun markSynced(id: String) = dao.updateSyncStatus(id, SyncStatus.SYNCED.name)

    private fun SaleWithItems.toDomain() = ConfirmedSale(
        id = sale.id,
        createdAt = sale.createdAt,
        totalAmount = sale.totalAmount,
        receivedAmount = sale.receivedAmount,
        changeAmount = sale.changeAmount,
        items = items.sortedBy { it.position }.map { DraftItem(it.id, it.optionalDescription.orEmpty(), it.unitPrice, it.quantity) },
    )
}
