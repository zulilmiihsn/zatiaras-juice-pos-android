package com.zatiaras.pos.feature.reports.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.window.DialogProperties
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
    viewModel: HomeDashboardViewModel = hiltViewModel()
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
    uiState: HomeDashboardUiState,
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
                            contentDescription = "Pengaturan",
                            tint = MaterialTheme.colorScheme.tertiary
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
        CloseStoreDialog(
            todayRevenue = uiState.stats.todayRevenue,
            todayTransactions = uiState.stats.todayTransactions,
            todayItemsSold = uiState.stats.todayItemsSold,
            openingBalance = uiState.openingBalance,
            todayExpenses = uiState.todayExpenses,
            onDismiss = { showCloseStoreDialog = false },
            onConfirm = {
                onCloseStore()
                showCloseStoreDialog = false
            }
        )
    }
}

@Composable
private fun TodayStatsSection(uiState: HomeDashboardUiState) {
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
private fun PeriodSummarySection(uiState: HomeDashboardUiState) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenStoreDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var openingBalance by remember { mutableLongStateOf(0L) }
    var balanceText by remember { mutableStateOf("") }
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Trigger enter animation
    LaunchedEffect(Unit) { isVisible = true }
    
    // Handle exit actions
    LaunchedEffect(pendingAction) {
        pendingAction?.let { action ->
            isVisible = false
            delay(300) // Wait for animation
            action()
        }
    }
    
    val presetAmounts = listOf(100_000L, 200_000L, 300_000L, 500_000L)
    
    Dialog(
        onDismissRequest = { pendingAction = { onDismiss() } },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.95f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with animated icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981),
                                            Color(0xFF34D399)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Title
                        Text(
                            text = "Buka Toko",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Description
                        Text(
                            text = "Masukkan modal awal kasir untuk memulai hari ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Quick amount selection chips
                        Text(
                            text = "Pilih Cepat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp)
                        )
                        
                        // Amount chips in 2x2 grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetAmounts.take(2).forEach { amount ->
                                    AmountChip(
                                        amount = amount,
                                        isSelected = openingBalance == amount,
                                        onClick = {
                                            openingBalance = amount
                                            balanceText = formatNumber(amount)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetAmounts.drop(2).forEach { amount ->
                                    AmountChip(
                                        amount = amount,
                                        isSelected = openingBalance == amount,
                                        onClick = {
                                            openingBalance = amount
                                            balanceText = formatNumber(amount)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Divider with "atau" text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = "  atau  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Custom amount input
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { input ->
                                val cleanInput = input.filter { it.isDigit() }
                                balanceText = if (cleanInput.isNotEmpty()) formatNumber(cleanInput.toLong()) else ""
                                openingBalance = cleanInput.toLongOrNull() ?: 0L
                            },
                            label = { Text("Masukkan Jumlah Lain") },
                            prefix = { Text("Rp ") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        
                        // Current selection display
                        if (openingBalance > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Modal Awal:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Rp ${formatNumber(openingBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pendingAction = { onDismiss() } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Text("Batal")
                            }
                            Button(
                                onClick = { pendingAction = { onConfirm(openingBalance) } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buka Toko", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountChip(
    amount: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Rp ${formatNumber(amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatNumber(number: Long): String {
    return java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(number)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloseStoreDialog(
    todayRevenue: Long,
    todayTransactions: Int,
    todayItemsSold: Int,
    openingBalance: Long,
    todayExpenses: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Trigger enter animation
    LaunchedEffect(Unit) { isVisible = true }
    
    // Handle exit actions
    LaunchedEffect(pendingAction) {
        pendingAction?.let { action ->
            isVisible = false
            delay(300) // Wait for animation
            action()
        }
    }

    Dialog(
        onDismissRequest = { pendingAction = { onDismiss() } },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.95f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFEF4444),
                                            Color(0xFFF87171)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Title
                        Text(
                            text = "Tutup Toko",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Description
                        Text(
                            text = "Berikut ringkasan penjualan hari ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Today's Summary Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Revenue row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pendapatan",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Rp ${formatNumber(todayRevenue)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                
                                // Transactions row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Transaksi",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$todayTransactions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                
                                // Items sold row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Item Terjual",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$todayItemsSold",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
        
                        // Cash Flow Summary Card
                        val netIncome = todayRevenue - todayExpenses
                        val currentCash = openingBalance + netIncome
        
                        Text(
                            text = "Arus Kas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )
        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Opening Balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Modal Awal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Rp ${formatNumber(openingBalance)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
        
                                // Net Income
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Penda. Bersih", // Pendapatan - Pengeluaran
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${if (netIncome >= 0) "+" else ""}Rp ${formatNumber(netIncome)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (netIncome >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
        
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
        
                                // Current Cash Total
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Uang Saat Ini",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Rp ${formatNumber(currentCash)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Confirmation message
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF3C7)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Setelah toko ditutup, tidak bisa melakukan transaksi sampai buka kembali.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pendingAction = { onDismiss() } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Text("Batal")
                            }
                            Button(
                                onClick = { pendingAction = { onConfirm() } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tutup Toko", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
