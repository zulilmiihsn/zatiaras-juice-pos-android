package com.zatiaras.pos.feature.pos.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.domain.util.DateUtils
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
        val (startOfDay, endOfDay) = DateUtils.getTodayRange()
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

    override suspend fun getTodaySummary(): Result<CashSummary> {
        return try {
            val (startOfDay, endOfDay) = DateUtils.getTodayRange()
            
            val totalIncome = cashRecordDao.getTotalIncomeForDay(startOfDay, endOfDay)
            val totalExpense = cashRecordDao.getTotalExpenseForDay(startOfDay, endOfDay)
            
            val summary = CashSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netCash = totalIncome - totalExpense
            )
            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today cash summary")
            Result.failure(e)
        }
    }

    override suspend fun syncToRemote(): Result<Unit> {
        // TODO(P1): Implement sinkronisasi buku kas ke remote via sync engine (lihat pola ProductSyncer).
        val unsynced = cashRecordDao.getUnsynced()
        Timber.d("Found ${unsynced.size} unsynced cash records (sync not yet implemented)")
        return Result.success(Unit)
    }
}

