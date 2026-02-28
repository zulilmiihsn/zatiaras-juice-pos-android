package com.zatiaras.pos.feature.pos.presentation

import app.cash.turbine.test
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.repository.ProductRepository
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.feature.pos.MainDispatcherRule
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for PosViewModel.
 * 
 * Tests:
 * - Initial state is loading
 * - Products and categories are loaded
 * - Cart operations (add, increment, decrement, remove)
 * - Search and filter functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var productRepository: ProductRepository
    private lateinit var storeSessionRepository: StoreSessionRepository
    private lateinit var addOnRepository: AddOnRepository
    private lateinit var viewModel: PosViewModel

    private val testCategory = Category(id = "cat-1", name = "Minuman")

    private val testProducts = listOf(
        Product(
            id = "prod-1",
            name = "Es Teh",
            price = 5000,
            category = testCategory,
            imageUrl = null,
            description = "Es teh manis",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Product(
            id = "prod-2",
            name = "Kopi Susu",
            price = 15000,
            category = testCategory,
            imageUrl = null,
            description = "Kopi susu gula aren",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    @Before
    fun setup() {
        productRepository = mockk()
        storeSessionRepository = mockk()
        addOnRepository = mockk()
        every { productRepository.getProducts() } returns flowOf(testProducts)
        every { productRepository.getCategories() } returns flowOf(listOf(testCategory))
        every { productRepository.getProductCount() } returns flowOf(2)
        every { storeSessionRepository.getActiveSession() } returns flowOf(null)
        // Mock addOnRepository methods if necessary, or just rely on relaxed mocking if used
        // Since getAddOnsByIds is suspended, we might need to mock it if it's called in init or tests
        // But it's only called in showProductOptionsSheet which is triggered by an event, so initialization should be fine.
        
        viewModel = PosViewModel(productRepository, storeSessionRepository, addOnRepository)
    }

    @Test
    fun `initial state loads categories and product count`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            
            // Skip loading state, get to loaded state
            if (state.isLoading) {
                val loadedState = awaitItem()
                assertEquals(2, loadedState.productCount) // Product count from repository
                assertEquals(1, loadedState.categories.size)
                assertEquals(false, loadedState.isLoading)
            } else {
                assertEquals(2, state.productCount)
                assertEquals(1, state.categories.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addToCart adds product to cart`() = runTest {
        advanceUntilIdle() // let loadData complete
        
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle() // let showProductOptionsSheet complete
        
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.cart.itemCount)
        assertEquals(testProducts[0].id, state.cart.items[0].product.id)
    }

    @Test
    fun `addToCart increments quantity for existing product`() = runTest {
        advanceUntilIdle()
        
        // First add
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        // Second add - same product
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.cart.uniqueItemCount)
        assertEquals(2, state.cart.itemCount)
    }

    @Test
    fun `incrementItem increases quantity by 1`() = runTest {
        advanceUntilIdle()
        
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        val uniqueKey = viewModel.uiState.value.cart.items[0].uniqueKey
        viewModel.onEvent(PosEvent.IncrementItem(uniqueKey))
        
        assertEquals(2, viewModel.uiState.value.cart.getQuantity(testProducts[0].id))
    }

    @Test
    fun `decrementItem decreases quantity by 1`() = runTest {
        advanceUntilIdle()
        
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        val uniqueKey = viewModel.uiState.value.cart.items[0].uniqueKey
        
        viewModel.onEvent(PosEvent.IncrementItem(uniqueKey))
        viewModel.onEvent(PosEvent.DecrementItem(uniqueKey))
        
        assertEquals(1, viewModel.uiState.value.cart.getQuantity(testProducts[0].id))
    }

    @Test
    fun `decrementItem removes item when quantity reaches 0`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)

        val uniqueKey = viewModel.uiState.value.cart.items[0].uniqueKey
        viewModel.onEvent(PosEvent.DecrementItem(uniqueKey))

        assertTrue(viewModel.uiState.value.cart.isEmpty())
    }

    @Test
    fun `clearCart removes all items`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)

        viewModel.onEvent(PosEvent.AddToCart(testProducts[1]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)

        assertEquals(2, viewModel.uiState.value.cart.uniqueItemCount)

        viewModel.onEvent(PosEvent.ClearCart)

        assertTrue(viewModel.uiState.value.cart.isEmpty())
    }

    @Test
    fun `searchQueryChanged updates search query state`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(PosEvent.SearchQueryChanged("Kopi"))

        val state = viewModel.uiState.value
        assertEquals("Kopi", state.searchQuery)
        // Note: Actual product filtering is done via PagingData,
        // not exposed in UiState directly
    }

    @Test
    fun `categorySelected filters products by category`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(PosEvent.CategorySelected(testCategory.id))

        val state = viewModel.uiState.value
        assertEquals(testCategory.id, state.selectedCategoryId)
    }

    @Test
    fun `cart calculates subtotal correctly`() = runTest {
        advanceUntilIdle()
        
        // Add first product (5000)
        viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        // Add second product (15000)
        viewModel.onEvent(PosEvent.AddToCart(testProducts[1]))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.ConfirmAddToCart)
        
        assertEquals(20000L, viewModel.uiState.value.cart.subtotal)
    }
}
