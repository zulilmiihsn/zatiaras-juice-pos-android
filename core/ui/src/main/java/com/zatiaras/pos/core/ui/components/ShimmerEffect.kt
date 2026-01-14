package com.zatiaras.pos.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for loading placeholders.
 * Creates a smooth, animated gradient that moves across the element.
 */
@Composable
fun shimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    )
    
    return Brush.linearGradient(
        colors = colors,
        start = Offset(shimmerProgress * 1000f - 500f, 0f),
        end = Offset(shimmerProgress * 1000f, 0f)
    )
}

/**
 * A shimmer box placeholder.
 * Use as a loading placeholder for any rectangular content.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Shimmer placeholder for product cards.
 */
@Composable
fun ShimmerProductCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Image placeholder
        ShimmerBox(
            height = 120.dp,
            shape = RoundedCornerShape(8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title placeholder
        ShimmerBox(
            height = 16.dp,
            shape = RoundedCornerShape(4.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Price placeholder
        ShimmerBox(
            width = 80.dp,
            height = 14.dp,
            shape = RoundedCornerShape(4.dp)
        )
    }
}

/**
 * Shimmer placeholder for cart items.
 */
@Composable
fun ShimmerCartItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Title
            ShimmerBox(
                height = 14.dp,
                shape = RoundedCornerShape(4.dp)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Subtitle
            ShimmerBox(
                width = 60.dp,
                height = 12.dp,
                shape = RoundedCornerShape(4.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Quantity controls placeholder
        Box(
            modifier = Modifier
                .size(80.dp, 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush())
        )
    }
}

/**
 * Shimmer placeholder for list items.
 */
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Avatar/icon placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Title
            ShimmerBox(
                height = 16.dp,
                shape = RoundedCornerShape(4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            ShimmerBox(
                width = 120.dp,
                height = 12.dp,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

/**
 * Shimmer placeholder for statistic cards (dashboard).
 */
@Composable
fun ShimmerStatCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Value placeholder
        ShimmerBox(
            width = 80.dp,
            height = 24.dp,
            shape = RoundedCornerShape(4.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label placeholder
        ShimmerBox(
            width = 60.dp,
            height = 12.dp,
            shape = RoundedCornerShape(4.dp)
        )
    }
}
