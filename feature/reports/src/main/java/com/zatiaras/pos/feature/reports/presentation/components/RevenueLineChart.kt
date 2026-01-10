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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Beautiful line chart for weekly revenue display.
 */
@Composable
fun RevenueLineChart(
    data: List<DailyRevenue>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
    )
    
    // Animation
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
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
                val padding = 24f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding
                
                if (data.size < 2) return@Canvas
                
                val stepX = chartWidth / (data.size - 1)
                
                // Create path for line
                val linePath = Path()
                val fillPath = Path()
                
                data.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val normalizedY = if (maxRevenue > 0) {
                        (point.revenue.toFloat() / maxRevenue.toFloat())
                    } else 0f
                    val y = chartHeight - (normalizedY * chartHeight * animationProgress.value)
                    
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                
                // Complete fill path
                fillPath.lineTo(padding + (data.size - 1) * stepX, chartHeight)
                fillPath.close()
                
                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(gradientColors)
                )
                
                // Draw line
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw points
                data.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val normalizedY = if (maxRevenue > 0) {
                        (point.revenue.toFloat() / maxRevenue.toFloat())
                    } else 0f
                    val y = chartHeight - (normalizedY * chartHeight * animationProgress.value)
                    
                    // Outer circle
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    
                    // Inner circle
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
