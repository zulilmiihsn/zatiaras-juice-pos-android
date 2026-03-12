package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
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
 * Design Principles:
 * - Separate Read/Write DTOs
 * - Exclude timestamps from Write DTOs (Supabase handles them)
 * - Safe timestamp parsing for Read DTOs
 */
@Singleton
class TransactionRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_TRANSAKSI = "transaksi"
        private const val TABLE_TRANSAKSI_ITEM = "transaksi_item"
    }

    // ==================== UPLOAD TO REMOTE (PUSH) ====================

    /**
     * Upload a transaction with its items to Supabase.
     */
    suspend fun uploadTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Upload transaction header using Write DTO
            val transactionDto = transaction.toWriteDto()
            postgrest.from(TABLE_TRANSAKSI).upsert(transactionDto)
            Timber.d("Uploaded transaction: ${transaction.id}")

            // Upload transaction items using Write DTO
            val itemDtos = items.map { it.toWriteDto() }
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
        try {
            if (transactions.isEmpty()) return@withContext Result.success(0)

            val transactionDtos = transactions.map { it.first.toWriteDto() }
            val itemDtos = transactions.flatMap { it.second.map { item -> item.toWriteDto() } }

            postgrest.from(TABLE_TRANSAKSI).upsert(transactionDtos)
            if (itemDtos.isNotEmpty()) {
                postgrest.from(TABLE_TRANSAKSI_ITEM).upsert(itemDtos)
            }

            Timber.d("Bulk uploaded ${transactionDtos.size} transactions with ${itemDtos.size} items")
            Result.success(transactions.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to bulk upload transactions")
            Result.failure(e)
        }
    }

    // ==================== FETCH FROM REMOTE (PULL) ====================

    /**
     * Fetch transactions with items (delta sync supported).
     */
    suspend fun fetchTransactionsExtended(
        lastSyncTimestamp: Long = 0,
        page: Int = 0,
        pageSize: Int = 50
    ): Result<List<Pair<TransactionEntity, List<TransactionItemEntity>>>> = 
        withContext(Dispatchers.IO) {
            try {
                if (lastSyncTimestamp > 0L) {
                    Timber.d("Delta transaction sync is not implemented yet, running paged full pull")
                }
                val from = page * pageSize
                val to = from + pageSize - 1
                
                // 1. Fetch transaction headers
                val transactions = postgrest.from(TABLE_TRANSAKSI)
                    .select {
                        range(from.toLong(), to.toLong())
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<TransaksiReadDto>()

                if (transactions.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // 2. Fetch ALL items for these transactions in one batch query
                val transactionIds = transactions.map { it.id }
                val allItems = postgrest.from(TABLE_TRANSAKSI_ITEM)
                    .select {
                        filter { isIn("transaksi_id", transactionIds) }
                    }
                    .decodeList<TransaksiItemReadDto>()

                // 3. Group items by transaction ID
                val itemsByTransactionId = allItems
                    .map { it.toEntity() }
                    .groupBy { it.transactionId }

                // 4. Pair each transaction with its items
                val results = transactions.map { dto ->
                    val entity = dto.toEntity()
                    val items = itemsByTransactionId[entity.id] ?: emptyList()
                    Pair(entity, items)
                }
                
                Timber.d("Fetched ${results.size} transactions with ${allItems.size} items (page $page)")
                Result.success(results)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch transactions")
                Result.failure(e)
            }
        }

    suspend fun fetchTransactionItems(transactionId: String): Result<List<TransactionItemEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val response = postgrest.from(TABLE_TRANSAKSI_ITEM)
                    .select {
                        filter { eq("transaksi_id", transactionId) }
                    }
                    .decodeList<TransaksiItemReadDto>()

                val entities = response.map { it.toEntity() }
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch items for: $transactionId")
                Result.failure(e)
            }
        }
}

// ==================== DTOs: READ ====================

@Serializable
data class TransaksiReadDto(
    val id: String,
    @SerialName("transaction_number") val transactionNumber: String,
    val subtotal: Long,
    @SerialName("discount_amount") val discountAmount: Long = 0,
    @SerialName("discount_percent") val discountPercent: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Long = 0,
    @SerialName("tax_percent") val taxPercent: Double = 0.0,
    @SerialName("grand_total") val grandTotal: Long,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("amount_paid") val amountPaid: Long,
    @SerialName("change_amount") val changeAmount: Long,
    val notes: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
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
        customerName = customerName,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt),
        isSynced = true,
        isDeleted = isDeleted
    )
}

@Serializable
data class TransaksiItemReadDto(
    val id: String,
    @SerialName("transaksi_id") val transactionId: String,
    @SerialName("produk_id") val productId: String? = null,
    @SerialName("produk_name") val productName: String,
    @SerialName("produk_price") val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String? = null
) {
    fun toEntity(): TransactionItemEntity = TransactionItemEntity(
        id = id,
        transactionId = transactionId,
        productId = productId ?: "",
        productName = productName,
        productPrice = productPrice,
        quantity = quantity,
        subtotal = subtotal,
        notes = notes
    )
}

// ==================== DTOs: WRITE ====================

@Serializable
data class TransaksiWriteDto(
    val id: String,
    @SerialName("transaction_number") val transactionNumber: String,
    val subtotal: Long,
    @SerialName("discount_amount") val discountAmount: Long,
    @SerialName("discount_percent") val discountPercent: Double,
    @SerialName("tax_amount") val taxAmount: Long,
    @SerialName("tax_percent") val taxPercent: Double,
    @SerialName("grand_total") val grandTotal: Long,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("amount_paid") val amountPaid: Long,
    @SerialName("change_amount") val changeAmount: Long,
    val notes: String?,
    @SerialName("customer_name") val customerName: String?,
    @SerialName("is_deleted") val isDeleted: Boolean
)

@Serializable
data class TransaksiItemWriteDto(
    val id: String,
    @SerialName("transaksi_id") val transactionId: String,
    @SerialName("produk_id") val productId: String?,
    @SerialName("produk_name") val productName: String,
    @SerialName("produk_price") val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String?
)

// ==================== MAPPERS ====================

fun TransactionEntity.toWriteDto(): TransaksiWriteDto = TransaksiWriteDto(
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
    customerName = customerName,
    isDeleted = isDeleted
)

fun TransactionItemEntity.toWriteDto(): TransaksiItemWriteDto = TransaksiItemWriteDto(
    id = id,
    transactionId = transactionId,
    productId = productId.ifBlank { null },
    productName = productName,
    productPrice = productPrice,
    quantity = quantity,
    subtotal = subtotal,
    notes = notes
)

private fun parseTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    return try {
        val cleanStr = isoString.replace(" ", "T")
        val withZ = if (!cleanStr.contains("Z") && !cleanStr.contains("+")) "${cleanStr}Z" else cleanStr
        java.time.Instant.parse(withZ).toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
