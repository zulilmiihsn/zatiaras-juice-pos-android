package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.IconColors
import com.zatiaras.pos.core.ui.theme.IndigoAccent
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import com.zatiaras.pos.feature.auth.R

private val SecurityIconColor = IconColors.Settings
private val AccessIconColor = WarningAmber
private val PrinterIconColor = IconColors.Printer
private val MenuIconColor = IconColors.Store
private val SyncIconColor = InfoBlue
private val AboutIconColor = IconColors.Preview

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAccessControl: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogoutClick: () -> Unit,
    onTaxPercentageChange: (Double) -> Unit,
    onLowPerformanceModeChange: (Boolean) -> Unit,
) {
    ProfileSection(uiState)
    AccessSection(
        uiState = uiState,
        onNavigateToSecurity = onNavigateToSecurity,
        onNavigateToAccessControl = onNavigateToAccessControl,
    )
    OperationsSection(
        onNavigateToPrinter = onNavigateToPrinter,
        onNavigateToInventory = onNavigateToInventory,
    )
    ReportsSection(
        currentTaxPercentage = uiState.taxPercentage,
        onTaxPercentageChange = onTaxPercentageChange,
    )
    DataSection(
        lastSyncInfo = uiState.lastSyncInfo,
        onNavigateToSync = onNavigateToSync,
        onNavigateToTransactionHistory = onNavigateToTransactionHistory,
        onNavigateToAbout = onNavigateToAbout,
    )
    PerformanceSection(
        enabled = uiState.lowPerformanceMode,
        onEnabledChange = onLowPerformanceModeChange,
    )
    Spacer(modifier = Modifier.height(32.dp))
    LogoutButton(onClick = onLogoutClick)
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun ProfileSection(uiState: SettingsUiState) {
    PremiumProfileCard(
        userEmail = uiState.userEmail,
        userRole = uiState.userRole,
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun AccessSection(
    uiState: SettingsUiState,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAccessControl: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_sec_access),
        subtitle = stringResource(R.string.settings_sec_desc),
    )
    Spacer(modifier = Modifier.height(12.dp))

    EnhancedSettingsCard(
        icon = Icons.Outlined.Security,
        iconBackgroundColor = SecurityIconColor,
        title = stringResource(R.string.sec_title),
        subtitle = stringResource(R.string.settings_security_subtitle),
        statusBadge = if (uiState.lockEnabled) stringResource(R.string.settings_status_active) else null,
        statusBadgeColor = if (uiState.lockEnabled) SuccessGreen else null,
        onClick = onNavigateToSecurity,
    )

    if (uiState.isOwner) {
        Spacer(modifier = Modifier.height(8.dp))
        EnhancedSettingsCard(
            icon = Icons.Outlined.AdminPanelSettings,
            iconBackgroundColor = AccessIconColor,
            title = stringResource(R.string.access_control_title),
            subtitle = stringResource(R.string.access_control_desc),
            statusBadge = stringResource(R.string.user_role_owner),
            statusBadgeColor = WarningAmber,
            onClick = onNavigateToAccessControl,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun OperationsSection(
    onNavigateToPrinter: () -> Unit,
    onNavigateToInventory: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_devices),
        subtitle = stringResource(R.string.settings_devices_desc),
    )
    Spacer(modifier = Modifier.height(12.dp))

    EnhancedSettingsCard(
        icon = Icons.Outlined.Print,
        iconBackgroundColor = PrinterIconColor,
        title = stringResource(R.string.settings_printer),
        subtitle = stringResource(R.string.settings_printer_desc),
        onClick = onNavigateToPrinter,
    )

    Spacer(modifier = Modifier.height(8.dp))
    EnhancedSettingsCard(
        icon = Icons.Outlined.Restaurant,
        iconBackgroundColor = MenuIconColor,
        title = stringResource(R.string.settings_menu),
        subtitle = stringResource(R.string.settings_menu_desc),
        onClick = onNavigateToInventory,
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ReportsSection(
    currentTaxPercentage: Double,
    onTaxPercentageChange: (Double) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_reports_title),
        subtitle = stringResource(R.string.settings_reports_desc),
    )
    Spacer(modifier = Modifier.height(12.dp))
    TaxPercentageCard(
        currentPercentage = currentTaxPercentage,
        onPercentageChange = onTaxPercentageChange,
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DataSection(
    lastSyncInfo: LastSyncInfo,
    onNavigateToSync: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_data_info),
        subtitle = stringResource(R.string.settings_sync_desc),
    )
    Spacer(modifier = Modifier.height(12.dp))

    EnhancedSettingsCard(
        icon = Icons.Outlined.Sync,
        iconBackgroundColor = SyncIconColor,
        title = stringResource(R.string.settings_sync),
        subtitle = lastSyncInfoText(lastSyncInfo),
        onClick = onNavigateToSync,
    )

    Spacer(modifier = Modifier.height(8.dp))
    EnhancedSettingsCard(
        icon = Icons.Outlined.History,
        iconBackgroundColor = IndigoAccent,
        title = stringResource(R.string.settings_history_title),
        subtitle = stringResource(R.string.settings_history_desc),
        onClick = onNavigateToTransactionHistory,
    )

    Spacer(modifier = Modifier.height(8.dp))
    EnhancedSettingsCard(
        icon = Icons.Outlined.Info,
        iconBackgroundColor = AboutIconColor,
        title = stringResource(R.string.settings_about),
        subtitle = stringResource(R.string.settings_about_version, "1.0"),
        onClick = onNavigateToAbout,
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun PerformanceSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_performance_title),
        subtitle = stringResource(R.string.settings_performance_desc),
    )
    Spacer(modifier = Modifier.height(12.dp))
    PerformanceModeCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
    )
}

@Composable
private fun lastSyncInfoText(lastSyncInfo: LastSyncInfo): String = when (lastSyncInfo.unit) {
    LastSyncUnit.NEVER -> stringResource(R.string.sync_never)
    LastSyncUnit.JUST_NOW -> stringResource(R.string.sync_just_now)
    LastSyncUnit.MINUTES_AGO -> stringResource(R.string.sync_mins_ago, lastSyncInfo.value)
    LastSyncUnit.HOURS_AGO -> stringResource(R.string.sync_hours_ago, lastSyncInfo.value)
    LastSyncUnit.DAYS_AGO -> stringResource(R.string.sync_days_ago, lastSyncInfo.value)
}
