package com.zatiaras.pos.feature.pos.data.mapper

import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import com.zatiaras.pos.core.data.local.entity.TransactionWithItems
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.CartItem
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionItem
import java.util.UUID

/**
 * Mapper functions for Transaction entities and domain models.
 */

/**
 * Convert TransactionEntity + items to domain Transaction.
 */
fun TransactionEntity.toDomain(items: List<TransactionItemEntity>): Transaction {
    return Transaction(
        id = id,
        transactionNumber = transactionNumber,
        items = items.map { it.toDomain() },
        subtotal = subtotal,
        discountAmount = discountAmount,
        discountPercent = discountPercent,
        taxAmount = taxAmount,
        taxPercent = taxPercent,
        grandTotal = grandTotal,
        paymentMethod = PaymentMethod.valueOf(paymentMethod),
        amountPaid = amountPaid,
        changeAmount = changeAmount,
        notes = notes,
        customerName = customerName,
        createdAt = createdAt,
        isSynced = isSynced
    )
}

/**
 * Convert TransactionItemEntity to domain TransactionItem.
 */
fun TransactionItemEntity.toDomain(): TransactionItem {
    return TransactionItem(
        id = id,
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        quantity = quantity,
        subtotal = subtotal,
        notes = notes
    )
}

/**
 * Convert CartItem to TransactionItemEntity.
 * Creates snapshot of product data at time of purchase.
 */
fun CartItem.toEntity(transactionId: String): TransactionItemEntity {
    return TransactionItemEntity(
        id = UUID.randomUUID().toString(),
        transactionId = transactionId,
        productId = product.id,
        productName = product.name,
        productPrice = product.price,
        quantity = quantity,
        subtotal = subtotal,
        notes = notes
    )
}

/**
 * Convert Cart items to list of TransactionItemEntity.
 */
fun Cart.toItemEntities(transactionId: String): List<TransactionItemEntity> {
    return items.map { it.toEntity(transactionId) }
}

/**
 * Create TransactionEntity from checkout data.
 */
fun createTransactionEntity(
    id: String,
    transactionNumber: String,
    cart: Cart,
    paymentMethod: PaymentMethod,
    amountPaid: Long,
    discountPercent: Double,
    taxPercent: Double,
    notes: String?,
    customerName: String?
): TransactionEntity {
    val subtotal = cart.subtotal
    val discountAmount = (subtotal * discountPercent / 100).toLong()
    val afterDiscount = subtotal - discountAmount
    val taxAmount = (afterDiscount * taxPercent / 100).toLong()
    val grandTotal = afterDiscount + taxAmount
    val changeAmount = if (paymentMethod == PaymentMethod.CASH) {
        maxOf(0, amountPaid - grandTotal)
    } else {
        0
    }
    
    return TransactionEntity(
        id = id,
        transactionNumber = transactionNumber,
        subtotal = subtotal,
        discountAmount = discountAmount,
        discountPercent = discountPercent,
        taxAmount = taxAmount,
        taxPercent = taxPercent,
        grandTotal = grandTotal,
        paymentMethod = paymentMethod.name,
        amountPaid = if (paymentMethod == PaymentMethod.CASH) amountPaid else grandTotal,
        changeAmount = changeAmount,
        notes = notes,
        customerName = customerName,
        isSynced = false
    )
}

/**
 * Convert TransactionWithItems (Room @Relation result) to domain Transaction.
 * This avoids N+1 query problem by using Room's relation mapping.
 */
fun TransactionWithItems.toDomain(): Transaction {
    return transaction.toDomain(items)
}
