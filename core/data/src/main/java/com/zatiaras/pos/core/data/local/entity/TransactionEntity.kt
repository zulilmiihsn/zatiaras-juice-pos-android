package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Transactions.
 * 
 * Stores completed POS transactions.
 * Each transaction has many TransactionItemEntity (one-to-many).
 * 
 * Indexed on:
 * - createdAt: For date-based queries (today's sales, reports)
 * - isSynced: For finding unsynced records
 * - transactionNumber: For lookup by receipt number
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isSynced"]),
        Index(value = ["sessionId"]),
        Index(value = ["transactionNumber"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,                             // UUID
    val transactionNumber: String,              // TRX-YYYYMMDD-XXXX
    val subtotal: Long,                         // Before tax/discount (IDR)
    val discountAmount: Long = 0,               // Discount in rupiah
    val discountPercent: Double = 0.0,          // Discount percentage (0-100)
    val taxAmount: Long = 0,                    // Tax in rupiah
    val taxPercent: Double = 0.0,               // Tax percentage (e.g., 11.0)
    val grandTotal: Long,                       // Final amount customer pays
    val paymentMethod: String,                  // CASH, QRIS, TRANSFER
    val amountPaid: Long,                       // Amount given by customer
    val changeAmount: Long,                     // Change returned
    val notes: String? = null,                  // Optional notes
    val customerName: String? = null,           // Optional customer name
    val sessionId: String? = null,              // Linked Store Session
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
