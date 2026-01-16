package com.zatiaras.pos.feature.pos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.repository.ProductRepository
import com.zatiaras.pos.feature.pos.domain.model.Cart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main POS screen.
 * 
 * Manages:
 * - Product catalog display (with pagination)
 * - Category filtering
 * - Search functionality
 * - Shopping cart state
 * 
 * The cart is stored in-memory only (not persisted).
 * This is intentional POS behavior - carts are session-based.
 */
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val storeSessionRepository: StoreSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    // Separate flows for category and search query to trigger pagination refresh
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    /**
     * Paginated products flow.
     * Automatically refreshes when category or search query changes.
     * Using cachedIn(viewModelScope) to cache data across configuration changes.
     */
    val pagedProducts: Flow<PagingData<Product>> = combine(
        _selectedCategoryId,
        _searchQuery
    ) { categoryId, query ->
        Pair(categoryId, query)
    }.flatMapLatest { (categoryId, query) ->
        when {
            query.isNotBlank() -> productRepository.searchProductsPaged(query)
            categoryId != null -> productRepository.getProductsByCategoryPaged(categoryId)
            else -> productRepository.getProductsPaged()
        }
    }.cachedIn(viewModelScope)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load categories (not paginated - typically small list)
            productRepository.getCategories()
                .catch { e ->
                    Timber.e(e, "Error loading categories")
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal memuat kategori"
                    )
                }
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories,
                        isLoading = false
                    )
                }
        }

        // Load product count for UI display
        viewModelScope.launch {
            productRepository.getProductCount()
                .catch { e ->
                    Timber.e(e, "Error loading product count")
                }
                .collect { count ->
                    _uiState.value = _uiState.value.copy(productCount = count)
                }
        }
        
        // Observe Store Session
        viewModelScope.launch {
            storeSessionRepository.getActiveSession().collectLatest { session ->
                _uiState.value = _uiState.value.copy(isStoreOpen = session != null)
            }
        }
    }

    fun onEvent(event: PosEvent) {
        when (event) {
            is PosEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
                _uiState.value = _uiState.value.copy(searchQuery = event.query)
            }
            
            is PosEvent.CategorySelected -> {
                _selectedCategoryId.value = event.categoryId
                _uiState.value = _uiState.value.copy(selectedCategoryId = event.categoryId)
            }
            
            is PosEvent.AddToCart -> {
                addToCart(event.product)
            }
            
            is PosEvent.IncrementItem -> {
                incrementItem(event.productId)
            }
            
            is PosEvent.DecrementItem -> {
                decrementItem(event.productId)
            }
            
            is PosEvent.RemoveFromCart -> {
                removeFromCart(event.productId)
            }
            
            is PosEvent.UpdateItemQuantity -> {
                updateQuantity(event.productId, event.quantity)
            }
            
            is PosEvent.ClearCart -> {
                clearCart()
            }
            
            is PosEvent.DismissError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
            
            is PosEvent.ToggleViewMode -> {
                _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
            }
            
            is PosEvent.AddCustomItem -> {
                val customProduct = Product(
                    id = "custom_${System.currentTimeMillis()}",
                    name = event.name,
                    price = event.price,
                    category = null,
                    imageUrl = null,
                    description = "Custom Item",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isActive = true
                )
                addToCart(customProduct)
            }

            // Navigation events are handled by the UI layer
            is PosEvent.ProceedToCheckout,
            is PosEvent.BackToCatalog -> {
                // No-op in ViewModel, handled by navigation
            }
        }
    }

    // ==================== Cart Operations ====================

    private fun addToCart(product: Product) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.addItem(product)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
        Timber.d("Added ${product.name} to cart. Total items: ${updatedCart.itemCount}")
    }

    private fun incrementItem(productId: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.incrementItem(productId)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun decrementItem(productId: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.decrementItem(productId)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun removeFromCart(productId: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.removeItem(productId)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun updateQuantity(productId: String, quantity: Int) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.updateQuantity(productId, quantity)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = Cart())
        Timber.d("Cart cleared")
    }

    /**
     * Get the current cart for checkout process.
     */
    fun getCurrentCart(): Cart = _uiState.value.cart
}
