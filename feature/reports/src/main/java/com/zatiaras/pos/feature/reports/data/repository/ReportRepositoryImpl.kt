package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import com.zatiaras.pos.core.domain.model.CashCategories
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProductSaleItem
import com.zatiaras.pos.feature.reports.domain.model.RawProfitLossData
import com.zatiaras.pos.feature.reports.domain.model.ManualCashRecord
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.model.TransactionSummaryItem
import com.zatiaras.pos.feature.reports.domain.model.CashFlowItem
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Implementation of ReportRepository.
 * Aggregates data from TransactionDao for reports.
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val cashRecordDao: CashRecordDao
) : ReportRepository {

    override suspend fun getDashboardStats(): Result<DashboardStats> {
        // Today's range
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        
        // This week's range (Monday to today)
        val (weekStart, weekEnd) = DateUtils.getThisWeekRange()
        
        // This month's range
        val (monthStart, _) = DateUtils.getThisMonthRange()
        
        // Previous week for comparison
        val (prevWeekStart, prevWeekEnd) = DateUtils.getPreviousWeekRange()
        
        return try {
            coroutineScope {
                val todayRevenueDeferred = async { transactionDao.getTotalRevenueForDay(todayStart, todayEnd) }
                val todayTransactionsDeferred = async { transactionDao.getTransactionCountForDay(todayStart, todayEnd) }
                val todayItemsDeferred = async { transactionDao.getTotalItemsSoldForDay(todayStart, todayEnd) }
                
                val weeklySummaryDeferred = async { transactionDao.getRevenueSummary(weekStart, weekEnd) }
                val monthlySummaryDeferred = async { transactionDao.getRevenueSummary(monthStart, todayEnd) }
                val prevWeekSummaryDeferred = async { transactionDao.getRevenueSummary(prevWeekStart, prevWeekEnd) }

                val todayRevenue = todayRevenueDeferred.await()
                val todayTransactions = todayTransactionsDeferred.await()
                val todayItems = todayItemsDeferred.await()
                
                val weeklySummary = weeklySummaryDeferred.await()
                val monthlySummary = monthlySummaryDeferred.await()
                val prevWeekSummary = prevWeekSummaryDeferred.await()
                
                // Calculate growth percentage
                val growth = if (prevWeekSummary.totalRevenue > 0) {
                    ((weeklySummary.totalRevenue - prevWeekSummary.totalRevenue).toDouble() / 
                        prevWeekSummary.totalRevenue) * 100
                } else {
                    if (weeklySummary.totalRevenue > 0) 100.0 else 0.0
                }
                
                val stats = DashboardStats(
                    todayRevenue = todayRevenue,
                    todayTransactions = todayTransactions,
                    todayItemsSold = todayItems,
                    weeklyRevenue = weeklySummary.totalRevenue,
                    monthlyRevenue = monthlySummary.totalRevenue,
                    revenueGrowthPercent = growth
                )
                Result.success(stats)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get dashboard stats")
            Result.failure(e)
        }
    }

    override suspend fun getDailyRevenueHistory(days: Int): Result<List<DailyRevenue>> {
        val (startDate, endDate) = DateUtils.getLastNDaysRange(days)
        
        return try {
            // Calculate timezone offset for SQL grouping
            val timeOffset = java.util.TimeZone.getDefault().rawOffset.toLong()
            
            // Use SQL aggregation for better performance on large datasets
            val dailyRevenueEntities = transactionDao.getDailyRevenue(startDate, endDate, timeOffset)
            
            // Map to a dictionary for fast lookup
            val dailyMap = dailyRevenueEntities.associateBy { it.dayTimestamp }
            
            val result = mutableListOf<DailyRevenue>()
            var currentDate = Instant.ofEpochMilli(startDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            // Iterate through each day in the range to ensure continuous data (fill gaps with 0)
            repeat(days) {
                val dayStart = currentDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val dailyEntity = dailyMap[dayStart]
                
                result.add(
                    DailyRevenue(
                        date = dayStart,
                        revenue = dailyEntity?.revenue ?: 0L,
                        transactionCount = dailyEntity?.transactionCount ?: 0
                    )
                )
                
                currentDate = currentDate.plusDays(1)
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get daily revenue history")
            Result.failure(e)
        }
    }

    override suspend fun getTopSellingProducts(
        startDate: Long,
        endDate: Long,
        limit: Int
    ): Result<List<TopProduct>> {
        return try {
            val products = transactionDao.getTopSellingProducts(startDate, endDate, limit)
                .map { entity ->
                    TopProduct(
                        productId = entity.productId,
                        productName = entity.productName,
                        quantitySold = entity.totalQuantity,
                        totalRevenue = entity.totalRevenue
                    )
                }
            Result.success(products)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get top selling products")
            Result.failure(e)
        }
    }

    override suspend fun getRawProfitLossData(startDate: Long, endDate: Long): Result<RawProfitLossData> {
        return try {
            // 1. Get POS Revenue
            val posSummary = transactionDao.getRevenueSummary(startDate, endDate)
            val posTransactions = transactionDao.getTransactionCountForDay(startDate, endDate)
            
            // 2. Get Product Sales Breakdown
            val productSales = transactionDao.getTopSellingProducts(startDate, endDate, 100)
                .map { entity ->
                    ProductSaleItem(
                        productId = entity.productId,
                        productName = entity.productName,
                        quantity = entity.totalQuantity,
                        revenue = entity.totalRevenue
                    )
                }
            
            // 3. Get Manual Records
            val manualRecordsEntity = cashRecordDao.getRecordsListByDateRange(startDate, endDate)
            val manualRecords = manualRecordsEntity.map { entity ->
                ManualCashRecord(
                    type = entity.type,
                    category = entity.category,
                    amount = entity.amount,
                    description = entity.description,
                    isDeleted = entity.isDeleted
                )
            }
            
            val rawData = RawProfitLossData(
                posGrossRevenue = posSummary.grossRevenue,
                posTotalDiscount = posSummary.totalDiscount,
                posTransactions = posTransactions,
                productSales = productSales,
                manualRecords = manualRecords
            )
            Result.success(rawData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get raw profit/loss report data")
            Result.failure(e)
        }
    }

    override suspend fun getTransactionsForAnalysis(
        startDate: Long,
        endDate: Long
    ): Result<List<TransactionSummaryItem>> {
        return try {
            val entities = transactionDao.getTransactionsForReports(startDate, endDate)
            val items = entities.map { entity ->
                TransactionSummaryItem(
                    createdAt = entity.createdAt,
                    paymentMethod = entity.paymentMethod,
                    grandTotal = entity.grandTotal
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get transactions for analysis")
            Result.failure(e)
        }
    }

    override suspend fun getCashRecordsForAnalysis(
        startDate: Long,
        endDate: Long
    ): Result<List<CashFlowItem>> {
        return try {
            val entities = cashRecordDao.getRecordsListByDateRange(startDate, endDate)
            val items = entities.map { entity ->
                CashFlowItem(
                    type = entity.type,
                    amount = entity.amount,
                    description = entity.description,
                    category = entity.category
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cash records for analysis")
            Result.failure(e)
        }
    }

}
