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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    // Gradient colors for bars (matching web app style)
    val barGradientColors = listOf(
        listOf(Color(0xFF667eea), Color(0xFF764ba2)), // Purple gradient
        listOf(Color(0xFF11998e), Color(0xFF38ef7d)), // Green gradient
        listOf(Color(0xFFf093fb), Color(0xFFf5576c)), // Pink gradient
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)), // Blue gradient
        listOf(Color(0xFFfa709a), Color(0xFFfee140)), // Orange-pink gradient
        listOf(Color(0xFF667eea), Color(0xFF764ba2)), // Purple gradient
        listOf(Color(0xFF11998e), Color(0xFF38ef7d))  // Green gradient
    )
    
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
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val maxRevenue = data.maxOfOrNull { it.revenue } ?: 1L
    val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pendapatan 7 Hari Terakhir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 16f
                val chartHeight = height - padding
                val barCount = data.size
                val totalSpacing = width * 0.3f // 30% of width for spacing
                val barWidth = (width - totalSpacing) / barCount
                val spacing = totalSpacing / (barCount + 1)
                
                data.forEachIndexed { index, point ->
                    val x = spacing + index * (barWidth + spacing)
                    val normalizedHeight = if (maxRevenue > 0) {
                        (point.revenue.toFloat() / maxRevenue.toFloat())
                    } else 0f
                    
                    val animatedHeight = normalizedHeight * chartHeight * animationProgress.value
                    val barTop = chartHeight - animatedHeight
                    
                    // Get gradient colors for this bar
                    val gradientColors = barGradientColors[index % barGradientColors.size]
                    
                    // Draw bar with gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = gradientColors,
                            startY = barTop,
                            endY = chartHeight
                        ),
                        topLeft = Offset(x, barTop),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
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

