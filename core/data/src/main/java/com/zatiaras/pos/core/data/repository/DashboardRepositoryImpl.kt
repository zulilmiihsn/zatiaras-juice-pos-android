package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.domain.repository.DashboardRepository
import com.zatiaras.pos.core.domain.util.DateUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DashboardRepository.
 * 
 * Provides dashboard metrics by aggregating data from TransactionDao.
 * Follows Single Source of Truth (Room) principle.
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : DashboardRepository {

    /**
     * Helper to get today's date range consistently.
     * Avoids repeating DateUtils.getTodayRange() + DateUtils.getEndOfDay() across methods.
     */
    private fun todayRange(): Pair<Long, Long> {
        val (startOfDay, _) = DateUtils.getTodayRange()
        val endOfDay = DateUtils.getEndOfDay()
        return startOfDay to endOfDay
    }

    override suspend fun getTodayRevenue(): Result<Long> {
        return try {
            val (start, end) = todayRange()
            val revenue = transactionDao.getTotalRevenueForDay(start, end)
            Result.success(revenue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's revenue")
            Result.failure(e)
        }
    }

    override suspend fun getTodayTransactionCount(): Result<Int> {
        return try {
            val (start, end) = todayRange()
            val count = transactionDao.getTransactionCountForDay(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's transaction count")
            Result.failure(e)
        }
    }

    override suspend fun getTodayItemsSold(): Result<Int> {
        return try {
            val (start, end) = todayRange()
            val itemsSold = transactionDao.getTotalItemsSoldForDay(start, end)
            Result.success(itemsSold)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's items sold")
            Result.failure(e)
        }
    }
}

