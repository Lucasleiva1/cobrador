package com.cajasimple.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "sales", indices = [Index("createdAt"), Index("syncStatus")])
data class SaleEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val createdAt: Long,
    val totalAmount: Long,
    val receivedAmount: Long,
    val changeAmount: Long,
    val syncStatus: String,
    val confirmedAt: Long,
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [ForeignKey(
        entity = SaleEntity::class,
        parentColumns = ["id"],
        childColumns = ["saleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("saleId")],
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val optionalDescription: String?,
    val unitPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val position: Int,
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId") val items: List<SaleItemEntity>,
)

@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey val singletonId: Int = 1,
    val saleId: String,
    val receivedAmount: Long,
    val paymentEntered: Boolean,
    val paymentStep: Boolean,
)

data class DraftWithItems(
    @Embedded val draft: DraftEntity,
    @Relation(parentColumn = "singletonId", entityColumn = "draftOwnerId") val items: List<DraftItemWithOwner>,
)

@Entity(tableName = "draft_items_owned", indices = [Index("draftOwnerId")])
data class DraftItemWithOwner(
    @PrimaryKey val id: String,
    val draftOwnerId: Int = 1,
    val description: String,
    val unitPrice: Long,
    val quantity: Int,
    val position: Int,
)
