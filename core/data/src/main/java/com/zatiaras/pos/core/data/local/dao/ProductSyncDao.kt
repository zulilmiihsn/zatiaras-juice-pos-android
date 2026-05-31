package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.ProductEntity

interface ProductSyncDao {
    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("UPDATE products SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE products SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
