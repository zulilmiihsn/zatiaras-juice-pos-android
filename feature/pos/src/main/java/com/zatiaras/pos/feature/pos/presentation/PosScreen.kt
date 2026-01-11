package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.zatiaras.pos.feature.pos.presentation.components.CartItemRow
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import com.zatiaras.pos.feature.pos.presentation.components.PosProductCard
import java.text.NumberFormat
import java.util.Locale

/**
 * Main POS Screen with product catalog and cart sidebar.
 * 
 * On tablet/landscape: Side-by-side layout (catalog | cart)
 * On phone/portrait: Catalog with floating cart button
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
    
    val priceFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    
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
                    // Cart button with badge
                    IconButton(onClick = { isCartVisible = !isCartVisible }) {
                        BadgedBox(
                            badge = {
                                if (uiState.cart.itemCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ) {
                                        Text(uiState.cart.itemCount.toString())
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onEvent(PosEvent.SearchQueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari produk...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.onEvent(PosEvent.SearchQueryChanged("")) 
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { viewModel.onEvent(PosEvent.CategorySelected(null)) },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { viewModel.onEvent(PosEvent.CategorySelected(category.id)) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Product Grid
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        
                        uiState.showEmptyState -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "😔",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tidak ada produk",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Coba ubah kata kunci atau kategori",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.filteredProducts,
                                    key = { it.id }
                                ) { product ->
                                    PosProductCard(
                                        product = product,
                                        quantityInCart = uiState.cart.getQuantity(product.id),
                                        onAddToCart = {
                                            viewModel.onEvent(PosEvent.AddToCart(product))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Cart Sidebar (animated)
            AnimatedVisibility(
                visible = isCartVisible && uiState.cart.isNotEmpty(),
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                CartSidebar(
                    cart = uiState.cart,
                    onIncrement = { productId ->
                        viewModel.onEvent(PosEvent.IncrementItem(productId))
                    },
                    onDecrement = { productId ->
                        viewModel.onEvent(PosEvent.DecrementItem(productId))
                    },
                    onRemove = { productId ->
                        viewModel.onEvent(PosEvent.RemoveFromCart(productId))
                    },
                    onClearCart = {
                        viewModel.onEvent(PosEvent.ClearCart)
                    },
                    onCheckout = onProceedToCheckout,
                    modifier = Modifier.width(320.dp)
                )
            }
        }
    }
}
