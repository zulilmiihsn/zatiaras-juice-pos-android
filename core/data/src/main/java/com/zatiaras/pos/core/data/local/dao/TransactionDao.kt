package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao

/**
 * Room entrypoint for transaction persistence.
 *
 * Transactions touch checkout, history, sync, and reports, so the inherited
 * contracts keep those responsibilities discoverable without changing callers.
 */
@Dao
interface TransactionDao :
    TransactionWriteDao,
    TransactionReadDao,
    TransactionSyncDao,
    TransactionReportDao

data class DailyRevenueEntity(
    val dayTimestamp: Long,
    val revenue: Long,
    val transactionCount: Int,
)

data class TopProductEntity(
    val productId: String,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenue: Long,
)

data class RevenueSummaryEntity(
    val totalRevenue: Long,
    val grossRevenue: Long,
    val totalDiscount: Long,
    val totalTax: Long,
)
