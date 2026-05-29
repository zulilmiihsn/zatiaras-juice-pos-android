package com.zatiaras.pos.feature.reports.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.components.OwnerPinDialog
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.reports.R
import com.zatiaras.pos.feature.reports.presentation.components.RevenueLineChart
import com.zatiaras.pos.feature.reports.presentation.components.StatisticsSection
import com.zatiaras.pos.feature.reports.presentation.components.TopProductsList

/**
 * Home dashboard orchestration for store status and sales summary.
 *
 * Keep the owner-gated open/close flow here because it coordinates dialog state,
 * PIN verification, and dashboard refresh callbacks. Visual sections live in
 * sibling component files.
 */
@Composable
fun HomeDashboardRoute(
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeDashboardScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onNavigateToSettings = onNavigateToSettings,
        onOpenStore = viewModel::openStore,
        onCloseStore = viewModel::closeStore,
        verifyPin = viewModel::verifyPin,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: HomeDashboardUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenStore: (Long) -> Unit = {},
    onCloseStore: () -> Unit = {},
    verifyPin: suspend (String) -> Boolean = { true },
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var showOpenStoreDialog by remember { mutableStateOf(false) }
    var showCloseStoreDialog by remember { mutableStateOf(false) }

    // Non-owner open/close actions wait here until the PIN dialog succeeds, then
    // resume the intended store action dialog.
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingStoreAction by remember { mutableStateOf<StoreAction?>(null) }
    val dimensions = LocalDimensions.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_tab_home),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.home_tab_settings),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AnimatedVisibility(
                visible = uiState.isLoading && uiState.stats.todayTransactions == 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimatedVisibility(
                visible = !uiState.isLoading || uiState.stats.todayTransactions > 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(dimensions.paddingL),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingL),
                ) {
                    // Keep errors inline so operators can still inspect the
                    // last loaded dashboard content.
                    if (uiState.error != null) {
                        item {
                            com.zatiaras.pos.core.ui.components.ErrorDisplay(
                                message = uiState.error.asString(),
                                onRetry = onRefresh,
                            )
                        }
                    }

                    // Store status is the primary operational control, so it
                    // stays above metrics and charts.
                    item {
                        StoreStatusBanner(
                            isStoreOpen = uiState.isStoreOpen,
                            onOpenClick = {
                                if (!uiState.isOwner) {
                                    pendingStoreAction = StoreAction.Open
                                    showPinDialog = true
                                } else {
                                    showOpenStoreDialog = true
                                }
                            },
                            onCloseClick = {
                                if (!uiState.isOwner) {
                                    pendingStoreAction = StoreAction.Close
                                    showPinDialog = true
                                } else {
                                    showCloseStoreDialog = true
                                }
                            },
                        )
                    }

                    item {
                        TodayStatsSection(uiState)
                    }

                    // Top products are more actionable during a shift than the
                    // trend chart, so they stay before the chart.
                    item {
                        TopProductsList(
                            products = uiState.topProducts,
                        )
                    }

                    item {
                        StatisticsSection(
                            averageTransactions = uiState.averageTransactionsPerDay,
                            peakHours = uiState.peakHours,
                            averageOrderValue = uiState.averageOrderValue,
                            averageItemsPerTransaction = uiState.averageItemsPerTransaction,
                            growthPercent = uiState.growthPercent,
                            busiestDay = uiState.busiestDay,
                        )
                    }

                    item {
                        RevenueLineChart(
                            data = uiState.weeklyRevenue,
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(dimensions.paddingXXL))
                    }
                }
            }
        }
    }

    // PIN dialog is shared by open and close actions; pendingStoreAction carries
    // the user's original intent through verification.
    if (showPinDialog) {
        OwnerPinDialog(
            onDismiss = {
                showPinDialog = false
                pendingStoreAction = null
            },
            onPinVerified = {
                showPinDialog = false
                when (pendingStoreAction) {
                    StoreAction.Open -> showOpenStoreDialog = true
                    StoreAction.Close -> showCloseStoreDialog = true
                    null -> {}
                }
                pendingStoreAction = null
            },
            verifyPin = verifyPin,
            screenName = if (pendingStoreAction == StoreAction.Open) stringResource(R.string.store_open_title) else stringResource(R.string.store_close_title),
        )
    }

    if (showOpenStoreDialog) {
        OpenStoreDialog(
            onDismiss = { showOpenStoreDialog = false },
            onConfirm = { amount ->
                onOpenStore(amount)
                showOpenStoreDialog = false
            },
        )
    }

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
            },
        )
    }
}

private enum class StoreAction { Open, Close }
