package com.cajasimple.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertConfirmedOnce(sale: SaleEntity, items: List<SaleItemEntity>): Boolean {
        val inserted = insertSale(sale)
        if (inserted == -1L) return false
        insertItems(items)
        return true
    }

    @Transaction
    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt ASC")
    suspend fun getBetween(start: Long, end: Long): List<SaleWithItems>

    @Transaction
    @Query("SELECT * FROM sales WHERE syncStatus != 'SYNCED' ORDER BY createdAt LIMIT :limit")
    suspend fun pendingSales(limit: Int = 25): List<SaleWithItems>

    @Query("UPDATE sales SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraftHeader(draft: DraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraftItems(items: List<DraftItemWithOwner>)

    @Query("DELETE FROM draft_items_owned")
    suspend fun clearDraftItems()

    @Query("DELETE FROM draft")
    suspend fun clearDraftHeader()

    @Transaction
    suspend fun replaceDraft(draft: DraftEntity, items: List<DraftItemWithOwner>) {
        clearDraftItems()
        saveDraftHeader(draft)
        saveDraftItems(items)
    }

    @Transaction
    @Query("SELECT * FROM draft WHERE singletonId = 1 LIMIT 1")
    suspend fun loadDraft(): DraftWithItems?

    @Transaction
    suspend fun clearDraft() {
        clearDraftItems()
        clearDraftHeader()
    }
}
