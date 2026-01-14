package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import com.zatiaras.pos.feature.pos.presentation.components.PagedProductCatalog

/**
 * Main POS Screen with product catalog and cart sidebar.
 * 
 * On tablet/landscape: Side-by-side layout (catalog | cart)
 * On phone/portrait: Catalog with floating cart button
 * 
 * Uses Paging 3 for efficient memory management with large product catalogs.
 * 
 * Refactored to use extracted components:
 * - PagedProductCatalog: Search, categories, paginated product grid
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
    val pagedProducts = viewModel.pagedProducts.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCartVisible by remember { mutableStateOf(false) }
    
    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PosEvent.DismissError)
        }
    }
    
    if (!uiState.isStoreOpen) {
        StoreClosedOverlay(onNavigateBack)
        return
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
            // Main Catalog Area (now paginated)
            // Main Catalog Area (now paginated)
            PagedProductCatalog(
                products = pagedProducts,
                categories = uiState.categories,
                cart = uiState.cart,
                selectedCategoryId = uiState.selectedCategoryId,
                searchQuery = uiState.searchQuery,
                isGridView = uiState.isGridView,
                onSearchChange = { viewModel.onEvent(PosEvent.SearchQueryChanged(it)) },
                onCategorySelect = { viewModel.onEvent(PosEvent.CategorySelected(it)) },
                onProductClick = { viewModel.onEvent(PosEvent.AddToCart(it)) },
                onToggleView = { viewModel.onEvent(PosEvent.ToggleViewMode) },
                onAddCustomItem = { name, price -> viewModel.onEvent(PosEvent.AddCustomItem(name, price)) },
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
private fun StoreClosedOverlay(onNavigateBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Toko Sedang Tutup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Silakan buka toko terlebih dahulu melalui Dashboard untuk memulai transaksi.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Kembali ke Dashboard")
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
