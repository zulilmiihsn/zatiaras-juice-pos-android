package com.zatiaras.pos.feature.pos.domain.model

import com.zatiaras.pos.core.domain.model.Product

/**
 * Represents the shopping cart.
 * 
 * This is an immutable data structure. All modifications return a new Cart instance.
 * The cart is stored in-memory only (not persisted to database).
 * This is intentional POS behavior - carts are session-based.
 */
data class Cart(
    val items: List<CartItem> = emptyList()
) {
    /**
     * Total number of items in cart (sum of all quantities).
     */
    val itemCount: Int get() = items.sumOf { it.quantity }
    
    /**
     * Number of unique products in cart.
     */
    val uniqueItemCount: Int get() = items.size
    
    /**
     * Cart subtotal before tax and discounts.
     */
    val subtotal: Long get() = items.sumOf { it.subtotal }
    
    /**
     * Returns true if cart has no items.
     */
    fun isEmpty(): Boolean = items.isEmpty()
    
    /**
     * Returns true if cart has items.
     */
    fun isNotEmpty(): Boolean = items.isNotEmpty()
    
    /**
     * Adds a product to cart or increments quantity if already exists.
     * 
     * @param product The product to add
     * @param quantity Number to add (default 1)
     * @return New Cart with updated items
     */
    fun addItem(product: Product, quantity: Int = 1): Cart {
        val existingIndex = items.indexOfFirst { it.product.id == product.id }
        
        return if (existingIndex >= 0) {
            // Product exists, update quantity
            val existingItem = items[existingIndex]
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + quantity)
            copy(items = items.toMutableList().apply { 
                set(existingIndex, updatedItem) 
            })
        } else {
            // New product, add to cart
            copy(items = items + CartItem(product, quantity))
        }
    }
    
    /**
     * Updates quantity of a specific product.
     * Removes item if quantity is 0 or less.
     * 
     * @param productId Product ID to update
     * @param quantity New quantity
     * @return New Cart with updated items
     */
    fun updateQuantity(productId: String, quantity: Int): Cart {
        if (quantity <= 0) {
            return removeItem(productId)
        }
        
        return copy(items = items.map { item ->
            if (item.product.id == productId) {
                item.copy(quantity = quantity)
            } else {
                item
            }
        })
    }
    
    /**
     * Increments quantity of a specific product by 1.
     * 
     * @param productId Product ID to increment
     * @return New Cart with updated items
     */
    fun incrementItem(productId: String): Cart {
        return copy(items = items.map { item ->
            if (item.product.id == productId) {
                item.incrementQuantity()
            } else {
                item
            }
        })
    }
    
    /**
     * Decrements quantity of a specific product by 1.
     * Removes item if quantity reaches 0.
     * 
     * @param productId Product ID to decrement
     * @return New Cart with updated items
     */
    fun decrementItem(productId: String): Cart {
        val updatedItems = items.mapNotNull { item ->
            if (item.product.id == productId) {
                val decremented = item.decrementQuantity()
                if (decremented.quantity > 0) decremented else null
            } else {
                item
            }
        }
        return copy(items = updatedItems)
    }
    
    /**
     * Removes a product from cart entirely.
     * 
     * @param productId Product ID to remove
     * @return New Cart without the specified item
     */
    fun removeItem(productId: String): Cart {
        return copy(items = items.filter { it.product.id != productId })
    }
    
    /**
     * Returns a new empty cart.
     */
    fun clear(): Cart = Cart()
    
    /**
     * Gets the CartItem for a specific product, if it exists.
     */
    fun getItem(productId: String): CartItem? {
        return items.find { it.product.id == productId }
    }
    
    /**
     * Returns the quantity of a specific product in cart.
     * Returns 0 if product is not in cart.
     */
    fun getQuantity(productId: String): Int {
        return getItem(productId)?.quantity ?: 0
    }
}
