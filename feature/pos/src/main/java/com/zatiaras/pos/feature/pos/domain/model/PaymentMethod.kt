package com.zatiaras.pos.feature.pos.domain.model

/**
 * Available payment methods for transactions.
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank")
}
