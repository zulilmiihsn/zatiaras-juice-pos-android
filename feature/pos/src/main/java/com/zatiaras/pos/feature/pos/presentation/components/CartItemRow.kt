package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.Slate400
import com.zatiaras.pos.core.ui.theme.Slate600
import com.zatiaras.pos.core.ui.theme.Slate800
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.domain.model.CartItem

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current

    // Modern card design with soft UI
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.paddingXXS, horizontal = 0.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = AppShapes.L,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
        ) {
            // Product Image with consistent styling
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(AppShapes.M)
                    .background(Slate50.copy(alpha = 0.5f))
                    .border(1.dp, Brand500.copy(alpha = 0.1f), AppShapes.M),
                contentAlignment = Alignment.Center
            ) {
                if (cartItem.product.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cartItem.product.imageUrl)
                            .crossfade(true)
                            .size(128)
                            .build(),
                        contentDescription = cartItem.product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Brand500.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Product Info & Controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = cartItem.product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        color = Slate800,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    
                    // Price
                    Text(
                        text = CurrencyFormatter.formatCurrency(cartItem.subtotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Brand500,
                            fontSize = 15.sp
                        )
                    )
                }
                
                // Variants/Modifiers text would go here
                if (cartItem.customizationSummary.isNotBlank()) {
                    Text(
                        text = cartItem.customizationSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Quantity Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern quantity stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Slate50, CircleShape)
                            .border(1.dp, Brand500.copy(alpha = 0.1f), CircleShape)
                            .height(32.dp)
                    ) {
                        IconButton(
                            onClick = if (cartItem.quantity == 1) onRemove else onDecrement,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (cartItem.quantity == 1) ErrorRed else Brand500
                            )
                        ) {
                            Icon(
                                imageVector = if (cartItem.quantity == 1) Icons.Outlined.Delete else Icons.Filled.Remove,
                                contentDescription = "Kurangi",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Animated count
                        AnimatedContent(
                            targetState = cartItem.quantity,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                } else {
                                    slideInVertically { height -> -height } + fadeIn() togetherWith
                                            slideOutVertically { height -> height } + fadeOut()
                                }.using(
                                    // Add specific settings for smooth motion
                                    SizeTransform(clip = false)
                                )
                            }, label = "Quantity Animation"
                        ) { count ->
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Brand500
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Tambah",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Optional: Unit price or other info
                    Text(
                        text = "${CurrencyFormatter.formatCurrency(cartItem.product.price)}/unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        }
    }
}

