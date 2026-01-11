package com.zatiaras.pos.feature.pos.presentation

import app.cash.turbine.test
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.inventory.domain.repository.ProductRepository
import com.zatiaras.pos.feature.pos.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
        every { productRepository.getProducts() } returns flowOf(testProducts)
        every { productRepository.getCategories() } returns flowOf(listOf(testCategory))
        viewModel = PosViewModel(productRepository)
    }

    @Test
    fun `initial state loads products and categories`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            
            // Skip loading state, get to loaded state
            if (state.isLoading) {
                val loadedState = awaitItem()
                assertEquals(2, loadedState.products.size)
                assertEquals(1, loadedState.categories.size)
                assertEquals(false, loadedState.isLoading)
            } else {
                assertEquals(2, state.products.size)
                assertEquals(1, state.categories.size)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addToCart adds product to cart`() = runTest {
        viewModel.uiState.test {
            // Skip initial states
            skipItems(1)
            
            // When
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            
            // Then
            val state = awaitItem()
            assertEquals(1, state.cart.itemCount)
            assertEquals(testProducts[0].id, state.cart.items[0].product.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addToCart increments quantity for existing product`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            // When - add same product twice
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            awaitItem() // first add
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            
            // Then
            val state = awaitItem()
            assertEquals(1, state.cart.uniqueItemCount) // Still 1 unique item
            assertEquals(2, state.cart.itemCount) // But quantity is 2
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `incrementItem increases quantity by 1`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            awaitItem()
            
            viewModel.onEvent(PosEvent.IncrementItem(testProducts[0].id))
            
            val state = awaitItem()
            assertEquals(2, state.cart.getQuantity(testProducts[0].id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decrementItem decreases quantity by 1`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            // Add 2 items
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            awaitItem()
            viewModel.onEvent(PosEvent.IncrementItem(testProducts[0].id))
            awaitItem()
            
            // Decrement
            viewModel.onEvent(PosEvent.DecrementItem(testProducts[0].id))
            
            val state = awaitItem()
            assertEquals(1, state.cart.getQuantity(testProducts[0].id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decrementItem removes item when quantity reaches 0`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            awaitItem()
            
            viewModel.onEvent(PosEvent.DecrementItem(testProducts[0].id))
            
            val state = awaitItem()
            assertTrue(state.cart.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearCart removes all items`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0]))
            awaitItem()
            viewModel.onEvent(PosEvent.AddToCart(testProducts[1]))
            awaitItem()
            
            viewModel.onEvent(PosEvent.ClearCart)
            
            val state = awaitItem()
            assertTrue(state.cart.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchQueryChanged filters products`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.SearchQueryChanged("Kopi"))
            
            val state = awaitItem()
            assertEquals("Kopi", state.searchQuery)
            assertEquals(1, state.filteredProducts.size)
            assertEquals("Kopi Susu", state.filteredProducts[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `categorySelected filters products by category`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.CategorySelected(testCategory.id))
            
            val state = awaitItem()
            assertEquals(testCategory.id, state.selectedCategoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cart calculates subtotal correctly`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(PosEvent.AddToCart(testProducts[0])) // 5000
            awaitItem()
            viewModel.onEvent(PosEvent.AddToCart(testProducts[1])) // 15000
            
            val state = awaitItem()
            assertEquals(20000L, state.cart.subtotal)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
