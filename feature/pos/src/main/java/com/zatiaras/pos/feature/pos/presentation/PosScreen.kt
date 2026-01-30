package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.core.ui.util.noRippleClickable
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import com.zatiaras.pos.feature.pos.presentation.components.PagedProductCatalog

/**
 * Main POS Screen with product catalog and floating cart bar.
 * 
 * Features:
 * - Floating cart summary bar at bottom (like GoFood/GrabFood)
 * - Slide-in cart sidebar when tapped
 * - Full-width product catalog
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
    val dimensions = LocalDimensions.current
    
    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PosEvent.DismissError)
        }
    }
    
    // Auto-hide cart sidebar when cart becomes empty
    LaunchedEffect(uiState.cart.isEmpty()) {
        if (uiState.cart.isEmpty()) {
            isCartVisible = false
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
                        stringResource(R.string.pos_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Catalog Area
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
                    .fillMaxSize()
                    .padding(bottom = if (uiState.cart.isNotEmpty()) 88.dp else 0.dp)
            )
            
            // Floating Cart Summary Bar (at bottom, like GoFood/GrabFood)
            AnimatedVisibility(
                visible = uiState.cart.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingCartBar(
                    itemCount = uiState.cart.itemCount,
                    total = uiState.cart.subtotal,
                    onClick = { isCartVisible = true }
                )
            }
            
            // Scrim (Overlay) - Dismiss on click
            if (isCartVisible && uiState.cart.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .noRippleClickable { isCartVisible = false }
                )
            }
            
            // Cart Sidebar (floating on top, slide from right)
            AnimatedVisibility(
                visible = isCartVisible && uiState.cart.isNotEmpty(),
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                CartSidebar(
                    cart = uiState.cart,
                    onIncrement = { viewModel.onEvent(PosEvent.IncrementItem(it)) },
                    onDecrement = { viewModel.onEvent(PosEvent.DecrementItem(it)) },
                    onRemove = { viewModel.onEvent(PosEvent.RemoveFromCart(it)) },
                    onClearCart = { viewModel.onEvent(PosEvent.ClearCart) },
                    onCheckout = onProceedToCheckout,
                    modifier = Modifier.width(dimensions.sidebarWidth)
                )
            }
        }
    }
}

/**
 * Floating cart summary bar - appears at bottom when cart has items.
 * Design inspired by food delivery apps (GoFood, GrabFood).
 */
@Composable
private fun FloatingCartBar(
    itemCount: Int,
    total: Long,
    onClick: () -> Unit
) {
    val priceFormatter = remember { CurrencyFormatter.getCurrencyFormatter() }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Cart icon + item count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "$itemCount item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // Right: Total + Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = priceFormatter.format(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Lihat Keranjang",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StoreClosedOverlay(onNavigateBack: () -> Unit) {
    val dimensions = LocalDimensions.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.paddingXXL),
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
                text = stringResource(R.string.pos_store_closed_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pos_store_closed_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.pos_back_to_dashboard))
            }
        }
    }
}
