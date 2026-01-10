package com.zatiaras.pos.feature.pos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.inventory.domain.repository.ProductRepository
import com.zatiaras.pos.feature.pos.domain.model.Cart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main POS screen.
 * 
 * Manages:
 * - Product catalog display
 * - Category filtering
 * - Search functionality
 * - Shopping cart state
 * 
 * The cart is stored in-memory only (not persisted).
 * This is intentional POS behavior - carts are session-based.
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                productRepository.getProducts(),
                productRepository.getCategories()
            ) { products, categories ->
                _uiState.value.copy(
                    products = products,
                    categories = categories,
                    isLoading = false
                )
            }
            .catch { e ->
                Timber.e(e, "Error loading POS data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat produk"
                )
            }
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onEvent(event: PosEvent) {
        when (event) {
            is PosEvent.SearchQueryChanged -> {
                _uiState.value = _uiState.value.copy(searchQuery = event.query)
            }
            
            is PosEvent.CategorySelected -> {
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
