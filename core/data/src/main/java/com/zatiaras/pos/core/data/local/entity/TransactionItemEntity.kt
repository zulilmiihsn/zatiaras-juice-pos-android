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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey
    val id: String,                             // UUID
    val transactionId: String,                  // FK to transactions
    val productId: String,                      // Reference (NOT FK - product may be deleted)
    val productName: String,                    // Snapshot of name at purchase time
    val productPrice: Long,                     // Snapshot of price at purchase time (IDR)
    val quantity: Int,                          // Quantity ordered
    val subtotal: Long,                         // productPrice * quantity
    val notes: String? = null                   // Item-level notes (e.g., "less ice")
)
