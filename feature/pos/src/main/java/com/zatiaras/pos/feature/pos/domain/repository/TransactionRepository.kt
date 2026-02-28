package com.zatiaras.pos.feature.pos.domain.repository

import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction operations.
 * 
 * Follows offline-first pattern:
 * - All writes go to Room first
 * - Background sync pushes to Supabase
 */
interface TransactionRepository {
    
    /**
     * Create a new transaction from cart contents.
     * 
     * @param cart The cart to convert to transaction
     * @param paymentMethod How the customer is paying
     * @param amountPaid Amount given by customer (for cash)
     * @param discountPercent Optional discount percentage (0-100)
     * @param taxPercent Tax percentage (default 11% PPN)
     * @param notes Optional transaction notes
     * @return Result containing the created Transaction or error
     */
    suspend fun createTransaction(
        cart: Cart,
        paymentMethod: PaymentMethod,
        amountPaid: Long,
        discountPercent: Double = 0.0,
        taxPercent: Double = 11.0,
        notes: String? = null,
        customerName: String? = null
    ): Result<Transaction>
    
    /**
     * Get a transaction by its ID.
     */
    suspend fun getTransactionById(id: String): Transaction?
    
    /**
     * Delete a transaction (soft delete).
     */
    suspend fun deleteTransaction(id: String): Result<Unit>
    
    /**
     * Update payment method for a transaction.
     */
    suspend fun updatePaymentMethod(id: String, paymentMethod: PaymentMethod): Result<Unit>
    
    /**
     * Get all transactions from today.
     */
    fun getTodayTransactions(): Flow<List<Transaction>>
    
    /**
     * Get transactions within a date range.
     * 
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    /**
     * Push unsynced transactions to remote server.
     */
    suspend fun syncToRemote(): Result<Unit>
    
    /**
     * Generate the next transaction number for today.
     * Format: TRX-YYYYMMDD-XXXX (e.g., TRX-20260109-0001)
     */
    suspend fun generateTransactionNumber(): String
    
    /**
     * Get today's transaction statistics.
     */
    suspend fun getTodayStats(): TransactionStats
}

/**
 * Simple stats container for dashboard display.
 */
data class TransactionStats(
    val totalTransactions: Int,
    val totalRevenue: Long,
    val totalItemsSold: Int
)
