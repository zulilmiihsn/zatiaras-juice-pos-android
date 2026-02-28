package com.zatiaras.pos.feature.reports.domain.model

/**
 * Domain model representing a cash flow record for AI analysis.
 * Maps relevant fields from CashRecordEntity without leaking data-layer types.
 */
data class CashFlowItem(
    val type: String,          // "INCOME" or "EXPENSE"
    val amount: Long,
    val description: String,
    val category: String? = null
)
