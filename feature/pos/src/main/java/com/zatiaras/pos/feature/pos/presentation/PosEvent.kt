package com.zatiaras.pos.feature.pos.presentation

import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod

/**
 * Events that can be triggered from POS UI.
 */
sealed interface PosEvent {
    // Catalog events
    data class SearchQueryChanged(val query: String) : PosEvent
    data class CategorySelected(val categoryId: String?) : PosEvent
    
    // Cart events
    data class AddToCart(val product: Product) : PosEvent
    data class IncrementItem(val productId: String) : PosEvent
    data class DecrementItem(val productId: String) : PosEvent
    data class RemoveFromCart(val productId: String) : PosEvent
    data class UpdateItemQuantity(val productId: String, val quantity: Int) : PosEvent
    data object ClearCart : PosEvent
    
    // Navigation events
    data object ProceedToCheckout : PosEvent
    data object BackToCatalog : PosEvent
    
    // Error handling
    data object DismissError : PosEvent
}

/**
 * Events for Checkout flow.
 */
sealed interface CheckoutEvent {
    data class SetPaymentMethod(val method: PaymentMethod) : CheckoutEvent
    data class SetAmountPaid(val amount: String) : CheckoutEvent
    data class SetDiscountPercent(val percent: String) : CheckoutEvent
    data class SetNotes(val notes: String) : CheckoutEvent
    data object ConfirmPayment : CheckoutEvent
    data object CancelCheckout : CheckoutEvent
}
