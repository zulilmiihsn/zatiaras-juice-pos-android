package com.zatiaras.pos.core.data.local.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.AddOnEntity

interface AddOnWriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOn(addOn: AddOnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOns(addOns: List<AddOnEntity>)

    @Update
    suspend fun updateAddOn(addOn: AddOnEntity)

    @Query("UPDATE add_ons SET isDeleted = 1, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteAddOn(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE add_ons SET isActive = NOT isActive, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun toggleActive(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE add_ons SET isActive = :isActive, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM add_ons WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun cleanupDeletedAddOns()
}
