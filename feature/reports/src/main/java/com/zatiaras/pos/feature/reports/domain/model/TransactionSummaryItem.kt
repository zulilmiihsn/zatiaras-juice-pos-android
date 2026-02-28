package com.zatiaras.pos.feature.reports.domain.model

/**
 * Domain model representing a transaction summary for AI analysis.
 * Maps relevant fields from TransactionEntity without leaking data-layer types.
 */
data class TransactionSummaryItem(
    val createdAt: Long,
    val paymentMethod: String,
    val grandTotal: Long
)
