package com.zatiaras.pos.feature.pos.domain.repository

import com.zatiaras.pos.feature.pos.domain.model.CashRecord
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Cash Record (Buku Kas) operations.
 */
interface CashRecordRepository {
    
    /**
     * Create a new cash record.
     */
    suspend fun createRecord(
        type: CashRecordType,
        amount: Long,
        description: String,
        category: String? = null,
        notes: String? = null,
        date: Long = System.currentTimeMillis()
    ): Result<CashRecord>
    
    /**
     * Get a cash record by ID.
     */
    suspend fun getRecordById(id: String): CashRecord?
    
    /**
     * Get today's cash records.
     */
    fun getTodayRecords(): Flow<List<CashRecord>>
    
    /**
     * Get records by date range.
     */
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<CashRecord>>
    
    /**
     * Delete a cash record.
     */
    suspend fun deleteRecord(id: String): Result<Unit>
    
    /**
     * Get today's cash summary.
     */
    suspend fun getTodaySummary(): Result<CashSummary>
    
    /**
     * Push unsynced records to remote.
     */
    suspend fun syncToRemote(): Result<Unit>
}

/**
 * Summary of cash records for a period.
 */
data class CashSummary(
    val totalIncome: Long,
    val totalExpense: Long,
    val netCash: Long
)
