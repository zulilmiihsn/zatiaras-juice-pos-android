package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.TransactionEntity

interface TransactionSyncDao {
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET paymentMethod = :paymentMethod, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePaymentMethod(id: String, paymentMethod: String, timestamp: Long = System.currentTimeMillis())
}
