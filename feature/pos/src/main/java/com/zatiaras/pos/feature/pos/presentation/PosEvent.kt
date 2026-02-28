package com.zatiaras.pos.feature.pos.presentation

import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod

/**
 * Events that can be triggered from POS UI.
 */
sealed interface PosEvent {
    // Catalog events
    data class SearchQueryChanged(val query: String) : PosEvent
    data class CategorySelected(val categoryId: String?) : PosEvent
    
    // Direct add to cart (for products without customizations)
    data class AddToCart(val product: Product) : PosEvent
    
    // Product Options Dialog events
    data class ShowProductOptions(val product: Product) : PosEvent
    data object HideProductOptions : PosEvent
    data class ToggleAddOn(val addOnId: String) : PosEvent
    data class SetSugarLevel(val level: SugarLevel) : PosEvent
    data class SetIceLevel(val level: IceLevel) : PosEvent
    data class SetProductNote(val note: String) : PosEvent
    data class SetProductQuantity(val quantity: Int) : PosEvent
    data object ConfirmAddToCart : PosEvent
    
    // Cart item manipulation (by uniqueKey)
    data class IncrementItem(val uniqueKey: String) : PosEvent
    data class DecrementItem(val uniqueKey: String) : PosEvent
    data class RemoveFromCart(val uniqueKey: String) : PosEvent
    data class UpdateItemQuantity(val uniqueKey: String, val quantity: Int) : PosEvent
    data object ClearCart : PosEvent
    
    // Navigation events
    data object ProceedToCheckout : PosEvent
    data object BackToCatalog : PosEvent
    
    // Error handling
    data object DismissError : PosEvent
    
    // UI Toggles
    data object ToggleViewMode : PosEvent
    data class AddCustomItem(val name: String, val price: Long, val quantity: Int = 1) : PosEvent
}

/**
 * Events for Checkout flow.
 */
sealed interface CheckoutEvent {
    data class SetPaymentMethod(val method: PaymentMethod) : CheckoutEvent
    data class SetAmountPaid(val amount: String) : CheckoutEvent
    data class SetDiscountPercent(val percent: String) : CheckoutEvent
    data class SetCustomerName(val name: String) : CheckoutEvent
    data class SetNotes(val notes: String) : CheckoutEvent
    data object ConfirmPayment : CheckoutEvent
    data object CancelCheckout : CheckoutEvent
    data object DismissError : CheckoutEvent
}
