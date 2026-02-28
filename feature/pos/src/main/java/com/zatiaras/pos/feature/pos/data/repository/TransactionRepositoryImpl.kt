package com.zatiaras.pos.feature.pos.data.repository

import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.feature.pos.data.mapper.createTransactionEntity
import com.zatiaras.pos.feature.pos.data.mapper.toDomain
import com.zatiaras.pos.feature.pos.data.mapper.toItemEntities
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.domain.repository.TransactionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository.
 * 
 * Offline-First Design:
 * 1. All transactions are saved to Room first
 * 2. Background sync is handled by SyncManager in :core:data
 * 3. Transactions marked isSynced = false until SyncWorker confirms
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun createTransaction(
        cart: Cart,
        paymentMethod: PaymentMethod,
        amountPaid: Long,
        discountPercent: Double,
        taxPercent: Double,
        notes: String?,
        customerName: String?
    ): Result<Transaction> {
        return try {
            if (cart.isEmpty()) {
                return Result.Error(IllegalStateException("Keranjang kosong"))
            }
            
            val transactionId = UUID.randomUUID().toString()
            val transactionNumber = generateTransactionNumber()
            
            // Create transaction entity
            val transactionEntity = createTransactionEntity(
                id = transactionId,
                transactionNumber = transactionNumber,
                cart = cart,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                discountPercent = discountPercent,
                taxPercent = taxPercent,
                notes = notes,
                customerName = customerName
            )
            
            // Create transaction item entities
            val itemEntities = cart.toItemEntities(transactionId)
            
            // Save to database in single transaction
            transactionDao.insertTransactionWithItems(transactionEntity, itemEntities)
            
            Timber.d("Transaction created: $transactionNumber with ${itemEntities.size} items")
            
            // Return domain model
            val transaction = transactionEntity.toDomain(itemEntities)
            Result.Success(transaction)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to create transaction")
            Result.Error(e)
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val items = transactionDao.getTransactionItems(id)
        return entity.toDomain(items)
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            transactionDao.softDelete(id)
            Timber.d("Transaction soft deleted: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete transaction: $id")
            Result.Error(e)
        }
    }

    override suspend fun updatePaymentMethod(id: String, paymentMethod: PaymentMethod): Result<Unit> {
        return try {
            transactionDao.updatePaymentMethod(id, paymentMethod.name)
            Timber.d("Transaction payment method updated: $id to ${paymentMethod.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update payment method for transaction: $id")
            Result.Error(e)
        }
    }

    override fun getTodayTransactions(): Flow<List<Transaction>> {
        val (startOfDay, endOfDay) = DateUtils.getTodayRange()
        // Use @Relation-based query to avoid N+1 problem
        return transactionDao.getTransactionsWithItems(startOfDay, endOfDay)
            .map { transactionsWithItems ->
                transactionsWithItems.map { it.toDomain() }
            }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        // Use @Relation-based query to avoid N+1 problem
        return transactionDao.getTransactionsWithItems(startDate, endDate)
            .map { transactionsWithItems ->
                transactionsWithItems.map { it.toDomain() }
            }
    }

    override suspend fun syncToRemote(): Result<Unit> {
        // Sync is handled by SyncManager in :core:data
        // This method exists for manual sync trigger from UI if needed
        val unsynced = transactionDao.getUnsyncedTransactions()
        Timber.d("TransactionRepository: ${unsynced.size} unsynced transactions (handled by SyncManager)")
        return Result.Success(Unit)
    }

    override suspend fun generateTransactionNumber(): String {
        val (startOfDay, endOfDay) = DateUtils.getTodayRange()
        val count = transactionDao.getTransactionCountForDay(startOfDay, endOfDay)
        val dateStr = DateUtils.formatDateCompact()
        val sequence = String.format("%04d", count + 1)
        return "TRX-$dateStr-$sequence"
    }

    override suspend fun getTodayStats(): TransactionStats {
        val (startOfDay, endOfDay) = DateUtils.getTodayRange()
        
        val totalTransactions = transactionDao.getTransactionCountForDay(startOfDay, endOfDay)
        val totalRevenue = transactionDao.getTotalRevenueForDay(startOfDay, endOfDay)
        val totalItemsSold = transactionDao.getTotalItemsSoldForDay(startOfDay, endOfDay)
        
        return TransactionStats(
            totalTransactions = totalTransactions,
            totalRevenue = totalRevenue,
            totalItemsSold = totalItemsSold
        )
    }
}
