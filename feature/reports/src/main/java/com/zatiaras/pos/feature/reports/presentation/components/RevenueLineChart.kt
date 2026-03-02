package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.GradientColors
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Beautiful bar chart for weekly revenue display.
 * Displays vertical bars with gradient colors like the web app.
 */
@Composable
fun RevenueLineChart(
    data: List<DailyRevenue>,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    // Animation
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }
    
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(AppShapes.L)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.reports_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val maxRevenue = data.maxOf { it.revenue }.coerceAtLeast(1000L).toFloat()
    val dayFormat = SimpleDateFormat("EEE", LocaleUtils.LOCALE_ID)
    
    // Base color for the chart (using primary theme color, which is pink)
    val primaryColor = MaterialTheme.colorScheme.primary
    
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
            Text(
                text = stringResource(R.string.reports_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingL))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Slightly taller
            ) {
                val width = size.width
                val height = size.height
                val barCount = data.size
                val barSpacing = width * 0.15f / (barCount + 1)
                val barWidth = (width - (barSpacing * (barCount + 1))) / barCount
                
                data.forEachIndexed { index, point ->
                    val x = barSpacing + index * (barWidth + barSpacing)
                    
                    // Normalized height with animation and a tiny minimum height for 0 values
                    val normalizedHeight = (point.revenue.toFloat() / maxRevenue).coerceIn(0f, 1f)
                    val minBarHeight = 4.dp.toPx()
                    val targetHeight = (normalizedHeight * height).coerceAtLeast(minBarHeight)
                    val animatedHeight = targetHeight * animationProgress.value
                    
                    val barTop = height - animatedHeight
                    
                    // Intensity based on height (taller bars are more solid/darker pink)
                    // Minimum intensity 0.35f, maximum 1.0f
                    val intensity = 0.35f + (normalizedHeight * 0.65f)
                    val barTopColor = primaryColor.copy(alpha = intensity * 0.7f) // Slightly lighter at the top
                    val barBottomColor = primaryColor.copy(alpha = intensity)     // Solid towards the bottom
                    
                    // Draw bar with dynamic gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barTopColor, barBottomColor),
                            startY = barTop,
                            endY = height
                        ),
                        topLeft = Offset(x, barTop),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { point ->
                    Text(
                        text = dayFormat.format(Date(point.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

