package com.zatiaras.pos.feature.reports.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.GradientColors
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.feature.reports.presentation.components.StatCard
import com.zatiaras.pos.feature.reports.presentation.components.formatRupiah

/**
 * Prominent store status banner - shown at TOP of dashboard
 */
@Composable
internal fun StoreStatusBanner(
    isStoreOpen: Boolean,
    onOpenClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isStoreOpen) onCloseClick() else onOpenClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isStoreOpen) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensions.paddingM)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isStoreOpen) SuccessGreen
                            else ErrorRed
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isStoreOpen) stringResource(R.string.store_is_open) else stringResource(R.string.store_is_closed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isStoreOpen) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (isStoreOpen) 
                            stringResource(R.string.store_tap_to_close) 
                        else 
                            stringResource(R.string.store_tap_to_open),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isStoreOpen) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = if (isStoreOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isStoreOpen) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
internal fun TodayStatsSection(uiState: HomeDashboardUiState) {
    Column {
        Text(
            text = stringResource(R.string.period_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Main revenue card
        StatCard(
            title = stringResource(R.string.stat_today_revenue),
            value = formatRupiah(uiState.stats.todayRevenue),
            icon = Icons.Default.AccountBalanceWallet,
            trendPercent = uiState.stats.revenueGrowthPercent,
            gradientColors = GradientColors.Revenue
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Transaction and items sold row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = stringResource(R.string.stat_transactions),
                value = uiState.stats.todayTransactions.toString(),
                icon = Icons.Default.Receipt,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                gradientColors = GradientColors.Transaction
            )
            
            StatCard(
                title = stringResource(R.string.stat_products_sold),
                value = uiState.stats.todayItemsSold.toString(),
                icon = Icons.Default.Inventory2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                gradientColors = GradientColors.ProductSold
            )
        }
    }
}

@Composable
internal fun PeriodSummarySection(uiState: HomeDashboardUiState) {
    Column {
        Text(
            text = stringResource(R.string.pnl_period),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PeriodCard(
                title = stringResource(R.string.period_this_week),
                value = formatRupiah(uiState.stats.weeklyRevenue),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                gradientColors = GradientColors.WeeklyPeriod
            )
            
            PeriodCard(
                title = stringResource(R.string.period_this_month),
                value = formatRupiah(uiState.stats.monthlyRevenue),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                gradientColors = GradientColors.MonthlyPeriod
            )
        }
    }
}

@Composable
internal fun PeriodCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(gradientColors)
            )
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
