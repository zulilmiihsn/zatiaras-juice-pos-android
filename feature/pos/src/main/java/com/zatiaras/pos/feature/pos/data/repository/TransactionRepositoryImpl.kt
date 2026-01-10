package com.zatiaras.pos.feature.pos.data.repository

import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.feature.pos.data.mapper.createTransactionEntity
import com.zatiaras.pos.feature.pos.data.mapper.toDomain
import com.zatiaras.pos.feature.pos.data.mapper.toItemEntities
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.domain.repository.TransactionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository.
 * 
 * Offline-First Design:
 * 1. All transactions are saved to Room first
 * 2. Background sync pushes to Supabase (handled by Sync Engine)
 * 3. Transactions marked isSynced = false until confirmed
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
        notes: String?
    ): Result<Transaction> {
        return try {
            if (cart.isEmpty()) {
                return Result.failure(IllegalStateException("Keranjang kosong"))
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
                notes = notes
            )
            
            // Create transaction item entities
            val itemEntities = cart.toItemEntities(transactionId)
            
            // Save to database in single transaction
            transactionDao.insertTransactionWithItems(transactionEntity, itemEntities)
            
            Timber.d("Transaction created: $transactionNumber with ${itemEntities.size} items")
            
            // Return domain model
            val transaction = transactionEntity.toDomain(itemEntities)
            Result.success(transaction)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to create transaction")
            Result.failure(e)
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val items = transactionDao.getTransactionItems(id)
        return entity.toDomain(items)
    }

    override fun getTodayTransactions(): Flow<List<Transaction>> {
        val (startOfDay, endOfDay) = getTodayRange()
        return transactionDao.getTransactionsByDateRange(startOfDay, endOfDay)
            .map { entities ->
                entities.map { entity ->
                    val items = transactionDao.getTransactionItems(entity.id)
                    entity.toDomain(items)
                }
            }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities ->
                entities.map { entity ->
                    val items = transactionDao.getTransactionItems(entity.id)
                    entity.toDomain(items)
                }
            }
    }

    override suspend fun syncToRemote(): Result<Unit> {
        // TODO: Implement in Phase 5 - Sync Engine
        // For now, just log and return success
        val unsynced = transactionDao.getUnsyncedTransactions()
        Timber.d("Found ${unsynced.size} unsynced transactions (sync not yet implemented)")
        return Result.success(Unit)
    }

    override suspend fun generateTransactionNumber(): String {
        val (startOfDay, endOfDay) = getTodayRange()
        val count = transactionDao.getTransactionCountForDay(startOfDay, endOfDay)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(System.currentTimeMillis())
        val sequence = String.format("%04d", count + 1)
        return "TRX-$dateStr-$sequence"
    }

    override suspend fun getTodayStats(): TransactionStats {
        val (startOfDay, endOfDay) = getTodayRange()
        
        val totalTransactions = transactionDao.getTransactionCountForDay(startOfDay, endOfDay)
        val totalRevenue = transactionDao.getTotalRevenueForDay(startOfDay, endOfDay)
        val totalItemsSold = transactionDao.getTotalItemsSoldForDay(startOfDay, endOfDay)
        
        return TransactionStats(
            totalTransactions = totalTransactions,
            totalRevenue = totalRevenue,
            totalItemsSold = totalItemsSold
        )
    }
    
    /**
     * Get start and end timestamps for today.
     */
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }
}
