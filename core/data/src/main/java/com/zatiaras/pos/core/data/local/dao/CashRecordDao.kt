package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Cash Record (Buku Kas) operations.
 */
@Dao
interface CashRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CashRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CashRecordEntity>)

    @Update
    suspend fun update(record: CashRecordEntity)

    @Query("SELECT * FROM cash_records WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): CashRecordEntity?

    /**
     * Get all cash records for today.
     */
    @Query("""
        SELECT * FROM cash_records 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """)
    fun getByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<CashRecordEntity>>

    /**
     * Get records list for report generation (suspend)
     */
    @Query("""
        SELECT * FROM cash_records 
        WHERE createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """)
    suspend fun getRecordsListByDateRange(startOfDay: Long, endOfDay: Long): List<CashRecordEntity>

    /**
     * Get today's total income.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM cash_records 
        WHERE type = 'INCOME' 
        AND createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
    """)
    suspend fun getTotalIncomeForDay(startOfDay: Long, endOfDay: Long): Long

    /**
     * Get today's total expense.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM cash_records 
        WHERE type = 'EXPENSE' 
        AND createdAt >= :startOfDay 
        AND createdAt < :endOfDay 
        AND isDeleted = 0
    """)
    suspend fun getTotalExpenseForDay(startOfDay: Long, endOfDay: Long): Long

    /**
     * Get all unsynced records.
     */
    @Query("SELECT * FROM cash_records WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CashRecordEntity>

    /**
     * Get count of unsynced records (efficient COUNT instead of loading all).
     */
    @Query("SELECT COUNT(*) FROM cash_records WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /**
     * Mark record as synced.
     */
    @Query("UPDATE cash_records SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Soft delete a record.
     */
    @Query("UPDATE cash_records SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}
