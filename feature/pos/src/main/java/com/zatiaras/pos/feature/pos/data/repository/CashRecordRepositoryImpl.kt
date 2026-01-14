package com.zatiaras.pos.feature.pos.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.feature.pos.data.mapper.toDomain
import com.zatiaras.pos.feature.pos.data.mapper.toDomainList
import com.zatiaras.pos.feature.pos.data.mapper.toEntity
import com.zatiaras.pos.feature.pos.domain.model.CashRecord
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import com.zatiaras.pos.feature.pos.domain.repository.CashRecordRepository
import com.zatiaras.pos.feature.pos.domain.repository.CashSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CashRecordRepository.
 * 
 * Offline-first design - all records saved to Room first.
 */
@Singleton
class CashRecordRepositoryImpl @Inject constructor(
    private val cashRecordDao: CashRecordDao
) : CashRecordRepository {

    override suspend fun createRecord(
        type: CashRecordType,
        amount: Long,
        description: String,
        category: String?,
        notes: String?,
        date: Long
    ): Result<CashRecord> {
        return try {
            val record = CashRecord(
                id = UUID.randomUUID().toString(),
                type = type,
                amount = amount,
                description = description,
                category = category,
                notes = notes,
                createdAt = date,
                isSynced = false
            )
            
            cashRecordDao.insert(record.toEntity())
            Timber.d("Cash record created: ${record.id} - ${record.type} ${record.amount}")
            
            Result.success(record)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create cash record")
            Result.failure(e)
        }
    }

    override suspend fun getRecordById(id: String): CashRecord? {
        return cashRecordDao.getById(id)?.toDomain()
    }

    override fun getTodayRecords(): Flow<List<CashRecord>> {
        val (startOfDay, endOfDay) = getTodayRange()
        return cashRecordDao.getByDateRange(startOfDay, endOfDay)
            .map { entities -> entities.toDomainList() }
    }

    override fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<CashRecord>> {
        return cashRecordDao.getByDateRange(startDate, endDate)
            .map { entities -> entities.toDomainList() }
    }

    override suspend fun deleteRecord(id: String): Result<Unit> {
        return try {
            cashRecordDao.softDelete(id)
            Timber.d("Cash record deleted: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete cash record: $id")
            Result.failure(e)
        }
    }

    override suspend fun getTodaySummary(): CashSummary {
        val (startOfDay, endOfDay) = getTodayRange()
        
        val totalIncome = cashRecordDao.getTotalIncomeForDay(startOfDay, endOfDay)
        val totalExpense = cashRecordDao.getTotalExpenseForDay(startOfDay, endOfDay)
        
        return CashSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCash = totalIncome - totalExpense
        )
    }

    override suspend fun syncToRemote(): Result<Unit> {
        // TODO: Implement in Phase 5 - Sync Engine
        val unsynced = cashRecordDao.getUnsynced()
        Timber.d("Found ${unsynced.size} unsynced cash records (sync not yet implemented)")
        return Result.success(Unit)
    }
    
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }
}
