package com.zatiaras.pos.feature.pos.domain.model

import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel

/**
 * Represents an item in the shopping cart.
 * 
 * Contains a snapshot of product data to ensure price consistency
 * even if product prices change during the transaction.
 * 
 * Supports:
 * - Add-ons (ekstra/topping) with their prices
 * - Sugar level customization (for beverages)
 * - Ice level customization (for beverages)
 * - Notes for special requests
 * 
 * The unique key for cart item is: productId + addOnIds + sugarLevel + iceLevel + notes
 * This allows same product with different customizations to be separate items.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
    val addOns: List<AddOn> = emptyList(),
    val sugarLevel: SugarLevel = SugarLevel.NORMAL,
    val iceLevel: IceLevel = IceLevel.NORMAL,
    val notes: String = ""
) {
    /**
     * Unique key for this cart item based on product + customizations.
     * Used to identify if items should be merged or kept separate.
     * Calculated lazily to avoid overhead.
     */
    val uniqueKey: String by lazy {
        buildString {
            append("p:").append(product.id)
            append("|a:").append(addOns.map { it.id }.sorted().joinToString(","))
            append("|s:").append(sugarLevel.id)
            append("|i:").append(iceLevel.id)
            append("|n:").append(notes.trim())
        }
    }
    
    /**
     * Total price of all selected add-ons
     */
    val addOnTotal: Long
        get() = addOns.sumOf { it.price }
    
    /**
     * Unit price including product price + add-on prices
     */
    val unitPrice: Long
        get() = product.price + addOnTotal
    
    /**
     * Calculated subtotal for this cart item.
     * (Product price + AddOn prices) × quantity.
     */
    val subtotal: Long
        get() = unitPrice * quantity
    
    /**
     * Returns true if this item has any customizations (non-default sugar/ice or add-ons)
     */
    val hasCustomizations: Boolean
        get() = addOns.isNotEmpty() || 
                sugarLevel != SugarLevel.NORMAL || 
                iceLevel != IceLevel.NORMAL ||
                notes.isNotBlank()
    
    /**
     * Returns a summary string of customizations for display
     * e.g., "Extra Cheese, Sedikit Gula, Tanpa Es"
     */
    val customizationSummary: String
        get() {
            val parts = mutableListOf<String>()
            
            // Add add-on names
            if (addOns.isNotEmpty()) {
                parts.add(addOns.joinToString(", ") { it.name })
            }
            
            // Add sugar level if not normal (only for beverages)
            if (product.supportsSugarIce && sugarLevel != SugarLevel.NORMAL) {
                parts.add(sugarLevel.label)
            }
            
            // Add ice level if not normal (only for beverages)
            if (product.supportsSugarIce && iceLevel != IceLevel.NORMAL) {
                parts.add(iceLevel.label)
            }
            
            // Add notes if present
            if (notes.isNotBlank()) {
                parts.add(notes)
            }
            
            return parts.joinToString(", ")
        }
    
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
