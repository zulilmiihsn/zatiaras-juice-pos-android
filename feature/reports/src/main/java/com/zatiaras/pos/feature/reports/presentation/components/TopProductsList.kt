package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.MedalColors
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.reports.domain.model.TopProduct

/**
 * List of top selling products with progress bars.
 */
@Composable
fun TopProductsList(
    products: List<TopProduct>,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    val maxQuantity = products.maxOfOrNull { it.quantitySold } ?: 1
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.paddingL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stat_top_products),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MedalColors.Star,
                    modifier = Modifier.size(dimensions.iconSizeM)
                )
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.reports_no_sales_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                products.forEachIndexed { index, product ->
                    TopProductItem(
                        rank = index + 1,
                        product = product,
                        progress = product.quantitySold.toFloat() / maxQuantity.toFloat()
                    )
                    
                    if (index < products.size - 1) {
                        Spacer(modifier = Modifier.height(dimensions.spacingM))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductItem(
    rank: Int,
    product: TopProduct,
    progress: Float
) {
    val dimensions = LocalDimensions.current
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = rank * 100),
        label = "progress"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    // Medal emojis for top 3, then numbers for the rest
    val rankDisplay = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> rank.toString()
    }
    
    val rankColors = listOf(
        MedalColors.Gold,
        MedalColors.Silver,
        MedalColors.Bronze,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary
    )
    
    val rankColor = rankColors.getOrElse(rank - 1) { MaterialTheme.colorScheme.primary }
    val isMedal = rank <= 3
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge or medal
        if (isMedal) {
            // Show medal emoji
            Text(
                text = rankDisplay,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.size(32.dp) // Maintain emoji text boundary
            )
        } else {
            // Show number badge for rank 4+
            Box(
                modifier = Modifier
                    .size(dimensions.iconSizeL)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rankDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }
        
        Spacer(modifier = Modifier.width(dimensions.spacingM))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = stringResource(R.string.top_products_sold, product.quantitySold),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingXS))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(AppShapes.XS),
                color = rankColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingXXS))
            
            Text(
                text = formatRupiah(product.totalRevenue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
