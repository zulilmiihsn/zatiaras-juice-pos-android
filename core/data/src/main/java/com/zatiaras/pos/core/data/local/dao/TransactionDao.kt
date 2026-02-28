package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import com.zatiaras.pos.core.data.local.entity.TransactionWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction operations.
 * 
 * All queries filter out soft-deleted transactions (isDeleted = false).
 */
@Dao
interface TransactionDao {

    // ==================== TRANSACTIONS ====================

    /**
     * Insert a new transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /**
     * Insert transaction items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    /**
     * Insert transaction with its items in a single transaction.
     */
    @Transaction
    suspend fun insertTransactionWithItems(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        insertTransaction(transaction)
        insertTransactionItems(items)
    }

    /**
     * Get transaction by ID with its items.
     */
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: String): TransactionEntity?

    /**
     * Get items for a transaction.
     */
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<TransactionItemEntity>

    /**
     * Get items for multiple transactions in a single batch query.
     * Avoids N+1 query problem when syncing multiple transactions.
     */
    @Query("SELECT * FROM transaction_items WHERE transactionId IN (:transactionIds)")
    suspend fun getTransactionItemsByTransactionIds(transactionIds: List<String>): List<TransactionItemEntity>

    /**
     * Get all transactions for today (reactive Flow).
     * Uses epoch milliseconds for date comparison.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """)
    fun getTransactionsByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    /**
     * Get transactions list for reports (Synchronous suspend).
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
    """)
    suspend fun getTransactionsForReports(startDate: Long, endDate: Long): List<TransactionEntity>


    /**
     * Get transactions with items in a single query.
     * Uses @Relation to avoid N+1 query problem.
     * Room will fetch all transactions and their items in optimized queries.
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """)
    fun getTransactionsWithItems(startOfDay: Long, endOfDay: Long): Flow<List<TransactionWithItems>>

    /**
     * Get today's transaction count (for generating transaction number).
     */
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay
    """)
    suspend fun getTransactionCountForDay(startOfDay: Long, endOfDay: Long): Int

    /**
     * Get today's total revenue.
     */
    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0) FROM transactions 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
    """)
    suspend fun getTotalRevenueForDay(startOfDay: Long, endOfDay: Long): Long

    /**
     * Get today's total items sold.
     */
    @Query("""
        SELECT COALESCE(SUM(ti.quantity), 0) 
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.createdAt >= :startOfDay 
        AND t.createdAt < :endOfDay 
        AND t.isDeleted = 0
    """)
    suspend fun getTotalItemsSoldForDay(startOfDay: Long, endOfDay: Long): Int

    // ==================== SYNC ====================

    /**
     * Get all unsynced transactions.
     */
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    /**
     * Get count of unsynced transactions (efficient COUNT instead of loading all).
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /**
     * Mark transaction as synced.
     */
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Soft delete a transaction.
     */
    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update payment method for a transaction.
     */
    @Query("UPDATE transactions SET paymentMethod = :paymentMethod, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePaymentMethod(id: String, paymentMethod: String, timestamp: Long = System.currentTimeMillis())

    // ==================== REPORTS ====================

    /**
     * Get daily revenue for a date range (for weekly/monthly charts).
     * Groups transactions by date and sums revenue.
     */
    @Query("""
        SELECT 
            ((createdAt + :timeOffset) / 86400000) * 86400000 - :timeOffset as dayTimestamp,
            COALESCE(SUM(grandTotal), 0) as revenue,
            COUNT(*) as transactionCount
        FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
        GROUP BY dayTimestamp
        ORDER BY dayTimestamp ASC
    """)
    suspend fun getDailyRevenue(
        startDate: Long, 
        endDate: Long, 
        timeOffset: Long = 0L
    ): List<DailyRevenueEntity>

    /**
     * Get top selling products by quantity sold.
     */
    @Query("""
        SELECT 
            ti.productId,
            ti.productName,
            SUM(ti.quantity) as totalQuantity,
            SUM(ti.subtotal) as totalRevenue
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.createdAt >= :startDate 
        AND t.createdAt < :endDate 
        AND t.isDeleted = 0
        GROUP BY ti.productId, ti.productName
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    suspend fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int = 10): List<TopProductEntity>

    /**
     * Get total profit/loss summary.
     * Note: Profit calculation requires cost data from products.
     */
    @Query("""
        SELECT 
            COALESCE(SUM(grandTotal), 0) as totalRevenue,
            COALESCE(SUM(subtotal), 0) as grossRevenue,
            COALESCE(SUM(discountAmount), 0) as totalDiscount,
            COALESCE(SUM(taxAmount), 0) as totalTax
        FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
    """)
    suspend fun getRevenueSummary(startDate: Long, endDate: Long): RevenueSummaryEntity
}

/**
 * Entity for daily revenue query result.
 */
data class DailyRevenueEntity(
    val dayTimestamp: Long,
    val revenue: Long,
    val transactionCount: Int
)

/**
 * Entity for top selling product query result.
 */
data class TopProductEntity(
    val productId: String,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenue: Long
)

/**
 * Entity for revenue summary query result.
 */
data class RevenueSummaryEntity(
    val totalRevenue: Long,
    val grossRevenue: Long,
    val totalDiscount: Long,
    val totalTax: Long
)
