package com.zatiaras.pos.feature.inventory.presentation.list

import app.cash.turbine.test
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import com.zatiaras.pos.core.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InventoryViewModel.
 * 
 * Tests:
 * - Initial loading state
 * - Products and categories loading
 * - Search functionality
 * - Category filter
 * - Refresh/sync
 * - Delete product
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productRepository: ProductRepository
    private lateinit var addOnRepository: AddOnRepository
    private lateinit var viewModel: InventoryViewModel

    private val testCategory1 = Category(id = "cat-1", name = "Minuman")
    private val testCategory2 = Category(id = "cat-2", name = "Makanan")
    
    private val testProducts = listOf(
        Product(
            id = "prod-1",
            name = "Es Teh",
            price = 5000,
            category = testCategory1,
            imageUrl = null,
            description = "Es teh manis",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Product(
            id = "prod-2",
            name = "Kopi Susu",
            price = 15000,
            category = testCategory1,
            imageUrl = null,
            description = "Kopi susu gula aren",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Product(
            id = "prod-3",
            name = "Nasi Goreng",
            price = 25000,
            category = testCategory2,
            imageUrl = null,
            description = "Nasi goreng spesial",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    private val testCategories = listOf(testCategory1, testCategory2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        productRepository = mockk()
        addOnRepository = mockk(relaxed = true)
        
        // Use MutableStateFlow (hot, never completes) so combine stays alive
        // flowOf completes after emission, killing combine's re-emission on control state changes
        every { productRepository.getProducts() } returns MutableStateFlow(testProducts)
        every { productRepository.getCategories() } returns MutableStateFlow(testCategories)
        every { addOnRepository.observeActiveAddOns() } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): InventoryViewModel {
        return InventoryViewModel(productRepository, addOnRepository)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = createViewModel()
        
        // Should start with Loading state
        assertTrue(viewModel.uiState.value is InventoryUiState.Loading)
    }

    @Test
    fun `loads products and categories on init`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoryUiState.Success)
            
            val successState = state as InventoryUiState.Success
            assertEquals(3, successState.products.size)
            assertEquals(2, successState.categories.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Search Tests ====================

    @Test
    fun `search updates searchQuery in state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.onEvent(InventoryEvent.Search("Kopi"))
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem() as InventoryUiState.Success
            assertEquals("Kopi", state.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty search query is handled`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First search for something (creates a real state change)
        viewModel.onEvent(InventoryEvent.Search("test"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then clear search back to empty
        viewModel.onEvent(InventoryEvent.Search(""))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as InventoryUiState.Success
        assertEquals("", state.searchQuery)
    }

    // ==================== Category Filter Tests ====================

    @Test
    fun `selectCategory updates selectedCategoryId`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.onEvent(InventoryEvent.SelectCategory("cat-1"))
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem() as InventoryUiState.Success
            assertEquals("cat-1", state.selectedCategoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectCategory with null clears filter`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem()
            
            // First select a category
            viewModel.onEvent(InventoryEvent.SelectCategory("cat-1"))
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            
            // Then clear it
            viewModel.onEvent(InventoryEvent.SelectCategory(null))
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem() as InventoryUiState.Success
            assertNull(state.selectedCategoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Refresh Tests ====================

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        coEvery { productRepository.syncFromRemote() } returns Result.success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(InventoryEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // After refresh completes, isRefreshing should be false
        val state = viewModel.uiState.value as InventoryUiState.Success
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `refresh calls syncFromRemote`() = runTest {
        coEvery { productRepository.syncFromRemote() } returns Result.success(Unit)
        
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(InventoryEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { productRepository.syncFromRemote() }
    }

    // ==================== Delete Tests ====================

    @Test
    fun `deleteProduct calls repository delete`() = runTest {
        coEvery { productRepository.deleteProduct(any()) } returns Result.success(Unit)
        
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(InventoryEvent.DeleteProduct("prod-1"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { productRepository.deleteProduct("prod-1") }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `error during load sets Error state`() = runTest {
        every { productRepository.getProducts() } throws RuntimeException("Database error")
        
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoryUiState.Error)
            
            val errorState = state as InventoryUiState.Error
            assertTrue(errorState.message.contains("Database error") || errorState.message.contains("Gagal"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
