package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.feature.reports.domain.model.CashFlowItem
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ManualCashRecord
import com.zatiaras.pos.feature.reports.domain.model.ProductSaleItem
import com.zatiaras.pos.feature.reports.domain.model.RawProfitLossData
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.model.TransactionSummaryItem
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local report aggregation backed by transaction and cash-record DAOs.
 *
 * Keep heavy aggregation in SQL/DAO calls where possible, then map results into
 * report-domain models for the presentation layer.
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val cashRecordDao: CashRecordDao,
) : ReportRepository {

    override suspend fun getDashboardStats(): Result<DashboardStats> {
        // Dashboard stats compare fixed calendar ranges in the device timezone.
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()

        val (weekStart, weekEnd) = DateUtils.getThisWeekRange()

        val (monthStart, _) = DateUtils.getThisMonthRange()

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

                // Growth uses previous week as baseline; no previous revenue but
                // current revenue counts as full growth.
                val growth = if (prevWeekSummary.totalRevenue > 0) {
                    (
                        (weeklySummary.totalRevenue - prevWeekSummary.totalRevenue).toDouble() /
                            prevWeekSummary.totalRevenue
                        ) * 100
                } else {
                    if (weeklySummary.totalRevenue > 0) 100.0 else 0.0
                }

                val stats = DashboardStats(
                    todayRevenue = todayRevenue,
                    todayTransactions = todayTransactions,
                    todayItemsSold = todayItems,
                    weeklyRevenue = weeklySummary.totalRevenue,
                    monthlyRevenue = monthlySummary.totalRevenue,
                    revenueGrowthPercent = growth,
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
            // Pass timezone offset into SQL so day buckets match local business
            // dates instead of UTC boundaries.
            val timeOffset = java.util.TimeZone.getDefault().rawOffset.toLong()

            val dailyRevenueEntities = transactionDao.getDailyRevenue(startDate, endDate, timeOffset)

            val dailyMap = dailyRevenueEntities.associateBy { it.dayTimestamp }

            val result = mutableListOf<DailyRevenue>()
            var currentDate = Instant.ofEpochMilli(startDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // Fill missing days with zero so charts stay continuous.
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
                        transactionCount = dailyEntity?.transactionCount ?: 0,
                    ),
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
        limit: Int,
    ): Result<List<TopProduct>> = try {
        val products = transactionDao.getTopSellingProducts(startDate, endDate, limit)
            .map { entity ->
                TopProduct(
                    productId = entity.productId,
                    productName = entity.productName,
                    quantitySold = entity.totalQuantity,
                    totalRevenue = entity.totalRevenue,
                )
            }
        Result.success(products)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get top selling products")
        Result.failure(e)
    }

    override suspend fun getRawProfitLossData(startDate: Long, endDate: Long): Result<RawProfitLossData> = try {
        // POS revenue and manual cash records are separated here; P&L use cases
        // decide which manual records become income or expense categories.
        val posSummary = transactionDao.getRevenueSummary(startDate, endDate)
        val posTransactions = transactionDao.getTransactionCountForDay(startDate, endDate)

        val productSales = transactionDao.getTopSellingProducts(startDate, endDate, 100)
            .map { entity ->
                ProductSaleItem(
                    productId = entity.productId,
                    productName = entity.productName,
                    quantity = entity.totalQuantity,
                    revenue = entity.totalRevenue,
                )
            }

        val manualRecordsEntity = cashRecordDao.getRecordsListByDateRange(startDate, endDate)
        val manualRecords = manualRecordsEntity.map { entity ->
            ManualCashRecord(
                type = entity.type,
                category = entity.category,
                amount = entity.amount,
                description = entity.description,
                isDeleted = entity.isDeleted,
            )
        }

        val rawData = RawProfitLossData(
            posGrossRevenue = posSummary.grossRevenue,
            posTotalDiscount = posSummary.totalDiscount,
            posTransactions = posTransactions,
            productSales = productSales,
            manualRecords = manualRecords,
        )
        Result.success(rawData)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get raw profit/loss report data")
        Result.failure(e)
    }

    override suspend fun getTransactionsForAnalysis(
        startDate: Long,
        endDate: Long,
    ): Result<List<TransactionSummaryItem>> = try {
        val entities = transactionDao.getTransactionsForReports(startDate, endDate)
        val items = entities.map { entity ->
            TransactionSummaryItem(
                createdAt = entity.createdAt,
                paymentMethod = entity.paymentMethod,
                grandTotal = entity.grandTotal,
            )
        }
        Result.success(items)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get transactions for analysis")
        Result.failure(e)
    }

    override suspend fun getCashRecordsForAnalysis(
        startDate: Long,
        endDate: Long,
    ): Result<List<CashFlowItem>> = try {
        val entities = cashRecordDao.getRecordsListByDateRange(startDate, endDate)
        val items = entities.map { entity ->
            CashFlowItem(
                type = entity.type,
                amount = entity.amount,
                description = entity.description,
                category = entity.category,
            )
        }
        Result.success(items)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get cash records for analysis")
        Result.failure(e)
    }
}
