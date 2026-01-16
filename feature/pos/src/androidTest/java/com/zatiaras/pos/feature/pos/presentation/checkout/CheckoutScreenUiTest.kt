package com.zatiaras.pos.feature.pos.presentation.checkout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.CartItem
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for Checkout screen components.
 * 
 * Tests:
 * - Payment method selection
 * - Amount input
 * - Quick amount buttons
 * - Change calculation display
 * - Confirm button state
 */
@RunWith(AndroidJUnit4::class)
class CheckoutScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCategory = Category(id = "cat-1", name = "Minuman")
    
    private val testProduct = Product(
        id = "prod-1",
        name = "Es Teh Manis",
        price = 5000,
        category = testCategory,
        imageUrl = null,
        description = "Es teh manis segar",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    private val testCart = Cart(
        items = listOf(
            CartItem(product = testProduct, quantity = 2)
        )
    )

    private val testState = CheckoutUiState.Ready(
        cart = testCart,
        subtotal = 10000,
        discountPercent = 0.0,
        discountAmount = 0,
        taxPercent = 11.0,
        taxAmount = 1100,
        grandTotal = 11100,
        selectedPaymentMethod = PaymentMethod.CASH,
        amountPaid = "",
        changeAmount = 0
    )

    @Test
    fun checkoutScreen_displaysGrandTotal() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify grand total is displayed (formatted as Rp11.100 or similar)
        composeTestRule.onNodeWithText("11.100", substring = true)
            .assertExists()
    }

    @Test
    fun checkoutScreen_displaysPaymentMethods() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify payment method options exist
        composeTestRule.onNodeWithText("Cash", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("QRIS", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Transfer", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun checkoutScreen_cashSelected_showsAmountInput() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify amount input field exists for cash payment
        composeTestRule.onNodeWithText("Jumlah Bayar", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun checkoutScreen_showsQuickAmountButtons() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify quick amount buttons exist
        composeTestRule.onNodeWithText("20.000", substring = true)
            .assertExists()
        composeTestRule.onNodeWithText("50.000", substring = true)
            .assertExists()
    }

    @Test
    fun checkoutScreen_clickQuickAmount_callsCallback() {
        var quickAmountClicked = 0L
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = { amount -> quickAmountClicked = amount },
                    onSetExactAmount = {}
                )
            }
        }

        // Click a quick amount button
        composeTestRule.onNodeWithText("20.000", substring = true)
            .performClick()

        assert(quickAmountClicked == 20000L) { "Quick amount should be 20000" }
    }

    @Test
    fun checkoutScreen_insufficientPayment_confirmButtonDisabled() {
        val insufficientState = testState.copy(amountPaid = "5000")
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = insufficientState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify confirm button is disabled
        composeTestRule.onNodeWithText("Konfirmasi", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun checkoutScreen_sufficientPayment_confirmButtonEnabled() {
        val sufficientState = testState.copy(
            amountPaid = "15000",
            changeAmount = 3900
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = sufficientState,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify confirm button is enabled
        composeTestRule.onNodeWithText("Konfirmasi", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun checkoutScreen_showsChangeAmount() {
        val stateWithChange = testState.copy(
            amountPaid = "15000",
            changeAmount = 3900
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = stateWithChange,
                    onEvent = {},
                    onNavigateBack = {},
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Verify change amount is displayed
        composeTestRule.onNodeWithText("Kembalian", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("3.900", substring = true)
            .assertExists()
    }

    @Test
    fun checkoutScreen_backButton_callsNavigateBack() {
        var backCalled = false
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                CheckoutScreenContent(
                    state = testState,
                    onEvent = {},
                    onNavigateBack = { backCalled = true },
                    onSetQuickAmount = {},
                    onSetExactAmount = {}
                )
            }
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back", substring = true, ignoreCase = true)
            .performClick()

        assert(backCalled) { "onNavigateBack should be called" }
    }
}
