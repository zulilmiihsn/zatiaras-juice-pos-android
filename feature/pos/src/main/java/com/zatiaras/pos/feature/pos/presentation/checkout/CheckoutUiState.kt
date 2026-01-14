package com.zatiaras.pos.feature.pos.presentation.checkout

import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction

/**
 * UI State for Checkout screen.
 */
sealed interface CheckoutUiState {
    
    data object Loading : CheckoutUiState
    
    /**
     * Ready state with all checkout information.
     */
    data class Ready(
        val cart: Cart,
        val subtotal: Long,
        val discountPercent: Double = 0.0,
        val discountAmount: Long = 0,
        val taxPercent: Double = 11.0,          // Default PPN Indonesia
        val taxAmount: Long = 0,
        val grandTotal: Long,
        val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
        val amountPaid: String = "",
        val changeAmount: Long = 0,
        val customerName: String = "",
        val notes: String = "",
        val isProcessing: Boolean = false,
        val paymentError: String? = null
    ) : CheckoutUiState {
        
        /**
         * Check if payment can be completed.
         * For CASH: amountPaid must be >= grandTotal
         * For QRIS/TRANSFER: always valid (exact amount assumed)
         */
        val canComplete: Boolean
            get() = when (selectedPaymentMethod) {
                PaymentMethod.CASH -> {
                    val paid = amountPaid.toLongOrNull() ?: 0
                    paid >= grandTotal
                }
                PaymentMethod.QRIS, PaymentMethod.TRANSFER -> true
            }
        
        /**
         * Formatted amount paid as Long.
         */
        val amountPaidValue: Long
            get() = amountPaid.toLongOrNull() ?: 0
    }
    
    /**
     * Transaction completed successfully.
     */
    data class Success(
        val transaction: Transaction
    ) : CheckoutUiState
    
    /**
     * Error state.
     */
    data class Error(val message: String) : CheckoutUiState
}
