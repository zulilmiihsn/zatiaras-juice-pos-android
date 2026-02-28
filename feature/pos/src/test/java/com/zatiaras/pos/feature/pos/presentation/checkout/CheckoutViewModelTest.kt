package com.zatiaras.pos.feature.pos.presentation.checkout

import app.cash.turbine.test
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.pos.MainDispatcherRule
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.presentation.CheckoutEvent
import com.zatiaras.pos.feature.pos.domain.usecase.CalculateCheckoutTotalsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.lifecycle.SavedStateHandle

/**
 * Unit tests for CheckoutViewModel.
 * 
 * Tests:
 * - Initial state is loading
 * - Initialization with cart
 * - Payment method selection
 * - Amount paid input
 * - Discount calculations
 * - Tax calculations
 * - Payment confirmation
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var calculateCheckoutTotalsUseCase: CalculateCheckoutTotalsUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: CheckoutViewModel

    private val testCategory = Category(id = "cat-1", name = "Minuman")
    
    private val testProduct1 = Product(
        id = "prod-1",
        name = "Es Teh",
        price = 5000,
        category = testCategory,
        imageUrl = null,
        description = "Es teh manis",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    private val testProduct2 = Product(
        id = "prod-2",
        name = "Kopi Susu",
        price = 15000,
        category = testCategory,
        imageUrl = null,
        description = "Kopi susu gula aren",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        transactionRepository = mockk()
        calculateCheckoutTotalsUseCase = CalculateCheckoutTotalsUseCase()
        savedStateHandle = SavedStateHandle()
        viewModel = CheckoutViewModel(transactionRepository, calculateCheckoutTotalsUseCase, savedStateHandle)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is CheckoutUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initializeWithCart sets Ready state with cart data`() = runTest {
        val cart = Cart()
            .addItem(testProduct1, quantity = 2)  // 5000 * 2 = 10000
            .addItem(testProduct2, quantity = 1)  // 15000 * 1 = 15000
        // Subtotal = 25000
        
        viewModel.uiState.test {
            awaitItem() // Loading
            
            viewModel.initializeWithCart(cart)
            
            val state = awaitItem()
            assertTrue(state is CheckoutUiState.Ready)
            
            val readyState = state as CheckoutUiState.Ready
            assertEquals(25000L, readyState.subtotal)
            assertEquals(2, readyState.cart.uniqueItemCount)
            assertEquals(PaymentMethod.CASH, readyState.selectedPaymentMethod)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Payment Method Tests ====================

    @Test
    fun `SetPaymentMethod changes payment method`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem() // Loading
            viewModel.initializeWithCart(cart)
            awaitItem() // Ready with CASH
            
            viewModel.onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.QRIS))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals(PaymentMethod.QRIS, state.selectedPaymentMethod)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching to non-cash payment clears amountPaid`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem() // Loading
            viewModel.initializeWithCart(cart)
            awaitItem() // Ready
            
            viewModel.onEvent(CheckoutEvent.SetAmountPaid("10000"))
            awaitItem() // intermediate
            awaitItem() // recalculated
            
            viewModel.onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.QRIS))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals("", state.amountPaid)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Amount Paid Tests ====================

    @Test
    fun `SetAmountPaid updates amount and calculates change`() = runTest {
        val cart = Cart().addItem(testProduct1) // 5000
        // With 11% tax: grandTotal = 5000 + 550 = 5550
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetAmountPaid("10000"))
            
            awaitItem() // intermediate state
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals("10000", state.amountPaid)
            assertEquals(10000L - state.grandTotal, state.changeAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SetAmountPaid filters non-digit characters`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetAmountPaid("10.000 IDR"))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals("10000", state.amountPaid)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuickAmount sets exact amount`() = runTest {
        val cart = Cart().addItem(testProduct1) // 5000
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.setQuickAmount(20000)
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals("20000", state.amountPaid)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setExactAmount sets grandTotal as amountPaid`() = runTest {
        val cart = Cart().addItem(testProduct1) // 5000
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            val initialState = awaitItem() as CheckoutUiState.Ready
            
            viewModel.setExactAmount()
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals(initialState.grandTotal.toString(), state.amountPaid)
            assertEquals(0L, state.changeAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Discount Tests ====================

    @Test
    fun `SetDiscountPercent updates discount and recalculates total`() = runTest {
        val cart = Cart().addItem(testProduct1) // 5000
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetDiscountPercent("10"))
            
            awaitItem() // intermediate state
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals(10.0, state.discountPercent, 0.01)
            assertEquals(500L, state.discountAmount) // 10% of 5000
            // After discount: 4500, Tax 11%: 495, Grand: 4995
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `discount is capped at 100 percent`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetDiscountPercent("150"))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals(100.0, state.discountPercent, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Tax Calculation Tests ====================

    @Test
    fun `tax is calculated as 11 percent by default`() = runTest {
        val cart = Cart().addItem(testProduct1) // 5000
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertEquals(11.0, state.taxPercent, 0.01)
            assertEquals(550L, state.taxAmount) // 11% of 5000
            assertEquals(5550L, state.grandTotal) // 5000 + 550
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== canComplete Tests ====================

    @Test
    fun `canComplete is false when cash payment is insufficient`() = runTest {
        val cart = Cart().addItem(testProduct1) // Grand total ~5550
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetAmountPaid("5000"))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertFalse(state.canComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canComplete is true when cash payment is sufficient`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.setExactAmount()
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertTrue(state.canComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canComplete is always true for QRIS payment`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.QRIS))
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertTrue(state.canComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Payment Confirmation Tests ====================

    @Test
    fun `ConfirmPayment with insufficient amount shows error`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            // Don't set amount paid
            viewModel.onEvent(CheckoutEvent.ConfirmPayment)
            
            val state = awaitItem() as CheckoutUiState.Ready
            assertNotNull(state.paymentError)
            assertTrue(state.paymentError!!.contains("kurang"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ConfirmPayment success transitions to Success state`() = runTest {
        val cart = Cart().addItem(testProduct1)
        val mockTransaction = Transaction(
            id = "txn-1",
            transactionNumber = "TRX-20260115-0001",
            items = emptyList(),
            subtotal = 5000,
            discountAmount = 0,
            discountPercent = 0.0,
            taxAmount = 550,
            taxPercent = 11.0,
            grandTotal = 5550,
            paymentMethod = PaymentMethod.CASH,
            amountPaid = 10000,
            changeAmount = 4450,
            notes = null,
            customerName = null,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
        
        coEvery { 
            transactionRepository.createTransaction(any(), any(), any(), any(), any(), any(), any()) 
        } returns Result.Success(mockTransaction)
        
        viewModel.uiState.test {
            awaitItem() // Loading
            viewModel.initializeWithCart(cart)
            awaitItem() // Ready
            
            viewModel.setQuickAmount(10000)
            awaitItem() // intermediate amount
            awaitItem() // recalculated amount
            
            viewModel.onEvent(CheckoutEvent.ConfirmPayment)
            
            // Skip processing state if it appears
            var state = awaitItem()
            if (state is CheckoutUiState.Ready && state.isProcessing) {
                state = awaitItem()
            }
            
            assertTrue(state is CheckoutUiState.Success)
            assertEquals(mockTransaction.transactionNumber, (state as CheckoutUiState.Success).transaction.transactionNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ConfirmPayment failure shows error in Ready state`() = runTest {
        val cart = Cart().addItem(testProduct1)
        
        coEvery { 
            transactionRepository.createTransaction(any(), any(), any(), any(), any(), any(), any()) 
        } returns Result.Error(Exception("Database error"))
        
        viewModel.uiState.test {
            awaitItem()
            viewModel.initializeWithCart(cart)
            awaitItem()
            
            viewModel.setExactAmount()
            awaitItem()
            
            viewModel.onEvent(CheckoutEvent.ConfirmPayment)
            advanceUntilIdle()
            
            // Skip states until we get the error
            var state: CheckoutUiState
            do {
                state = awaitItem()
            } while (state is CheckoutUiState.Ready && state.isProcessing)
            
            assertTrue(state is CheckoutUiState.Ready)
            assertNotNull((state as CheckoutUiState.Ready).paymentError)
            assertFalse(state.isProcessing)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
