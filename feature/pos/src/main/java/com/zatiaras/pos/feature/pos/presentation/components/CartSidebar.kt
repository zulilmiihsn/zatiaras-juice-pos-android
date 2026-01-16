package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import java.text.NumberFormat
import java.util.Locale

/**
 * Cart sidebar component for POS screen.
 * 
 * UX optimized for Ibu-ibu:
 * - Large, prominent checkout button
 * - Clear pricing
 * - Easy-to-read typography
 */
@Composable
fun CartSidebar(
    cart: Cart,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    
    val dimensions = LocalDimensions.current
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = AppShapes.CartSidebar
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            CartHeader(
                itemCount = cart.itemCount,
                onClearCart = onClearCart
            )
            
            HorizontalDivider()
            
            // Cart Items
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(dimensions.paddingM),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
            ) {
                items(
                    items = cart.items,
                    key = { it.product.id }
                ) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onIncrement = { onIncrement(cartItem.product.id) },
                        onDecrement = { onDecrement(cartItem.product.id) },
                        onRemove = { onRemove(cartItem.product.id) }
                    )
                }
            }
            
            HorizontalDivider()
            
            // Footer with Total and Checkout - LARGER for Ibu-ibu
            CartFooter(
                subtotal = cart.subtotal,
                priceFormatter = priceFormatter,
                onCheckout = onCheckout
            )
        }
    }
}

@Composable
private fun CartHeader(
    itemCount: Int,
    onClearCart: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.paddingM, vertical = dimensions.paddingS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Keranjang ($itemCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onClearCart,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Kosongkan",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CartFooter(
    subtotal: Long,
    priceFormatter: NumberFormat,
    onCheckout: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimensions.paddingM)
    ) {
        // Subtotal row - larger text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subtotal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = priceFormatter.format(subtotal),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Checkout Button - LARGE and GREEN for Ibu-ibu
        Button(
            onClick = onCheckout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Bayar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

