package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.AlertTriangle
import compose.icons.evaicons.outline.ArrowBack
import compose.icons.evaicons.outline.Cube
import compose.icons.evaicons.outline.Grid
import compose.icons.evaicons.outline.List
import compose.icons.evaicons.outline.Plus
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
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
    val state = uiState
    
    // Snackbar State
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Show snackbar when message is available
    val currentMessage = (state as? InventoryUiState.Success)?.snackbarMessage
    LaunchedEffect(currentMessage) {
        if (currentMessage != null) {
            // MOST IMPORTANT: Launch in the enclosing scope so it isn't cancelled
            // when LaunchedEffect(currentMessage) restarts with null.
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = currentMessage,
                    duration = SnackbarDuration.Short
                )
            }
            // Clear the state so it doesn't repeatedly trigger on recomposition
            viewModel.onEvent(InventoryEvent.SnackbarDismissed)
        }
    }
    
    // Dialog States
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddAddOnDialog by remember { mutableStateOf(false) }
    
    // Edit Dialog States  
    var categoryToEdit by remember { mutableStateOf<com.zatiaras.pos.core.domain.model.Category?>(null) }
    var addOnToEdit by remember { mutableStateOf<com.zatiaras.pos.core.domain.model.AddOn?>(null) }
    
    // Determine FAB action based on current state
    val fabAction: () -> Unit = {
        if (state is InventoryUiState.Success) {
            when (state.selectedTab) {
                InventoryTab.MENU -> onNavigateToDetail(null)
                InventoryTab.CATEGORIES -> showAddCategoryDialog = true
                InventoryTab.ADD_ONS -> showAddAddOnDialog = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.inventory_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = EvaIcons.Outline.ArrowBack,
                            contentDescription = stringResource(R.string.inventory_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { com.zatiaras.pos.core.ui.components.ZatSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state is InventoryUiState.Success) {
                FloatingActionButton(
                    onClick = fabAction,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = EvaIcons.Outline.Plus,
                        contentDescription = stringResource(R.string.inventory_add_product)
                    )
                }
            }
        }
    ) { paddingValues ->
        InventoryContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onProductClick = { productId -> onNavigateToDetail(productId) },
            onEditCategory = { category -> categoryToEdit = category },
            onEditAddOn = { addOn -> addOnToEdit = addOn },
            modifier = Modifier.padding(paddingValues)
        )
        
        // Add Category Dialog
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name -> 
                    viewModel.onEvent(InventoryEvent.AddCategory(name)) 
                }
            )
        }
        
        // Add Add-on Dialog
        if (showAddAddOnDialog) {
            AddAddOnDialog(
                onDismiss = { showAddAddOnDialog = false },
                onConfirm = { name, price -> 
                    viewModel.onEvent(InventoryEvent.AddAddOn(name, price)) 
                }
            )
        }
        
        // Edit Category Dialog
        categoryToEdit?.let { category ->
            // Get products from uiState if Success
            val products = if (state is InventoryUiState.Success) {
                state.products
            } else {
                emptyList()
            }
            
            EditCategoryDialog(
                category = category,
                allProducts = products,
                onDismiss = { categoryToEdit = null },
                onConfirm = { id, name, productIds ->
                    viewModel.onEvent(InventoryEvent.UpdateCategoryWithProducts(id, name, productIds))
                }
            )
        }
        
        // Edit Add-on Dialog
        addOnToEdit?.let { addOn ->
            EditAddOnDialog(
                addOn = addOn,
                onDismiss = { addOnToEdit = null },
                onConfirm = { id, name, price ->
                    viewModel.onEvent(InventoryEvent.UpdateAddOn(id, name, price))
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryContent(
    uiState: InventoryUiState,
    onEvent: (InventoryEvent) -> Unit,
    onProductClick: (String) -> Unit,
    onEditCategory: (com.zatiaras.pos.core.domain.model.Category) -> Unit,
    onEditAddOn: (com.zatiaras.pos.core.domain.model.AddOn) -> Unit,
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
            Column(modifier = modifier.fillMaxSize()) {
                // Tabs
                InventoryTabs(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { onEvent(InventoryEvent.ChangeTab(it)) }
                )
                
                // Show search and toggle only for Menu tab
                if (uiState.selectedTab == InventoryTab.MENU) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InventorySearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { onEvent(InventoryEvent.Search(it)) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Toggle Grid/List View
                        IconButton(
                            onClick = { onEvent(InventoryEvent.ToggleViewMode) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isGridView) EvaIcons.Outline.List else EvaIcons.Outline.Grid,
                                contentDescription = if (uiState.isGridView) stringResource(R.string.inventory_view_list) else stringResource(R.string.inventory_view_grid),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    CategoryFilterRow(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = { onEvent(InventoryEvent.SelectCategory(it)) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { onEvent(InventoryEvent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (uiState.selectedTab) {
                            InventoryTab.MENU -> {
                                ProductListContent(
                                    products = uiState.filteredProducts,
                                    onProductClick = onProductClick,
                                    isEmpty = uiState.isEmpty,
                                    hasFilters = uiState.searchQuery.isNotBlank() || uiState.selectedCategoryId != null,
                                    isGridView = uiState.isGridView
                                )
                            }
                            InventoryTab.CATEGORIES -> {
                                CategoryListContent(
                                    categories = uiState.categories,
                                    onEditCategory = onEditCategory,
                                    onDeleteCategory = { onEvent(InventoryEvent.DeleteCategory(it)) }
                                )
                            }
                            InventoryTab.ADD_ONS -> {
                                AddOnListContent(
                                    addOns = uiState.addOns,
                                    onEditAddOn = onEditAddOn,
                                    onDeleteAddOn = { onEvent(InventoryEvent.DeleteAddOn(it)) }
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
fun ProductListContent(
    products: List<com.zatiaras.pos.core.domain.model.Product>,
    onProductClick: (String) -> Unit,
    isEmpty: Boolean,
    hasFilters: Boolean,
    isGridView: Boolean = true
) {
    if (isEmpty) {
        EmptyContent(
            hasFilters = hasFilters,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val dimensions = LocalDimensions.current
        
        if (isGridView) {
            // Grid View
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(dimensions.paddingM),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
                }
            }
        } else {
            // List View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(dimensions.paddingM),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    ProductListItem(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
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
                imageVector = EvaIcons.Outline.AlertTriangle,
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
                imageVector = EvaIcons.Outline.Cube,
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
