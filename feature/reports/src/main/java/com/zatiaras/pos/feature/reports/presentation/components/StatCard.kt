package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.LossRedLight
import com.zatiaras.pos.core.ui.theme.ProfitGreen
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.reports.R
import java.util.Locale

/**
 * Beautiful stat card with icon, value, and optional trend indicator.
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trendPercent: Double? = null,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTintColor: Color = MaterialTheme.colorScheme.primary
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier
            .animateContentSize(),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBrush != null) Color.Transparent else containerColor,
            contentColor = contentColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(
                    if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier
                )
                .padding(dimensions.paddingM)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(dimensions.spacingXS))
                        
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(dimensions.spacingXS))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(iconContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTintColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Trend indicator
                if (trendPercent != null) {
                    Spacer(modifier = Modifier.height(dimensions.spacingM))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isPositive = trendPercent >= 0
                        val trendColor = if (isPositive) {
                            ProfitGreen
                        } else {
                            LossRedLight
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.XS)
                                .background(trendColor.copy(alpha = 0.2f))
                                .padding(horizontal = dimensions.spacingS, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(dimensions.spacingXS))
                                Text(
                                    text = "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.1f", trendPercent)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = trendColor
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(dimensions.spacingS))
                        
                        Text(
                            text = stringResource(R.string.reports_vs_last_week),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format currency in Indonesian Rupiah.
 */
fun formatRupiah(amount: Long): String {
    return CurrencyFormatter.formatCurrency(amount)
}
