package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.Cart
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Plus

@Composable
fun PagedProductCatalog(
    products: LazyPagingItems<Product>,
    categories: List<Category>,
    selectedCategory: Category?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryResult: (Category?) -> Unit,
    cart: Cart,
    isGridView: Boolean,
    onToggleViewMode: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddCustomItem: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustomItemDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CatalogFilters(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategoryResult,
            isGridView = isGridView,
            onToggleViewMode = onToggleViewMode,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            PagedProducts(
                products = products,
                cart = cart,
                isGridView = isGridView,
                onProductClick = onProductClick,
                modifier = Modifier.fillMaxSize(),
            )

            CustomItemButton(
                onClick = { showCustomItemDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }

    if (showCustomItemDialog) {
        CustomItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onConfirm = { name, price ->
                onAddCustomItem(name, price)
                showCustomItemDialog = false
            },
        )
    }
}

@Composable
private fun CatalogFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    isGridView: Boolean,
    onToggleViewMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )

            ViewModeButton(
                isGridView = isGridView,
                onClick = onToggleViewMode,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CategoryList(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
        )
    }
}

@Composable
private fun ViewModeButton(
    isGridView: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
        ),
    ) {
        Icon(
            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
            contentDescription = stringResource(
                if (isGridView) R.string.pos_to_list_view else R.string.pos_to_grid_view,
            ),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CustomItemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.Full,
        color = Brand500,
        modifier = modifier.height(48.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = EvaIcons.Outline.Plus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.pos_custom_item),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
