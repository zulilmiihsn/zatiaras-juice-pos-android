package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.domain.util.DateUtils
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
    private val transactionDao: TransactionDao,
    private val cashRecordDao: com.zatiaras.pos.core.data.local.dao.CashRecordDao
) : ReportRepository {

    override suspend fun getDashboardStats(): DashboardStats {
        // Today's range
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        
        // This week's range (Monday to today)
        val (weekStart, weekEnd) = DateUtils.getThisWeekRange()
        
        // This month's range
        val (monthStart, _) = DateUtils.getThisMonthRange()
        
        // Previous week for comparison
        val (prevWeekStart, prevWeekEnd) = DateUtils.getPreviousWeekRange()
        
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
        val (startDate, endDate) = DateUtils.getLastNDaysRange(days)
        
        try {
            val entities = transactionDao.getDailyRevenue(startDate, endDate)
            
            // Fill in missing days with zero values
            val result = mutableListOf<DailyRevenue>()
            val entityMap = entities.associateBy { it.dayTimestamp }
            
            val iterCalendar = Calendar.getInstance()
            iterCalendar.timeInMillis = startDate
            
            repeat(days) {
                val dayStart = DateUtils.getStartOfDay(iterCalendar.timeInMillis)
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
            // 1. Get POS Revenue
            val posSummary = transactionDao.getRevenueSummary(startDate, endDate)
            val posTransactions = transactionDao.getTransactionCountForDay(startDate, endDate)
            
            // 2. Get Manual Records & Aggregate
            val manualRecords = cashRecordDao.getRecordsListByDateRange(startDate, endDate)
            val manualSummary = aggregateManualRecords(manualRecords)
            
            // 3. Combine Data
            val posNetRevenue = posSummary.grossRevenue - posSummary.totalDiscount
            val operatingRevenue = posNetRevenue + manualSummary.operatingIncome
            val otherRevenue = manualSummary.otherIncome
            val totalRevenue = operatingRevenue + otherRevenue
            
            val totalExpenses = manualSummary.operatingExpense + manualSummary.otherExpense
            
            // 4. Calculate Profit & Tax (UMKM 0.5% on Gross Profit)
            val grossProfit = totalRevenue - totalExpenses
            val tax = if (grossProfit > 0) (grossProfit * 0.005).toLong() else 0L
            val netProfit = grossProfit - tax
            
            return ProfitLossReport(
                periodStart = startDate,
                periodEnd = endDate,
                operatingRevenue = operatingRevenue,
                otherRevenue = otherRevenue,
                grossRevenue = totalRevenue,
                operatingExpenses = manualSummary.operatingExpense,
                otherExpenses = manualSummary.otherExpense,
                totalExpenses = totalExpenses,
                grossProfit = grossProfit,
                tax = tax,
                netProfit = netProfit,
                transactionCount = posTransactions + manualRecords.count { it.type == "INCOME" }
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to get profit/loss report")
            return ProfitLossReport(
                periodStart = startDate,
                periodEnd = endDate,
                operatingRevenue = 0,
                otherRevenue = 0,
                grossRevenue = 0,
                operatingExpenses = 0,
                otherExpenses = 0,
                totalExpenses = 0,
                grossProfit = 0,
                tax = 0,
                netProfit = 0,
                transactionCount = 0
            )
        }
    }
    
    // --- Helper Functions (KISS) ---
    
    private data class ManualRecordSummary(
        val operatingIncome: Long,
        val otherIncome: Long,
        val operatingExpense: Long,
        val otherExpense: Long
    )
    
    private fun aggregateManualRecords(
        records: List<com.zatiaras.pos.core.data.local.entity.CashRecordEntity>
    ): ManualRecordSummary {
        var operatingIncome = 0L
        var otherIncome = 0L
        var operatingExpense = 0L
        var otherExpense = 0L
        
        records.forEach { record ->
            when {
                record.type == "INCOME" && record.category == com.zatiaras.pos.core.domain.model.CashCategories.OPERATING_INCOME -> 
                    operatingIncome += record.amount
                record.type == "INCOME" -> 
                    otherIncome += record.amount
                record.type == "EXPENSE" && com.zatiaras.pos.core.domain.model.CashCategories.OPERATING_EXPENSES.contains(record.category) -> 
                    operatingExpense += record.amount
                record.type == "EXPENSE" -> 
                    otherExpense += record.amount
            }
        }
        
        return ManualRecordSummary(operatingIncome, otherIncome, operatingExpense, otherExpense)
    }
}
