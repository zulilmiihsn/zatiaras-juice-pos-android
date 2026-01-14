package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Cash Records (Buku Kas).
 * 
 * Stores manual income/expense entries that are not from POS transactions.
 * 
 * Indexed on:
 * - createdAt: For date-based queries
 * - isSynced: For finding unsynced records
 * - type: For filtering by income/expense
 */
@Entity(
    tableName = "cash_records",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isSynced"]),
        Index(value = ["sessionId"]),
        Index(value = ["type"])
    ]
)
data class CashRecordEntity(
    @PrimaryKey
    val id: String,
    val type: String,                    // INCOME or EXPENSE
    val amount: Long,                    // Always positive (in IDR)
    val description: String,
    val category: String? = null,
    val notes: String? = null,
    val sessionId: String? = null,              // Linked Store Session
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
