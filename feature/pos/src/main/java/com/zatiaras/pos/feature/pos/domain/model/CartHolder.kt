package com.zatiaras.pos.feature.pos.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for sharing Cart between POS and Checkout screens.
 * 
 * This allows the cart to be passed without complex navigation arguments.
 * The cart is cleared after successful transaction.
 */
@Singleton
class CartHolder @Inject constructor() {
    
    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()
    
    /**
     * Get current cart snapshot.
     */
    fun getCart(): Cart = _cart.value
    
    /**
     * Update the cart.
     */
    fun updateCart(cart: Cart) {
        _cart.value = cart
    }
    
    /**
     * Clear the cart (after successful transaction).
     */
    fun clearCart() {
        _cart.value = Cart()
    }
    
    /**
     * Check if cart has items.
     */
    fun hasItems(): Boolean = _cart.value.isNotEmpty()
}
