package com.zatiaras.pos.feature.pos.domain.model

import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel

/**
 * Represents the shopping cart.
 * 
 * This is an immutable data structure. All modifications return a new Cart instance.
 * The cart is stored in-memory only (not persisted to database).
 * This is intentional POS behavior - carts are session-based.
 * 
 * Items are uniquely identified by their uniqueKey (product + customizations).
 * Same product with different customizations = different cart items.
 */
data class Cart(
    val items: List<CartItem> = emptyList()
) {
    /**
     * Total number of items in cart (sum of all quantities).
     */
    val itemCount: Int get() = items.sumOf { it.quantity }
    
    /**
     * Number of unique line items in cart.
     */
    val uniqueItemCount: Int get() = items.size
    
    /**
     * Cart subtotal before tax and discounts.
     * Includes product prices + add-on prices.
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
     * Adds a product to cart with customizations.
     * 
     * If an item with the same uniqueKey exists, increments quantity.
     * Otherwise, adds a new item to cart.
     * 
     * @param product The product to add
     * @param quantity Number to add (default 1)
     * @param addOns Selected add-ons
     * @param sugarLevel Sugar level customization
     * @param iceLevel Ice level customization
     * @param notes Special requests/notes
     * @return New Cart with updated items
     */
    fun addItem(
        product: Product,
        quantity: Int = 1,
        addOns: List<AddOn> = emptyList(),
        sugarLevel: SugarLevel = SugarLevel.NORMAL,
        iceLevel: IceLevel = IceLevel.NORMAL,
        notes: String = ""
    ): Cart {
        val newItem = CartItem(
            product = product,
            quantity = quantity,
            addOns = addOns,
            sugarLevel = sugarLevel,
            iceLevel = iceLevel,
            notes = notes
        )
        
        val existingIndex = items.indexOfFirst { it.uniqueKey == newItem.uniqueKey }
        
        return if (existingIndex >= 0) {
            // Item with same customizations exists, merge quantities
            val existingItem = items[existingIndex]
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + quantity)
            copy(items = items.toMutableList().apply { 
                set(existingIndex, updatedItem) 
            })
        } else {
            // New item, add to cart
            copy(items = items + newItem)
        }
    }
    
    /**
     * Adds a pre-configured CartItem to cart.
     * Merges with existing item if uniqueKey matches.
     */
    fun addItem(cartItem: CartItem): Cart {
        val existingIndex = items.indexOfFirst { it.uniqueKey == cartItem.uniqueKey }
        
        return if (existingIndex >= 0) {
            val existingItem = items[existingIndex]
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + cartItem.quantity)
            copy(items = items.toMutableList().apply { 
                set(existingIndex, updatedItem) 
            })
        } else {
            copy(items = items + cartItem)
        }
    }
    
    /**
     * Updates quantity of a specific cart item by its uniqueKey.
     * Removes item if quantity is 0 or less.
     * 
     * @param uniqueKey The unique identifier for the cart item
     * @param quantity New quantity
     * @return New Cart with updated items
     */
    fun updateQuantity(uniqueKey: String, quantity: Int): Cart {
        if (quantity <= 0) {
            return removeItem(uniqueKey)
        }
        
        return copy(items = items.map { item ->
            if (item.uniqueKey == uniqueKey) {
                item.copy(quantity = quantity)
            } else {
                item
            }
        })
    }
    
    /**
     * Increments quantity of a specific cart item by 1.
     * 
     * @param uniqueKey The unique identifier for the cart item
     * @return New Cart with updated items
     */
    fun incrementItem(uniqueKey: String): Cart {
        return copy(items = items.map { item ->
            if (item.uniqueKey == uniqueKey) {
                item.incrementQuantity()
            } else {
                item
            }
        })
    }
    
    /**
     * Decrements quantity of a specific cart item by 1.
     * Removes item if quantity reaches 0.
     * 
     * @param uniqueKey The unique identifier for the cart item
     * @return New Cart with updated items
     */
    fun decrementItem(uniqueKey: String): Cart {
        val updatedItems = items.mapNotNull { item ->
            if (item.uniqueKey == uniqueKey) {
                val decremented = item.decrementQuantity()
                if (decremented.quantity > 0) decremented else null
            } else {
                item
            }
        }
        return copy(items = updatedItems)
    }
    
    /**
     * Removes a cart item entirely.
     * 
     * @param uniqueKey The unique identifier for the cart item
     * @return New Cart without the specified item
     */
    fun removeItem(uniqueKey: String): Cart {
        return copy(items = items.filter { it.uniqueKey != uniqueKey })
    }
    
    /**
     * Returns a new empty cart.
     */
    fun clear(): Cart = Cart()
    
    /**
     * Gets the CartItem by its uniqueKey, if it exists.
     */
    fun getItem(uniqueKey: String): CartItem? {
        return items.find { it.uniqueKey == uniqueKey }
    }
    
    /**
     * Gets all cart items for a specific product (regardless of customizations).
     */
    fun getItemsForProduct(productId: String): List<CartItem> {
        return items.filter { it.product.id == productId }
    }
    
    /**
     * Returns the total quantity of a specific product in cart
     * (across all customization variants).
     */
    fun getTotalQuantityForProduct(productId: String): Int {
        return getItemsForProduct(productId).sumOf { it.quantity }
    }

    /**
     * Alias for getTotalQuantityForProduct for compatibility.
     */
    fun getQuantity(productId: String): Int = getTotalQuantityForProduct(productId)
}
