package com.zatiaras.pos.feature.reports.domain.model

/**
 * Represents a top-selling product.
 */
data class TopProduct(
    val productId: String,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Long
)
