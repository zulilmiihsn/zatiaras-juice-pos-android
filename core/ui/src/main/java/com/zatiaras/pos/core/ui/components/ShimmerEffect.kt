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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes

/**
 * Shared brush for loading placeholders.
 *
 * Use this instead of ad hoc animated gradients so all skeleton states move at
 * the same speed and read as one design pattern.
 */
@Composable
fun shimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
    )

    return Brush.linearGradient(
        colors = colors,
        start = Offset(shimmerProgress * 1000f - 500f, 0f),
        end = Offset(shimmerProgress * 1000f, 0f),
    )
}

/**
 * Primitive rectangular shimmer used by the composed skeleton components below.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 20.dp,
    shape: Shape = AppShapes.S,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(shimmerBrush()),
    )
}

/**
 * Shimmer placeholder for product cards.
 */
@Composable
fun ShimmerProductCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(AppShapes.M)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
    ) {
        ShimmerBox(
            height = 120.dp,
            shape = AppShapes.S,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ShimmerBox(
            height = 16.dp,
            shape = AppShapes.XS,
        )

        Spacer(modifier = Modifier.height(4.dp))

        ShimmerBox(
            width = 80.dp,
            height = 14.dp,
            shape = AppShapes.XS,
        )
    }
}

/**
 * Shimmer placeholder for cart items.
 */
@Composable
fun ShimmerCartItem(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(AppShapes.S)
                .background(shimmerBrush()),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                height = 14.dp,
                shape = AppShapes.XS,
            )

            Spacer(modifier = Modifier.height(6.dp))

            ShimmerBox(
                width = 60.dp,
                height = 12.dp,
                shape = AppShapes.XS,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(80.dp, 32.dp)
                .clip(AppShapes.S)
                .background(shimmerBrush()),
        )
    }
}

/**
 * Shimmer placeholder for list items.
 */
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(shimmerBrush()),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                height = 16.dp,
                shape = AppShapes.XS,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ShimmerBox(
                width = 120.dp,
                height = 12.dp,
                shape = AppShapes.XS,
            )
        }
    }
}

/**
 * Shimmer placeholder for statistic cards (dashboard).
 */
@Composable
fun ShimmerStatCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(AppShapes.M)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(shimmerBrush()),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ShimmerBox(
            width = 80.dp,
            height = 24.dp,
            shape = AppShapes.XS,
        )

        Spacer(modifier = Modifier.height(4.dp))

        ShimmerBox(
            width = 60.dp,
            height = 12.dp,
            shape = AppShapes.XS,
        )
    }
}
