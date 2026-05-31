package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query

interface TransactionReportDao {
    @Query(
        """
        SELECT 
            ((createdAt + :timeOffset) / 86400000) * 86400000 - :timeOffset as dayTimestamp,
            COALESCE(SUM(grandTotal), 0) as revenue,
            COUNT(*) as transactionCount
        FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
        GROUP BY dayTimestamp
        ORDER BY dayTimestamp ASC
    """,
    )
    suspend fun getDailyRevenue(
        startDate: Long,
        endDate: Long,
        timeOffset: Long = 0L,
    ): List<DailyRevenueEntity>

    @Query(
        """
        SELECT 
            ti.productId,
            ti.productName,
            SUM(ti.quantity) as totalQuantity,
            SUM(ti.subtotal) as totalRevenue
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.createdAt >= :startDate 
        AND t.createdAt < :endDate 
        AND t.isDeleted = 0
        GROUP BY ti.productId, ti.productName
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int = 10): List<TopProductEntity>

    @Query(
        """
        SELECT 
            COALESCE(SUM(grandTotal), 0) as totalRevenue,
            COALESCE(SUM(subtotal), 0) as grossRevenue,
            COALESCE(SUM(discountAmount), 0) as totalDiscount,
            COALESCE(SUM(taxAmount), 0) as totalTax
        FROM transactions 
        WHERE createdAt >= :startDate 
        AND createdAt < :endDate 
        AND isDeleted = 0
    """,
    )
    suspend fun getRevenueSummary(startDate: Long, endDate: Long): RevenueSummaryEntity
}
