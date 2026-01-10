package com.zatiaras.pos.feature.pos.domain.model

import java.util.UUID

/**
 * Type of manual cash record entry.
 */
enum class CashRecordType(val displayName: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

/**
 * Represents a manual cash record entry (Buku Kas).
 * 
 * Used for recording income/expenses that are not from POS transactions.
 * Examples:
 * - Restocking supplies (expense)
 * - Received payment from catering order (income)
 * - Paid electricity bill (expense)
 */
data class CashRecord(
    val id: String = UUID.randomUUID().toString(),
    val type: CashRecordType,
    val amount: Long,                    // Always positive
    val description: String,
    val category: String? = null,        // Optional category (e.g., "Utilities", "Supplies")
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    /**
     * Signed amount based on type.
     * Positive for income, negative for expense.
     */
    val signedAmount: Long
        get() = when (type) {
            CashRecordType.INCOME -> amount
            CashRecordType.EXPENSE -> -amount
        }
}
