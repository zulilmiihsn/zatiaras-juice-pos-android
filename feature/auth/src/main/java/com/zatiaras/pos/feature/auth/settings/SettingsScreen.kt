package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.feature.auth.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToAccessControl: () -> Unit = {},
    onNavigateToTransactionHistory: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToSecurity = onNavigateToSecurity,
        onNavigateToAccessControl = onNavigateToAccessControl,
        onNavigateToPrinter = onNavigateToPrinter,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToTransactionHistory = onNavigateToTransactionHistory,
        onNavigateToSync = onNavigateToSync,
        onNavigateToAbout = onNavigateToAbout,
        onLogoutClick = viewModel::logout,
        onTaxPercentageChange = viewModel::updateTaxPercentage,
        onLowPerformanceModeChange = viewModel::updateLowPerformanceMode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAccessControl: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogoutClick: () -> Unit,
    onTaxPercentageChange: (Double) -> Unit = {},
    onLowPerformanceModeChange: (Boolean) -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(onNavigateBack = onNavigateBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsContent(
                uiState = uiState,
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToAccessControl = onNavigateToAccessControl,
                onNavigateToPrinter = onNavigateToPrinter,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToTransactionHistory = onNavigateToTransactionHistory,
                onNavigateToSync = onNavigateToSync,
                onNavigateToAbout = onNavigateToAbout,
                onLogoutClick = onLogoutClick,
                onTaxPercentageChange = onTaxPercentageChange,
                onLowPerformanceModeChange = onLowPerformanceModeChange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.auth_back),
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
