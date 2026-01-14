package com.zatiaras.pos.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.home.R
import java.text.NumberFormat
import java.util.Locale

data class HomeMenuItem(
    val id: String,
    val titleResId: Int,
    val subtitleResId: Int,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    onNavigateToPOS: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    HomeScreen(
        uiState = uiState,
        onMenuClick = { menuId ->
            when (menuId) {
                "pos" -> onNavigateToPOS()
                "inventory" -> onNavigateToInventory()
                "transactions" -> onNavigateToTransactions()
                "reports" -> onNavigateToReports()
                "settings" -> onNavigateToSettings()
            }
        },
        onLogoutClick = viewModel::logout,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMenuClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onEvent: (HomeEvent) -> Unit
) {
    val dimensions = LocalDimensions.current
    var showOpenStoreDialog by remember { mutableStateOf(false) }
    var showCloseStoreDialog by remember { mutableStateOf(false) }

    val menuItems = remember {
        listOf(
            HomeMenuItem(
                id = "pos",
                titleResId = R.string.menu_pos,
                subtitleResId = R.string.menu_pos_subtitle,
                icon = Icons.Filled.ShoppingCart,
                gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
            ),
            HomeMenuItem(
                id = "inventory",
                titleResId = R.string.menu_inventory,
                subtitleResId = R.string.menu_inventory_subtitle,
                icon = Icons.Filled.Inventory2,
                gradientColors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
            ),
            HomeMenuItem(
                id = "transactions",
                titleResId = R.string.menu_transactions,
                subtitleResId = R.string.menu_transactions_subtitle,
                icon = Icons.Filled.Receipt,
                gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
            ),
            HomeMenuItem(
                id = "reports",
                titleResId = R.string.menu_reports,
                subtitleResId = R.string.menu_reports_subtitle,
                icon = Icons.Filled.Analytics,
                gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
            ),
            HomeMenuItem(
                id = "settings",
                titleResId = R.string.menu_settings,
                subtitleResId = R.string.menu_settings_subtitle,
                icon = Icons.Filled.Settings,
                gradientColors = listOf(Color(0xFF636363), Color(0xFFa2ab58))
            )
        )
    }

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
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = stringResource(R.string.home_logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(dimensions.paddingM)
        ) {
            // Greeting Card
            GreetingHeader(uiState.userName, uiState.branchName)
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))

            // Dashboard Metrics (Always visible, but real data only if Store Open)
            DashboardMetricsGrid(uiState.metrics, uiState.isStoreOpen)

            Spacer(modifier = Modifier.height(dimensions.spacingM))

            // Store Control (Buka/Tutup Toko)
            StoreSessionControl(
                isStoreOpen = uiState.isStoreOpen,
                onOpenClick = { showOpenStoreDialog = true },
                onCloseClick = { showCloseStoreDialog = true }
            )

            Spacer(modifier = Modifier.height(dimensions.spacingL))

            Text(
                text = stringResource(R.string.home_main_menu),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = dimensions.spacingS)
            )

            // Menu Grid (Non-scrollable within Scrollable Column, using FlowRow logic or just Column rows)
            // Since we are in verticalScroll Column, we shouldn't use LazyVerticalGrid with infinite height.
            // We'll use a simple Column with Rows layout for the menu items.
            SimpleMenuGrid(menuItems, onMenuClick)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showOpenStoreDialog) {
        OpenStoreDialog(
            onDismiss = { showOpenStoreDialog = false },
            onConfirm = { amount ->
                onEvent(HomeEvent.OpenStore(amount))
                showOpenStoreDialog = false
            }
        )
    }

    if (showCloseStoreDialog) {
        AlertDialog(
            onDismissRequest = { showCloseStoreDialog = false },
            title = { Text(stringResource(R.string.session_close_store)) },
            text = { Text(stringResource(R.string.session_close_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(HomeEvent.CloseStore)
                        showCloseStoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.session_confirm_close))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseStoreDialog = false }) {
                    Text(stringResource(R.string.session_cancel))
                }
            }
        )
    }
}

@Composable
fun GreetingHeader(userName: String, branchName: String) {
    val dimensions = LocalDimensions.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = AppShapes.L
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingL),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = userName.ifEmpty { stringResource(R.string.home_user_fallback) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = branchName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Filled.Store,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeXL),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DashboardMetricsGrid(metrics: DashboardMetrics, isStoreOpen: Boolean) {
    if (!isStoreOpen) return
    
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = stringResource(R.string.metric_revenue),
                value = formatCurrency.format(metrics.revenue),
                icon = Icons.Outlined.MonetizationOn,
                color = Color(0xFF11998e),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.metric_profit),
                value = formatCurrency.format(metrics.profit),
                icon = Icons.Outlined.TrendingUp,
                color = Color(0xFF4facfe),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = stringResource(R.string.metric_transactions),
                value = metrics.transactions.toString(),
                icon = Icons.Outlined.Receipt,
                color = Color(0xFFf093fb),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.metric_items),
                value = metrics.itemsSold.toString(),
                icon = Icons.Outlined.ShoppingBag,
                color = Color(0xFFf5576c),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = AppShapes.M
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StoreSessionControl(
    isStoreOpen: Boolean,
    onOpenClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Card(
        onClick = { if (isStoreOpen) onCloseClick() else onOpenClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStoreOpen) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
        ),
        shape = AppShapes.L
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isStoreOpen) stringResource(R.string.session_store_open) else stringResource(R.string.session_store_closed),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isStoreOpen) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (isStoreOpen) "Ketuk untuk menutup toko" else "Ketuk untuk membuka toko",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isStoreOpen) MaterialTheme.colorScheme.onErrorContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f)
                )
            }
            Icon(
                imageVector = if (isStoreOpen) Icons.Outlined.Lock else Icons.Outlined.Store,
                contentDescription = null,
                tint = if (isStoreOpen) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun SimpleMenuGrid(menuItems: List<HomeMenuItem>, onMenuClick: (String) -> Unit) {
    val chunkedItems = menuItems.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunkedItems.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        MenuCard(item = item, onClick = { onMenuClick(item.id) })
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MenuCard(
    item: HomeMenuItem,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val alpha = if (item.enabled) 1f else 0.5f
    val title = stringResource(item.titleResId)
    val subtitle = if (item.enabled) {
        stringResource(item.subtitleResId)
    } else {
        stringResource(R.string.menu_coming_soon)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f) // Slightly shorter than before
            .clip(AppShapes.L)
            .clickable(enabled = item.enabled, onClick = onClick),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = item.gradientColors.map { it.copy(alpha = alpha) }
                    )
                )
                .padding(dimensions.paddingM)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White.copy(alpha = alpha)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = alpha)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = alpha * 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun OpenStoreDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.session_open_store))
        },
        text = {
            Column {
                Text(stringResource(R.string.session_open_desc))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text(stringResource(R.string.session_initial_cash_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    onConfirm(amount)
                },
                enabled = amountText.isNotEmpty()
            ) {
                Text(stringResource(R.string.session_confirm_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.session_cancel))
            }
        }
    )
}
