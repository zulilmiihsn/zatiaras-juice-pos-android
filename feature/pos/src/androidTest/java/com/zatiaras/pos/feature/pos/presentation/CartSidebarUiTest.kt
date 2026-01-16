package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.CartItem
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for CartSidebar component.
 * 
 * Tests:
 * - Empty cart display
 * - Cart with items display
 * - Item increment/decrement buttons
 * - Checkout button state
 */
@RunWith(AndroidJUnit4::class)
class CartSidebarUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCategory = Category(id = "cat-1", name = "Minuman")
    
    private val testProduct1 = Product(
        id = "prod-1",
        name = "Es Teh Manis",
        price = 5000,
        category = testCategory,
        imageUrl = null,
        description = "Es teh manis segar",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    private val testProduct2 = Product(
        id = "prod-2",
        name = "Kopi Susu Gula Aren",
        price = 15000,
        category = testCategory,
        imageUrl = null,
        description = "Kopi susu dengan gula aren",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Test
    fun emptyCart_showsEmptyMessage() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = Cart(),
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Verify empty cart message is displayed
        composeTestRule.onNodeWithText("Keranjang Kosong", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun cartWithItems_showsItemsAndSubtotal() {
        val cart = Cart(
            items = listOf(
                CartItem(product = testProduct1, quantity = 2),
                CartItem(product = testProduct2, quantity = 1)
            )
        )

        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = cart,
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Verify product names are displayed
        composeTestRule.onNodeWithText("Es Teh Manis", substring = true)
            .assertExists()
        composeTestRule.onNodeWithText("Kopi Susu Gula Aren", substring = true)
            .assertExists()

        // Verify quantities are displayed
        composeTestRule.onNodeWithText("2", substring = true)
            .assertExists()
    }

    @Test
    fun cartWithItems_checkoutButtonIsEnabled() {
        val cart = Cart(
            items = listOf(
                CartItem(product = testProduct1, quantity = 1)
            )
        )

        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = cart,
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Verify checkout button exists
        composeTestRule.onNodeWithText("Checkout", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun emptyCart_checkoutButtonIsDisabled() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = Cart(),
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Verify checkout button is not clickable when cart is empty
        composeTestRule.onNodeWithText("Checkout", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun incrementButton_callsOnIncrement() {
        var incrementCalled = false
        var incrementedProductId = ""
        
        val cart = Cart(
            items = listOf(
                CartItem(product = testProduct1, quantity = 1)
            )
        )

        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = cart,
                    onIncrement = { productId ->
                        incrementCalled = true
                        incrementedProductId = productId
                    },
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Click increment button (usually has "+" or ContentDescription)
        composeTestRule.onNodeWithContentDescription("Tambah", substring = true, ignoreCase = true)
            .performClick()

        assert(incrementCalled) { "onIncrement should be called" }
        assert(incrementedProductId == testProduct1.id) { "Should increment correct product" }
    }

    @Test
    fun decrementButton_callsOnDecrement() {
        var decrementCalled = false
        var decrementedProductId = ""
        
        val cart = Cart(
            items = listOf(
                CartItem(product = testProduct1, quantity = 2)
            )
        )

        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = cart,
                    onIncrement = {},
                    onDecrement = { productId ->
                        decrementCalled = true
                        decrementedProductId = productId
                    },
                    onRemove = {},
                    onCheckout = {},
                    onClearCart = {}
                )
            }
        }

        // Click decrement button
        composeTestRule.onNodeWithContentDescription("Kurang", substring = true, ignoreCase = true)
            .performClick()

        assert(decrementCalled) { "onDecrement should be called" }
        assert(decrementedProductId == testProduct1.id) { "Should decrement correct product" }
    }

    @Test
    fun checkoutButton_callsOnCheckout() {
        var checkoutCalled = false
        
        val cart = Cart(
            items = listOf(
                CartItem(product = testProduct1, quantity = 1)
            )
        )

        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CartSidebar(
                    cart = cart,
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onCheckout = { checkoutCalled = true },
                    onClearCart = {}
                )
            }
        }

        // Click checkout button
        composeTestRule.onNodeWithText("Checkout", substring = true, ignoreCase = true)
            .performClick()

        assert(checkoutCalled) { "onCheckout should be called" }
    }
}
