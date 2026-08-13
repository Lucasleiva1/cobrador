package com.cajasimple.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SaleEntity::class, SaleItemEntity::class, DraftEntity::class, DraftItemWithOwner::class],
    version = 1,
    exportSchema = false,
)
abstract class CajaDatabase : RoomDatabase() {
    abstract fun salesDao(): SalesDao
}
