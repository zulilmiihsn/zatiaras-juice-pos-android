package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import androidx.room.Transaction
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import com.zatiaras.pos.core.data.local.entity.TransactionWithItems
import kotlinx.coroutines.flow.Flow

interface TransactionReadDao {
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId IN (:transactionIds)")
    suspend fun getTransactionItemsByTransactionIds(transactionIds: List<String>): List<TransactionItemEntity>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """,
    )
    fun getTransactionsByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
    """,
    )
    suspend fun getTransactionsForReports(startDate: Long, endDate: Long): List<TransactionEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """,
    )
    fun getTransactionsWithItems(startOfDay: Long, endOfDay: Long): Flow<List<TransactionWithItems>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay
    """,
    )
    suspend fun getTransactionCountForDay(startOfDay: Long, endOfDay: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(grandTotal), 0) FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
    """,
    )
    suspend fun getTotalRevenueForDay(startOfDay: Long, endOfDay: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(ti.quantity), 0) 
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.createdAt >= :startOfDay 
        AND t.createdAt < :endOfDay 
        AND t.isDeleted = 0
    """,
    )
    suspend fun getTotalItemsSoldForDay(startOfDay: Long, endOfDay: Long): Int
}
