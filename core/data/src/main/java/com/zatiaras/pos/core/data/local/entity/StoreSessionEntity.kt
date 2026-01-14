package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a store session (Shift/Buka Toko).
 * Maps to 'sesi_toko' table in Supabase.
 */
@Entity(
    tableName = "store_sessions",
    indices = [
        Index("isActive"),
        Index("openingTime"),
        Index("isSynced")
    ]
)
data class StoreSessionEntity(
    @PrimaryKey
    val id: String,
    val openingCash: Long,
    val openingTime: Long,
    val closingTime: Long? = null,
    val isActive: Boolean,
    val branchId: String? = null,
    
    // Sync metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
