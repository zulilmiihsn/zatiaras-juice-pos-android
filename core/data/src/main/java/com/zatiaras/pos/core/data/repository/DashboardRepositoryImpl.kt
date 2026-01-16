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

    override suspend fun getTodayRevenue(): Long {
        return try {
            val (startOfDay, endOfDay) = DateUtils.getTodayRange()
            val endOfDayActual = DateUtils.getEndOfDay()
            transactionDao.getTotalRevenueForDay(startOfDay, endOfDayActual)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's revenue")
            0L
        }
    }

    override suspend fun getTodayTransactionCount(): Int {
        return try {
            val (startOfDay, _) = DateUtils.getTodayRange()
            val endOfDayActual = DateUtils.getEndOfDay()
            transactionDao.getTransactionCountForDay(startOfDay, endOfDayActual)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's transaction count")
            0
        }
    }

    override suspend fun getTodayItemsSold(): Int {
        return try {
            val (startOfDay, _) = DateUtils.getTodayRange()
            val endOfDayActual = DateUtils.getEndOfDay()
            transactionDao.getTotalItemsSoldForDay(startOfDay, endOfDayActual)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get today's items sold")
            0
        }
    }
}
