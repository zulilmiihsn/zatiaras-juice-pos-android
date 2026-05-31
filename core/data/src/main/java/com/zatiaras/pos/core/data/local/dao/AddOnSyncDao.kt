package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.AddOnEntity

interface AddOnSyncDao {
    @Query("SELECT * FROM add_ons WHERE isSynced = 0")
    suspend fun getUnsyncedAddOns(): List<AddOnEntity>

    @Query("UPDATE add_ons SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE add_ons SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<String>)

    @Query("SELECT * FROM add_ons WHERE updatedAt > :timestamp")
    suspend fun getAddOnsUpdatedAfter(timestamp: Long): List<AddOnEntity>

    @Query("SELECT COUNT(*) FROM add_ons WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}
