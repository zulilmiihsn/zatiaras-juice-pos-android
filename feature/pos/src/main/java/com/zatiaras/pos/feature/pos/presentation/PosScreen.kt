package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import com.zatiaras.pos.feature.pos.presentation.components.ProductCatalog

/**
 * Main POS Screen with product catalog and cart sidebar.
 * 
 * On tablet/landscape: Side-by-side layout (catalog | cart)
 * On phone/portrait: Catalog with floating cart button
 * 
 * Refactored to use extracted components:
 * - ProductCatalog: Search, categories, product grid
 * - CartSidebar: Cart items, checkout button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateBack: () -> Unit,
    onProceedToCheckout: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCartVisible by remember { mutableStateOf(false) }
    
    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PosEvent.DismissError)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Kasir",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    CartButton(
                        itemCount = uiState.cart.itemCount,
                        onClick = { isCartVisible = !isCartVisible }
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Catalog Area
            ProductCatalog(
                products = uiState.filteredProducts,
                categories = uiState.categories,
                cart = uiState.cart,
                selectedCategoryId = uiState.selectedCategoryId,
                searchQuery = uiState.searchQuery,
                isLoading = uiState.isLoading,
                showEmptyState = uiState.showEmptyState,
                onSearchChange = { viewModel.onEvent(PosEvent.SearchQueryChanged(it)) },
                onCategorySelect = { viewModel.onEvent(PosEvent.CategorySelected(it)) },
                onProductClick = { viewModel.onEvent(PosEvent.AddToCart(it)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Cart Sidebar (animated)
            AnimatedVisibility(
                visible = isCartVisible && uiState.cart.isNotEmpty(),
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                CartSidebar(
                    cart = uiState.cart,
                    onIncrement = { viewModel.onEvent(PosEvent.IncrementItem(it)) },
                    onDecrement = { viewModel.onEvent(PosEvent.DecrementItem(it)) },
                    onRemove = { viewModel.onEvent(PosEvent.RemoveFromCart(it)) },
                    onClearCart = { viewModel.onEvent(PosEvent.ClearCart) },
                    onCheckout = onProceedToCheckout,
                    modifier = Modifier.width(320.dp)
                )
            }
        }
    }
}

@Composable
private fun CartButton(
    itemCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (itemCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ) {
                        Text(itemCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Keranjang"
            )
        }
    }
}

