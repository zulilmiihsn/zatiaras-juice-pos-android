package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Transaction operations with Supabase.
 * 
 * Handles:
 * - Uploading transactions to Supabase
 * - Fetching transactions for sync
 * - Delta sync (only changed items)
 * 
 * Supabase tables:
 * - transaksi: Main transaction records
 * - transaksi_item: Line items for each transaction
 */
@Singleton
class TransactionRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_TRANSAKSI = "transaksi"
        private const val TABLE_TRANSAKSI_ITEM = "transaksi_item"
    }

    // ==================== UPLOAD TO REMOTE ====================

    /**
     * Upload a transaction with its items to Supabase.
     * Uses upsert for idempotency (safe to retry on failure).
     */
    suspend fun uploadTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Upload transaction header
            val transactionDto = transaction.toDto()
            postgrest.from(TABLE_TRANSAKSI).upsert(transactionDto)
            Timber.d("Uploaded transaction: ${transaction.id}")

            // Upload transaction items
            val itemDtos = items.map { it.toDto() }
            if (itemDtos.isNotEmpty()) {
                postgrest.from(TABLE_TRANSAKSI_ITEM).upsert(itemDtos)
                Timber.d("Uploaded ${itemDtos.size} items for transaction: ${transaction.id}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload transaction: ${transaction.id}")
            Result.failure(e)
        }
    }

    /**
     * Upload multiple transactions in batch.
     */
    suspend fun uploadTransactions(
        transactions: List<Pair<TransactionEntity, List<TransactionItemEntity>>>
    ): Result<Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        var lastError: Exception? = null

        for ((transaction, items) in transactions) {
            uploadTransaction(transaction, items).fold(
                onSuccess = { successCount++ },
                onFailure = { lastError = it as? Exception }
            )
        }

        if (successCount == transactions.size) {
            Timber.d("Successfully uploaded all ${successCount} transactions")
            Result.success(successCount)
        } else if (successCount > 0) {
            Timber.w("Partially uploaded ${successCount}/${transactions.size} transactions")
            Result.success(successCount)
        } else {
            Result.failure(lastError ?: Exception("Failed to upload any transactions"))
        }
    }

    // ==================== FETCH FROM REMOTE ====================

    /**
     * Fetch transactions updated after given timestamp.
     * Used for pulling remote changes to local.
     */
    suspend fun fetchTransactions(lastSyncTimestamp: Long = 0): Result<List<TransactionEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = if (lastSyncTimestamp > 0) {
                    postgrest.from(TABLE_TRANSAKSI)
                        .select()
                        .decodeList<TransaksiDto>()
                        .filter { it.updatedAt > lastSyncTimestamp }
                } else {
                    postgrest.from(TABLE_TRANSAKSI)
                        .select()
                        .decodeList<TransaksiDto>()
                }

                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} transactions from remote (since $lastSyncTimestamp)")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch transactions")
                Result.failure(e)
            }
        }

    /**
     * Fetch items for a specific transaction.
     */
    suspend fun fetchTransactionItems(transactionId: String): Result<List<TransactionItemEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val response = postgrest.from(TABLE_TRANSAKSI_ITEM)
                    .select {
                        filter { eq("transaction_id", transactionId) }
                    }
                    .decodeList<TransaksiItemDto>()

                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} items for transaction: $transactionId")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch transaction items: $transactionId")
                Result.failure(e)
            }
        }
}

// ==================== DTOs ====================

/**
 * DTO for "transaksi" table in Supabase.
 */
@Serializable
data class TransaksiDto(
    val id: String,
    @SerialName("transaction_number")
    val transactionNumber: String,
    val subtotal: Long,
    @SerialName("discount_amount")
    val discountAmount: Long = 0,
    @SerialName("discount_percent")
    val discountPercent: Double = 0.0,
    @SerialName("tax_amount")
    val taxAmount: Long = 0,
    @SerialName("tax_percent")
    val taxPercent: Double = 0.0,
    @SerialName("grand_total")
    val grandTotal: Long,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("amount_paid")
    val amountPaid: Long,
    @SerialName("change_amount")
    val changeAmount: Long,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
) {
    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        transactionNumber = transactionNumber,
        subtotal = subtotal,
        discountAmount = discountAmount,
        discountPercent = discountPercent,
        taxAmount = taxAmount,
        taxPercent = taxPercent,
        grandTotal = grandTotal,
        paymentMethod = paymentMethod,
        amountPaid = amountPaid,
        changeAmount = changeAmount,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = true, // Came from remote, so it's synced
        isDeleted = isDeleted
    )
}

/**
 * DTO for "transaksi_item" table in Supabase.
 */
@Serializable
data class TransaksiItemDto(
    val id: String,
    @SerialName("transaction_id")
    val transactionId: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("product_name")
    val productName: String,
    @SerialName("product_price")
    val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String? = null
) {
    fun toEntity(): TransactionItemEntity = TransactionItemEntity(
        id = id,
        transactionId = transactionId,
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        quantity = quantity,
        subtotal = subtotal,
        notes = notes
    )
}

// ==================== Extension Functions ====================

/**
 * Convert TransactionEntity to DTO for upload.
 */
fun TransactionEntity.toDto(): TransaksiDto = TransaksiDto(
    id = id,
    transactionNumber = transactionNumber,
    subtotal = subtotal,
    discountAmount = discountAmount,
    discountPercent = discountPercent,
    taxAmount = taxAmount,
    taxPercent = taxPercent,
    grandTotal = grandTotal,
    paymentMethod = paymentMethod,
    amountPaid = amountPaid,
    changeAmount = changeAmount,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

/**
 * Convert TransactionItemEntity to DTO for upload.
 */
fun TransactionItemEntity.toDto(): TransaksiItemDto = TransaksiItemDto(
    id = id,
    transactionId = transactionId,
    productId = productId,
    productName = productName,
    productPrice = productPrice,
    quantity = quantity,
    subtotal = subtotal,
    notes = notes
)
