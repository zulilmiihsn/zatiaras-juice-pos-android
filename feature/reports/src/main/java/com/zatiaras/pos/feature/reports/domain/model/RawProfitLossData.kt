package com.zatiaras.pos.feature.reports.domain.model

data class RawProfitLossData(
    val posGrossRevenue: Long,
    val posTotalDiscount: Long,
    val posTransactions: Int,
    val productSales: List<ProductSaleItem>,
    val manualRecords: List<ManualCashRecord>
)

data class ManualCashRecord(
    val type: String,
    val category: String?,
    val amount: Long,
    val description: String,
    val isDeleted: Boolean
)
