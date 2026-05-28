package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Transaction Line Items.
 *
 * Stores individual items within a transaction.
 * Contains SNAPSHOT data - the product name and price at time of purchase.
 * This ensures historical accuracy even if product prices change.
 *
 * Foreign key to TransactionEntity with CASCADE delete.
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["transactionId"])],
)
data class TransactionItemEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val productId: String,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String? = null,
)
