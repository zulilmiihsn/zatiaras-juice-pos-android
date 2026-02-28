package com.zatiaras.pos.feature.pos.domain.model

import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Helper: generate uniqueKey for a product with default customizations */
private fun keyFor(product: Product): String = CartItem(product, 1).uniqueKey

/**
 * Unit tests for Cart domain model.
 * 
 * Tests:
 * - Empty cart behavior
 * - Adding items
 * - Incrementing/decrementing quantities
 * - Removing items
 * - Subtotal calculations
 * - Edge cases
 */
class CartTest {

    private val testCategory = Category(id = "cat-1", name = "Minuman")
    
    private lateinit var product1: Product
    private lateinit var product2: Product
    private lateinit var product3: Product

    @Before
    fun setup() {
        product1 = Product(
            id = "prod-1",
            name = "Es Teh",
            price = 5000,
            category = testCategory,
            imageUrl = null,
            description = "Es teh manis",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        product2 = Product(
            id = "prod-2",
            name = "Kopi Susu",
            price = 15000,
            category = testCategory,
            imageUrl = null,
            description = "Kopi susu gula aren",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        product3 = Product(
            id = "prod-3",
            name = "Es Jeruk",
            price = 8000,
            category = testCategory,
            imageUrl = null,
            description = "Es jeruk segar",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // ==================== Empty Cart Tests ====================

    @Test
    fun `new cart is empty`() {
        val cart = Cart()
        
        assertTrue(cart.isEmpty())
        assertEquals(0, cart.itemCount)
        assertEquals(0, cart.uniqueItemCount)
        assertEquals(0L, cart.subtotal)
    }

    @Test
    fun `isEmpty returns true for empty cart`() {
        val cart = Cart()
        assertTrue(cart.isEmpty())
        assertFalse(cart.isNotEmpty())
    }

    // ==================== Add Item Tests ====================

    @Test
    fun `addItem adds new product to cart`() {
        val cart = Cart().addItem(product1)
        
        assertFalse(cart.isEmpty())
        assertEquals(1, cart.itemCount)
        assertEquals(1, cart.uniqueItemCount)
        assertEquals(product1.id, cart.items[0].product.id)
    }

    @Test
    fun `addItem increments quantity for existing product`() {
        val cart = Cart()
            .addItem(product1)
            .addItem(product1)
        
        assertEquals(2, cart.itemCount)
        assertEquals(1, cart.uniqueItemCount)
        assertEquals(2, cart.getQuantity(product1.id))
    }

    @Test
    fun `addItem with quantity adds multiple items at once`() {
        val cart = Cart().addItem(product1, quantity = 5)
        
        assertEquals(5, cart.itemCount)
        assertEquals(1, cart.uniqueItemCount)
        assertEquals(5, cart.getQuantity(product1.id))
    }

    @Test
    fun `addItem preserves existing items when adding new product`() {
        val cart = Cart()
            .addItem(product1)
            .addItem(product2)
        
        assertEquals(2, cart.itemCount)
        assertEquals(2, cart.uniqueItemCount)
        assertEquals(1, cart.getQuantity(product1.id))
        assertEquals(1, cart.getQuantity(product2.id))
    }

    // ==================== Subtotal Tests ====================

    @Test
    fun `subtotal calculates correctly for single item`() {
        val cart = Cart().addItem(product1)
        assertEquals(5000L, cart.subtotal)
    }

    @Test
    fun `subtotal calculates correctly for multiple quantities`() {
        val cart = Cart().addItem(product1, quantity = 3)
        assertEquals(15000L, cart.subtotal) // 5000 * 3
    }

    @Test
    fun `subtotal calculates correctly for multiple products`() {
        val cart = Cart()
            .addItem(product1, quantity = 2)  // 5000 * 2 = 10000
            .addItem(product2, quantity = 1)  // 15000 * 1 = 15000
            .addItem(product3, quantity = 3)  // 8000 * 3 = 24000
        
        assertEquals(49000L, cart.subtotal) // 10000 + 15000 + 24000
    }

    // ==================== Increment/Decrement Tests ====================

    @Test
    fun `incrementItem increases quantity by 1`() {
        val cart = Cart()
            .addItem(product1)
            .incrementItem(keyFor(product1))
        
        assertEquals(2, cart.getQuantity(product1.id))
    }

    @Test
    fun `decrementItem decreases quantity by 1`() {
        val cart = Cart()
            .addItem(product1, quantity = 3)
            .decrementItem(keyFor(product1))
        
        assertEquals(2, cart.getQuantity(product1.id))
    }

    @Test
    fun `decrementItem removes item when quantity reaches 0`() {
        val cart = Cart()
            .addItem(product1)
            .decrementItem(keyFor(product1))
        
        assertTrue(cart.isEmpty())
        assertEquals(0, cart.getQuantity(product1.id))
    }

    @Test
    fun `decrementItem does not affect other items`() {
        val cart = Cart()
            .addItem(product1, quantity = 2)
            .addItem(product2)
            .decrementItem(keyFor(product1))
        
        assertEquals(1, cart.getQuantity(product1.id))
        assertEquals(1, cart.getQuantity(product2.id))
    }

    // ==================== Update Quantity Tests ====================

    @Test
    fun `updateQuantity sets exact quantity`() {
        val cart = Cart()
            .addItem(product1)
            .updateQuantity(keyFor(product1), 5)
        
        assertEquals(5, cart.getQuantity(product1.id))
    }

    @Test
    fun `updateQuantity with 0 removes item`() {
        val cart = Cart()
            .addItem(product1)
            .updateQuantity(keyFor(product1), 0)
        
        assertTrue(cart.isEmpty())
    }

    @Test
    fun `updateQuantity with negative value removes item`() {
        val cart = Cart()
            .addItem(product1)
            .updateQuantity(keyFor(product1), -1)
        
        assertTrue(cart.isEmpty())
    }

    // ==================== Remove Item Tests ====================

    @Test
    fun `removeItem removes product entirely`() {
        val cart = Cart()
            .addItem(product1, quantity = 5)
            .removeItem(keyFor(product1))
        
        assertTrue(cart.isEmpty())
        assertNull(cart.getItem(keyFor(product1)))
    }

    @Test
    fun `removeItem does not affect other items`() {
        val cart = Cart()
            .addItem(product1)
            .addItem(product2)
            .removeItem(keyFor(product1))
        
        assertEquals(1, cart.uniqueItemCount)
        assertEquals(1, cart.getQuantity(product2.id))
        assertNull(cart.getItem(keyFor(product1)))
    }

    @Test
    fun `removeItem with non-existent id does nothing`() {
        val cart = Cart()
            .addItem(product1)
            .removeItem("non-existent-id")
        
        assertEquals(1, cart.itemCount)
    }

    // ==================== Clear Tests ====================

    @Test
    fun `clear returns empty cart`() {
        val cart = Cart()
            .addItem(product1)
            .addItem(product2)
            .clear()
        
        assertTrue(cart.isEmpty())
        assertEquals(0, cart.itemCount)
    }

    // ==================== Get Item Tests ====================

    @Test
    fun `getItem returns correct item`() {
        val cart = Cart().addItem(product1)
        val item = cart.getItem(keyFor(product1))
        
        assertNotNull(item)
        assertEquals(product1.id, item?.product?.id)
    }

    @Test
    fun `getItem returns null for non-existent product`() {
        val cart = Cart()
        val item = cart.getItem("non-existent-id")
        
        assertNull(item)
    }

    @Test
    fun `getQuantity returns 0 for non-existent product`() {
        val cart = Cart()
        assertEquals(0, cart.getQuantity("non-existent-id"))
    }

    // ==================== Immutability Tests ====================

    @Test
    fun `addItem returns new cart instance`() {
        val cart1 = Cart()
        val cart2 = cart1.addItem(product1)
        
        assertNotSame(cart1, cart2)
        assertTrue(cart1.isEmpty())
        assertFalse(cart2.isEmpty())
    }

    @Test
    fun `removeItem returns new cart instance`() {
        val cart1 = Cart().addItem(product1)
        val cart2 = cart1.removeItem(keyFor(product1))
        
        assertNotSame(cart1, cart2)
        assertFalse(cart1.isEmpty())
        assertTrue(cart2.isEmpty())
    }
}
