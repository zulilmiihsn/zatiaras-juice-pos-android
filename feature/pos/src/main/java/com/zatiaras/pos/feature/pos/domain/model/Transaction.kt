package com.zatiaras.pos.feature.pos.domain.model

/**
 * Represents a completed transaction.
 */
data class Transaction(
    val id: String,
    val transactionNumber: String,
    val items: List<TransactionItem>,
    val subtotal: Long,
    val discountAmount: Long,
    val discountPercent: Double,
    val taxAmount: Long,
    val taxPercent: Double,
    val grandTotal: Long,
    val paymentMethod: PaymentMethod,
    val amountPaid: Long,
    val changeAmount: Long,
    val notes: String?,
    val customerName: String?,
    val createdAt: Long,
    val isSynced: Boolean
) {
    /**
     * Total quantity of all items in this transaction.
     */
    val totalQuantity: Int get() = items.sumOf { it.quantity }
}

/**
 * Represents a line item in a transaction.
 * Contains snapshot data at the time of purchase.
 */
data class TransactionItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String?
)
