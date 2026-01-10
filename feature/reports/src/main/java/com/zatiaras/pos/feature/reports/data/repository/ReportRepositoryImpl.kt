package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReportRepository.
 * Aggregates data from TransactionDao for reports.
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : ReportRepository {

    override suspend fun getDashboardStats(): DashboardStats {
        val calendar = Calendar.getInstance()
        
        // Today's range
        val todayStart = getStartOfDay(calendar.timeInMillis)
        val todayEnd = getEndOfDay(calendar.timeInMillis)
        
        // This week's range (Monday to today)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = getStartOfDay(calendar.timeInMillis)
        calendar.timeInMillis = System.currentTimeMillis()
        val weekEnd = todayEnd
        
        // This month's range
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = getStartOfDay(calendar.timeInMillis)
        calendar.timeInMillis = System.currentTimeMillis()
        
        // Previous week for comparison
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val prevWeekStart = getStartOfDay(calendar.timeInMillis)
        val prevWeekEnd = getEndOfDay(calendar.timeInMillis)
        
        try {
            val todayRevenue = transactionDao.getTotalRevenueForDay(todayStart, todayEnd)
            val todayTransactions = transactionDao.getTransactionCountForDay(todayStart, todayEnd)
            val todayItems = transactionDao.getTotalItemsSoldForDay(todayStart, todayEnd)
            
            val weeklySummary = transactionDao.getRevenueSummary(weekStart, weekEnd)
            val monthlySummary = transactionDao.getRevenueSummary(monthStart, todayEnd)
            val prevWeekSummary = transactionDao.getRevenueSummary(prevWeekStart, prevWeekEnd)
            
            // Calculate growth percentage
            val growth = if (prevWeekSummary.totalRevenue > 0) {
                ((weeklySummary.totalRevenue - prevWeekSummary.totalRevenue).toDouble() / 
                    prevWeekSummary.totalRevenue) * 100
            } else {
                if (weeklySummary.totalRevenue > 0) 100.0 else 0.0
            }
            
            return DashboardStats(
                todayRevenue = todayRevenue,
                todayTransactions = todayTransactions,
                todayItemsSold = todayItems,
                weeklyRevenue = weeklySummary.totalRevenue,
                monthlyRevenue = monthlySummary.totalRevenue,
                revenueGrowthPercent = growth
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get dashboard stats")
            return DashboardStats(0, 0, 0, 0, 0, 0.0)
        }
    }

    override suspend fun getDailyRevenueHistory(days: Int): List<DailyRevenue> {
        val calendar = Calendar.getInstance()
        val endDate = getEndOfDay(calendar.timeInMillis)
        
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startDate = getStartOfDay(calendar.timeInMillis)
        
        try {
            val entities = transactionDao.getDailyRevenue(startDate, endDate)
            
            // Fill in missing days with zero values
            val result = mutableListOf<DailyRevenue>()
            val entityMap = entities.associateBy { it.dayTimestamp }
            
            val iterCalendar = Calendar.getInstance()
            iterCalendar.timeInMillis = startDate
            
            repeat(days) {
                val dayStart = getStartOfDay(iterCalendar.timeInMillis)
                val entity = entityMap[dayStart]
                
                result.add(
                    DailyRevenue(
                        date = dayStart,
                        revenue = entity?.revenue ?: 0L,
                        transactionCount = entity?.transactionCount ?: 0
                    )
                )
                
                iterCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "Failed to get daily revenue history")
            return emptyList()
        }
    }

    override suspend fun getTopSellingProducts(
        startDate: Long,
        endDate: Long,
        limit: Int
    ): List<TopProduct> {
        try {
            return transactionDao.getTopSellingProducts(startDate, endDate, limit)
                .map { entity ->
                    TopProduct(
                        productId = entity.productId,
                        productName = entity.productName,
                        quantitySold = entity.totalQuantity,
                        totalRevenue = entity.totalRevenue
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get top selling products")
            return emptyList()
        }
    }

    override suspend fun getProfitLossReport(startDate: Long, endDate: Long): ProfitLossReport {
        try {
            val summary = transactionDao.getRevenueSummary(startDate, endDate)
            val transactionCount = transactionDao.getTransactionCountForDay(startDate, endDate)
            
            // Net revenue = gross - discount
            val netRevenue = summary.grossRevenue - summary.totalDiscount
            
            // For now, estimated cost is 0 (would need product cost data)
            val estimatedCost = 0L
            val grossProfit = summary.totalRevenue - estimatedCost
            
            return ProfitLossReport(
                periodStart = startDate,
                periodEnd = endDate,
                grossRevenue = summary.grossRevenue,
                totalDiscount = summary.totalDiscount,
                netRevenue = netRevenue,
                totalTax = summary.totalTax,
                grandTotal = summary.totalRevenue,
                estimatedCost = estimatedCost,
                grossProfit = grossProfit,
                transactionCount = transactionCount
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get profit/loss report")
            return ProfitLossReport(
                periodStart = startDate,
                periodEnd = endDate,
                grossRevenue = 0,
                totalDiscount = 0,
                netRevenue = 0,
                totalTax = 0,
                grandTotal = 0,
                estimatedCost = 0,
                grossProfit = 0,
                transactionCount = 0
            )
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
