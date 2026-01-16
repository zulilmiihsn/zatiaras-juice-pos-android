package com.zatiaras.pos.feature.reports.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.reports.presentation.components.RevenueLineChart
import com.zatiaras.pos.feature.reports.presentation.components.StatCard
import com.zatiaras.pos.feature.reports.presentation.components.TopProductsList
import com.zatiaras.pos.feature.reports.presentation.components.formatRupiah

@Composable
fun ReportDashboardRoute(
    onNavigateBack: (() -> Unit)?,
    onNavigateToPnl: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    viewModel: ReportDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    ReportDashboardScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onNavigateBack = onNavigateBack,
        onNavigateToPnl = onNavigateToPnl,
        onNavigateToChat = onNavigateToChat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDashboardScreen(
    uiState: ReportDashboardUiState,
    onRefresh: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onNavigateToPnl: () -> Unit,
    onNavigateToChat: () -> Unit
) {

    val pullToRefreshState = rememberPullToRefreshState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Laporan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Tanya AI") }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = uiState.isLoading && uiState.stats.todayTransactions == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            AnimatedVisibility(
                visible = !uiState.isLoading || uiState.stats.todayTransactions > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val dimensions = LocalDimensions.current
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(dimensions.paddingM),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
                ) {
                    // Today's Stats Section
                    item {
                        TodayStatsSection(uiState)
                    }
                    
                    // Top Products (PRIORITAS - di atas chart)
                    item {
                        TopProductsList(
                            products = uiState.topProducts
                        )
                    }
                    
                    // Weekly Revenue Chart (dipindah ke bawah)
                    item {
                        RevenueLineChart(
                            data = uiState.weeklyRevenue
                        )
                    }
                    
                    // Period Summary Cards
                    item {
                        PeriodSummarySection(uiState)
                    }
                    
                    // P&L Report Quick Access
                    item {
                        QuickAccessCard(
                            title = "Laporan Laba Rugi",
                            subtitle = "Lihat detail pendapatan, diskon, dan pajak",
                            onClick = onNavigateToPnl
                        )
                    }
                    
                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatsSection(uiState: ReportDashboardUiState) {
    Column {
        Text(
            text = "Hari Ini",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Main revenue card
        StatCard(
            title = "Pendapatan Hari Ini",
            value = formatRupiah(uiState.stats.todayRevenue),
            icon = Icons.Default.AccountBalanceWallet,
            trendPercent = uiState.stats.revenueGrowthPercent,
            gradientColors = listOf(
                Color(0xFF667eea),
                Color(0xFF764ba2)
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Transaction and items sold row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Transaksi",
                value = uiState.stats.todayTransactions.toString(),
                icon = Icons.Default.Receipt,
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    Color(0xFF11998e),
                    Color(0xFF38ef7d)
                )
            )
            
            StatCard(
                title = "Item Terjual",
                value = uiState.stats.todayItemsSold.toString(),
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    Color(0xFFf093fb),
                    Color(0xFFf5576c)
                )
            )
        }
    }
}

@Composable
private fun PeriodSummarySection(uiState: ReportDashboardUiState) {
    Column {
        Text(
            text = "Ringkasan Periode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PeriodCard(
                title = "Minggu Ini",
                value = formatRupiah(uiState.stats.weeklyRevenue),
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    Color(0xFF4facfe),
                    Color(0xFF00f2fe)
                )
            )
            
            PeriodCard(
                title = "Bulan Ini",
                value = formatRupiah(uiState.stats.monthlyRevenue),
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    Color(0xFFfa709a),
                    Color(0xFFfee140)
                )
            )
        }
    }
}

@Composable
private fun PeriodCard(
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

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF667eea), Color(0xFF764ba2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
