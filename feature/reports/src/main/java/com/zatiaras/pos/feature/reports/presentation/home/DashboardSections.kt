package com.zatiaras.pos.feature.reports.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.IndigoAccent
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.Slate800
import com.zatiaras.pos.core.ui.theme.Slate900
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.SuccessGreenDark
import com.zatiaras.pos.core.ui.theme.ErrorRedDark
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
            .clip(AppShapes.L)
            .clickable { if (isStoreOpen) onCloseClick() else onOpenClick() }
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isStoreOpen) 
                        listOf(SuccessGreen.copy(alpha = 0.2f), SuccessGreen.copy(alpha = 0.05f))
                    else 
                        listOf(ErrorRed.copy(alpha = 0.2f), ErrorRed.copy(alpha = 0.05f))
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(0.dp)
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
                        .size(dimensions.spacingM)
                        .clip(CircleShape)
                        .background(
                            if (isStoreOpen) SuccessGreen
                            else ErrorRed
                        )
                )
                Spacer(modifier = Modifier.width(dimensions.spacingS))
                Column {
                    Text(
                        text = if (isStoreOpen) stringResource(R.string.store_is_open) else stringResource(R.string.store_is_closed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isStoreOpen) 
                            stringResource(R.string.store_tap_to_close) 
                        else 
                            stringResource(R.string.store_tap_to_open),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (isStoreOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isStoreOpen) 
                    SuccessGreen 
                else 
                    ErrorRed,
                modifier = Modifier.size(dimensions.iconSizeL)
            )
        }
    }
}

@Composable
internal fun TodayStatsSection(uiState: HomeDashboardUiState) {
    val isGrowthNegative = uiState.stats.revenueGrowthPercent < 0
    val infiniteTransition = rememberInfiniteTransition(label = "revenueCardEffects")
    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    var isRevenueCardVisible by remember { mutableStateOf(true) }

    val revenueGradientStart by animateColorAsState(
        targetValue = if (isGrowthNegative) ErrorRedDark else SuccessGreenDark,
        animationSpec = tween(durationMillis = 700),
        label = "revenueGradientStart"
    )

    val revenueGradientEnd by animateColorAsState(
        targetValue = if (isGrowthNegative) ErrorRed else SuccessGreen,
        animationSpec = tween(durationMillis = 700),
        label = "revenueGradientEnd"
    )

    val revenueCardScale by animateFloatAsState(
        targetValue = if (isGrowthNegative) 1f else 1.015f,
        animationSpec = tween(durationMillis = 450),
        label = "revenueCardScale"
    )

    val revenueGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "revenueGlowPulse"
    )

    val revenueShimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "revenueShimmerProgress"
    )

    val revenueBackgroundMotion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "revenueBackgroundMotion"
    )

    Column {
        Text(
            text = stringResource(R.string.period_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = LocalDimensions.current.spacingS)
        )
        
        val dimensions = LocalDimensions.current
        
        // Main revenue card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.L)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    isRevenueCardVisible = bounds.bottom > 0f && bounds.top < screenHeightPx
                }
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isGrowthNegative) ErrorRed.copy(alpha = 0.22f * revenueGlowPulse) else SuccessGreen.copy(alpha = 0.26f * revenueGlowPulse),
                            Color.White.copy(alpha = 0.18f * revenueGlowPulse),
                            if (isGrowthNegative) ErrorRedDark.copy(alpha = 0.20f * revenueGlowPulse) else SuccessGreenDark.copy(alpha = 0.22f * revenueGlowPulse)
                        )
                    ),
                    shape = AppShapes.L
                )
                .graphicsLayer {
                    scaleX = revenueCardScale
                    scaleY = revenueCardScale
                    shadowElevation = if (isRevenueCardVisible) {
                        if (isGrowthNegative) {
                            10f * revenueGlowPulse
                        } else {
                            18f * revenueGlowPulse
                        }
                    } else {
                        0f
                    }
                    ambientShadowColor = if (isGrowthNegative) ErrorRed else SuccessGreen
                    spotShadowColor = if (isGrowthNegative) ErrorRed else SuccessGreen
                }
                .drawWithContent {
                    drawContent()

                    if (!isRevenueCardVisible) return@drawWithContent

                    val activeSweepWindow = 0.75f
                    if (revenueShimmerProgress > activeSweepWindow) return@drawWithContent

                    val sweepProgress = revenueShimmerProgress / activeSweepWindow
                    val shimmerWidth = size.width * 0.34f
                    val travel = size.width + (shimmerWidth * 2)
                    val currentX = (travel * sweepProgress) - shimmerWidth

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isGrowthNegative) 0.14f else 0.22f),
                                Color.Transparent
                            ),
                            start = Offset(currentX - shimmerWidth, 0f),
                            end = Offset(currentX, size.height)
                        )
                    )
                }
        ) {
            StatCard(
                title = stringResource(R.string.stat_today_revenue),
                value = formatRupiah(uiState.stats.todayRevenue),
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.fillMaxWidth(),
                trendPercent = uiState.stats.revenueGrowthPercent,
                backgroundBrush = Brush.horizontalGradient(
                    colors = listOf(revenueGradientStart, revenueGradientEnd),
                    startX = 0f + (700f * revenueBackgroundMotion),
                    endX = 1200f + (700f * revenueBackgroundMotion)
                ),
                contentColor = Color.White,
                iconContainerColor = Color.White.copy(alpha = 0.2f),
                iconTintColor = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingM))
        
        // Transaction and items sold row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
        ) {
            StatCard(
                title = stringResource(R.string.stat_transactions),
                value = uiState.stats.todayTransactions.toString(),
                icon = Icons.Default.Receipt,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                backgroundBrush = Brush.horizontalGradient(
                    colors = listOf(Slate900, Slate800)
                ),
                contentColor = Color.White,
                iconContainerColor = Color.White.copy(alpha = 0.16f),
                iconTintColor = InfoBlue
            )
            
            StatCard(
                title = stringResource(R.string.stat_products_sold),
                value = uiState.stats.todayItemsSold.toString(),
                icon = Icons.Default.Inventory2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                backgroundBrush = Brush.horizontalGradient(
                    colors = listOf(IndigoAccent, SuccessGreenDark)
                ),
                contentColor = Color.White,
                iconContainerColor = Color.White.copy(alpha = 0.2f),
                iconTintColor = SuccessGreen
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
            modifier = Modifier.padding(bottom = LocalDimensions.current.spacingS)
        )
        
        val dimensions = LocalDimensions.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
        ) {
            PeriodCard(
                title = stringResource(R.string.period_this_week),
                value = formatRupiah(uiState.stats.weeklyRevenue),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            PeriodCard(
                title = stringResource(R.string.period_this_month),
                value = formatRupiah(uiState.stats.monthlyRevenue),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
internal fun PeriodCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val dimensions = LocalDimensions.current
    Box(
        modifier = modifier
            .clip(AppShapes.L)
            .background(containerColor)
            .fillMaxHeight()
            .padding(dimensions.paddingM)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingXS))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
