package com.zatiaras.pos.feature.pos.domain.model

import com.zatiaras.pos.core.domain.model.Product

/**
 * Represents an item in the shopping cart.
 * 
 * Contains a snapshot of product data to ensure price consistency
 * even if product prices change during the transaction.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String? = null
) {
    /**
     * Calculated subtotal for this cart item.
     * Product price × quantity.
     */
    val subtotal: Long get() = product.price * quantity
    
    /**
     * Creates a copy with updated quantity.
     */
    fun withQuantity(newQuantity: Int): CartItem = copy(quantity = newQuantity)
    
    /**
     * Creates a copy with incremented quantity.
     */
    fun incrementQuantity(): CartItem = copy(quantity = quantity + 1)
    
    /**
     * Creates a copy with decremented quantity (minimum 0).
     */
    fun decrementQuantity(): CartItem = copy(quantity = maxOf(0, quantity - 1))
}
