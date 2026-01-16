package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.feature.inventory.presentation.components.CategoryFilterRow
import com.zatiaras.pos.feature.inventory.presentation.components.InventorySearchBar
import com.zatiaras.pos.feature.inventory.presentation.components.ProductCard
import com.zatiaras.pos.feature.inventory.R
import com.zatiaras.pos.core.ui.theme.LocalDimensions

/**
 * Main Inventory List Screen.
 * 
 * Displays:
 * - Search bar
 * - Category filter chips
 * - Product grid (2 columns)
 * - FAB to add new product
 * - Empty state when no products match filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (productId: String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.inventory_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.inventory_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDetail(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.inventory_add_product)
                )
            }
        }
    ) { paddingValues ->
        InventoryContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onProductClick = { productId -> onNavigateToDetail(productId) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryContent(
    uiState: InventoryUiState,
    onEvent: (InventoryEvent) -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is InventoryUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is InventoryUiState.Error -> {
            ErrorContent(
                message = uiState.message,
                modifier = modifier
            )
        }

        is InventoryUiState.Success -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onEvent(InventoryEvent.Refresh) },
                modifier = modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    Spacer(modifier = Modifier.height(8.dp))
                    InventorySearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { onEvent(InventoryEvent.Search(it)) }
                    )

                    // Category Filter
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryFilterRow(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = { onEvent(InventoryEvent.SelectCategory(it)) }
                    )

                    // Product Grid
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.isEmpty) {
                        EmptyContent(
                            hasFilters = uiState.searchQuery.isNotBlank() || 
                                        uiState.selectedCategoryId != null,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val dimensions = LocalDimensions.current
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(dimensions.paddingM),
                            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS),
                            verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
                        ) {
                            items(
                                items = uiState.filteredProducts,
                                key = { it.id }
                            ) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProductClick(product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensions.paddingXXL)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyContent(
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensions.paddingXXL)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasFilters) {
                    stringResource(R.string.inventory_no_match)
                } else {
                    stringResource(R.string.inventory_empty)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasFilters) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.inventory_add_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
