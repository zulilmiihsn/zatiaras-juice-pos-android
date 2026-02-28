package com.zatiaras.pos.feature.pos.presentation.history

import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod

data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val isOwner: Boolean = false,
    val allTransactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val paymentFilter: PaymentFilter = PaymentFilter.ALL,
    val showDeleteConfirmDialog: Boolean = false,
    val selectedTransaction: Transaction? = null,
    val showDetailDialog: Boolean = false,
    val error: String? = null
) {
    val displayedTransactions: List<Transaction>
        get() {
            var result = allTransactions
            
            // Search filter
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter { 
                    it.notes?.lowercase()?.contains(query) == true ||
                    it.transactionNumber.lowercase().contains(query) 
                }
            }
            
            // Payment filter
            if (paymentFilter != PaymentFilter.ALL) {
                result = result.filter {
                    when (paymentFilter) {
                        PaymentFilter.QRIS -> it.paymentMethod == PaymentMethod.QRIS
                        PaymentFilter.TUNAI -> it.paymentMethod == PaymentMethod.CASH
                        else -> true
                    }
                }
            }
            
            return result
        }
}

enum class PaymentFilter(val label: String) {
    ALL("Semua"),
    QRIS("QRIS"),
    TUNAI("Tunai")
}
