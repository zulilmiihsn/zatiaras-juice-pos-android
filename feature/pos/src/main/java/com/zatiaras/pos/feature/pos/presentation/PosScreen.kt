package com.zatiaras.pos.feature.pos.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand400
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Slate600
import com.zatiaras.pos.core.ui.theme.Slate900
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.core.ui.util.noRippleClickable
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.presentation.components.CartSidebar
import com.zatiaras.pos.feature.pos.presentation.components.PagedProductCatalog
import com.zatiaras.pos.feature.pos.presentation.components.ProductOptionsBottomSheet
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.ArrowIosForward
import compose.icons.evaicons.outline.Lock

/**
 * Main cashier surface for catalog browsing and cart building.
 *
 * Keep responsive layout decisions here because cart presentation differs
 * between tablet side-by-side mode and mobile slide-over mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateBack: () -> Unit,
    onProceedToCheckout: () -> Unit,
    viewModel: PosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedProducts = viewModel.pagedProducts.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCartVisible by remember { mutableStateOf(false) }

    // Errors are one-shot UI messages; consume them after snackbar dispatch.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PosEvent.DismissError)
        }
    }

    // Hide the mobile cart panel once no items remain so an empty overlay cannot
    // block catalog interaction.
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { com.zatiaras.pos.core.ui.components.ZatSnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(paddingValues),
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Wide screens keep the cart persistent because there is enough
                // space to review cart and catalog together.
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PagedProductCatalog(
                            products = pagedProducts,
                            categories = uiState.categories,
                            cart = uiState.cart,
                            selectedCategory = uiState.categories.find { it.id == uiState.selectedCategoryId },
                            searchQuery = uiState.searchQuery,
                            isGridView = uiState.isGridView,
                            onSearchQueryChange = { viewModel.onEvent(PosEvent.SearchQueryChanged(it)) },
                            onCategoryResult = { category -> viewModel.onEvent(PosEvent.CategorySelected(category?.id)) },
                            onProductClick = { viewModel.onEvent(PosEvent.AddToCart(it)) },
                            onAddCustomItem = { name, price ->
                                viewModel.onEvent(PosEvent.AddCustomItem(name, price))
                            },
                            onToggleViewMode = { viewModel.onEvent(PosEvent.ToggleViewMode) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(350.dp)
                            .fillMaxHeight()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            ),
                    ) {
                        CartSidebar(
                            cart = uiState.cart,
                            onIncrement = { viewModel.onEvent(PosEvent.IncrementItem(it)) },
                            onDecrement = { viewModel.onEvent(PosEvent.DecrementItem(it)) },
                            onRemove = { viewModel.onEvent(PosEvent.RemoveFromCart(it)) },
                            onClearCart = { viewModel.onEvent(PosEvent.ClearCart) },
                            onCheckout = onProceedToCheckout,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                // Mobile uses a floating summary plus slide-over cart to keep
                // product browsing as the primary view.
                Box(modifier = Modifier.fillMaxSize()) {
                    PagedProductCatalog(
                        products = pagedProducts,
                        categories = uiState.categories,
                        cart = uiState.cart,
                        selectedCategory = uiState.categories.find { it.id == uiState.selectedCategoryId },
                        searchQuery = uiState.searchQuery,
                        isGridView = uiState.isGridView,
                        onToggleViewMode = { viewModel.onEvent(PosEvent.ToggleViewMode) },
                        onSearchQueryChange = { viewModel.onEvent(PosEvent.SearchQueryChanged(it)) },
                        onCategoryResult = { category -> viewModel.onEvent(PosEvent.CategorySelected(category?.id)) },
                        onProductClick = { viewModel.onEvent(PosEvent.AddToCart(it)) },
                        onAddCustomItem = { name, price ->
                            viewModel.onEvent(PosEvent.AddCustomItem(name, price))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (uiState.cart.isNotEmpty()) 80.dp else 0.dp),
                    )

                    AnimatedVisibility(
                        visible = uiState.cart.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    ) {
                        FloatingCartBar(
                            itemCount = uiState.cart.itemCount,
                            total = uiState.cart.subtotal,
                            onClick = { isCartVisible = true },
                        )
                    }

                    if (isCartVisible && uiState.cart.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Slate900.copy(alpha = 0.4f))
                                .noRippleClickable { isCartVisible = false },
                        )
                    }

                    AnimatedVisibility(
                        visible = isCartVisible && uiState.cart.isNotEmpty(),
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it }),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .widthIn(max = 400.dp)
                            .systemBarsPadding(),
                    ) {
                        CartSidebar(
                            cart = uiState.cart,
                            onIncrement = { viewModel.onEvent(PosEvent.IncrementItem(it)) },
                            onDecrement = { viewModel.onEvent(PosEvent.DecrementItem(it)) },
                            onRemove = { viewModel.onEvent(PosEvent.RemoveFromCart(it)) },
                            onClearCart = { viewModel.onEvent(PosEvent.ClearCart) },
                            onCheckout = onProceedToCheckout,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    // Product customization is confirmed from the bottom sheet before cart
    // mutation so partially edited options never leak into cart state.
    val selectedProduct = uiState.selectedProduct
    if (uiState.showProductOptionsSheet && selectedProduct != null) {
        ProductOptionsBottomSheet(
            product = selectedProduct,
            onAddToCart = { _, quantity, notes ->
                viewModel.onEvent(PosEvent.SetProductQuantity(quantity))
                viewModel.onEvent(PosEvent.SetProductNote(notes))
                viewModel.onEvent(PosEvent.ConfirmAddToCart)
            },
            onDismiss = { viewModel.onEvent(PosEvent.HideProductOptions) },
        )
    }
}

/**
 * Mobile summary action that opens the cart panel.
 */
@Composable
private fun FloatingCartBar(
    itemCount: Int,
    total: Long,
    onClick: () -> Unit,
) {
    val priceFormatter = remember { CurrencyFormatter.getCurrencyFormatter() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .noRippleClickable(onClick),
        shape = AppShapes.Full,
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Brand500, Brand400),
                    ),
                )
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Slate50.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = itemCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    Text(
                        text = "produk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = priceFormatter.format(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )

                    Icon(
                        imageVector = EvaIcons.Outline.ArrowIosForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Full-screen block shown when the store is closed.
 *
 * POS actions are unavailable until the dashboard opens the store for the day.
 */
@Composable
private fun StoreClosedOverlay(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(ErrorRed.copy(alpha = 0.08f), androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, ErrorRed.copy(alpha = 0.18f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = EvaIcons.Outline.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ErrorRed,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.pos_store_closed_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = Slate900,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.pos_store_closed_message),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                textAlign = TextAlign.Center,
                color = Slate600,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(0.dp, AppShapes.L),
                shape = AppShapes.L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand500,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.pos_back_to_dashboard),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
