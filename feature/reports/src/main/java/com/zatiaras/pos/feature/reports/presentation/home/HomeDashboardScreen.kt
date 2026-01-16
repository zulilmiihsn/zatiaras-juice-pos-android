package com.zatiaras.pos.feature.reports.presentation.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import com.zatiaras.pos.feature.reports.presentation.components.StatisticsSection
import com.zatiaras.pos.feature.reports.presentation.components.formatRupiah
import com.zatiaras.pos.feature.reports.presentation.dashboard.ReportDashboardUiState
import com.zatiaras.pos.feature.reports.presentation.dashboard.ReportDashboardViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf

/**
 * Home Dashboard Screen - Complete Business Overview
 * This is the "Beranda" tab showing:
 * - Today's stats with revenue, transactions, items sold
 * - Weekly revenue chart
 * - Period summary (weekly & monthly)
 * - Top selling products
 */
@Composable
fun HomeDashboardRoute(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ReportDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    HomeDashboardScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onNavigateToSettings = onNavigateToSettings,
        onOpenStore = viewModel::openStore,
        onCloseStore = viewModel::closeStore
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: ReportDashboardUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenStore: (Long) -> Unit = {},
    onCloseStore: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var showOpenStoreDialog by remember { mutableStateOf(false) }
    var showCloseStoreDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    // Store Status Banner (TOP - Most Prominent)
                    item {
                        StoreStatusBanner(
                            isStoreOpen = uiState.isStoreOpen,
                            onOpenClick = { showOpenStoreDialog = true },
                            onCloseClick = { showCloseStoreDialog = true }
                        )
                    }
                    
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
                    
                    // Statistics Section (6 metrics in grid)
                    item {
                        StatisticsSection(
                            averageTransactions = uiState.averageTransactionsPerDay,
                            peakHours = uiState.peakHours,
                            averageOrderValue = uiState.averageOrderValue,
                            averageItemsPerTransaction = uiState.averageItemsPerTransaction,
                            growthPercent = uiState.growthPercent,
                            busiestDay = uiState.busiestDay
                        )
                    }
                    
                    // Weekly Revenue Chart (dipindah ke bawah)
                    item {
                        RevenueLineChart(
                            data = uiState.weeklyRevenue
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
    
    // Open Store Dialog
    if (showOpenStoreDialog) {
        OpenStoreDialog(
            onDismiss = { showOpenStoreDialog = false },
            onConfirm = { amount ->
                onOpenStore(amount)
                showOpenStoreDialog = false
            }
        )
    }
    
    // Close Store Dialog
    if (showCloseStoreDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCloseStoreDialog = false },
            title = { Text("Tutup Toko") },
            text = { Text("Apakah Anda yakin ingin menutup toko untuk hari ini?") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        onCloseStore()
                        showCloseStoreDialog = false
                    }
                ) {
                    Text("Tutup Toko")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCloseStoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
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

/**
 * Prominent store status banner - shown at TOP of dashboard
 */
@Composable
private fun StoreStatusBanner(
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
                            if (isStoreOpen) Color(0xFF10B981) // Green
                            else Color(0xFFEF4444) // Red
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isStoreOpen) "Toko Buka" else "Toko Tutup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isStoreOpen) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (isStoreOpen) 
                            "Ketuk untuk menutup toko" 
                        else 
                            "Ketuk untuk membuka toko",
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
private fun OpenStoreDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var openingBalance by remember { mutableLongStateOf(0L) }
    var balanceText by remember { mutableStateOf("0") }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Buka Toko",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Masukkan modal awal kasir:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input ->
                        balanceText = input.filter { it.isDigit() }
                        openingBalance = balanceText.toLongOrNull() ?: 0L
                    },
                    label = { Text("Modal Awal") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(openingBalance) }
            ) {
                Text("Buka Toko")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
